package com.onlyflows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: GeneralSettingsViewModel = viewModel(),
) {
    val use24h by viewModel.use24hFormat.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val sidebarPrefs  = remember { (context.applicationContext as OnlyFlowsApp).container.sidebarPreferences }
    val hiddenNavItems by sidebarPrefs.hiddenNavItems.collectAsStateWithLifecycle(initialValue = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "General",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
            SettingsGroupLabel("Format")
            SwitchSettingsRow(
                label    = "Format 24h",
                subtitle = if (use24h) "13:30" else "1:30 PM",
                checked  = use24h,
                onCheckedChange = { viewModel.setUse24hFormat(it) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsGroupLabel("Sidebar — visible sections")
            listOf(
                Triple("nav_projects",    "Projects",          "Lists & projects"),
                Triple("nav_goals",       "Goals",             "Goal tracking"),
                Triple("nav_rss",         "RSS",               "News feed"),
                Triple("nav_datetracker", "Dates & Countdowns","Anniversaries & countdowns"),
                Triple("nav_trash",       "Trash",             "Deleted items"),
            ).forEachIndexed { index, (key, label, subtitle) ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SwitchSettingsRow(
                    label    = label,
                    subtitle = subtitle,
                    checked  = key !in hiddenNavItems,
                    onCheckedChange = { visible ->
                        scope.launch { sidebarPrefs.setNavItemHidden(key, !visible) }
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SwitchSettingsRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
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
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
