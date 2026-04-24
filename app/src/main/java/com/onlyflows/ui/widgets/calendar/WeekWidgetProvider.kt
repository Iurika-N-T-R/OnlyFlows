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

val selectedDayKey  = ActionParameters.Key<String>("selected_day")
val weekWidgetIdKey = ActionParameters.Key<Int>("week_widget_id")

class WeekWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val glanceIdInt = id.hashCode()
        val prefs = WidgetPreferences(context)
        val selectedDateStr = prefs.getWeekSelectedDate(glanceIdInt)
        val today = LocalDate.now()
        val selectedDate = if (selectedDateStr.isNotBlank())
            runCatching { LocalDate.parse(selectedDateStr) }.getOrNull() ?: today
        else today

        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weekDays = (0..6).map { offset -> weekStart.plusDays(offset.toLong()) }

        val db = AppDatabase.getInstance(context)
        val tasks: List<TaskEntity> = db.taskDao()
            .searchByDate(selectedDate.toString()).first()
            .filter { t -> t.deletedAt == null && !t.isCompleted }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.DarkBg),
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    weekDays.forEach { day ->
                        val isSelected = day == selectedDate
                        val isToday = day == today
                        val dayBg = if (isSelected) WidgetColors.AccentBlue else WidgetColors.DarkBg
                        val textColor = when {
                            isSelected -> Color.White
                            isToday    -> WidgetColors.AccentBlue
                            else       -> WidgetColors.TextSecondary
                        }
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(2.dp)
                                .background(dayBg)
                                .clickable(
                                    actionRunCallback<SelectDayCallback>(
                                        actionParametersOf(
                                            selectedDayKey  to day.toString(),
                                            weekWidgetIdKey to glanceIdInt,
                                        )
                                    )
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = day.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()),
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
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
                                color = ColorProvider(WidgetColors.TextSecondary),
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
                                    modifier = GlanceModifier.size(10.dp).background(WidgetColors.AccentBlue),
                                ) {}
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    text = task.title,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.TextPrimary),
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
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val day = parameters[selectedDayKey] ?: return
        val widgetIdHash = parameters[weekWidgetIdKey] ?: glanceId.hashCode()
        WidgetPreferences(context).saveWeekSelectedDate(widgetIdHash, day)
        WeekWidgetProvider().updateAll(context)
    }
}
