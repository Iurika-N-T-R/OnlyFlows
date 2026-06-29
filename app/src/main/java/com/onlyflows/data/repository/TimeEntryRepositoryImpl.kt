package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.dao.TimeEntryDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.TimeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeEntryRepository(
    private val dao: TimeEntryDao,
    private val actionLog: ActionLogRepository,
    private val syncDao: SyncDao,
    private val syncthingWriter: LanSyncWriter,
) {

    private suspend fun log(type: ActionType, entryId: String, taskId: String, extra: String? = null) {
        actionLog.insert(
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

    fun observeByTask(taskId: String): Flow<List<TimeEntry>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    fun observeByProject(projectId: String): Flow<List<TimeEntry>> =
        dao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun getActiveEntry(taskId: String): TimeEntry? =
        dao.getActiveEntry(taskId)?.toDomain()

    suspend fun insert(entry: TimeEntry) {
        dao.insert(entry.toEntity())
        syncthingWriter.writeTimeEntry(entry.toEntity())
        log(ActionType.TIMER_STARTED, entry.id, entry.taskId)
    }

    suspend fun stopEntry(id: String) {
        val entity = dao.getById(id) ?: return
        val endedAt = LocalDateTime.now()
        val updated = entity.copy(endedAt = endedAt)
        dao.update(updated)
        syncthingWriter.writeTimeEntry(updated)
        val duration = ChronoUnit.SECONDS.between(entity.startedAt, endedAt)
        log(ActionType.TIMER_STOPPED, id, entity.taskId, extra = "${duration}s")
    }

    suspend fun getTotalCompletedSeconds(taskId: String): Long =
        dao.getByTask(taskId)
            .filter { it.endedAt != null }
            .sumOf { ChronoUnit.SECONDS.between(it.startedAt, it.endedAt!!) }

    suspend fun getTotalSecondsForProject(projectId: String): Long =
        dao.getByProject(projectId)
            .filter { it.endedAt != null }
            .sumOf { ChronoUnit.SECONDS.between(it.startedAt, it.endedAt!!) }

    suspend fun deleteByTask(taskId: String) {
        val now = LocalDateTime.now()
        dao.getByTask(taskId).forEach { entry ->
            syncDao.insertTombstone(DeletedEntityEntity("tomb_${entry.id}", "time_entries", entry.id, now))
            syncthingWriter.writeTombstone("time_entries", entry.id, now)
        }
        dao.deleteByTask(taskId)
    }
}
