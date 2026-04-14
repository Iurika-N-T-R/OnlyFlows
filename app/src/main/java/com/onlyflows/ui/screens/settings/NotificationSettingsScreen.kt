package com.onlyflows.ui.screens.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs   = (context.applicationContext as OnlyFlowsApp).container.notificationPreferences
    val scope   = rememberCoroutineScope()

    val soundEnabled     by prefs.soundEnabled.collectAsStateWithLifecycle(initialValue = true)
    val vibrationEnabled by prefs.vibrationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val bypassDnd        by prefs.bypassDnd.collectAsStateWithLifecycle(initialValue = false)

    // Runtime check: are notifications allowed at system level?
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifsEnabled = nm.areNotificationsEnabled()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sons & notifications",
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
                .padding(vertical = 8.dp),
        ) {

            // System-level notification warning
            if (!notifsEnabled) {
                Surface(
                    color  = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            context.startActivity(intent)
                        },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsOff, null,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notifications désactivées",
                                style  = MaterialTheme.typography.bodyMedium,
                                color  = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Appuyez pour activer dans les paramètres système",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Icon(
                            Icons.Outlined.OpenInNew, null,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            NotifGroupLabel("Alertes")

            NotifToggleRow(
                icon    = Icons.Outlined.VolumeUp,
                label   = "Son",
                subtitle = "Jouer un son lors des rappels",
                checked = soundEnabled,
                onCheckedChange = { scope.launch { prefs.setSoundEnabled(it) } },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            NotifToggleRow(
                icon    = Icons.Outlined.Vibration,
                label   = "Vibration",
                subtitle = "Vibrer lors des rappels",
                checked = vibrationEnabled,
                onCheckedChange = { scope.launch { prefs.setVibrationEnabled(it) } },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            NotifToggleRow(
                icon    = Icons.Outlined.DoNotDisturbOff,
                label   = "Contourner Ne pas déranger",
                subtitle = "Les rappels sonnent même en mode silencieux",
                checked = bypassDnd,
                onCheckedChange = { scope.launch { prefs.setBypassDnd(it) } },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            NotifGroupLabel("Système")

            // Direct link to system notification settings for this app
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                        }
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Outlined.Settings, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paramètres système", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Gérer sonnerie, canaux et priorité",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Outlined.OpenInNew, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun NotifGroupLabel(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun NotifToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
