package com.onlyflows.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.onlyflows.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupManager(private val context: Context) {

    private val dbName = "onlyflows.db"

    /** Checkpoint WAL and copy the DB file into the SAF folder [folderUri].
     *  Returns the created filename on success. */
    fun backup(folderUri: Uri): Result<String> = runCatching {
        // 1. Close the DB — SQLite checkpoints WAL automatically on close
        AppDatabase.closeInstance()

        // 2. Source file
        val dbFile = context.getDatabasePath(dbName)
        require(dbFile.exists()) { "Database file not found" }

        // 3. Create destination file in the SAF folder
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: error("Cannot open backup folder")
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName  = "onlyflows_backup_$timestamp.db"

        // Delete existing file with same name if any (shouldn't happen with timestamps)
        folder.findFile(fileName)?.delete()
        val destDoc = folder.createFile("application/octet-stream", fileName)
            ?: error("Cannot create backup file in chosen folder")

        // 4. Copy bytes
        context.contentResolver.openOutputStream(destDoc.uri)!!.use { out ->
            FileInputStream(dbFile).use { it.copyTo(out) }
        }

        fileName
    }

    /** Copy a backup file [fileUri] over the current DB.
     *  The app MUST be restarted (or the DB instance reset) after restore. */
    fun restore(fileUri: Uri): Result<Unit> = runCatching {
        val dbFile = context.getDatabasePath(dbName)

        // Close the DB instance before overwriting
        AppDatabase.closeInstance()

        // Delete WAL/SHM so Room doesn't try to replay a stale journal
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()

        context.contentResolver.openInputStream(fileUri)!!.use { input ->
            FileOutputStream(dbFile).use { input.copyTo(it) }
        }
    }

    /** Human-readable display name for a SAF tree URI. */
    fun folderDisplayName(folderUri: Uri): String {
        return DocumentFile.fromTreeUri(context, folderUri)?.name ?: folderUri.lastPathSegment ?: "—"
    }
}
