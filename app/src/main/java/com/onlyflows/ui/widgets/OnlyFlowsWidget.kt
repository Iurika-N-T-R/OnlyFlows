package com.onlyflows.ui.widgets

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
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.preferences.WidgetCheckboxAction
import com.onlyflows.data.preferences.WidgetConfig
import com.onlyflows.data.preferences.WidgetListType
import com.onlyflows.data.preferences.WidgetPlusDestination
import com.onlyflows.domain.model.Task
import com.onlyflows.ui.widgets.common.WidgetColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

private val SMALL    = DpSize(110.dp, 110.dp)
private val WIDE_MED = DpSize(220.dp, 165.dp)
private val WIDE_LRG = DpSize(220.dp, 220.dp)

val taskIdKey   = ActionParameters.Key<String>("taskId")
val widgetIdKey = ActionParameters.Key<Int>("widgetId")
const val EXTRA_TASK_ID = "extra_task_id"

class OnlyFlowsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE_MED, WIDE_LRG))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app      = context.applicationContext as OnlyFlowsApp
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config   = app.container.widgetPreferences.getConfig(widgetId)
        val db       = AppDatabase.getInstance(context)
        val today    = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        val entities: List<TaskEntity> = when (config.listType) {
            WidgetListType.TODAY    -> db.taskDao().observeToday(today).first()
            WidgetListType.TOMORROW -> db.taskDao().searchByDate(tomorrow).first()
            WidgetListType.INBOX    -> db.taskDao().observeInbox().first()
            WidgetListType.UPCOMING -> db.taskDao().observeUpcoming().first()
            WidgetListType.ANYTIME  -> db.taskDao().observeAnytime().first()
            WidgetListType.SOMEDAY  -> db.taskDao().observeSomeday().first()
        }

        val projects: Map<String, ProjectEntity> = db.projectDao().observeActive().first()
            .associateBy { it.id }

        val tasks = entities
            .filter { !it.isCompleted && it.deletedAt == null }
            .map { it.toDomain() }

        val listTitle = when (config.listType) {
            WidgetListType.TODAY    -> "Today"
            WidgetListType.TOMORROW -> "Tomorrow"
            WidgetListType.INBOX    -> "Inbox"
            WidgetListType.UPCOMING -> "Upcoming"
            WidgetListType.ANYTIME  -> "Anytime"
            WidgetListType.SOMEDAY  -> "Someday"
        }

        val headerIconRes = when (config.listType) {
            WidgetListType.TODAY    -> R.drawable.ic_nav_today
            WidgetListType.TOMORROW -> R.drawable.ic_nav_upcoming
            WidgetListType.INBOX    -> R.drawable.ic_nav_inbox
            WidgetListType.UPCOMING -> R.drawable.ic_nav_upcoming
            WidgetListType.ANYTIME  -> R.drawable.ic_nav_anytime
            WidgetListType.SOMEDAY  -> R.drawable.ic_nav_someday
        }

        val plusDest = when (config.plusDestination) {
            WidgetPlusDestination.TODAY -> "TODAY"
            WidgetPlusDestination.INBOX -> "INBOX"
        }
        val addTaskIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_ADD_TASK, true)
            .putExtra("extra_destination", plusDest)

        val configIntent = Intent(context, WidgetConfigActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

        provideContent {
            GlanceTheme {
                val size   = LocalSize.current
                val isTall = size.height >= 160.dp

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetColors.DarkBg)
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {

                        // ── Header ────────────────────────────────────────────
                        Row(
                            modifier          = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider           = ImageProvider(headerIconRes),
                                contentDescription = listTitle,
                                modifier           = GlanceModifier.size(18.dp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text     = "$listTitle ▾",
                                style    = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 15.sp,
                                    color      = ColorProvider(WidgetColors.TextPrimary),
                                ),
                                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(configIntent)),
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Box(
                                modifier         = GlanceModifier
                                    .size(26.dp)
                                    .background(WidgetColors.AccentBlue)
                                    .cornerRadius(13.dp)
                                    .clickable(actionStartActivity(addTaskIntent)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text  = "+",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp,
                                        color      = ColorProvider(Color.White),
                                    ),
                                )
                            }
                            Spacer(GlanceModifier.width(6.dp))
                            Box(
                                modifier         = GlanceModifier
                                    .size(26.dp)
                                    .cornerRadius(13.dp)
                                    .clickable(actionStartActivity(configIntent)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text  = "⋮",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        color    = ColorProvider(WidgetColors.TextSecondary),
                                    ),
                                )
                            }
                        }

                        // ── Task list ─────────────────────────────────────────
                        LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                            if (tasks.isEmpty()) {
                                item {
                                    Text(
                                        text     = "No tasks",
                                        style    = TextStyle(
                                            color    = ColorProvider(WidgetColors.TextSecondary),
                                            fontSize = 13.sp,
                                        ),
                                        modifier = GlanceModifier.padding(vertical = 8.dp),
                                    )
                                }
                            } else {
                                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                    DarkWidgetTaskRow(
                                        context         = context,
                                        task            = task,
                                        projectColor    = task.projectId?.let { projects[it]?.colorHex },
                                        projectName     = task.projectId?.let { projects[it]?.title },
                                        widgetId        = widgetId,
                                        checkboxAction  = config.checkboxAction,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Complete task callback ───────────────────────────────────────────────────

class CompleteTaskCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[taskIdKey] ?: return
        val now    = LocalDateTime.now().toString()
        AppDatabase.getInstance(context).taskDao().setCompleted(taskId, true, now)
        OnlyFlowsWidget().update(context, glanceId)
    }
}

