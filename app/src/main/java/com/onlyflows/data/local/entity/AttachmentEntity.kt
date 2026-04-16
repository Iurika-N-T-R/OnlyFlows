package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Attachment
import java.time.LocalDateTime

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val fileName: String,
    val mimeType: String,
    val uri: String,
    val fileSize: Long = 0L,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val syncedFrom: String? = null,
)

fun AttachmentEntity.toDomain() = Attachment(
    id         = id,
    taskId     = taskId,
    fileName   = fileName,
    mimeType   = mimeType,
    uri        = uri,
    fileSize   = fileSize,
    createdAt  = createdAt,
    updatedAt  = updatedAt,
    syncedFrom = syncedFrom,
)

fun Attachment.toEntity() = AttachmentEntity(
    id         = id,
    taskId     = taskId,
    fileName   = fileName,
    mimeType   = mimeType,
    uri        = uri,
    fileSize   = fileSize,
    createdAt  = createdAt,
    updatedAt  = updatedAt,
    syncedFrom = syncedFrom,
)
