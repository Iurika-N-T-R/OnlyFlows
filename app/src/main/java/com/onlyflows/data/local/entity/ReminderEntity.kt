package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Reminder
import com.onlyflows.domain.model.ReminderRecurrence
import java.time.LocalDateTime

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val scheduledAt: LocalDateTime,
    val locationId: String? = null,
    val recurrenceType: String = ReminderRecurrence.NONE.name,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun ReminderEntity.toDomain() = Reminder(
    id             = id,
    taskId         = taskId,
    scheduledAt    = scheduledAt,
    locationId     = locationId,
    recurrenceType = runCatching { ReminderRecurrence.valueOf(recurrenceType) }.getOrDefault(ReminderRecurrence.NONE),
    updatedAt      = updatedAt,
)

fun Reminder.toEntity() = ReminderEntity(
    id             = id,
    taskId         = taskId,
    scheduledAt    = scheduledAt,
    locationId     = locationId,
    recurrenceType = recurrenceType.name,
    updatedAt      = updatedAt,
)
