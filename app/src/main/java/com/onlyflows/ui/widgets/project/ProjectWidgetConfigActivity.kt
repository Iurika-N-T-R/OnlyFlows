package com.onlyflows.ui.widgets.project

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.preferences.WidgetConfig
import com.onlyflows.data.preferences.WidgetListType
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val db = AppDatabase.getInstance(this)

        setContent {
            OnlyFlowsTheme {
                var projects by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }
                LaunchedEffect(Unit) {
                    projects = db.projectDao().observeActive().first()
                        .filter { it.type == "PROJECT" }
                }

                ProjectPickerScreen(
                    projects = projects,
                    onPick = { project ->
                        lifecycleScope.launch {
                            val manager = GlanceAppWidgetManager(this@ProjectWidgetConfigActivity)
                            val glanceId = manager.getGlanceIdBy(appWidgetId)
                            WidgetPreferences(this@ProjectWidgetConfigActivity).saveConfig(
                                glanceId.hashCode(),
                                WidgetConfig(listType = WidgetListType.TODAY, projectId = project.id),
                            )
                            ProjectWidgetProvider().update(this@ProjectWidgetConfigActivity, glanceId)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectPickerScreen(
    projects: List<ProjectEntity>,
    onPick: (ProjectEntity) -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Project") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(projects) { project ->
                val color = runCatching {
                    val c = android.graphics.Color.parseColor(project.colorHex)
                    Color(c)
                }.getOrElse { Color(0xFF4A90D9) }
                ListItem(
                    headlineContent = { Text(project.title) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(16.dp),
                        ) {}
                    },
                    modifier = Modifier.clickable { onPick(project) },
                )
                HorizontalDivider()
            }
        }
    }
}
