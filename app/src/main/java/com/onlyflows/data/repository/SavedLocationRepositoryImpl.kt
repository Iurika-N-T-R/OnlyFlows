package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.SavedLocationDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.SavedLocation
import com.onlyflows.domain.repository.SavedLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class SavedLocationRepositoryImpl(
    private val dao: SavedLocationDao,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : SavedLocationRepository {

    override fun observeAll(): Flow<List<SavedLocation>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): SavedLocation? =
        dao.getById(id)?.toDomain()

    override suspend fun getAll(): List<SavedLocation> =
        dao.getAll().map { it.toDomain() }

    override suspend fun insert(location: SavedLocation) {
        dao.insert(location.toEntity())
        syncthingWriter?.writeSavedLocation(location.toEntity())
    }

    override suspend fun delete(location: SavedLocation) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_${location.id}", "saved_locations", location.id, now))
        syncthingWriter?.writeTombstone("saved_locations", location.id, now)
        dao.delete(location.toEntity())
    }
}
