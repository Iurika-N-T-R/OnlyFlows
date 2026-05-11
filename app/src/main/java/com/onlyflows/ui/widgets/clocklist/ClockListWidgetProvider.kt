package com.onlyflows.ui.widgets.clocklist

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.WidgetListType
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.widgets.common.WidgetColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val SIZE_NORMAL = DpSize(250.dp, 110.dp)
private val SIZE_LARGE  = DpSize(370.dp, 165.dp)

class ClockListWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_NORMAL, SIZE_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val prefs    = WidgetPreferences(context)
        val config   = prefs.getConfig(widgetId)

        val now   = LocalTime.now()
        val today = LocalDate.now()
        val hour   = now.hour.toString().padStart(2, '0')
        val minute = now.minute.toString().padStart(2, '0')
        val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")
        val dayStr = today.format(dayFmt)

        val db = AppDatabase.getInstance(context)
        val tomorrow = today.plusDays(1).toString()

        val entities: List<TaskEntity> = when (config.listType) {
            WidgetListType.TODAY    -> db.taskDao().observeToday(today.toString()).first()
            WidgetListType.TOMORROW -> db.taskDao().searchByDate(tomorrow).first()
            WidgetListType.INBOX    -> db.taskDao().observeInbox().first()
            WidgetListType.UPCOMING -> db.taskDao().observeUpcoming().first()
            WidgetListType.ANYTIME  -> db.taskDao().observeAnytime().first()
            WidgetListType.SOMEDAY  -> db.taskDao().observeSomeday().first()
            WidgetListType.OVERDUE  -> db.taskDao().observeOverdueTasks(today.toString()).first()
        }
        val projects = db.projectDao().observeActive().first().associateBy { it.id }
        val tasks = entities.filter { !it.isCompleted && it.deletedAt == null }

        val listTitle = when (config.listType) {
            WidgetListType.TODAY    -> "Today"
            WidgetListType.TOMORROW -> "Tomorrow"
            WidgetListType.INBOX    -> "Inbox"
            WidgetListType.UPCOMING -> "Upcoming"
            WidgetListType.ANYTIME  -> "Anytime"
            WidgetListType.SOMEDAY  -> "Someday"
            WidgetListType.OVERDUE  -> "Overdue"
        }

        val overdueCount = if (config.listType == WidgetListType.TODAY)
            db.taskDao().observeOverdueTasks(today.toString()).first().size
        else 0

        val configIntent = Intent(context, ClockListConfigActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

        val openAppIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetColors.DarkBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // ── Left: Clock ───────────────────────────────────────────
                    Column(
                        modifier            = GlanceModifier
                            .fillMaxHeight()
                            .clickable(actionStartActivity(openAppIntent)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment   = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = "$hour:$minute",
                            style = TextStyle(
                                fontSize   = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color      = ColorProvider(WidgetColors.TextPrimary),
                                textAlign  = TextAlign.Center,
                            ),
                        )
                        Text(
                            text  = dayStr,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color    = ColorProvider(WidgetColors.TextSecondary),
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }

                    Spacer(GlanceModifier.width(1.dp).fillMaxHeight().background(Color(0xFF3A3A3C)))
                    Spacer(GlanceModifier.width(12.dp))

                    // ── Right: Task list ──────────────────────────────────────
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    ) {
                        // Header
                        Row(
                            modifier          = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable(actionStartActivity(configIntent)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text  = "★",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color    = ColorProvider(Color(0xFFFFCC00)),
                                ),
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            Text(
                                text  = listTitle,
                                style = TextStyle(
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = ColorProvider(WidgetColors.TextPrimary),
                                ),
                                modifier = GlanceModifier.defaultWeight(),
                            )
                            if (overdueCount > 0) {
                                Box(
                                    modifier         = GlanceModifier
                                        .background(Color(0xFFFF3B30))
                                        .cornerRadius(9.dp)
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text  = overdueCount.toString(),
                                        style = TextStyle(
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = ColorProvider(Color.White),
                                        ),
                                    )
                                }
                                Spacer(GlanceModifier.width(4.dp))
                            }
                            Text(
                                text  = tasks.size.toString(),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color    = ColorProvider(WidgetColors.TextSecondary),
                                ),
                            )
                        }

                        // Tasks
                        if (tasks.isEmpty()) {
                            Box(
                                modifier         = GlanceModifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text  = "No tasks",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color    = ColorProvider(WidgetColors.TextSecondary),
                                    ),
                                )
                            }
                        } else {
                            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                    val openIntent = Intent(context, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        .putExtra("extra_task_id", task.id)
                                    val projectColor = task.projectId?.let { pid ->
                                        projects[pid]?.colorHex?.let { hex ->
                                            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
                                        }
                                    } ?: WidgetColors.TextSecondary

                                    Row(
                                        modifier          = GlanceModifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable(actionStartActivity(openIntent)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier         = GlanceModifier
                                                .size(16.dp)
                                                .cornerRadius(8.dp)
                                                .background(Color(0xFF3A3A3C)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(10.dp)
                                                    .cornerRadius(5.dp)
                                                    .background(projectColor),
                                            ) {}
                                        }
                                        Spacer(GlanceModifier.width(8.dp))
                                        Text(
                                            text     = task.title,
                                            maxLines = 2,
                                            style    = TextStyle(
                                                fontSize = 13.sp,
                                                color    = ColorProvider(WidgetColors.TextPrimary),
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
    }
}
