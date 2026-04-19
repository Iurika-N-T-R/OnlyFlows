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
import org.json.JSONObject
import java.time.LocalDateTime

private const val TAG = "ConflictResolver"

// Syncthing conflict file pattern: <uuid>.sync-conflict-<date>-<time>-<deviceId>.json
private val CONFLICT_REGEX = Regex("""^(.+?)\.sync-conflict-.+\.json$""")

data class ConflictResolution(val resolved: Int, val errors: Int)

class ConflictResolver(
    private val context: Context,
    private val syncDao: SyncDao,
    private val prefs: SyncthingPreferences,
) {

    /**
     * Scan all entity subdirs for .sync-conflict-* files and resolve them.
     * Last-write-wins on updatedAt. Tiebreaker: larger deviceId string.
     */
    suspend fun resolveAll(): ConflictResolution = withContext(Dispatchers.IO) {
        val root = getRoot() ?: return@withContext ConflictResolution(0, 0)
        var resolved = 0
        var errors = 0

        val entityDirs = listOf(
            "tasks", "projects", "sections", "checklist_items", "tags",
            "folders", "notes", "comments", "reminders", "time_entries",
            "saved_locations", "attachments/metadata",
        )

        for (subpath in entityDirs) {
            val dir = navigateTo(root, subpath) ?: continue
            val conflictFiles = dir.listFiles().filter { f ->
                CONFLICT_REGEX.matches(f.name ?: "")
            }
            for (conflictFile in conflictFiles) {
                val match = CONFLICT_REGEX.find(conflictFile.name ?: "") ?: continue
                val baseId = match.groupValues[1]
                val origFile = dir.findFile("$baseId.json")

                val result = runCatching {
                    resolveConflict(dir, subpath, baseId, origFile, conflictFile)
                }
                if (result.isSuccess && result.getOrDefault(false)) resolved++
                else if (result.isFailure) {
                    AppLog.e(TAG, "Error resolving conflict ${conflictFile.name}", result.exceptionOrNull())
                    errors++
                }
            }
        }
        AppLog.d(TAG, "Conflict resolution done: $resolved resolved, $errors errors")
        ConflictResolution(resolved, errors)
    }

    private suspend fun resolveConflict(
        dir: DocumentFile,
        entityType: String,
        baseId: String,
        origFile: DocumentFile?,
        conflictFile: DocumentFile,
    ): Boolean {
        val conflictJson = readJson(conflictFile) ?: return false
        val conflictUpdatedAt = conflictJson.getUpdatedAt()

        if (origFile == null) {
            // No original → conflict file IS the data
            val newFile = dir.createFile("application/json", "$baseId.json") ?: return false
            context.contentResolver.openOutputStream(newFile.uri, "wt")?.use {
                it.write(conflictJson.toString(2).toByteArray())
            }
            conflictFile.delete()
            upsertToRoom(entityType, baseId, conflictJson)
            AppLog.d(TAG, "Conflict: no original, used conflict file for $baseId")
            return true
        }

        val origJson = readJson(origFile)
        val origUpdatedAt = origJson?.getUpdatedAt() ?: LocalDateTime.MIN

        val winner: JSONObject
        val loser: DocumentFile

        if (conflictUpdatedAt > origUpdatedAt) {
            // Conflict file is newer — overwrite original
            winner = conflictJson
            loser = conflictFile
            context.contentResolver.openOutputStream(origFile.uri, "wt")?.use {
                it.write(winner.toString(2).toByteArray())
            }
            AppLog.d(TAG, "Conflict resolved: conflict file wins for $baseId ($conflictUpdatedAt > $origUpdatedAt)")
        } else if (origUpdatedAt > conflictUpdatedAt) {
            // Original is newer — just delete conflict file
            winner = origJson!!
            loser = conflictFile
            AppLog.d(TAG, "Conflict resolved: original wins for $baseId ($origUpdatedAt > $conflictUpdatedAt)")
        } else {
            // Same timestamp — tiebreaker: use conflict file deviceId extracted from filename
            // (larger string wins, deterministic)
            val conflictDeviceId = conflictFile.name
                ?.replace(Regex("""^.+\.sync-conflict-\d{8}-\d{6}-(.+)\.json$"""), "$1")
                ?: ""
            val thisDeviceId = prefs.getDeviceIdNow()
            winner = if (conflictDeviceId > thisDeviceId) conflictJson else origJson!!
            loser = conflictFile
            if (winner === conflictJson) {
                context.contentResolver.openOutputStream(origFile.uri, "wt")?.use {
                    it.write(winner.toString(2).toByteArray())
                }
            }
            AppLog.d(TAG, "Conflict resolved (tie): tiebreaker for $baseId")
        }

        loser.delete()
        upsertToRoom(entityType, baseId, winner)
        return true
    }

    private suspend fun upsertToRoom(entityType: String, id: String, json: JSONObject) {
        // Check if tombstoned first
        if (syncDao.isTombstoned(id, entityType.removeSuffix("/metadata"))) return
        runCatching {
            when (entityType) {
                "tasks"            -> syncDao.upsertTask(taskEntityFromJson(json))
                "projects"         -> syncDao.upsertProject(projectEntityFromJson(json))
                "sections"         -> syncDao.upsertSection(sectionEntityFromJson(json))
                "checklist_items"  -> syncDao.upsertChecklistItem(checklistItemEntityFromJson(json))
                "tags"             -> syncDao.upsertTag(tagEntityFromJson(json))
                "folders"          -> syncDao.upsertFolder(folderEntityFromJson(json))
                "notes"            -> syncDao.upsertNote(noteEntityFromJson(json))
                "comments"         -> syncDao.upsertComment(commentEntityFromJson(json))
                "reminders"        -> syncDao.upsertReminder(reminderEntityFromJson(json))
                "time_entries"     -> syncDao.upsertTimeEntry(timeEntryEntityFromJson(json))
                "saved_locations"  -> syncDao.upsertSavedLocation(savedLocationEntityFromJson(json))
                "attachments/metadata" -> { /* metadata only — file handled by importer */ }
            }
        }.onFailure { AppLog.e(TAG, "Failed to upsert $entityType/$id after conflict resolution", it) }
    }

    private fun readJson(file: DocumentFile): JSONObject? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use {
            JSONObject(it.readText())
        }
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

    private fun JSONObject.getUpdatedAt(): LocalDateTime = runCatching {
        LocalDateTime.parse(getString("updatedAt"))
    }.getOrDefault(LocalDateTime.MIN)
}
