package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.NoteDao
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Note
import com.onlyflows.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(private val noteDao: NoteDao) : NoteRepository {
    override fun observeByProject(projectId: String): Flow<List<Note>> =
        noteDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(note: Note)      { noteDao.insert(note.toEntity()) }
    override suspend fun update(note: Note)      { noteDao.update(note.toEntity()) }
    override suspend fun delete(id: String)      { noteDao.delete(id) }
}
