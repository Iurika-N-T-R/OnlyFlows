package com.onlyflows.ui.widgets.calendar

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.widgets.common.WidgetColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

val selectedDayKey   = ActionParameters.Key<String>("selected_day")
val weekWidgetIdKey  = ActionParameters.Key<Int>("week_widget_id")
val weekNavDeltaKey  = ActionParameters.Key<Int>("week_nav_delta")

class WeekWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val glanceIdInt = id.hashCode()
        val prefs = WidgetPreferences(context)
        val today = LocalDate.now()

        // Reference week start (Monday of displayed week)
        val refStartStr = prefs.getWeekRefStart(glanceIdInt)
        val refStart = refStartStr.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: today.minusDays(today.dayOfWeek.value.toLong() - 1)

        val weekDays = (0..6).map { offset -> refStart.plusDays(offset.toLong()) }

        val selectedDateStr = prefs.getWeekSelectedDate(glanceIdInt)
        val selectedDate = selectedDateStr.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: today

        val db = AppDatabase.getInstance(context)
        val tasks: List<TaskEntity> = if (selectedDate == today) {
            db.taskDao().observeToday(today.toString()).first()
                .filter { t -> t.deletedAt == null }
        } else {
            db.taskDao().searchByDate(selectedDate.toString()).first()
                .filter { t -> t.deletedAt == null && !t.isCompleted }
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.DarkBg),
            ) {
                // ── Week header: < Mon … Sun > ────────────────────────────
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // < prev week
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .cornerRadius(12.dp)
                            .clickable(
                                actionRunCallback<WeekNavCallback>(
                                    actionParametersOf(
                                        weekWidgetIdKey to glanceIdInt,
                                        weekNavDeltaKey to -7,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "<",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.AccentBlue),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }

                    weekDays.forEach { day ->
                        val isSelected = day == selectedDate
                        val isToday    = day == today
                        val textColor  = when {
                            isSelected -> Color.White
                            isToday    -> WidgetColors.AccentBlue
                            else       -> WidgetColors.TextSecondary
                        }
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(2.dp)
                                .cornerRadius(12.dp)
                                .background(if (isSelected) WidgetColors.AccentBlue else WidgetColors.DarkBg)
                                .clickable(
                                    actionRunCallback<SelectDayCallback>(
                                        actionParametersOf(
                                            selectedDayKey  to day.toString(),
                                            weekWidgetIdKey to glanceIdInt,
                                        )
                                    )
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment   = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = day.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()),
                                style = TextStyle(
                                    color     = ColorProvider(textColor),
                                    fontSize  = 10.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = TextStyle(
                                    color      = ColorProvider(textColor),
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign  = TextAlign.Center,
                                ),
                            )
                        }
                    }

                    // > next week
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .cornerRadius(12.dp)
                            .clickable(
                                actionRunCallback<WeekNavCallback>(
                                    actionParametersOf(
                                        weekWidgetIdKey to glanceIdInt,
                                        weekNavDeltaKey to 7,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ">",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.AccentBlue),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }

                if (tasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No tasks",
                            style = TextStyle(
                                color    = ColorProvider(WidgetColors.TextSecondary),
                                fontSize = 13.sp,
                            ),
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(tasks) { task ->
                            val openIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("extra_task_id", task.id)
                            }
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clickable(actionStartActivity(openIntent)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(10.dp)
                                        .cornerRadius(5.dp)
                                        .background(WidgetColors.AccentBlue),
                                ) {}
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    text = task.title,
                                    maxLines = 1,
                                    style = TextStyle(
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
        }
    }
}

class SelectDayCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val day          = parameters[selectedDayKey] ?: return
        val widgetIdHash = parameters[weekWidgetIdKey] ?: glanceId.hashCode()
        WidgetPreferences(context).saveWeekSelectedDate(widgetIdHash, day)
        WeekWidgetProvider().updateAll(context)
    }
}

class WeekNavCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta        = parameters[weekNavDeltaKey] ?: return
        val widgetIdHash = parameters[weekWidgetIdKey] ?: glanceId.hashCode()
        val prefs        = WidgetPreferences(context)

        val refStartStr = prefs.getWeekRefStart(widgetIdHash)
        val today       = LocalDate.now()
        val currentRef  = refStartStr.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: today.minusDays(today.dayOfWeek.value.toLong() - 1)

        val newRef = currentRef.plusDays(delta.toLong())
        prefs.saveWeekRefStart(widgetIdHash, newRef.toString())
        // Select first day of new week
        prefs.saveWeekSelectedDate(widgetIdHash, newRef.toString())
        WeekWidgetProvider().updateAll(context)
    }
}
