package com.onlyflows.domain.model

import java.time.LocalDateTime

data class GoalLog(
    val id: String,
    val goalId: String,
    val label: String,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val isStarred: Boolean = false,
    val loggedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,
)
