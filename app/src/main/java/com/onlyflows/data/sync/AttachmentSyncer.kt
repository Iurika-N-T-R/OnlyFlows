package com.onlyflows.data.sync

import android.content.Context
import android.util.Log
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.preferences.SyncPreferences
import com.onlyflows.data.sync.network.AttachmentDto
import com.onlyflows.data.sync.network.SyncClient
import com.onlyflows.domain.sync.PeerInfo
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDateTime

private const val TAG = "AttachmentSyncer"

class AttachmentSyncer(
    private val context: Context,
    private val syncClient: SyncClient,
    private val syncDao: SyncDao,
    private val syncPreferences: SyncPreferences,
) {
    private val attachmentDir: File
        get() = File(context.getExternalFilesDir("attachments") ?: context.filesDir, "sync")
            .also { it.mkdirs() }

    /**
     * For each accepted attachment DTO from a peer, download the file if:
     * - syncAttachments preference is enabled
     * - file size is within the configured limit
     * - file does not already exist locally
     */
    suspend fun syncAttachments(peer: PeerInfo, dtos: List<AttachmentDto>) {
        val enabled = syncPreferences.syncAttachments.first()
        if (!enabled) return
        val maxBytes = syncPreferences.maxAttachmentSizeMb.first() * 1024L * 1024L

        for (dto in dtos) {
            if (dto.syncedFrom != null) {
                // Already originated from a remote device — check we haven't already stored it
                val localFile = File(attachmentDir, "${dto.id}_${dto.fileName}")
                if (localFile.exists()) {
                    // Update the URI in Room to point to local file (idempotent)
                    updateUri(dto.id, localFile.absolutePath)
                    continue
                }
            }
            if (dto.fileSize > maxBytes) {
                Log.d(TAG, "Skipping ${dto.fileName} (${dto.fileSize} bytes > limit)")
                continue
            }
            val localFile = File(attachmentDir, "${dto.id}_${dto.fileName}")
            if (localFile.exists()) {
                updateUri(dto.id, localFile.absolutePath)
                continue
            }
            Log.d(TAG, "Downloading ${dto.fileName} from ${peer.displayName}")
            val bytes = syncClient.downloadFile(peer, dto.id)
            if (bytes != null && bytes.isNotEmpty()) {
                localFile.writeBytes(bytes)
                updateUri(dto.id, localFile.absolutePath)
                Log.d(TAG, "Saved ${dto.fileName} (${bytes.size} bytes)")
            } else {
                Log.w(TAG, "Failed to download ${dto.fileName}")
            }
        }
    }

    private suspend fun updateUri(attachmentId: String, localPath: String) {
        syncDao.updateAttachmentUri(attachmentId, "file://$localPath")
    }
}
