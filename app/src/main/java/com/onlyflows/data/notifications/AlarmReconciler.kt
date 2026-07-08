package com.onlyflows.data.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.R
import com.onlyflows.data.debug.AppLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.onlyflows.domain.model.ReminderRecurrence

/**
 * Re-arms every alarm from DB state. Idempotent (same PendingIntent request codes replace
 * themselves), safe to call any time. This is the single recovery path for alarms lost to
 * reboot, force-stop, OEM killers, or LAN-synced reminders that bypass the repositories.
 * Called from: OnlyFlowsApp.onCreate, BootReceiver, LanSyncCoordinator.applyDelta (on change).
 */
object AlarmReconciler {
    private const val TAG = "AlarmReconciler"
    private val mutex = Mutex()

    suspend fun reconcile(context: Context) = mutex.withLock {
        val container    = (context.applicationContext as OnlyFlowsApp).container
        val taskRepo     = container.taskRepository
        val reminderRepo = container.reminderRepository
        val scheduler    = ReminderScheduler(context)
        val alertScheduler = DailyAlertScheduler(context)

        // 1. Roll past-due recurring reminders forward — a missed firing (Doze, force-stop,
        // phone off) must not kill the chain forever.
        var rolled = 0
        reminderRepo.getPastRecurring().forEach { r ->
            val task = taskRepo.observeById(r.taskId).first()
            if (task == null || task.isCompleted) return@forEach
            var next = r.scheduledAt
            val now = LocalDateTime.now()
            while (!next.isAfter(now)) {
                next = if (r.recurrenceType == ReminderRecurrence.DAILY) next.plusDays(1) else next.plusWeeks(1)
            }
            reminderRepo.update(r.copy(scheduledAt = next, updatedAt = LocalDateTime.now()))
            rolled++
        }

        // 2. Re-arm all future task reminders + their geofences
        val locationRepo    = container.savedLocationRepository
        val geofenceManager = GeofenceManager(context)
        var armed = 0
        reminderRepo.getFutureReminders().forEach { r ->
            val task = taskRepo.observeById(r.taskId).first()
            if (task == null || task.isCompleted) return@forEach
            scheduler.schedule(r.id, r.taskId, task.title, task.note, r.scheduledAt, r.recurrenceType)
            armed++
            r.locationId?.let { locId ->
                locationRepo.getById(locId)?.let { loc ->
                    geofenceManager.register(loc.id, loc.latitude, loc.longitude, loc.radiusMeters)
                }
            }
        }

        // 3. Checklist item reminders
        container.checklistItemReminderRepository.getFutureReminders().forEach { rem ->
            val item = taskRepo.getChecklistItemById(rem.checklistItemId) ?: return@forEach
            scheduler.scheduleChecklistReminder(rem.id, rem.checklistItemId, item.taskId, item.title, rem.scheduledAt)
        }

        // 4. Daily alerts
        val alertPrefs = container.dailyAlertPreferences
        alertScheduler.scheduleAll(alertPrefs.enabled.first(), alertPrefs.times.first(), alertPrefs.days.first())

        // 5. Imminence alarms for tasks with future due date+time
        val notifPrefs = container.notificationPreferences
        val lead = notifPrefs.imminenceLeadMinutes.first()
        var imminence = 0
        taskRepo.getUpcomingDeadlines(LocalDate.now().toString()).forEach { t ->
            val due  = t.dueDate ?: return@forEach
            val time = t.dueTime ?: return@forEach
            val fireAt = LocalDateTime.of(due, time).minusMinutes(lead.toLong())
            if (fireAt.isAfter(LocalDateTime.now())) {
                scheduler.scheduleImminence("imm_${t.id}", t.id, t.title, fireAt)
                imminence++
            }
        }

        // 6. Weekly recap
        if (notifPrefs.weeklyRecapEnabled.first()) {
            alertScheduler.scheduleWeeklyRecap(DayOfWeek.SUNDAY, notifPrefs.weeklyRecapTime.first())
        }

        AppLog.i(TAG, "Reconciled alarms: $armed reminders + $imminence due-time alerts armed, $rolled past-due recurring rolled forward")
        showStatus(context, armed, imminence)
    }

    private const val STATUS_CHANNEL  = "onlyflows_status"
    private const val STATUS_NOTIF_ID = 424242

    /** Persistent silent notification proving the reminder system is armed and healthy. */
    private fun showStatus(context: Context, armed: Int, imminence: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(STATUS_CHANNEL, "Notification status", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Persistent reminder-system status" }
        )
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        val tap = PendingIntent.getActivity(
            context, STATUS_NOTIF_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = buildString {
            append("$armed reminder${if (armed == 1) "" else "s"} · $imminence due-time alert${if (imminence == 1) "" else "s"} armed")
            append(if (exact) " · exact alarms ✓" else " · ⚠ inexact — grant “Alarms & reminders”")
            append(" · checked ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}")
        }
        nm.notify(
            STATUS_NOTIF_ID,
            NotificationCompat.Builder(context, STATUS_CHANNEL)
                .setSmallIcon(R.drawable.ic_nav_today)
                .setContentTitle("Reminders active")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setContentIntent(tap)
                .build(),
        )
    }
}
