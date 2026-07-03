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

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE deletedAt IS NULL")
    suspend fun getAll(): List<GoalEntity>

    @Query("UPDATE goals SET periodStart = :start, updatedAt = :now WHERE id = :id")
    suspend fun anchorPeriod(id: String, start: java.time.LocalDate, now: java.time.LocalDateTime = java.time.LocalDateTime.now())

    @Query("UPDATE goals SET currentCount = 0, isCompleted = 0, periodStart = :start, updatedAt = :now WHERE id = :id")
    suspend fun resetPeriod(id: String, start: java.time.LocalDate, now: java.time.LocalDateTime = java.time.LocalDateTime.now())

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
