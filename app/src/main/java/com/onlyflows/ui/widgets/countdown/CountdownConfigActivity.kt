package com.onlyflows.ui.widgets.countdown

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CountdownConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val db = AppDatabase.getInstance(this)

        setContent {
            OnlyFlowsTheme {
                var projects by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }
                var tasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }

                LaunchedEffect(Unit) {
                    projects = db.projectDao().observeActive().first()
                        .filter { it.type == "PROJECT" && it.dueDate != null }
                    tasks = db.taskDao().observeAll().first()
                        .filter { it.deletedAt == null && !it.isCompleted && it.dueDate != null }
                }

                CountdownConfigScreen(
                    projects = projects,
                    tasks = tasks,
                    onSave = { name, date ->
                        lifecycleScope.launch {
                            WidgetPreferences(this@CountdownConfigActivity)
                                .saveCountdownConfig(appWidgetId, name, date.toString())
                            val manager = GlanceAppWidgetManager(this@CountdownConfigActivity)
                            val glanceIds = manager.getGlanceIds(CountdownWidgetProvider::class.java)
                            glanceIds.forEach { CountdownWidgetProvider().update(this@CountdownConfigActivity, it) }
                            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(RESULT_OK, result)
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
private fun CountdownConfigScreen(
    projects: List<ProjectEntity>,
    tasks: List<TaskEntity>,
    onSave: (name: String, date: LocalDate) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Countdown") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Annuler") }
                },
                actions = {
                    TextButton(
                        onClick = { selectedDate?.let { onSave(name.trim(), it) } },
                        enabled = name.isNotBlank() && selectedDate != null,
                    ) { Text("OK", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'événement") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(4.dp))
                if (selectedDate != null) {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), selectedDate!!)
                    Text(
                        text = when {
                            days > 0   -> "$days jours restants"
                            days == 0L -> "Aujourd'hui !"
                            else       -> "${-days} jours passés"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
            }

            if (projects.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Projets", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                items(projects) { project ->
                    val date = runCatching { LocalDate.parse(project.dueDate!!) }.getOrNull()
                    val color = runCatching {
                        val c = android.graphics.Color.parseColor(project.colorHex)
                        Color(c)
                    }.getOrElse { Color(0xFF4A90D9) }
                    ListItem(
                        headlineContent = { Text(project.title) },
                        supportingContent = date?.let { { Text(it.format(displayFormatter)) } },
                        leadingContent = {
                            Surface(shape = CircleShape, color = color, modifier = Modifier.size(14.dp)) {}
                        },
                        modifier = Modifier
                            .clickable {
                                name = project.title
                                selectedDate = date
                            }
                            .then(
                                if (selectedDate != null && name == project.title)
                                    Modifier
                                else Modifier
                            ),
                        colors = if (selectedDate != null && name == project.title && selectedDate == date)
                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else ListItemDefaults.colors(),
                    )
                    HorizontalDivider()
                }
            }

            if (tasks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckBox, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tâches", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                items(tasks) { task ->
                    val date = task.dueDate
                    ListItem(
                        headlineContent = { Text(task.title) },
                        supportingContent = date?.let { { Text(it.format(displayFormatter)) } },
                        leadingContent = {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable {
                            name = task.title
                            selectedDate = date
                        },
                        colors = if (selectedDate != null && name == task.title && selectedDate == date)
                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else ListItemDefaults.colors(),
                    )
                    HorizontalDivider()
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        selectedDate?.let { cal.set(it.year, it.monthValue - 1, it.dayOfMonth) }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedDate = LocalDate.of(year, month + 1, day)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate?.format(displayFormatter) ?: "Choisir une autre date")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
