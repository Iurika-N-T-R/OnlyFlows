package com.onlyflows.domain.repository

import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {
    fun observeAll(): Flow<List<Task>>
    fun observeToday(): Flow<List<Task>>
    fun observeInbox(): Flow<List<Task>>
    fun observeByGroup(group: com.onlyflows.domain.model.TaskGroup): Flow<List<Task>>
    fun observeOverdue(): Flow<List<Task>>
    fun observeUpcoming(): Flow<List<Task>>
    fun observeByProject(projectId: String): Flow<List<Task>>
    fun observeArchivedByProject(projectId: String): Flow<List<Task>>
    fun observeAllArchived(): Flow<List<Task>>
    fun observeById(id: String): Flow<Task?>
    fun observeAnytime(): Flow<List<Task>>
    fun observeSomeday(): Flow<List<Task>>
    fun observeDependents(taskId: String): Flow<List<Task>>
    suspend fun setDependsOn(id: String, dependsOnId: String?)
    suspend fun setScheduledGroup(id: String, group: TaskGroup?)
    fun search(query: String): Flow<List<Task>>
    fun searchByDate(date: LocalDate): Flow<List<Task>>

    suspend fun insert(task: Task)
    suspend fun update(task: Task)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun completeRecurring(id: String)
    suspend fun setStarred(id: String, starred: Boolean)
    suspend fun moveTask(id: String, projectId: String?, sectionId: String?)
    suspend fun delete(id: String)
    suspend fun deleteAll(ids: List<String>)
    suspend fun softDelete(id: String)
    fun observeTrash(): Flow<List<Task>>
    suspend fun restoreFromTrash(id: String)
    suspend fun emptyTrash()
    suspend fun reorderTasks(idsInOrder: List<String>)

    // Checklist
    suspend fun addChecklistItem(item: ChecklistItem)
    suspend fun toggleChecklistItem(id: String, completed: Boolean)
    suspend fun deleteChecklistItem(id: String)
    suspend fun updateChecklistItem(item: ChecklistItem)

    // Counts (for sidebar badges and project progress)
    fun observeInboxCount(): Flow<Int>
    fun observeTodayCount(): Flow<Int>
    fun observeOverdueCount(): Flow<Int>
    fun observeAllCount(projectId: String): Flow<Int>
    fun observeCompletedCount(projectId: String): Flow<Int>
}
