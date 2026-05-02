package com.onlyflows.ui.widgets.calendar

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.MainActivity
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.widgets.common.WidgetColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val PORTRAIT  = DpSize(110.dp, 110.dp)
private val LANDSCAPE = DpSize(250.dp, 220.dp)

val monthSelectedDayKey  = ActionParameters.Key<String>("month_selected_day")
val monthWidgetIdKey     = ActionParameters.Key<Int>("month_widget_id")
val monthNavDeltaKey     = ActionParameters.Key<Int>("month_nav_delta")

class MonthWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(PORTRAIT, LANDSCAPE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val glanceIdInt = id.hashCode()
        val prefs = WidgetPreferences(context)

        val today = LocalDate.now()

        val selectedDateStr = prefs.getMonthSelectedDate(glanceIdInt)
        val selectedDate = selectedDateStr.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today

        val displayedMonthStr = prefs.getMonthDisplayedMonth(glanceIdInt)
        val displayedMonth = displayedMonthStr.takeIf { it.isNotBlank() }
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.of(selectedDate.year, selectedDate.month)

        // Monday-first: dayOfWeek 1=Mon..7=Sun → offset = (dayOfWeek - 1) % 7
        val firstDay = displayedMonth.atDay(1)
        val firstDayOffset = (firstDay.dayOfWeek.value - 1) // 0=Mon,6=Sun
        val daysInMonth = displayedMonth.lengthOfMonth()

        val db = AppDatabase.getInstance(context)
        val tasks: List<TaskEntity> = db.taskDao()
            .searchByDate(selectedDate.toString()).first()
            .filter { t -> t.deletedAt == null && !t.isCompleted }
        val projects: Map<String, ProjectEntity> = db.projectDao().observeActive().first()
            .associateBy { it.id }

        provideContent {
            val size = LocalSize.current
            val isWide = size.width >= LANDSCAPE.width

            if (isWide) {
                Row(modifier = GlanceModifier.fillMaxSize().background(WidgetColors.DarkBg)) {
                    MonthCalendar(
                        modifier        = GlanceModifier.width(210.dp).fillMaxHeight(),
                        displayedMonth  = displayedMonth,
                        firstDayOffset  = firstDayOffset,
                        daysInMonth     = daysInMonth,
                        today           = today,
                        selectedDate    = selectedDate,
                        glanceIdInt     = glanceIdInt,
                    )
                    // Separator
                    Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(Color(0xFF3A3A3C))) {}
                    DayTaskList(
                        modifier  = GlanceModifier.defaultWeight().fillMaxHeight(),
                        tasks     = tasks,
                        projects  = projects,
                        context   = context,
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize().background(WidgetColors.DarkBg)) {
                    MonthCalendar(
                        modifier        = GlanceModifier.fillMaxWidth(),
                        displayedMonth  = displayedMonth,
                        firstDayOffset  = firstDayOffset,
                        daysInMonth     = daysInMonth,
                        today           = today,
                        selectedDate    = selectedDate,
                        glanceIdInt     = glanceIdInt,
                    )
                    DayTaskList(
                        modifier  = GlanceModifier.fillMaxWidth().defaultWeight(),
                        tasks     = tasks,
                        projects  = projects,
                        context   = context,
                    )
                }
            }
        }
    }
}

