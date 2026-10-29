package com.onlyflows.data.smartadd

import android.content.Context
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.notifications.ReminderCreator
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Note
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.smartadd.Batch
import com.onlyflows.domain.smartadd.TaskDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Saves a [Batch] from the file import or the natural-language / voice input.
 * Nothing is duplicated: a project, list, section, task or note whose title already exists is
 * completed (missing date, priority, description, checklist items, tags) instead of added again.
 */
class ContentWriter(context: Context) {
    private val app = context.applicationContext
    private val c = (app as OnlyFlowsApp).container

    data class Summary(val created: Int = 0, val updated: Int = 0, val skipped: Int = 0, val lines: List<String> = emptyList()) {
        val text get() = "$created created · $updated updated · $skipped skipped"
    }

    private var created = 0
    private var updated = 0
    private var skipped = 0
    private val lines = ArrayList<String>()
    private var tags = emptyList<Tag>()

    private fun same(a: String, b: String) = a.trim().equals(b.trim(), ignoreCase = true)
    private fun id(prefix: String) = "${prefix}_${UUID.randomUUID()}"

    suspend fun write(batch: Batch): Summary = withContext(Dispatchers.IO) {
        created = 0; updated = 0; skipped = 0; lines.clear()
        tags = c.tagRepository.observeAll().first()
        val projects = c.projectRepository.observeAllItems().first().toMutableList()

        batch.containers.forEach { draft ->
            var project = projects.firstOrNull { same(it.title, draft.title) }
            if (project == null) {
                project = Project(
                    id = id("project"), title = draft.title.trim(), note = draft.note, type = draft.type,
                    dueDate = draft.dueDate, sortOrder = projects.size,
                )
                c.projectRepository.insert(project)
                projects += project
                created++; lines += "Created ${word(project)} \"${project.title}\""
            } else if ((project.note.isBlank() && draft.note.isNotBlank()) || (project.dueDate == null && draft.dueDate != null)) {
                project = project.copy(
                    note = project.note.ifBlank { draft.note },
                    dueDate = project.dueDate ?: draft.dueDate,
                    updatedAt = LocalDateTime.now(),
                )
                c.projectRepository.update(project)
                updated++; lines += "Updated ${word(project)} \"${project.title}\""
            }

            val sections = project.sections.toMutableList()
            val existingTasks = c.taskRepository.observeByProject(project.id).first().toMutableList()
            draft.tasks.forEach { t ->
                val sectionId = t.section?.takeIf { it.isNotBlank() }?.let { name ->
                    sections.firstOrNull { same(it.title, name) }?.id ?: Section(id("section"), project.id, name.trim(), sections.size).also {
                        c.projectRepository.insertSection(it); sections += it
                    }.id
                }
                writeTask(t, project.id, sectionId, existingTasks, project.title)
            }

            if (draft.notes.isNotEmpty()) {
                val existingNotes = c.noteRepository.observeByProject(project.id).first().toMutableList()
                draft.notes.forEach { n ->
                    if (existingNotes.any { same(it.title, n.title) && (n.content.isBlank() || same(it.content, n.content)) }) { skipped++; return@forEach }
                    val now = LocalDateTime.now()
                    val note = Note(id = id("note"), projectId = project.id, title = n.title.trim(), content = n.content, createdAt = now, updatedAt = now)
                    c.noteRepository.insert(note); existingNotes += note
                    created++; lines += "Added note \"${note.title.take(40)}\" to ${project.title}"
                }
            }
        }

        if (batch.tasks.isNotEmpty()) {
            val loose = c.taskRepository.observeAll().first().filter { it.projectId == null }.toMutableList()
            batch.tasks.forEach { writeTask(it, null, null, loose, null) }
        }
        Summary(created, updated, skipped, lines.toList())
    }

    private fun word(p: Project) = when (p.type) {
        com.onlyflows.domain.model.ProjectType.LIST -> "list"
        com.onlyflows.domain.model.ProjectType.NOTE -> "notes list"
        else -> "project"
    }

    private suspend fun tagIds(names: List<String>): List<String> = names.filter { it.isNotBlank() }.map { name ->
        tags.firstOrNull { same(it.name, name) }?.id ?: Tag(id("tag"), name.trim()).also { c.tagRepository.insert(it); tags = tags + it }.id
    }

    /** Same rule as the add sheet: a date today is Today, a later date Anytime, none stays in Inbox. */
    private fun groupFor(date: LocalDate?) = when {
        date == null -> null
        date == LocalDate.now() -> TaskGroup.TODAY
        else -> TaskGroup.ANYTIME
    }

