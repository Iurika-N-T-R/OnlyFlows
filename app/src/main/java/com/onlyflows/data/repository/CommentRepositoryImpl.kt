package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.CommentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.Comment
import com.onlyflows.domain.repository.ActionLogRepository
import com.onlyflows.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class CommentRepositoryImpl(
    private val dao: CommentDao,
    private val actionLog: ActionLogRepository? = null,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : CommentRepository {

    override fun observeByTask(taskId: String): Flow<List<Comment>> =
        dao.observeByTask(taskId).map { it.map { e -> e.toDomain() } }

    override fun observeByProject(projectId: String): Flow<List<Comment>> =
        dao.observeByProject(projectId).map { it.map { e -> e.toDomain() } }

    override suspend fun add(comment: Comment) {
        dao.insert(comment.toEntity())
        syncthingWriter?.writeComment(comment.toEntity())
        actionLog?.insert(
            ActionLog(
                id         = "al_${System.currentTimeMillis()}_COMMENT_ADDED",
                actionType = ActionType.COMMENT_ADDED,
                entityId   = comment.taskId ?: comment.projectId ?: comment.id,
                entityType = if (comment.taskId != null) "task" else "project",
                label      = comment.content.take(60),
                occurredAt = LocalDateTime.now(),
            )
        )
    }

    override suspend fun edit(id: String, newContent: String) {
        val now = LocalDateTime.now()
        dao.updateContent(id, newContent, now.toString())
        dao.getById(id)?.let { syncthingWriter?.writeComment(it) }
    }

    override suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "comments", id, now))
        syncthingWriter?.writeTombstone("comments", id, now)
        dao.delete(id)
    }
}
