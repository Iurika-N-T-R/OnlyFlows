package com.onlyflows.data.repository

import android.net.Uri
import com.onlyflows.data.local.dao.AttachmentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.Attachment
import com.onlyflows.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class AttachmentRepositoryImpl(
    private val dao: AttachmentDao,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : AttachmentRepository {

    override fun observeByTask(taskId: String): Flow<List<Attachment>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun add(attachment: Attachment) {
        dao.insert(attachment.toEntity())
        syncthingWriter?.let { writer ->
            runCatching {
                writer.copyAttachmentFile(
                    id        = attachment.id,
                    fileName  = attachment.fileName,
                    sourceUri = Uri.parse(attachment.uri),
                )
                writer.writeAttachmentMeta(attachment.toEntity())
            }
        }
    }

    override suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "attachments", id, now))
        syncthingWriter?.writeTombstone("attachments", id, now)
        dao.delete(id)
    }

    override suspend fun deleteByTask(taskId: String) {
        val now = LocalDateTime.now()
        dao.getByTask(taskId).forEach { attachment ->
            syncDao?.insertTombstone(DeletedEntityEntity("tomb_${attachment.id}", "attachments", attachment.id, now))
            syncthingWriter?.writeTombstone("attachments", attachment.id, now)
        }
        dao.deleteByTask(taskId)
    }

    override suspend fun countByTask(taskId: String): Int =
        dao.countByTask(taskId)
}
