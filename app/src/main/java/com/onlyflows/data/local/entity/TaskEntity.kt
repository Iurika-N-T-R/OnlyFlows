package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.RecurrenceBasis
import com.onlyflows.domain.model.RecurrenceRule
import com.onlyflows.domain.model.RecurrenceType
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.model.TaskDifficulty
import com.onlyflows.domain.model.TaskImpact
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(
    tableName = "tasks",
    indices = [
        Index("projectId"),
        Index("scheduledGroup"),
        Index("dueDate"),
        Index("isCompleted"),
        Index("deletedAt"),
        Index(value = ["dueDate", "isCompleted", "deletedAt"]),
        Index(value = ["projectId", "deletedAt"]),
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val projectId: String? = null,
    val sectionId: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val scheduledGroup: String? = null,   // TaskGroup.name or null
    val hasReminders: Boolean = false,
    val hasAttachments: Boolean = false,
    val isStarred: Boolean = false,
    val tagIds: List<String> = emptyList(),
    val goalIds: List<String> = emptyList(),
    val priority: String = "NONE",
    val difficulty: String = "NONE",
    val impact: String = "NONE",
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val recurrenceJson: String? = null,
    val sortOrder: Int = 0,
    val hasNote: Boolean = false,
    val hasChecklist: Boolean = false,
    val recurrenceDoneCount: Int = 0,
    val recurrenceAbandonedCount: Int = 0,
    val dependsOnTaskId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
)

fun TaskEntity.toDomain(checklistItems: List<com.onlyflows.data.local.entity.ChecklistItemEntity> = emptyList()) = Task(
    id             = id,
    title          = title,
    note           = note,
    isCompleted    = isCompleted,
    isArchived     = isArchived,
    projectId      = projectId,
    sectionId      = sectionId,
    dueDate        = dueDate,
    dueTime        = dueTime,
    endTime        = endTime,
    scheduledGroup = scheduledGroup?.let { runCatching { TaskGroup.valueOf(it) }.getOrNull() },
    hasReminders   = hasReminders,
    hasAttachments = hasAttachments,
    isStarred      = isStarred,
    tagIds         = tagIds,
    goalIds        = goalIds,
    priority       = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.NONE),
    difficulty     = runCatching { TaskDifficulty.valueOf(difficulty) }.getOrDefault(TaskDifficulty.NONE),
    impact         = runCatching { TaskImpact.valueOf(impact) }.getOrDefault(TaskImpact.NONE),
    startDate      = startDate,
    startTime      = startTime,
    recurrence              = recurrenceJson?.let { parseRecurrenceJson(it) },
    sortOrder               = sortOrder,
    hasNote                 = note.isNotBlank(),
    hasChecklist            = hasChecklist || checklistItems.isNotEmpty(),
    recurrenceDoneCount     = recurrenceDoneCount,
    recurrenceAbandonedCount = recurrenceAbandonedCount,
    checklistItems          = checklistItems.map { it.toDomain() },
    dependsOnTaskId         = dependsOnTaskId,
    createdAt               = createdAt,
    updatedAt               = updatedAt,
    deletedAt               = deletedAt,
    completedAt             = completedAt,
)

fun Task.toEntity() = TaskEntity(
    id             = id,
    title          = title,
    note           = note,
    isCompleted    = isCompleted,
    isArchived     = isArchived,
    projectId      = projectId,
    sectionId      = sectionId,
    dueDate        = dueDate,
    dueTime        = dueTime,
    endTime        = endTime,
    scheduledGroup = scheduledGroup?.name,
    hasReminders   = hasReminders,
    hasAttachments = hasAttachments,
    isStarred      = isStarred,
    tagIds         = tagIds,
    goalIds        = goalIds,
    priority       = priority.name,
    difficulty     = difficulty.name,
    impact         = impact.name,
    startDate      = startDate,
    startTime      = startTime,
    recurrenceJson           = recurrence?.toJson(),
    sortOrder                = sortOrder,
    hasNote                  = hasNote,
    hasChecklist             = hasChecklist || checklistItems.isNotEmpty(),
    recurrenceDoneCount      = recurrenceDoneCount,
    recurrenceAbandonedCount = recurrenceAbandonedCount,
    dependsOnTaskId          = dependsOnTaskId,
    createdAt                = createdAt,
    updatedAt                = updatedAt,
    deletedAt                = deletedAt,
    completedAt              = completedAt,
)

private fun RecurrenceRule.toJson(): String {
    return buildString {
        append("{")
        append("\"type\":\"${type.name}\",")
        append("\"interval\":$interval,")
        append("\"daysOfWeek\":[${daysOfWeek.joinToString(",")}],")
        append("\"basis\":\"${basis.name}\",")
        append("\"skipWeekends\":$skipWeekends")
        if (endDate != null) append(",\"endDate\":\"$endDate\"")
        if (endCount != null) append(",\"endCount\":$endCount")
        append("}")
    }
}

private fun parseRecurrenceJson(json: String): RecurrenceRule? = runCatching {
    val obj = org.json.JSONObject(json)
    RecurrenceRule(
        type         = RecurrenceType.valueOf(obj.getString("type")),
        interval     = obj.optInt("interval", 1),
        daysOfWeek   = obj.optJSONArray("daysOfWeek")?.let { arr ->
            (0 until arr.length()).map { arr.getInt(it) }
        } ?: emptyList(),
        basis        = RecurrenceBasis.valueOf(obj.optString("basis", "COMPLETION_DATE")),
        skipWeekends = obj.optBoolean("skipWeekends", false),
        endDate      = obj.optString("endDate").takeIf { it.isNotBlank() }
                           ?.let { java.time.LocalDate.parse(it) },
        endCount     = if (obj.has("endCount")) obj.getInt("endCount") else null,
    )
}.getOrNull()
