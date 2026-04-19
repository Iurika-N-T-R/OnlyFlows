package com.onlyflows.data.syncthing

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.FileObserver
import android.provider.DocumentsContract
import com.onlyflows.data.debug.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.io.File

private const val TAG = "SyncthingCoordinator"
private const val DEBOUNCE_MS = 3_000L  // wait 3s after last event before importing

/**
 * Watches the Syncthing folder with FileObserver and triggers import automatically
 * whenever Syncthing drops new or modified files.
 *
 * Lifecycle: call [start] once (e.g. from Application.onCreate). It self-manages
 * starting/stopping the observer based on SyncthingStatus.
 */
class SyncthingCoordinator(
    private val context: Context,
    private val prefs: SyncthingPreferences,
    private val monitor: SyncthingStatusMonitor,
    private val importer: SyncthingImporter,
    private val conflictResolver: ConflictResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observer: FileObserver? = null
    private var debounceJob: Job? = null

    fun start() {
        scope.launch {
            monitor.status.collectLatest { status ->
                if (status == SyncthingStatus.Ready) {
                    startObserver()
                } else {
                    stopObserver()
                }
            }
        }
    }

    private suspend fun startObserver() {
        stopObserver()
        val uriStr = prefs.folderUri.first()
        if (uriStr.isBlank()) return
        val folder = safUriToFile(Uri.parse(uriStr)) ?: run {
            AppLog.w(TAG, "Cannot resolve real path from SAF URI — FileObserver unavailable, falling back to WorkManager only")
            return
        }
        if (!folder.exists()) return

        observer = object : FileObserver(folder, CLOSE_WRITE or MOVED_TO or DELETE or CREATE) {
            override fun onEvent(event: Int, path: String?) {
                // Ignore our own tombstone writes to avoid import loops
                if (path?.contains("tombstones") == true) return
                scheduleImport()
            }
        }.also {
            it.startWatching()
            AppLog.d(TAG, "FileObserver started on ${folder.absolutePath}")
        }

        // Run an immediate import when we (re-)connect
        triggerImport()
    }

    private fun stopObserver() {
        observer?.stopWatching()
        observer = null
        debounceJob?.cancel()
        AppLog.d(TAG, "FileObserver stopped")
    }

    private fun scheduleImport() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            triggerImport()
        }
    }

    private suspend fun triggerImport() {
        try {
            conflictResolver.resolveAll()
            val result = importer.importAll()
            prefs.setLastScannedAt(System.currentTimeMillis())
            if (result.imported > 0 || result.tombstonesApplied > 0) {
                AppLog.d(TAG, "Auto-import: ${result.imported} imported, ${result.tombstonesApplied} tombstones")
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Auto-import failed", e)
        }
    }

    /**
     * Converts a SAF tree URI to a real File path.
     * Works for primary external storage (the common Syncthing location).
     * Returns null for SD card or unsupported volumes.
     */
    private fun safUriToFile(uri: Uri): File? = runCatching {
        val docId = DocumentsContract.getTreeDocumentId(uri) ?: return null
        val parts = docId.split(":")
        if (parts.size != 2) return null
        when (parts[0]) {
            "primary" -> File(Environment.getExternalStorageDirectory(), parts[1])
            else      -> null  // SD card path — FileObserver not supported, WorkManager covers it
        }
    }.getOrNull()
}
