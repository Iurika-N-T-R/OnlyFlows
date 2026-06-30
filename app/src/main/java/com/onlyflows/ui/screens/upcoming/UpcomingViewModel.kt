package com.onlyflows.ui.screens.upcoming

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.calendar.CalendarRepository
import com.onlyflows.data.preferences.CalendarPreferences
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ── View mode ─────────────────────────────────────────────────────────────────

enum class UpcomingViewMode { WEEK, MONTH }

// ── Flat item model ───────────────────────────────────────────────────────────

sealed interface UpcomingItem {
    data class WeekHeader(val key: String, val label: String)                   : UpcomingItem
    data class MonthHeader(val key: String, val label: String)                  : UpcomingItem
    data class DayHeader(val date: LocalDate, val label: String)                : UpcomingItem
    data class TaskRow(val task: Task, val date: LocalDate)                     : UpcomingItem
    data class AgendaRow(val event: AgendaEvent, val date: LocalDate)           : UpcomingItem
    data class ChecklistReminderRow(
        val reminder: com.onlyflows.domain.model.ChecklistItemReminder,
        val itemTitle: String,
        val taskTitle: String,
        val taskId: String,
        val date: LocalDate,
    ) : UpcomingItem
    data class EmptyDay(val date: LocalDate)                                    : UpcomingItem
    data class DaySpacer(val key: String)                                       : UpcomingItem
}

/** The same key used in `items(key = { ... })` — single source of truth. */
val UpcomingItem.lazyKey: String get() = when (this) {
    is UpcomingItem.WeekHeader  -> this.key
    is UpcomingItem.MonthHeader -> this.key
    is UpcomingItem.DayHeader   -> "day_${this.date}"
    is UpcomingItem.TaskRow     -> "task_${this.task.id}"
    is UpcomingItem.AgendaRow   -> "agenda_${this.event.id}_${this.date}"
    is UpcomingItem.ChecklistReminderRow -> "checklist_reminder_${this.reminder.id}"
    is UpcomingItem.EmptyDay    -> "empty_${this.date}"
    is UpcomingItem.DaySpacer   -> this.key
}

// ── State & Events ────────────────────────────────────────────────────────────

data class UpcomingUiState(
    val items: List<UpcomingItem>      = emptyList(),
    val tags: List<Tag>                = emptyList(),
    val viewMode: UpcomingViewMode     = UpcomingViewMode.WEEK,
    val todayItemIndex: Int            = 0,
    val pendingDeleteTask: Task?       = null,
    val isLoading: Boolean             = true,
    val hasCalendarPermission: Boolean = false,
)

sealed interface UpcomingEvent {
    data object GoToToday      : UpcomingEvent
    data object ToggleViewMode : UpcomingEvent
    data class RescheduleTask(val taskId: String, val newDate: LocalDate) : UpcomingEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class UpcomingViewModel(app: Application) : AndroidViewModel(app) {

    private val container      = (app as OnlyFlowsApp).container
    private val taskRepo       = container.taskRepository
    private val tagRepo        = container.tagRepository
    private val calendarRepo: CalendarRepository   = container.calendarRepository
    private val calendarPrefs: CalendarPreferences = container.calendarPreferences
    private val checklistReminderRepo = container.checklistItemReminderRepository

    private val _uiState = MutableStateFlow(UpcomingUiState())
    val uiState: StateFlow<UpcomingUiState> = _uiState.asStateFlow()

    private val dayFmt   = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val weekFmt  = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    // Sliding window: 14 days back, 365 days forward
    private val windowStart get() = LocalDate.now().minusDays(14)
    private val windowEnd   get() = LocalDate.now().plusDays(365)

    private var allTasks: List<Task>                          = emptyList()
    private var allTags:  List<Tag>                           = emptyList()
    private var checklistItemsById: Map<String, com.onlyflows.domain.model.ChecklistItem> = emptyMap()
    private var taskTitlesById: Map<String, String> = emptyMap()
    private var checklistReminders: List<com.onlyflows.domain.model.ChecklistItemReminder> = emptyList()
    private var calendarEventsByDate: Map<LocalDate, List<AgendaEvent>> = emptyMap()