// ── Toggle priority callback ─────────────────────────────────────────────────

class TogglePriorityCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[taskIdKey] ?: return
        val dao    = AppDatabase.getInstance(context).taskDao()
        val task   = dao.getById(taskId) ?: return
        val next   = if (task.priority == "HIGH") "NONE" else "HIGH"
        dao.update(task.copy(priority = next))
        OnlyFlowsWidget().update(context, glanceId)
    }
}

// ── Shared dark-mode task row ────────────────────────────────────────────────

@GlanceComposable
@androidx.compose.runtime.Composable
fun DarkWidgetTaskRow(
    context: Context,
    task: Task,
    projectColor: String?,
    projectName: String?,
    widgetId: Int,
    checkboxAction: WidgetCheckboxAction = WidgetCheckboxAction.COMPLETE,
) {
    val openIntent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(EXTRA_TASK_ID, task.id)

    val checkboxColor = runCatching {
        if (!projectColor.isNullOrBlank()) Color(AndroidColor.parseColor(projectColor))
        else WidgetColors.AccentBlue
    }.getOrDefault(WidgetColors.AccentBlue)

    val checkboxClickAction = when (checkboxAction) {
        WidgetCheckboxAction.COMPLETE        -> actionRunCallback<CompleteTaskCallback>(
            actionParametersOf(taskIdKey to task.id, widgetIdKey to widgetId)
        )
        WidgetCheckboxAction.TOGGLE_PRIORITY -> actionRunCallback<TogglePriorityCallback>(
            actionParametersOf(taskIdKey to task.id, widgetIdKey to widgetId)
        )
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(actionStartActivity(openIntent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored checkbox square
        Box(
            modifier = GlanceModifier
                .size(16.dp)
                .background(checkboxColor)
                .cornerRadius(4.dp)
                .clickable(checkboxClickAction),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text     = task.title,
            maxLines = 1,
            style    = TextStyle(
                fontSize = 13.sp,
                color    = ColorProvider(WidgetColors.TextPrimary),
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (!projectName.isNullOrBlank()) {
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text     = projectName,
                maxLines = 1,
                style    = TextStyle(
                    fontSize = 11.sp,
                    color    = ColorProvider(WidgetColors.TextSecondary),
                ),
            )
        }
    }
}
