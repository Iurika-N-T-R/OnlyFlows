package com.onlyflows.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(taskId: String, title: String, note: String?, at: LocalDateTime) {
        val triggerMs = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMs <= System.currentTimeMillis()) {
            Log.w(TAG, "Reminder for '$title' is in the past — skipped (at=$at)")
            return
        }

        val intent = buildIntent(taskId, title, note, triggerMs)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+: need explicit user grant
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                    Log.d(TAG, "Exact alarm set for '$title' at $at")
                } else {
                    // Best effort — will fire within ~5 min (still works, just not pinpoint)
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                    Log.w(TAG, "Inexact alarm (no exact-alarm permission) for '$title' at $at")
                }
            } else {
                // API 21-30: setExactAndAllowWhileIdle works without any special permission
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                Log.d(TAG, "Exact alarm set for '$title' at $at (API ${Build.VERSION.SDK_INT})")
            }
        } catch (e: SecurityException) {
            // Permission denied at runtime — fall back
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
            Log.e(TAG, "SecurityException scheduling alarm for '$title', used inexact fallback", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for '$title'", e)
        }
    }

    fun cancel(taskId: String) {
        val intent = buildIntent(taskId, "", null, 0L)
        alarmManager.cancel(intent)
        Log.d(TAG, "Alarm cancelled for task $taskId")
    }

    private fun buildIntent(taskId: String, title: String, note: String?, scheduledAtMs: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_NOTE, note ?: "")
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAtMs)
        }
        return PendingIntent.getBroadcast(
            context,
            abs(taskId.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "ReminderScheduler"
    }
}
