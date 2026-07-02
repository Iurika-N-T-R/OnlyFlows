package com.onlyflows.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Note
import java.time.LocalDateTime

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun NoteEntity.toDomain() = Note(
    id        = id,
    projectId = projectId,
    title      = title,
    content    = content,
    isPinned   = isPinned,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.toEntity() = NoteEntity(
    id        = id,
    projectId = projectId,
    title      = title,
    content    = content,
    isPinned   = isPinned,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
