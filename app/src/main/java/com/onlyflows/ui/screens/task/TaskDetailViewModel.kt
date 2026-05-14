package com.onlyflows.ui.screens.task

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.debug.AppLog
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.RecurrenceRule
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.model.Attachment
import com.onlyflows.domain.model.Comment
import com.onlyflows.domain.model.Reminder
import com.onlyflows.domain.model.TimeEntry
import com.onlyflows.domain.repository.AttachmentRepository
import com.onlyflows.domain.repository.CommentRepository
import com.onlyflows.domain.repository.GoalRepository
import com.onlyflows.domain.repository.ProjectRepository
import com.onlyflows.domain.repository.ReminderRepository
import com.onlyflows.domain.repository.TagRepository
import com.onlyflows.domain.repository.TaskRepository
import com.onlyflows.domain.repository.TimeEntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class TimerState(
    val activeEntry: TimeEntry? = null,          // non-null = timer running
    val totalCompletedSeconds: Long = 0L,        // sum of finished entries
    val displaySeconds: Long = 0L,               // totalCompleted + current elapsed, ticks every 1s
)

data class TaskDetailUiState(
    val task: Task?                   = null,
    val tags: List<Tag>               = emptyList(),
    val projectName: String?          = null,
    val blockingTask: Task?           = null,        // task this one depends on
    val dependingTasks: List<Task>    = emptyList(), // tasks that depend on this one
    val allTasks: List<Task>          = emptyList(), // for dependency picker
    val showDatePicker: Boolean       = false,
    val showTagsPicker: Boolean       = false,
    val showWhenPicker: Boolean       = false,
    val showPriorityPicker: Boolean   = false,
    val showStartDatePicker: Boolean  = false,
    val showRecurrencePicker: Boolean = false,
    val showReminderPicker: Boolean           = false,
    val showDependencyPicker: Boolean         = false,
    val checklistDatePickerItemId: String?    = null,
    val isLoading: Boolean                    = true,
    val convertedProjectId: String?           = null,
    val comments: List<Comment>               = emptyList(),
    val reminders: List<Reminder>             = emptyList(),
    val attachments: List<Attachment>         = emptyList(),
    val showSmartMove: Boolean                = false,
    val projects: List<Project>               = emptyList(),
    val showGoalPicker: Boolean               = false,
    val showAddGoal: Boolean                  = false,
    val allGoals: List<Goal>                  = emptyList(),
)

