package com.onlyflows.data.syncthing

import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.local.dao.SyncDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SyncthingExporter"

/**
 * Writes every Room entity to the Syncthing folder.
 * Called once when the user first configures the folder, so the other device
 * can import all existing data without any user action.
 */
class SyncthingExporter(
    private val syncDao: SyncDao,
    private val writer: SyncthingWriter,
) {
    suspend fun exportAll(): Int = withContext(Dispatchers.IO) {
        var count = 0

        suspend fun <T> write(fetch: suspend () -> List<T>, export: suspend (T) -> Unit) {
            runCatching { fetch() }.getOrDefault(emptyList()).forEach { entity ->
                runCatching { export(entity) }.onSuccess { count++ }
                    .onFailure { AppLog.e(TAG, "Export error", it) }
            }
        }

        write({ syncDao.getAllTasks() })           { writer.writeTask(it) }
        write({ syncDao.getAllProjects() })         { writer.writeProject(it) }
        write({ syncDao.getAllSections() })         { writer.writeSection(it) }
        write({ syncDao.getAllChecklistItems() })   { writer.writeChecklistItem(it) }
        write({ syncDao.getAllTags() })             { writer.writeTag(it) }
        write({ syncDao.getAllFolders() })          { writer.writeFolder(it) }
        write({ syncDao.getAllNotes() })            { writer.writeNote(it) }
        write({ syncDao.getAllComments() })         { writer.writeComment(it) }
        write({ syncDao.getAllReminders() })        { writer.writeReminder(it) }
        write({ syncDao.getAllTimeEntries() })      { writer.writeTimeEntry(it) }
        write({ syncDao.getAllSavedLocations() })   { writer.writeSavedLocation(it) }
        write({ syncDao.getAllGoals() })            { writer.writeGoal(it) }
        write({ syncDao.getAllGoalLogs() })         { writer.writeGoalLog(it) }
        write({ syncDao.getAllDateTrackers() })     { writer.writeDateTracker(it) }
        write({ syncDao.getAllAttachments() })      { writer.writeAttachmentMeta(it) }

        AppLog.d(TAG, "Initial export complete: $count entities")
        count
    }
}
