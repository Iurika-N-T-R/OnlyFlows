package com.onlyflows.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val POPUP_MAX = 3

sealed class DateQuery {
    data class Single(val date: LocalDate) : DateQuery()
    data class Range(val start: LocalDate, val end: LocalDate) : DateQuery()
}

data class QuickFindUiState(
    val query: String = "",
    val taskResults: List<Task> = emptyList(),
    val projectResults: List<Project> = emptyList(),
    val tagResults: List<Tag> = emptyList(),
    val dateResults: List<Task> = emptyList(),
    val eventResults: List<AgendaEvent> = emptyList(),
    val hasMoreTasks: Boolean = false,
    val hasMoreProjects: Boolean = false,
    val hasMoreTags: Boolean = false,
    val parsedDate: LocalDate? = null,
    val dateQuery: DateQuery? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class QuickFindViewModel(app: Application) : AndroidViewModel(app) {
    private val container      = (app as OnlyFlowsApp).container
    private val taskRepo       = container.taskRepository
    private val projectRepo    = container.projectRepository
    private val tagRepo        = container.tagRepository
    private val calendarRepo   = container.calendarRepository
    private val calendarPrefs  = container.calendarPreferences

    private val _query   = MutableStateFlow("")
    private val _uiState = MutableStateFlow(QuickFindUiState())
    val uiState: StateFlow<QuickFindUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(200)
                .flatMapLatest { q ->
                    if (q.isBlank()) {
                        flowOf(QuickFindUiState())
                    } else {
                        val dq = tryParseDateQuery(q)
                        val dateFlow: Flow<List<Task>> = when (dq) {
                            is DateQuery.Single -> taskRepo.searchByDate(dq.date)
                            is DateQuery.Range  -> taskRepo.searchByDateRange(dq.start, dq.end)
                            null                -> flowOf(emptyList())
                        }
                        val singleDate = (dq as? DateQuery.Single)?.date

                        // Fetch calendar events for the resolved date/range (IO, non-blocking)
                        val events: List<AgendaEvent> = if (dq != null && calendarRepo.hasPermission()) {
                            val disabledIds = calendarPrefs.disabledCalendarIds.first()
                            withContext(Dispatchers.IO) {
                                when (dq) {
                                    is DateQuery.Single -> calendarRepo.getEventsForDateRange(dq.date, dq.date, disabledIds)
                                        .values.flatten()
                                    is DateQuery.Range  -> calendarRepo.getEventsForDateRange(dq.start, dq.end, disabledIds)
                                        .values.flatten()
                                }
                            }.take(POPUP_MAX)
                        } else emptyList()

                        combine(
                            taskRepo.search(q),
                            projectRepo.observeAll().map { list ->
                                list.filter { it.title.contains(q, ignoreCase = true) }
                            },
                            tagRepo.observeAll().map { list ->
                                list.filter { it.name.contains(q, ignoreCase = true) }
                            },
                            dateFlow,
                        ) { allTasks, allProjects, allTags, dateTasks ->
                            QuickFindUiState(
                                query           = q,
                                taskResults     = allTasks.take(POPUP_MAX),
                                projectResults  = allProjects.take(POPUP_MAX),
                                tagResults      = allTags.take(POPUP_MAX),
                                dateResults     = dateTasks.take(POPUP_MAX),
                                eventResults    = events,
                                hasMoreTasks    = allTasks.size > POPUP_MAX,
                                hasMoreProjects = allProjects.size > POPUP_MAX,
                                hasMoreTags     = allTags.size > POPUP_MAX,
                                parsedDate      = singleDate,
                                dateQuery       = dq,
                            )
                        }
                    }
                }
                .collect { newState ->
                    _uiState.update { current -> newState.copy(query = current.query) }
                }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
        _uiState.update { it.copy(query = q) }
    }

    // ── Natural language date parser ──────────────────────────────────────────

    internal fun tryParseDateQuery(query: String): DateQuery? {
        val q    = query.trim().lowercase()
        val now  = LocalDate.now()
        val year = now.year

        if (q.isEmpty()) return null

        // ── Relative range keywords (FR + EN) — return a date RANGE ──────────
        data class RangeKeyword(val text: String, val start: LocalDate, val end: LocalDate)
        val monday   = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday   = monday.plusDays(6)
        val monthEnd = now.withDayOfMonth(now.month.length(now.isLeapYear))
        val rangeKeywords = listOf(
            RangeKeyword("cette semaine", monday, sunday),
            RangeKeyword("this week",     monday, sunday),
            RangeKeyword("ce mois-ci",    now.withDayOfMonth(1), monthEnd),
            RangeKeyword("ce mois",       now.withDayOfMonth(1), monthEnd),
            RangeKeyword("this month",    now.withDayOfMonth(1), monthEnd),
        )
        if (q.length >= 2) {
            rangeKeywords.firstOrNull { it.text == q || it.text.startsWith(q) }
                ?.let { return DateQuery.Range(it.start, it.end) }
        }

        // ── Single-day relative keywords (FR + EN) ────────────────────────────
        data class SingleKeyword(val text: String, val date: LocalDate)
        val singleKeywords = listOf(
            SingleKeyword("aujourd'hui", now),
            SingleKeyword("aujourdhui",  now),
            SingleKeyword("today",       now),
            SingleKeyword("demain",      now.plusDays(1)),
            SingleKeyword("tomorrow",    now.plusDays(1)),
            SingleKeyword("hier",        now.minusDays(1)),
            SingleKeyword("yesterday",   now.minusDays(1)),
        )
        if (q.length >= 2) {
            singleKeywords.firstOrNull { it.text == q || it.text.startsWith(q) }
                ?.let { return DateQuery.Single(it.date) }
        }

        // dd/MM  (e.g. "10/02")
        Regex("""^(\d{1,2})/(\d{1,2})$""").matchEntire(q)?.let { m ->
            runCatching {
                LocalDate.of(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())
            }.getOrNull()?.let { return DateQuery.Single(it) }
        }

        // dd/MM/yyyy  (e.g. "10/02/2025")
        Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})$""").matchEntire(q)?.let { m ->
            runCatching {
                LocalDate.of(
                    m.groupValues[3].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[1].toInt(),
                )
            }.getOrNull()?.let { return DateQuery.Single(it) }
        }

        // "semaine N" / "week N" — return Monday→Sunday range of that ISO week
        Regex("""^(?:semaine|week)\s+(\d{1,2})$""", RegexOption.IGNORE_CASE).matchEntire(q)?.let { m ->
            runCatching {
                val weekNum  = m.groupValues[1].toLong()
                val weekStart = now.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekNum)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                DateQuery.Range(weekStart, weekStart.plusDays(6))
            }.getOrNull()?.let { return it }
        }

        // Normalize French abbreviations → English, then try standard patterns
        val normalized = normalizeFrench(q)

        // Text patterns: parse day+month only, then inject current year
        val textPatterns = listOf("d MMM", "MMM d", "d MMMM", "MMMM d", "dd MMM", "MMM dd")
        for (locale in listOf(Locale.ENGLISH, Locale.FRENCH)) {
            val input = if (locale == Locale.ENGLISH) normalized else q
            val capitalized = input.split(" ").joinToString(" ") { token ->
                if (token.all { it.isLetter() }) token.replaceFirstChar { it.uppercaseChar() } else token
            }
            for (pattern in textPatterns) {
                runCatching {
                    val accessor = DateTimeFormatter.ofPattern(pattern, locale).parse(capitalized)
                    val month = accessor.get(ChronoField.MONTH_OF_YEAR)
                    val day   = accessor.get(ChronoField.DAY_OF_MONTH)
                    LocalDate.of(year, month, day)
                }.getOrNull()?.let { return DateQuery.Single(it) }
            }
        }

        return null
    }

    private fun normalizeFrench(input: String): String {
        // Map French month names/abbreviations → English equivalents
        // Sorted longest-first to avoid partial replacements
        val frToEn = listOf(
            "janvier"   to "january",
            "février"   to "february",
            "fevrier"   to "february",
            "septembre" to "september",
            "octobre"   to "october",
            "novembre"  to "november",
            "décembre"  to "december",
            "decembre"  to "december",
            "janvier"   to "january",
            "juillet"   to "july",
            "janvier"   to "january",
            "janv"      to "jan",
            "févr"      to "feb",
            "fevr"      to "feb",
            "fév"       to "feb",
            "fev"       to "feb",
            "mars"      to "march",
            "avr"       to "apr",
            "avril"     to "april",
            "mai"       to "may",
            "juin"      to "june",
            "juil"      to "jul",
            "août"      to "august",
            "aout"      to "august",
            "aoû"       to "august",
            "sept"      to "sep",
            "oct"       to "oct",
            "nov"       to "nov",
            "déc"       to "dec",
            "dec"       to "dec",
        )
        var result = input.lowercase()
        for ((fr, en) in frToEn) {
            result = result.replace(fr, en)
        }
        return result
    }
}
