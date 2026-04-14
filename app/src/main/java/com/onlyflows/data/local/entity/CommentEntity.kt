package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Comment
import java.time.LocalDateTime

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val taskId: String? = null,
    val projectId: String? = null,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun CommentEntity.toDomain() = Comment(
    id        = id,
    taskId    = taskId,
    projectId = projectId,
    content   = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Comment.toEntity() = CommentEntity(
    id        = id,
    taskId    = taskId,
    projectId = projectId,
    content   = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
