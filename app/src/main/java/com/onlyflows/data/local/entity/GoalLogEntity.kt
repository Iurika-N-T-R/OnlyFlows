package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.GoalLog
import java.time.LocalDateTime

@Entity(
    tableName = "goal_logs",
    indices = [Index("goalId"), Index("deletedAt")],
)
data class GoalLogEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val label: String,
    val note: String = "",
    val tagsJson: String = "",
    val isStarred: Boolean = false,
    val loggedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)

fun GoalLogEntity.toDomain() = GoalLog(
    id        = id,
    goalId    = goalId,
    label     = label,
    note      = note,
    tags      = if (tagsJson.isBlank()) emptyList() else tagsJson.split("|").map { it.trim() }.filter { it.isNotEmpty() },
    isStarred = isStarred,
    loggedAt  = loggedAt,
    deletedAt = deletedAt,
)

fun GoalLog.toEntity() = GoalLogEntity(
    id        = id,
    goalId    = goalId,
    label     = label,
    note      = note,
    tagsJson  = tags.joinToString("|"),
    isStarred = isStarred,
    loggedAt  = loggedAt,
    deletedAt = deletedAt,
)
