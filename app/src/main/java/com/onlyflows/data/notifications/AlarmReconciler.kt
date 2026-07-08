package com.onlyflows.data.notifications

import android.content.Context
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.debug.AppLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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
        taskRepo.getUpcomingDeadlines(LocalDate.now().toString()).forEach { t ->
            val due  = t.dueDate ?: return@forEach
            val time = t.dueTime ?: return@forEach
            val fireAt = LocalDateTime.of(due, time).minusMinutes(lead.toLong())
            if (fireAt.isAfter(LocalDateTime.now())) {
                scheduler.scheduleImminence("imm_${t.id}", t.id, t.title, fireAt)
            }
        }

        // 6. Weekly recap
        if (notifPrefs.weeklyRecapEnabled.first()) {
            alertScheduler.scheduleWeeklyRecap(DayOfWeek.SUNDAY, notifPrefs.weeklyRecapTime.first())
        }

        AppLog.i(TAG, "Reconciled alarms: $armed reminders armed, $rolled past-due recurring rolled forward")
    }
}
