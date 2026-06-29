package com.onlyflows.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.local.entity.PairedDeviceEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LanSyncUiState(
    val enabled: Boolean = false,
    val deviceName: String = "",
    val deviceId: String = "",
    val pairedDevices: List<PairedDeviceEntity> = emptyList(),
    val pairingCode: String? = null,
    val pairingCodeExpiryMs: Long = 0L,
    val discoveredPeers: List<Triple<String, Int, String>> = emptyList(),
    val pairResult: String? = null,   // "ok" | "error" | null
    val isSyncing: Boolean = false,
)

class LanSyncViewModel(app: Application) : AndroidViewModel(app) {

    private val container   = (app as OnlyFlowsApp).container
    private val coordinator = container.lanSyncCoordinator
    private val prefs       = container.lanSyncPreferences
    private val dao         = (app as OnlyFlowsApp).container.let {
        com.onlyflows.data.local.AppDatabase.getInstance(app).pairedDeviceDao()
    }

    val uiState: StateFlow<LanSyncUiState> = combine(
        prefs.enabled,
        prefs.deviceName,
        dao.observeAll(),
        coordinator.discoveredPeers,
        coordinator.pairingCode,
        coordinator.pairingCodeExpiryMs,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        LanSyncUiState(
            enabled             = args[0] as Boolean,
            deviceName          = args[1] as String,
            pairedDevices       = args[2] as List<PairedDeviceEntity>,
            discoveredPeers     = args[3] as List<Triple<String, Int, String>>,
            pairingCode         = args[4] as String?,
            pairingCodeExpiryMs = args[5] as Long,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanSyncUiState())

    private val _deviceIdState = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceIdState

    init {
        viewModelScope.launch { _deviceIdState.value = prefs.getDeviceId() }
    }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setEnabled(enabled) }

    fun setDeviceName(name: String) = viewModelScope.launch { prefs.setDeviceName(name) }

    fun generateCode() { coordinator.generatePairingCode() }

    fun rescan() { coordinator.rescan() }

    fun forceSync() {
        viewModelScope.launch { coordinator.forceSync() }
    }

    fun unpair(id: String) = viewModelScope.launch { coordinator.unpairDevice(id) }

    fun pairWithPeer(ip: String, port: Int, code: String) {
        viewModelScope.launch {
            val ok = coordinator.pairWithPeer(ip, port, code)
            // result surfaced via pairedDevices flow update
        }
    }
}
