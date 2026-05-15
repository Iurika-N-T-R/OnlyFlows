package com.onlyflows.ui.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.MainActivity
import com.onlyflows.R
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.SectionEntity
import com.onlyflows.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.util.Locale

class OnlyFlowsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Safe defaults shown if data load fails
        var tasks       = emptyList<TaskEntity>()
        var projectMap  = emptyMap<String, ProjectEntity>()
        var sectionMap  = emptyMap<String, SectionEntity>()
        var prefs       = WidgetPrefs()
        var scopeLabel  = "All"
        var nextType    = "ALL"
        var nextId      = ""

        try {
            val db        = AppDatabase.getInstance(context)
            val prefsRepo = WidgetPrefsRepository(context)
            prefs         = prefsRepo.prefs.first()

            val projectId = if (prefs.scopeType == "PROJECT") prefs.scopeId.ifEmpty { null } else null
            val sectionId = if (prefs.scopeType == "SECTION") prefs.scopeId.ifEmpty { null } else null

            val rawTasks    = db.taskDao().observeActiveForWidget(projectId, sectionId).first()
            val allProjects = db.projectDao().observeActiveProjectsAndLists().first()
            val allSections = db.sectionDao().getAll()

            projectMap = allProjects.associate { it.id to it }
            sectionMap = allSections.associate { it.id to it }

            tasks = when (prefs.sortBy) {
                "PRIORITY" -> rawTasks.sortedByDescending { priorityRank(it.priority) }
                "DATE"     -> rawTasks.sortedWith(compareBy(nullsLast()) { it.dueDate })
                "TAG"      -> rawTasks.sortedBy { it.tagIds.firstOrNull() ?: "￿" }
                else       -> rawTasks
            }

            scopeLabel = when (prefs.scopeType) {
                "PROJECT" -> projectMap[prefs.scopeId]?.title ?: "All"
                "SECTION" -> sectionMap[prefs.scopeId]?.title ?: "All"
                else      -> "All"
            }

            val scopeOptions = buildScopeOptions(allProjects, allSections)
            val curIdx = scopeOptions.indexOfFirst { it.first == prefs.scopeType && it.second == prefs.scopeId }
                .takeIf { it >= 0 } ?: 0
            val next = scopeOptions[(curIdx + 1) % scopeOptions.size]
            nextType = next.first
            nextId   = next.second
        } catch (_: Exception) { /* show empty widget rather than crashing */ }

        provideContent {
            WidgetContent(
                tasks         = tasks,
                projectMap    = projectMap,
                sectionMap    = sectionMap,
                prefs         = prefs,
                scopeLabel    = scopeLabel,
                nextScopeType = nextType,
                nextScopeId   = nextId,
                isDark        = context.isNightMode(),
            )
        }
    }

    private fun buildScopeOptions(
        projects: List<ProjectEntity>,
        sections: List<SectionEntity>,
    ): List<Pair<String, String>> = buildList {
        add("ALL" to "")
        projects.forEach { p ->
            add("PROJECT" to p.id)
            sections.filter { it.projectId == p.id }.forEach { s ->
                add("SECTION" to s.id)
            }
        }
    }
}

// ── Color scheme ──────────────────────────────────────────────────────────────

private data class WColors(
    val bg: Color,
    val onBg: Color,
    val onSurface: Color,
    val accent: Color,
    val chipBg: Color,
    val chipSelectedBg: Color,
    val chipSelectedText: Color,
    val divider: Color,
)

private fun lightColors() = WColors(
    bg               = Color(0xFFFFFFFF),
    onBg             = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF6B7280),
    accent           = Color(0xFF4A90D9),
    chipBg           = Color(0xFFEBF4FF),
    chipSelectedBg   = Color(0xFF4A90D9),
    chipSelectedText = Color(0xFFFFFFFF),
    divider          = Color(0xFFE5E7EB),
)

