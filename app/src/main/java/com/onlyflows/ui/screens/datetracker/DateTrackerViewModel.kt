package com.onlyflows.ui.screens.datetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.DateTracker
import com.onlyflows.domain.model.DateTrackerCategory
import com.onlyflows.data.repository.DateTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DateTrackerUiState(
    val trackers: List<DateTracker>    = emptyList(),
    val showAddSheet: Boolean          = false,
    val editingTracker: DateTracker?   = null,
    // Draft fields (shared between add and edit)
    val draftName: String              = "",
    val draftEmoji: String             = "📅",
    val draftDate: LocalDate?          = null,
    val draftCategory: DateTrackerCategory = DateTrackerCategory.COUNTDOWN,
    val draftIsAnnual: Boolean         = false,
)

sealed interface DateTrackerEvent {
    data object ShowAddSheet : DateTrackerEvent
    data object HideAddSheet : DateTrackerEvent
    data class ShowEditSheet(val tracker: DateTracker) : DateTrackerEvent
    data object HideEditSheet : DateTrackerEvent
    data class SetDraftName(val name: String) : DateTrackerEvent
    data class SetDraftEmoji(val emoji: String) : DateTrackerEvent
    data class SetDraftDate(val date: LocalDate) : DateTrackerEvent
    data class SetDraftCategory(val category: DateTrackerCategory) : DateTrackerEvent
    data class SetDraftIsAnnual(val isAnnual: Boolean) : DateTrackerEvent
    data object SaveDraft : DateTrackerEvent
    data object SaveEdit : DateTrackerEvent
    data class Delete(val id: String) : DateTrackerEvent
}

class DateTrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DateTrackerRepository =
        (app as OnlyFlowsApp).container.dateTrackerRepository

    private val _state = MutableStateFlow(DateTrackerUiState())
    val uiState: StateFlow<DateTrackerUiState> get() = _state

    init {
        viewModelScope.launch {
            repo.observeAll().collect { trackers ->
                _state.update { it.copy(trackers = trackers) }
            }
        }
    }

    fun onEvent(event: DateTrackerEvent) {
        when (event) {
            DateTrackerEvent.ShowAddSheet  -> _state.update { it.copy(showAddSheet = true, editingTracker = null, draftName = "", draftEmoji = "📅", draftDate = null, draftCategory = DateTrackerCategory.COUNTDOWN, draftIsAnnual = false) }
            DateTrackerEvent.HideAddSheet  -> _state.update { it.copy(showAddSheet = false) }
            is DateTrackerEvent.ShowEditSheet -> _state.update { it.copy(
                showAddSheet    = false,
                editingTracker  = event.tracker,
                draftName       = event.tracker.name,
                draftEmoji      = event.tracker.emoji,
                draftDate       = event.tracker.targetDate,
                draftCategory   = event.tracker.category,
                draftIsAnnual   = event.tracker.isAnnual,
            )}
            DateTrackerEvent.HideEditSheet -> _state.update { it.copy(editingTracker = null) }
            is DateTrackerEvent.SetDraftName     -> _state.update { it.copy(draftName = event.name) }
            is DateTrackerEvent.SetDraftEmoji    -> _state.update { it.copy(draftEmoji = event.emoji) }
            is DateTrackerEvent.SetDraftDate     -> _state.update { it.copy(draftDate = event.date) }
            is DateTrackerEvent.SetDraftCategory -> _state.update { it.copy(
                draftCategory = event.category,
                draftIsAnnual = if (event.category == DateTrackerCategory.ANNIVERSARY) true else it.draftIsAnnual,
            )}
            is DateTrackerEvent.SetDraftIsAnnual -> _state.update { it.copy(draftIsAnnual = event.isAnnual) }
            DateTrackerEvent.SaveDraft -> saveDraft()
            DateTrackerEvent.SaveEdit  -> saveEdit()
            is DateTrackerEvent.Delete  -> viewModelScope.launch { repo.delete(event.id) }
        }
    }

    private fun saveDraft() {
        val s = _state.value
        val date = s.draftDate ?: return
        if (s.draftName.isBlank()) return
        viewModelScope.launch {
            repo.insert(
                DateTracker(
                    id         = UUID.randomUUID().toString(),
                    name       = s.draftName.trim(),
                    emoji      = s.draftEmoji,
                    targetDate = date,
                    category   = s.draftCategory,
                    isAnnual   = s.draftIsAnnual,
                    sortOrder  = _state.value.trackers.size,
                    createdAt  = LocalDateTime.now(),
                )
            )
            _state.update { it.copy(showAddSheet = false) }
        }
    }

    private fun saveEdit() {
        val s = _state.value
        val original = s.editingTracker ?: return
        val date = s.draftDate ?: return
        if (s.draftName.isBlank()) return
        viewModelScope.launch {
            repo.update(
                original.copy(
                    name       = s.draftName.trim(),
                    emoji      = s.draftEmoji,
                    targetDate = date,
                    category   = s.draftCategory,
                    isAnnual   = s.draftIsAnnual,
                )
            )
            _state.update { it.copy(editingTracker = null) }
        }
    }
}
