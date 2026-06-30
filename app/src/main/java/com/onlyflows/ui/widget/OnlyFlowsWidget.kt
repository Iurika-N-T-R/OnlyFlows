package com.onlyflows.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.SectionEntity
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.ui.theme.DarkColorScheme
import com.onlyflows.ui.theme.LightColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home-screen widget (Jetpack Glance, official pattern):
 *  - provideGlance loads data off the main thread, then provideContent renders.
 *  - GlanceTheme drives colors (system / forced light / dark via the theme pref).
 *  - Pill → filter/settings dialog · + → quick-add dialog · row tap → complete task.
 */
class OnlyFlowsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    // Forced light/dark when the user overrides; otherwise follow the system.
    private fun colors(theme: String) = when (theme) {
        "LIGHT" -> ColorProviders(light = LightColorScheme, dark = LightColorScheme)
        "DARK"  -> ColorProviders(light = DarkColorScheme,  dark = DarkColorScheme)
        else    -> ColorProviders(light = LightColorScheme, dark = DarkColorScheme)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var tasks      = emptyList<TaskEntity>()
        var projectMap = emptyMap<String, ProjectEntity>()
        var sectionMap = emptyMap<String, SectionEntity>()
        var prefs      = WidgetPrefs()
        var scopeLabel = "All"

        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                prefs  = WidgetPrefsRepository(context).prefs.first()

                tasks = widgetTasksFlow(db.taskDao(), prefs).first()
                    .sortedWith(compareBy(nullsLast()) { it.dueDate })
                projectMap = db.projectDao().observeActiveProjectsAndLists().first().associateBy { it.id }
                sectionMap = db.sectionDao().getAll().associateBy { it.id }

                scopeLabel = when (prefs.scopeType) {
                    "TODAY" -> "Today"; "ANYTIME" -> "Anytime"; "INBOX" -> "Inbox"
                    "UPCOMING" -> "Upcoming"; "SOMEDAY" -> "Someday"
                    "PROJECT" -> projectMap[prefs.scopeId]?.title ?: "All"
                    "SECTION" -> sectionMap[prefs.scopeId]?.title ?: "All"
                    else -> "All"
                }
            } catch (_: Exception) { /* render empty rather than crash */ }
        }

        val themeColors = colors(prefs.theme)
        provideContent {
            GlanceTheme(colors = themeColors) {
                WidgetContent(tasks, projectMap, sectionMap, prefs, scopeLabel)
            }
        }
    }
}

// ── Content ─────────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(
    tasks: List<TaskEntity>,
    projectMap: Map<String, ProjectEntity>,
    sectionMap: Map<String, SectionEntity>,
    prefs: WidgetPrefs,
    scopeLabel: String,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp),
    ) {
        // Header: filter pill (→ chooser) + add button (→ quick-add)
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .clickable(actionStartActivity<WidgetConfigActivity>())
                    .defaultWeight()
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(50.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text  = "$scopeLabel  ▾",
                    style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .clickable(actionStartActivity<QuickAddActivity>())
                    .size(30.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold))
            }
        }

        Spacer(GlanceModifier.height(8.dp))
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.outline)) {}
        Spacer(GlanceModifier.height(4.dp))

        if (tasks.isEmpty()) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tasks", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                if (prefs.groupByPriority) {
                    listOf("HIGH" to "High", "MEDIUM" to "Medium", "LOW" to "Low", "NONE" to "None").forEach { (key, label) ->
                        val group = tasks.filter { it.priority == key }
                        if (group.isNotEmpty()) {
                            item(itemId = "h_$key".hashCode().toLong()) { PriorityHeader(label) }
                            items(group, itemId = { it.id.hashCode().toLong() }) { TaskRow(it, projectMap, sectionMap, prefs.showDetails) }
                        }
                    }
                } else {
                    items(tasks, itemId = { it.id.hashCode().toLong() }) { TaskRow(it, projectMap, sectionMap, prefs.showDetails) }
                }
            }
        }
    }
}

@Composable
private fun PriorityHeader(label: String) {
    Text(
        text     = label,
        modifier = GlanceModifier.padding(top = 4.dp, bottom = 1.dp),
        style    = TextStyle(color = GlanceTheme.colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    projectMap: Map<String, ProjectEntity>,
    sectionMap: Map<String, SectionEntity>,
    showDetails: Boolean,
) {
    val overdue = isOverdue(task)
    val context = when {
        task.sectionId != null -> sectionMap[task.sectionId]?.title
        task.projectId != null -> projectMap[task.projectId]?.title
        else                   -> null
    }
    val dateText = buildDateText(task)
    val meta     = listOfNotNull(context, dateText).joinToString(" · ")

    Column(
        modifier = GlanceModifier
            .clickable(actionRunCallback<ToggleTaskCallback>(
                androidx.glance.action.actionParametersOf(keyTaskId to task.id)
            ))
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(13.dp).cornerRadius(50.dp).background(GlanceTheme.colors.primary)) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text     = task.title,
                maxLines = 1,
                style    = TextStyle(
                    color      = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurface,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        if (showDetails && (meta.isNotEmpty() || priorityColor(task.priority) != null)) {
            Spacer(GlanceModifier.height(1.dp))
            Row(modifier = GlanceModifier.padding(start = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                if (meta.isNotEmpty()) {
                    Text(meta, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 9.sp))
                }
                priorityColor(task.priority)?.let { pc ->
                    Spacer(GlanceModifier.width(4.dp))
                    Box(GlanceModifier.size(6.dp).cornerRadius(50.dp).background(ColorProvider(pc))) {}
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun buildDateText(task: TaskEntity): String? {
    val date = task.dueDate ?: return null
    val d    = date.format(dateFormatter)
    val t    = task.dueTime?.format(timeFormatter)
    return if (t != null) "$d · $t" else d
}

private fun isOverdue(task: TaskEntity): Boolean {
    val due = task.dueDate ?: return false
    return !task.isCompleted && due.isBefore(LocalDate.now())
}

private fun priorityColor(priority: String): androidx.compose.ui.graphics.Color? = when (priority) {
    "HIGH"   -> androidx.compose.ui.graphics.Color(0xFFE04444)
    "MEDIUM" -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
    "LOW"    -> androidx.compose.ui.graphics.Color(0xFF4A90D9)
    else     -> null
}
