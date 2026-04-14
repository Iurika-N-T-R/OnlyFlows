package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.TimeEntry
import java.time.LocalDateTime

@Entity(tableName = "time_entries")
data class TimeEntryEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime? = null,
)

fun TimeEntryEntity.toDomain() = TimeEntry(
    id        = id,
    taskId    = taskId,
    startedAt = startedAt,
    endedAt   = endedAt,
)

fun TimeEntry.toEntity() = TimeEntryEntity(
    id        = id,
    taskId    = taskId,
    startedAt = startedAt,
    endedAt   = endedAt,
)