sealed interface TaskDetailEvent {
    data class ToggleChecklistItem(val itemId: String, val current: Boolean) : TaskDetailEvent
    data class AddChecklistItem(val title: String)                           : TaskDetailEvent
    data class UpdateTitle(val title: String)                                : TaskDetailEvent
    data class UpdateNote(val note: String)                                  : TaskDetailEvent
    data class SetDueDate(val date: LocalDate?, val dueTime: LocalTime? = null, val endTime: LocalTime? = null) : TaskDetailEvent
    data class SetScheduledGroup(val group: TaskGroup?)                      : TaskDetailEvent
    data class ToggleTag(val tagId: String)                                  : TaskDetailEvent
    data class CreateTag(val name: String)                                   : TaskDetailEvent
    data class DeleteTag(val tagId: String)                                  : TaskDetailEvent
    data object ToggleStar                                                   : TaskDetailEvent
    data object ToggleComplete                                               : TaskDetailEvent
    data object ShowDatePicker                                               : TaskDetailEvent
    data object HideDatePicker                                               : TaskDetailEvent
    data object ShowTagsPicker                                               : TaskDetailEvent
    data object HideTagsPicker                                               : TaskDetailEvent
    data object ShowWhenPicker                                               : TaskDetailEvent
    data object HideWhenPicker                                               : TaskDetailEvent
    data class SetPriority(val priority: TaskPriority)                       : TaskDetailEvent
    data object ShowPriorityPicker                                           : TaskDetailEvent
    data object HidePriorityPicker                                           : TaskDetailEvent
    data object Delete                                                       : TaskDetailEvent
    data class SetStartDate(val date: LocalDate?)                            : TaskDetailEvent
    data class SetRecurrence(val rule: RecurrenceRule?)                      : TaskDetailEvent
    data class AddReminder(val at: LocalDateTime, val locationId: String? = null) : TaskDetailEvent
    data class DeleteReminder(val id: String)                               : TaskDetailEvent
    data object ShowStartDatePicker                                          : TaskDetailEvent
    data object HideStartDatePicker                                          : TaskDetailEvent
    data object ShowRecurrencePicker                                         : TaskDetailEvent
    data object HideRecurrencePicker                                         : TaskDetailEvent
    data object ShowReminderPicker                                              : TaskDetailEvent
    data object HideReminderPicker                                              : TaskDetailEvent
    data class ShowChecklistDatePicker(val itemId: String)                      : TaskDetailEvent
    data object HideChecklistDatePicker                                         : TaskDetailEvent
    data class SetChecklistItemDates(
        val itemId: String,
        val startDate: LocalDate?,
        val dueDate: LocalDate?,
    ) : TaskDetailEvent
    data class SetDependsOn(val taskId: String?)                                : TaskDetailEvent
    data object ShowDependencyPicker                                            : TaskDetailEvent
    data object HideDependencyPicker                                            : TaskDetailEvent
    data object ShowSmartMove                                                   : TaskDetailEvent
    data object HideSmartMove                                                   : TaskDetailEvent
    data class  MoveTask(
        val scheduledGroup: TaskGroup?,
        val projectId: String?,
        val sectionId: String?,
    ) : TaskDetailEvent
    data class SetSection(val sectionId: String?)                               : TaskDetailEvent
    data object ConvertToProject                                                : TaskDetailEvent
    data object StartTimer                                                      : TaskDetailEvent
    data object PauseTimer                                                      : TaskDetailEvent
    data object StopTimer                                                       : TaskDetailEvent
    data class AddComment(val content: String)                                  : TaskDetailEvent
    data class EditComment(val id: String, val content: String)                 : TaskDetailEvent
    data class DeleteComment(val id: String)                                    : TaskDetailEvent
    data class AddAttachment(
        val uri: String,
        val fileName: String,
        val mimeType: String,
        val fileSize: Long,
    ) : TaskDetailEvent
    data class DeleteAttachment(val id: String)                                 : TaskDetailEvent
    data object ShowGoalPicker                                                  : TaskDetailEvent
    data object HideGoalPicker                                                  : TaskDetailEvent
    data class SetGoals(val goalIds: List<String>)                              : TaskDetailEvent
    data object ShowAddGoal                                                     : TaskDetailEvent
    data object HideAddGoal                                                     : TaskDetailEvent
    data class CreateAndAttachGoal(
        val title: String,
        val frequency: com.onlyflows.domain.model.GoalFrequency,
        val targetCount: Int,
        val colorHex: String,
        val emoji: String,
        val deadline: java.time.LocalDate?,
        val tags: List<String>,
    ) : TaskDetailEvent
}

class TaskDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val container          = (app as OnlyFlowsApp).container
    private val taskRepo: TaskRepository                              = container.taskRepository
    private val projectRepo: ProjectRepository                        = container.projectRepository
    private val tagRepo: TagRepository                                = container.tagRepository
    private val scheduler: com.onlyflows.data.notifications.ReminderScheduler = container.reminderScheduler
    private val timeRepo: TimeEntryRepository                         = container.timeEntryRepository
    private val commentRepo: CommentRepository                        = container.commentRepository
    private val reminderRepo: ReminderRepository                      = container.reminderRepository
    private val attachmentRepo: AttachmentRepository                  = container.attachmentRepository
    private val locationRepo                                          = container.savedLocationRepository
    private val geofenceManager                                       = container.geofenceManager
    private val goalRepo: GoalRepository                              = container.goalRepository

    private val _uiState    = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    init {
        // Ticker: updates displaySeconds every second when timer is running
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_timerState.value.activeEntry == null) continue
                _timerState.update { state ->
                    val elapsed = state.activeEntry?.startedAt?.let {
                        ChronoUnit.SECONDS.between(it, LocalDateTime.now())
                    } ?: 0L
                    state.copy(displaySeconds = state.totalCompletedSeconds + elapsed)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadTask(taskId: String) {
        AppLog.d("TaskDetailVM", "loadTask: id=$taskId")
        viewModelScope.launch {
            combine(
                taskRepo.observeById(taskId).filterNotNull(),
                tagRepo.observeAll(),
                taskRepo.observeDependents(taskId),
                taskRepo.observeAll(),
            ) { task, tags, dependingTasks, allTasks ->
                object {
                    val task = task
                    val tags = tags
                    val dependingTasks = dependingTasks
                    val allTasks = allTasks
                }
            }
            .flatMapLatest { snapshot ->
                val projectFlow = snapshot.task.projectId?.let { pid ->
                    projectRepo.observeById(pid).map { it?.title }
                } ?: flowOf(null)
                val blockingFlow = snapshot.task.dependsOnTaskId?.let { bid ->
                    taskRepo.observeById(bid)
                } ?: flowOf(null)
                val commentsFlow = commentRepo.observeByTask(taskId)
                combine(projectFlow, blockingFlow, commentsFlow) { projectName, blockingTask, comments ->
                    TaskDetailUiState(
                        task           = snapshot.task,
                        tags           = snapshot.tags,
                        projectName    = projectName,
                        blockingTask   = blockingTask,
                        dependingTasks = snapshot.dependingTasks,
                        allTasks       = snapshot.allTasks,
                        comments       = comments,
                        isLoading      = false,
                        // preserve picker visibility
                        showDatePicker       = _uiState.value.showDatePicker,
                        showTagsPicker       = _uiState.value.showTagsPicker,
                        showWhenPicker       = _uiState.value.showWhenPicker,
                        showPriorityPicker   = _uiState.value.showPriorityPicker,
                        showStartDatePicker  = _uiState.value.showStartDatePicker,
                        showRecurrencePicker = _uiState.value.showRecurrencePicker,
                        showReminderPicker   = _uiState.value.showReminderPicker,
                        showDependencyPicker = _uiState.value.showDependencyPicker,
                        showSmartMove        = _uiState.value.showSmartMove,
                    )
                }
            }
            .collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        reminders      = current.reminders,
                        attachments    = current.attachments,
                        allGoals       = current.allGoals,
                        projects       = current.projects,
                        showGoalPicker = current.showGoalPicker,
                        showAddGoal    = current.showAddGoal,
                    )
                }
            }
        }
    }

    fun loadComments(taskId: String) {
        // Comments are now observed inside loadTask's combine for atomic state updates.
    }

    fun loadReminders(taskId: String) {
        viewModelScope.launch {
            reminderRepo.observeByTask(taskId).collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
    }

    fun loadAttachments(taskId: String) {
        viewModelScope.launch {
            attachmentRepo.observeByTask(taskId).collect { attachments ->
                _uiState.update { it.copy(attachments = attachments) }
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            projectRepo.observeAllProjectsAndLists().collect { projects ->
                _uiState.update { it.copy(projects = projects) }
            }
        }
    }

    fun loadGoals() {
        viewModelScope.launch {
            goalRepo.observeActive().collect { goals ->
                _uiState.update { it.copy(allGoals = goals) }
            }
        }
    }

    fun loadTimer(taskId: String) {
        viewModelScope.launch {
            timeRepo.observeByTask(taskId).collect { entries ->
                val active    = entries.firstOrNull { it.endedAt == null }
                val completed = entries.filter { it.endedAt != null }
                val totalSecs = completed.sumOf { ChronoUnit.SECONDS.between(it.startedAt, it.endedAt!!) }
                val elapsed   = active?.startedAt?.let { ChronoUnit.SECONDS.between(it, LocalDateTime.now()) } ?: 0L
                _timerState.value = TimerState(
                    activeEntry            = active,
                    totalCompletedSeconds  = totalSecs,
                    displaySeconds         = totalSecs + elapsed,
                )
            }
        }
    }

    fun onEvent(event: TaskDetailEvent) {
        val task = _uiState.value.task ?: return
        when (event) {
            is TaskDetailEvent.ToggleChecklistItem -> viewModelScope.launch {
                taskRepo.toggleChecklistItem(event.itemId, !event.current)
            }
            is TaskDetailEvent.AddChecklistItem -> viewModelScope.launch {
                if (event.title.isBlank()) return@launch
                val item = ChecklistItem(
                    id        = "ci_${System.currentTimeMillis()}",
                    taskId    = task.id,
                    title     = event.title,
                    sortOrder = task.checklistItems.size,
                )
                taskRepo.addChecklistItem(item)
            }
            is TaskDetailEvent.UpdateTitle -> viewModelScope.launch {
                AppLog.d("TaskDetailVM", "UpdateTitle: \"${task.title}\" → \"${event.title}\" id=${task.id}")
                taskRepo.update(task.copy(title = event.title, updatedAt = LocalDateTime.now()))
            }
            is TaskDetailEvent.UpdateNote -> viewModelScope.launch {
                AppLog.d("TaskDetailVM", "UpdateNote: id=${task.id} len=${event.note.length}")
                taskRepo.update(task.copy(note = event.note, updatedAt = LocalDateTime.now()))
            }
            is TaskDetailEvent.SetDueDate -> {
                AppLog.d("TaskDetailVM", "SetDueDate: \"${task.title}\" date=${event.date} time=${event.dueTime} prevDue=${task.dueDate} group=${task.scheduledGroup}")
                viewModelScope.launch {
                    var updated = task.copy(
                        dueDate   = event.date,
                        dueTime   = event.dueTime,
                        endTime   = event.endTime,
                        updatedAt = LocalDateTime.now(),
                    )
                    // Clearing the due date on a floating task (no project, no group) would drop
                    // it into Inbox. Assign a default group to keep it accessible.
                    if (event.date == null && task.startDate == null
                        && task.projectId == null && task.scheduledGroup == null) {
                        updated = updated.copy(
                            scheduledGroup = if (task.priority != TaskPriority.NONE) TaskGroup.ANYTIME else TaskGroup.SOMEDAY
                        )
                        AppLog.d("TaskDetailVM", "SetDueDate null→floating: assigned group=${updated.scheduledGroup}")
                    }
                    taskRepo.update(updated)
                }
                _uiState.update { it.copy(showDatePicker = false) }
            }
            is TaskDetailEvent.SetScheduledGroup -> {
                AppLog.d("TaskDetailVM", "SetScheduledGroup: \"${task.title}\" ${task.scheduledGroup} → ${event.group}")
                viewModelScope.launch {
                    taskRepo.update(task.copy(scheduledGroup = event.group, updatedAt = LocalDateTime.now()))
                }
                _uiState.update { it.copy(showWhenPicker = false) }
            }
            is TaskDetailEvent.ToggleTag -> viewModelScope.launch {
                val tagIds = task.tagIds.toMutableList()
                if (event.tagId in tagIds) tagIds.remove(event.tagId) else tagIds.add(event.tagId)
                taskRepo.update(task.copy(tagIds = tagIds, updatedAt = LocalDateTime.now()))
            }
            is TaskDetailEvent.CreateTag -> viewModelScope.launch {
                val newTag = com.onlyflows.domain.model.Tag(
                    id   = "tag_${System.currentTimeMillis()}",
                    name = event.name,
                )
                tagRepo.insert(newTag)
            }
            is TaskDetailEvent.DeleteTag -> viewModelScope.launch {
                tagRepo.delete(event.tagId)
            }
            TaskDetailEvent.ToggleStar     -> viewModelScope.launch {
                AppLog.d("TaskDetailVM", "${if (!task.isStarred) "star" else "unstar"}: \"${task.title}\"")
                taskRepo.setStarred(task.id, !task.isStarred)
            }
            TaskDetailEvent.ToggleComplete -> viewModelScope.launch {
                val completing = !task.isCompleted
                AppLog.d("TaskDetailVM", "toggleComplete: \"${task.title}\" ${task.isCompleted} → $completing recurrence=${task.recurrence != null}")
                if (completing) {
                    scheduler.cancel(task.id)
                    reminderRepo.deleteByTask(task.id)
                    taskRepo.update(task.copy(hasReminders = false, updatedAt = LocalDateTime.now()))
                }
                val endCountReached = completing
                    && task.recurrence?.endCount != null
                    && (task.recurrenceDoneCount + 1) >= task.recurrence.endCount
                if (completing && task.recurrence != null && !endCountReached) {
                    taskRepo.completeRecurring(task.id)
                } else {
                    taskRepo.setCompleted(task.id, completing)
                }
                // Auto-update linked goal progress
                task.goalIds.forEach { goalId ->
                    val goal = _uiState.value.allGoals.find { it.id == goalId } ?: return@forEach
                    val next = if (completing)
                        (goal.currentCount + 1).coerceAtMost(goal.targetCount)
                    else
                        (goal.currentCount - 1).coerceAtLeast(0)
                    goalRepo.setProgress(goalId, next)
                    if (next >= goal.targetCount) goalRepo.setCompleted(goalId, true)
                    else if (!completing) goalRepo.setCompleted(goalId, false)
                }
            }
            TaskDetailEvent.Delete -> viewModelScope.launch {
                AppLog.d("TaskDetailVM", "delete: \"${task.title}\" id=${task.id}")
                timeRepo.getActiveEntry(task.id)?.let { timeRepo.stopEntry(it.id) }
                taskRepo.softDelete(task.id)
            }
            TaskDetailEvent.ShowDatePicker -> _uiState.update { it.copy(showDatePicker = true) }
            TaskDetailEvent.HideDatePicker -> _uiState.update { it.copy(showDatePicker = false) }
            TaskDetailEvent.ShowTagsPicker -> _uiState.update { it.copy(showTagsPicker = true) }
            TaskDetailEvent.HideTagsPicker -> _uiState.update { it.copy(showTagsPicker = false) }
            TaskDetailEvent.ShowWhenPicker -> _uiState.update { it.copy(showWhenPicker = true) }
            TaskDetailEvent.HideWhenPicker -> _uiState.update { it.copy(showWhenPicker = false) }
            is TaskDetailEvent.SetSection -> viewModelScope.launch {
                taskRepo.update(task.copy(sectionId = event.sectionId, updatedAt = LocalDateTime.now()))
            }
            is TaskDetailEvent.SetPriority -> viewModelScope.launch {
                AppLog.d("TaskDetailVM", "SetPriority: \"${task.title}\" ${task.priority} → ${event.priority}")
                taskRepo.update(task.copy(priority = event.priority, updatedAt = LocalDateTime.now()))
                _uiState.update { it.copy(showPriorityPicker = false) }
            }
            TaskDetailEvent.ShowPriorityPicker -> _uiState.update { it.copy(showPriorityPicker = true) }
            TaskDetailEvent.HidePriorityPicker -> _uiState.update { it.copy(showPriorityPicker = false) }
            is TaskDetailEvent.SetStartDate -> {
                AppLog.d("TaskDetailVM", "SetStartDate: \"${task.title}\" ${task.startDate} → ${event.date}")
                viewModelScope.launch {
                    var updated = task.copy(startDate = event.date, updatedAt = LocalDateTime.now())
                    if (event.date == null && task.dueDate == null
                        && task.projectId == null && task.scheduledGroup == null) {
                        updated = updated.copy(
                            scheduledGroup = if (task.priority != TaskPriority.NONE) TaskGroup.ANYTIME else TaskGroup.SOMEDAY
                        )
                        AppLog.d("TaskDetailVM", "SetStartDate null→floating: assigned group=${updated.scheduledGroup}")
                    }
                    taskRepo.update(updated)
                }
                _uiState.update { it.copy(showStartDatePicker = false) }
            }
            is TaskDetailEvent.SetRecurrence -> {
                AppLog.d("TaskDetailVM", "SetRecurrence: \"${task.title}\" rule=${event.rule}")
                viewModelScope.launch { taskRepo.update(task.copy(recurrence = event.rule, updatedAt = LocalDateTime.now())) }
                _uiState.update { it.copy(showRecurrencePicker = false) }
            }
            is TaskDetailEvent.AddReminder -> viewModelScope.launch {
                AppLog.d("TaskDetailVM", "AddReminder: at=${event.at} for \"${task.title}\" id=${task.id}")
                val reminder = Reminder(
                    id          = "rem_${System.currentTimeMillis()}",
                    taskId      = task.id,
                    scheduledAt = event.at,
                    locationId  = event.locationId,
                )
                reminderRepo.add(reminder)
                scheduler.schedule(task.id, task.title, task.note, event.at)
                // Register geofence if a location is attached
                event.locationId?.let { locId ->
                    locationRepo.getById(locId)?.let { loc ->
                        geofenceManager.register(loc.id, loc.latitude, loc.longitude, loc.radiusMeters)
                    }
                }
                taskRepo.update(task.copy(hasReminders = true, updatedAt = LocalDateTime.now()))
                _uiState.update { it.copy(showReminderPicker = false) }
            }
            is TaskDetailEvent.DeleteReminder -> viewModelScope.launch {
                // Unregister geofence if this was a location-based reminder
                _uiState.value.reminders.find { it.id == event.id }
                    ?.locationId?.let { geofenceManager.unregister(it) }
                reminderRepo.delete(event.id)
                val remaining = _uiState.value.reminders.filter { it.id != event.id }
                scheduler.cancel(task.id)
                remaining.forEach { scheduler.schedule(task.id, task.title, task.note, it.scheduledAt) }
                taskRepo.update(task.copy(hasReminders = remaining.isNotEmpty(), updatedAt = LocalDateTime.now()))
            }
            TaskDetailEvent.ShowStartDatePicker  -> _uiState.update { it.copy(showStartDatePicker = true) }
            TaskDetailEvent.HideStartDatePicker  -> _uiState.update { it.copy(showStartDatePicker = false) }
            TaskDetailEvent.ShowRecurrencePicker -> _uiState.update { it.copy(showRecurrencePicker = true) }
            TaskDetailEvent.HideRecurrencePicker -> _uiState.update { it.copy(showRecurrencePicker = false) }
            TaskDetailEvent.ShowReminderPicker   -> _uiState.update { it.copy(showReminderPicker = true) }
            TaskDetailEvent.HideReminderPicker   -> _uiState.update { it.copy(showReminderPicker = false) }
            is TaskDetailEvent.ShowChecklistDatePicker -> _uiState.update {
                it.copy(checklistDatePickerItemId = event.itemId)
            }
            TaskDetailEvent.HideChecklistDatePicker -> _uiState.update {
                it.copy(checklistDatePickerItemId = null)
            }
            is TaskDetailEvent.SetChecklistItemDates -> viewModelScope.launch {
                val task = _uiState.value.task ?: return@launch
                val item = task.checklistItems.find { it.id == event.itemId } ?: return@launch
                taskRepo.updateChecklistItem(
                    item.copy(startDate = event.startDate, dueDate = event.dueDate)
                )
                _uiState.update { it.copy(checklistDatePickerItemId = null) }
            }
            is TaskDetailEvent.SetDependsOn -> viewModelScope.launch {
                taskRepo.setDependsOn(task.id, event.taskId)
                _uiState.update { it.copy(showDependencyPicker = false) }
            }
            TaskDetailEvent.ShowDependencyPicker -> _uiState.update { it.copy(showDependencyPicker = true) }
            TaskDetailEvent.HideDependencyPicker -> _uiState.update { it.copy(showDependencyPicker = false) }
            TaskDetailEvent.StartTimer -> viewModelScope.launch {
                // Stop any running entry first (safety)
                timeRepo.getActiveEntry(task.id)?.let { timeRepo.stopEntry(it.id) }
                timeRepo.insert(TimeEntry(
                    id        = "te_${System.currentTimeMillis()}",
                    taskId    = task.id,
                    startedAt = LocalDateTime.now(),
                ))
            }
            TaskDetailEvent.PauseTimer -> viewModelScope.launch {
                timeRepo.getActiveEntry(task.id)?.let { timeRepo.stopEntry(it.id) }
            }
            TaskDetailEvent.StopTimer -> viewModelScope.launch {
                timeRepo.getActiveEntry(task.id)?.let { timeRepo.stopEntry(it.id) }
            }
            is TaskDetailEvent.AddComment -> viewModelScope.launch {
                if (event.content.isBlank()) return@launch
                commentRepo.add(Comment(
                    id        = "cmt_${System.currentTimeMillis()}",
                    taskId    = task.id,
                    content   = event.content.trim(),
                ))
            }
            is TaskDetailEvent.EditComment -> viewModelScope.launch {
                commentRepo.edit(event.id, event.content.trim())
            }
            is TaskDetailEvent.DeleteComment -> viewModelScope.launch {
                commentRepo.delete(event.id)
            }
            is TaskDetailEvent.AddAttachment -> viewModelScope.launch {
                val attachment = Attachment(
                    id       = "att_${System.currentTimeMillis()}",
                    taskId   = task.id,
                    fileName = event.fileName,
                    mimeType = event.mimeType,
                    uri      = event.uri,
                    fileSize = event.fileSize,
                )
                attachmentRepo.add(attachment)
                taskRepo.update(task.copy(hasAttachments = true, updatedAt = LocalDateTime.now()))
            }
            is TaskDetailEvent.DeleteAttachment -> viewModelScope.launch {
                attachmentRepo.delete(event.id)
                val remaining = _uiState.value.attachments.filter { it.id != event.id }
                if (remaining.isEmpty()) {
                    taskRepo.update(task.copy(hasAttachments = false, updatedAt = LocalDateTime.now()))
                }
            }
            TaskDetailEvent.ShowSmartMove -> _uiState.update { it.copy(showSmartMove = true) }
            TaskDetailEvent.HideSmartMove -> _uiState.update { it.copy(showSmartMove = false) }
            is TaskDetailEvent.MoveTask -> viewModelScope.launch {
                if (event.projectId != null) {
                    taskRepo.moveTask(task.id, event.projectId, event.sectionId)
                } else {
                    taskRepo.moveTask(task.id, null, null)
                    taskRepo.setScheduledGroup(task.id, event.scheduledGroup)
                }
                _uiState.update { it.copy(showSmartMove = false) }
            }
            TaskDetailEvent.ConvertToProject -> viewModelScope.launch {
                val newProject = Project(
                    id    = "proj_${UUID.randomUUID()}",
                    title = task.title,
                    note  = task.note ?: "",
                    dueDate = task.dueDate,
                )
                projectRepo.insert(newProject)
                task.checklistItems.forEachIndexed { index, item ->
                    val newTask = Task(
                        id        = UUID.randomUUID().toString(),
                        title     = item.title,
                        projectId = newProject.id,
                        sortOrder = index,
                        isCompleted = item.isCompleted,
                    )
                    taskRepo.insert(newTask)
                }
                taskRepo.delete(task.id)
                _uiState.update { it.copy(convertedProjectId = newProject.id) }
            }
            TaskDetailEvent.ShowGoalPicker -> _uiState.update { it.copy(showGoalPicker = true) }
            TaskDetailEvent.HideGoalPicker -> _uiState.update { it.copy(showGoalPicker = false) }
            is TaskDetailEvent.SetGoals -> viewModelScope.launch {
                taskRepo.setGoals(task.id, event.goalIds)
                _uiState.update { it.copy(showGoalPicker = false) }
            }
            TaskDetailEvent.ShowAddGoal -> _uiState.update { it.copy(showGoalPicker = false, showAddGoal = true) }
            TaskDetailEvent.HideAddGoal -> _uiState.update { it.copy(showAddGoal = false, showGoalPicker = true) }
            is TaskDetailEvent.CreateAndAttachGoal -> viewModelScope.launch {
                val newGoalId = UUID.randomUUID().toString()
                goalRepo.insert(
                    Goal(
                        id          = newGoalId,
                        title       = event.title,
                        frequency   = event.frequency,
                        targetCount = event.targetCount,
                        colorHex    = event.colorHex,
                        emoji       = event.emoji,
                        deadline    = event.deadline,
                        tags        = event.tags,
                    )
                )
                val newIds = task.goalIds + newGoalId
                taskRepo.setGoals(task.id, newIds)
                _uiState.update { it.copy(showAddGoal = false) }
            }
        }
    }
}
