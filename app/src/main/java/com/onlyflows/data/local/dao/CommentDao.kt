package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Update
    suspend fun update(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun observeByTask(taskId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTask(taskId: String): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeByProject(projectId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CommentEntity?

    @Query("UPDATE comments SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateContent(id: String, content: String, updatedAt: String)
}