private fun darkColors() = WColors(
    bg               = Color(0xFF161B22),
    onBg             = Color(0xFFF0F4F8),
    onSurface        = Color(0xFF8B9BB4),
    accent           = Color(0xFF4A90D9),
    chipBg           = Color(0xFF1C2330),
    chipSelectedBg   = Color(0xFF4A90D9),
    chipSelectedText = Color(0xFFFFFFFF),
    divider          = Color(0xFF2A3448),
)

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(
    tasks: List<TaskEntity>,
    projectMap: Map<String, ProjectEntity>,
    sectionMap: Map<String, SectionEntity>,
    prefs: WidgetPrefs,
    scopeLabel: String,
    nextScopeType: String,
    nextScopeId: String,
    isDark: Boolean,
) {
    val c     = if (isDark) darkColors() else lightColors()
    val bgRes = if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(bgRes))
            .cornerRadius(24.dp)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header: scope pill (weight=1) + [+] button ────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // defaultWeight() MUST be called inline inside Row lambda (RowScope extension)
                Box(
                    modifier = GlanceModifier
                        .clickable(
                            actionRunCallback<SetScopeCallback>(
                                actionParametersOf(
                                    keyScopeType to nextScopeType,
                                    keyScopeId   to nextScopeId,
                                )
                            )
                        )
                        .defaultWeight()
                        .background(ImageProvider(R.drawable.widget_chip_selected_bg))
                        .cornerRadius(50.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text     = scopeLabel,
                        maxLines = 1,
                        style    = TextStyle(
                            color      = ColorProvider(Color(0xFFFFFFFF)),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .clickable(actionStartActivity<MainActivity>())
                        .size(28.dp)
                        .background(ImageProvider(R.drawable.widget_btn_circle))
                        .cornerRadius(50.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "+",
                        style = TextStyle(
                            color      = ColorProvider(Color(0xFFFFFFFF)),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center,
                        ),
                    )
                }
            }

            Spacer(GlanceModifier.height(8.dp))

            // ── Sort chips — defaultWeight() inline for equal distribution ─
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                val chips = listOf("Date" to "DATE", "Priority" to "PRIORITY", "Tag" to "TAG", "None" to "NONE")
                chips.forEachIndexed { i, (label, sort) ->
                    if (i > 0) Spacer(GlanceModifier.width(4.dp))
                    val selected  = prefs.sortBy == sort
                    val bgColor   = if (selected) c.chipSelectedBg   else c.chipBg
                    val textColor = if (selected) c.chipSelectedText else c.onSurface
                    Box(
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<SetSortCallback>(actionParametersOf(keySortBy to sort)))
                            .defaultWeight()
                            .background(ColorProvider(bgColor))
                            .cornerRadius(50.dp)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text     = label,
                            maxLines = 1,
                            style    = TextStyle(
                                color     = ColorProvider(textColor),
                                fontSize  = 9.sp,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // ── Divider ───────────────────────────────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(c.divider)),
            ) {}

            Spacer(GlanceModifier.height(4.dp))

            // ── Task list ─────────────────────────────────────────────────
            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "No tasks",
                        style = TextStyle(
                            color    = ColorProvider(c.onSurface),
                            fontSize = 13.sp,
                        ),
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                        TaskRow(task = task, projectMap = projectMap, sectionMap = sectionMap, c = c)
                    }
                }
            }
        }
    }
}

// ── Task row ──────────────────────────────────────────────────────────────────

@Composable
private fun TaskRow(
    task: TaskEntity,
    projectMap: Map<String, ProjectEntity>,
    sectionMap: Map<String, SectionEntity>,
    c: WColors,
) {
    val contextLabel = when {
        task.sectionId != null -> sectionMap[task.sectionId]?.title
        task.projectId != null -> projectMap[task.projectId]?.title
        else                   -> null
    }
    val contextColor = task.projectId
        ?.let { projectMap[it]?.colorHex }
        ?.let { parseHexColor(it) }
        ?: c.accent

    val dateText   = buildDateText(task)
    val pColor     = priorityColor(task.priority)
    val hasMetaRow = dateText != null || pColor != null || contextLabel != null

    Column(
        modifier = GlanceModifier
            .clickable(
                actionRunCallback<ToggleTaskCallback>(
                    actionParametersOf(keyTaskId to task.id)
                )
            )
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(13.dp)
                    .background(ColorProvider(Color(0x334A90D9)))
                    .cornerRadius(50.dp),
            ) {}
            Spacer(GlanceModifier.width(5.dp))
            Text(
                text     = task.title,
                maxLines = 1,
                style    = TextStyle(
                    color      = ColorProvider(c.onBg),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        if (hasMetaRow) {
            Spacer(GlanceModifier.height(1.dp))
            Row(
                modifier          = GlanceModifier.padding(start = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val metaParts = buildList {
                    if (contextLabel != null) add(contextLabel)
                    if (dateText != null)     add(dateText)
                }.joinToString(" · ")

                if (metaParts.isNotEmpty()) {
                    Text(
                        text     = metaParts,
                        maxLines = 1,
                        style    = TextStyle(
                            color    = ColorProvider(contextColor),
                            fontSize = 9.sp,
                        ),
                    )
                }
                if (pColor != null) {
                    Spacer(GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .size(6.dp)
                            .background(ColorProvider(pColor))
                            .cornerRadius(50.dp),
                    ) {}
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun buildDateText(task: TaskEntity): String? {
    val date     = task.dueDate ?: return null
    val datePart = date.format(dateFormatter)
    val timePart = task.dueTime?.format(timeFormatter)
    return if (timePart != null) "$datePart · $timePart" else datePart
}

private fun priorityColor(priority: String): Color? = when (priority) {
    "HIGH"   -> Color(0xFFE04444)
    "MEDIUM" -> Color(0xFFF59E0B)
    "LOW"    -> Color(0xFF4A90D9)
    else     -> null
}

private fun priorityRank(priority: String) = when (priority) {
    "HIGH"   -> 3
    "MEDIUM" -> 2
    "LOW"    -> 1
    else     -> 0
}

private fun parseHexColor(hex: String): Color? =
    runCatching {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    }.getOrNull()

fun Context.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
