package com.onlyflows.domain.repository

import com.onlyflows.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface ReminderRepository {
    fun observeByTask(taskId: String): Flow<List<Reminder>>
    suspend fun add(reminder: Reminder)
    suspend fun delete(id: String)
    suspend fun deleteByTask(taskId: String)
    suspend fun getFutureReminders(): List<Reminder>
}
