package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.TimeEntryDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.TimeEntry
import com.onlyflows.domain.repository.ActionLogRepository
import com.onlyflows.domain.repository.TimeEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeEntryRepositoryImpl(
    private val dao: TimeEntryDao,
    private val actionLog: ActionLogRepository? = null,
) : TimeEntryRepository {

    private suspend fun log(type: ActionType, entryId: String, taskId: String, extra: String? = null) {
        actionLog?.insert(
            ActionLog(
                id         = "al_${System.currentTimeMillis()}_${type.name}",
                actionType = type,
                entityId   = taskId,
                entityType = "timer",
                label      = "Timer for task $taskId",
                extra      = extra,
                occurredAt = LocalDateTime.now(),
            )
        )
    }

    override fun observeByTask(taskId: String): Flow<List<TimeEntry>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    override fun observeByProject(projectId: String): Flow<List<TimeEntry>> =
        dao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveEntry(taskId: String): TimeEntry? =
        dao.getActiveEntry(taskId)?.toDomain()

    override suspend fun insert(entry: TimeEntry) {
        dao.insert(entry.toEntity())
        log(ActionType.TIMER_STARTED, entry.id, entry.taskId)
    }

    override suspend fun stopEntry(id: String) {
        val entity = dao.getById(id) ?: return
        val endedAt = LocalDateTime.now()
        dao.update(entity.copy(endedAt = endedAt))
        val duration = ChronoUnit.SECONDS.between(entity.startedAt, endedAt)
        log(ActionType.TIMER_STOPPED, id, entity.taskId, extra = "${duration}s")
    }

    override suspend fun getTotalCompletedSeconds(taskId: String): Long =
        dao.getByTask(taskId)
            .filter { it.endedAt != null }
            .sumOf { ChronoUnit.SECONDS.between(it.startedAt, it.endedAt!!) }

    override suspend fun getTotalSecondsForProject(projectId: String): Long =
        dao.getByProject(projectId)
            .filter { it.endedAt != null }
            .sumOf { ChronoUnit.SECONDS.between(it.startedAt, it.endedAt!!) }

    override suspend fun deleteByTask(taskId: String) =
        dao.deleteByTask(taskId)
}
