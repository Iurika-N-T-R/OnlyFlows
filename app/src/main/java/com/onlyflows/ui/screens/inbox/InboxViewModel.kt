package com.onlyflows.ui.screens.inbox

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
import com.onlyflows.data.repository.TimeEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val tasks: List<Task>            = emptyList(),
    val completedTasks: List<Task>   = emptyList(),
    val tags: List<Tag>              = emptyList(),
    val projects: List<Project>      = emptyList(),
    val selectedTaskIds: Set<String> = emptySet(),
    val pendingDeleteTask: Task?     = null,
    val showAddTask: Boolean         = false,
    val showMoveSheet: Boolean       = false,
    val showBulkMore: Boolean        = false,
    val isLoading: Boolean           = true,
)

sealed interface InboxEvent {
    data class ToggleComplete(val taskId: String, val current: Boolean) : InboxEvent
    data object ShowAddTask                                              : InboxEvent
    data object HideAddTask                                              : InboxEvent
    data class AddTask(
        val id: String,
        val title: String,
        val note: String = "",
        val priority: TaskPriority = TaskPriority.NONE,
        val projectId: String? = null,
        val sectionId: String? = null,
        val checklistItems: List<String> = emptyList(),
    ) : InboxEvent
    data class ToggleChecklistItem(val itemId: String, val completed: Boolean) : InboxEvent
    data class ToggleSelect(val taskId: String)                         : InboxEvent
    data object ClearSelection                                          : InboxEvent
    data class DeleteSelected(val ids: Set<String>)                     : InboxEvent
    data object ShowMoveSheet                                           : InboxEvent
    data object HideMoveSheet                                           : InboxEvent
    data object ShowBulkMore                                            : InboxEvent
    data object HideBulkMore                                            : InboxEvent
    data class BulkStar(val starred: Boolean)                           : InboxEvent
    data class BulkSetPriority(val priority: TaskPriority)              : InboxEvent
    data class BulkSchedule(val date: java.time.LocalDate?)             : InboxEvent
    data class MoveSelected(
        val scheduledGroup: TaskGroup?,
        val projectId: String?,
        val sectionId: String?,
    ) : InboxEvent
    data object StartTimerForSelected                                    : InboxEvent
    data object AbandonSelected                                          : InboxEvent
}

class InboxViewModel(app: Application) : AndroidViewModel(app) {

    private val container   = (app as OnlyFlowsApp).container
    private val taskRepo       = container.taskRepository
    private val tagRepo        = container.tagRepository
    private val projectRepo    = container.projectRepository
    private val timeEntryRepo: TimeEntryRepository = container.timeEntryRepository

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepo.observeInbox(),
                taskRepo.observeInboxCompleted(),
                tagRepo.observeAll(),
                projectRepo.observeAllProjectsAndLists(),
            ) { tasks, completed, tags, projects -> listOf(tasks, completed, tags, projects) }
            .distinctUntilChanged()
            .collect { (tasks, completed, tags, projects) ->
                @Suppress("UNCHECKED_CAST")
                _uiState.update { it.copy(
                    tasks = tasks as List<Task>, completedTasks = completed as List<Task>,
                    tags = tags as List<Tag>, projects = projects as List<Project>, isLoading = false,
                ) }
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

    fun onEvent(event: InboxEvent) {
        when (event) {
            is InboxEvent.ToggleComplete -> viewModelScope.launch {
                val completing = !event.current
                val task = _uiState.value.tasks.find { it.id == event.taskId }
                if (completing && task?.recurrence != null) {
                    taskRepo.completeRecurring(event.taskId)
                } else {
                    taskRepo.setCompleted(event.taskId, completing)
                }
            }
            InboxEvent.ShowAddTask -> _uiState.update { it.copy(showAddTask = true) }
            InboxEvent.HideAddTask -> _uiState.update { it.copy(showAddTask = false) }
            is InboxEvent.AddTask -> viewModelScope.launch {
                val task = Task(
                    id        = event.id,
                    title     = event.title,
                    note      = event.note,
                    priority  = event.priority,
                    projectId = event.projectId,
                    sectionId = event.sectionId,
                )
                taskRepo.insert(task)
                event.checklistItems.forEachIndexed { index, itemTitle ->
                    taskRepo.addChecklistItem(com.onlyflows.domain.model.ChecklistItem(
                        id        = "cli_${System.currentTimeMillis()}_$index",
                        taskId    = event.id,
                        title     = itemTitle,
                        sortOrder = index,
                    ))
                }
                _uiState.update { it.copy(showAddTask = false) }
            }
            is InboxEvent.ToggleChecklistItem -> viewModelScope.launch {
                taskRepo.toggleChecklistItem(event.itemId, event.completed)
            }
            is InboxEvent.ToggleSelect -> _uiState.update { state ->
                val selected = state.selectedTaskIds.toMutableSet()
                if (event.taskId in selected) selected.remove(event.taskId) else selected.add(event.taskId)
                state.copy(selectedTaskIds = selected)
            }
            InboxEvent.ClearSelection -> _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            is InboxEvent.DeleteSelected -> viewModelScope.launch {
                event.ids.forEach { taskRepo.softDelete(it) }
                _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            }
            InboxEvent.ShowMoveSheet -> _uiState.update { it.copy(showMoveSheet = true) }
            InboxEvent.HideMoveSheet -> _uiState.update { it.copy(showMoveSheet = false) }
            InboxEvent.ShowBulkMore  -> _uiState.update { it.copy(showBulkMore = true) }
            InboxEvent.HideBulkMore  -> _uiState.update { it.copy(showBulkMore = false) }
            is InboxEvent.BulkStar -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskRepo.setStarred(it, event.starred) }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is InboxEvent.BulkSetPriority -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(priority = event.priority, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is InboxEvent.BulkSchedule -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasks.filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(dueDate = event.date, updatedAt = java.time.LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is InboxEvent.MoveSelected -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskId ->
                    when {
                        event.projectId != null -> taskRepo.moveTask(taskId, event.projectId, event.sectionId)
                        else -> taskRepo.setScheduledGroup(taskId, event.scheduledGroup)
                    }
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoveSheet = false) }
            }
            InboxEvent.StartTimerForSelected -> viewModelScope.launch {
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
            InboxEvent.AbandonSelected -> viewModelScope.launch {
                val ids   = _uiState.value.selectedTaskIds
                val tasks = _uiState.value.tasks
                ids.forEach { id ->
                    val task = tasks.find { it.id == id } ?: return@forEach
                    if (task.recurrence != null) taskRepo.abandonRecurring(id)
                    else taskRepo.abandonTask(id)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showBulkMore = false) }
            }
        }
    }
}
