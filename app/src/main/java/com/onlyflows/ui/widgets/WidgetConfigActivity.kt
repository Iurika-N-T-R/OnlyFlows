package com.onlyflows.ui.widgets

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
import com.onlyflows.data.preferences.WidgetCheckboxAction
import com.onlyflows.data.preferences.WidgetConfig
import com.onlyflows.data.preferences.WidgetListType
import com.onlyflows.data.preferences.WidgetPlusDestination
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val prefs = (application as OnlyFlowsApp).container.widgetPreferences

        lifecycleScope.launch {
            val existing = prefs.getConfig(appWidgetId)
            setContent {
                OnlyFlowsTheme {
                    WidgetConfigScreen(
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
            val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
            val glanceIds = manager.getGlanceIds(OnlyFlowsWidget::class.java)
            glanceIds.forEach { OnlyFlowsWidget().update(this@WidgetConfigActivity, it) }
            val result = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    initial: WidgetConfig,
    onConfirm: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var listType       by remember { mutableStateOf(initial.listType) }
    var plusDest       by remember { mutableStateOf(initial.plusDestination) }
    var checkboxAction by remember { mutableStateOf(initial.checkboxAction) }

    var showListPicker    by remember { mutableStateOf(false) }
    var showPlusPicker    by remember { mutableStateOf(false) }
    var showCheckboxPicker by remember { mutableStateOf(false) }

    if (showListPicker) {
        PickerDialog(
            title   = "Liste affichée",
            options = WidgetListType.entries.map { it to it.label() },
            current = listType,
            onPick  = { listType = it; showListPicker = false },
            onDismiss = { showListPicker = false },
        )
    }
    if (showPlusPicker) {
        PickerDialog(
            title   = "Bouton +",
            options = WidgetPlusDestination.entries.map { it to it.label() },
            current = plusDest,
            onPick  = { plusDest = it; showPlusPicker = false },
            onDismiss = { showPlusPicker = false },
        )
    }
    if (showCheckboxPicker) {
        PickerDialog(
            title   = "Case à cocher",
            options = WidgetCheckboxAction.entries.map { it to it.label() },
            current = checkboxAction,
            onPick  = { checkboxAction = it; showCheckboxPicker = false },
            onDismiss = { showCheckboxPicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Annuler") }
                },
                actions = {
                    TextButton(onClick = {
                        onConfirm(WidgetConfig(
                            listType        = listType,
                            plusDestination = plusDest,
                            checkboxAction  = checkboxAction,
                        ))
                    }) { Text("OK", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column {
                    ConfigRow(
                        label    = "Liste",
                        value    = listType.label(),
                        onClick  = { showListPicker = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
                    ConfigRow(
                        label    = "Bouton +",
                        value    = plusDest.label(),
                        onClick  = { showPlusPicker = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
                    ConfigRow(
                        label    = "Case à cocher",
                        value    = checkboxAction.label(),
                        onClick  = { showCheckboxPicker = true },
                        showDivider = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector        = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun <T> PickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    current: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = current == value,
                            onClick  = { onPick(value) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
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

private fun WidgetPlusDestination.label() = when (this) {
    WidgetPlusDestination.TODAY -> "Aujourd'hui"
    WidgetPlusDestination.INBOX -> "Inbox"
}

private fun WidgetCheckboxAction.label() = when (this) {
    WidgetCheckboxAction.COMPLETE        -> "Compléter"
    WidgetCheckboxAction.TOGGLE_PRIORITY -> "Basculer priorité"
}
