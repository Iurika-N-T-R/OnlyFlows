package com.onlyflows.data.syncthing

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val SYNCTHING_PACKAGES = listOf(
    "com.github.catfriend1.syncthingandroid",
    "com.nutomic.syncthingandroid",
)

sealed class SyncthingStatus {
    object FolderNotSet          : SyncthingStatus()
    object SyncthingNotInstalled : SyncthingStatus()
    object NoWifi                : SyncthingStatus()
    object Ready                 : SyncthingStatus()
    object Importing             : SyncthingStatus()
}

class SyncthingStatusMonitor(
    private val context: Context,
    private val prefs: SyncthingPreferences,
) {

    val isSyncthingInstalled: Boolean
        get() = SYNCTHING_PACKAGES.any { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }

    val syncthingPackage: String?
        get() = SYNCTHING_PACKAGES.firstOrNull { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }

    val wifiConnected: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(cm.hasWifi()) }
            override fun onLost(network: Network) { trySend(cm.hasWifi()) }
            override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) {
                trySend(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, callback)
        trySend(cm.hasWifi())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    val status: Flow<SyncthingStatus> = combine(
        prefs.folderUri,
        prefs.enabled,
        wifiConnected,
    ) { folderUri, enabled, hasWifi ->
        when {
            !enabled || folderUri.isBlank() -> SyncthingStatus.FolderNotSet
            !isSyncthingInstalled           -> SyncthingStatus.SyncthingNotInstalled
            !hasWifi                        -> SyncthingStatus.NoWifi
            else                            -> SyncthingStatus.Ready
        }
    }

    val isReady: Flow<Boolean> = status.map { it is SyncthingStatus.Ready }

    private fun ConnectivityManager.hasWifi(): Boolean {
        val net = activeNetwork ?: return false
        return getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
