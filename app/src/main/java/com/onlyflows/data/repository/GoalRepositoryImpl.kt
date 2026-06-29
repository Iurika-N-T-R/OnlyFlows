package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.GoalDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class GoalRepository(
    private val dao: GoalDao,
    private val syncDao: SyncDao,
    private val syncthingWriter: SyncthingWriter,
) {

    fun observeAll(): Flow<List<Goal>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    fun observeActive(): Flow<List<Goal>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    fun observeById(id: String): Flow<Goal?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun insert(goal: Goal) {
        dao.insert(goal.toEntity())
        syncthingWriter.writeGoal(goal.toEntity())
    }

    suspend fun update(goal: Goal) {
        dao.update(goal.toEntity())
        syncthingWriter.writeGoal(goal.toEntity())
    }

    suspend fun setProgress(id: String, count: Int) {
        dao.setProgress(id, count)
        dao.getById(id)?.let { syncthingWriter.writeGoal(it) }
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        dao.setCompleted(id, completed)
        dao.getById(id)?.let { syncthingWriter.writeGoal(it) }
    }

    suspend fun softDelete(id: String) {
        val now = LocalDateTime.now()
        dao.softDelete(id)
        syncthingWriter.writeTombstone("goals", id, now)
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "goals", id, now))
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "goals", id, now))
        syncthingWriter.writeTombstone("goals", id, now)
        dao.delete(id)
    }
}
