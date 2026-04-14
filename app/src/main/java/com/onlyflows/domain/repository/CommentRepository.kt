package com.onlyflows.domain.repository

import com.onlyflows.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun observeByTask(taskId: String): Flow<List<Comment>>
    fun observeByProject(projectId: String): Flow<List<Comment>>
    suspend fun add(comment: Comment)
    suspend fun edit(id: String, newContent: String)
    suspend fun delete(id: String)
}
