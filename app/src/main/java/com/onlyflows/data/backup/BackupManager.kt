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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backups are a ZIP holding the database **and** the attachment files, because attachment rows
 * only name a file in `filesDir/attachments` — a database-only copy restored every task with a
 * paperclip that opened onto nothing.
 *
 * Archives written before that change are plain `.db` files; [restore] detects which it was
 * handed and keeps reading the old ones.
 */
class BackupManager(private val context: Context) {

    private val dbName = "onlyflows.db"

    private companion object {
        const val DB_ENTRY          = "onlyflows.db"
        const val ATTACHMENTS_ENTRY = "attachments/"
        const val ATTACHMENTS_DIR   = "attachments"
        /** Local file header of every ZIP: "PK\u0003\u0004". */
        val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    }

    private val attachmentsDir: File get() = File(context.filesDir, ATTACHMENTS_DIR)

    /** Checkpoint WAL and write a backup archive into the SAF folder [folderUri].
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
        val fileName  = "onlyflows_backup_$timestamp.zip"

        // Delete existing file with same name if any (shouldn't happen with timestamps)
        folder.findFile(fileName)?.delete()
        val destDoc = folder.createFile("application/zip", fileName)
            ?: error("Cannot create backup file in chosen folder")

        // 4. Write the archive
        context.contentResolver.openOutputStream(destDoc.uri)!!.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(DB_ENTRY))
                FileInputStream(dbFile).use { it.copyTo(zip) }
                zip.closeEntry()

                attachmentsDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relative = file.relativeTo(attachmentsDir).invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry("$ATTACHMENTS_ENTRY$relative"))
                        FileInputStream(file).use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
            }
        }

        fileName
    }

    /** Restore a backup archive (or a legacy plain `.db` file) over the current data.
     *  The app MUST be restarted (or the DB instance reset) after restore. */
    fun restore(fileUri: Uri): Result<Unit> = runCatching {
        // Stage the archive locally: the content URI cannot be reopened reliably, and nothing
        // should be overwritten before we know the payload is usable.
        val staged = File.createTempFile("onlyflows_restore", null, context.cacheDir)
        try {
            context.contentResolver.openInputStream(fileUri)!!.use { input ->
                FileOutputStream(staged).use { input.copyTo(it) }
            }
            if (isZip(staged)) restoreArchive(staged) else restoreLegacyDb(staged)
        } finally {
            staged.delete()
        }
    }

    private fun restoreArchive(archive: File) {
        // Unpack to a scratch folder first so a truncated archive cannot leave the app with
        // half of the old data and half of the new.
        val scratch = File(context.cacheDir, "restore_${System.currentTimeMillis()}")
        val newDb   = File(scratch, DB_ENTRY)
        val newAtt  = File(scratch, ATTACHMENTS_DIR)
        try {
            scratch.mkdirs()
            ZipInputStream(FileInputStream(archive).buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = safeTarget(scratch, entry.name)
                    if (target != null && !entry.isDirectory) {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            require(newDb.exists() && newDb.length() > 0) { "Backup contains no database" }

            swapInDatabase(newDb)

            // Attachments last: the database is already the new one, so its rows and the files
            // on disk describe the same backup either way this step ends.
            attachmentsDir.deleteRecursively()
            if (newAtt.exists()) {
                attachmentsDir.parentFile?.mkdirs()
                newAtt.copyRecursively(attachmentsDir, overwrite = true)
            }
        } finally {
            scratch.deleteRecursively()
        }
    }

    /** Pre-ZIP backups were the raw database file; they carry no attachments. */
    private fun restoreLegacyDb(dbCopy: File) = swapInDatabase(dbCopy)

    private fun swapInDatabase(source: File) {
        val dbFile = context.getDatabasePath(dbName)

        // Close the DB instance before overwriting
        AppDatabase.closeInstance()

        // Delete WAL/SHM so Room doesn't try to replay a stale journal
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()

        dbFile.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(dbFile).use { input.copyTo(it) }
        }
    }

    private fun isZip(file: File): Boolean {
        val header = ByteArray(4)
        val read = FileInputStream(file).use { it.read(header) }
        return read == 4 && header.contentEquals(ZIP_MAGIC)
    }

    /** Resolves a zip entry inside [root], or null when the name tries to escape it (zip slip). */
    private fun safeTarget(root: File, entryName: String): File? {
        val candidate = File(root, entryName).normalize()
        val rootPath  = root.normalize().path + File.separator
        return if (candidate.path.startsWith(rootPath)) candidate else null
    }

    /** Human-readable display name for a SAF tree URI. */
    fun folderDisplayName(folderUri: Uri): String {
        return DocumentFile.fromTreeUri(context, folderUri)?.name ?: folderUri.lastPathSegment ?: "—"
    }
}
