package com.onlyflows.data.notifications

import android.content.Context
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Reminder
import com.onlyflows.domain.model.ReminderRecurrence
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime

/** Attaches a reminder to an existing task: stores it, arms the alarm, registers any geofence
 *  and flags the task as having reminders.
 *
 *  Every one of those steps is required for a reminder to actually fire, and they used to live
 *  only inside TaskDetailViewModel — which is why the add-task flow could not create one without
 *  first navigating to the detail screen. This is the single place that knows the full sequence;
 *  every creation path calls it instead of repeating four steps that are easy to get wrong. */
object ReminderCreator {

    suspend fun attach(
        context: Context,
        taskId: String,
        at: LocalDateTime,
        locationId: String? = null,
        recurrence: ReminderRecurrence = ReminderRecurrence.NONE,
    ) {
        val container = (context.applicationContext as OnlyFlowsApp).container
        // Creation screens insert the task asynchronously, so it may not exist yet when this
        // runs. Wait for it rather than dropping the reminder on the floor; the timeout stops
        // a never-inserted id from hanging the coroutine forever.
        val task = withTimeoutOrNull(5_000) {
            container.taskRepository.observeById(taskId).filterNotNull().first()
        } ?: return

        val reminderId = "rem_${System.currentTimeMillis()}"
        container.reminderRepository.add(
            Reminder(
                id             = reminderId,
                taskId         = taskId,
                scheduledAt    = at,
                locationId     = locationId,
                recurrenceType = recurrence,
            )
        )
        container.reminderScheduler.schedule(reminderId, taskId, task.title, task.note, at, recurrence)

        locationId?.let { id ->
            container.savedLocationRepository.getById(id)?.let { loc ->
                container.geofenceManager.register(loc.id, loc.latitude, loc.longitude, loc.radiusMeters)
            }
        }

        container.taskRepository.update(
            task.copy(hasReminders = true, updatedAt = LocalDateTime.now())
        )
    }
}
