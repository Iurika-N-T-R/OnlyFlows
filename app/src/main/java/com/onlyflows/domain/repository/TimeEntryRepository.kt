package com.onlyflows.domain.repository

import com.onlyflows.domain.model.TimeEntry
import kotlinx.coroutines.flow.Flow

interface TimeEntryRepository {
    fun observeByTask(taskId: String): Flow<List<TimeEntry>>
    suspend fun getActiveEntry(taskId: String): TimeEntry?
    suspend fun insert(entry: TimeEntry)
    suspend fun stopEntry(id: String)           // sets endedAt = now
    suspend fun getTotalCompletedSeconds(taskId: String): Long
    suspend fun getTotalSecondsForProject(projectId: String): Long
    suspend fun deleteByTask(taskId: String)
}
