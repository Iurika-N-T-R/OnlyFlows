package com.onlyflows.domain.repository

import com.onlyflows.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeByProject(projectId: String): Flow<List<Note>>
    suspend fun insert(note: Note)
    suspend fun update(note: Note)
    suspend fun delete(id: String)
}
