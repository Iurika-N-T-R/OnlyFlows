package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun observeByTask(taskId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getByTask(taskId: String): List<ChecklistItemEntity>

    @Query("SELECT taskId FROM checklist_items WHERE id = :id LIMIT 1")
    suspend fun getTaskIdForItem(id: String): String?

    @Query("SELECT * FROM checklist_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChecklistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("UPDATE checklist_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM checklist_items WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
