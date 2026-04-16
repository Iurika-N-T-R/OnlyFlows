package com.onlyflows.ui.screens.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.sync.SyncForegroundService
import com.onlyflows.domain.sync.PeerInfo
import com.onlyflows.domain.sync.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncUiState(
    val syncEnabled: Boolean = false,
    val thisDeviceName: String = "",
    val thisDeviceId: String = "",
    val peers: List<PeerInfo> = emptyList(),
    val syncState: SyncState = SyncState.Idle,
    val syncAttachments: Boolean = true,
    val maxAttachmentSizeMb: Int = 50,
    val trustedDeviceIds: Set<String> = emptySet(),
)

class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as OnlyFlowsApp).container
    private val syncRepo = container.syncRepository
    private val syncPrefs = container.syncPreferences

    val uiState: StateFlow<SyncUiState> = combine(
        syncPrefs.syncEnabled,
        syncPrefs.deviceName,
        syncPrefs.deviceId,
        syncRepo.observePeers(),
        syncRepo.observeSyncState(),
        syncPrefs.syncAttachments,
        syncPrefs.maxAttachmentSizeMb,
        syncRepo.observeTrustedDeviceIds(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SyncUiState(
            syncEnabled         = values[0] as Boolean,
            thisDeviceName      = values[1] as String,
            thisDeviceId        = (values[2] as String).take(8),
            peers               = values[3] as List<PeerInfo>,
            syncState           = values[4] as SyncState,
            syncAttachments     = values[5] as Boolean,
            maxAttachmentSizeMb = values[6] as Int,
            trustedDeviceIds    = values[7] as Set<String>,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch {
            syncPrefs.setSyncEnabled(enabled)
            val ctx = getApplication<Application>()
            if (enabled) {
                ctx.startForegroundService(Intent(ctx, SyncForegroundService::class.java))
            } else {
                ctx.stopService(Intent(ctx, SyncForegroundService::class.java))
            }
        }
    }

    fun setDeviceName(name: String) {
        viewModelScope.launch { syncPrefs.setDeviceName(name) }
    }

    fun syncNow(peer: PeerInfo) {
        viewModelScope.launch { syncRepo.syncWithPeer(peer) }
    }

    fun setSyncAttachments(enabled: Boolean) {
        viewModelScope.launch { syncPrefs.setSyncAttachments(enabled) }
    }

    fun setMaxAttachmentSizeMb(mb: Int) {
        viewModelScope.launch { syncPrefs.setMaxAttachmentSizeMb(mb) }
    }

    fun trustDevice(deviceId: String) {
        viewModelScope.launch { syncRepo.trustDevice(deviceId) }
    }

    fun untrustDevice(deviceId: String) {
        viewModelScope.launch { syncRepo.untrustDevice(deviceId) }
    }
}
