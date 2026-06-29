package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.GoalLogDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.GoalLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class GoalLogRepository(
    private val dao: GoalLogDao,
    private val syncDao: SyncDao,
    private val syncthingWriter: SyncthingWriter,
) {

    fun observeByGoal(goalId: String): Flow<List<GoalLog>> =
        dao.observeByGoal(goalId).map { it.map { e -> e.toDomain() } }

    suspend fun insert(log: GoalLog) {
        dao.insert(log.toEntity())
        syncthingWriter.writeGoalLog(log.toEntity())
    }

    suspend fun setStarred(id: String, starred: Boolean) {
        dao.setStarred(id, starred)
        dao.getById(id)?.let { syncthingWriter.writeGoalLog(it) }
    }

    suspend fun softDelete(id: String) {
        val now = LocalDateTime.now()
        dao.softDelete(id)
        syncthingWriter.writeTombstone("goal_logs", id, now)
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "goal_logs", id, now))
    }
}
