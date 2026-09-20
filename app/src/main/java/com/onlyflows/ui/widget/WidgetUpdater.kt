package com.onlyflows.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId

/** Forces all OnlyFlows widgets to rebuild. Called after any task mutation (wired in AppContainer). */
object WidgetUpdater {

    private const val TAG = "WidgetUpdater"
    private const val PERIODIC_MS = 15 * 60 * 1000L
    private const val REFRESH_REQUEST_CODE = 0x0F10

    fun updateAll(context: Context) {
        update(context, OnlyFlowsWidgetProvider::class.java)
        update(context, CountdownWidgetProvider::class.java)
    }

    /** Re-arms the single shared refresh alarm, or cancels it when no widget is placed.
     *
     *  Fires at the earlier of the next midnight or [PERIODIC_MS] from now: the midnight leg is what
     *  rolls the TODAY filter and the overdue colouring over to the new day, the periodic leg is a
     *  safety net for mutations that never reached us (OEM process kill, missed LAN delta).
     *  `*AndAllowWhileIdle` is required — a plain `set()` is deferred for hours by Doze and by the
     *  app-standby bucket as soon as the app stops being used, which is what left widgets stale. */
    fun scheduleNextRefresh(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = refreshPI(context)

        if (widgetCount(context) == 0) {
            am.cancel(pi)
            return
        }

        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val midnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val triggerMs = minOf(now + PERIODIC_MS, midnight + 2_000)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            Log.e(TAG, "SecurityException on refresh alarm, used inexact fallback", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule widget refresh", e)
        }
    }

    private fun widgetCount(context: Context): Int {
        val mgr = AppWidgetManager.getInstance(context) ?: return 0
        return listOf(OnlyFlowsWidgetProvider::class.java, CountdownWidgetProvider::class.java)
            .sumOf { mgr.getAppWidgetIds(ComponentName(context, it)).size }
    }

    private fun refreshPI(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REFRESH_REQUEST_CODE,
            Intent(context, WidgetRefreshReceiver::class.java)
                .setAction(WidgetRefreshReceiver.ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

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
