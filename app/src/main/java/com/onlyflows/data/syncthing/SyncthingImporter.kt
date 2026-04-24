package com.onlyflows.data.syncthing

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

private const val TAG = "SyncthingImporter"

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val tombstonesApplied: Int,
    val errors: Int,
)

class SyncthingImporter(
    private val context: Context,
    private val syncDao: SyncDao,
    private val prefs: SyncthingPreferences,
) {

    /**
     * Full scan: reads every entity JSON file from the Syncthing folder and upserts
     * into Room if the file's updatedAt is newer than what Room has.
     * Also applies all tombstones from _meta/tombstones.json.
     * Resolves any remaining conflict files before importing.
     */
    suspend fun importAll(): ImportResult = withContext(Dispatchers.IO) {
        val root = getRoot() ?: return@withContext ImportResult(0, 0, 0, 0)
        var imported = 0
        var skipped = 0
        var errors = 0

        val entityDirs = listOf(
            "tasks", "projects", "sections", "checklist_items", "tags",
            "folders", "notes", "comments", "reminders", "time_entries",
            "saved_locations", "goals", "goal_logs", "date_trackers",
        )

        for (subpath in entityDirs) {
            val dir = navigateTo(root, subpath) ?: continue
            for (file in dir.listFiles()) {
                val name = file.name ?: continue
                if (!name.endsWith(".json")) continue
                if (CONFLICT_REGEX.matches(name)) continue  // handled by ConflictResolver
                val id = name.removeSuffix(".json")
                val result = runCatching {
                    val json = readJson(file) ?: return@runCatching false
                    importEntity(subpath, id, json)
                }
                when {
                    result.isSuccess && result.getOrDefault(false) -> imported++
                    result.isSuccess                               -> skipped++
                    else -> {
                        AppLog.e(TAG, "Error importing $subpath/$id", result.exceptionOrNull())
                        errors++
                    }
                }
            }
        }

        // Attachments: metadata + physical files
        val attachErrors = importAttachments(root)
        errors += attachErrors

        // Tombstones
        val tombstonesApplied = applyTombstones(root)

        AppLog.d(TAG, "Import done: $imported imported, $skipped skipped, $tombstonesApplied tombstones, $errors errors")
        ImportResult(imported, skipped, tombstonesApplied, errors)
    }

    private suspend fun importEntity(entityType: String, id: String, json: JSONObject): Boolean {
        if (syncDao.isTombstoned(id, entityType)) return false

        val fileUpdatedAt = json.getUpdatedAt()
        if (fileUpdatedAt == LocalDateTime.MIN) return false

        val roomUpdatedAt = getRoomUpdatedAt(entityType, id)
        if (roomUpdatedAt != null && !fileUpdatedAt.isAfter(roomUpdatedAt)) return false

        upsertToRoom(entityType, id, json)
        return true
    }

    private suspend fun getRoomUpdatedAt(entityType: String, id: String): LocalDateTime? =
        when (entityType) {
            "tasks"           -> syncDao.getTaskUpdatedAt(id)
            "projects"        -> syncDao.getProjectUpdatedAt(id)
            "sections"        -> syncDao.getSectionUpdatedAt(id)
            "checklist_items" -> syncDao.getChecklistItemUpdatedAt(id)
            "tags"            -> syncDao.getTagUpdatedAt(id)
            "folders"         -> syncDao.getFolderUpdatedAt(id)
            "notes"           -> syncDao.getNoteUpdatedAt(id)
            "comments"        -> syncDao.getCommentUpdatedAt(id)
            "reminders"       -> syncDao.getReminderUpdatedAt(id)
            "time_entries"    -> syncDao.getTimeEntryUpdatedAt(id)
            "saved_locations" -> syncDao.getSavedLocationUpdatedAt(id)
            "goals"           -> syncDao.getGoalUpdatedAt(id)
            "goal_logs"       -> syncDao.getGoalLogLoggedAt(id)
            "date_trackers"   -> syncDao.getDateTrackerCreatedAt(id)
            else              -> null
        }

    private suspend fun upsertToRoom(entityType: String, id: String, json: JSONObject) {
        runCatching {
            when (entityType) {
                "tasks"           -> syncDao.upsertTask(taskEntityFromJson(json))
                "projects"        -> syncDao.upsertProject(projectEntityFromJson(json))
                "sections"        -> syncDao.upsertSection(sectionEntityFromJson(json))
                "checklist_items" -> syncDao.upsertChecklistItem(checklistItemEntityFromJson(json))
                "tags"            -> syncDao.upsertTag(tagEntityFromJson(json))
                "folders"         -> syncDao.upsertFolder(folderEntityFromJson(json))
                "notes"           -> syncDao.upsertNote(noteEntityFromJson(json))
                "comments"        -> syncDao.upsertComment(commentEntityFromJson(json))
                "reminders"       -> syncDao.upsertReminder(reminderEntityFromJson(json))
                "time_entries"    -> syncDao.upsertTimeEntry(timeEntryEntityFromJson(json))
                "saved_locations" -> syncDao.upsertSavedLocation(savedLocationEntityFromJson(json))
                "goals"           -> syncDao.upsertGoal(goalEntityFromJson(json))
                "goal_logs"       -> syncDao.upsertGoalLog(goalLogEntityFromJson(json))
                "date_trackers"   -> syncDao.upsertDateTracker(dateTrackerEntityFromJson(json))
            }
        }.onFailure { AppLog.e(TAG, "Upsert failed $entityType/$id", it) }
    }

    private suspend fun importAttachments(root: DocumentFile): Int {
        var errors = 0
        val metaDir = navigateTo(root, "attachments/metadata") ?: return 0
        val filesDir = navigateTo(root, "attachments/files")

        for (file in metaDir.listFiles()) {
            val name = file.name ?: continue
            if (!name.endsWith(".json") || CONFLICT_REGEX.matches(name)) continue
            val id = name.removeSuffix(".json")
            runCatching {
                val json = readJson(file) ?: return@runCatching
                if (syncDao.isTombstoned(id, "attachments")) return@runCatching

                val fileUpdatedAt = json.getUpdatedAt()
                val roomUpdatedAt = syncDao.getAttachmentUpdatedAt(id)
                if (roomUpdatedAt != null && !fileUpdatedAt.isAfter(roomUpdatedAt)) return@runCatching

                // Locate physical file in attachments/files/
                val fileName = json.optString("fileName", "")
                val physicalFile = filesDir?.findFile(fileName)
                // If physical file not yet synced, skip — it will be imported on next scan
                val localUri = physicalFile?.uri?.toString() ?: return@runCatching

                syncDao.upsertAttachment(attachmentEntityFromJson(json, localUri))
            }.onFailure {
                AppLog.e(TAG, "Attachment import error $id", it)
                errors++
            }
        }
        return errors
    }

    private suspend fun applyTombstones(root: DocumentFile): Int {
        val metaDir = root.findFile("_meta") ?: return 0
        val tombstonesFile = metaDir.findFile("tombstones.json") ?: return 0
        val text = readText(tombstonesFile) ?: return 0
        val arr = runCatching { JSONArray(text) }.getOrDefault(JSONArray())
        var applied = 0

        val existingTombstones = syncDao.getAllTombstones().map { "${it.entityType}/${it.entityId}" }.toSet()

        for (i in 0 until arr.length()) {
            val obj = runCatching { arr.getJSONObject(i) }.getOrNull() ?: continue
            val entityType = obj.optString("entityType") ?: continue
            val entityId = obj.optString("entityId") ?: continue
            val deletedAt = runCatching {
                LocalDateTime.parse(obj.optString("deletedAt"))
            }.getOrNull() ?: continue

            val key = "$entityType/$entityId"
            if (key in existingTombstones) continue

            runCatching {
                // Only apply if tombstone is newer than what Room has
                val roomUpdatedAt = getRoomUpdatedAt(entityType, entityId)
                if (roomUpdatedAt != null && deletedAt.isBefore(roomUpdatedAt)) return@runCatching

                applyDelete(entityType, entityId)
                syncDao.insertTombstone(
                    DeletedEntityEntity(
                        id = "tomb_$entityId",
                        entityType = entityType,
                        entityId = entityId,
                        deletedAt = deletedAt,
                    )
                )
                applied++
            }.onFailure { AppLog.e(TAG, "Failed to apply tombstone $entityType/$entityId", it) }
        }
        return applied
    }

    private suspend fun applyDelete(entityType: String, entityId: String) {
        when (entityType) {
            "tasks"           -> syncDao.deleteTask(entityId)
            "projects"        -> syncDao.deleteProject(entityId)
            "sections"        -> syncDao.deleteSection(entityId)
            "checklist_items" -> syncDao.deleteChecklistItem(entityId)
            "tags"            -> syncDao.deleteTag(entityId)
            "folders"         -> syncDao.deleteFolder(entityId)
            "notes"           -> syncDao.deleteNote(entityId)
            "comments"        -> syncDao.deleteComment(entityId)
            "reminders"       -> syncDao.deleteReminder(entityId)
            "time_entries"    -> syncDao.deleteTimeEntry(entityId)
            "saved_locations" -> syncDao.deleteSavedLocation(entityId)
            "attachments"     -> syncDao.deleteAttachment(entityId)
            "goals"           -> syncDao.deleteGoal(entityId)
            "goal_logs"       -> syncDao.deleteGoalLog(entityId)
            "date_trackers"   -> syncDao.deleteDateTracker(entityId)
        }
    }

    private fun readJson(file: DocumentFile): JSONObject? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use {
            JSONObject(it.readText())
        }
    }.getOrNull()

    private fun readText(file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.readText()
    }.getOrNull()

    private fun navigateTo(root: DocumentFile, subpath: String): DocumentFile? {
        var current: DocumentFile = root
        for (segment in subpath.split("/")) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }

    private suspend fun getRoot(): DocumentFile? {
        val uriStr = prefs.folderUri.first()
        if (uriStr.isBlank()) return null
        return DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
    }
}

private val CONFLICT_REGEX = Regex("""^(.+?)\.sync-conflict-.+\.json$""")
