package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.GoalDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(private val dao: GoalDao) : GoalRepository {

    override fun observeAll(): Flow<List<Goal>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeActive(): Flow<List<Goal>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeById(id: String): Flow<Goal?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun insert(goal: Goal) = dao.insert(goal.toEntity())

    override suspend fun update(goal: Goal) = dao.update(goal.toEntity())

    override suspend fun setProgress(id: String, count: Int) = dao.setProgress(id, count)

    override suspend fun setCompleted(id: String, completed: Boolean) = dao.setCompleted(id, completed)

    override suspend fun softDelete(id: String) = dao.softDelete(id)

    override suspend fun delete(id: String) = dao.delete(id)
}
