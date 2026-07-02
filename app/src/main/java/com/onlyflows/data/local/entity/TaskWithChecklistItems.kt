package com.onlyflows.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.onlyflows.domain.model.Task

data class TaskWithChecklistItems(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
    )
    val checklistItems: List<ChecklistItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
        projection   = ["id"],
        entity       = CommentEntity::class,
    )
    val commentIds: List<String> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
        projection   = ["id"],
        entity       = TimeEntryEntity::class,
    )
    val timeEntryIds: List<String> = emptyList(),
)

fun TaskWithChecklistItems.toDomain(): Task =
    task.toDomain(checklistItems).copy(
        hasComments    = commentIds.isNotEmpty(),
        hasTimeEntries = timeEntryIds.isNotEmpty(),
    )
