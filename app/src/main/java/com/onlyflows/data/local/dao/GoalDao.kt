package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE deletedAt IS NULL AND isCompleted = 0 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeById(id: String): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET currentCount = :count, updatedAt = :now WHERE id = :id")
    suspend fun setProgress(id: String, count: Int, now: java.time.LocalDateTime = java.time.LocalDateTime.now())

    @Query("UPDATE goals SET isCompleted = :completed, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, now: java.time.LocalDateTime = java.time.LocalDateTime.now())

    @Query("UPDATE goals SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: java.time.LocalDateTime = java.time.LocalDateTime.now())

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: String)
}
