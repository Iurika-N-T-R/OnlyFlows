package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.ChecklistItemDao
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.nextDueDate
import com.onlyflows.domain.repository.TaskRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val checklistDao: ChecklistItemDao,
) : TaskRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<Task>> =
        taskDao.observeAll().flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUpcoming(): Flow<List<Task>> =
        taskDao.observeUpcoming().flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeOverdue(): Flow<List<Task>> =
        taskDao.observeOverdueTasks(java.time.LocalDate.now().toString()).flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeInbox(): Flow<List<Task>> =
        taskDao.observeInbox().flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByGroup(group: com.onlyflows.domain.model.TaskGroup): Flow<List<Task>> =
        taskDao.observeByGroup(group.name).flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeToday(): Flow<List<Task>> =
        taskDao.observeToday().flatMapLatest { entities ->
            // For each task, also observe its checklist items
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items ->
                    entity.toDomain(items)
                }
            }
            if (flows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(flows) { it.toList() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByProject(projectId: String): Flow<List<Task>> =
        taskDao.observeByProject(projectId).flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeArchivedByProject(projectId: String): Flow<List<Task>> =
        taskDao.observeArchivedByProject(projectId).flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllArchived(): Flow<List<Task>> =
        taskDao.observeAllArchived().flatMapLatest { entities ->
            val flows = entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeById(id: String): Flow<Task?> =
        taskDao.observeById(id).flatMapLatest { entity ->
            if (entity == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                checklistDao.observeByTask(id).map { items ->
                    entity.toDomain(items)
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDependents(taskId: String): Flow<List<Task>> =
        taskDao.observeDependents(taskId).flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { e ->
                checklistDao.observeByTask(e.id).map { items -> e.toDomain(items) }
            }) { it.toList() }
        }

    override suspend fun setDependsOn(id: String, dependsOnId: String?) {
        taskDao.setDependsOn(id, dependsOnId, LocalDateTime.now().toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAnytime(): Flow<List<Task>> =
        taskDao.observeAnytime().flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSomeday(): Flow<List<Task>> =
        taskDao.observeSomeday().flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }) { it.toList() }
        }

    override suspend fun setScheduledGroup(id: String, group: com.onlyflows.domain.model.TaskGroup?) {
        taskDao.setScheduledGroup(id, group?.name, LocalDateTime.now().toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun search(query: String): Flow<List<Task>> =
        if (query.isBlank()) flowOf(emptyList())
        else taskDao.search(query).flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun searchByDate(date: java.time.LocalDate): Flow<List<Task>> =
        taskDao.searchByDate(date.toString()).flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }) { it.toList() }
        }

    override suspend fun insert(task: Task) {
        taskDao.insert(task.toEntity())
        checklistDao.insertAll(task.checklistItems.map { it.toEntity() })
    }

    override suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        taskDao.setCompleted(id, completed, LocalDateTime.now().toString())
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        taskDao.setArchived(id, archived, LocalDateTime.now().toString())
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

    override suspend fun setStarred(id: String, starred: Boolean) {
        taskDao.setStarred(id, starred, LocalDateTime.now().toString())
    }

    override suspend fun moveTask(id: String, projectId: String?, sectionId: String?) {
        taskDao.moveTask(id, projectId, sectionId, LocalDateTime.now().toString())
    }

    override suspend fun delete(id: String) {
        taskDao.delete(id)
    }

    override suspend fun deleteAll(ids: List<String>) {
        taskDao.deleteAll(ids)
    }

    override suspend fun softDelete(id: String) {
        taskDao.softDelete(id, LocalDateTime.now().toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrash().flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { entity ->
                checklistDao.observeByTask(entity.id).map { items -> entity.toDomain(items) }
            }) { it.toList() }
        }

    override suspend fun restoreFromTrash(id: String) {
        taskDao.restoreFromTrash(id)
    }

    override suspend fun emptyTrash() {
        taskDao.emptyTrash()
    }

    override suspend fun addChecklistItem(item: ChecklistItem) {
        checklistDao.insert(item.toEntity())
    }

    override suspend fun toggleChecklistItem(id: String, completed: Boolean) {
        checklistDao.setCompleted(id, completed)
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
}
