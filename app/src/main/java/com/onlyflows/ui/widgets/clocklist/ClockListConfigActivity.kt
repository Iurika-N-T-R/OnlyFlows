package com.onlyflows.ui.widgets.clocklist

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.preferences.WidgetConfig
import com.onlyflows.data.preferences.WidgetListType
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.launch

class ClockListConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val prefs = (application as OnlyFlowsApp).container.widgetPreferences
        lifecycleScope.launch {
            val existing = prefs.getConfig(appWidgetId)
            setContent {
                OnlyFlowsTheme {
                    ClockListConfigScreen(
                        initial   = existing,
                        onConfirm = { config -> saveAndFinish(config) },
                        onCancel  = { finish() },
                    )
                }
            }
        }
    }

    private fun saveAndFinish(config: WidgetConfig) {
        val prefs = (application as OnlyFlowsApp).container.widgetPreferences
        lifecycleScope.launch {
            prefs.saveConfig(appWidgetId, config)
            val manager = GlanceAppWidgetManager(this@ClockListConfigActivity)
            val glanceIds = manager.getGlanceIds(ClockListWidgetProvider::class.java)
            glanceIds.forEach { ClockListWidgetProvider().update(this@ClockListConfigActivity, it) }
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockListConfigScreen(
    initial: WidgetConfig,
    onConfirm: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var listType by remember { mutableStateOf(initial.listType) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Liste affichée") },
            text = {
                Column {
                    WidgetListType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { listType = type; showPicker = false }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = listType == type, onClick = { listType = type; showPicker = false })
                            Spacer(Modifier.width(8.dp))
                            Text(type.label(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annuler") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clock + Liste", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Annuler") } },
                actions = {
                    TextButton(onClick = { onConfirm(WidgetConfig(listType = listType)) }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Liste", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(listType.label(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

private fun WidgetListType.label() = when (this) {
    WidgetListType.TODAY    -> "Aujourd'hui"
    WidgetListType.TOMORROW -> "Demain"
    WidgetListType.INBOX    -> "Inbox"
    WidgetListType.UPCOMING -> "Upcoming"
    WidgetListType.ANYTIME  -> "Anytime"
    WidgetListType.SOMEDAY  -> "Someday"
    WidgetListType.OVERDUE  -> "En retard"
}
