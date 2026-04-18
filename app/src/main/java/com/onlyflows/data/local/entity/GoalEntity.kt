package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.model.GoalFrequency
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "goals",
    indices = [Index("isCompleted"), Index("deletedAt")],
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val emoji: String = "🎯",
    val colorHex: String = "#4A90D9",
    val frequency: String = "DAILY",
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val deadline: LocalDate? = null,
    val tagsJson: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)

fun GoalEntity.toDomain() = Goal(
    id           = id,
    title        = title,
    note         = note,
    emoji        = emoji,
    colorHex     = colorHex,
    frequency    = runCatching { GoalFrequency.valueOf(frequency) }.getOrDefault(GoalFrequency.DAILY),
    targetCount  = targetCount,
    currentCount = currentCount,
    sortOrder    = sortOrder,
    isCompleted  = isCompleted,
    deadline     = deadline,
    tags         = if (tagsJson.isBlank()) emptyList() else tagsJson.split("|").map { it.trim() }.filter { it.isNotEmpty() },
    createdAt    = createdAt,
    updatedAt    = updatedAt,
    deletedAt    = deletedAt,
)

fun Goal.toEntity() = GoalEntity(
    id           = id,
    title        = title,
    note         = note,
    emoji        = emoji,
    colorHex     = colorHex,
    frequency    = frequency.name,
    targetCount  = targetCount,
    currentCount = currentCount,
    sortOrder    = sortOrder,
    isCompleted  = isCompleted,
    deadline     = deadline,
    tagsJson     = tags.joinToString("|"),
    createdAt    = createdAt,
    updatedAt    = updatedAt,
    deletedAt    = deletedAt,
)
