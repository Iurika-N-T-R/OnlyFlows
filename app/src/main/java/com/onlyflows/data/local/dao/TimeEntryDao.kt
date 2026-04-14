package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.TimeEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimeEntryEntity)

    @Update
    suspend fun update(entry: TimeEntryEntity)

    @Query("SELECT * FROM time_entries WHERE taskId = :taskId ORDER BY startedAt ASC")
    fun observeByTask(taskId: String): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries WHERE taskId = :taskId ORDER BY startedAt ASC")
    suspend fun getByTask(taskId: String): List<TimeEntryEntity>

    @Query("SELECT * FROM time_entries WHERE taskId = :taskId AND endedAt IS NULL LIMIT 1")
    suspend fun getActiveEntry(taskId: String): TimeEntryEntity?

    @Query("SELECT * FROM time_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TimeEntryEntity?

    @Query("SELECT * FROM time_entries WHERE taskId IN (SELECT id FROM tasks WHERE projectId = :projectId) ORDER BY startedAt ASC")
    suspend fun getByProject(projectId: String): List<TimeEntryEntity>

    @Query("DELETE FROM time_entries WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
