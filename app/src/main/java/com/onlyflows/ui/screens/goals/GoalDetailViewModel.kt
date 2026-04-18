package com.onlyflows.ui.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.model.GoalLog
import com.onlyflows.domain.repository.GoalLogRepository
import com.onlyflows.domain.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalDetailUiState(
    val goal: Goal? = null,
    val logs: List<GoalLog> = emptyList(),
    val commentDraft: String = "",
    val showAddLog: Boolean = false,
)

sealed interface GoalDetailEvent {
    data class SetComment(val text: String) : GoalDetailEvent
    data object ShowAddLog : GoalDetailEvent
    data object HideAddLog : GoalDetailEvent
    data class AddLog(val label: String, val note: String, val tags: List<String>) : GoalDetailEvent
    data class DeleteLog(val logId: String) : GoalDetailEvent
    data class ToggleStar(val logId: String, val starred: Boolean) : GoalDetailEvent
}

class GoalDetailViewModel(
    app: Application,
    savedState: SavedStateHandle,
) : AndroidViewModel(app) {

    private val goalId: String = savedState["goalId"] ?: ""
    private val goalRepo: GoalRepository = (app as OnlyFlowsApp).container.goalRepository
    private val logRepo: GoalLogRepository = (app as OnlyFlowsApp).container.goalLogRepository

    private val _commentDraft = MutableStateFlow("")
    private val _showAddLog = MutableStateFlow(false)

    val uiState: StateFlow<GoalDetailUiState> = combine(
        goalRepo.observeById(goalId),
        logRepo.observeByGoal(goalId),
        _commentDraft,
        _showAddLog,
    ) { goal, logs, draft, showAdd ->
        GoalDetailUiState(goal = goal, logs = logs, commentDraft = draft, showAddLog = showAdd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    fun onEvent(event: GoalDetailEvent) {
        when (event) {
            is GoalDetailEvent.SetComment -> _commentDraft.value = event.text
            GoalDetailEvent.ShowAddLog -> _showAddLog.value = true
            GoalDetailEvent.HideAddLog -> _showAddLog.value = false
            is GoalDetailEvent.AddLog -> {
                _showAddLog.value = false
                viewModelScope.launch {
                    logRepo.insert(
                        GoalLog(
                            id     = UUID.randomUUID().toString(),
                            goalId = goalId,
                            label  = event.label,
                            note   = event.note,
                            tags   = event.tags,
                        )
                    )
                }
            }
            is GoalDetailEvent.DeleteLog -> viewModelScope.launch { logRepo.softDelete(event.logId) }
            is GoalDetailEvent.ToggleStar -> viewModelScope.launch { logRepo.setStarred(event.logId, event.starred) }
        }
    }
}
