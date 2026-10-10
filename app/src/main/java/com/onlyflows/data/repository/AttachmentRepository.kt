package com.onlyflows.data.repository

import android.content.Context
import android.net.Uri
import com.onlyflows.data.local.AttachmentStore
import com.onlyflows.data.local.dao.AttachmentDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.domain.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AttachmentRepository(
    private val context: Context,
    private val dao: AttachmentDao,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
) {

    fun observeByTask(taskId: String): Flow<List<Attachment>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    /**
     * Copies the picked file into app storage before inserting the row. [Attachment.uri] is the
     * picker's source URI on the way in and is replaced with our own FileProvider URI, so the
     * attachment keeps working after the original file is moved, renamed or deleted.
     *
     * Returns false when the bytes could not be read — nothing is inserted in that case rather
     * than leaving a row pointing at a file that was never stored.
     */
    suspend fun add(attachment: Attachment): Boolean = withContext(Dispatchers.IO) {
        val stored = AttachmentStore.copyIn(
            context   = context,
            id        = attachment.id,
            fileName  = attachment.fileName,
            sourceUri = Uri.parse(attachment.uri),
        ) ?: return@withContext false

        val entity = attachment.copy(
            uri      = AttachmentStore.shareUri(context, stored).toString(),
            fileSize = if (attachment.fileSize > 0L) attachment.fileSize else stored.length(),
        ).toEntity()
        dao.insert(entity)
        lanSyncWriter.writeAttachmentMeta(entity)
        true
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "attachments", id, now))
        lanSyncWriter.writeTombstone("attachments", id, now)
        dao.delete(id)
        withContext(Dispatchers.IO) { AttachmentStore.deleteFor(context, id) }
    }

    suspend fun deleteByTask(taskId: String) {
        val now = LocalDateTime.now()
        dao.getByTask(taskId).forEach { attachment ->
            syncDao.insertTombstone(DeletedEntityEntity("tomb_${attachment.id}", "attachments", attachment.id, now))
            lanSyncWriter.writeTombstone("attachments", attachment.id, now)
            withContext(Dispatchers.IO) { AttachmentStore.deleteFor(context, attachment.id) }
        }
        dao.deleteByTask(taskId)
    }

    suspend fun countByTask(taskId: String): Int =
        dao.countByTask(taskId)
}
