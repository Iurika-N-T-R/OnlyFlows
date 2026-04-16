package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeByTask(taskId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTask(taskId: String): List<AttachmentEntity>

    @Query("SELECT COUNT(*) FROM attachments WHERE taskId = :taskId")
    suspend fun countByTask(taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM attachments WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
