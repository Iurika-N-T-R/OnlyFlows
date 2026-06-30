package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.ChecklistItem
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(
        entity        = TaskEntity::class,
        parentColumns = ["id"],
        childColumns  = ["taskId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("taskId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val dueDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val sectionId: String? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun ChecklistItemEntity.toDomain() = ChecklistItem(
    id          = id,
    taskId      = taskId,
    title       = title,
    isCompleted = isCompleted,
    sortOrder   = sortOrder,
    dueDate     = dueDate,
    startDate   = startDate,
    sectionId   = sectionId,
    updatedAt   = updatedAt,
)

fun ChecklistItem.toEntity() = ChecklistItemEntity(
    id          = id,
    taskId      = taskId,
    title       = title,
    isCompleted = isCompleted,
    sortOrder   = sortOrder,
    dueDate     = dueDate,
    startDate   = startDate,
    sectionId   = sectionId,
    updatedAt   = updatedAt,
)
