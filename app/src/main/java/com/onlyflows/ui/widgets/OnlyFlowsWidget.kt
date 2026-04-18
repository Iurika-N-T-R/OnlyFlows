package com.onlyflows.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.R
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.preferences.WidgetConfig
import com.onlyflows.data.preferences.WidgetListType
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

private val SQUARE    = DpSize(110.dp, 110.dp)
private val RECTANGLE = DpSize(220.dp, 110.dp)

val taskIdKey    = ActionParameters.Key<String>("taskId")
val widgetIdKey  = ActionParameters.Key<Int>("widgetId")
const val EXTRA_TASK_ID = "extra_task_id"

class OnlyFlowsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SQUARE, RECTANGLE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app      = context.applicationContext as OnlyFlowsApp
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config   = app.container.widgetPreferences.getConfig(widgetId)
        val db       = AppDatabase.getInstance(context)
        val today    = LocalDate.now().toString()

        val entities: List<TaskEntity> = when (config.listType) {
            WidgetListType.TODAY    -> db.taskDao().observeToday(today).first()
            WidgetListType.INBOX    -> db.taskDao().observeInbox().first()
            WidgetListType.UPCOMING -> db.taskDao().observeUpcoming().first()
            WidgetListType.ANYTIME  -> db.taskDao().observeAnytime().first()
            WidgetListType.SOMEDAY  -> db.taskDao().observeSomeday().first()
        }

        val tasks = entities
            .filter { !it.isCompleted && it.deletedAt == null }
            .map { it.toDomain() }

        val listTitle = when (config.listType) {
            WidgetListType.TODAY    -> "Today"
            WidgetListType.INBOX    -> "Inbox"
            WidgetListType.UPCOMING -> "Upcoming"
            WidgetListType.ANYTIME  -> "Anytime"
            WidgetListType.SOMEDAY  -> "Someday"
        }

        val headerIconRes = when (config.listType) {
            WidgetListType.TODAY    -> R.drawable.ic_nav_today
            WidgetListType.INBOX    -> R.drawable.ic_nav_inbox
            WidgetListType.UPCOMING -> R.drawable.ic_nav_upcoming
            WidgetListType.ANYTIME  -> R.drawable.ic_nav_anytime
            WidgetListType.SOMEDAY  -> R.drawable.ic_nav_someday
        }

        // Intent that opens MainActivity and triggers the add-task sheet
        val addTaskIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_ADD_TASK, true)

        provideContent {
            GlanceTheme {
                val size    = LocalSize.current
                val showFab = size.width >= 180.dp

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {

                        // ── Header (click cycles list type) ───────────────────
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable(
                                    actionRunCallback<CycleListCallback>(
                                        actionParametersOf(widgetIdKey to widgetId)
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider           = ImageProvider(headerIconRes),
                                contentDescription = listTitle,
                                modifier           = GlanceModifier.size(18.dp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text     = listTitle,
                                style    = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                modifier = GlanceModifier.defaultWeight(),
                            )
                            Text(
                                text  = tasks.size.toString(),
                                style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color(0xFF888888))),
                            )
                        }

                        // ── Task list (scrollable) ────────────────────────────
                        LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                            if (tasks.isEmpty()) {
                                item {
                                    Text(
                                        text     = "No tasks",
                                        style    = TextStyle(color = ColorProvider(Color(0xFFAAAAAA))),
                                        modifier = GlanceModifier.padding(vertical = 8.dp),
                                    )
                                }
                            } else {
                                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                    WidgetTaskRow(context = context, task = task)
                                }
                            }
                        }
                    }

                    // ── FAB — circle, bottom-end (rectangle only) ─────────────
                    if (showFab) {
                        Box(
                            modifier         = GlanceModifier.fillMaxSize().padding(bottom = 2.dp, end = 2.dp),
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(36.dp)
                                    .background(Color(0xFF1AAAF5))
                                    .cornerRadius(18.dp)
                                    .clickable(actionStartActivity(addTaskIntent)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text  = "+",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 22.sp,
                                        color      = ColorProvider(Color.White),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Cycle list type on header click ──────────────────────────────────────────

class CycleListCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val widgetId = parameters[widgetIdKey] ?: return
        val prefs    = (context.applicationContext as OnlyFlowsApp).container.widgetPreferences
        val config   = prefs.getConfig(widgetId)
        val types    = WidgetListType.entries
        val next     = types[(types.indexOf(config.listType) + 1) % types.size]
        prefs.saveConfig(widgetId, WidgetConfig(listType = next))
        OnlyFlowsWidget().update(context, glanceId)
    }
}

// ── Complete task on checkbox tap ─────────────────────────────────────────────

class CompleteTaskCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[taskIdKey] ?: return
        val now    = LocalDateTime.now().toString()
        AppDatabase.getInstance(context).taskDao().setCompleted(taskId, true, now)
        OnlyFlowsWidget().update(context, glanceId)
    }
}

// ── Task row ──────────────────────────────────────────────────────────────────

@GlanceComposable
@androidx.compose.runtime.Composable
private fun WidgetTaskRow(context: Context, task: Task) {
    val openIntent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(EXTRA_TASK_ID, task.id)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(actionStartActivity(openIntent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider           = ImageProvider(R.drawable.ic_task_unchecked),
            contentDescription = "Complete",
            modifier           = GlanceModifier
                .size(16.dp)
                .clickable(actionRunCallback<CompleteTaskCallback>(actionParametersOf(taskIdKey to task.id))),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text     = task.title,
            maxLines = 1,
            style    = TextStyle(fontSize = 13.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}
