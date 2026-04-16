package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import java.time.LocalDateTime

@Entity(tableName = "action_log")
data class ActionLogEntity(
    @PrimaryKey val id: String,
    val actionType: String,    // ActionType enum name
    val entityId: String,
    val entityType: String,    // "task" | "project" | "timer" | "comment"
    val label: String,
    val extra: String? = null,
    val occurredAt: LocalDateTime,
)

fun ActionLogEntity.toDomain() = ActionLog(
    id         = id,
    actionType = ActionType.valueOf(actionType),
    entityId   = entityId,
    entityType = entityType,
    label      = label,
    extra      = extra,
    occurredAt = occurredAt,
)

fun ActionLog.toEntity() = ActionLogEntity(
    id         = id,
    actionType = actionType.name,
    entityId   = entityId,
    entityType = entityType,
    label      = label,
    extra      = extra,
    occurredAt = occurredAt,
)
