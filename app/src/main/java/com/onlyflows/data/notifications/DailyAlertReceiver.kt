package com.onlyflows.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import kotlin.math.abs

class DailyAlertReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION     = "com.onlyflows.ACTION_DAILY_ALERT"
        const val EXTRA_DAY  = "daily_alert_day"
        const val EXTRA_TIME = "daily_alert_time"

        private const val TAG = "DailyAlertReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        val dayName = intent.getStringExtra(EXTRA_DAY)   ?: return
        val hhmm    = intent.getStringExtra(EXTRA_TIME)  ?: return

        Log.d(TAG, "Daily alert fired: $dayName $hhmm")

        val pendingResult = goAsync()
        val container = (context.applicationContext as OnlyFlowsApp).container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (enabled, _, _) = container.dailyAlertPreferences.snapshot()
                if (!enabled) {
                    Log.d(TAG, "Daily alerts disabled — skipping notification")
                    return@launch
                }

                val (soundEnabled, vibrationEnabled, bypassDnd) = container.notificationPreferences.snapshot()

                val notifId = abs("daily_${dayName}_${hhmm}".hashCode())

                val tapIntent = PendingIntent.getActivity(
                    context, notifId,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val channelId = if (soundEnabled || vibrationEnabled)
                    ReminderReceiver.CHANNEL_ID else ReminderReceiver.CHANNEL_ID_SILENT

                val today        = java.time.LocalDate.now().toString()
                val taskCount    = container.taskRepository.observeTodayCount().first()
                val overdueCount = container.taskRepository.observeOverdueCount().first()
                val upcoming     = container.taskRepository.getUpcomingDeadlines(today)

                val shortText    = buildString {
                    append("$taskCount task${if (taskCount != 1) "s" else ""} today")
                    if (overdueCount > 0) append(" · $overdueCount overdue")
                }
                val bigText = buildString {
                    appendLine(shortText)
                    if (upcoming.isNotEmpty()) {
                        appendLine("─────")
                        upcoming.forEach { t -> appendLine("• ${t.title}${if (t.dueDate != null) "  (${t.dueDate})" else ""}") }
                    }
                }.trimEnd()

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_nav_today)
                    .setContentTitle("Good morning — $taskCount task${if (taskCount != 1) "s" else ""} today")
                    .setContentText(shortText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setContentIntent(tapIntent)
                    .addAction(0, "View", tapIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

                if (soundEnabled) {
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                } else {
                    builder.setSound(null)
                }

                if (vibrationEnabled) {
                    builder.setVibrate(longArrayOf(0, 250, 100, 250))
                } else {
                    builder.setVibrate(longArrayOf(0))
                }

                if (bypassDnd) {
                    builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                }

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(notifId, builder.build())

                // Reschedule for the same time next week
                val dayOfWeek = runCatching { DayOfWeek.valueOf(dayName) }.getOrNull()
                if (dayOfWeek != null) {
                    DailyAlertScheduler(context).scheduleOne(dayOfWeek, hhmm)
                    Log.d(TAG, "Rescheduled daily alert: $dayName $hhmm (+7 days)")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
