package com.onlyflows.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container    = (context.applicationContext as OnlyFlowsApp).container
        val reminderRepo = container.reminderRepository
        val taskRepo     = container.taskRepository
        val scheduler    = ReminderScheduler(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Reschedule task reminders + re-register geofences
                val reminders      = reminderRepo.getFutureReminders()
                val locationRepo   = container.savedLocationRepository
                val geofenceManager = GeofenceManager(context)

                reminders.forEach { reminder ->
                    // Fetch task title/note for the notification
                    taskRepo.observeById(reminder.taskId).collect { task ->
                        if (task != null) {
                            scheduler.schedule(
                                reminderId     = reminder.id,
                                taskId         = reminder.taskId,
                                title          = task.title,
                                note           = task.note,
                                at             = reminder.scheduledAt,
                                recurrenceType = reminder.recurrenceType,
                            )
                        }
                        return@collect
                    }
                    // Re-register geofence if this reminder has a location
                    reminder.locationId?.let { locId ->
                        locationRepo.getById(locId)?.let { loc ->
                            geofenceManager.register(loc.id, loc.latitude, loc.longitude, loc.radiusMeters)
                        }
                    }
                }

                // Reschedule checklist item reminders
                val checklistReminderRepo = container.checklistItemReminderRepository
                checklistReminderRepo.getFutureReminders().forEach { reminder ->
                    val item = taskRepo.getChecklistItemById(reminder.checklistItemId)
                    if (item != null) {
                        scheduler.scheduleChecklistReminder(
                            reminderId = reminder.id,
                            itemId     = reminder.checklistItemId,
                            taskId     = item.taskId,
                            title      = item.title,
                            at         = reminder.scheduledAt,
                        )
                    }
                }

                // Reschedule daily alerts
                val alertPrefs     = container.dailyAlertPreferences
                val alertScheduler = DailyAlertScheduler(context)
                alertScheduler.scheduleAll(
                    alertPrefs.enabled.first(),
                    alertPrefs.times.first(),
                    alertPrefs.days.first(),
                )

                // Rearm imminence alarms for tasks with future due dates+times
                val notifPrefs = container.notificationPreferences
                val lead = notifPrefs.imminenceLeadMinutes.first()
                val today = LocalDate.now().toString()
                container.taskRepository.getUpcomingDeadlines(today).forEach { task ->
                    val dueTime = task.dueTime ?: return@forEach
                    val dueDate = task.dueDate ?: return@forEach
                    val fireAt = LocalDateTime.of(dueDate, dueTime).minusMinutes(lead.toLong())
                    if (fireAt.isAfter(LocalDateTime.now())) {
                        scheduler.scheduleImminence("imm_${task.id}", task.id, task.title, fireAt)
                    }
                }

                // Rearm weekly recap
                if (notifPrefs.weeklyRecapEnabled.first()) {
                    val recapTime = notifPrefs.weeklyRecapTime.first()
                    alertScheduler.scheduleWeeklyRecap(DayOfWeek.SUNDAY, recapTime)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
