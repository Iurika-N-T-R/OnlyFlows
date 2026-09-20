package com.onlyflows.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import com.onlyflows.MainActivity
import com.onlyflows.R
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.DateTrackerEntity
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Built-in pickable trackers (not stored in DB). */
const val TRACKER_END_OF_MONTH = "BUILTIN_END_OF_MONTH"
const val TRACKER_END_OF_YEAR  = "BUILTIN_END_OF_YEAR"

/** 2x2 card widget showing the day countdown of one DateTracker (or a built-in month/year tracker). */
class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            runCatching { mgr.updateAppWidget(id, buildCountdownWidget(context, id)) }
                .onFailure { Log.e("CountdownWidget", "build failed for $id", it) }
        }
        WidgetUpdater.scheduleNextRefresh(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.scheduleNextRefresh(context)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { WidgetPrefs(context, it).clear() }
    }
}

// ── Build ─────────────────────────────────────────────────────────────────────

private data class Countdown(val days: Long, val unit: String, val title: String)

private fun buildCountdownWidget(context: Context, id: Int): RemoteViews {
    val prefs = WidgetPrefs(context, id)
    val dark  = when (prefs.theme) {
        "LIGHT" -> false
        "DARK"  -> true
        else    -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                   Configuration.UI_MODE_NIGHT_YES
    }
    val bg    = context.getColor(if (dark) R.color.widget_bg_dark    else R.color.widget_bg_light)
    val text  = context.getColor(if (dark) R.color.widget_text_dark  else R.color.widget_text_light)
    val text2 = context.getColor(if (dark) R.color.widget_text2_dark else R.color.widget_text2_light)

    val cd = computeCountdown(context, prefs.trackerId)

    return RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setInt(R.id.countdown_root, "setBackgroundColor", bg)
        setTextViewText(R.id.countdown_count, cd.days.toString())
        setTextColor(R.id.countdown_count, text)
        setTextViewText(R.id.countdown_unit, cd.unit)
        setTextColor(R.id.countdown_unit, text2)
        setTextViewText(R.id.countdown_title, cd.title)
        setTextColor(R.id.countdown_title, text)
        setOnClickPendingIntent(
            R.id.countdown_root,
            PendingIntent.getActivity(
                context, id,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    }
}

private fun computeCountdown(context: Context, trackerId: String): Countdown {
    val today = LocalDate.now()
    return when (trackerId) {
        TRACKER_END_OF_MONTH -> Countdown(
            ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth())),
            "days left", "📅 End of month",
        )
        TRACKER_END_OF_YEAR -> Countdown(
            ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 31)),
            "days left", "🎆 End of year",
        )
        else -> {
            val t = runBlocking { AppDatabase.getInstance(context).dateTrackerDao().getById(trackerId) }
                ?: return Countdown(0, "", "Tracker deleted")
            trackerCountdown(t, today)
        }
    }
}

private fun trackerCountdown(t: DateTrackerEntity, today: LocalDate): Countdown {
    val title = "${t.emoji} ${t.name}"
    // Annual (incl. anniversaries): countdown to the next occurrence.
    val target = if (t.isAnnual || t.category == "ANNIVERSARY") {
        val thisYear = t.targetDate.withYear(today.year)
        if (thisYear.isBefore(today)) thisYear.plusYears(1) else thisYear
    } else t.targetDate
    val days = ChronoUnit.DAYS.between(today, target)
    return when {
        days > 0L  -> Countdown(days, "days left", title)
        days == 0L -> Countdown(0, "today! 🎉", title)
        else       -> Countdown(-days, "days ago", title)
    }
}
