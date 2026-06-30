package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.ChecklistSection
import java.time.LocalDateTime

@Entity(
    tableName = "checklist_sections",
    foreignKeys = [ForeignKey(
        entity        = TaskEntity::class,
        parentColumns = ["id"],
        childColumns  = ["taskId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("taskId")],
)
data class ChecklistSectionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val sortOrder: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun ChecklistSectionEntity.toDomain() = ChecklistSection(
    id        = id,
    taskId    = taskId,
    title     = title,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
)

fun ChecklistSection.toEntity() = ChecklistSectionEntity(
    id        = id,
    taskId    = taskId,
    title     = title,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
)
