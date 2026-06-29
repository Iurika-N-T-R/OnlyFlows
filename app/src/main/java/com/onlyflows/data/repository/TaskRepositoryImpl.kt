package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.ChecklistItemDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.TaskWithChecklistItems
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.nextDueDate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TaskRepository(
    private val taskDao: TaskDao,
    private val checklistDao: ChecklistItemDao,
    private val actionLog: ActionLogRepository,
    private val syncDao: SyncDao,
    private val syncthingWriter: LanSyncWriter,
    private val onTaskMutated: suspend () -> Unit,
) {

    private suspend fun refreshWidgets() = onTaskMutated()

    private suspend fun syncWrite(id: String) {
        taskDao.getById(id)?.let { syncthingWriter.writeTask(it) }
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
        syncthingWriter.writeTombstone("tasks", id, now)
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
        taskDao.observeTodayCompletedWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

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

    suspend fun setScheduledGroup(id: String, group: com.onlyflows.domain.model.TaskGroup?) {
        taskDao.setScheduledGroup(id, group?.name, LocalDateTime.now().toString())
        syncWrite(id)
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
        syncthingWriter.writeTask(task.toEntity())
        log(ActionType.TASK_CREATED, task.id, task.title)
        refreshWidgets()
    }

    suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
        syncthingWriter.writeTask(task.toEntity())
        refreshWidgets()
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        taskDao.setCompleted(id, completed, LocalDateTime.now().toString())
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
                updatedAt            = LocalDateTime.now(),
            )
        )

        // 2 — compute next due date
        val nextDue = nextDueDate(
            rule         = rule,
            completedOn  = LocalDate.now(),
            currentDue   = task.dueDate,
        ) ?: return

        // 3 — insert fresh occurrence (reset completion + checklist)
        val nextTask = task.copy(
            id            = "task_${System.currentTimeMillis()}",
            isCompleted   = false,
            dueDate       = nextDue,
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
        syncWrite(id)
        syncWrite(nextTask.id)
    }

    suspend fun abandonTask(id: String) {
        taskDao.setArchived(id, true, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        syncWrite(id)
        log(ActionType.TASK_ABANDONED, id, label)
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
        ) ?: return

        // 3 — insert fresh occurrence (reset completion + checklist)
        val nextTask = task.copy(
            id             = "task_${System.currentTimeMillis()}",
            isCompleted    = false,
            isArchived     = false,
            dueDate        = nextDue,
            checklistItems = task.checklistItems.map { it.copy(isCompleted = false) },
            createdAt      = LocalDateTime.now(),
            updatedAt      = LocalDateTime.now(),
        )
        taskDao.insert(nextTask.toEntity())
        syncthingWriter.writeTask(nextTask.toEntity())
        if (nextTask.checklistItems.isNotEmpty()) {
            val newItems = nextTask.checklistItems.mapIndexed { idx, item ->
                item.copy(id = "cli_${System.currentTimeMillis()}_$idx", taskId = nextTask.id).toEntity()
            }
            checklistDao.insertAll(newItems)
            newItems.forEach { syncthingWriter.writeChecklistItem(it) }
        }
        syncWrite(id)
    }

    suspend fun setStarred(id: String, starred: Boolean) {
        taskDao.setStarred(id, starred, LocalDateTime.now().toString())
        syncWrite(id)
    }

    suspend fun moveTask(id: String, projectId: String?, sectionId: String?) {
        taskDao.moveTask(id, projectId, sectionId, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        syncWrite(id)
        log(ActionType.TASK_MOVED, id, label, extra = projectId ?: "inbox")
    }

    suspend fun delete(id: String) {
        tombstone(id)
        taskDao.delete(id)
    }

    suspend fun deleteAll(ids: List<String>) {
        ids.forEach { tombstone(it) }
        taskDao.deleteAll(ids)
    }

    suspend fun softDelete(id: String) {
        val label = taskDao.getById(id)?.title ?: id
        taskDao.softDelete(id, LocalDateTime.now().toString())
        syncWrite(id)
        log(ActionType.TASK_DELETED, id, label)
    }

    fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrashWithItems().map { list -> list.map { it.toDomain() } }

    suspend fun restoreFromTrash(id: String) {
        taskDao.restoreFromTrash(id)
        syncWrite(id)
        val label = taskDao.getById(id)?.title ?: id
        log(ActionType.TASK_RESTORED, id, label)
    }

    suspend fun emptyTrash() {
        taskDao.getTrashIds().forEach { tombstone(it) }
        taskDao.emptyTrash()
    }

    suspend fun addChecklistItem(item: ChecklistItem) {
        checklistDao.insert(item.toEntity())
        syncthingWriter.writeChecklistItem(item.toEntity())
    }

    suspend fun toggleChecklistItem(id: String, completed: Boolean) {
        checklistDao.setCompleted(id, completed)
        checklistDao.getById(id)?.let { syncthingWriter.writeChecklistItem(it) }
    }

    suspend fun deleteChecklistItem(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "checklist_items", id, now))
        syncthingWriter.writeTombstone("checklist_items", id, now)
        checklistDao.delete(id)
    }

    suspend fun updateChecklistItem(item: ChecklistItem) {
        checklistDao.update(item.toEntity())
        syncthingWriter.writeChecklistItem(item.toEntity())
    }

    suspend fun reorderTasks(idsInOrder: List<String>) {
        idsInOrder.forEachIndexed { index, id ->
            taskDao.updateSortOrder(id, index)
        }
        idsInOrder.forEach { syncWrite(it) }
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

    fun observeActiveCount(): Flow<Int>        = taskDao.observeActiveCount()
    fun observeAllCompletedCount(): Flow<Int>  = taskDao.observeAllCompletedCount()
    fun observeAllArchivedCount(): Flow<Int>   = taskDao.observeAllArchivedCount()
}
