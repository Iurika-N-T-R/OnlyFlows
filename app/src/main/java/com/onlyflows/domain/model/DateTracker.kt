package com.onlyflows.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class DateTrackerCategory { COUNTDOWN, ANNIVERSARY, EVENT, HOLIDAY, OTHER }

data class DateTracker(
    val id: String,
    val name: String,
    val emoji: String,
    val targetDate: LocalDate,
    val category: DateTrackerCategory,
    val isAnnual: Boolean,
    val sortOrder: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
