package com.onlyflows.domain.repository

import com.onlyflows.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeAll(): Flow<List<Goal>>
    fun observeActive(): Flow<List<Goal>>
    fun observeById(id: String): Flow<Goal?>
    suspend fun insert(goal: Goal)
    suspend fun update(goal: Goal)
    suspend fun setProgress(id: String, count: Int)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun softDelete(id: String)
    suspend fun delete(id: String)
}
