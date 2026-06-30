package com.onlyflows.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

class WeeklyRecapReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION     = "com.onlyflows.ACTION_WEEKLY_RECAP"
        const val EXTRA_TIME = "weekly_recap_time"
        private const val TAG = "WeeklyRecapReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val hhmm = intent.getStringExtra(EXTRA_TIME) ?: "20:00"

        val pendingResult = goAsync()
        val container = (context.applicationContext as OnlyFlowsApp).container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = container.notificationPreferences.weeklyRecapEnabled.first()
                if (!enabled) return@launch

                val sevenDaysAgo   = LocalDate.now().minusDays(7).toString()
                val completedCount = container.taskRepository.getCompletedThisWeekCount(sevenDaysAgo)
                val overdueCount   = container.taskRepository.observeOverdueCount().first()

                val (soundEnabled, vibrationEnabled, bypassDnd) = container.notificationPreferences.snapshot()
                val channelId = if (soundEnabled || vibrationEnabled) ReminderReceiver.CHANNEL_ID else ReminderReceiver.CHANNEL_ID_SILENT
                val notifId = abs("weekly_recap".hashCode())

                val tapIntent = PendingIntent.getActivity(
                    context, notifId,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val bigText = "✅ $completedCount completed this week" +
                    if (overdueCount > 0) "\n⚠️ $overdueCount overdue" else ""

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_nav_today)
                    .setContentTitle("OnlyFlows Weekly Recap")
                    .setContentText("$completedCount completed · $overdueCount overdue")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setContentIntent(tapIntent)
                    .addAction(0, "View report", tapIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                if (soundEnabled) builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                if (vibrationEnabled) builder.setVibrate(longArrayOf(0, 200))
                if (bypassDnd) builder.setCategory(NotificationCompat.CATEGORY_ALARM)

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(notifId, builder.build())

                // Reschedule for next Sunday
                DailyAlertScheduler(context).scheduleWeeklyRecap(DayOfWeek.SUNDAY, hhmm)
                Log.d(TAG, "Weekly recap sent and rescheduled for next Sunday $hhmm")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
