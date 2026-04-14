package com.onlyflows.domain.repository

import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.StatusLogEntry
import com.onlyflows.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeAll(): Flow<List<Project>>
    fun observeArchived(): Flow<List<Project>>
    fun observeById(id: String): Flow<Project?>
    fun observeChildren(parentId: String): Flow<List<Project>>
    suspend fun insert(project: Project)
    suspend fun update(project: Project)
    suspend fun delete(id: String)
    suspend fun archive(id: String)
    suspend fun updateStatus(id: String, status: ProjectStatus)
    suspend fun insertSection(section: Section)
    suspend fun updateSection(section: Section)
    suspend fun deleteSection(sectionId: String)
    fun observeStatusLog(projectId: String): Flow<List<StatusLogEntry>>
}

interface FolderRepository {
    fun observeAll(): Flow<List<Folder>>
    suspend fun insert(folder: Folder)
    suspend fun update(folder: Folder)
    suspend fun delete(id: String)
}

interface TagRepository {
    fun observeAll(): Flow<List<Tag>>
    suspend fun insert(tag: Tag)
    suspend fun update(tag: Tag)
    suspend fun delete(id: String)
}
