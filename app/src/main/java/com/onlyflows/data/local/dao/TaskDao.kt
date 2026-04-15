package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ProjectTaskCount
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.local.entity.TaskWithChecklistItems
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    // Inbox = no project AND no explicit scheduled group (unprocessed tasks)
    @Query("SELECT * FROM tasks WHERE projectId IS NULL AND scheduledGroup IS NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE scheduledGroup = :group AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeByGroup(group: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeArchivedByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAllArchived(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE sectionId = :sectionId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeBySection(sectionId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeByIdFlow(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE dependsOnTaskId = :taskId")
    fun observeDependents(taskId: String): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET dependsOnTaskId = :dependsOnId, updatedAt = :now WHERE id = :id")
    suspend fun setDependsOn(id: String, dependsOnId: String?, now: String)

    // Anytime = explicitly moved there (any task) OR project tasks with priority and no due date (auto)
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND dueDate IS NULL AND deletedAt IS NULL
          AND (scheduledGroup = 'ANYTIME'
            OR (projectId IS NOT NULL AND priority != 'NONE' AND scheduledGroup IS NULL))
        ORDER BY
          CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END,
          sortOrder ASC
    """)
    fun observeAnytime(): Flow<List<TaskEntity>>

    // Someday = explicitly moved there (any task) OR project tasks without priority and no due date (auto)
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND dueDate IS NULL AND deletedAt IS NULL
          AND (scheduledGroup = 'SOMEDAY'
            OR (projectId IS NOT NULL AND priority = 'NONE' AND scheduledGroup IS NULL))
        ORDER BY sortOrder ASC
    """)
    fun observeSomeday(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET scheduledGroup = :group, updatedAt = :now WHERE id = :id")
    suspend fun setScheduledGroup(id: String, group: String?, now: String)

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 50")
    fun search(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (dueDate = :date OR startDate = :date) AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 30")
    fun searchByDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE scheduledGroup IN ('TODAY', 'THIS_EVENING') AND isArchived = 0 AND deletedAt IS NULL ORDER BY scheduledGroup ASC, sortOrder ASC")
    fun observeToday(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate < :today AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY dueDate ASC, sortOrder ASC")
    fun observeOverdueTasks(today: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY dueDate ASC, sortOrder ASC")
    fun observeUpcoming(): Flow<List<TaskEntity>>

    // ── Counts for sidebar ────────────────────────────────────────────────────

    /** Total non-completed, non-archived tasks in a project */
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    fun observeTotalCount(projectId: String): Flow<Int>

    /** Completed tasks in a project */
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND isCompleted = 1 AND isArchived = 0 AND deletedAt IS NULL")
    fun observeCompletedCount(projectId: String): Flow<Int>

    /** All tasks in a project (for progress ratio, excludes archived) */
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND isArchived = 0 AND deletedAt IS NULL")
    fun observeAllCount(projectId: String): Flow<Int>

    /** Aggregate progress for all projects in one query */
    @Query("""
        SELECT projectId,
               COUNT(*) AS total,
               SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS completed
        FROM tasks
        WHERE isArchived = 0 AND deletedAt IS NULL AND projectId IS NOT NULL
        GROUP BY projectId
    """)
    fun observeAllProjectProgress(): Flow<List<ProjectTaskCount>>

    /** Count inbox tasks (unprocessed: no project, no scheduled group, not completed) */
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId IS NULL AND scheduledGroup IS NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    fun observeInboxCount(): Flow<Int>

    /** Count today tasks (not completed, not archived) */
    @Query("SELECT COUNT(*) FROM tasks WHERE scheduledGroup = 'TODAY' AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL AND (dueDate IS NULL OR dueDate >= :today)")
    fun observeTodayCount(today: String = java.time.LocalDate.now().toString()): Flow<Int>

    /** Count overdue tasks (past due date, not completed, not archived) */
    @Query("SELECT COUNT(*) FROM tasks WHERE dueDate < :today AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    fun observeOverdueCount(today: String): Flow<Int>

    // ── Writes ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, now: String)

    @Query("UPDATE tasks SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: String)

    @Query("UPDATE tasks SET isStarred = :starred, updatedAt = :now WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean, now: String)

    @Query("UPDATE tasks SET projectId = :projectId, sectionId = :sectionId, updatedAt = :now WHERE id = :id")
    suspend fun moveTask(id: String, projectId: String?, sectionId: String?, now: String)

    @Query("UPDATE tasks SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE tasks SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("UPDATE tasks SET sectionId = NULL, updatedAt = :now WHERE sectionId = :sectionId")
    suspend fun clearSection(sectionId: String, now: String)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    // ── @Relation queries (single-join, no N+1) ───────────────────────────────

    @Transaction
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeAllWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE projectId IS NULL AND scheduledGroup IS NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeInboxWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE scheduledGroup = :group AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeByGroupWithItems(group: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeByProjectWithItems(projectId: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeArchivedByProjectWithItems(projectId: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAllArchivedWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeByIdWithItems(id: String): Flow<TaskWithChecklistItems?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE dependsOnTaskId = :taskId")
    fun observeDependentsWithItems(taskId: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE scheduledGroup IN ('TODAY', 'THIS_EVENING') AND isArchived = 0 AND deletedAt IS NULL ORDER BY scheduledGroup ASC, sortOrder ASC")
    fun observeTodayWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE dueDate < :today AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY dueDate ASC, sortOrder ASC")
    fun observeOverdueWithItems(today: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY dueDate ASC, sortOrder ASC")
    fun observeUpcomingWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND dueDate IS NULL AND deletedAt IS NULL
          AND (scheduledGroup = 'ANYTIME'
            OR (projectId IS NOT NULL AND priority != 'NONE' AND scheduledGroup IS NULL))
        ORDER BY
          CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END,
          sortOrder ASC
    """)
    fun observeAnytimeWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND dueDate IS NULL AND deletedAt IS NULL
          AND (scheduledGroup = 'SOMEDAY'
            OR (projectId IS NOT NULL AND priority = 'NONE' AND scheduledGroup IS NULL))
        ORDER BY sortOrder ASC
    """)
    fun observeSomedayWithItems(): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 50")
    fun searchWithItems(query: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE (dueDate = :date OR startDate = :date) AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 30")
    fun searchByDateWithItems(date: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashWithItems(): Flow<List<TaskWithChecklistItems>>

}