    private suspend fun writeTask(t: TaskDraft, projectId: String?, sectionId: String?, existing: MutableList<Task>, where: String?) {
        if (t.title.isBlank()) return
        val now = LocalDateTime.now()
        val old = existing.firstOrNull { same(it.title, t.title) && (sectionId == null || it.sectionId == sectionId || it.sectionId == null) }
        val newTags = tagIds(t.tagNames)
        if (old == null) {
            val taskId = id("task")
            val task = Task(
                id = taskId, title = t.title.trim(), note = t.note, isCompleted = t.completed,
                projectId = projectId, sectionId = sectionId, dueDate = t.dueDate, dueTime = t.dueTime,
                scheduledGroup = groupFor(t.dueDate), tagIds = newTags, priority = t.priority, recurrence = t.recurrence,
                hasNote = t.note.isNotBlank(), hasChecklist = t.checklist.isNotEmpty(),
                checklistItems = t.checklist.mapIndexed { i, s -> ChecklistItem(id("cli"), taskId, s.trim(), sortOrder = i) },
                createdAt = now, updatedAt = now, completedAt = if (t.completed) now else null,
            )
            c.taskRepository.insert(task)
            existing += task
            created++; lines += "Created task \"${task.title}\"" + (where?.let { " in $it" } ?: "")
            remind(t, taskId)
            return
        }
        // Merge into the task that already exists: fill what it lacks, add what it does not have.
        val missingItems = t.checklist.filter { s -> old.checklistItems.none { same(it.title, s) } }
        val note = when {
            t.note.isBlank() || old.note.contains(t.note.trim(), ignoreCase = true) -> old.note
            old.note.isBlank() -> t.note
            else -> old.note + "\n\n" + t.note
        }
        val merged = old.copy(
            note = note,
            dueDate = old.dueDate ?: t.dueDate,
            dueTime = old.dueTime ?: t.dueTime,
            scheduledGroup = old.scheduledGroup ?: if (old.dueDate == null) groupFor(t.dueDate) else null,
            priority = if (old.priority == TaskPriority.NONE) t.priority else old.priority,
            recurrence = old.recurrence ?: t.recurrence,
            tagIds = (old.tagIds + newTags).distinct(),
            hasNote = note.isNotBlank(),
            hasChecklist = old.hasChecklist || missingItems.isNotEmpty(),
        )
        val fieldsChanged = merged.copy(checklistItems = old.checklistItems) != old
        if (fieldsChanged) c.taskRepository.update(merged.copy(updatedAt = now))
        missingItems.forEachIndexed { i, s ->
            c.taskRepository.addChecklistItem(ChecklistItem(id("cli"), old.id, s.trim(), sortOrder = old.checklistItems.size + i))
        }
        if (fieldsChanged || missingItems.isNotEmpty()) {
            updated++; lines += "Updated task \"${old.title}\"" + if (missingItems.isNotEmpty()) " (+${missingItems.size} items)" else ""
        } else skipped++
        if (!old.hasReminders) remind(t, old.id)
    }

    private suspend fun remind(t: TaskDraft, taskId: String) {
        val at = t.reminderAt ?: return
        if (at.isBefore(LocalDateTime.now())) return
        ReminderCreator.attach(app, taskId, at)
    }
}

/** Reads a picked file (JSON, CSV, TXT, Markdown…) and saves what it holds, merging with what exists. */
suspend fun importFromUri(context: Context, uri: android.net.Uri): ContentWriter.Summary = withContext(Dispatchers.IO) {
    try {
        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }.orEmpty()
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            require(bytes.size <= 5 * 1024 * 1024) { "the file is larger than 5 MB" }
            // Excel in French saves CSV as Windows-1252: if the bytes are not valid UTF-8, read them as that
            runCatching { Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString() }
                .getOrElse { String(bytes, charset("windows-1252")) }
        } ?: error("the file could not be opened")
        val batch = FileImporter.parse(text, name)
        if (batch.isEmpty) ContentWriter.Summary(lines = listOf("Nothing to import was found in $name"))
        else ContentWriter(context).write(batch)
    } catch (e: Exception) {
        ContentWriter.Summary(lines = listOf("Could not import: ${e.message ?: e.javaClass.simpleName}"))
    }
}

/** What the file picker offers for an import. */
val IMPORT_MIME_TYPES = arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/tab-separated-values", "text/plain", "text/markdown", "text/*", "*/*")

/** Imports text pasted from an AI assistant or anywhere else: same formats as a file, ```fences``` removed. */
suspend fun importFromText(context: Context, pasted: String): ContentWriter.Summary = withContext(Dispatchers.IO) {
    // Assistants wrap their answer in ```json … ```: keep what is inside the first fenced block.
    val text = Regex("```[a-zA-Z]*\\s*\\n([\\s\\S]*?)```").find(pasted)?.groupValues?.get(1) ?: pasted
    try {
        val batch = FileImporter.parse(text.trim())
        if (batch.isEmpty) ContentWriter.Summary(lines = listOf("Nothing to import was found in the pasted text"))
        else ContentWriter(context).write(batch)
    } catch (e: Exception) {
        ContentWriter.Summary(lines = listOf("Could not import: ${e.message ?: e.javaClass.simpleName}"))
    }
}
