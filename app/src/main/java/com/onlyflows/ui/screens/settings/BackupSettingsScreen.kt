package com.onlyflows.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.backup.BackupManager
import com.onlyflows.data.backup.BackupWorker
import com.onlyflows.data.preferences.BackupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ── State ─────────────────────────────────────────────────────────────────────

data class BackupSettingsUiState(
    val folderUri: String            = "",
    val folderDisplayName: String    = "",
    val autoBackupEnabled: Boolean   = false,
    val intervalDays: Int            = 1,
    val lastBackupMs: Long           = 0L,
    val isBackingUp: Boolean         = false,
    val snackbar: String?            = null,
    val showRestoreConfirm: Boolean  = false,
    val pendingRestoreUri: Uri?      = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class BackupSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container  = (app as OnlyFlowsApp).container
    private val prefs: BackupPreferences = container.backupPreferences
    private val manager    = BackupManager(app)
    private val workManager = WorkManager.getInstance(app)

    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.folderUri,
                prefs.autoBackupEnabled,
                prefs.intervalDays,
                prefs.lastBackupMs,
            ) { uri, enabled, interval, lastMs ->
                listOf<Any>(uri, enabled, interval, lastMs)
            }.collect { args ->
                val uri      = args[0] as String
                val enabled  = args[1] as Boolean
                val interval = args[2] as Int
                val lastMs   = args[3] as Long
                val displayName = if (uri.isNotBlank())
                    manager.folderDisplayName(Uri.parse(uri)) else ""
                _uiState.update {
                    it.copy(
                        folderUri         = uri,
                        folderDisplayName = displayName,
                        autoBackupEnabled = enabled,
                        intervalDays      = interval,
                        lastBackupMs      = lastMs,
                    )
                }
            }
        }
    }

    fun onFolderPicked(uri: Uri) {
        // Persist permission so we can write here even after reboot
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
        viewModelScope.launch {
            prefs.setFolderUri(uri.toString())
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoBackupEnabled(enabled)
            if (enabled) scheduleAutoBackup(_uiState.value.intervalDays)
            else workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    fun setInterval(days: Int) {
        viewModelScope.launch {
            prefs.setIntervalDays(days)
            if (_uiState.value.autoBackupEnabled) scheduleAutoBackup(days)
        }
    }

    fun backupNow() {
        val uriStr = _uiState.value.folderUri
        if (uriStr.isBlank()) {
            _uiState.update { it.copy(snackbar = "Choisissez d'abord un dossier de sauvegarde") }
            return
        }
        _uiState.update { it.copy(isBackingUp = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                manager.backup(Uri.parse(uriStr))
            }
            result.fold(
                onSuccess = { fileName ->
                    prefs.setLastBackupMs(System.currentTimeMillis())
                    _uiState.update { it.copy(isBackingUp = false, snackbar = "Sauvegarde créée : $fileName") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isBackingUp = false, snackbar = "Erreur : ${e.message}") }
                },
            )
        }
    }

    fun onRestoreFilePicked(uri: Uri) {
        _uiState.update { it.copy(showRestoreConfirm = true, pendingRestoreUri = uri) }
    }

    fun confirmRestore() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update { it.copy(showRestoreConfirm = false, pendingRestoreUri = null, isBackingUp = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { manager.restore(uri) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isBackingUp = false, snackbar = "Restauration réussie — redémarrez l'application") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isBackingUp = false, snackbar = "Erreur restauration : ${e.message}") }
                },
            )
        }
    }

    fun cancelRestore() {
        _uiState.update { it.copy(showRestoreConfirm = false, pendingRestoreUri = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbar = null) }
    }

    private fun scheduleAutoBackup(intervalDays: Int) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalDays.toLong(), TimeUnit.DAYS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        private const val WORK_NAME = "onlyflows_auto_backup"
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: BackupSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF folder picker
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.onFolderPicked(uri)
    }

    // SAF file picker for restore
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.onRestoreFilePicked(uri)
    }

    LaunchedEffect(state.snackbar) {
        val msg = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    if (state.showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title   = { Text("Restaurer la sauvegarde ?") },
            text    = { Text("Toutes les données actuelles seront remplacées. Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) {
                    Text("Restaurer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sauvegarde",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {

            // ── Dossier ──────────────────────────────────────────────────────
            SectionLabel("Dossier de sauvegarde")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { folderPicker.launch(
                        if (state.folderUri.isNotBlank()) Uri.parse(state.folderUri) else null
                    ) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Choisir un dossier", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text  = if (state.folderDisplayName.isNotBlank()) state.folderDisplayName else "Non défini",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.folderDisplayName.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Divider()

            // ── Sauvegarde manuelle ───────────────────────────────────────────
            SectionLabel("Sauvegarde manuelle")

            val lastFmt = remember(state.lastBackupMs) {
                if (state.lastBackupMs == 0L) "Jamais"
                else Instant.ofEpochMilli(state.lastBackupMs)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dernière sauvegarde", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text  = lastFmt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick  = viewModel::backupNow,
                    enabled  = !state.isBackingUp && state.folderUri.isNotBlank(),
                ) {
                    if (state.isBackingUp) {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color     = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sauvegarder")
                    }
                }
            }
            Divider()

            // ── Sauvegarde automatique ────────────────────────────────────────
            SectionLabel("Sauvegarde automatique")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Activer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Sauvegarde automatique en arrière-plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked         = state.autoBackupEnabled,
                    onCheckedChange = viewModel::setAutoBackup,
                    enabled         = state.folderUri.isNotBlank(),
                )
            }

            if (state.autoBackupEnabled) {
                SectionLabel("Fréquence")
                listOf(1 to "Quotidienne", 7 to "Hebdomadaire").forEach { (days, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setInterval(days) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        RadioButton(
                            selected  = state.intervalDays == days,
                            onClick   = { viewModel.setInterval(days) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Divider()
            }

            // ── Restauration ──────────────────────────────────────────────────
            SectionLabel("Restauration")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePicker.launch(arrayOf("application/octet-stream", "*/*")) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Restaurer depuis une sauvegarde",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "Remplace toutes les données actuelles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Divider()
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant,
    )
}
