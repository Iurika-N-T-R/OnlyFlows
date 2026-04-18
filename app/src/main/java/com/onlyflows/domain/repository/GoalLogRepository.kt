package com.onlyflows.domain.repository

import com.onlyflows.domain.model.GoalLog
import kotlinx.coroutines.flow.Flow

interface GoalLogRepository {
    fun observeByGoal(goalId: String): Flow<List<GoalLog>>
    suspend fun insert(log: GoalLog)
    suspend fun setStarred(id: String, starred: Boolean)
    suspend fun softDelete(id: String)
}
