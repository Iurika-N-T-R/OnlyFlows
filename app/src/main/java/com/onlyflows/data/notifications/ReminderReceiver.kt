package com.onlyflows.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.R
import com.onlyflows.domain.model.ReminderRecurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID          = "onlyflows_reminders"
        const val CHANNEL_ID_SILENT   = "onlyflows_reminders_silent"
        const val ACTION_SHOW         = "com.onlyflows.ACTION_SHOW_REMINDER"
        const val ACTION_COMPLETE     = "com.onlyflows.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE       = "com.onlyflows.ACTION_SNOOZE_REMINDER"
        const val EXTRA_REMINDER_ID      = "reminder_id"
        const val EXTRA_TASK_ID          = "task_id"
        const val EXTRA_TITLE            = "task_title"
        const val EXTRA_NOTE             = "task_note"
        const val EXTRA_SCHEDULED_AT     = "scheduled_at_ms"
        const val EXTRA_RECURRENCE_TYPE  = "recurrence_type"

        private const val TAG = "ReminderReceiver"

        /** Call from OnlyFlowsApp.onCreate() to set up both channels */
        fun ensureChannels(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Full sound+vibration channel
            val fullChannel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for task reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttrs)
            }

            // Silent channel (no sound, no vibration)
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                "Task Reminders (silent)",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Quiet reminders — no sound or vibration"
                enableVibration(false)
                setSound(null, null)
            }

            nm.createNotificationChannel(fullChannel)
            nm.createNotificationChannel(silentChannel)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        when (intent.action) {
            ACTION_SHOW     -> showNotification(context, taskId, intent)
            ACTION_COMPLETE -> completeTask(context, taskId, intent)
            ACTION_SNOOZE   -> snoozeReminder(context, taskId, intent)
        }
    }

    private fun showNotification(context: Context, taskId: String, intent: Intent) {
        val pendingResult  = goAsync()
        val title          = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val note           = intent.getStringExtra(EXTRA_NOTE)?.takeIf { it.isNotBlank() }
        val scheduledAtMs  = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
        val reminderId     = intent.getStringExtra(EXTRA_REMINDER_ID)
        val recurrenceType = intent.getStringExtra(EXTRA_RECURRENCE_TYPE)
            ?.let { runCatching { ReminderRecurrence.valueOf(it) }.getOrNull() }
            ?: ReminderRecurrence.NONE
        val notifId        = abs(taskId.hashCode())

        Log.d(TAG, "Showing notification for task $taskId: '$title'")

        val notifPrefs = (context.applicationContext as OnlyFlowsApp).container.notificationPreferences

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (soundEnabled, vibrationEnabled, bypassDnd) = notifPrefs.snapshot()

                // Tap → open app
                val tapIntent = PendingIntent.getActivity(
                    context, notifId,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("task_id", taskId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val completeIntent = PendingIntent.getBroadcast(
                    context, notifId + 1,
                    Intent(context, ReminderReceiver::class.java).apply {
                        action = ACTION_COMPLETE
                        putExtra(EXTRA_TASK_ID, taskId)
                        if (reminderId != null) putExtra(EXTRA_REMINDER_ID, reminderId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val snoozeIntent = PendingIntent.getBroadcast(
                    context, notifId + 2,
                    Intent(context, ReminderReceiver::class.java).apply {
                        action = ACTION_SNOOZE
                        putExtra(EXTRA_TASK_ID, taskId)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_NOTE, note ?: "")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                // Choose channel based on sound/vibration prefs
                val channelId = if (soundEnabled || vibrationEnabled) CHANNEL_ID else CHANNEL_ID_SILENT

                val bodyText = note ?: if (scheduledAtMs > 0L) {
                    val dt = java.time.Instant.ofEpochMilli(scheduledAtMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                    "Rappel · %s".format(
                        dt.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", java.util.Locale.FRENCH))
                    )
                } else null

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_nav_today)
                    .setContentTitle(title)
                    .setContentIntent(tapIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(0, "Terminer", completeIntent)
                    .addAction(0, "Reporter 15 min", snoozeIntent)

                if (bodyText != null) {
                    builder.setContentText(bodyText)
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                }

                // Sound
                if (soundEnabled) {
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                } else {
                    builder.setSound(null)
                }

                // Vibration
                if (vibrationEnabled) {
                    builder.setVibrate(longArrayOf(0, 250, 100, 250))
                } else {
                    builder.setVibrate(longArrayOf(0))
                }

                // Bypass DND (Android 8+)
                if (bypassDnd) {
                    builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                }

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(notifId, builder.build())

                if (recurrenceType != ReminderRecurrence.NONE && reminderId != null) {
                    val container = (context.applicationContext as OnlyFlowsApp).container
                    val task = container.taskRepository.observeById(taskId).first()
                    if (task != null && !task.isCompleted) {
                        val current = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(scheduledAtMs), ZoneId.systemDefault()
                        )
                        val next = when (recurrenceType) {
                            ReminderRecurrence.DAILY  -> current.plusDays(1)
                            ReminderRecurrence.WEEKLY -> current.plusWeeks(1)
                            else                      -> null
                        }
                        if (next != null) {
                            val existing = container.reminderRepository.getById(reminderId)
                            if (existing != null) {
                                val updated = existing.copy(
                                    scheduledAt = next,
                                    updatedAt   = LocalDateTime.now(),
                                )
                                container.reminderRepository.update(updated)
                                ReminderScheduler(context).schedule(
                                    reminderId     = reminderId,
                                    taskId         = taskId,
                                    title          = title,
                                    note           = note,
                                    at             = next,
                                    recurrenceType = recurrenceType,
                                )
                                Log.d(TAG, "Rescheduled recurring reminder $reminderId → $next")
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun completeTask(context: Context, taskId: String, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        val pendingResult = goAsync()
        val repo = (context.applicationContext as OnlyFlowsApp).container.taskRepository
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.setCompleted(taskId, true)
                Log.d(TAG, "Task $taskId marked completed via notification action")
            } finally {
                pendingResult.finish()
            }
        }
        val scheduler = ReminderScheduler(context)
        if (reminderId != null) scheduler.cancelByReminderId(reminderId)
        scheduler.cancel(taskId)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(abs(taskId.hashCode()))
    }

    private fun snoozeReminder(context: Context, taskId: String, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: taskId
        val title      = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val note       = intent.getStringExtra(EXTRA_NOTE) ?: ""
        val snoozeAt   = LocalDateTime.now().plusMinutes(15)
        ReminderScheduler(context).schedule(reminderId, taskId, title, note, snoozeAt)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(abs(taskId.hashCode()))
        Log.d(TAG, "Task $taskId snoozed until $snoozeAt")
    }
}
