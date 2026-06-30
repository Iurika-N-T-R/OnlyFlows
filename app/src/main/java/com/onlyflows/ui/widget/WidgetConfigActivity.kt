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
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Widget configuration (shown on placement and via the gear). Filter + theme + details. */
class WidgetConfigActivity : ComponentActivity() {

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
                val scope = rememberCoroutineScope()
                var filterType by remember { mutableStateOf(prefs.filterType) }
                var filterId   by remember { mutableStateOf(prefs.filterId) }
                var theme      by remember { mutableStateOf(prefs.theme) }
                var details    by remember { mutableStateOf(prefs.showDetails) }
                var projects   by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }

                LaunchedEffect(Unit) {
                    projects = withContext(Dispatchers.IO) {
                        com.onlyflows.data.local.AppDatabase.getInstance(this@WidgetConfigActivity)
                            .projectDao().observeActiveProjectsAndLists().first()
                    }
                }

                fun done() {
                    prefs.filterType = filterType
                    prefs.filterId   = filterId
                    prefs.theme      = theme
                    prefs.showDetails = details
                    WidgetUpdater.updateAll(this@WidgetConfigActivity)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                    finish()
                }

                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState()).padding(20.dp),
                    ) {
                        Text("Widget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(14.dp))
                        Section("Filtre")
                        listOf(
                            "TODAY" to "Today", "ANYTIME" to "Anytime", "INBOX" to "Inbox",
                            "UPCOMING" to "Upcoming", "SOMEDAY" to "Someday", "ALL" to "Toutes",
                        ).forEach { (type, label) ->
                            Pick(label, selected = filterType == type) { filterType = type; filterId = "" }
                        }
                        projects.forEach { p ->
                            Pick(p.title, selected = filterType == "PROJECT" && filterId == p.id) {
                                filterType = "PROJECT"; filterId = p.id
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Section("Thème")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FOLLOW" to "Système", "LIGHT" to "Clair", "DARK" to "Sombre").forEach { (k, l) ->
                                FilterChip(selected = theme == k, onClick = { theme = k }, label = { Text(l) })
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("Show details", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Switch(checked = details, onCheckedChange = { details = it })
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { done() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
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
