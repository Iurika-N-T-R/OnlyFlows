package com.onlyflows.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Rebuilds every widget and re-arms the shared refresh alarm.
 *
 *  Targeted by the alarm itself, and by the system broadcasts that invalidate a widget's contents.
 *  DATE_CHANGED/TIME_SET matter because the TODAY filter and the overdue colouring are derived from
 *  `LocalDate.now()` at build time; BOOT_COMPLETED and MY_PACKAGE_REPLACED matter because both wipe
 *  pending alarms, which previously left the refresh chain dead until the next manual task edit. */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdater.updateAll(context)
        WidgetUpdater.scheduleNextRefresh(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.onlyflows.action.WIDGET_REFRESH"
    }
}
