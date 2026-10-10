package com.onlyflows.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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
import com.onlyflows.data.calendar.CalendarRepository
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.preferences.CalendarPreferences
import com.onlyflows.domain.model.AgendaEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_ITEMS = 100

class OnlyFlowsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            runCatching {
                mgr.updateAppWidget(id, buildWidget(context, id))
                // Pre-S RemoteViewsCompat collections only re-read items after an explicit notify
                mgr.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
            }.onFailure { Log.e("OnlyFlowsWidget", "build failed for $id", it) }
        }
        WidgetUpdater.scheduleNextRefresh(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.scheduleNextRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle,
    ) {
        runCatching {
            mgr.updateAppWidget(id, buildWidget(context, id))
            mgr.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }.onFailure { Log.e("OnlyFlowsWidget", "options change failed for $id", it) }
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

    val tasks  = runBlocking { loadTasks(db.taskDao(), prefs) }
    val events = runBlocking { loadEvents(context, prefs) }
    val label  = runBlocking { resolveLabel(db, prefs) }
    val projectNames = runBlocking { db.projectDao().observeActiveProjectsAndLists().first() }
        .associate { it.id to it.title }
    val tagNames = runBlocking { db.tagDao().observeAll().first() }
        .associate { it.id to it.name }

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
        viewId = R.id.widget_list, items = buildItems(context, tasks, events, prefs.showDetails, c, projectNames, tagNames),
    )
    return rv
}

/** Tasks and calendar events interleaved chronologically; undated tasks land at the end. */
private fun buildItems(
    context: Context, tasks: List<TaskEntity>, events: List<AgendaEvent>, showDetails: Boolean, c: WColors,
    projectNames: Map<String, String>, tagNames: Map<String, String>,
): RemoteCollectionItems {
    val taskRows = tasks.map { t ->
        Triple(taskSortKey(t), t.id.hashCode().toLong(), buildRow(context, t, showDetails, c, projectNames, tagNames))
    }
    val eventRows = events.map { e ->
        Triple(eventSortKey(e), "evt:${e.id}".hashCode().toLong(), buildEventRow(context, e, c))
    }

    val b = RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
    (taskRows + eventRows)
        .sortedWith(compareBy(nullsLast()) { it.first })
        .take(MAX_ITEMS)
        .forEach { (_, stableId, views) -> b.addItem(stableId, views) }
    return b.build()
}

private fun taskSortKey(t: TaskEntity): LocalDateTime? =
    t.dueDate?.atTime(t.dueTime ?: java.time.LocalTime.MIDNIGHT)

/** All-day events store BEGIN as UTC midnight, so their date must be read in UTC (see CalendarRepository). */
private fun eventSortKey(e: AgendaEvent): LocalDateTime =
    if (e.isAllDay) Instant.ofEpochMilli(e.startMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay()
    else Instant.ofEpochMilli(e.startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

private fun buildRow(
    context: Context, t: TaskEntity, showDetails: Boolean, c: WColors,
    projectNames: Map<String, String>, tagNames: Map<String, String>,
): RemoteViews {
    val overdue = !t.isCompleted && t.dueDate?.isBefore(LocalDate.now()) == true
    return RemoteViews(context.packageName, R.layout.widget_row).apply {
        setTextViewText(R.id.widget_text, t.title)
        setTextColor(R.id.widget_text, if (overdue) c.overdue else c.text)
        setImageViewResource(R.id.widget_checkbox, if (t.isCompleted) R.drawable.ic_task_checkmark else R.drawable.ic_task_unchecked)
        setInt(R.id.widget_checkbox, "setColorFilter", c.accent)

        val meta = if (showDetails) buildMeta(t, projectNames, tagNames) else null
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

private fun buildEventRow(context: Context, e: AgendaEvent, c: WColors): RemoteViews {
    val dotColor = runCatching { android.graphics.Color.parseColor(e.colorHex) }.getOrDefault(c.accent)
    return RemoteViews(context.packageName, R.layout.widget_row).apply {
        setTextViewText(R.id.widget_text, e.title)
        setTextColor(R.id.widget_text, c.text)
        setImageViewResource(R.id.widget_checkbox, R.drawable.ic_event_dot)
        setInt(R.id.widget_checkbox, "setColorFilter", dotColor)

        setTextViewText(R.id.widget_meta, eventMeta(e))
        setTextColor(R.id.widget_meta, c.text2)
        setViewVisibility(R.id.widget_meta, View.VISIBLE)

        setOnClickFillInIntent(
            R.id.widget_row,
            Intent(WidgetClickActivity.OPEN_EVENT).putExtra(WidgetClickActivity.EXTRA_EVENT_ID, e.id),
        )
        // The dot is not a checkbox: without its own fill-in it would inherit the row's click target.
        setOnClickFillInIntent(
            R.id.widget_checkbox,
            Intent(WidgetClickActivity.OPEN_EVENT).putExtra(WidgetClickActivity.EXTRA_EVENT_ID, e.id),
        )
    }
}

/** Events beyond today carry their date — without it a birthday a year out read "All day". */
private fun eventMeta(e: AgendaEvent): String {
    val date = eventSortKey(e).toLocalDate()
    val time = e.time ?: "All day"
    return if (date == LocalDate.now()) time else "${formatRowDate(date)} · $time"
}

// ── Data ──────────────────────────────────────────────────────────────────────

// Matches the Upcoming screen's forward window: a 30-day horizon silently hid yearly
// all-day events (birthdays, anniversaries) while showing tasks far beyond them.
private const val UPCOMING_EVENT_DAYS = 365L

private suspend fun loadEvents(context: Context, prefs: WidgetPrefs): List<AgendaEvent> {
    val repo = CalendarRepository(context)
    if (!repo.hasPermission()) return emptyList()
    val disabled = CalendarPreferences(context).disabledCalendarIds.first()
    val today = LocalDate.now()
    return when (prefs.filterType) {
        "TODAY"    -> repo.getEventsForDay(today, disabled)
        "UPCOMING" -> repo.getEventsForDateRange(today, today.plusDays(UPCOMING_EVENT_DAYS), disabled)
            .values.flatten()
        else       -> emptyList()
    }
}

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

private val dateFmt     = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val dateYearFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val timeFmt     = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

/** Year is only noise for the current one, and essential for every other. */
private fun formatRowDate(date: LocalDate): String =
    date.format(if (date.year == LocalDate.now().year) dateFmt else dateYearFmt)

private fun buildMeta(t: TaskEntity, projectNames: Map<String, String>, tagNames: Map<String, String>): String? {
    val dateTime = t.dueDate?.let { d ->
        val tt = t.dueTime?.format(timeFmt)
        if (tt != null) "${formatRowDate(d)} $tt" else formatRowDate(d)
    }
    val indicators = buildString {
        if (t.isStarred) append("★")
        if (t.priority != "NONE") append("⚑")
        if (t.hasNote || t.note.isNotBlank()) append("≡")
        if (t.hasChecklist) append("☑")
        if (t.recurrenceJson != null) append("↻")
        if (t.hasReminders) append("🔔")
        if (t.hasAttachments) append("📎")
        if (t.dependsOnTaskId != null) append("🔒")
    }
    val parts = listOfNotNull(
        dateTime,
        projectNames[t.projectId],
        t.tagIds.mapNotNull { tagNames[it] }.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" },
        indicators.takeIf { it.isNotEmpty() },
    )
    return if (parts.isEmpty()) null else parts.joinToString(" · ")
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
