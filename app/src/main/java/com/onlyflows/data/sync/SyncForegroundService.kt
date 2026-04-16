package com.onlyflows.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.onlyflows.OnlyFlowsApp

class SyncForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "onlyflows_sync"
        const val NOTIF_ID = 9001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OnlyFlows Sync")
            .setContentText("LAN sync active")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
        (application as OnlyFlowsApp).container.syncOrchestrator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as OnlyFlowsApp).container.syncOrchestrator.stop()
    }

    private fun createChannel() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "LAN Sync", NotificationManager.IMPORTANCE_LOW)
                    .also { it.description = "OnlyFlows device synchronisation" }
            )
        }
    }
}
