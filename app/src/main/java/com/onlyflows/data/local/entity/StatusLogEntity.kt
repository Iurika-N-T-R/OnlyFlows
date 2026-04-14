package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.StatusLogEntry
import java.time.LocalDateTime

@Entity(tableName = "project_status_log")
data class StatusLogEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val status: String,
    val changedAt: String,
)

fun StatusLogEntity.toDomain() = StatusLogEntry(
    status    = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.NOT_STARTED),
    changedAt = runCatching { LocalDateTime.parse(changedAt) }.getOrDefault(LocalDateTime.now()),
)
