package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.DateTrackerDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.DateTracker
import com.onlyflows.domain.repository.DateTrackerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DateTrackerRepositoryImpl(
    private val dao: DateTrackerDao,
) : DateTrackerRepository {

    override fun observeAll(): Flow<List<DateTracker>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): DateTracker? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(tracker: DateTracker) =
        dao.insert(tracker.toEntity())

    override suspend fun update(tracker: DateTracker) =
        dao.update(tracker.toEntity())

    override suspend fun delete(id: String) =
        dao.softDelete(id)
}
