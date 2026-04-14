package com.onlyflows.domain.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


// ── RecurrenceRule ────────────────────────────────────────────────────────────

enum class RecurrenceType { HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, WEEKDAYS }
enum class RecurrenceBasis { COMPLETION_DATE, DUE_DATE, SPECIFIC_DATE }

data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(), // 1=Mon..7=Sun, used for WEEKLY
    val basis: RecurrenceBasis = RecurrenceBasis.COMPLETION_DATE,
    val skipWeekends: Boolean = false,
)

// ── ProjectStatus ────────────────────────────────────────────────────────────

enum class ProjectStatus { NOT_STARTED, IN_PROGRESS, WAITING, DONE }

// ── ProjectType ───────────────────────────────────────────────────────────────

enum class ProjectType { PROJECT, LIST, NOTE }

// ── Task ────────────────────────────────────────────────────────────────────

enum class TaskGroup { TODAY, THIS_EVENING, SOMEDAY, ANYTIME }
enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }

@Parcelize
data class Task(
    val id: String,
    val title: String,
    val note: String = "",
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val projectId: String? = null,
    val sectionId: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,   // null = all day
    val endTime: LocalTime? = null,
    val scheduledGroup: TaskGroup? = null,
    val hasReminders: Boolean = false,
    val hasAttachments: Boolean = false,
    val isStarred: Boolean = false,
    val tagIds: List<String> = emptyList(),
    val priority: TaskPriority = TaskPriority.NONE,
    val startDate: LocalDate? = null,
    @IgnoredOnParcel val recurrence: RecurrenceRule? = null,
    val sortOrder: Int = 0,
    val hasNote: Boolean = false,
    val hasChecklist: Boolean = false,
    val recurrenceDoneCount: Int = 0,
    val recurrenceAbandonedCount: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val dependsOnTaskId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
) : Parcelable

// ── ChecklistItem ────────────────────────────────────────────────────────────

@Parcelize
data class ChecklistItem(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val dueDate: LocalDate? = null,
    val startDate: LocalDate? = null,
) : Parcelable

// ── Folder (area grouping projects) ─────────────────────────────────────────

@Parcelize
data class Folder(
    val id: String,
    val title: String,
    val sortOrder: Int = 0,
) : Parcelable

// ── Project ──────────────────────────────────────────────────────────────────

@Parcelize
data class Project(
    val id: String,
    val title: String,
    val note: String = "",
    val colorHex: String = "#4A90D9",
    val iconEmoji: String = "",
    val isCompleted: Boolean = false,
    val folderId: String? = null,           // null = standalone project
    val parentProjectId: String? = null,    // non-null for nested lists
    val type: ProjectType = ProjectType.PROJECT,
    val sections: List<Section> = emptyList(),
    val sortOrder: Int = 0,
    val dueDate: LocalDate? = null,
    val status: ProjectStatus = ProjectStatus.NOT_STARTED,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── StatusLogEntry ────────────────────────────────────────────────────────────

data class StatusLogEntry(
    val status: ProjectStatus,
    val changedAt: LocalDateTime,
)

// ── Section ──────────────────────────────────────────────────────────────────

@Parcelize
data class Section(
    val id: String,
    val projectId: String,
    val title: String,
    val sortOrder: Int = 0,
) : Parcelable

// ── Tag ──────────────────────────────────────────────────────────────────────

@Parcelize
data class Tag(
    val id: String,
    val name: String,
    val colorHex: String = "#4A90D9",
) : Parcelable

// ── Reminder ─────────────────────────────────────────────────────────────────

@Parcelize
data class Reminder(
    val id: String,
    val taskId: String,
    val scheduledAt: LocalDateTime,
) : Parcelable

// ── AgendaEvent ──────────────────────────────────────────────────────────────

data class AgendaEvent(
    val title: String,
    val time: String?,
    val colorHex: String = "#4A90D9",
    val isAllDay: Boolean = false,
    val id: String = "",
    val startMillis: Long = 0L,
    val endMillis: Long = Long.MAX_VALUE,
    val calendarId: String = "",
)

data class CalendarAccount(
    val id: String,
    val displayName: String,
    val accountName: String,
    val colorHex: String = "#4A90D9",
)

// ── Comment ───────────────────────────────────────────────────────────────────

data class Comment(
    val id: String,
    val taskId: String? = null,
    val projectId: String? = null,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ── Attachment ────────────────────────────────────────────────────────────────

data class Attachment(
    val id: String,
    val taskId: String,
    val fileName: String,
    val mimeType: String,
    val uri: String,           // persistable content URI as string
    val fileSize: Long = 0L,   // bytes
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

// ── TimeEntry ─────────────────────────────────────────────────────────────────

data class TimeEntry(
    val id: String,
    val taskId: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime? = null, // null = currently running
)

// ── Note (item in a NOTE-type project) ───────────────────────────────────────

data class Note(
    val id: String,
    val projectId: String,
    val title: String = "",
    val content: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ── FilterPreset ─────────────────────────────────────────────────────────────

data class FilterPreset(
    val id: String,
    val name: String,
    val tagIds: List<String> = emptyList(),
    val projectIds: List<String> = emptyList(),
    val dueDateRange: Pair<LocalDate, LocalDate>? = null,
    val isStarredOnly: Boolean = false,
)
