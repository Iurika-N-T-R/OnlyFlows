package com.onlyflows.wear.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Receives the phone's refresh pings and forwards them to the UI via WearRefresh. */
class WearRefreshService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/onlyflows/refresh") WearRefresh.ping()
    }
}
