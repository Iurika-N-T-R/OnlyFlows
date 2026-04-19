package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.GoalDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class GoalRepositoryImpl(
    private val dao: GoalDao,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : GoalRepository {

    override fun observeAll(): Flow<List<Goal>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeActive(): Flow<List<Goal>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeById(id: String): Flow<Goal?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun insert(goal: Goal) {
        dao.insert(goal.toEntity())
        syncthingWriter?.writeGoal(goal.toEntity())
    }

    override suspend fun update(goal: Goal) {
        dao.update(goal.toEntity())
        syncthingWriter?.writeGoal(goal.toEntity())
    }

    override suspend fun setProgress(id: String, count: Int) = dao.setProgress(id, count)

    override suspend fun setCompleted(id: String, completed: Boolean) = dao.setCompleted(id, completed)

    override suspend fun softDelete(id: String) {
        val now = LocalDateTime.now()
        dao.softDelete(id)
        syncthingWriter?.writeTombstone("goals", id, now)
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "goals", id, now))
    }

    override suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "goals", id, now))
        syncthingWriter?.writeTombstone("goals", id, now)
        dao.delete(id)
    }
}
