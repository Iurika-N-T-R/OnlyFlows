package com.onlyflows.data.repository

import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.data.local.dao.SavedColorDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.SavedColorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * The user's own colour swatches, shared across paired devices.
 *
 * The hex doubles as the row id, so saving the same colour on two devices converges on one row
 * instead of producing duplicates that sync back and forth.
 */
class SavedColorRepository(
    private val dao: SavedColorDao,
    private val syncDao: SyncDao,
    private val lanSyncWriter: LanSyncWriter,
) {

    /** Newest first. */
    fun observeAll(): Flow<List<String>> =
        dao.observeAll().map { list -> list.map { it.id } }

    suspend fun save(hex: String) {
        val id = hex.trim().uppercase()
        if (id.isBlank()) return
        val now = LocalDateTime.now()

        // Everything already saved shifts down one, so the newest sits at the front without
        // needing a separate "saved at" column to sort on.
        val existing = dao.getAll().filterNot { it.id == id }
        val reordered = existing.mapIndexed { index, entity ->
            entity.copy(sortOrder = index + 1, updatedAt = now)
        }
        val head = SavedColorEntity(id = id, sortOrder = 0, updatedAt = now)

        val kept = (listOf(head) + reordered).take(MAX_SAVED_COLORS)
        dao.upsertAll(kept)
        kept.forEach { lanSyncWriter.writeSavedColor(it) }

        // Anything pushed past the cap goes, tombstone included, or it would come straight
        // back from the peer on the next delta.
        (listOf(head) + reordered).drop(MAX_SAVED_COLORS).forEach { remove(it.id) }
    }

    suspend fun remove(hex: String) {
        val id = hex.trim().uppercase()
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "saved_colors", id, now))
        lanSyncWriter.writeTombstone("saved_colors", id, now)
        dao.delete(id)
    }

    /** One-time move of the swatches that 0.81.0 wrote to DataStore. */
    suspend fun importLegacy(colors: List<String>) {
        if (colors.isEmpty() || dao.count() > 0) return
        val now = LocalDateTime.now()
        val entities = colors.mapIndexed { index, hex ->
            SavedColorEntity(id = hex.trim().uppercase(), sortOrder = index, updatedAt = now)
        }
        dao.upsertAll(entities)
        entities.forEach { lanSyncWriter.writeSavedColor(it) }
    }

    private companion object {
        /** Beyond this the swatch row stops being a palette and becomes a second problem. */
        const val MAX_SAVED_COLORS = 24
    }
}
