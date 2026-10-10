package com.onlyflows.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


// ── RecurrenceRule ────────────────────────────────────────────────────────────

enum class RecurrenceType { HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, WEEKDAYS }
enum class RecurrenceBasis { COMPLETION_DATE, DUE_DATE, SPECIFIC_DATE }

@Immutable
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(), // 1=Mon..7=Sun, used for WEEKLY
    val basis: RecurrenceBasis = RecurrenceBasis.COMPLETION_DATE,
    val skipWeekends: Boolean = false,
    val endDate: LocalDate? = null,    // stop after this date
    val endCount: Int? = null,         // stop after N completions
)

// ── ProjectStatus ────────────────────────────────────────────────────────────

enum class ProjectStatus { NOT_STARTED, IN_PROGRESS, WAITING, DONE, PAUSED }

// ── ProjectType ───────────────────────────────────────────────────────────────

enum class ProjectType { PROJECT, LIST, NOTE }

// ── SavedLocation ─────────────────────────────────────────────────────────────

@Immutable
data class SavedLocation(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ── ActionLog ────────────────────────────────────────────────────────────────

enum class ActionType {
    TASK_CREATED, TASK_COMPLETED, TASK_UNCOMPLETED,
    TASK_ARCHIVED, TASK_DELETED, TASK_RESTORED,
    TASK_ABANDONED, TASK_MOVED,
    TIMER_STARTED, TIMER_STOPPED,
    PROJECT_CREATED, PROJECT_DELETED,
    SECTION_CREATED, SECTION_DELETED,
    PROJECT_STATUS_CHANGED,
    COMMENT_ADDED,
}

@Immutable
data class ActionLog(
    val id: String,
    val actionType: ActionType,
    val entityId: String,
    val entityType: String,
    val label: String,
    val extra: String? = null,
    val occurredAt: LocalDateTime,
)

// ── Task ────────────────────────────────────────────────────────────────────

enum class TaskGroup { TODAY, THIS_EVENING, SOMEDAY, ANYTIME }
enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
enum class TaskDifficulty { NONE, EASY, MEDIUM, HARD }
enum class TaskImpact { NONE, LOW, HIGH }

@Immutable
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
    val goalIds: List<String> = emptyList(),
    val priority: TaskPriority = TaskPriority.NONE,
    val difficulty: TaskDifficulty = TaskDifficulty.NONE,
    val impact: TaskImpact = TaskImpact.NONE,
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,   // null = all day; non-null fires a "starts now" alert
    @IgnoredOnParcel val recurrence: RecurrenceRule? = null,
    val sortOrder: Int = 0,
    val hasNote: Boolean = false,
    val hasChecklist: Boolean = false,
    // Derived from relations at query time (TaskWithChecklistItems) — not stored on the entity
    val hasComments: Boolean = false,
    val hasTimeEntries: Boolean = false,
    val recurrenceDoneCount: Int = 0,
    val recurrenceAbandonedCount: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val dependsOnTaskId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
) : Parcelable

// ── ChecklistItem ────────────────────────────────────────────────────────────

