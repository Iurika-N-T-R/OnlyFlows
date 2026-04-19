package com.onlyflows.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.syncthing.ImportResult
import com.onlyflows.data.syncthing.SyncthingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncthingUiState(
    val isSyncing: Boolean = false,
    val lastResult: ImportResult? = null,
    val errorMessage: String? = null,
)

class SyncthingViewModel(app: Application) : AndroidViewModel(app) {

    private val container   = (app as OnlyFlowsApp).container
    private val prefs       = container.syncthingPreferences
    private val importer    = container.syncthingImporter
    private val resolver    = container.conflictResolver
    private val monitor     = container.syncthingStatusMonitor
    private val coordinator = container.syncthingCoordinator
    private val exporter    = container.syncthingExporter

    val status: StateFlow<SyncthingStatus> = combine(
        monitor.status,
        coordinator.isImporting,
    ) { status, importing ->
        if (importing && status == SyncthingStatus.Ready) SyncthingStatus.Importing else status
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncthingStatus.FolderNotSet)

    val folderUri: StateFlow<String> = prefs.folderUri
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val enabled: StateFlow<Boolean> = prefs.enabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lastScannedAt: StateFlow<Long> = prefs.lastScannedAt
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val _ui = MutableStateFlow(SyncthingUiState())
    val ui: StateFlow<SyncthingUiState> = _ui

    fun setFolderUri(uri: String) {
        viewModelScope.launch {
            val isFirst = prefs.folderUri.first().isBlank()
            prefs.setFolderUri(uri)
            if (isFirst) {
                // Export all existing data so the other device can import it
                runCatching { exporter.exportAll() }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setEnabled(enabled) }
    }

    fun forceSyncNow() {
        if (_ui.value.isSyncing) return
        viewModelScope.launch {
            _ui.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                resolver.resolveAll()
                val result = importer.importAll()
                prefs.setLastScannedAt(System.currentTimeMillis())
                _ui.update { it.copy(isSyncing = false, lastResult = result) }
            } catch (e: Exception) {
                _ui.update { it.copy(isSyncing = false, errorMessage = e.message) }
            }
        }
    }
}
