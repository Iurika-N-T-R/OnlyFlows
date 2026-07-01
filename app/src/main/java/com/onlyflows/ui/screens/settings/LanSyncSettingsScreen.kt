package com.onlyflows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanSyncSettingsScreen(
    onBack: () -> Unit = {},
    vm: LanSyncViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showPairDialog by remember { mutableStateOf(false) }
    var selectedPeer by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
    var codeInput by remember { mutableStateOf("") }
    var nameEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LAN Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = vm::rescan) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Scan network")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("LAN sync active", style = MaterialTheme.typography.bodyLarge)
                            Text("Syncs with paired devices on the same network",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.enabled, onCheckedChange = vm::setEnabled)
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("This device name", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        if (nameEditing) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    vm.setDeviceName(nameInput); nameEditing = false
                                }),
                                trailingIcon = {
                                    TextButton(onClick = { vm.setDeviceName(nameInput); nameEditing = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(state.deviceName, style = MaterialTheme.typography.bodyLarge)
                                TextButton(onClick = { nameInput = state.deviceName; nameEditing = true }) {
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Pair a device", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        if (state.pairingCode != null) {
                            val remaining = ((state.pairingCodeExpiryMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                            Text("Code on this device:", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(state.pairingCode!!, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Expires in ${remaining}s", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Generate a code on this device, then enter it on the other.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = vm::generateCode, modifier = Modifier.weight(1f)) {
                                Text("Generate code")
                            }
                            Button(
                                onClick = {
                                    selectedPeer = state.discoveredPeers.firstOrNull()
                                    showPairDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                if (state.discoveredPeers.isNotEmpty())
                                    Text("Enter code (${state.discoveredPeers.size})")
                                else
                                    Text("Enter code")
                            }
                        }
                    }
                }
            }

            if (state.pairedDevices.isNotEmpty()) {
                item {
                    Text("Paired devices", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(state.pairedDevices, key = { it.id }) { device ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                                Text(device.lastKnownIp, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.unpair(device.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { showPairDialog = false; codeInput = "" },
            title = { Text("Pair with a device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.discoveredPeers.isEmpty()) {
                        Text("Scanning… Tap ↺ if nothing appears.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else if (state.discoveredPeers.size == 1) {
                        Text("Device found: ${state.discoveredPeers.first().third}",
                            style = MaterialTheme.typography.bodyMedium)
                    } else {
                        state.discoveredPeers.forEach { peer ->
                            FilterChip(
                                selected = selectedPeer == peer,
                                onClick  = { selectedPeer = peer },
                                label    = { Text(peer.third) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) codeInput = it },
                        label = { Text("6-digit code") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val peer = selectedPeer ?: state.discoveredPeers.firstOrNull() ?: return@TextButton
                        vm.pairWithPeer(peer.first, peer.second, codeInput)
                        showPairDialog = false; codeInput = ""
                    },
                    enabled = codeInput.length == 6 &&
                        (selectedPeer != null || state.discoveredPeers.isNotEmpty()),
                ) { Text("Pair") }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false; codeInput = "" }) { Text("Cancel") }
            },
        )
    }
}