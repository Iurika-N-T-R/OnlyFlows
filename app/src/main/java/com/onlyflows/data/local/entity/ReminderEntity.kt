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
)

fun ReminderEntity.toDomain() = Reminder(
    id          = id,
    taskId      = taskId,
    scheduledAt = scheduledAt,
)

fun Reminder.toEntity() = ReminderEntity(
    id          = id,
    taskId      = taskId,
    scheduledAt = scheduledAt,
)
