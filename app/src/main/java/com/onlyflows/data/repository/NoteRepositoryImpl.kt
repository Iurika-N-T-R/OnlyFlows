package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.NoteDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Note
import com.onlyflows.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val syncDao: SyncDao? = null,
) : NoteRepository {
    override fun observeByProject(projectId: String): Flow<List<Note>> =
        noteDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(note: Note)      { noteDao.insert(note.toEntity()) }
    override suspend fun update(note: Note)      { noteDao.update(note.toEntity()) }
    override suspend fun delete(id: String) {
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "notes", id, LocalDateTime.now()))
        noteDao.delete(id)
    }
}
