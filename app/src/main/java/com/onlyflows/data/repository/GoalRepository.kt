package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.GoalDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.data.local.entity.GoalEntity
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.model.GoalFrequency
import com.onlyflows.domain.model.currentPeriodStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class GoalRepository(
    private val dao: GoalDao,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
) {

    fun observeAll(): Flow<List<Goal>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    fun observeActive(): Flow<List<Goal>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    fun observeById(id: String): Flow<Goal?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun getById(id: String): GoalEntity? = dao.getById(id)

    /** Returns the goal with its period rolled over if expired (DAILY resets at midnight,
     *  WEEKLY on Monday, MONTHLY/YEARLY on the 1st). ONCE never resets. A null periodStart
     *  (pre-v38 goal) is anchored to the current period without resetting. */
    suspend fun rolloverIfExpired(goal: GoalEntity): GoalEntity {
        val freq  = runCatching { GoalFrequency.valueOf(goal.frequency) }.getOrNull() ?: return goal
        val start = currentPeriodStart(freq) ?: return goal
        when {
            goal.periodStart == null          -> dao.anchorPeriod(goal.id, start)
            goal.periodStart.isBefore(start)  -> dao.resetPeriod(goal.id, start)
            else                              -> return goal
        }
        val updated = dao.getById(goal.id) ?: return goal
        lanSyncWriter.writeGoal(updated)
        return updated
    }

    /** Roll over every active periodic goal — called when a goals screen opens. */
    suspend fun rolloverExpiredPeriods() = dao.getAll().forEach { rolloverIfExpired(it) }

    suspend fun insert(goal: Goal) {
        dao.insert(goal.toEntity())
        lanSyncWriter.writeGoal(goal.toEntity())
    }

    suspend fun update(goal: Goal) {
        dao.update(goal.toEntity())
        lanSyncWriter.writeGoal(goal.toEntity())
    }

    suspend fun setProgress(id: String, count: Int) {
        dao.setProgress(id, count)
        dao.getById(id)?.let { lanSyncWriter.writeGoal(it) }
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        dao.setCompleted(id, completed)
        dao.getById(id)?.let { lanSyncWriter.writeGoal(it) }
    }

    suspend fun softDelete(id: String) {
        val now = LocalDateTime.now()
        dao.softDelete(id)
        lanSyncWriter.writeTombstone("goals", id, now)
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "goals", id, now))
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "goals", id, now))
        lanSyncWriter.writeTombstone("goals", id, now)
        dao.delete(id)
    }
}
