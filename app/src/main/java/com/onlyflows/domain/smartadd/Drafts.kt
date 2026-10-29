package com.onlyflows.domain.smartadd

import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.RecurrenceRule
import com.onlyflows.domain.model.TaskPriority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * What the file import and the natural-language / voice input want to create, before it is
 * matched against what already exists. [ContentWriter] turns it into rows, merging by title.
 */
data class TaskDraft(
    val title: String,
    val note: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val recurrence: RecurrenceRule? = null,
    val completed: Boolean = false,
    val checklist: List<String> = emptyList(),
    val reminderAt: LocalDateTime? = null,
    val tagNames: List<String> = emptyList(),
    /** Section of the project the task goes in, created when missing. */
    val section: String? = null,
)

data class NoteDraft(val title: String, val content: String = "")

/** A project, a checklist (LIST: each item is a checkable task) or a notes list (NOTE). */
data class ContainerDraft(
    val title: String,
    val type: ProjectType,
    val note: String = "",
    val dueDate: LocalDate? = null,
    val tasks: List<TaskDraft> = emptyList(),
    val notes: List<NoteDraft> = emptyList(),
)

data class Batch(
    val containers: List<ContainerDraft> = emptyList(),
    /** Tasks outside any project (Inbox / Today / Upcoming). */
    val tasks: List<TaskDraft> = emptyList(),
) {
    val isEmpty get() = containers.isEmpty() && tasks.isEmpty()
    operator fun plus(o: Batch) = Batch(containers + o.containers, tasks + o.tasks)
}

/** What one sentence means: ready to save, a choice the user has to make, or nothing usable. */
sealed interface Interpretation {
    data class Ready(val batch: Batch) : Interpretation
    data class Ask(val question: String, val options: List<Pair<String, Batch>>) : Interpretation
    data class Invalid(val message: String) : Interpretation
}
