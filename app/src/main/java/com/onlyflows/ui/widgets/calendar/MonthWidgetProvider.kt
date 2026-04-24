package com.onlyflows.ui.widgets.calendar

import android.content.Context
import android.content.Intent
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
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.widgets.common.WidgetColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val PORTRAIT  = DpSize(220.dp, 220.dp)
private val LANDSCAPE = DpSize(440.dp, 220.dp)

val monthSelectedDayKey = ActionParameters.Key<String>("month_selected_day")
val monthWidgetIdKey    = ActionParameters.Key<Int>("month_widget_id")

class MonthWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(PORTRAIT, LANDSCAPE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val glanceIdInt = id.hashCode()
        val prefs = WidgetPreferences(context)
        val selectedDateStr = prefs.getMonthSelectedDate(glanceIdInt)
        val today = LocalDate.now()
        val selectedDate = if (selectedDateStr.isNotBlank())
            runCatching { LocalDate.parse(selectedDateStr) }.getOrNull() ?: today
        else today

        val currentMonth = YearMonth.of(selectedDate.year, selectedDate.month)
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
        val daysInMonth = currentMonth.lengthOfMonth()

        val db = AppDatabase.getInstance(context)
        val tasks: List<TaskEntity> = db.taskDao()
            .searchByDate(selectedDate.toString()).first()
            .filter { t -> t.deletedAt == null && !t.isCompleted }

        provideContent {
            val size = LocalSize.current
            val isWide = size.width >= LANDSCAPE.width

            if (isWide) {
                Row(modifier = GlanceModifier.fillMaxSize().background(WidgetColors.DarkBg)) {
                    MonthGrid(
                        modifier = GlanceModifier.width(200.dp).fillMaxHeight(),
                        currentMonth = currentMonth,
                        firstDayOfWeek = firstDayOfWeek,
                        daysInMonth = daysInMonth,
                        today = today,
                        selectedDate = selectedDate,
                        glanceIdInt = glanceIdInt,
                    )
                    DayTaskList(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        tasks = tasks,
                        context = context,
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize().background(WidgetColors.DarkBg)) {
                    MonthGrid(
                        modifier = GlanceModifier.fillMaxWidth(),
                        currentMonth = currentMonth,
                        firstDayOfWeek = firstDayOfWeek,
                        daysInMonth = daysInMonth,
                        today = today,
                        selectedDate = selectedDate,
                        glanceIdInt = glanceIdInt,
                    )
                    DayTaskList(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        tasks = tasks,
                        context = context,
                    )
                }
            }
        }
    }
}

@GlanceComposable
@androidx.compose.runtime.Composable
private fun MonthGrid(
    modifier: GlanceModifier,
    currentMonth: YearMonth,
    firstDayOfWeek: Int,
    daysInMonth: Int,
    today: LocalDate,
    selectedDate: LocalDate,
    glanceIdInt: Int,
) {
    Column(modifier = modifier.padding(6.dp)) {
        Text(
            text = "${currentMonth.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())} ${currentMonth.year}",
            style = TextStyle(
                color = ColorProvider(WidgetColors.TextPrimary),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
        )
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            listOf("S","M","T","W","T","F","S").forEach { label ->
                Text(
                    text = label,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.TextSecondary),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        var cellIndex = 0
        repeat(rows) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                repeat(7) {
                    val dayNum = cellIndex - firstDayOfWeek + 1
                    cellIndex++
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = GlanceModifier.defaultWeight().height(18.dp)) {}
                    } else {
                        val dayDate = currentMonth.atDay(dayNum)
                        val isSelected = dayDate == selectedDate
                        val isToday = dayDate == today
                        val textColor = when {
                            isSelected -> Color.White
                            isToday    -> WidgetColors.AccentBlue
                            else       -> WidgetColors.TextPrimary
                        }
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(18.dp)
                                .background(if (isSelected) WidgetColors.AccentBlue else WidgetColors.DarkBg)
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
                                text = dayNum.toString(),
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = 10.sp,
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
    context: Context,
) {
    if (tasks.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No tasks",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.TextSecondary),
                    fontSize = 12.sp,
                ),
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(tasks) { task ->
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("extra_task_id", task.id)
                }
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .clickable(actionStartActivity(openIntent)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = GlanceModifier.size(8.dp).background(WidgetColors.AccentBlue)) {}
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = task.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.TextPrimary),
                            fontSize = 12.sp,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

class SelectMonthDayCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val day = parameters[monthSelectedDayKey] ?: return
        val widgetIdHash = parameters[monthWidgetIdKey] ?: glanceId.hashCode()
        WidgetPreferences(context).saveMonthSelectedDate(widgetIdHash, day)
        MonthWidgetProvider().updateAll(context)
    }
}
