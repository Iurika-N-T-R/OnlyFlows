package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.ProjectType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val colorHex: String = "#4A90D9",
    val iconEmoji: String = "",
    val isCompleted: Boolean = false,
    val folderId: String? = null,
    val parentProjectId: String? = null,
    val type: String = "PROJECT",
    val sortOrder: Int = 0,
    val dueDate: String? = null,
    val status: String = "NOT_STARTED",
    val tagIds: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString(),
)

fun ProjectEntity.toDomain(sections: List<SectionEntity> = emptyList()) = Project(
    id              = id,
    title           = title,
    note            = note,
    colorHex        = colorHex,
    iconEmoji       = iconEmoji,
    isCompleted     = isCompleted,
    folderId        = folderId,
    parentProjectId = parentProjectId,
    type            = runCatching { ProjectType.valueOf(type) }.getOrDefault(ProjectType.PROJECT),
    sections        = sections.map { it.toDomain() },
    sortOrder       = sortOrder,
    dueDate         = dueDate?.let { LocalDate.parse(it) },
    status          = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.NOT_STARTED),
    tagIds          = tagIds,
    isArchived      = isArchived,
    createdAt       = runCatching { LocalDateTime.parse(createdAt) }.getOrDefault(LocalDateTime.now()),
    updatedAt       = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun Project.toEntity() = ProjectEntity(
    id              = id,
    title           = title,
    note            = note,
    colorHex        = colorHex,
    iconEmoji       = iconEmoji,
    isCompleted     = isCompleted,
    folderId        = folderId,
    parentProjectId = parentProjectId,
    type            = type.name,
    sortOrder       = sortOrder,
    dueDate         = dueDate?.toString(),
    status          = status.name,
    tagIds          = tagIds,
    isArchived      = isArchived,
    createdAt       = createdAt.toString(),
    updatedAt       = updatedAt.toString(),
)
