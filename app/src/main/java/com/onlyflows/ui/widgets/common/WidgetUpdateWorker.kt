package com.onlyflows.ui.widgets.common

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.onlyflows.ui.widgets.OnlyFlowsWidget
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        OnlyFlowsWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build(),
            )
        }
    }
}
