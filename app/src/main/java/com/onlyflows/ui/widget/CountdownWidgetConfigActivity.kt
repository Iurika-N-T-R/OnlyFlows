package com.onlyflows.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.DateTrackerEntity
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Countdown widget configuration: pick one tracker (built-in month/year or any DateTracker) + theme. */
class CountdownWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        // Default result = cancelled, so abandoning placement removes the widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        val prefs = WidgetPrefs(this, widgetId)

        setContent {
            OnlyFlowsTheme {
                var trackerId by remember { mutableStateOf(prefs.trackerId) }
                var theme     by remember { mutableStateOf(prefs.theme) }
                var trackers  by remember { mutableStateOf<List<DateTrackerEntity>>(emptyList()) }

                LaunchedEffect(Unit) {
                    trackers = withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(this@CountdownWidgetConfigActivity)
                            .dateTrackerDao().observeAll().first()
                    }
                }

                fun done() {
                    prefs.trackerId = trackerId
                    prefs.theme     = theme
                    val mgr = AppWidgetManager.getInstance(this)
                    sendBroadcast(
                        Intent(this, CountdownWidgetProvider::class.java)
                            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                mgr.getAppWidgetIds(android.content.ComponentName(this, CountdownWidgetProvider::class.java)))
                    )
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                    finish()
                }

                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState()).padding(20.dp),
                    ) {
                        Text("Countdown widget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(14.dp))
                        Section("Tracker")
                        Pick("📅 End of month", selected = trackerId == TRACKER_END_OF_MONTH) { trackerId = TRACKER_END_OF_MONTH }
                        Pick("🎆 End of year",  selected = trackerId == TRACKER_END_OF_YEAR)  { trackerId = TRACKER_END_OF_YEAR }
                        trackers.forEach { t ->
                            Pick("${t.emoji} ${t.name}", selected = trackerId == t.id) { trackerId = t.id }
                        }

                        Spacer(Modifier.height(16.dp))
                        Section("Theme")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FOLLOW" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (k, l) ->
                                FilterChip(selected = theme == k, onClick = { theme = k }, label = { Text(l) })
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { done() },
                            enabled = trackerId.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

@Composable
private fun Pick(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
    }
}
