package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY scheduledAt ASC")
    fun observeByTask(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE scheduledAt > :now ORDER BY scheduledAt ASC")
    suspend fun getFutureReminders(now: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
