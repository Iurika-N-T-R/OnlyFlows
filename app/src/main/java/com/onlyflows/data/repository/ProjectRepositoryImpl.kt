package com.onlyflows.data.repository

import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.local.dao.FolderDao
import com.onlyflows.data.local.dao.ProjectDao
import com.onlyflows.data.local.dao.SectionDao
import com.onlyflows.data.local.dao.StatusLogDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.dao.TagDao
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.StatusLogEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.data.local.entity.toEntity
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.StatusLogEntry
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.repository.ActionLogRepository
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
    private val actionLog: ActionLogRepository? = null,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : ProjectRepository {

    private suspend fun tombstone(id: String, type: String) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", type, id, now))
        syncthingWriter?.writeTombstone(type, id, now)
    }

    private suspend fun log(type: ActionType, projectId: String, label: String, extra: String? = null) {
        actionLog?.insert(
            ActionLog(
                id         = "al_${System.currentTimeMillis()}_${type.name}",
                actionType = type,
                entityId   = projectId,
                entityType = "project",
                label      = label,
                extra      = extra,
                occurredAt = LocalDateTime.now(),
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<Project>> =
        projectDao.observeActiveProjects().flatMapLatest { projects ->
            val flows = projects.map { project ->
                sectionDao.observeByProject(project.id).map { sections ->
                    project.toDomain(sections)
                }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllItems(): Flow<List<Project>> =
        projectDao.observeActive().flatMapLatest { projects ->
            val flows = projects.map { project ->
                sectionDao.observeByProject(project.id).map { sections ->
                    project.toDomain(sections)
                }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeById(id: String): Flow<Project?> =
        projectDao.observeById(id).flatMapLatest { entity ->
            if (entity == null) kotlinx.coroutines.flow.flowOf(null)
            else sectionDao.observeByProject(id).map { sections -> entity.toDomain(sections) }
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
        statusLogDao.insert(
            StatusLogEntity(
                id        = "log_${project.id}_${System.currentTimeMillis()}",
                projectId = project.id,
                status    = project.status.name,
                changedAt = LocalDateTime.now().toString(),
            )
        )
        syncthingWriter?.writeProject(project.toEntity())
        project.sections.forEach { syncthingWriter?.writeSection(it.toEntity()) }
        log(ActionType.PROJECT_CREATED, project.id, project.title)
        AppLog.i("ProjectRepo", "Project created: ${project.title}")
    }

    override suspend fun update(project: Project) {
        val entity = project.copy(updatedAt = LocalDateTime.now()).toEntity()
        projectDao.update(entity)
        syncthingWriter?.writeProject(entity)
    }

    override suspend fun delete(id: String) {
        tombstone(id, "projects")
        projectDao.delete(id)
        log(ActionType.PROJECT_DELETED, id, id)
        AppLog.i("ProjectRepo", "Project deleted: $id")
    }

    override suspend fun archive(id: String) {
        projectDao.archive(id)
        // No full entity available without re-read; archive is a soft status change — importer handles on next scan
    }

    override suspend fun unarchive(id: String) {
        projectDao.unarchive(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeArchived(): Flow<List<Project>> =
        projectDao.observeArchived().flatMapLatest { projects ->
            val flows = projects.map { project ->
                sectionDao.observeByProject(project.id).map { sections ->
                    project.toDomain(sections)
                }
            }
            if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else combine(flows) { it.toList() }
        }

    override suspend fun updateStatus(id: String, status: ProjectStatus) {
        projectDao.updateStatus(id, status.name, LocalDateTime.now().toString())
        statusLogDao.insert(
            StatusLogEntity(
                id        = "log_${id}_${System.currentTimeMillis()}",
                projectId = id,
                status    = status.name,
                changedAt = LocalDateTime.now().toString(),
            )
        )
        projectDao.getById(id)?.let { syncthingWriter?.writeProject(it) }
        log(ActionType.PROJECT_STATUS_CHANGED, id, id, extra = status.name)
    }

    override suspend fun insertSection(section: Section) {
        sectionDao.insert(section.toEntity())
        syncthingWriter?.writeSection(section.toEntity())
        log(ActionType.SECTION_CREATED, section.id, section.title)
    }

    override suspend fun updateSection(section: Section) {
        val entity = section.toEntity().copy(updatedAt = LocalDateTime.now())
        sectionDao.update(entity)
        syncthingWriter?.writeSection(entity)
    }

    override suspend fun deleteSection(sectionId: String) {
        tombstone(sectionId, "sections")
        val now = LocalDateTime.now().toString()
        taskDao.softDeleteBySection(sectionId, now)
        sectionDao.delete(sectionId)
        log(ActionType.SECTION_DELETED, sectionId, sectionId)
    }

    override fun observeStatusLog(projectId: String): Flow<List<StatusLogEntry>> =
        statusLogDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }
}

class FolderRepositoryImpl(
    private val folderDao: FolderDao,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : FolderRepository {

    override fun observeAll(): Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(folder: Folder) {
        folderDao.insert(folder.toEntity())
        syncthingWriter?.writeFolder(folder.toEntity())
    }

    override suspend fun update(folder: Folder) {
        folderDao.update(folder.toEntity())
        syncthingWriter?.writeFolder(folder.toEntity())
    }

    override suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "folders", id, now))
        syncthingWriter?.writeTombstone("folders", id, now)
        folderDao.delete(id)
    }
}

class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val syncDao: SyncDao? = null,
    private val syncthingWriter: SyncthingWriter? = null,
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeUnused(): Flow<List<Tag>> =
        tagDao.observeUnused().map { list -> list.map { it.toDomain() } }

    override fun observeUsageCount(tagId: String): Flow<Int> =
        tagDao.observeUsageCount(tagId)

    override suspend fun insert(tag: Tag) {
        tagDao.insert(tag.toEntity())
        syncthingWriter?.writeTag(tag.toEntity())
    }

    override suspend fun update(tag: Tag) {
        tagDao.update(tag.toEntity())
        syncthingWriter?.writeTag(tag.toEntity())
    }

    override suspend fun delete(id: String) {
        val now = LocalDateTime.now()
        syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "tags", id, now))
        syncthingWriter?.writeTombstone("tags", id, now)
        tagDao.delete(id)
    }

    override suspend fun deleteUnused() {
        val now = LocalDateTime.now()
        tagDao.getUnusedIds().forEach { id ->
            syncDao?.insertTombstone(DeletedEntityEntity("tomb_$id", "tags", id, now))
            syncthingWriter?.writeTombstone("tags", id, now)
        }
        tagDao.deleteUnused()
    }
}
