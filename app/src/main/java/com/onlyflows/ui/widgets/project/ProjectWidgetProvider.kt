package com.onlyflows.ui.widgets.project

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.MainActivity
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.widgets.common.WidgetColors
import kotlinx.coroutines.flow.first

class ProjectWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val prefs = WidgetPreferences(context)
        val config = prefs.getConfig(appWidgetId)
        val projectId = config.projectId

        val db = AppDatabase.getInstance(context)
        val project: ProjectEntity? = projectId?.let { db.projectDao().getById(it) }

        val tasks: List<TaskEntity> = if (projectId != null) {
            db.taskDao().observeByProject(projectId).first()
                .filter { t -> t.deletedAt == null && !t.isCompleted }
                .sortedBy { t -> t.sortOrder }
        } else emptyList()

        val projectColor: Color = project?.let { p ->
            runCatching { Color(AndroidColor.parseColor(p.colorHex)) }.getOrNull()
        } ?: WidgetColors.AccentBlue

        val configIntent = Intent(context, ProjectWidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.DarkBg),
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionStartActivity(configIntent)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier.size(10.dp).cornerRadius(5.dp).background(projectColor),
                    ) {}
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = project?.title ?: "Select project",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.TextPrimary),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = tasks.size.toString(),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.AccentBlue),
                            fontSize = 12.sp,
                        ),
                    )
                }

                if (tasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (projectId == null) "Tap header to configure" else "No tasks",
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
                                    modifier = GlanceModifier.size(12.dp).cornerRadius(6.dp).background(projectColor),
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
