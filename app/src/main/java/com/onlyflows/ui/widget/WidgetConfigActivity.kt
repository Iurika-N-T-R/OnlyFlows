package com.onlyflows.ui.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Settings screen for the home-screen widget, opened from the gear button.
 *  Filter/sort stay on the widget itself; this only holds the persistent options. */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = WidgetPrefsRepository(this)

        setContent {
            OnlyFlowsTheme {
                val prefs by repo.prefs.collectAsState(initial = WidgetPrefs())
                val scope = rememberCoroutineScope()

                // Persist an edit, then push the widget update.
                fun edit(block: suspend WidgetPrefsRepository.() -> Unit) = scope.launch {
                    repo.block()
                    OnlyFlowsWidget().updateAll(this@WidgetConfigActivity)
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text(
                            "Widget",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(16.dp))

                        SwitchRow(
                            label   = "Afficher les détails",
                            checked = prefs.showDetails,
                            onChange = { v -> edit { setShowDetails(v) } },
                        )
                        SwitchRow(
                            label   = "Grouper par priorité",
                            checked = prefs.groupByPriority,
                            onChange = { v -> edit { setGroupByPriority(v) } },
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("Thème", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
