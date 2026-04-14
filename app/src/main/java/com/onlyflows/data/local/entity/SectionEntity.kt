package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.Section

@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity        = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns  = ["projectId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class SectionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val sortOrder: Int = 0,
)

fun SectionEntity.toDomain() = Section(
    id        = id,
    projectId = projectId,
    title     = title,
    sortOrder = sortOrder,
)

fun Section.toEntity() = SectionEntity(
    id        = id,
    projectId = projectId,
    title     = title,
    sortOrder = sortOrder,
)
