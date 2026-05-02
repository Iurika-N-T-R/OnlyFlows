package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.GoalLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface GoalLogDao {

    @Query("SELECT * FROM goal_logs WHERE goalId = :goalId AND deletedAt IS NULL ORDER BY loggedAt DESC")
    fun observeByGoal(goalId: String): Flow<List<GoalLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: GoalLogEntity)

    @Query("SELECT * FROM goal_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalLogEntity?

    @Query("UPDATE goal_logs SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    @Query("UPDATE goal_logs SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: LocalDateTime = LocalDateTime.now())
}
