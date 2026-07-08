package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.ReminderDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ReminderRepository(
    private val dao: ReminderDao,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
) {

    fun observeByTask(taskId: String): Flow<List<Reminder>> =
        dao.observeByTask(taskId).map { it.map { e -> e.toDomain() } }

    suspend fun add(reminder: Reminder) {
        dao.insert(reminder.toEntity())
        lanSyncWriter.writeReminder(reminder.toEntity())
    }

    suspend fun update(reminder: Reminder) {
        val entity = reminder.toEntity()
        dao.upsert(entity)
        lanSyncWriter.writeReminder(entity)
    }

    suspend fun getById(id: String): Reminder? =
        dao.getById(id)?.toDomain()

    suspend fun getByTask(taskId: String): List<Reminder> =
        dao.getByTask(taskId).map { it.toDomain() }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "reminders", id, now))
        lanSyncWriter.writeTombstone("reminders", id, now)
        dao.delete(id)
    }

    suspend fun deleteByTask(taskId: String) {
        val now = LocalDateTime.now()
        dao.getByTask(taskId).forEach { reminder ->
            syncDao.insertTombstone(DeletedEntityEntity("tomb_${reminder.id}", "reminders", reminder.id, now))
            lanSyncWriter.writeTombstone("reminders", reminder.id, now)
        }
        dao.deleteByTask(taskId)
    }

    suspend fun getFutureReminders(): List<Reminder> =
        dao.getFutureReminders(LocalDateTime.now().toString()).map { it.toDomain() }

    /** Recurring reminders whose scheduledAt slipped into the past (missed firings). */
    suspend fun getPastRecurring(): List<Reminder> =
        dao.getPastRecurring(LocalDateTime.now().toString()).map { it.toDomain() }
}
