package com.onlyflows.data.repository

import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.local.dao.ChecklistItemDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.TaskWithChecklistItems
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.nextDueDate
import com.onlyflows.domain.repository.ActionLogRepository
import com.onlyflows.domain.repository.TaskRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val checklistDao: ChecklistItemDao,
    private val actionLog: ActionLogRepository? = null,
    private val syncDao: SyncDao? = null,
) : TaskRepository {

    private suspend fun tombstone(id: String) {
        syncDao?.insertTombstone(
            DeletedEntityEntity(
                id         = "tomb_$id",
                entityType = "tasks",
                entityId   = id,
                deletedAt  = LocalDateTime.now(),
            )
        )
    }

    private suspend fun log(type: ActionType, taskId: String, label: String, extra: String? = null) {
        actionLog?.insert(
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

    override fun observeAll(): Flow<List<Task>> =
        taskDao.observeAllWithItems().map { list -> list.map { it.toDomain() } }

    override fun observeUpcoming(): Flow<List<Task>> =
        taskDao.observeUpcomingWithItems().map { list -> list.map { it.toDomain() } }

    override fun observeOverdue(): Flow<List<Task>> =
        taskDao.observeOverdueWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    override fun observeInbox(): Flow<List<Task>> =
        taskDao.observeInboxWithItems().map { list -> list.map { it.toDomain() } }

    override fun observeByGroup(group: com.onlyflows.domain.model.TaskGroup): Flow<List<Task>> =
        taskDao.observeByGroupWithItems(group.name).map { list -> list.map { it.toDomain() } }

    override fun observeToday(): Flow<List<Task>> =
        taskDao.observeTodayWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    override fun observeTodayCompleted(): Flow<List<Task>> =
        taskDao.observeTodayCompletedWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    override fun observeTodayAbandoned(): Flow<List<Task>> =
        taskDao.observeTodayAbandonedWithItems(java.time.LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    override fun observeByProject(projectId: String): Flow<List<Task>> =
        taskDao.observeByProjectWithItems(projectId).map { list -> list.map { it.toDomain() } }

    override fun observeArchivedByProject(projectId: String): Flow<List<Task>> =
        taskDao.observeArchivedByProjectWithItems(projectId).map { list -> list.map { it.toDomain() } }

    override fun observeAllArchived(): Flow<List<Task>> =
        taskDao.observeAllArchivedWithItems().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Task?> =
        taskDao.observeByIdWithItems(id).map { it?.toDomain() }

    override fun observeDependents(taskId: String): Flow<List<Task>> =
        taskDao.observeDependentsWithItems(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun setDependsOn(id: String, dependsOnId: String?) {
        taskDao.setDependsOn(id, dependsOnId, LocalDateTime.now().toString())
    }

    override fun observeAnytime(): Flow<List<Task>> =
        taskDao.observeAnytimeWithItems().map { list -> list.map { it.toDomain() } }

    override fun observeSomeday(): Flow<List<Task>> =
        taskDao.observeSomedayWithItems().map { list -> list.map { it.toDomain() } }

    override suspend fun setScheduledGroup(id: String, group: com.onlyflows.domain.model.TaskGroup?) {
        taskDao.setScheduledGroup(id, group?.name, LocalDateTime.now().toString())
    }

    override fun search(query: String): Flow<List<Task>> =
        if (query.isBlank()) flowOf(emptyList())
        else taskDao.searchWithItems(query).map { list -> list.map { it.toDomain() } }

    override fun searchByDate(date: java.time.LocalDate): Flow<List<Task>> =
        taskDao.searchByDateWithItems(date.toString()).map { list -> list.map { it.toDomain() } }

    override fun searchByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<Task>> =
        taskDao.searchByDateRangeWithItems(start.toString(), end.toString()).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(task: Task) {
        taskDao.insert(task.toEntity())
        checklistDao.insertAll(task.checklistItems.map { it.toEntity() })
        log(ActionType.TASK_CREATED, task.id, task.title)
        AppLog.d("TaskRepo", "insert: \"${task.title}\" id=${task.id} group=${task.scheduledGroup} due=${task.dueDate} project=${task.projectId}")
    }

    override suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
        AppLog.d("TaskRepo", "update: \"${task.title}\" id=${task.id} due=${task.dueDate} start=${task.startDate} group=${task.scheduledGroup} priority=${task.priority} project=${task.projectId}")
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        taskDao.setCompleted(id, completed, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        log(if (completed) ActionType.TASK_COMPLETED else ActionType.TASK_UNCOMPLETED, id, label)
        AppLog.d("TaskRepo", "${if (completed) "complete" else "uncomplete"}: \"$label\" id=$id")
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        taskDao.setArchived(id, archived, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        log(if (archived) ActionType.TASK_ARCHIVED else ActionType.TASK_RESTORED, id, label)
        AppLog.d("TaskRepo", "${if (archived) "archive" else "restore"}: \"$label\" id=$id")
    }

    override suspend fun completeRecurring(id: String) {
        val entity = taskDao.getById(id) ?: return
        val items  = checklistDao.getByTask(id)
        val task   = entity.toDomain(items)
        val rule   = task.recurrence ?: return

        // 1 — mark current occurrence as completed + increment done count
        taskDao.update(
            entity.copy(
                isCompleted          = true,
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
    }

    override suspend fun abandonTask(id: String) {
        taskDao.setArchived(id, true, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        log(ActionType.TASK_ABANDONED, id, label)
    }

    override suspend fun abandonRecurring(id: String) {
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
        if (nextTask.checklistItems.isNotEmpty()) {
            checklistDao.insertAll(
                nextTask.checklistItems.mapIndexed { idx, item ->
                    item.copy(id = "cli_${System.currentTimeMillis()}_$idx", taskId = nextTask.id).toEntity()
                }
            )
        }
    }

    override suspend fun setStarred(id: String, starred: Boolean) {
        taskDao.setStarred(id, starred, LocalDateTime.now().toString())
        AppLog.d("TaskRepo", "${if (starred) "star" else "unstar"}: id=$id")
    }

    override suspend fun moveTask(id: String, projectId: String?, sectionId: String?) {
        taskDao.moveTask(id, projectId, sectionId, LocalDateTime.now().toString())
        val label = taskDao.getById(id)?.title ?: id
        log(ActionType.TASK_MOVED, id, label, extra = projectId ?: "inbox")
        AppLog.d("TaskRepo", "move: \"$label\" id=$id → project=${projectId ?: "inbox"} section=$sectionId")
    }

    override suspend fun delete(id: String) {
        tombstone(id)
        taskDao.delete(id)
        AppLog.d("TaskRepo", "hardDelete: id=$id")
    }

    override suspend fun deleteAll(ids: List<String>) {
        ids.forEach { tombstone(it) }
        taskDao.deleteAll(ids)
        AppLog.d("TaskRepo", "hardDeleteAll: ${ids.size} tasks")
    }

    override suspend fun softDelete(id: String) {
        val label = taskDao.getById(id)?.title ?: id
        taskDao.softDelete(id, LocalDateTime.now().toString())
        log(ActionType.TASK_DELETED, id, label)
        AppLog.d("TaskRepo", "softDelete: \"$label\" id=$id")
    }

    override fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrashWithItems().map { list -> list.map { it.toDomain() } }

    override suspend fun restoreFromTrash(id: String) {
        taskDao.restoreFromTrash(id)
        val label = taskDao.getById(id)?.title ?: id
        log(ActionType.TASK_RESTORED, id, label)
    }

    override suspend fun emptyTrash() {
        taskDao.getTrashIds().forEach { tombstone(it) }
        taskDao.emptyTrash()
    }

    override suspend fun addChecklistItem(item: ChecklistItem) {
        checklistDao.insert(item.toEntity())
        AppLog.d("TaskRepo", "addChecklist: \"${item.title}\" taskId=${item.taskId}")
    }

    override suspend fun toggleChecklistItem(id: String, completed: Boolean) {
        checklistDao.setCompleted(id, completed)
        AppLog.d("TaskRepo", "toggleChecklist: id=$id completed=$completed")
    }

    override suspend fun deleteChecklistItem(id: String) {
        checklistDao.delete(id)
    }

    override suspend fun updateChecklistItem(item: ChecklistItem) {
        checklistDao.update(item.toEntity())
    }

    override suspend fun reorderTasks(idsInOrder: List<String>) {
        idsInOrder.forEachIndexed { index, id ->
            taskDao.updateSortOrder(id, index)
        }
    }

    override fun observeInboxCount(): Flow<Int>   = taskDao.observeInboxCount()
    override fun observeTodayCount(): Flow<Int>   = taskDao.observeTodayCount()
    override fun observeOverdueCount(): Flow<Int> = taskDao.observeOverdueCount(java.time.LocalDate.now().toString())
    override fun observeAllCount(projectId: String): Flow<Int>       = taskDao.observeAllCount(projectId)
    override fun observeCompletedCount(projectId: String): Flow<Int> = taskDao.observeCompletedCount(projectId)
    override fun observeAllProjectProgress(): Flow<List<com.onlyflows.domain.model.ProjectTaskProgress>> =
        taskDao.observeAllProjectProgress().map { list ->
            list.map { com.onlyflows.domain.model.ProjectTaskProgress(it.projectId, it.total, it.completed) }
        }

    override fun observeActiveCount(): Flow<Int>        = taskDao.observeActiveCount()
    override fun observeAllCompletedCount(): Flow<Int>  = taskDao.observeAllCompletedCount()
    override fun observeAllArchivedCount(): Flow<Int>   = taskDao.observeAllArchivedCount()
}
