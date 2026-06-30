package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.ChecklistItemReminder
import java.time.LocalDateTime

@Entity(
    tableName = "checklist_item_reminders",
    foreignKeys = [ForeignKey(
        entity        = ChecklistItemEntity::class,
        parentColumns = ["id"],
        childColumns  = ["checklistItemId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("checklistItemId")],
)
data class ChecklistItemReminderEntity(
    @PrimaryKey val id: String,
    val checklistItemId: String,
    val scheduledAt: LocalDateTime,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun ChecklistItemReminderEntity.toDomain() = ChecklistItemReminder(
    id              = id,
    checklistItemId = checklistItemId,
    scheduledAt     = scheduledAt,
    updatedAt       = updatedAt,
)

fun ChecklistItemReminder.toEntity() = ChecklistItemReminderEntity(
    id              = id,
    checklistItemId = checklistItemId,
    scheduledAt     = scheduledAt,
    updatedAt       = updatedAt,
)
