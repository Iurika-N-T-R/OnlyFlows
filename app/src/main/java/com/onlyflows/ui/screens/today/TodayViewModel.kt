package com.onlyflows.ui.screens.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.calendar.CalendarRepository
import com.onlyflows.data.preferences.CalendarPreferences
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.data.repository.ProjectRepository
import com.onlyflows.domain.model.TimeEntry
import com.onlyflows.data.repository.TagRepository
import com.onlyflows.data.repository.TaskRepository
import com.onlyflows.data.repository.TimeEntryRepository
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

// Sort: due date asc (nulls last) → starred first → sortOrder → createdAt
private val taskComparator: Comparator<Task> = compareBy<Task>(
    { it.dueDate?.toEpochDay() ?: Long.MAX_VALUE },
    { if (it.isStarred) 0 else 1 },
    { it.sortOrder },
    { it.createdAt },
)

data class TodayUiState(
    val todayTasks: List<Task>              = emptyList(),
    val eveningTasks: List<Task>            = emptyList(),
    val overdueTasks: List<Task>            = emptyList(),
    val completedTasks: List<Task>          = emptyList(),
    val abandonedTasks: List<Task>          = emptyList(),
    val tags: List<Tag>                     = emptyList(),
    val allProjects: List<com.onlyflows.domain.model.Project> = emptyList(),
    val activeTagFilter: Set<String>        = emptySet(),
    val activePriorityFilter: TaskPriority? = null,
    val selectedTaskIds: Set<String>        = emptySet(),
    val movingToCompletedIds: Set<String>   = emptySet(),
    val pendingDeleteTask: Task?            = null,
    val showAddTask: Boolean                = false,
    val showMoveSheet: Boolean              = false,
    val showMoreSheet: Boolean              = false,
    val isLoading: Boolean                  = true,
    val agendaEvents: List<AgendaEvent>     = emptyList(),
    val hasCalendarPermission: Boolean      = false,
)