@GlanceComposable
@androidx.compose.runtime.Composable
private fun MonthCalendar(
    modifier: GlanceModifier,
    displayedMonth: YearMonth,
    firstDayOffset: Int,
    daysInMonth: Int,
    today: LocalDate,
    selectedDate: LocalDate,
    glanceIdInt: Int,
) {
    val locale = Locale.getDefault()
    val monthName = displayedMonth.month.getDisplayName(JTextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase() }
    val monthLabel = "$monthName ${displayedMonth.year}"

    Column(modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {

        // ── Month header with nav ─────────────────────────────────────────
        Row(
            modifier          = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // < prev
            Box(
                modifier         = GlanceModifier
                    .size(24.dp)
                    .cornerRadius(12.dp)
                    .clickable(
                        actionRunCallback<MonthNavCallback>(
                            actionParametersOf(monthWidgetIdKey to glanceIdInt, monthNavDeltaKey to -1)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "<",
                    style = TextStyle(
                        fontSize   = 14.sp,
                        color      = ColorProvider(WidgetColors.AccentBlue),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Text(
                text     = monthLabel,
                style    = TextStyle(
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ColorProvider(WidgetColors.AccentBlue),
                    textAlign  = TextAlign.Center,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )

            // > next
            Box(
                modifier         = GlanceModifier
                    .size(24.dp)
                    .cornerRadius(12.dp)
                    .clickable(
                        actionRunCallback<MonthNavCallback>(
                            actionParametersOf(monthWidgetIdKey to glanceIdInt, monthNavDeltaKey to 1)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = ">",
                    style = TextStyle(
                        fontSize   = 14.sp,
                        color      = ColorProvider(WidgetColors.AccentBlue),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // ── Day-of-week headers (Mon-first: L M M J V S D) ───────────────
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { label ->
                Text(
                    text     = label,
                    style    = TextStyle(
                        color     = ColorProvider(WidgetColors.TextSecondary),
                        fontSize  = 9.sp,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }

        // ── Calendar grid ─────────────────────────────────────────────────
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        var cellIndex = 0
        repeat(rows) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                repeat(7) {
                    val dayNum = cellIndex - firstDayOffset + 1
                    cellIndex++
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = GlanceModifier.defaultWeight().height(20.dp)) {}
                    } else {
                        val dayDate   = displayedMonth.atDay(dayNum)
                        val isSelected = dayDate == selectedDate
                        val isToday    = dayDate == today
                        val textColor = when {
                            isSelected -> Color.White
                            isToday    -> WidgetColors.AccentBlue
                            else       -> WidgetColors.TextPrimary
                        }
                        Box(
                            modifier         = GlanceModifier
                                .defaultWeight()
                                .height(20.dp)
                                .background(if (isSelected) WidgetColors.AccentBlue else WidgetColors.DarkBg)
                                .cornerRadius(10.dp)
                                .clickable(
                                    actionRunCallback<SelectMonthDayCallback>(
                                        actionParametersOf(
                                            monthSelectedDayKey to dayDate.toString(),
                                            monthWidgetIdKey    to glanceIdInt,
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = dayNum.toString(),
                                style = TextStyle(
                                    color     = ColorProvider(textColor),
                                    fontSize  = 10.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@GlanceComposable
@androidx.compose.runtime.Composable
private fun DayTaskList(
    modifier: GlanceModifier,
    tasks: List<TaskEntity>,
    projects: Map<String, ProjectEntity>,
    context: Context,
) {
    if (tasks.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text  = "No tasks",
                style = TextStyle(
                    color    = ColorProvider(WidgetColors.TextSecondary),
                    fontSize = 12.sp,
                ),
            )
        }
    } else {
        LazyColumn(modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            items(tasks) { task ->
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("extra_task_id", task.id)
                }
                val projectColor: Color = task.projectId?.let { pid ->
                    projects[pid]?.colorHex?.let { hex ->
                        runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
                    }
                } ?: WidgetColors.TextSecondary

                Row(
                    modifier          = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(actionStartActivity(openIntent)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = task.title,
                        maxLines = 1,
                        style    = TextStyle(
                            color    = ColorProvider(WidgetColors.TextPrimary),
                            fontSize = 13.sp,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

// ── Callbacks ─────────────────────────────────────────────────────────────────

class SelectMonthDayCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val day         = parameters[monthSelectedDayKey] ?: return
        val widgetIdHash = parameters[monthWidgetIdKey] ?: glanceId.hashCode()
        WidgetPreferences(context).saveMonthSelectedDate(widgetIdHash, day)
        MonthWidgetProvider().updateAll(context)
    }
}

class MonthNavCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta        = parameters[monthNavDeltaKey] ?: return
        val widgetIdHash = parameters[monthWidgetIdKey] ?: glanceId.hashCode()
        val prefs        = WidgetPreferences(context)

        val currentStr = prefs.getMonthDisplayedMonth(widgetIdHash)
        val current    = currentStr.takeIf { it.isNotBlank() }
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
        val next = current.plusMonths(delta.toLong())
        prefs.saveMonthDisplayedMonth(widgetIdHash, next.toString())
        prefs.saveMonthSelectedDate(widgetIdHash, next.atDay(1).toString())
        MonthWidgetProvider().updateAll(context)
    }
}
