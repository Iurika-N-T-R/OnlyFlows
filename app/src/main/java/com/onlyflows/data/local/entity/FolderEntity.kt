package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Folder
import java.time.LocalDateTime

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sortOrder: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun FolderEntity.toDomain() = Folder(id = id, title = title, sortOrder = sortOrder, updatedAt = updatedAt)
fun Folder.toEntity()       = FolderEntity(id = id, title = title, sortOrder = sortOrder, updatedAt = updatedAt)
