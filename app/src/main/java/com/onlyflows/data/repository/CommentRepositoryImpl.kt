package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.CommentDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Comment
import com.onlyflows.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class CommentRepositoryImpl(private val dao: CommentDao) : CommentRepository {

    override fun observeByTask(taskId: String): Flow<List<Comment>> =
        dao.observeByTask(taskId).map { it.map { e -> e.toDomain() } }

    override fun observeByProject(projectId: String): Flow<List<Comment>> =
        dao.observeByProject(projectId).map { it.map { e -> e.toDomain() } }

    override suspend fun add(comment: Comment) = dao.insert(comment.toEntity())

    override suspend fun edit(id: String, newContent: String) {
        dao.updateContent(id, newContent, LocalDateTime.now().toString())
    }

    override suspend fun delete(id: String) = dao.delete(id)
}
