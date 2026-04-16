package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.AttachmentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Attachment
import com.onlyflows.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class AttachmentRepositoryImpl(
    private val dao: AttachmentDao,
    private val syncDao: SyncDao? = null,
) : AttachmentRepository {

    override fun observeByTask(taskId: String): Flow<List<Attachment>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun add(attachment: Attachment) =
        dao.insert(attachment.toEntity())

    override suspend fun delete(id: String) {
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "attachments", id, LocalDateTime.now()))
        dao.delete(id)
    }

    override suspend fun deleteByTask(taskId: String) =
        dao.deleteByTask(taskId)

    override suspend fun countByTask(taskId: String): Int =
        dao.countByTask(taskId)
}
