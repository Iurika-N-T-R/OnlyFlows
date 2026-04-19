package com.onlyflows.data.syncthing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.debug.AppLog
import java.util.concurrent.TimeUnit

private const val TAG = "SyncthingImportWorker"
private const val WORK_NAME = "syncthing_periodic_import"

class SyncthingImportWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as OnlyFlowsApp).container
        val prefs = container.syncthingPreferences
        val importer = container.syncthingImporter
        val conflictResolver = container.conflictResolver

        return try {
            val conflicts = conflictResolver.resolveAll()
            val importResult = importer.importAll()
            prefs.setLastScannedAt(System.currentTimeMillis())
            AppLog.d(TAG, "Periodic import: $importResult, conflicts: $conflicts")
            Result.success()
        } catch (e: Exception) {
            AppLog.e(TAG, "Periodic import failed", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncthingImportWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
