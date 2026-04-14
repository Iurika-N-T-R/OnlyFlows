package com.onlyflows.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val reminderRepo = (context.applicationContext as OnlyFlowsApp).container.reminderRepository
        val taskRepo     = (context.applicationContext as OnlyFlowsApp).container.taskRepository
        val scheduler    = ReminderScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val reminders = reminderRepo.getFutureReminders()
            reminders.forEach { reminder ->
                // Fetch task title/note for the notification
                taskRepo.observeById(reminder.taskId).collect { task ->
                    if (task != null) {
                        scheduler.schedule(reminder.taskId, task.title, task.note, reminder.scheduledAt)
                    }
                    return@collect
                }
            }
        }
    }
}
