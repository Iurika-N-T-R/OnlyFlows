package com.onlyflows.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onGeneralSettingsClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onCalendarSettingsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onLocationsSettingsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLogsClick: () -> Unit = {},
    onSyncthingClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onRssSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val quoteRepo = remember { (context.applicationContext as OnlyFlowsApp).container.quoteRepository }
    var quoteImportMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val quotePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val count = try { quoteRepo.importFromUri(uri) } catch (_: Exception) { 0 }
            snackbarHostState.showSnackbar("$count citations importées")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Paramètres",
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
            SettingsGroupLabel("Général")
            SettingsRow(
                icon    = Icons.Outlined.Tune,
                label   = "Général",
                onClick = onGeneralSettingsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.Palette,
                label   = "Apparence",
                onClick = onAppearanceClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.NotificationsNone,
                label   = "Sons & notifications",
                onClick = onNotificationSettingsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.CalendarMonth,
                label   = "Calendriers",
                onClick = onCalendarSettingsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.CloudUpload,
                label   = "Sauvegarde",
                onClick = onBackupClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.SyncAlt,
                label   = "Synchronisation Syncthing",
                onClick = onSyncthingClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.LocationOn,
                label   = "Locations",
                onClick = onLocationsSettingsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.BarChart,
                label   = "Statistiques",
                onClick = onStatisticsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.Label,
                label   = "Tags",
                onClick = onTagsClick,
            )
            Divider()
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.RssFeed,
                label   = "Flux RSS",
                onClick = onRssSettingsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.FormatQuote,
                label   = "Citations — importer JSON",
                onClick = { quotePicker.launch(arrayOf("application/json", "*/*")) },
            )
            Divider()
            SettingsGroupLabel("Application")
            SettingsRow(
                icon    = Icons.Outlined.BugReport,
                label   = "Logs",
                onClick = onLogsClick,
            )
            Divider()
            SettingsRow(
                icon    = Icons.Outlined.Info,
                label   = "À propos",
                onClick = onAboutClick,
            )
            Divider()
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(
    modifier  = Modifier.padding(horizontal = 16.dp),
    thickness = 0.5.dp,
    color     = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
internal fun SettingsGroupLabel(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(14.dp),
        )
    }
}
