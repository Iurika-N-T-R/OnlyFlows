package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.ChecklistSectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistSectionDao {

    @Query("SELECT * FROM checklist_sections WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun observeByTask(taskId: String): Flow<List<ChecklistSectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: ChecklistSectionEntity)

    @Update
    suspend fun update(section: ChecklistSectionEntity)

    @Query("DELETE FROM checklist_sections WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE checklist_items SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun unassignItems(sectionId: String)
}
