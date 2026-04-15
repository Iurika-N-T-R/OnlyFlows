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
)

fun TaskWithChecklistItems.toDomain(): Task =
    task.toDomain(checklistItems)
