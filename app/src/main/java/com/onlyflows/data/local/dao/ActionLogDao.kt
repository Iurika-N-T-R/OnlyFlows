package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ActionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ActionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActionLogEntity)

    @Query("SELECT * FROM action_log ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<ActionLogEntity>>

    @Query("SELECT * FROM action_log WHERE entityId = :id ORDER BY occurredAt DESC")
    fun observeByEntity(id: String): Flow<List<ActionLogEntity>>

    @Query("SELECT COUNT(*) FROM action_log WHERE actionType = :type")
    fun observeCountByType(type: String): Flow<Int>

    @Query("""
        SELECT strftime('%Y-%m-%d', occurredAt) AS date, COUNT(*) AS count
        FROM action_log
        WHERE actionType = 'TASK_COMPLETED' AND occurredAt >= :since
        GROUP BY strftime('%Y-%m-%d', occurredAt)
        ORDER BY date ASC
    """)
    fun observeCompletedPerDay(since: String): Flow<List<DailyCount>>

    @Query("DELETE FROM action_log WHERE occurredAt < :before")
    suspend fun deleteOlderThan(before: LocalDateTime)
}

data class DailyCount(val date: String, val count: Int)
