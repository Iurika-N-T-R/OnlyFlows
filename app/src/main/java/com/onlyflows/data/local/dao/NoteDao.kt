package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

data class NoteCount(val projectId: String, val total: Int)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE project_id = :projectId AND isArchived = 0 ORDER BY isPinned DESC, created_at DESC")
    fun observeByProject(projectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE project_id = :projectId AND isArchived = 1 ORDER BY updated_at DESC")
    fun observeArchivedByProject(projectId: String): Flow<List<NoteEntity>>

    @Query("SELECT project_id AS projectId, COUNT(*) AS total FROM notes WHERE isArchived = 0 GROUP BY project_id")
    fun observeCountsByProject(): Flow<List<NoteCount>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)
}
