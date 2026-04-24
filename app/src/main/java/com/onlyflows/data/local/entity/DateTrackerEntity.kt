package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.DateTracker
import com.onlyflows.domain.model.DateTrackerCategory
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "date_trackers",
    indices   = [Index("deletedAt")],
)
data class DateTrackerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String        = "📅",
    val targetDate: LocalDate,
    val category: String     = "COUNTDOWN",
    val isAnnual: Boolean    = false,
    val sortOrder: Int       = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)

fun DateTrackerEntity.toDomain() = DateTracker(
    id         = id,
    name       = name,
    emoji      = emoji,
    targetDate = targetDate,
    category   = runCatching { DateTrackerCategory.valueOf(category) }.getOrDefault(DateTrackerCategory.COUNTDOWN),
    isAnnual   = isAnnual,
    sortOrder  = sortOrder,
    createdAt  = createdAt,
)

fun DateTracker.toEntity() = DateTrackerEntity(
    id         = id,
    name       = name,
    emoji      = emoji,
    targetDate = targetDate,
    category   = category.name,
    isAnnual   = isAnnual,
    sortOrder  = sortOrder,
    createdAt  = createdAt,
)
