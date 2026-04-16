package com.onlyflows.data.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.GeofencingEvent
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val triggeredIds = event.triggeringGeofences?.map { it.requestId } ?: return

        val container      = (context.applicationContext as OnlyFlowsApp).container
        val reminderRepo   = container.reminderRepository
        val locationRepo   = container.savedLocationRepository

        CoroutineScope(Dispatchers.IO).launch {
            triggeredIds.forEach { locationId ->
                val location = locationRepo.getById(locationId) ?: return@forEach
                // Find all future reminders linked to this location
                val reminders = reminderRepo.getFutureReminders()
                    .filter { it.locationId == locationId }

                reminders.forEach { reminder ->
                    showNotification(context, location.label, reminder.id.hashCode())
                }
            }
        }
    }

    private fun showNotification(context: Context, locationLabel: String, notifId: Int) {
        val channelId = ReminderReceiver.CHANNEL_ID
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("You arrived at $locationLabel")
            .setContentText("You have a reminder linked to this location.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification)
    }
}
