package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.ReminderDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Reminder
import com.onlyflows.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ReminderRepositoryImpl(
    private val dao: ReminderDao,
    private val syncDao: SyncDao? = null,
) : ReminderRepository {

    override fun observeByTask(taskId: String): Flow<List<Reminder>> =
        dao.observeByTask(taskId).map { it.map { e -> e.toDomain() } }

    override suspend fun add(reminder: Reminder) = dao.insert(reminder.toEntity())

    override suspend fun delete(id: String) {
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "reminders", id, LocalDateTime.now()))
        dao.delete(id)
    }

    override suspend fun deleteByTask(taskId: String) = dao.deleteByTask(taskId)

    override suspend fun getFutureReminders(): List<Reminder> =
        dao.getFutureReminders(LocalDateTime.now().toString()).map { it.toDomain() }
}
