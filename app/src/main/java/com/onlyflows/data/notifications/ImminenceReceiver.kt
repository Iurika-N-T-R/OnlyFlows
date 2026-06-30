package com.onlyflows.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class ImminenceReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW   = "com.onlyflows.ACTION_IMMINENCE"
        const val EXTRA_TASK_ID = "imminence_task_id"
        const val EXTRA_TITLE   = "imminence_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW) return
        val taskId  = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val title   = intent.getStringExtra(EXTRA_TITLE)   ?: return
        val notifId = abs("imm_$taskId".hashCode())

        val pendingResult = goAsync()
        val container = (context.applicationContext as OnlyFlowsApp).container

        val tapIntent = PendingIntent.getActivity(
            context, notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("task_id", taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (soundEnabled, vibrationEnabled, bypassDnd) = container.notificationPreferences.snapshot()
                val channelId = if (soundEnabled || vibrationEnabled) ReminderReceiver.CHANNEL_ID else ReminderReceiver.CHANNEL_ID_SILENT

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_nav_today)
                    .setContentTitle("Due soon — $title")
                    .setContentText("This task is due soon")
                    .setContentIntent(tapIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

                if (soundEnabled) builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                if (vibrationEnabled) builder.setVibrate(longArrayOf(0, 250, 100, 250))
                if (bypassDnd) builder.setCategory(NotificationCompat.CATEGORY_ALARM)

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(notifId, builder.build())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
