package com.onlyflows.domain.repository

import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.DailyCount
import kotlinx.coroutines.flow.Flow

interface ActionLogRepository {
    fun observeAll(): Flow<List<ActionLog>>
    fun observeByEntity(entityId: String): Flow<List<ActionLog>>
    fun observeCountByType(type: String): Flow<Int>
    fun observeCompletedPerDay(since: String): Flow<List<DailyCount>>
    suspend fun insert(entry: ActionLog)
}
