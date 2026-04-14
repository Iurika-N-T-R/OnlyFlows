package com.onlyflows.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.flow.first

class BackupWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs         = (context.applicationContext as OnlyFlowsApp).container.backupPreferences
        val folderUriStr  = prefs.folderUri.first()
        if (folderUriStr.isBlank()) return Result.failure()

        val folderUri = Uri.parse(folderUriStr)
        val manager   = BackupManager(context.applicationContext)
        val result    = manager.backup(folderUri)

        return if (result.isSuccess) {
            prefs.setLastBackupMs(System.currentTimeMillis())
            Result.success()
        } else {
            Result.retry()
        }
    }
}
