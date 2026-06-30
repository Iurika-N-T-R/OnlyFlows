package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ChecklistItemReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemReminderDao {

    @Query("SELECT * FROM checklist_item_reminders WHERE checklistItemId = :itemId ORDER BY scheduledAt ASC")
    fun observeByItem(itemId: String): Flow<List<ChecklistItemReminderEntity>>

    @Query("SELECT * FROM checklist_item_reminders WHERE scheduledAt > :now ORDER BY scheduledAt ASC")
    suspend fun getFutureReminders(now: String): List<ChecklistItemReminderEntity>

    @Query("SELECT * FROM checklist_item_reminders")
    fun observeAll(): Flow<List<ChecklistItemReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ChecklistItemReminderEntity)

    @Query("DELETE FROM checklist_item_reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM checklist_item_reminders WHERE checklistItemId = :itemId")
    suspend fun getByItem(itemId: String): List<ChecklistItemReminderEntity>

    @Query("DELETE FROM checklist_item_reminders WHERE checklistItemId = :itemId")
    suspend fun deleteByItem(itemId: String)
}
