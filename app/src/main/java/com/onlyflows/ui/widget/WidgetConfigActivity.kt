package com.onlyflows.ui.widget

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
import androidx.glance.appwidget.updateAll
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.SectionEntity
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Filter chooser + persistent settings for the home-screen widget.
 *  Opened by tapping the widget's scope pill (filter) or used as its settings screen. */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = WidgetPrefsRepository(this)
        val db   = AppDatabase.getInstance(this)

        setContent {
            OnlyFlowsTheme {
                val prefs by repo.prefs.collectAsState(initial = WidgetPrefs())
                val scope = rememberCoroutineScope()

                var projects by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }
                var sections by remember { mutableStateOf<List<SectionEntity>>(emptyList()) }
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        projects = db.projectDao().observeActiveProjectsAndLists().first()
                        sections = db.sectionDao().getAll()
                    }
                }

                fun edit(block: suspend WidgetPrefsRepository.() -> Unit) = scope.launch {
                    repo.block()
                    OnlyFlowsWidget().updateAll(this@WidgetConfigActivity)
                }

                fun selectScope(type: String, id: String) = scope.launch {
                    repo.setScopeType(type)
                    repo.setScopeId(id)
                    OnlyFlowsWidget().updateAll(this@WidgetConfigActivity)
                    finish()   // pill behaves like a chooser: pick → close
                }

                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                    ) {
                        Text("Widget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                        // ── Filtre / liste ────────────────────────────────────
                        Spacer(Modifier.height(16.dp))
                        SectionTitle("Filtre")
                        val smartLists = listOf(
                            "ALL" to "Toutes", "TODAY" to "Today", "ANYTIME" to "Anytime",
                            "INBOX" to "Inbox", "UPCOMING" to "Upcoming", "SOMEDAY" to "Someday",
                        )
                        smartLists.forEach { (type, label) ->
                            FilterRow(label, selected = prefs.scopeType == type) { selectScope(type, "") }
                        }
                        projects.forEach { p ->
                            FilterRow(p.title, selected = prefs.scopeType == "PROJECT" && prefs.scopeId == p.id) {
                                selectScope("PROJECT", p.id)
                            }
                            sections.filter { it.projectId == p.id }.forEach { s ->
                                FilterRow("   ${s.title}", selected = prefs.scopeType == "SECTION" && prefs.scopeId == s.id) {
                                    selectScope("SECTION", s.id)
                                }
                            }
                        }

                        // ── Réglages ──────────────────────────────────────────
                        Spacer(Modifier.height(20.dp))
                        SectionTitle("Réglages")
                        SwitchRow("Afficher les détails", prefs.showDetails) { v -> edit { setShowDetails(v) } }
                        SwitchRow("Grouper par priorité", prefs.groupByPriority) { v -> edit { setGroupByPriority(v) } }

                        Spacer(Modifier.height(16.dp))
                        SectionTitle("Thème")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FOLLOW" to "Système", "LIGHT" to "Clair", "DARK" to "Sombre").forEach { (key, label) ->
                                FilterChip(
                                    selected = prefs.theme == key,
                                    onClick  = { edit { setTheme(key) } },
                                    label    = { Text(label) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun FilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Text("✓", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
