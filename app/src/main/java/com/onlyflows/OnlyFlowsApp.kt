package com.onlyflows

import android.app.Application
import com.onlyflows.data.notifications.ReminderReceiver

class OnlyFlowsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderReceiver.ensureChannels(this)
    }
}
