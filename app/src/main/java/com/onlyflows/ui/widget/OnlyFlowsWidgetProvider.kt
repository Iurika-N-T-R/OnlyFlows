package com.onlyflows.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.core.widget.RemoteViewsCompat.RemoteCollectionItems
import com.onlyflows.MainActivity
import com.onlyflows.R
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_ITEMS = 100

class OnlyFlowsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            runCatching { mgr.updateAppWidget(id, buildWidget(context, id)) }
                .onFailure { Log.e("OnlyFlowsWidget", "build failed for $id", it) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle,
    ) {
        runCatching { mgr.updateAppWidget(id, buildWidget(context, id)) }
            .onFailure { Log.e("OnlyFlowsWidget", "options change failed for $id", it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { WidgetPrefs(context, it).clear() }
    }
}

// ── Colors ──────────────────────────────────────────────────────────────────

private data class WColors(val bg: Int, val text: Int, val text2: Int, val overdue: Int, val accent: Int)

private fun colors(context: Context, isDark: Boolean) = WColors(
    bg      = context.getColor(if (isDark) R.color.widget_bg_dark    else R.color.widget_bg_light),
    text    = context.getColor(if (isDark) R.color.widget_text_dark  else R.color.widget_text_light),
    text2   = context.getColor(if (isDark) R.color.widget_text2_dark else R.color.widget_text2_light),
    overdue = context.getColor(R.color.widget_overdue),
    accent  = context.getColor(R.color.widget_accent),
)

private fun isDark(context: Context, theme: String) = when (theme) {
    "LIGHT" -> false
    "DARK"  -> true
    else    -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
               Configuration.UI_MODE_NIGHT_YES
}

// ── Build ─────────────────────────────────────────────────────────────────────

private fun buildWidget(context: Context, id: Int): RemoteViews {
    val prefs = WidgetPrefs(context, id)
    val c     = colors(context, isDark(context, prefs.theme))
    val db    = AppDatabase.getInstance(context)

    val tasks = runBlocking { loadTasks(db.taskDao(), prefs) }
    val label = runBlocking { resolveLabel(db, prefs) }

    val rv = RemoteViews(context.packageName, R.layout.widget_scrollable).apply {
        setInt(R.id.widget_root,  "setBackgroundColor", c.bg)
        setInt(R.id.widget_list,  "setBackgroundColor", c.bg)
        setInt(R.id.widget_empty, "setBackgroundColor", c.bg)
        setTextColor(R.id.widget_empty, c.text2)
        setTextViewText(R.id.widget_title, label)

        // Header actions
        setOnClickPendingIntent(R.id.widget_title,    openAppPI(context, id))
        setOnClickPendingIntent(R.id.widget_add,      quickAddPI(context, id, prefs))
        setOnClickPendingIntent(R.id.widget_settings, configPI(context, id))

        // List
        setEmptyView(R.id.widget_list, R.id.widget_empty)
        setPendingIntentTemplate(R.id.widget_list, clickTemplatePI(context, id))
    }

    RemoteViewsCompat.setRemoteAdapter(
        context = context, remoteViews = rv, appWidgetId = id,
        viewId = R.id.widget_list, items = buildItems(context, tasks, prefs.showDetails, c),
    )
    return rv
}

private fun buildItems(context: Context, tasks: List<TaskEntity>, showDetails: Boolean, c: WColors): RemoteCollectionItems {
    val b = RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
    tasks.forEach { t -> b.addItem(t.id.hashCode().toLong(), buildRow(context, t, showDetails, c)) }
    return b.build()
}

private fun buildRow(context: Context, t: TaskEntity, showDetails: Boolean, c: WColors): RemoteViews {
    val overdue = !t.isCompleted && t.dueDate?.isBefore(LocalDate.now()) == true
    return RemoteViews(context.packageName, R.layout.widget_row).apply {
        setTextViewText(R.id.widget_text, t.title)
        setTextColor(R.id.widget_text, if (overdue) c.overdue else c.text)
        setImageViewResource(R.id.widget_checkbox, if (t.isCompleted) R.drawable.ic_task_checkmark else R.drawable.ic_task_unchecked)
        setInt(R.id.widget_checkbox, "setColorFilter", c.accent)

        val meta = if (showDetails) buildMeta(t) else null
        if (meta != null) {
            setTextViewText(R.id.widget_meta, meta)
            setTextColor(R.id.widget_meta, if (overdue) c.overdue else c.text2)
            setViewVisibility(R.id.widget_meta, View.VISIBLE)
        } else {
            setViewVisibility(R.id.widget_meta, View.GONE)
        }

        setOnClickFillInIntent(
            R.id.widget_row,
            Intent(WidgetClickActivity.OPEN_TASK).putExtra(WidgetClickActivity.EXTRA_TASK_ID, t.id),
        )
        setOnClickFillInIntent(
            R.id.widget_checkbox,
            Intent(WidgetClickActivity.COMPLETE_TASK)
                .putExtra(WidgetClickActivity.EXTRA_TASK_ID, t.id)
                .putExtra(WidgetClickActivity.EXTRA_COMPLETED, !t.isCompleted),
        )
    }
}

// ── Data ──────────────────────────────────────────────────────────────────────

private suspend fun loadTasks(dao: TaskDao, prefs: WidgetPrefs): List<TaskEntity> {
    val today = LocalDate.now().toString()
    val flow = when (prefs.filterType) {
        "TODAY"    -> dao.observeToday(today)
        "ANYTIME"  -> dao.observeAnytime()
        "INBOX"    -> dao.observeInbox()
        "UPCOMING" -> dao.observeUpcoming()
        "SOMEDAY"  -> dao.observeSomeday()
        "PROJECT"  -> dao.observeActiveForWidget(prefs.filterId.ifEmpty { null }, null)
        else       -> dao.observeActiveForWidget(null, null)
    }
    return flow.first().sortedWith(compareBy(nullsLast()) { it.dueDate }).take(MAX_ITEMS)
}

private suspend fun resolveLabel(db: AppDatabase, prefs: WidgetPrefs): String = when (prefs.filterType) {
    "TODAY" -> "Today"; "ANYTIME" -> "Anytime"; "INBOX" -> "Inbox"
    "UPCOMING" -> "Upcoming"; "SOMEDAY" -> "Someday"
    "PROJECT" -> db.projectDao().observeActiveProjectsAndLists().first()
        .firstOrNull { it.id == prefs.filterId }?.title ?: "All"
    else -> "All"
}

private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun buildMeta(t: TaskEntity): String? {
    val date = t.dueDate ?: return null
    val d = date.format(dateFmt)
    val tt = t.dueTime?.format(timeFmt)
    return if (tt != null) "$d · $tt" else d
}

// ── PendingIntents ──────────────────────────────────────────────────────────

private fun clickTemplatePI(context: Context, id: Int): PendingIntent =
    PendingIntent.getActivity(
        context, id,
        Intent(context, WidgetClickActivity::class.java),
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

private fun openAppPI(context: Context, id: Int): PendingIntent =
    PendingIntent.getActivity(
        context, id,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

private fun configPI(context: Context, id: Int): PendingIntent =
    PendingIntent.getActivity(
        context, id,
        Intent(context, WidgetConfigActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

private fun quickAddPI(context: Context, id: Int, prefs: WidgetPrefs): PendingIntent =
    PendingIntent.getActivity(
        context, "add$id".hashCode(),
        Intent(context, WidgetQuickAddActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(WidgetQuickAddActivity.EXTRA_FILTER_TYPE, prefs.filterType)
            .putExtra(WidgetQuickAddActivity.EXTRA_FILTER_ID, prefs.filterId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
