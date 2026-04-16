package com.onlyflows.ui.screens.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.notifications.DailyAlertScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek

private val DAY_LABELS = listOf(
    DayOfWeek.MONDAY    to "L",
    DayOfWeek.TUESDAY   to "M",
    DayOfWeek.WEDNESDAY to "M",
    DayOfWeek.THURSDAY  to "J",
    DayOfWeek.FRIDAY    to "V",
    DayOfWeek.SATURDAY  to "S",
    DayOfWeek.SUNDAY    to "D",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val container = (context.applicationContext as OnlyFlowsApp).container
    val prefs   = container.notificationPreferences
    val alertPrefs = container.dailyAlertPreferences
    val scope   = rememberCoroutineScope()

    val soundEnabled     by prefs.soundEnabled.collectAsStateWithLifecycle(initialValue = true)
    val vibrationEnabled by prefs.vibrationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val bypassDnd        by prefs.bypassDnd.collectAsStateWithLifecycle(initialValue = false)

    val alertEnabled by alertPrefs.enabled.collectAsStateWithLifecycle(initialValue = false)
    val alertTimes   by alertPrefs.times.collectAsStateWithLifecycle(initialValue = emptySet())
    val alertDays    by alertPrefs.days.collectAsStateWithLifecycle(initialValue = emptySet())

    // Runtime check: are notifications allowed at system level?
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifsEnabled = nm.areNotificationsEnabled()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)

    fun rescheduleAll(enabled: Boolean, times: Set<String>, days: Set<String>) {
        DailyAlertScheduler(context).scheduleAll(enabled, times, days)
    }

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
                .verticalScroll(rememberScrollState())
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

            // ── Daily alerts section ─────────────────────────────────────────
            NotifGroupLabel("Alertes quotidiennes")

            NotifToggleRow(
                icon     = Icons.Outlined.Alarm,
                label    = "Alertes quotidiennes",
                subtitle = "Recevoir une alerte à heures fixes",
                checked  = alertEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        alertPrefs.setEnabled(enabled)
                        rescheduleAll(enabled, alertPrefs.times.first(), alertPrefs.days.first())
                    }
                },
            )

            if (alertEnabled) {
                // Day chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    DAY_LABELS.forEach { (day, label) ->
                        val selected = day.name in alertDays
                        FilterChip(
                            selected = selected,
                            onClick  = {
                                scope.launch {
                                    alertPrefs.toggleDay(day.name)
                                    val newDays = alertPrefs.days.first()
                                    rescheduleAll(alertEnabled, alertTimes, newDays)
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                // Time list
                val sortedTimes = alertTimes.sortedBy { it }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    sortedTimes.forEach { hhmm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(
                                text  = hhmm,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        alertPrefs.removeTime(hhmm)
                                        val newTimes = alertPrefs.times.first()
                                        rescheduleAll(alertEnabled, newTimes, alertDays)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Close, "Supprimer",
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Add time button
                    TextButton(
                        onClick  = { showTimePicker = true },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter un horaire")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // ── System section ───────────────────────────────────────────────
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

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    val hh = timePickerState.hour.toString().padStart(2, '0')
                    val mm = timePickerState.minute.toString().padStart(2, '0')
                    val hhmm = "$hh:$mm"
                    scope.launch {
                        alertPrefs.addTime(hhmm)
                        val newTimes = alertPrefs.times.first()
                        rescheduleAll(alertEnabled, newTimes, alertDays)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Annuler") }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
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