    init {
        viewModelScope.launch {
            combine(
                taskRepo.observeUpcoming(),
                tagRepo.observeAll(),
                taskRepo.observeAll(),
                checklistReminderRepo.observeAll(),
            ) { tasks, tags, fullTasks, reminders ->
                object {
                    val tasks = tasks
                    val tags = tags
                    val fullTasks = fullTasks
                    val reminders = reminders
                }
            }.collect { snapshot ->
                allTasks = snapshot.tasks
                allTags  = snapshot.tags
                checklistItemsById = snapshot.fullTasks.flatMap { it.checklistItems }.associateBy { it.id }
                taskTitlesById     = snapshot.fullTasks.associate { it.id to it.title }
                checklistReminders = snapshot.reminders
                recompute()
            }
        }
        viewModelScope.launch {
            val hasPermission = calendarRepo.hasPermission()
            _uiState.update { it.copy(hasCalendarPermission = hasPermission) }
            if (hasPermission) {
                calendarPrefs.disabledCalendarIds.collectLatest { disabledIds ->
                    calendarEventsByDate = withContext(Dispatchers.IO) {
                        calendarRepo.getEventsForDateRange(windowStart, windowEnd, disabledIds)
                    }
                    recompute()
                }
            }
        }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasCalendarPermission = true) }
        viewModelScope.launch {
            val disabledIds = calendarPrefs.disabledCalendarIds.first()
            calendarEventsByDate = withContext(Dispatchers.IO) {
                calendarRepo.getEventsForDateRange(windowStart, windowEnd, disabledIds)
            }
            recompute()
        }
    }

    fun onEvent(event: UpcomingEvent) {
        when (event) {
            UpcomingEvent.GoToToday     -> { /* handled by Screen via listState.scrollToItem */ }
            UpcomingEvent.ToggleViewMode -> {
                val next = if (_uiState.value.viewMode == UpcomingViewMode.WEEK)
                    UpcomingViewMode.MONTH else UpcomingViewMode.WEEK
                _uiState.update { it.copy(viewMode = next) }
                recompute()
            }
            is UpcomingEvent.RescheduleTask -> viewModelScope.launch {
                val task = allTasks.find { it.id == event.taskId } ?: return@launch
                taskRepo.update(task.copy(dueDate = event.newDate))
            }
        }
    }

    private fun recompute() {
        val mode       = _uiState.value.viewMode
        val today      = LocalDate.now()
        val tasksByDate = allTasks.groupBy { it.dueDate }
        val checklistReminderRowsByDate = checklistReminders
            .mapNotNull { reminder ->
                val item = checklistItemsById[reminder.checklistItemId] ?: return@mapNotNull null
                val taskTitle = taskTitlesById[item.taskId] ?: return@mapNotNull null
                UpcomingItem.ChecklistReminderRow(
                    reminder  = reminder,
                    itemTitle = item.title,
                    taskTitle = taskTitle,
                    taskId    = item.taskId,
                    date      = reminder.scheduledAt.toLocalDate(),
                )
            }
            .groupBy { it.date }

        val items      = mutableListOf<UpcomingItem>()
        var todayIndex = 0

        var currentGroupKey: String? = null
        var cursor = windowStart

        while (!cursor.isAfter(windowEnd)) {
            // ── Group header (week or month) ─────────────────────────────────
            val groupKey = when (mode) {
                UpcomingViewMode.WEEK  -> {
                    cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                }
                UpcomingViewMode.MONTH -> "${cursor.year}_${cursor.monthValue}"
            }
            if (groupKey != currentGroupKey) {
                currentGroupKey = groupKey
                when (mode) {
                    UpcomingViewMode.WEEK  -> {
                        val monday = cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val sunday = monday.plusDays(6)
                        items += UpcomingItem.WeekHeader(
                            key   = "week_$monday",
                            label = "${monday.format(weekFmt)} – ${sunday.format(weekFmt)}",
                        )
                    }
                    UpcomingViewMode.MONTH -> {
                        items += UpcomingItem.MonthHeader(
                            key   = "month_$groupKey",
                            label = cursor.format(monthFmt),
                        )
                    }
                }
            }

            // ── Today marker ─────────────────────────────────────────────────
            if (cursor == today) todayIndex = items.size

            // ── Day header ───────────────────────────────────────────────────
            items += UpcomingItem.DayHeader(cursor, cursor.format(dayFmt))

            // ── Agenda events ─────────────────────────────────────────────────
            val dayEvents = calendarEventsByDate[cursor] ?: emptyList()
            dayEvents.forEach { event -> items += UpcomingItem.AgendaRow(event, cursor) }

            // ── Tasks or empty placeholder ────────────────────────────────────
            val dayTasks = tasksByDate[cursor] ?: emptyList()
            val dayChecklistReminders = checklistReminderRowsByDate[cursor] ?: emptyList()
            if (dayTasks.isEmpty() && dayEvents.isEmpty() && dayChecklistReminders.isEmpty()) {
                items += UpcomingItem.EmptyDay(cursor)
            } else {
                dayTasks.forEach { task -> items += UpcomingItem.TaskRow(task, cursor) }
                items += dayChecklistReminders
            }

            items += UpcomingItem.DaySpacer("spacer_$cursor")
            cursor = cursor.plusDays(1)
        }

        _uiState.update {
            it.copy(
                items          = items,
                tags           = allTags,
                todayItemIndex = todayIndex,
                isLoading      = false,
            )
        }
    }

    fun deleteTask(id: String) {
        val task = allTasks.find { it.id == id } ?: return
        _uiState.update { it.copy(pendingDeleteTask = task) }
    }

    fun confirmDelete() {
        val task = _uiState.value.pendingDeleteTask ?: return
        _uiState.update { it.copy(pendingDeleteTask = null) }
        viewModelScope.launch { taskRepo.softDelete(task.id) }
    }

    fun undoDelete() {
        _uiState.update { it.copy(pendingDeleteTask = null) }
    }

    fun toggleComplete(taskId: String, current: Boolean) {
        viewModelScope.launch {
            val completing = !current
            val task = allTasks.find { it.id == taskId }
            if (completing && task?.recurrence != null) taskRepo.completeRecurring(taskId)
            else taskRepo.setCompleted(taskId, completing)
        }
    }

    fun toggleChecklistItem(itemId: String, completed: Boolean) {
        viewModelScope.launch { taskRepo.toggleChecklistItem(itemId, completed) }
    }
}
