package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "deleted_entities",
    indices = [Index(value = ["entityType", "entityId"])],
)
data class DeletedEntityEntity(
    @PrimaryKey val id: String,         // "tomb_<entityId>"
    val entityType: String,             // "tasks" | "projects" | "checklist_items" | ...
    val entityId: String,
    val deletedAt: LocalDateTime,
)
