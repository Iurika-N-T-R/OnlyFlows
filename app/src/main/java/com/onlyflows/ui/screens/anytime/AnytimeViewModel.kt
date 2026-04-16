package com.onlyflows.ui.screens.anytime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.model.TimeEntry
import com.onlyflows.domain.repository.TimeEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnytimeUiState(
    val tasks: List<Task>            = emptyList(),
    val tags: List<Tag>              = emptyList(),
    val projects: List<Project>      = emptyList(),
    val selectedTaskIds: Set<String> = emptySet(),
    val activeTagFilter: Set<String> = emptySet(),
    val activePriorityFilter: TaskPriority? = null,
    val pendingDeleteTask: Task?     = null,
    val showMoveSheet: Boolean       = false,
    val showBulkMore: Boolean        = false,
    val isLoading: Boolean           = true,
)

sealed interface AnytimeEvent {
    data class ToggleComplete(val taskId: String, val current: Boolean) : AnytimeEvent
    data class ToggleTagFilter(val tagId: String)                       : AnytimeEvent
    data class SetPriorityFilter(val priority: TaskPriority?)          : AnytimeEvent
    data class ToggleSelect(val taskId: String)                        : AnytimeEvent
    data object ClearSelection                                         : AnytimeEvent
    data class DeleteSelected(val ids: Set<String>)                    : AnytimeEvent
    data object ShowMoveSheet                                          : AnytimeEvent
    data object HideMoveSheet                                          : AnytimeEvent
    data object ShowBulkMore                                           : AnytimeEvent
    data object HideBulkMore                                           : AnytimeEvent
    data class BulkStar(val starred: Boolean)                          : AnytimeEvent
    data class BulkSetPriority(val priority: TaskPriority)             : AnytimeEvent
    data class BulkAddTag(val tagId: String)                           : AnytimeEvent
    data class BulkSchedule(val date: java.time.LocalDate?)            : AnytimeEvent
    data object StartTimerForSelected                                   : AnytimeEvent
    data object AbandonSelected                                         : AnytimeEvent
    // scheduledGroup = null → Inbox, SOMEDAY → Someday, ANYTIME → Anytime, else project move
    data class MoveSelected(
        val scheduledGroup: TaskGroup?,
        val projectId: String?,
        val sectionId: String?,
    ) : AnytimeEvent
}

class AnytimeViewModel(app: Application) : AndroidViewModel(app) {

    private val container      = (app as OnlyFlowsApp).container
    private val taskRepo       = container.taskRepository
    private val tagRepo        = container.tagRepository
    private val projectRepo    = container.projectRepository
    private val timeEntryRepo: TimeEntryRepository = container.timeEntryRepository

    private val _uiState = MutableStateFlow(AnytimeUiState())
    val uiState: StateFlow<AnytimeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepo.observeAnytime(),
                tagRepo.observeAll(),
                projectRepo.observeAll(),
            ) { tasks, tags, projects -> Triple(tasks, tags, projects) }
            .collect { (tasks, tags, projects) ->
                _uiState.update { it.copy(tasks = tasks, tags = tags, projects = projects, isLoading = false) }
            }
        }
    }

    fun deleteTask(id: String) {
        val task = _uiState.value.tasks.find { it.id == id } ?: return
        _uiState.update { it.copy(pendingDeleteTask = task) }
    }

    fun confirmDelete() {
        val task = _uiState.value.pendingDeleteTask ?: return
        _uiState.update { it.copy(pendingDeleteTask = null) }
        viewModelScope.launch { taskRepo.softDelete(task.id) }
    }

    fun undoDelete() {
        _uiState.update { it.copy(pendingDeleteTask = null) }
    }

    fun onEvent(event: AnytimeEvent) {
        when (event) {
            is AnytimeEvent.ToggleComplete -> viewModelScope.launch {
                val completing = !event.current
                val task = _uiState.value.tasks.find { it.id == event.taskId }
                if (completing && task?.recurrence != null) taskRepo.completeRecurring(event.taskId)
                else taskRepo.setCompleted(event.taskId, completing)
            }
            is AnytimeEvent.ToggleTagFilter -> _uiState.update { state ->
                val tags = state.activeTagFilter.toMutableSet()
                if (event.tagId in tags) tags.remove(event.tagId) else tags.add(event.tagId)
                state.copy(activeTagFilter = tags)
            }
            is AnytimeEvent.SetPriorityFilter -> _uiState.update {
                it.copy(activePriorityFilter = event.priority)
            }
            is AnytimeEvent.ToggleSelect -> _uiState.update { state ->
                val selected = state.selectedTaskIds.toMutableSet()
                if (event.taskId in selected) selected.remove(event.taskId) else selected.add(event.taskId)
                state.copy(selectedTaskIds = selected)
            }
            AnytimeEvent.ClearSelection -> _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            is AnytimeEvent.DeleteSelected -> viewModelScope.launch {
                event.ids.forEach { taskRepo.softDelete(it) }
                _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            }
            AnytimeEvent.ShowMoveSheet -> _uiState.update { it.copy(showMoveSheet = true) }
            AnytimeEvent.HideMoveSheet -> _uiState.update { it.copy(showMoveSheet = false) }
            AnytimeEvent.ShowBulkMore  -> _uiState.update { it.copy(showBulkMore = true) }
            AnytimeEvent.HideBulkMore  -> _uiState.update { it.copy(showBulkMore = false) }
            is AnytimeEvent.BulkStar -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskRepo.setStarred(it, event.starred) }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is AnytimeEvent.BulkSetPriority -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(priority = event.priority, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is AnytimeEvent.BulkAddTag -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    if (event.tagId !in task.tagIds)
                        taskRepo.update(task.copy(tagIds = task.tagIds + event.tagId, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false) }
            }
            is AnytimeEvent.BulkSchedule -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(dueDate = event.date, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            AnytimeEvent.StartTimerForSelected -> viewModelScope.launch {
                val taskId = _uiState.value.selectedTaskIds.firstOrNull() ?: return@launch
                timeEntryRepo.getActiveEntry(taskId)?.let { timeEntryRepo.stopEntry(it.id) }
                timeEntryRepo.insert(
                    TimeEntry(
                        id        = "te_${System.currentTimeMillis()}",
                        taskId    = taskId,
                        startedAt = java.time.LocalDateTime.now(),
                    )
                )
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showBulkMore = false) }
            }
            AnytimeEvent.AbandonSelected -> viewModelScope.launch {
                val ids   = _uiState.value.selectedTaskIds
                val tasks = _uiState.value.tasks
                ids.forEach { id ->
                    val task = tasks.find { it.id == id } ?: return@forEach
                    if (task.recurrence != null) taskRepo.abandonRecurring(id)
                    else taskRepo.abandonTask(id)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showBulkMore = false) }
            }
            is AnytimeEvent.MoveSelected -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskId ->
                    when {
                        event.projectId != null -> taskRepo.moveTask(taskId, event.projectId, event.sectionId)
                        else -> {
                            // Set scheduledGroup (null = clear → back to Inbox auto-routing)
                            taskRepo.setScheduledGroup(taskId, event.scheduledGroup)
                        }
                    }
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoveSheet = false) }
            }
        }
    }
}
