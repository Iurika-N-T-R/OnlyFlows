package com.onlyflows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.sync.PeerInfo
import com.onlyflows.domain.sync.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit = {},
    vm: SyncViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var deviceNameInput by remember(state.thisDeviceName) { mutableStateOf(state.thisDeviceName) }
    var showSizePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Synchronisation LAN",
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Enable toggle ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Activer la sync LAN",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Synchronise automatiquement avec les appareils sur le même Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked         = state.syncEnabled,
                        onCheckedChange = vm::toggleSync,
                    )
                }
            }

            // ── Device name ────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = deviceNameInput,
                    onValueChange = { deviceNameInput = it },
                    label         = { Text("Nom de cet appareil") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        vm.setDeviceName(deviceNameInput)
                        focusManager.clearFocus()
                    }),
                    trailingIcon  = {
                        Text(
                            state.thisDeviceId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                )
            }

            // ── Sync status ────────────────────────────────────────────────────
            item {
                SyncStatusCard(state.syncState)
            }

            // ── Attachment sync ────────────────────────────────────────────────
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                SettingsGroupLabel("Pièces jointes")
            }
            item {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Synchroniser les fichiers", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked         = state.syncAttachments,
                        onCheckedChange = vm::setSyncAttachments,
                    )
                }
            }
            if (state.syncAttachments) {
                item {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Taille max par fichier", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${state.maxAttachmentSizeMb} Mo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { showSizePicker = true }) {
                            Text("Modifier")
                        }
                    }
                }
            }

            // ── Discovered peers ───────────────────────────────────────────────
            item {
                val trustedCount = state.peers.count { it.deviceId in state.trustedDeviceIds }
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                SettingsGroupLabel("Appareils détectés (${state.peers.size}) — $trustedCount autorisé(s)")
            }
            if (state.peers.isEmpty()) {
                item {
                    Text(
                        if (state.syncEnabled)
                            "Aucun appareil OnlyFlows trouvé sur ce réseau."
                        else
                            "Activez la synchronisation pour découvrir les appareils.",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(state.peers) { peer ->
                    val isTrusted = peer.deviceId in state.trustedDeviceIds
                    PeerRow(
                        peer      = peer,
                        isTrusted = isTrusted,
                        onSyncNow = { vm.syncNow(peer) },
                        onTrust   = { vm.trustDevice(peer.deviceId) },
                        onRevoke  = { vm.untrustDevice(peer.deviceId) },
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showSizePicker) {
        MaxSizeDialog(
            current   = state.maxAttachmentSizeMb,
            onConfirm = { vm.setMaxAttachmentSizeMb(it); showSizePicker = false },
            onDismiss = { showSizePicker = false },
        )
    }
}

@Composable
private fun SyncStatusCard(syncState: SyncState) {
    val (icon, text, color) = when (syncState) {
        is SyncState.Idle       -> Triple(Icons.Outlined.Sync, "En attente", MaterialTheme.colorScheme.onSurfaceVariant)
        is SyncState.Discovering -> Triple(Icons.Outlined.Sync, "Recherche d'appareils…", MaterialTheme.colorScheme.primary)
        is SyncState.Syncing    -> Triple(Icons.Outlined.Sync, "Synchronisation avec ${syncState.peerName}…", MaterialTheme.colorScheme.primary)
        is SyncState.Done       -> Triple(Icons.Outlined.Sync, "${syncState.peersCount} appareil(s) synchronisé(s)", MaterialTheme.colorScheme.tertiary)
        is SyncState.Error      -> Triple(Icons.Outlined.SyncProblem, "Erreur : ${syncState.message}", MaterialTheme.colorScheme.error)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
        if (syncState is SyncState.Syncing || syncState is SyncState.Discovering) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun PeerRow(
    peer: PeerInfo,
    isTrusted: Boolean,
    onSyncNow: () -> Unit,
    onTrust: () -> Unit,
    onRevoke: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isTrusted) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            contentDescription = if (isTrusted) "Autorisé" else "Non autorisé",
            tint     = if (isTrusted) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(peer.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${peer.host}:${peer.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isTrusted) {
            TextButton(onClick = onSyncNow) { Text("Sync") }
            TextButton(onClick = onRevoke) {
                Text("Révoquer", color = MaterialTheme.colorScheme.error)
            }
        } else {
            TextButton(onClick = onTrust) { Text("Autoriser") }
        }
    }
}

@Composable
private fun MaxSizeDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Taille max des fichiers") },
        text    = {
            OutlinedTextField(
                value         = input,
                onValueChange = { if (it.all(Char::isDigit)) input = it },
                label         = { Text("Mo") },
                singleLine    = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input.toIntOrNull() ?: current) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