sealed interface TodayEvent {
    data class ToggleComplete(val taskId: String, val current: Boolean) : TodayEvent
    data class ToggleSelect(val taskId: String)                         : TodayEvent
    data object ClearSelection                                          : TodayEvent
    data object ShowAddTask                                             : TodayEvent
    data object HideAddTask                                             : TodayEvent
    data class AddTask(
        val id: String,
        val title: String,
        val note: String = "",
        val priority: TaskPriority = TaskPriority.NONE,
        val projectId: String? = null,
        val sectionId: String? = null,
        val checklistItems: List<String> = emptyList(),
        val group: TaskGroup? = null,
        val dueDate: java.time.LocalDate? = null,
        val dueTime: java.time.LocalTime? = null,
        val endTime: java.time.LocalTime? = null,
        val tagIds: List<String> = emptyList(),
    ) : TodayEvent
    data class DeleteSelected(val ids: Set<String>)                     : TodayEvent
    data class ToggleChecklistItem(val itemId: String, val completed: Boolean) : TodayEvent
    data class ToggleTagFilter(val tagId: String)                       : TodayEvent
    data class SetPriorityFilter(val priority: TaskPriority?)          : TodayEvent
    data object ShowMoveSheet                                           : TodayEvent
    data object HideMoveSheet                                           : TodayEvent
    data class MoveSelected(val projectId: String?, val sectionId: String?) : TodayEvent
    data object ShowMoreSheet                                           : TodayEvent
    data object HideMoreSheet                                           : TodayEvent
    data class StarSelected(val star: Boolean)                          : TodayEvent
    data class SetPrioritySelected(val priority: TaskPriority)          : TodayEvent
    data class AddTagSelected(val tagId: String)                        : TodayEvent
    data class ScheduleSelected(val date: LocalDate?)                   : TodayEvent
    data object StartTimerForSelected                                   : TodayEvent
    data object AbandonSelected                                         : TodayEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as OnlyFlowsApp).container
    private val taskRepo: TaskRepository           = container.taskRepository
    private val tagRepo: TagRepository             = container.tagRepository
    private val timeEntryRepo: TimeEntryRepository = container.timeEntryRepository
    private val projectRepo: ProjectRepository     = container.projectRepository
    private val calendarRepo: CalendarRepository   = container.calendarRepository
    private val calendarPrefs: CalendarPreferences = container.calendarPreferences

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private fun dateChangeFlow() = callbackFlow {
        trySend(LocalDate.now())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { trySend(LocalDate.now()) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        getApplication<android.app.Application>().registerReceiver(receiver, filter)
        awaitClose { getApplication<android.app.Application>().unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    init {
        AppLog.d("TodayVM", "init")
        viewModelScope.launch {
            dateChangeFlow().flatMapLatest {
                combine(
                    taskRepo.observeToday(),
                    taskRepo.observeOverdue(),
                    tagRepo.observeAll(),
                    projectRepo.observeAllProjectsAndLists(),
                    taskRepo.observeTodayCompleted(),
                ) { args -> args }
            }.distinctUntilChanged().collect { args ->
                @Suppress("UNCHECKED_CAST")
                val tasks     = args[0] as List<Task>
                @Suppress("UNCHECKED_CAST")
                val overdue   = args[1] as List<Task>
                @Suppress("UNCHECKED_CAST")
                val tags      = args[2] as List<Tag>
                @Suppress("UNCHECKED_CAST")
                val projects  = args[3] as List<com.onlyflows.domain.model.Project>
                @Suppress("UNCHECKED_CAST")
                val completed = args[4] as List<Task>
                val today = LocalDate.now()
                val todayFiltered = tasks.filter {
                    it.scheduledGroup == TaskGroup.TODAY
                    || (it.scheduledGroup == null && (
                        it.dueDate == today
                        || (it.startDate != null && !it.startDate.isAfter(today)
                            && (it.dueDate == null || !it.dueDate.isBefore(today)))
                    ))
                }.sortedWith(taskComparator)
                val eveningFiltered = tasks.filter { it.scheduledGroup == TaskGroup.THIS_EVENING }.sortedWith(taskComparator)
                val visibleTagIds: Set<String> = (todayFiltered + eveningFiltered + overdue)
                    .flatMapTo(mutableSetOf()) { it.tagIds }
                val derivedTags = tags.filter { it.id in visibleTagIds }
                AppLog.d("TodayVM", "data updated: today=${todayFiltered.size} overdue=${overdue.size} completed=${completed.size} raw=${tasks.size}")
                _uiState.update { state ->
                    state.copy(
                        todayTasks      = todayFiltered,
                        eveningTasks    = eveningFiltered,
                        overdueTasks    = overdue.sortedWith(taskComparator),
                        completedTasks  = completed,
                        tags            = derivedTags,
                        activeTagFilter = state.activeTagFilter.intersect(visibleTagIds),
                        allProjects     = projects,
                        isLoading       = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            dateChangeFlow().flatMapLatest {
                taskRepo.observeTodayAbandoned()
            }.distinctUntilChanged().collect { abandoned ->
                _uiState.update { it.copy(abandonedTasks = abandoned) }
            }
        }
        viewModelScope.launch {
            val hasPermission = calendarRepo.hasPermission()
            _uiState.update { it.copy(hasCalendarPermission = hasPermission) }
            combine(
                calendarPrefs.disabledCalendarIds,
                dateChangeFlow(),
            ) { disabledIds, _ -> disabledIds }.collectLatest { disabledIds ->
                if (_uiState.value.hasCalendarPermission) {
                    val events = withContext(Dispatchers.IO) {
                        calendarRepo.getEventsForDay(LocalDate.now(), disabledIds)
                    }
                    _uiState.update { it.copy(agendaEvents = events) }
                }
            }
        }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasCalendarPermission = true) }
        viewModelScope.launch {
            val disabledIds = calendarPrefs.disabledCalendarIds.first()
            val events = withContext(Dispatchers.IO) {
                calendarRepo.getEventsForDay(LocalDate.now(), disabledIds)
            }
            _uiState.update { it.copy(agendaEvents = events) }
        }
    }

    fun deleteTask(id: String) {
        val allTasks = _uiState.value.todayTasks + _uiState.value.eveningTasks + _uiState.value.overdueTasks
        val task = allTasks.find { it.id == id } ?: return
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

    fun onEvent(event: TodayEvent) {
        when (event) {
            is TodayEvent.ToggleComplete -> viewModelScope.launch {
                val completing = !event.current
                val task = (_uiState.value.todayTasks + _uiState.value.eveningTasks + _uiState.value.overdueTasks).find { it.id == event.taskId }
                AppLog.d("TodayVM", "toggleComplete: \"${task?.title ?: event.taskId}\" → completing=$completing recurrence=${task?.recurrence != null}")
                if (completing && task?.recurrence != null) {
                    taskRepo.completeRecurring(event.taskId)
                } else {
                    taskRepo.setCompleted(event.taskId, completing)
                }
            }
            is TodayEvent.ToggleSelect   -> toggleSelect(event.taskId)
            is TodayEvent.ClearSelection -> _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            is TodayEvent.ShowAddTask    -> _uiState.update { it.copy(showAddTask = true) }
            is TodayEvent.HideAddTask    -> _uiState.update { it.copy(showAddTask = false) }
            is TodayEvent.AddTask        -> { AppLog.d("TodayVM", "addTask: \"${event.title}\" project=${event.projectId}"); addTask(event.id, event.title, event.note, event.priority, event.projectId, event.sectionId, event.checklistItems, event.group, event.dueDate, event.dueTime, event.endTime, event.tagIds) }
            is TodayEvent.DeleteSelected -> { AppLog.d("TodayVM", "deleteSelected: ${event.ids.size} tasks"); deleteSelected(event.ids) }
            is TodayEvent.ToggleChecklistItem -> viewModelScope.launch {
                taskRepo.toggleChecklistItem(event.itemId, event.completed)
                val allTasks = _uiState.value.todayTasks + _uiState.value.eveningTasks
                val task = allTasks.find { t -> t.checklistItems.any { it.id == event.itemId } }
                if (task != null) {
                    val updatedItems = task.checklistItems.map {
                        if (it.id == event.itemId) it.copy(isCompleted = event.completed) else it
                    }
                    if (updatedItems.isNotEmpty() && updatedItems.all { it.isCompleted }) {
                        if (task.recurrence != null) taskRepo.completeRecurring(task.id)
                        else taskRepo.setCompleted(task.id, true)
                        _uiState.update { it.copy(movingToCompletedIds = it.movingToCompletedIds + task.id) }
                        delay(800)
                        _uiState.update { it.copy(movingToCompletedIds = it.movingToCompletedIds - task.id) }
                    }
                }
            }
            is TodayEvent.ToggleTagFilter -> _uiState.update { state ->
                val tags = state.activeTagFilter.toMutableSet()
                if (event.tagId in tags) tags.remove(event.tagId) else tags.add(event.tagId)
                state.copy(activeTagFilter = tags)
            }
            is TodayEvent.SetPriorityFilter -> _uiState.update {
                it.copy(activePriorityFilter = event.priority)
            }
            TodayEvent.ShowMoveSheet -> _uiState.update { it.copy(showMoveSheet = true) }
            TodayEvent.HideMoveSheet -> _uiState.update { it.copy(showMoveSheet = false) }
            is TodayEvent.MoveSelected -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskId ->
                    taskRepo.moveTask(taskId, event.projectId, event.sectionId)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoveSheet = false) }
            }
            TodayEvent.ShowMoreSheet -> _uiState.update { it.copy(showMoreSheet = true) }
            TodayEvent.HideMoreSheet -> _uiState.update { it.copy(showMoreSheet = false) }
            is TodayEvent.StarSelected -> viewModelScope.launch {
                bulkUpdateSelected { task -> task.copy(isStarred = event.star) }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoreSheet = false) }
            }
            is TodayEvent.SetPrioritySelected -> viewModelScope.launch {
                bulkUpdateSelected { task -> task.copy(priority = event.priority) }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoreSheet = false) }
            }
            is TodayEvent.AddTagSelected -> viewModelScope.launch {
                bulkUpdateSelected { task ->
                    if (event.tagId in task.tagIds) task
                    else task.copy(tagIds = task.tagIds + event.tagId)
                }
                _uiState.update { it.copy(showMoreSheet = false) }
            }
            is TodayEvent.ScheduleSelected -> viewModelScope.launch {
                bulkUpdateSelected { task -> task.copy(dueDate = event.date) }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoreSheet = false) }
            }
            TodayEvent.StartTimerForSelected -> viewModelScope.launch {
                val taskId = _uiState.value.selectedTaskIds.firstOrNull() ?: return@launch
                timeEntryRepo.getActiveEntry(taskId)?.let { timeEntryRepo.stopEntry(it.id) }
                timeEntryRepo.insert(
                    TimeEntry(
                        id        = "te_${System.currentTimeMillis()}",
                        taskId    = taskId,
                        startedAt = LocalDateTime.now(),
                    )
                )
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoreSheet = false) }
            }
            TodayEvent.AbandonSelected -> viewModelScope.launch {
                val ids      = _uiState.value.selectedTaskIds
                val allTasks = _uiState.value.todayTasks + _uiState.value.eveningTasks + _uiState.value.overdueTasks
                ids.forEach { id ->
                    val task = allTasks.find { it.id == id } ?: return@forEach
                    if (task.recurrence != null) taskRepo.abandonRecurring(id)
                    else taskRepo.abandonTask(id)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoreSheet = false) }
            }
        }
    }

    private suspend fun bulkUpdateSelected(transform: (Task) -> Task) {
        val ids      = _uiState.value.selectedTaskIds
        val allTasks = _uiState.value.todayTasks + _uiState.value.eveningTasks + _uiState.value.overdueTasks
        allTasks.filter { it.id in ids }.forEach { task ->
            taskRepo.update(transform(task))
        }
    }

    private fun toggleSelect(taskId: String) {
        _uiState.update { state ->
            val selected = state.selectedTaskIds.toMutableSet()
            if (taskId in selected) selected.remove(taskId) else selected.add(taskId)
            state.copy(selectedTaskIds = selected)
        }
    }

    private fun addTask(id: String, title: String, note: String = "", priority: TaskPriority = TaskPriority.NONE, projectId: String? = null, sectionId: String? = null, checklistItems: List<String> = emptyList(), group: TaskGroup? = null, dueDate: java.time.LocalDate? = null, dueTime: java.time.LocalTime? = null, endTime: java.time.LocalTime? = null, tagIds: List<String> = emptyList()) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newTask = Task(
                id             = id,
                title          = title,
                note           = note,
                priority       = priority,
                projectId      = projectId,
                sectionId      = sectionId,
                // The sheet carries the user's explicit choice and defaults to TODAY on this
                // screen, so it is the source of truth — no heuristic may override it.
                scheduledGroup = group,
                dueDate        = dueDate,
                dueTime        = dueTime,
                endTime        = endTime,
                tagIds         = tagIds,
                createdAt      = LocalDateTime.now(),
                updatedAt      = LocalDateTime.now(),
            )
            taskRepo.insert(newTask)
            checklistItems.forEachIndexed { index, itemTitle ->
                taskRepo.addChecklistItem(com.onlyflows.domain.model.ChecklistItem(
                    id        = "cli_${System.currentTimeMillis()}_$index",
                    taskId    = id,
                    title     = itemTitle,
                    sortOrder = index,
                ))
            }
            _uiState.update { it.copy(showAddTask = false) }
        }
    }

    private fun deleteSelected(ids: Set<String>) {
        viewModelScope.launch {
            ids.forEach { taskRepo.softDelete(it) }
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
        }
    }
}
