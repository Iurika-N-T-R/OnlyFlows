package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sortOrder: Int = 0,
)

fun FolderEntity.toDomain() = Folder(id = id, title = title, sortOrder = sortOrder)
fun Folder.toEntity()       = FolderEntity(id = id, title = title, sortOrder = sortOrder)
