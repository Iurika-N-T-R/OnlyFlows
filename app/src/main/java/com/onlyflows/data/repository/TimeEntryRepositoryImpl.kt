package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.TimeEntryDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.TimeEntry
import com.onlyflows.domain.repository.TimeEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeEntryRepositoryImpl(private val dao: TimeEntryDao) : TimeEntryRepository {

    override fun observeByTask(taskId: String): Flow<List<TimeEntry>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveEntry(taskId: String): TimeEntry? =
        dao.getActiveEntry(taskId)?.toDomain()

    override suspend fun insert(entry: TimeEntry) =
        dao.insert(entry.toEntity())

    override suspend fun stopEntry(id: String) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(endedAt = LocalDateTime.now()))
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