@Immutable
@Parcelize
data class ChecklistItem(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val dueDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val sectionId: String? = null,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── ChecklistSection (groups items within one task's checklist) ──────────────

@Immutable
@Parcelize
data class ChecklistSection(
    val id: String,
    val taskId: String,
    val title: String,
    val sortOrder: Int = 0,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── ChecklistItemReminder (one-shot reminder on a single checklist item) ─────

@Immutable
@Parcelize
data class ChecklistItemReminder(
    val id: String,
    val checklistItemId: String,
    val scheduledAt: LocalDateTime,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── Folder (area grouping projects) ─────────────────────────────────────────

@Immutable
@Parcelize
data class Folder(
    val id: String,
    val title: String,
    val sortOrder: Int = 0,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── Project ──────────────────────────────────────────────────────────────────

@Immutable
@Parcelize
data class Project(
    val id: String,
    val title: String,
    val note: String = "",
    val colorHex: String = "#4A90D9",
    val iconEmoji: String = "",
    // Card cover, base64-encoded downscaled JPEG — stored inline so it syncs with the entity
    val coverImage: String? = null,
    val isCompleted: Boolean = false,
    val folderId: String? = null,           // null = standalone project
    val parentProjectId: String? = null,    // non-null for nested lists
    val type: ProjectType = ProjectType.PROJECT,
    val sections: List<Section> = emptyList(),
    val sortOrder: Int = 0,
    val dueDate: LocalDate? = null,
    val status: ProjectStatus = ProjectStatus.NOT_STARTED,
    // When the project was last moved to DONE — a finished project shows this instead of
    // piling up overdue days against a due date it already met (or missed) long ago.
    val completedAt: LocalDateTime? = null,
    val tagIds: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── StatusLogEntry ────────────────────────────────────────────────────────────

@Immutable
data class StatusLogEntry(
    val status: ProjectStatus,
    val changedAt: LocalDateTime,
)

// ── Section ──────────────────────────────────────────────────────────────────

@Immutable
@Parcelize
data class Section(
    val id: String,
    val projectId: String,
    val title: String,
    val sortOrder: Int = 0,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── Tag ──────────────────────────────────────────────────────────────────────

@Immutable
@Parcelize
data class Tag(
    val id: String,
    val name: String,
    val colorHex: String = "#4A90D9",
    /** Folder this tag belongs to; null = ungrouped. Free text, shared by every tag in it. */
    val groupName: String? = null,
    /** Manual position inside the whole tag list — 0 for every tag until one is reordered. */
    val sortOrder: Int = 0,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── ReminderRecurrence ────────────────────────────────────────────────────────

enum class ReminderRecurrence { NONE, DAILY, WEEKLY }

// ── Reminder ─────────────────────────────────────────────────────────────────

@Immutable
@Parcelize
data class Reminder(
    val id: String,
    val taskId: String,
    val scheduledAt: LocalDateTime,
    val locationId: String? = null,
    val recurrenceType: ReminderRecurrence = ReminderRecurrence.NONE,
    @IgnoredOnParcel val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Parcelable

// ── AgendaEvent ──────────────────────────────────────────────────────────────

@Immutable
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

@Immutable
data class CalendarAccount(
    val id: String,
    val displayName: String,
    val accountName: String,
    val colorHex: String = "#4A90D9",
)

// ── Comment ───────────────────────────────────────────────────────────────────

@Immutable
data class Comment(
    val id: String,
    val taskId: String? = null,
    val projectId: String? = null,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ── Attachment ────────────────────────────────────────────────────────────────

@Immutable
data class Attachment(
    val id: String,
    val taskId: String,
    val fileName: String,
    val mimeType: String,
    val uri: String,           // persistable content URI as string
    val fileSize: Long = 0L,   // bytes
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val syncedFrom: String? = null,
)

// ── TimeEntry ─────────────────────────────────────────────────────────────────

@Immutable
data class TimeEntry(
    val id: String,
    val taskId: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime? = null, // null = currently running
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ── Note (item in a NOTE-type project) ───────────────────────────────────────

@Immutable
data class Note(
    val id: String,
    val projectId: String,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// ── FilterPreset ─────────────────────────────────────────────────────────────

// ── DailyCount ───────────────────────────────────────────────────────────────

data class DailyCount(val date: String, val count: Int)

// ── ProjectTaskProgress ───────────────────────────────────────────────────────

@Immutable
data class ProjectTaskProgress(
    val projectId: String,
    val total: Int,
    val completed: Int,
)

@Immutable
data class FilterPreset(
    val id: String,
    val name: String,
    val tagIds: List<String> = emptyList(),
    val projectIds: List<String> = emptyList(),
    val dueDateRange: Pair<LocalDate, LocalDate>? = null,
    val isStarredOnly: Boolean = false,
)
