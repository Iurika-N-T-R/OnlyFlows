package com.onlyflows.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    fun observePeers(): StateFlow<List<PeerInfo>>
    fun observeSyncState(): StateFlow<SyncState>
    fun observeTrustedDeviceIds(): Flow<Set<String>>
    suspend fun syncWithPeer(peer: PeerInfo)
    suspend fun trustDevice(deviceId: String)
    suspend fun untrustDevice(deviceId: String)
    suspend fun startServer()
    suspend fun stopServer()
}
