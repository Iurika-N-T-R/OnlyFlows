package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Reminder
import java.time.LocalDateTime

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val scheduledAt: LocalDateTime,
    val locationId: String? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun ReminderEntity.toDomain() = Reminder(
    id          = id,
    taskId      = taskId,
    scheduledAt = scheduledAt,
    locationId  = locationId,
    updatedAt   = updatedAt,
)

fun Reminder.toEntity() = ReminderEntity(
    id          = id,
    taskId      = taskId,
    scheduledAt = scheduledAt,
    locationId  = locationId,
    updatedAt   = updatedAt,
)
