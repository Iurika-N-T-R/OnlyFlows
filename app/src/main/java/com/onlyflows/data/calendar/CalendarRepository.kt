package com.onlyflows.data.calendar

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.domain.model.CalendarAccount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    fun getCalendars(): List<CalendarAccount> {
        if (!hasPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        ) ?: return emptyList()

        val accounts = mutableListOf<CalendarAccount>()
        cursor.use {
            while (it.moveToNext()) {
                val color = it.getInt(3)
                accounts += CalendarAccount(
                    id          = it.getLong(0).toString(),
                    displayName = it.getString(1) ?: "",
                    accountName = it.getString(2) ?: "",
                    colorHex    = "#${(color and 0xFFFFFF).toString(16).padStart(6, '0')}",
                )
            }
        }
        return accounts
    }

    /** Returns events grouped by [LocalDate] for [start]..[end] inclusive.
     *  Calendars whose IDs are in [disabledIds] are skipped. */
    fun getEventsForDateRange(
        start: LocalDate,
        end: LocalDate,
        disabledIds: Set<String> = emptySet(),
    ): Map<LocalDate, List<AgendaEvent>> {
        if (!hasPermission()) return emptyMap()

        val zone     = ZoneId.systemDefault()
        // All-day events (birthdays, anniversaries) store BEGIN at UTC midnight, which can
        // land outside a window built from local midnights. Query one day of slack on each
        // side and drop the extras after the real date is computed below.
        val startMs  = start.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs    = end.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startMs)
        ContentUris.appendId(uriBuilder, endMs)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,        // 0
            CalendarContract.Instances.TITLE,           // 1
            CalendarContract.Instances.BEGIN,           // 2
            CalendarContract.Instances.END,             // 3
            CalendarContract.Instances.ALL_DAY,         // 4
            CalendarContract.Instances.CALENDAR_ID,     // 5
            CalendarContract.Instances.DISPLAY_COLOR,   // 6
        )

        val cursor = context.contentResolver.query(
            uriBuilder.build(), projection, null, null,
            "${CalendarContract.Instances.BEGIN} ASC",
        ) ?: return emptyMap()

        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val result  = mutableMapOf<LocalDate, MutableList<AgendaEvent>>()

        cursor.use {
            while (it.moveToNext()) {
                val calId = it.getLong(5).toString()
                if (calId in disabledIds) continue

                val beginMs  = it.getLong(2)
                val endMs2   = it.getLong(3)
                val isAllDay = it.getInt(4) == 1
                val color    = it.getInt(6)
                val colorHex = "#${(color and 0xFFFFFF).toString(16).padStart(6, '0')}"

                val localStart = Instant.ofEpochMilli(beginMs).atZone(zone).toLocalDateTime()
                val timeStr = if (isAllDay) null else {
                    val localEnd = Instant.ofEpochMilli(endMs2).atZone(zone).toLocalDateTime()
                    "${localStart.format(timeFmt)} – ${localEnd.format(timeFmt)}"
                }

                // All-day events: BEGIN is stored as UTC midnight of the event date,
                // so interpret it in UTC (not local zone) to get the correct date.
                val eventDate = if (isAllDay)
                    Instant.ofEpochMilli(beginMs).atZone(ZoneOffset.UTC).toLocalDate()
                else
                    localStart.toLocalDate()
                if (eventDate.isBefore(start) || eventDate.isAfter(end)) continue
                val event = AgendaEvent(
                    id          = it.getLong(0).toString(),
                    title       = it.getString(1) ?: "",
                    time        = timeStr,
                    colorHex    = colorHex,
                    isAllDay    = isAllDay,
                    startMillis = beginMs,
                    endMillis   = endMs2,
                    calendarId  = calId,
                )
                result.getOrPut(eventDate) { mutableListOf() } += event
            }
        }
        return result
    }

    /** Like [getEventsForDateRange] but for a single day, filtering out events
     *  that have already ended (all-day events are always kept). */
    fun getEventsForDay(date: LocalDate, disabledIds: Set<String> = emptySet()): List<AgendaEvent> {
        val now    = System.currentTimeMillis()
        val events = getEventsForDateRange(date, date, disabledIds)[date] ?: return emptyList()
        return events.filter { it.isAllDay || it.endMillis > now }
    }

    /** Returns upcoming events (up to 1 year ahead) whose title contains [query] (case-insensitive). */
    fun searchByTitle(query: String, disabledIds: Set<String> = emptySet()): List<AgendaEvent> {
        if (!hasPermission() || query.isBlank()) return emptyList()
        val zone     = ZoneId.systemDefault()
        val nowMs    = System.currentTimeMillis()
        val futureMs = nowMs + 365L * 24 * 60 * 60 * 1000

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, nowMs)
        ContentUris.appendId(uriBuilder, futureMs)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.DISPLAY_COLOR,
        )

        val cursor = context.contentResolver.query(
            uriBuilder.build(), projection, null, null,
            "${CalendarContract.Instances.BEGIN} ASC",
        ) ?: return emptyList()

        val results    = mutableListOf<AgendaEvent>()
        val timeFmt    = DateTimeFormatter.ofPattern("HH:mm")
        val lowerQuery = query.lowercase()

        cursor.use {
            while (it.moveToNext()) {
                val title = it.getString(1) ?: continue
                if (!title.lowercase().contains(lowerQuery)) continue
                val calId = it.getLong(5).toString()
                if (calId in disabledIds) continue

                val beginMs  = it.getLong(2)
                val endMs    = it.getLong(3)
                val isAllDay = it.getInt(4) == 1
                val color    = it.getInt(6)
                val colorHex = "#${(color and 0xFFFFFF).toString(16).padStart(6, '0')}"

                val localStart = Instant.ofEpochMilli(beginMs).atZone(zone).toLocalDateTime()
                val timeStr = if (isAllDay) null else {
                    val localEnd = Instant.ofEpochMilli(endMs).atZone(zone).toLocalDateTime()
                    "${localStart.format(timeFmt)} – ${localEnd.format(timeFmt)}"
                }

                results += AgendaEvent(
                    id          = it.getLong(0).toString(),
                    title       = title,
                    time        = timeStr,
                    colorHex    = colorHex,
                    isAllDay    = isAllDay,
                    startMillis = beginMs,
                    endMillis   = endMs,
                    calendarId  = calId,
                )
            }
        }
        return results
    }
}
