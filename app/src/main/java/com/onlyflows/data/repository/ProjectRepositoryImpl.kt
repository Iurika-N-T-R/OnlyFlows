package com.onlyflows.data.repository

import com.onlyflows.data.local.dao.FolderDao
import com.onlyflows.data.local.dao.ProjectDao
import com.onlyflows.data.local.dao.SectionDao
import com.onlyflows.data.local.dao.StatusLogDao
import com.onlyflows.data.local.dao.TagDao
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.StatusLogEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.StatusLogEntry
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.repository.FolderRepository
import com.onlyflows.domain.repository.ProjectRepository
import com.onlyflows.domain.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val sectionDao: SectionDao,
    private val statusLogDao: StatusLogDao,
    private val taskDao: TaskDao,
) : ProjectRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<Project>> =
        projectDao.observeActive().flatMapLatest { projects ->
            val flows = projects.map { project ->
                sectionDao.observeByProject(project.id).map { sections ->
                    project.toDomain(sections)
                }
            }
            if (flows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(flows) { it.toList() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeById(id: String): Flow<Project?> =
        projectDao.observeById(id).flatMapLatest { entity ->
            if (entity == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                sectionDao.observeByProject(id).map { sections ->
                    entity.toDomain(sections)
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChildren(parentId: String): Flow<List<Project>> =
        projectDao.observeChildren(parentId).flatMapLatest { projects ->
            val flows = projects.map { project ->
                sectionDao.observeByProject(project.id).map { sections ->
                    project.toDomain(sections)
                }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    override suspend fun insert(project: Project) {
        projectDao.insert(project.toEntity())
        project.sections.forEach { sectionDao.insert(it.toEntity()) }
        // Insert initial status log entry
        statusLogDao.insert(
            StatusLogEntity(
                id        = "log_${project.id}_${System.currentTimeMillis()}",
                projectId = project.id,
                status    = project.status.name,
                changedAt = LocalDateTime.now().toString(),
            )
        )
    }

    override suspend fun update(project: Project) {
        projectDao.update(project.copy(updatedAt = LocalDateTime.now()).toEntity())
    }

    override suspend fun delete(id: String) {
        projectDao.delete(id)
    }

    override suspend fun archive(id: String) {
        projectDao.archive(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeArchived(): Flow<List<Project>> =
        projectDao.observeArchived().flatMapLatest { projects ->
            val flows = projects.map { project ->
                sectionDao.observeByProject(project.id).map { sections ->
                    project.toDomain(sections)
                }
            }
            if (flows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(flows) { it.toList() }
            }
        }

    override suspend fun updateStatus(id: String, status: ProjectStatus) {
        projectDao.updateStatus(id, status.name)
        statusLogDao.insert(
            StatusLogEntity(
                id        = "log_${id}_${System.currentTimeMillis()}",
                projectId = id,
                status    = status.name,
                changedAt = LocalDateTime.now().toString(),
            )
        )
    }

    override suspend fun insertSection(section: Section) {
        sectionDao.insert(section.toEntity())
    }

    override suspend fun updateSection(section: Section) {
        sectionDao.update(section.toEntity())
    }

    override suspend fun deleteSection(sectionId: String) {
        taskDao.clearSection(sectionId, java.time.LocalDateTime.now().toString())
        sectionDao.delete(sectionId)
    }

    override fun observeStatusLog(projectId: String): Flow<List<StatusLogEntry>> =
        statusLogDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }
}

class FolderRepositoryImpl(private val folderDao: FolderDao) : FolderRepository {

    override fun observeAll(): Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(folder: Folder) { folderDao.insert(folder.toEntity()) }
    override suspend fun update(folder: Folder) { folderDao.update(folder.toEntity()) }
    override suspend fun delete(id: String)     { folderDao.delete(id) }
}

class TagRepositoryImpl(private val tagDao: TagDao) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(tag: Tag)  { tagDao.insert(tag.toEntity()) }
    override suspend fun update(tag: Tag)  { tagDao.update(tag.toEntity()) }
    override suspend fun delete(id: String) { tagDao.delete(id) }
}
