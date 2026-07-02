package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.CommentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class CommentRepository(
    private val dao: CommentDao,
    private val actionLog: ActionLogRepository,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
) {

    fun observeByTask(taskId: String): Flow<List<Comment>> =
        dao.observeByTask(taskId).map { it.map { e -> e.toDomain() } }

    fun observeByProject(projectId: String): Flow<List<Comment>> =
        dao.observeByProject(projectId).map { it.map { e -> e.toDomain() } }

    suspend fun add(comment: Comment) {
        dao.insert(comment.toEntity())
        lanSyncWriter.writeComment(comment.toEntity())
        actionLog.insert(
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

    suspend fun edit(id: String, newContent: String) {
        val now = LocalDateTime.now()
        dao.updateContent(id, newContent, now.toString())
        dao.getById(id)?.let { lanSyncWriter.writeComment(it) }
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "comments", id, now))
        lanSyncWriter.writeTombstone("comments", id, now)
        dao.delete(id)
    }
}
