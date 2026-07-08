package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ProjectTaskCount
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.local.entity.TaskWithChecklistItems
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Inbox = no project AND no explicit scheduled group (unprocessed tasks)
    @Query("SELECT * FROM tasks WHERE projectId IS NULL AND scheduledGroup IS NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("UPDATE tasks SET dependsOnTaskId = :dependsOnId, updatedAt = :now WHERE id = :id")
    suspend fun setDependsOn(id: String, dependsOnId: String?, now: String)

    @Query("UPDATE tasks SET goalIds = :goalIds, updatedAt = :now WHERE id = :id")
    suspend fun setGoals(id: String, goalIds: String, now: String)

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

    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL
          AND (
            scheduledGroup IN ('TODAY', 'THIS_EVENING')
            OR dueDate = :today
            OR (startDate <= :today AND (dueDate IS NULL OR dueDate >= :today))
          )
        ORDER BY scheduledGroup ASC, sortOrder ASC
    """)
    fun observeToday(today: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL
          AND (
            dueDate < :today
            OR (scheduledGroup = 'TODAY' AND (dueDate IS NULL OR dueDate < :today))
          )
        ORDER BY dueDate ASC, sortOrder ASC
    """)
    fun observeOverdueTasksForWidget(today: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL
          AND (:projectId IS NULL OR projectId = :projectId)
          AND (:sectionId IS NULL OR sectionId = :sectionId)
        ORDER BY
          CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
          dueDate ASC, sortOrder ASC
    """)
    fun observeActiveForWidget(
        projectId: String? = null,
        sectionId: String? = null
    ): Flow<List<TaskEntity>>

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

    /** All active (non-completed, non-archived, non-deleted) tasks globally */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    fun observeActiveCount(): Flow<Int>

    /** All completed tasks globally */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND deletedAt IS NULL")
    fun observeAllCompletedCount(): Flow<Int>

    /** All archived tasks globally (abandoned) */
    @Query("SELECT COUNT(*) FROM tasks WHERE isArchived = 1 AND deletedAt IS NULL")
    fun observeAllArchivedCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE dueDate >= :today AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY dueDate ASC LIMIT 3")
    suspend fun getUpcomingDeadlines(today: String): List<TaskEntity>

    /** All active tasks with a due time or start time from today on — alarm reconciliation. */
    @Query("SELECT * FROM tasks WHERE ((dueDate >= :today AND dueTime IS NOT NULL) OR (startDate >= :today AND startTime IS NOT NULL)) AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    suspend fun getUpcomingTimed(today: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND deletedAt IS NULL AND completedAt >= :sevenDaysAgo")
    suspend fun getCompletedThisWeekCount(sevenDaysAgo: String): Int

    // ── Writes ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = CASE WHEN :completed THEN :now ELSE NULL END, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, now: String)

    @Query("UPDATE tasks SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: String)

    @Query("UPDATE tasks SET isStarred = :starred, updatedAt = :now WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean, now: String)

    @Query("UPDATE tasks SET projectId = :projectId, sectionId = :sectionId, updatedAt = :now WHERE id = :id")
    suspend fun moveTask(id: String, projectId: String?, sectionId: String?, now: String)

    @Query("UPDATE tasks SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE tasks SET deletedAt = NULL, updatedAt = :now WHERE id = :id")
    suspend fun restoreFromTrash(id: String, now: String)

    @Query("SELECT id FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun getTrashIds(): List<String>

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("UPDATE tasks SET sectionId = NULL, updatedAt = :now WHERE sectionId = :sectionId")
    suspend fun clearSection(sectionId: String, now: String)

    @Query("UPDATE tasks SET deletedAt = :now, updatedAt = :now WHERE sectionId = :sectionId AND deletedAt IS NULL")
    suspend fun softDeleteBySection(sectionId: String, now: String)

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
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL
          AND (
            scheduledGroup IN ('TODAY', 'THIS_EVENING')
            OR dueDate = :today
            OR (startDate <= :today AND (dueDate IS NULL OR dueDate >= :today))
          )
        ORDER BY scheduledGroup ASC, sortOrder ASC
    """)
    fun observeTodayWithItems(today: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 1 AND deletedAt IS NULL AND completedAt >= :startOfDay
          AND (
            scheduledGroup IN ('TODAY', 'THIS_EVENING')
            OR dueDate = :today
            OR (startDate <= :today AND (dueDate IS NULL OR dueDate >= :today))
          )
        ORDER BY completedAt DESC
    """)
    fun observeTodayCompletedWithItems(today: String, startOfDay: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE isArchived = 1 AND isCompleted = 0 AND deletedAt IS NULL
          AND (
            scheduledGroup IN ('TODAY', 'THIS_EVENING')
            OR dueDate = :today
            OR (startDate <= :today AND (dueDate IS NULL OR dueDate >= :today))
          )
        ORDER BY updatedAt DESC
    """)
    fun observeTodayAbandonedWithItems(today: String): Flow<List<TaskWithChecklistItems>>

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

    // ── Completed-today sections (mirror the active queries, isCompleted=1 + completed today) ──
    @Transaction
    @Query("""SELECT * FROM tasks WHERE projectId IS NULL AND scheduledGroup IS NULL
        AND isCompleted = 1 AND isArchived = 0 AND deletedAt IS NULL AND completedAt >= :startOfDay
        ORDER BY completedAt DESC""")
    fun observeInboxCompletedWithItems(startOfDay: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("""SELECT * FROM tasks WHERE isCompleted = 1 AND isArchived = 0 AND deletedAt IS NULL
        AND completedAt >= :startOfDay
        AND (scheduledGroup = 'ANYTIME' OR (projectId IS NOT NULL AND priority != 'NONE' AND scheduledGroup IS NULL))
        ORDER BY completedAt DESC""")
    fun observeAnytimeCompletedWithItems(startOfDay: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("""SELECT * FROM tasks WHERE isCompleted = 1 AND isArchived = 0 AND deletedAt IS NULL
        AND completedAt >= :startOfDay
        AND (scheduledGroup = 'SOMEDAY' OR (projectId IS NOT NULL AND priority = 'NONE' AND scheduledGroup IS NULL))
        ORDER BY completedAt DESC""")
    fun observeSomedayCompletedWithItems(startOfDay: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 50")
    fun searchWithItems(query: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE (dueDate = :date OR startDate = :date) AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 30")
    fun searchByDateWithItems(date: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE ((dueDate >= :start AND dueDate <= :end) OR (startDate >= :start AND startDate <= :end)) AND deletedAt IS NULL ORDER BY dueDate ASC, startDate ASC LIMIT 50")
    fun searchByDateRangeWithItems(start: String, end: String): Flow<List<TaskWithChecklistItems>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashWithItems(): Flow<List<TaskWithChecklistItems>>

}
