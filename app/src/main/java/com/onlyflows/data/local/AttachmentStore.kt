package com.onlyflows.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Physical storage for task attachments.
 *
 * Attachments used to be stored as the picker's `content://` URI, which is a *link*: the row
 * survived but the bytes did not — renaming, moving or deleting the original file, or clearing
 * the source app's data, left a dead attachment, and nothing could be sent to a paired device.
 * Every attachment is now copied into app-private storage under `filesDir/attachments/<id>/`
 * and exposed to other apps through [FileProvider].
 */
object AttachmentStore {

    private const val DIR = "attachments"

    /** `filesDir/attachments/<id>` — one folder per attachment keeps original file names intact. */
    fun dirFor(context: Context, id: String): File =
        File(File(context.filesDir, DIR), sanitize(id))

    fun fileFor(context: Context, id: String, fileName: String): File =
        File(dirFor(context, id), sanitize(fileName).ifBlank { "file" })

    /** The already-stored file for an attachment, whatever its name, or null if nothing is stored. */
    fun existingFile(context: Context, id: String): File? =
        dirFor(context, id).listFiles()?.firstOrNull { it.isFile }

    /** Copies [sourceUri] into app storage. Returns the destination file, or null on failure. */
    fun copyIn(context: Context, id: String, fileName: String, sourceUri: Uri): File? {
        val dest = fileFor(context, id, fileName)
        dest.parentFile?.mkdirs()
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest
        }.getOrElse {
            dest.delete()
            null
        }
    }

    /** Writes raw bytes received from a paired device. Returns the destination file, or null. */
    fun writeBytes(context: Context, id: String, fileName: String, bytes: ByteArray): File? {
        val dest = fileFor(context, id, fileName)
        dest.parentFile?.mkdirs()
        return runCatching { dest.writeBytes(bytes); dest }.getOrElse {
            dest.delete()
            null
        }
    }

    fun deleteFor(context: Context, id: String) {
        runCatching { dirFor(context, id).deleteRecursively() }
    }

    /** A URI other apps can read, granted per-intent with FLAG_GRANT_READ_URI_PERMISSION. */
    fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Strips path separators so a hostile or odd file name cannot escape the folder. */
    private fun sanitize(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|\r\n]"""), "_").trim().take(120)
}
