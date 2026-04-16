package com.onlyflows.data.sync

import android.util.Log
import com.onlyflows.data.preferences.SyncPreferences
import com.onlyflows.domain.sync.SyncRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

private const val TAG = "SyncOrchestrator"
private const val RESYNC_INTERVAL_MS = 30_000L

class SyncOrchestrator(
    private val syncRepository: SyncRepository,
    private val syncPreferences: SyncPreferences,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var peerWatchJob: Job? = null
    private var periodicJob: Job? = null

    fun start() {
        Log.d(TAG, "Starting sync orchestrator")
        // Watch for new peers and sync immediately when one appears (trusted only)
        peerWatchJob = scope.launch {
            syncRepository.startServer()
            syncRepository.observePeers().collectLatest { peers ->
                val trusted = syncPreferences.trustedDeviceIds.first()
                peers.filter { it.deviceId in trusted }.forEach { peer ->
                    launch {
                        syncRepository.syncWithPeer(peer)
                    }
                }
            }
        }
        // Periodic re-sync every 30 seconds (trusted only)
        periodicJob = scope.launch {
            while (isActive) {
                delay(RESYNC_INTERVAL_MS)
                val trusted = syncPreferences.trustedDeviceIds.first()
                syncRepository.observePeers().value
                    .filter { it.deviceId in trusted }
                    .forEach { peer ->
                        launch { syncRepository.syncWithPeer(peer) }
                    }
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping sync orchestrator")
        peerWatchJob?.cancel()
        periodicJob?.cancel()
        scope.launch { syncRepository.stopServer() }
    }
}
