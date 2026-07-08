package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.ChecklistItemDao
import com.onlyflows.data.local.dao.ChecklistSectionDao
import com.onlyflows.data.local.dao.CommentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.data.notifications.ReminderScheduler
import com.onlyflows.data.preferences.NotificationPreferences
import com.onlyflows.data.local.entity.CommentEntity
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.TaskWithChecklistItems
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.ChecklistSection
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.nextDueDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TaskRepository(
    private val taskDao: TaskDao,
    private val checklistDao: ChecklistItemDao,
    private val checklistSectionDao: ChecklistSectionDao,
    private val commentDao: CommentDao,
    private val goalRepo: GoalRepository,
    private val reminderRepo: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val notificationPreferences: NotificationPreferences,
    private val actionLog: ActionLogRepository,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
    private val onTaskMutated: suspend () -> Unit,
) {

    private suspend fun refreshWidgets() = onTaskMutated()

    private suspend fun syncWrite(id: String) {
        taskDao.getById(id)?.let { lanSyncWriter.writeTask(it) }
    }

    private suspend fun tombstone(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(
            DeletedEntityEntity(
                id         = "tomb_$id",
                entityType = "tasks",
                entityId   = id,
                deletedAt  = now,
            )
        )
        lanSyncWriter.writeTombstone("tasks", id, now)
    }

    /** Recurring tasks keep a tracking log as comments: each completion/abandon adds an auto
     *  comment "🔁 <status> — <date>", and the full comment history is copied onto the next
     *  occurrence so it persists across occurrences and syncs like any other comment. */
    private suspend fun addRecurrenceLogComment(taskId: String, status: String) {
        val now = LocalDateTime.now()
        val comment = CommentEntity(
            id        = "cmt_${System.currentTimeMillis()}_rec",
            taskId    = taskId,
            content   = "🔁 $status — ${LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}",
            createdAt = now,
            updatedAt = now,
        )
        commentDao.insert(comment)
        lanSyncWriter.writeComment(comment)
    }

    private suspend fun copyCommentsToNextOccurrence(fromTaskId: String, toTaskId: String) {
        commentDao.getByTask(fromTaskId).forEachIndexed { idx, c ->
            val copy = c.copy(id = "cmt_${System.currentTimeMillis()}_$idx", taskId = toTaskId)
            commentDao.insert(copy)
            lanSyncWriter.writeComment(copy)
        }
    }

    /** Recurring tasks: carry reminders onto the next occurrence, shifted by the due-date delta
     *  (a 09:00 reminder stays 09:00 relative to the new due date). Location reminders keep
     *  firing as-is — geofences are registered per location id, only the row moves. */
    private suspend fun moveRemindersToNextOccurrence(oldTaskId: String, oldDue: LocalDate?, next: Task) {
        val reminders = reminderRepo.getByTask(oldTaskId)
        if (reminders.isEmpty()) return
        val nextDue   = next.dueDate
        val shiftDays = if (oldDue != null && nextDue != null) ChronoUnit.DAYS.between(oldDue, nextDue) else null
        reminders.forEachIndexed { idx, r ->
            reminderScheduler.cancelByReminderId(r.id)
            val newAt = when {
                shiftDays != null -> r.scheduledAt.plusDays(shiftDays)
                nextDue != null   -> nextDue.atTime(r.scheduledAt.toLocalTime())
                else              -> r.scheduledAt
            }
            val moved = r.copy(
                id          = "rem_${System.currentTimeMillis()}_$idx",
                taskId      = next.id,
                scheduledAt = newAt,
                updatedAt   = LocalDateTime.now(),
            )
            reminderRepo.add(moved)
            if (moved.locationId == null) {
                reminderScheduler.schedule(
                    reminderId     = moved.id,
                    taskId         = next.id,
                    title          = next.title,
                    note           = next.note.takeIf { it.isNotBlank() },
                    at             = newAt,
                    recurrenceType = moved.recurrenceType,
                )
            }
        }
        reminderRepo.deleteByTask(oldTaskId)
    }

    /** Recurrence ended (or task done for good): cancel every pending alarm and drop the rows. */
    private suspend fun clearReminders(taskId: String) {
        reminderRepo.getByTask(taskId).forEach { reminderScheduler.cancelByReminderId(it.id) }
        reminderScheduler.cancelImminence("imm_$taskId")
        reminderScheduler.cancelImminence("start_$taskId")
        reminderRepo.deleteByTask(taskId)
    }

    /** Move the pre-due-date imminence + start alerts onto the next occurrence (same id scheme as AlarmReconciler). */
    private suspend fun rearmImminence(oldTaskId: String, next: Task) {
        reminderScheduler.cancelImminence("imm_$oldTaskId")
        reminderScheduler.cancelImminence("start_$oldTaskId")
        val startDate = next.startDate
        val startTime = next.startTime
        if (startDate != null && startTime != null) {
            reminderScheduler.scheduleImminence(
                "start_${next.id}", next.id, next.title, startDate.atTime(startTime),
                kind = com.onlyflows.data.notifications.ImminenceReceiver.KIND_START,
            )
        }
        val due  = next.dueDate ?: return
        val time = next.dueTime ?: return
        val lead = notificationPreferences.imminenceLeadMinutes.first()
        reminderScheduler.scheduleImminence("imm_${next.id}", next.id, next.title, due.atTime(time).minusMinutes(lead.toLong()))
    }

    /** Auto-advance goals linked to a task. Lives here (not in a ViewModel) so every completion
     *  path counts: list screens, task detail, widget, notification action, checklist auto-complete
     *  and recurring occurrences. */
    private suspend fun applyGoalProgress(taskId: String, completing: Boolean) {
        val goalIds = taskDao.getById(taskId)?.goalIds ?: return
        goalIds.forEach { goalId ->
            val stored = goalRepo.getById(goalId) ?: return@forEach
            if (stored.deletedAt != null) return@forEach
            val goal = goalRepo.rolloverIfExpired(stored)
            val next = if (completing) (goal.currentCount + 1).coerceAtMost(goal.targetCount)
                       else (goal.currentCount - 1).coerceAtLeast(0)
            goalRepo.setProgress(goalId, next)
            if (next >= goal.targetCount) goalRepo.setCompleted(goalId, true)
            else if (!completing) goalRepo.setCompleted(goalId, false)
        }
    }

    private suspend fun log(type: ActionType, taskId: String, label: String, extra: String? = null) {
        actionLog.insert(
            ActionLog(
                id         = "al_${System.currentTimeMillis()}_${type.name}",
                actionType = type,
                entityId   = taskId,
                entityType = "task",
                label      = label,
                extra      = extra,
                occurredAt = LocalDateTime.now(),
            )
        )
    }

    fun observeAll(): Flow<List<Task>> =
        taskDao.observeAllWithItems().map { list -> list.map { it.toDomain() } }

    fun observeUpcoming(): Flow<List<Task>> =
        taskDao.observeUpcomingWithItems().map { list -> list.map { it.toDomain() } }

    fun observeOverdue(): Flow<List<Task>> =
        taskDao.observeOverdueWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    fun observeInbox(): Flow<List<Task>> =
        taskDao.observeInboxWithItems().map { list -> list.map { it.toDomain() } }

    fun observeByGroup(group: com.onlyflows.domain.model.TaskGroup): Flow<List<Task>> =
        taskDao.observeByGroupWithItems(group.name).map { list -> list.map { it.toDomain() } }

    fun observeToday(): Flow<List<Task>> =
        taskDao.observeTodayWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    fun observeTodayCompleted(): Flow<List<Task>> =
        taskDao.observeTodayCompletedWithItems(LocalDate.now().toString(), LocalDate.now().atStartOfDay().toString())
            .map { list -> list.map { it.toDomain() } }

    fun observeTodayAbandoned(): Flow<List<Task>> =
        taskDao.observeTodayAbandonedWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    fun observeByProject(projectId: String): Flow<List<Task>> =
        taskDao.observeByProjectWithItems(projectId).map { list -> list.map { it.toDomain() } }

    fun observeArchivedByProject(projectId: String): Flow<List<Task>> =
        taskDao.observeArchivedByProjectWithItems(projectId).map { list -> list.map { it.toDomain() } }

    fun observeAllArchived(): Flow<List<Task>> =
        taskDao.observeAllArchivedWithItems().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Task?> =
        taskDao.observeByIdWithItems(id).map { it?.toDomain() }

    fun observeDependents(taskId: String): Flow<List<Task>> =
        taskDao.observeDependentsWithItems(taskId).map { list -> list.map { it.toDomain() } }

    suspend fun setDependsOn(id: String, dependsOnId: String?) {
        taskDao.setDependsOn(id, dependsOnId, LocalDateTime.now().toString())
        syncWrite(id)
    }

    suspend fun setGoals(id: String, goalIds: List<String>) {
        taskDao.setGoals(id, goalIds.joinToString(","), LocalDateTime.now().toString())
        syncWrite(id)
    }

    fun observeAnytime(): Flow<List<Task>> =
        taskDao.observeAnytimeWithItems().map { list -> list.map { it.toDomain() } }

    fun observeSomeday(): Flow<List<Task>> =
        taskDao.observeSomedayWithItems().map { list -> list.map { it.toDomain() } }

    private fun startOfDay() = LocalDate.now().atStartOfDay().toString()

    fun observeInboxCompleted(): Flow<List<Task>> =
        taskDao.observeInboxCompletedWithItems(startOfDay()).map { list -> list.map { it.toDomain() } }

    fun observeAnytimeCompleted(): Flow<List<Task>> =
        taskDao.observeAnytimeCompletedWithItems(startOfDay()).map { list -> list.map { it.toDomain() } }

    fun observeSomedayCompleted(): Flow<List<Task>> =
        taskDao.observeSomedayCompletedWithItems(startOfDay()).map { list -> list.map { it.toDomain() } }

    suspend fun setScheduledGroup(id: String, group: com.onlyflows.domain.model.TaskGroup?) {
        taskDao.setScheduledGroup(id, group?.name, LocalDateTime.now().toString())
        syncWrite(id)
        refreshWidgets()
    }

    fun search(query: String): Flow<List<Task>> =
        if (query.isBlank()) flowOf(emptyList())
        else taskDao.searchWithItems(query).map { list -> list.map { it.toDomain() } }

    fun searchByDate(date: java.time.LocalDate): Flow<List<Task>> =
        taskDao.searchByDateWithItems(date.toString()).map { list -> list.map { it.toDomain() } }

    fun searchByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<Task>> =
        taskDao.searchByDateRangeWithItems(start.toString(), end.toString()).map { list -> list.map { it.toDomain() } }

    suspend fun insert(task: Task) {
        taskDao.insert(task.toEntity())
        checklistDao.insertAll(task.checklistItems.map { it.toEntity() })
        lanSyncWriter.writeTask(task.toEntity())
        log(ActionType.TASK_CREATED, task.id, task.title)
        refreshWidgets()
    }

    suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
        lanSyncWriter.writeTask(task.toEntity())
        refreshWidgets()
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        val wasCompleted = taskDao.getById(id)?.isCompleted
        taskDao.setCompleted(id, completed, LocalDateTime.now().toString())
        // Guard against idempotent calls (e.g. bulk-complete on an already completed task)
        if (wasCompleted != null && wasCompleted != completed) applyGoalProgress(id, completed)
        val label = taskDao.getById(id)?.title ?: id
        syncWrite(id)
        log(if (completed) ActionType.TASK_COMPLETED else ActionType.TASK_UNCOMPLETED, id, label)
        refreshWidgets()
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        taskDao.setArchived(id, archived, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        syncWrite(id)
        log(if (archived) ActionType.TASK_ARCHIVED else ActionType.TASK_RESTORED, id, label)
        refreshWidgets()
    }

    /** Complete respecting recurrence — for callers without a domain Task (widget, notifications). */
    suspend fun complete(id: String) {
        if (taskDao.getById(id)?.recurrenceJson != null) completeRecurring(id)
        else setCompleted(id, true)
    }

    suspend fun completeRecurring(id: String) {
        val entity = taskDao.getById(id) ?: return
        val items  = checklistDao.getByTask(id)
        val task   = entity.toDomain(items)
        val rule   = task.recurrence ?: return

        // 1 — mark current occurrence as completed + archive → goes to Logbook, not Completed section
        taskDao.update(
            entity.copy(
                isCompleted          = true,
                isArchived           = true,
                recurrenceDoneCount  = entity.recurrenceDoneCount + 1,
                completedAt          = LocalDateTime.now(),
                updatedAt            = LocalDateTime.now(),
            )
        )
        applyGoalProgress(id, completing = true)

        // 2 — compute next due date (checked here, not in ViewModels, so endCount holds on
        //     every completion path: lists, widget, notifications, checklist auto-complete)
        val endCountReached = rule.endCount != null && (entity.recurrenceDoneCount + 1) >= rule.endCount
        val nextDue = if (endCountReached) null else nextDueDate(
            rule         = rule,
            completedOn  = LocalDate.now(),
            currentDue   = task.dueDate,
        )
        if (nextDue == null) {
            // recurrence ended — keep the tracking log on the final occurrence
            clearReminders(id)
            addRecurrenceLogComment(id, "Done")
            syncWrite(id)
            refreshWidgets()
            return
        }

        // 3 — insert fresh occurrence (reset completion + checklist)
        val nextTask = task.copy(
            id            = "task_${System.currentTimeMillis()}",
            isCompleted   = false,
            dueDate       = nextDue,
            recurrenceDoneCount = entity.recurrenceDoneCount + 1,   // carry running total onto live occurrence
            checklistItems = task.checklistItems.map { it.copy(isCompleted = false) },
            createdAt     = LocalDateTime.now(),
            updatedAt     = LocalDateTime.now(),
        )
        taskDao.insert(nextTask.toEntity())
        if (nextTask.checklistItems.isNotEmpty()) {
            checklistDao.insertAll(
                nextTask.checklistItems.mapIndexed { idx, item ->
                    item.copy(id = "cli_${System.currentTimeMillis()}_$idx", taskId = nextTask.id).toEntity()
                }
            )
        }
        // 4 — reminders + imminence alert follow the task onto the new occurrence
        moveRemindersToNextOccurrence(oldTaskId = id, oldDue = task.dueDate, next = nextTask)
        rearmImminence(oldTaskId = id, next = nextTask)
        // 5 — tracking log lives on the new occurrence (history + this completion)
        copyCommentsToNextOccurrence(fromTaskId = id, toTaskId = nextTask.id)
        addRecurrenceLogComment(nextTask.id, "Done")
        syncWrite(id)
        syncWrite(nextTask.id)
        refreshWidgets()
    }

    suspend fun abandonTask(id: String) {
        taskDao.setArchived(id, true, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        syncWrite(id)
        log(ActionType.TASK_ABANDONED, id, label)
        refreshWidgets()
    }

    suspend fun abandonRecurring(id: String) {
        val entity = taskDao.getById(id) ?: return
        val items  = checklistDao.getByTask(id)
        val task   = entity.toDomain(items)
        val rule   = task.recurrence ?: return

        // 1 — increment abandoned count, archive current occurrence
        taskDao.update(
            entity.copy(
                isArchived               = true,
                recurrenceAbandonedCount = entity.recurrenceAbandonedCount + 1,
                updatedAt                = LocalDateTime.now(),
            )
        )
        log(ActionType.TASK_ABANDONED, id, task.title)

        // 2 — compute next due date
        val nextDue = nextDueDate(
            rule        = rule,
            completedOn = LocalDate.now(),
            currentDue  = task.dueDate,
        )
        if (nextDue == null) {
            // recurrence ended — keep the tracking log on the final occurrence
            clearReminders(id)
            addRecurrenceLogComment(id, "Abandoned")
            syncWrite(id)
            refreshWidgets()
            return
        }

        // 3 — insert fresh occurrence (reset completion + checklist)
        val nextTask = task.copy(
            id             = "task_${System.currentTimeMillis()}",
            isCompleted    = false,
            isArchived     = false,
            dueDate        = nextDue,
            recurrenceAbandonedCount = entity.recurrenceAbandonedCount + 1,   // carry running total onto live occurrence
            checklistItems = task.checklistItems.map { it.copy(isCompleted = false) },
            createdAt      = LocalDateTime.now(),
            updatedAt      = LocalDateTime.now(),
        )
        taskDao.insert(nextTask.toEntity())
        lanSyncWriter.writeTask(nextTask.toEntity())
        if (nextTask.checklistItems.isNotEmpty()) {
            val newItems = nextTask.checklistItems.mapIndexed { idx, item ->
                item.copy(id = "cli_${System.currentTimeMillis()}_$idx", taskId = nextTask.id).toEntity()
            }
            checklistDao.insertAll(newItems)
            newItems.forEach { lanSyncWriter.writeChecklistItem(it) }
        }
        // 4 — reminders + imminence alert follow the task onto the new occurrence
        moveRemindersToNextOccurrence(oldTaskId = id, oldDue = task.dueDate, next = nextTask)
        rearmImminence(oldTaskId = id, next = nextTask)
        // 5 — tracking log lives on the new occurrence (history + this abandon)
        copyCommentsToNextOccurrence(fromTaskId = id, toTaskId = nextTask.id)
        addRecurrenceLogComment(nextTask.id, "Abandoned")
        syncWrite(id)
        refreshWidgets()
    }

    suspend fun setStarred(id: String, starred: Boolean) {
        taskDao.setStarred(id, starred, LocalDateTime.now().toString())
        syncWrite(id)
        refreshWidgets()
    }

    suspend fun moveTask(id: String, projectId: String?, sectionId: String?) {
        taskDao.moveTask(id, projectId, sectionId, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        syncWrite(id)
        log(ActionType.TASK_MOVED, id, label, extra = projectId ?: "inbox")
        refreshWidgets()
    }

    suspend fun delete(id: String) {
        tombstone(id)
        taskDao.delete(id)
        refreshWidgets()
    }

    suspend fun deleteAll(ids: List<String>) {
        ids.forEach { tombstone(it) }
        taskDao.deleteAll(ids)
        refreshWidgets()
    }

    suspend fun softDelete(id: String) {
        val label = taskDao.getById(id)?.title ?: id
        taskDao.softDelete(id, LocalDateTime.now().toString())
        syncWrite(id)
        log(ActionType.TASK_DELETED, id, label)
        refreshWidgets()
    }

    fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrashWithItems().map { list -> list.map { it.toDomain() } }

    suspend fun restoreFromTrash(id: String) {
        taskDao.restoreFromTrash(id, LocalDateTime.now().toString())
        syncWrite(id)
        val label = taskDao.getById(id)?.title ?: id
        log(ActionType.TASK_RESTORED, id, label)
        refreshWidgets()
    }

    suspend fun emptyTrash() {
        taskDao.getTrashIds().forEach { tombstone(it) }
        taskDao.emptyTrash()
        refreshWidgets()
    }

    suspend fun addChecklistItem(item: ChecklistItem) {
        checklistDao.insert(item.toEntity())
        lanSyncWriter.writeChecklistItem(item.toEntity())
    }

    suspend fun toggleChecklistItem(id: String, completed: Boolean) {
        checklistDao.setCompleted(id, completed)
        checklistDao.getById(id)?.let { lanSyncWriter.writeChecklistItem(it) }
        refreshWidgets()
    }

    suspend fun deleteChecklistItem(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "checklist_items", id, now))
        lanSyncWriter.writeTombstone("checklist_items", id, now)
        checklistDao.delete(id)
    }

    suspend fun updateChecklistItem(item: ChecklistItem) {
        checklistDao.update(item.toEntity())
        lanSyncWriter.writeChecklistItem(item.toEntity())
    }

    suspend fun getChecklistItemById(id: String): ChecklistItem? =
        checklistDao.getById(id)?.toDomain()

    suspend fun reorderChecklistItems(items: List<ChecklistItem>) {
        items.forEach { item ->
            val entity = item.toEntity()
            checklistDao.update(entity)
            lanSyncWriter.writeChecklistItem(entity)
        }
    }

    fun observeChecklistSections(taskId: String): Flow<List<ChecklistSection>> =
        checklistSectionDao.observeByTask(taskId).map { it.map { e -> e.toDomain() } }

    suspend fun addChecklistSection(section: ChecklistSection) {
        val entity = section.toEntity()
        checklistSectionDao.insert(entity)
        lanSyncWriter.writeChecklistSection(entity)
    }

    suspend fun renameChecklistSection(section: ChecklistSection) {
        val entity = section.toEntity()
        checklistSectionDao.update(entity)
        lanSyncWriter.writeChecklistSection(entity)
    }

    suspend fun deleteChecklistSection(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "checklist_sections", id, now))
        lanSyncWriter.writeTombstone("checklist_sections", id, now)
        checklistSectionDao.unassignItems(id)
        checklistSectionDao.delete(id)
    }

    suspend fun setChecklistItemSection(itemId: String, sectionId: String?) {
        checklistDao.getById(itemId)?.let { entity ->
            val updated = entity.copy(sectionId = sectionId)
            checklistDao.update(updated)
            lanSyncWriter.writeChecklistItem(updated)
        }
    }

    suspend fun reorderTasks(idsInOrder: List<String>) {
        idsInOrder.forEachIndexed { index, id ->
            taskDao.updateSortOrder(id, index)
        }
        idsInOrder.forEach { syncWrite(it) }
        refreshWidgets()
    }

    fun observeInboxCount(): Flow<Int>   = taskDao.observeInboxCount()
    fun observeTodayCount(): Flow<Int>   = taskDao.observeTodayCount()
    fun observeOverdueCount(): Flow<Int> = taskDao.observeOverdueCount(java.time.LocalDate.now().toString())
    fun observeAllCount(projectId: String): Flow<Int>       = taskDao.observeAllCount(projectId)
    fun observeCompletedCount(projectId: String): Flow<Int> = taskDao.observeCompletedCount(projectId)
    fun observeAllProjectProgress(): Flow<List<com.onlyflows.domain.model.ProjectTaskProgress>> =
        taskDao.observeAllProjectProgress().map { list ->
            list.map { com.onlyflows.domain.model.ProjectTaskProgress(it.projectId, it.total, it.completed) }
        }

    suspend fun getCompletedThisWeekCount(sevenDaysAgo: String): Int = taskDao.getCompletedThisWeekCount(sevenDaysAgo)
    suspend fun getUpcomingDeadlines(today: String): List<com.onlyflows.data.local.entity.TaskEntity> = taskDao.getUpcomingDeadlines(today)

    suspend fun getUpcomingTimed(today: String): List<com.onlyflows.data.local.entity.TaskEntity> = taskDao.getUpcomingTimed(today)

    fun observeActiveCount(): Flow<Int>        = taskDao.observeActiveCount()
    fun observeAllCompletedCount(): Flow<Int>  = taskDao.observeAllCompletedCount()
    fun observeAllArchivedCount(): Flow<Int>   = taskDao.observeAllArchivedCount()
}
