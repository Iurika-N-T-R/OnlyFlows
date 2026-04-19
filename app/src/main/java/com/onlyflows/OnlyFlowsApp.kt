package com.onlyflows

import android.app.Application
import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.notifications.ReminderReceiver
import com.onlyflows.data.syncthing.SyncthingImportWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class OnlyFlowsApp : Application() {
    lateinit var container: AppContainer
        private set

    private val _addTaskRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val addTaskRequested: SharedFlow<Unit> = _addTaskRequested.asSharedFlow()

    fun requestAddTask() { _addTaskRequested.tryEmit(Unit) }

    override fun onCreate() {
        super.onCreate()
        installUncaughtExceptionLogger()
        container = AppContainer(this)
        container.syncthingCoordinator.start()
        SyncthingImportWorker.schedule(this)
        ReminderReceiver.ensureChannels(this)
        AppLog.i("OnlyFlowsApp", "App started")
    }

    private fun installUncaughtExceptionLogger() {
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLog.e("Crash", "Uncaught exception on ${thread.name}", throwable)
            default?.uncaughtException(thread, throwable)
        }
    }
}
