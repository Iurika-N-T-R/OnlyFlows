package com.onlyflows.data.repository

import android.net.Uri
import com.onlyflows.data.local.dao.AttachmentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.Attachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class AttachmentRepository(
    private val dao: AttachmentDao,
    private val syncDao: SyncDao,
    private val syncthingWriter: SyncthingWriter,
) {

    fun observeByTask(taskId: String): Flow<List<Attachment>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    suspend fun add(attachment: Attachment) {
        dao.insert(attachment.toEntity())
        runCatching {
            syncthingWriter.copyAttachmentFile(
                id        = attachment.id,
                fileName  = attachment.fileName,
                sourceUri = Uri.parse(attachment.uri),
            )
            syncthingWriter.writeAttachmentMeta(attachment.toEntity())
        }
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "attachments", id, now))
        syncthingWriter.writeTombstone("attachments", id, now)
        dao.delete(id)
    }

    suspend fun deleteByTask(taskId: String) {
        val now = LocalDateTime.now()
        dao.getByTask(taskId).forEach { attachment ->
            syncDao.insertTombstone(DeletedEntityEntity("tomb_${attachment.id}", "attachments", attachment.id, now))
            syncthingWriter.writeTombstone("attachments", attachment.id, now)
        }
        dao.deleteByTask(taskId)
    }

    suspend fun countByTask(taskId: String): Int =
        dao.countByTask(taskId)
}
