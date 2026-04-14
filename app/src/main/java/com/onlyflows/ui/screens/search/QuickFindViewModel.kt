package com.onlyflows.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val POPUP_MAX = 3

data class QuickFindUiState(
    val query: String = "",
    val taskResults: List<Task> = emptyList(),
    val projectResults: List<Project> = emptyList(),
    val tagResults: List<Tag> = emptyList(),
    val dateResults: List<Task> = emptyList(),
    val hasMoreTasks: Boolean = false,
    val hasMoreProjects: Boolean = false,
    val hasMoreTags: Boolean = false,
    val parsedDate: LocalDate? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class QuickFindViewModel(app: Application) : AndroidViewModel(app) {
    private val container   = (app as OnlyFlowsApp).container
    private val taskRepo    = container.taskRepository
    private val projectRepo = container.projectRepository
    private val tagRepo     = container.tagRepository

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
                        val parsedDate = tryParseDate(q)
                        val dateFlow: Flow<List<Task>> =
                            if (parsedDate != null) taskRepo.searchByDate(parsedDate)
                            else flowOf(emptyList())

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
                                hasMoreTasks    = allTasks.size > POPUP_MAX,
                                hasMoreProjects = allProjects.size > POPUP_MAX,
                                hasMoreTags     = allTags.size > POPUP_MAX,
                                parsedDate      = parsedDate,
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

    internal fun tryParseDate(query: String): LocalDate? {
        val q    = query.trim()
        val year = LocalDate.now().year

        // dd/MM  (e.g. "10/02")
        Regex("""^(\d{1,2})/(\d{1,2})$""").matchEntire(q)?.let { m ->
            runCatching {
                LocalDate.of(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())
            }.getOrNull()?.let { return it }
        }

        // dd/MM/yyyy  (e.g. "10/02/2025")
        Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})$""").matchEntire(q)?.let { m ->
            runCatching {
                LocalDate.of(
                    m.groupValues[3].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[1].toInt(),
                )
            }.getOrNull()?.let { return it }
        }

        // "semaine N" / "week N"  (e.g. "semaine 24", "week 24")
        Regex("""^(?:semaine|week)\s+(\d{1,2})$""", RegexOption.IGNORE_CASE).matchEntire(q)?.let { m ->
            runCatching {
                val weekNum = m.groupValues[1].toLong()
                LocalDate.now()
                    .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekNum)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }.getOrNull()?.let { return it }
        }

        // Normalize French abbreviations → English, then try standard patterns
        val normalized = normalizeFrench(q)

        // Text patterns: try both English and French locales
        val textPatterns = listOf("d MMM", "MMM d", "d MMMM", "MMMM d", "dd MMM", "MMM dd")
        for (locale in listOf(Locale.ENGLISH, Locale.FRENCH)) {
            val input = if (locale == Locale.ENGLISH) normalized else q
            // Capitalize first char so "10 feb" → "10 Feb" works
            val capitalized = input.replaceFirstChar { it.uppercaseChar() }
            for (pattern in textPatterns) {
                runCatching {
                    DateTimeFormatter.ofPattern(pattern, locale)
                        .parse(capitalized, LocalDate::from)
                        .withYear(year)
                }.getOrNull()?.let { return it }
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
