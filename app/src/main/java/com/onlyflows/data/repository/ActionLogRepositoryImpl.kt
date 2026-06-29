package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.ActionLogDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.DailyCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActionLogRepository(private val dao: ActionLogDao) {

    fun observeAll(): Flow<List<ActionLog>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByEntity(entityId: String): Flow<List<ActionLog>> =
        dao.observeByEntity(entityId).map { list -> list.map { it.toDomain() } }

    fun observeCountByType(type: String): Flow<Int> =
        dao.observeCountByType(type)

    fun observeCompletedPerDay(since: String): Flow<List<DailyCount>> =
        dao.observeCompletedPerDay(since).map { list ->
            list.map { DailyCount(it.date, it.count) }
        }

    suspend fun insert(entry: ActionLog) =
        dao.insert(entry.toEntity())
}
