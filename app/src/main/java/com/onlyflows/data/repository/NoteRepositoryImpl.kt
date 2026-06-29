package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.NoteDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.lan.LanSyncWriter
import com.onlyflows.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class NoteRepository(
    private val noteDao: NoteDao,
    private val syncDao: SyncDao,
    private val syncthingWriter: LanSyncWriter,
) {
    fun observeByProject(projectId: String): Flow<List<Note>> =
        noteDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Note? = noteDao.getById(id)?.toDomain()

    suspend fun insert(note: Note) {
        noteDao.insert(note.toEntity())
        syncthingWriter.writeNote(note.toEntity())
    }

    suspend fun update(note: Note) {
        noteDao.update(note.toEntity())
        syncthingWriter.writeNote(note.toEntity())
    }

    suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", "notes", id, now))
        syncthingWriter.writeTombstone("notes", id, now)
        noteDao.delete(id)
    }
}
