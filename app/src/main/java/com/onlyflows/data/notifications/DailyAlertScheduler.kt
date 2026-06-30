package com.onlyflows.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class DailyAlertScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun scheduleAll(enabled: Boolean, times: Set<String>, days: Set<String>) {
        cancelAll(times, days)
        if (!enabled || times.isEmpty() || days.isEmpty()) return

        for (dayName in days) {
            val dayOfWeek = runCatching { DayOfWeek.valueOf(dayName) }.getOrNull() ?: continue
            for (hhmm in times) {
                scheduleOne(dayOfWeek, hhmm)
            }
        }
    }

    fun scheduleOne(dayOfWeek: DayOfWeek, hhmm: String) {
        val time = runCatching { LocalTime.parse(hhmm, timeFormatter) }.getOrNull() ?: return
        val triggerMs = nextOccurrenceMs(dayOfWeek, time)
        val intent = buildIntent(dayOfWeek.name, hhmm)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                    Log.d(TAG, "Daily alert scheduled: $dayOfWeek $hhmm")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                    Log.w(TAG, "Daily alert (inexact, no exact-alarm permission): $dayOfWeek $hhmm")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
                Log.d(TAG, "Daily alert scheduled: $dayOfWeek $hhmm")
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
            Log.e(TAG, "SecurityException scheduling daily alert $dayOfWeek $hhmm, used inexact fallback", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule daily alert $dayOfWeek $hhmm", e)
        }
    }

    fun cancelAll(times: Set<String>, days: Set<String>) {
        for (dayName in days) {
            val dayOfWeek = runCatching { DayOfWeek.valueOf(dayName) }.getOrNull() ?: continue
            for (hhmm in times) {
                cancel(dayOfWeek.name, hhmm)
            }
        }
    }

    fun cancel(dayName: String, hhmm: String) {
        val intent = buildIntent(dayName, hhmm)
        alarmManager.cancel(intent)
        Log.d(TAG, "Daily alert cancelled: $dayName $hhmm")
    }

    private fun nextOccurrenceMs(dayOfWeek: DayOfWeek, time: LocalTime): Long {
        val zone = ZoneId.systemDefault()
        val now  = ZonedDateTime.now(zone)
        var candidate = now.toLocalDate().with(dayOfWeek).atTime(time).atZone(zone)

        // If the candidate is in the past (or within 60s), advance by 7 days
        if (!candidate.isAfter(now.plusSeconds(60))) {
            candidate = candidate.plusWeeks(1)
        }

        // If today is the right day but time already passed, the +7d above handles it.
        // If today is before the target weekday, LocalDate.with() gives us the same week.
        // But if today is after the target weekday, LocalDate.with() gives the previous occurrence —
        // so we need to add 7 days in that case too.
        if (candidate.isBefore(now)) {
            candidate = candidate.plusWeeks(1)
        }

        return candidate.toInstant().toEpochMilli()
    }

    fun scheduleWeeklyRecap(dayOfWeek: DayOfWeek, hhmm: String) {
        val time = runCatching { LocalTime.parse(hhmm, timeFormatter) }.getOrNull() ?: return
        val triggerMs = nextOccurrenceMs(dayOfWeek, time)
        val intent = Intent(context, WeeklyRecapReceiver::class.java).apply {
            action = WeeklyRecapReceiver.ACTION
            putExtra(WeeklyRecapReceiver.EXTRA_TIME, hhmm)
        }
        val pending = PendingIntent.getBroadcast(
            context, abs("weekly_recap_${hhmm}".hashCode()), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pending)
            }
            Log.d(TAG, "Weekly recap scheduled: $dayOfWeek $hhmm")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule weekly recap", e)
        }
    }

    fun cancelWeeklyRecap(hhmm: String) {
        val intent = Intent(context, WeeklyRecapReceiver::class.java).apply { action = WeeklyRecapReceiver.ACTION }
        val pending = PendingIntent.getBroadcast(
            context, abs("weekly_recap_${hhmm}".hashCode()), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    private fun buildIntent(dayName: String, hhmm: String): PendingIntent {
        val intent = Intent(context, DailyAlertReceiver::class.java).apply {
            action = DailyAlertReceiver.ACTION
            putExtra(DailyAlertReceiver.EXTRA_DAY, dayName)
            putExtra(DailyAlertReceiver.EXTRA_TIME, hhmm)
        }
        val requestCode = abs("daily_${dayName}_${hhmm}".hashCode())
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "DailyAlertScheduler"
    }
}
