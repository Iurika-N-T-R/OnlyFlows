package com.onlyflows.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class GoalFrequency { ONCE, DAILY, WEEKLY, MONTHLY, YEARLY }
enum class GoalStatus { ON_TRACK, AT_RISK, NOT_STARTED, COMPLETED }

@Immutable
data class Goal(
    val id: String,
    val title: String,
    val note: String = "",
    val emoji: String = "🎯",
    val colorHex: String = "#4A90D9",
    val frequency: GoalFrequency = GoalFrequency.DAILY,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val deadline: LocalDate? = null,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)

fun Goal.computeStatus(): GoalStatus = when {
    isCompleted -> GoalStatus.COMPLETED
    currentCount == 0 -> GoalStatus.NOT_STARTED
    deadline != null -> {
        val totalDays = ChronoUnit.DAYS.between(createdAt.toLocalDate(), deadline).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now()).coerceAtLeast(0)
        val expected = elapsed.toFloat() / totalDays
        val actual = currentCount.toFloat() / targetCount
        if (actual >= expected * 0.85f) GoalStatus.ON_TRACK else GoalStatus.AT_RISK
    }
    else -> GoalStatus.ON_TRACK
}
