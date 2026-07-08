package com.onlyflows.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Forces all OnlyFlows widgets to rebuild. Called after any task mutation (wired in AppContainer). */
object WidgetUpdater {
    fun updateAll(context: Context) {
        update(context, OnlyFlowsWidgetProvider::class.java)
        update(context, CountdownWidgetProvider::class.java)
    }

    private fun update(context: Context, provider: Class<*>) {
        val mgr = AppWidgetManager.getInstance(context) ?: return
        val ids = mgr.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, provider).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        )
    }
}
