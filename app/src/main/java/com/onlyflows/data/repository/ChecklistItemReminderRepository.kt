package com.onlyflows.data.repository

import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.data.local.dao.ChecklistItemReminderDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.ChecklistItemReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ChecklistItemReminderRepository(
    private val dao: ChecklistItemReminderDao,
    private val syncDao: SyncDao,
    private val syncthingWriter: LanSyncWriter,
) {

    fun observeByItem(itemId: String): Flow<List<ChecklistItemReminder>> =
        dao.observeByItem(itemId).map { it.map { e -> e.toDomain() } }

    fun observeAll(): Flow<List<ChecklistItemReminder>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    suspend fun set(reminder: ChecklistItemReminder) {
        val entity = reminder.toEntity()
        dao.upsert(entity)
        syncthingWriter.writeChecklistItemReminder(entity)
    }

    suspend fun clear(itemId: String) {
        dao.getByItem(itemId).forEach { r ->
            val now = LocalDateTime.now()
            syncDao.insertTombstone(DeletedEntityEntity("tomb_${r.id}", "checklist_item_reminders", r.id, now))
            syncthingWriter.writeTombstone("checklist_item_reminders", r.id, now)
        }
        dao.deleteByItem(itemId)
    }

    suspend fun getFutureReminders(): List<ChecklistItemReminder> =
        dao.getFutureReminders(LocalDateTime.now().toString()).map { it.toDomain() }
}
