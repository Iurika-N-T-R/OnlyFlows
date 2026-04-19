package com.onlyflows.data.syncthing

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.FileObserver
import android.provider.DocumentsContract
import com.onlyflows.data.debug.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.io.File

private const val TAG = "SyncthingCoordinator"
private const val DEBOUNCE_MS = 3_000L

class SyncthingCoordinator(
    private val context: Context,
    private val prefs: SyncthingPreferences,
    private val monitor: SyncthingStatusMonitor,
    private val importer: SyncthingImporter,
    private val conflictResolver: ConflictResolver,
    private val writer: SyncthingWriter,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observer: FileObserver? = null
    private var debounceJob: Job? = null

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    fun start() {
        scope.launch {
            // Write device info on first start
            val deviceId = prefs.getDeviceIdNow()
            val deviceName = android.os.Build.MODEL
            val appVersion = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            }.getOrDefault("")
            writer.writeDeviceInfo(deviceId, deviceName, appVersion)
        }

        scope.launch {
            monitor.status.collectLatest { status ->
                if (status == SyncthingStatus.Ready) startObserver()
                else stopObserver()
            }
        }
    }

    private suspend fun startObserver() {
        stopObserver()
        val uriStr = prefs.folderUri.first()
        if (uriStr.isBlank()) return
        val folder = safUriToFile(Uri.parse(uriStr)) ?: run {
            AppLog.w(TAG, "Cannot resolve real path — FileObserver unavailable, WorkManager covers it")
            triggerImport()  // still do one immediate import via SAF
            return
        }
        if (!folder.exists()) return

        observer = object : FileObserver(folder, CLOSE_WRITE or MOVED_TO or DELETE or CREATE) {
            override fun onEvent(event: Int, path: String?) {
                if (path?.contains("tombstones") == true) return
                scheduleImport()
            }
        }.also {
            it.startWatching()
            AppLog.d(TAG, "FileObserver started on ${folder.absolutePath}")
        }

        triggerImport()
    }

    private fun stopObserver() {
        observer?.stopWatching()
        observer = null
        debounceJob?.cancel()
    }

    private fun scheduleImport() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            triggerImport()
        }
    }

    private suspend fun triggerImport() {
        _isImporting.value = true
        try {
            conflictResolver.resolveAll()
            val result = importer.importAll()
            prefs.setLastScannedAt(System.currentTimeMillis())
            if (result.imported > 0 || result.tombstonesApplied > 0) {
                AppLog.d(TAG, "Auto-import: ${result.imported} imported, ${result.tombstonesApplied} tombstones")
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Auto-import failed", e)
        } finally {
            _isImporting.value = false
        }
    }

    private fun safUriToFile(uri: Uri): File? = runCatching {
        val docId = DocumentsContract.getTreeDocumentId(uri) ?: return null
        val parts = docId.split(":")
        if (parts.size != 2) return null
        when (parts[0]) {
            "primary" -> File(Environment.getExternalStorageDirectory(), parts[1])
            else      -> null
        }
    }.getOrNull()
}
