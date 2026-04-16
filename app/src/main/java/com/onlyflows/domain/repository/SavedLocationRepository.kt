package com.onlyflows.domain.repository

import com.onlyflows.domain.model.SavedLocation
import kotlinx.coroutines.flow.Flow

interface SavedLocationRepository {
    fun observeAll(): Flow<List<SavedLocation>>
    suspend fun getById(id: String): SavedLocation?
    suspend fun getAll(): List<SavedLocation>
    suspend fun insert(location: SavedLocation)
    suspend fun delete(location: SavedLocation)
}
