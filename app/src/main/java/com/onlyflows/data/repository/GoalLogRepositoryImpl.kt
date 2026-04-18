package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.GoalLogDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.GoalLog
import com.onlyflows.domain.repository.GoalLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalLogRepositoryImpl(private val dao: GoalLogDao) : GoalLogRepository {
    override fun observeByGoal(goalId: String): Flow<List<GoalLog>> =
        dao.observeByGoal(goalId).map { it.map { e -> e.toDomain() } }

    override suspend fun insert(log: GoalLog) = dao.insert(log.toEntity())
    override suspend fun setStarred(id: String, starred: Boolean) = dao.setStarred(id, starred)
    override suspend fun softDelete(id: String) = dao.softDelete(id)
}
