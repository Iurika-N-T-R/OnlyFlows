package com.onlyflows.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.preferences.WidgetConfig
import com.onlyflows.data.preferences.WidgetListType
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

        setContent {
            OnlyFlowsTheme {
                WidgetConfigScreen(
                    onConfirm = { config -> saveAndFinish(config) },
                    onCancel  = { finish() },
                )
            }
        }
    }

    private fun saveAndFinish(config: WidgetConfig) {
        val prefs = (application as OnlyFlowsApp).container.widgetPreferences
        lifecycleScope.launch {
            prefs.saveConfig(appWidgetId, config)
            OnlyFlowsWidget().update(this@WidgetConfigActivity,
                GlanceAppWidgetManager(this@WidgetConfigActivity)
                    .getGlanceIds(OnlyFlowsWidget::class.java)
                    .firstOrNull() ?: return@launch
            )
            val result = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    onConfirm: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedList by remember { mutableStateOf(WidgetListType.TODAY) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Configure Widget", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Show list", style = MaterialTheme.typography.titleMedium)

            WidgetListType.entries.forEach { listType ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(
                        selected = selectedList == listType,
                        onClick  = { selectedList = listType },
                    )
                    Text(
                        text  = listType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick  = { onConfirm(WidgetConfig(listType = selectedList)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Add Widget")
                }
            }
        }
    }
}
