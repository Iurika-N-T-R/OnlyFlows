package com.onlyflows.domain.repository

import com.onlyflows.domain.model.DateTracker
import kotlinx.coroutines.flow.Flow

interface DateTrackerRepository {
    fun observeAll(): Flow<List<DateTracker>>
    suspend fun getById(id: String): DateTracker?
    suspend fun insert(tracker: DateTracker)
    suspend fun update(tracker: DateTracker)
    suspend fun delete(id: String)
}
