package com.onlyflows.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.data.syncthing.SyncthingStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncthingSettingsScreen(
    onBack: () -> Unit = {},
    vm: SyncthingViewModel = viewModel(),
) {
    val context     = LocalContext.current
    val status      by vm.status.collectAsState()
    val folderUri   by vm.folderUri.collectAsState()
    val enabled     by vm.enabled.collectAsState()
    val lastScanned by vm.lastScannedAt.collectAsState()
    val ui          by vm.ui.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            vm.setFolderUri(uri.toString())
        }
    }

    val syncthingPackages = listOf(
        "com.github.catfriend1.syncthingandroid",
        "com.nutomic.syncthingandroid",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Synchronisation Syncthing",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status card
            StatusCard(status = status)

            // Enable toggle
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activer la synchronisation", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Synchronise automatiquement via Syncthing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = vm::setEnabled)
                }
            }

            // Folder picker
            SectionCard {
                Text(
                    "Dossier partagé",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                if (folderUri.isNotBlank()) {
                    Text(
                        text  = Uri.parse(folderUri).lastPathSegment ?: folderUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (folderUri.isBlank()) "Choisir le dossier" else "Changer le dossier")
                }
            }

            // Syncthing app install prompt
            if (status == SyncthingStatus.SyncthingNotInstalled) {
                SectionCard {
                    Text(
                        "Syncthing n'est pas installé",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Installez Syncthing (Syncthing-Fork recommandé) pour activer la synchronisation entre appareils.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.github.catfriend1.syncthingandroid"),
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            runCatching { context.startActivity(intent) }.onFailure {
                                val fallback = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.github.catfriend1.syncthingandroid"),
                                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(fallback)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Installer Syncthing-Fork")
                    }
                }
            }

            // Force sync + last scanned
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Synchronisation forcée", style = MaterialTheme.typography.bodyLarge)
                        if (lastScanned > 0L) {
                            val dt = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(lastScanned),
                                ZoneId.systemDefault(),
                            )
                            Text(
                                "Dernier scan : ${dt.format(TIME_FMT)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                "Jamais synchronisé",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (ui.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = vm::forceSyncNow, enabled = folderUri.isNotBlank()) {
                            Icon(Icons.Outlined.Sync, contentDescription = "Synchroniser maintenant")
                        }
                    }
                }

                ui.lastResult?.let { result ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${result.imported} importés · ${result.skipped} ignorés · ${result.tombstonesApplied} suppressions · ${result.errors} erreurs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ui.errorMessage?.let { err ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Erreur : $err",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Guide
            SectionCard {
                Text(
                    "Comment configurer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                GuideStep("1", "Installez Syncthing-Fork sur chaque appareil")
                GuideStep("2", "Dans Syncthing, ajoutez un dossier partagé et notez son chemin")
                GuideStep("3", "Ici, choisissez ce même dossier avec le bouton ci-dessus")
                GuideStep("4", "Activez la synchronisation — OnlyFlows détectera automatiquement les changements via Wi-Fi")
                GuideStep("5", "Associez vos appareils dans Syncthing (ID d'appareil)")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(status: SyncthingStatus) {
    val (label, color) = when (status) {
        SyncthingStatus.Ready                 -> "Prêt" to MaterialTheme.colorScheme.primary
        SyncthingStatus.FolderNotSet          -> "Dossier non configuré" to MaterialTheme.colorScheme.onSurfaceVariant
        SyncthingStatus.SyncthingNotInstalled -> "Syncthing non installé" to MaterialTheme.colorScheme.error
        SyncthingStatus.NoWifi                -> "Wi-Fi déconnecté" to MaterialTheme.colorScheme.tertiary
        SyncthingStatus.Importing             -> "Importation en cours…" to MaterialTheme.colorScheme.primary
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.SyncAlt,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text  = "Statut : $label",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun GuideStep(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text  = "$number.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
