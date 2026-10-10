package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Tag
import java.time.LocalDateTime

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String = "#4A90D9",
    val groupName: String? = null,
    val sortOrder: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun TagEntity.toDomain() = Tag(
    id        = id,
    name      = name,
    colorHex  = colorHex,
    groupName = groupName,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
)

fun Tag.toEntity() = TagEntity(
    id        = id,
    name      = name,
    colorHex  = colorHex,
    groupName = groupName,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
)
