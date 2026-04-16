package com.onlyflows.ui.screens.someday

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

data class SomedayUiState(
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

sealed interface SomedayEvent {
    data class ToggleComplete(val taskId: String, val current: Boolean) : SomedayEvent
    data class ToggleTagFilter(val tagId: String)                       : SomedayEvent
    data class SetPriorityFilter(val priority: TaskPriority?)          : SomedayEvent
    data class ToggleSelect(val taskId: String)                        : SomedayEvent
    data object ClearSelection                                         : SomedayEvent
    data class DeleteSelected(val ids: Set<String>)                    : SomedayEvent
    data object ShowMoveSheet                                          : SomedayEvent
    data object HideMoveSheet                                          : SomedayEvent
    data object ShowBulkMore                                           : SomedayEvent
    data object HideBulkMore                                           : SomedayEvent
    data class BulkStar(val starred: Boolean)                          : SomedayEvent
    data class BulkSetPriority(val priority: TaskPriority)             : SomedayEvent
    data class BulkAddTag(val tagId: String)                           : SomedayEvent
    data class BulkSchedule(val date: java.time.LocalDate?)            : SomedayEvent
    data object StartTimerForSelected                                   : SomedayEvent
    data object AbandonSelected                                         : SomedayEvent
    data class MoveSelected(
        val scheduledGroup: TaskGroup?,
        val projectId: String?,
        val sectionId: String?,
    ) : SomedayEvent
}

class SomedayViewModel(app: Application) : AndroidViewModel(app) {

    private val container      = (app as OnlyFlowsApp).container
    private val taskRepo       = container.taskRepository
    private val tagRepo        = container.tagRepository
    private val projectRepo    = container.projectRepository
    private val timeEntryRepo: TimeEntryRepository = container.timeEntryRepository

    private val _uiState = MutableStateFlow(SomedayUiState())
    val uiState: StateFlow<SomedayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepo.observeSomeday(),
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

    fun onEvent(event: SomedayEvent) {
        when (event) {
            is SomedayEvent.ToggleComplete -> viewModelScope.launch {
                val completing = !event.current
                val task = _uiState.value.tasks.find { it.id == event.taskId }
                if (completing && task?.recurrence != null) taskRepo.completeRecurring(event.taskId)
                else taskRepo.setCompleted(event.taskId, completing)
            }
            is SomedayEvent.ToggleTagFilter -> _uiState.update { state ->
                val tags = state.activeTagFilter.toMutableSet()
                if (event.tagId in tags) tags.remove(event.tagId) else tags.add(event.tagId)
                state.copy(activeTagFilter = tags)
            }
            is SomedayEvent.SetPriorityFilter -> _uiState.update {
                it.copy(activePriorityFilter = event.priority)
            }
            is SomedayEvent.ToggleSelect -> _uiState.update { state ->
                val selected = state.selectedTaskIds.toMutableSet()
                if (event.taskId in selected) selected.remove(event.taskId) else selected.add(event.taskId)
                state.copy(selectedTaskIds = selected)
            }
            SomedayEvent.ClearSelection -> _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            is SomedayEvent.DeleteSelected -> viewModelScope.launch {
                event.ids.forEach { taskRepo.softDelete(it) }
                _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            }
            SomedayEvent.ShowMoveSheet -> _uiState.update { it.copy(showMoveSheet = true) }
            SomedayEvent.HideMoveSheet -> _uiState.update { it.copy(showMoveSheet = false) }
            SomedayEvent.ShowBulkMore  -> _uiState.update { it.copy(showBulkMore = true) }
            SomedayEvent.HideBulkMore  -> _uiState.update { it.copy(showBulkMore = false) }
            is SomedayEvent.BulkStar -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskRepo.setStarred(it, event.starred) }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is SomedayEvent.BulkSetPriority -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(priority = event.priority, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is SomedayEvent.BulkAddTag -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    if (event.tagId !in task.tagIds)
                        taskRepo.update(task.copy(tagIds = task.tagIds + event.tagId, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false) }
            }
            is SomedayEvent.BulkSchedule -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(dueDate = event.date, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            SomedayEvent.StartTimerForSelected -> viewModelScope.launch {
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
            SomedayEvent.AbandonSelected -> viewModelScope.launch {
                val ids   = _uiState.value.selectedTaskIds
                val tasks = _uiState.value.tasks
                ids.forEach { id ->
                    val task = tasks.find { it.id == id } ?: return@forEach
                    if (task.recurrence != null) taskRepo.abandonRecurring(id)
                    else taskRepo.abandonTask(id)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showBulkMore = false) }
            }
            is SomedayEvent.MoveSelected -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskId ->
                    when {
                        event.projectId != null -> taskRepo.moveTask(taskId, event.projectId, event.sectionId)
                        else -> taskRepo.setScheduledGroup(taskId, event.scheduledGroup)
                    }
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoveSheet = false) }
            }
        }
    }
}
