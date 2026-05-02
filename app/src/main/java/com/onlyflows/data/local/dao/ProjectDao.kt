package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.FolderEntity
import com.onlyflows.data.local.entity.ProjectEntity
import com.onlyflows.data.local.entity.SectionEntity
import com.onlyflows.data.local.entity.StatusLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects WHERE isCompleted = 0 AND isArchived = 0 AND parentProjectId IS NULL ORDER BY folderId ASC, sortOrder ASC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isCompleted = 0 AND isArchived = 0 AND parentProjectId IS NULL AND type = 'PROJECT' ORDER BY folderId ASC, sortOrder ASC")
    fun observeActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE parentProjectId = :parentId AND isArchived = 0 ORDER BY sortOrder ASC")
    fun observeChildren(parentId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("UPDATE projects SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("UPDATE projects SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE projects SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, now: String)
}

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun observeByProject(projectId: String): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<SectionEntity>)

    @Update
    suspend fun update(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface StatusLogDao {

    @Query("SELECT * FROM project_status_log WHERE projectId = :projectId ORDER BY changedAt ASC")
    fun observeByProject(projectId: String): Flow<List<StatusLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StatusLogEntity)
}

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}
