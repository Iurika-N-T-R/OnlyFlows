package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.DateTrackerDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.domain.model.DateTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class DateTrackerRepository(
    private val dao: DateTrackerDao,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
) {

    fun observeAll(): Flow<List<DateTracker>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    suspend fun getById(id: String): DateTracker? =
        dao.getById(id)?.toDomain()

    suspend fun insert(tracker: DateTracker) {
        val entity = tracker.toEntity()
        dao.insert(entity)
        lanSyncWriter.writeDateTracker(entity)
    }

    suspend fun update(tracker: DateTracker) {
        val entity = tracker.toEntity()
        dao.update(entity)
        lanSyncWriter.writeDateTracker(entity)
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "date_trackers", id, now))
        lanSyncWriter.writeTombstone("date_trackers", id, now)
        dao.softDelete(id)
    }
}
