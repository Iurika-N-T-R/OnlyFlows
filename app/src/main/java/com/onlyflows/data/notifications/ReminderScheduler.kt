package com.onlyflows.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.onlyflows.domain.model.ReminderRecurrence
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule an alarm keyed by [reminderId].
     * Recurring reminders pass a non-NONE [recurrenceType]; the Receiver reschedules them
     * automatically until the task is completed.
     */
    fun schedule(
        reminderId: String,
        taskId: String,
        title: String,
        note: String?,
        at: LocalDateTime,
        recurrenceType: ReminderRecurrence = ReminderRecurrence.NONE,
    ) {
        val triggerMs = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMs <= System.currentTimeMillis()) {
            Log.w(TAG, "Reminder for '$title' is in the past — skipped (at=$at)")
            return
        }

        val intent = buildIntent(reminderId, taskId, title, note, triggerMs, recurrenceType)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                    Log.d(TAG, "Exact alarm set for '$title' at $at (reminderId=$reminderId recurrence=$recurrenceType)")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                    Log.w(TAG, "Inexact alarm (no exact-alarm permission) for '$title' at $at")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                Log.d(TAG, "Exact alarm set for '$title' at $at (API ${Build.VERSION.SDK_INT})")
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
            Log.e(TAG, "SecurityException scheduling alarm for '$title', used inexact fallback", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for '$title'", e)
        }
    }

    /** Cancel an alarm keyed by reminderId (new-style, post-v31). */
    fun cancelByReminderId(reminderId: String) {
        val intent = buildCancelIntent(abs(reminderId.hashCode()))
        alarmManager.cancel(intent)
        Log.d(TAG, "Alarm cancelled for reminderId=$reminderId")
    }

    /** Legacy cancel keyed by taskId (pre-v31 alarms). */
    fun cancel(taskId: String) {
        val intent = buildCancelIntent(abs(taskId.hashCode()))
        alarmManager.cancel(intent)
        Log.d(TAG, "Legacy alarm cancelled for taskId=$taskId")
    }

    private fun buildIntent(
        reminderId: String,
        taskId: String,
        title: String,
        note: String?,
        scheduledAtMs: Long,
        recurrenceType: ReminderRecurrence,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_NOTE, note ?: "")
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAtMs)
            putExtra(ReminderReceiver.EXTRA_RECURRENCE_TYPE, recurrenceType.name)
        }
        return PendingIntent.getBroadcast(
            context,
            abs(reminderId.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildCancelIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "ReminderScheduler"
    }
}
