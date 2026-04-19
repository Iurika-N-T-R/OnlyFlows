package com.onlyflows.data.syncthing

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

private const val TAG = "SyncthingWriter"

class SyncthingWriter(
    private val context: Context,
    private val prefs: SyncthingPreferences,
) {

    // ── Public write methods ──────────────────────────────────────────────────

    suspend fun writeTask(entity: TaskEntity)                 = writeEntity("tasks", entity.id, entity.toJson())
    suspend fun writeProject(entity: ProjectEntity)           = writeEntity("projects", entity.id, entity.toJson())
    suspend fun writeSection(entity: SectionEntity)           = writeEntity("sections", entity.id, entity.toJson())
    suspend fun writeChecklistItem(entity: ChecklistItemEntity) = writeEntity("checklist_items", entity.id, entity.toJson())
    suspend fun writeTag(entity: TagEntity)                   = writeEntity("tags", entity.id, entity.toJson())
    suspend fun writeFolder(entity: FolderEntity)             = writeEntity("folders", entity.id, entity.toJson())
    suspend fun writeNote(entity: NoteEntity)                 = writeEntity("notes", entity.id, entity.toJson())
    suspend fun writeComment(entity: CommentEntity)           = writeEntity("comments", entity.id, entity.toJson())
    suspend fun writeReminder(entity: ReminderEntity)         = writeEntity("reminders", entity.id, entity.toJson())
    suspend fun writeTimeEntry(entity: TimeEntryEntity)       = writeEntity("time_entries", entity.id, entity.toJson())
    suspend fun writeSavedLocation(entity: SavedLocationEntity) = writeEntity("saved_locations", entity.id, entity.toJson())

    suspend fun writeAttachmentMeta(entity: AttachmentEntity) =
        writeEntity("attachments/metadata", entity.id, entity.toJson())

    /**
     * Copy the physical file from a content:// URI into the Syncthing folder.
     * Returns the SAF URI string of the destination file (to store in AttachmentEntity.uri on
     * the remote device after import), or null on failure.
     */
    suspend fun copyAttachmentFile(
        id: String,
        fileName: String,
        sourceUri: Uri,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val filesDir = getOrCreateDir("attachments/files") ?: return@withContext null
            val existing = filesDir.findFile(fileName)
            val destFile = existing ?: filesDir.createFile("application/octet-stream", fileName)
                ?: return@withContext null
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(destFile.uri, "wt")?.use { output ->
                    input.copyTo(output)
                }
            }
            AppLog.d(TAG, "Attachment file written: $fileName")
            destFile.uri.toString()
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to copy attachment $id", e)
            null
        }
    }

    /** Append a tombstone to _meta/tombstones.json */
    suspend fun writeTombstone(entityType: String, entityId: String, deletedAt: LocalDateTime) =
        withContext(Dispatchers.IO) {
            try {
                val root = getRoot() ?: return@withContext
                val meta = root.findFile("_meta") ?: root.createDirectory("_meta") ?: return@withContext
                val existing = meta.findFile("tombstones.json")
                val arr: JSONArray = if (existing != null) {
                    val text = context.contentResolver.openInputStream(existing.uri)
                        ?.bufferedReader()?.readText() ?: "[]"
                    runCatching { JSONArray(text) }.getOrDefault(JSONArray())
                } else JSONArray()

                // Avoid duplicates
                val alreadyPresent = (0 until arr.length()).any { i ->
                    val obj = arr.getJSONObject(i)
                    obj.optString("entityId") == entityId && obj.optString("entityType") == entityType
                }
                if (!alreadyPresent) {
                    arr.put(JSONObject().apply {
                        put("entityType", entityType)
                        put("entityId", entityId)
                        put("deletedAt", deletedAt.toString())
                    })
                    val file = existing ?: meta.createFile("application/json", "tombstones.json")
                        ?: return@withContext
                    context.contentResolver.openOutputStream(file.uri, "wt")?.use {
                        it.write(arr.toString(2).toByteArray())
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to write tombstone $entityType/$entityId", e)
            }
        }

    /** Write device info to _meta/device.json */
    suspend fun writeDeviceInfo(deviceId: String, deviceName: String, appVersion: String) =
        withContext(Dispatchers.IO) {
            try {
                val root = getRoot() ?: return@withContext
                val meta = root.findFile("_meta") ?: root.createDirectory("_meta") ?: return@withContext
                val file = meta.findFile("device.json")
                    ?: meta.createFile("application/json", "device.json")
                    ?: return@withContext
                val json = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("deviceName", deviceName)
                    put("appVersion", appVersion)
                    put("lastWrittenAt", LocalDateTime.now().toString())
                }
                context.contentResolver.openOutputStream(file.uri, "wt")?.use {
                    it.write(json.toString(2).toByteArray())
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to write device.json", e)
            }
        }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun writeEntity(subpath: String, id: String, json: JSONObject) =
        withContext(Dispatchers.IO) {
            try {
                val dir = getOrCreateDir(subpath) ?: return@withContext
                val file = dir.findFile("$id.json")
                    ?: dir.createFile("application/json", "$id.json")
                    ?: return@withContext
                context.contentResolver.openOutputStream(file.uri, "wt")?.use {
                    it.write(json.toString(2).toByteArray())
                }
                AppLog.d(TAG, "Written $subpath/$id.json")
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to write $subpath/$id", e)
            }
        }

    private suspend fun getOrCreateDir(subpath: String): DocumentFile? {
        val root = getRoot() ?: return null
        var current: DocumentFile = root
        for (segment in subpath.split("/")) {
            current = current.findFile(segment) ?: current.createDirectory(segment) ?: return null
        }
        return current
    }

    private suspend fun getRoot(): DocumentFile? {
        val uriStr = prefs.folderUri.first()
        if (uriStr.isBlank()) return null
        return DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
    }
}
