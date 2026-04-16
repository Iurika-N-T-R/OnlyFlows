package com.onlyflows.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container    = (context.applicationContext as OnlyFlowsApp).container
        val reminderRepo = container.reminderRepository
        val taskRepo     = container.taskRepository
        val scheduler    = ReminderScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            // Reschedule task reminders + re-register geofences
            val reminders      = reminderRepo.getFutureReminders()
            val locationRepo   = container.savedLocationRepository
            val geofenceManager = GeofenceManager(context)

            reminders.forEach { reminder ->
                // Fetch task title/note for the notification
                taskRepo.observeById(reminder.taskId).collect { task ->
                    if (task != null) {
                        scheduler.schedule(reminder.taskId, task.title, task.note, reminder.scheduledAt)
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

            // Reschedule daily alerts
            val alertPrefs     = container.dailyAlertPreferences
            val alertScheduler = DailyAlertScheduler(context)
            alertScheduler.scheduleAll(
                alertPrefs.enabled.first(),
                alertPrefs.times.first(),
                alertPrefs.days.first(),
            )
        }
    }
}
