package com.onlyflows.ui.screens.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Note
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.StatusLogEntry
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.model.Comment
import com.onlyflows.domain.repository.CommentRepository
import com.onlyflows.domain.repository.NoteRepository
import com.onlyflows.domain.repository.ProjectRepository
import com.onlyflows.domain.repository.TagRepository
import com.onlyflows.domain.repository.TaskRepository
import com.onlyflows.domain.repository.TimeEntryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ProjectDetailUiState(
    val project: Project?                        = null,
    val tasksBySection: Map<String?, List<Task>> = emptyMap(),
    val tags: List<Tag>                          = emptyList(),
    val allProjects: List<Project>               = emptyList(),
    val childLists: List<Project>                = emptyList(),
    val selectedTaskIds: Set<String>             = emptySet(),
    val movingToCompletedIds: Set<String>        = emptySet(),
    val showAddTask: Boolean                     = false,
    val showMoveSheet: Boolean                   = false,
    val addTaskSectionId: String?                = null,
    val isLoading: Boolean                       = true,
    val statusLog: List<StatusLogEntry>          = emptyList(),
    val activeTagFilter: Set<String>             = emptySet(),
    val activePriorityFilter: TaskPriority?      = null,
    val notes: List<Note>                        = emptyList(),
    val editingNote: Note?                       = null,
    val isAddingNote: Boolean                    = false,
    val noteViewMode: Boolean                    = false,
    val archivedTasks: List<Task>                = emptyList(),
    val totalCompletedSeconds: Long              = 0L,   // sum of finished entries (from DB)
    val activeProjectEntry: com.onlyflows.domain.model.TimeEntry? = null, // running entry if any
    val totalTrackedSeconds: Long                = 0L,   // display value = completed + elapsed (ticked)
    val comments: List<Comment>                  = emptyList(),
    val showBulkMore: Boolean                    = false,
    val collapsedSectionIds: Set<String>         = emptySet(),
    val hideCompleted: Boolean                   = false,
)

sealed interface ProjectDetailEvent {
    data class ToggleComplete(val taskId: String, val current: Boolean) : ProjectDetailEvent
    data class ToggleSelect(val taskId: String)                         : ProjectDetailEvent
    data object ClearSelection                                          : ProjectDetailEvent
    data class ShowAddTask(val sectionId: String?)                      : ProjectDetailEvent
    data object HideAddTask                                             : ProjectDetailEvent
    data class AddTask(val id: String, val title: String, val note: String = "", val sectionId: String?, val priority: TaskPriority = TaskPriority.NONE, val checklistItems: List<String> = emptyList()) : ProjectDetailEvent
    data class DeleteSelected(val ids: Set<String>)                     : ProjectDetailEvent
    data class ReorderTasks(val idsInOrder: List<String>)                        : ProjectDetailEvent
    data class ToggleChecklistItem(val itemId: String, val completed: Boolean)   : ProjectDetailEvent
    data class UpdateStatus(val status: ProjectStatus)                           : ProjectDetailEvent
    data class UpdateProject(
        val title: String,
        val note: String,
        val colorHex: String,
        val dueDate: java.time.LocalDate?,
    ) : ProjectDetailEvent
    data class AddSection(val title: String)                                    : ProjectDetailEvent
    data class RenameSection(val sectionId: String, val title: String)         : ProjectDetailEvent
    data class DeleteSection(val sectionId: String)                             : ProjectDetailEvent
    data class ReorderSections(val orderedIds: List<String>)                    : ProjectDetailEvent
    data class ToggleSectionCollapse(val sectionId: String)                     : ProjectDetailEvent
    data object ToggleHideCompleted                                              : ProjectDetailEvent
    data class ToggleTagFilter(val tagId: String)                  : ProjectDetailEvent
    data class SetPriorityFilter(val priority: TaskPriority?)  : ProjectDetailEvent
    data object Archive                                        : ProjectDetailEvent
    data object ShowMoveSheet                                                  : ProjectDetailEvent
    data object HideMoveSheet                                                  : ProjectDetailEvent
    data class MoveSelected(val projectId: String?, val sectionId: String?)    : ProjectDetailEvent
    data class MoveTaskToSection(val taskId: String, val sectionId: String?)   : ProjectDetailEvent
    data class MoveSelectedToSection(val sectionId: String?)                   : ProjectDetailEvent
    data class ArchiveSelected(val ids: Set<String>)                           : ProjectDetailEvent
    data class UnarchiveTask(val taskId: String)                               : ProjectDetailEvent
    data class CreateChildList(val title: String)                             : ProjectDetailEvent
    data class UpdateNote(val note: String)                                   : ProjectDetailEvent
    // ── Note-list CRUD (NOTE-type projects) ──
    data object ShowNewNoteEditor                                             : ProjectDetailEvent
    data class  OpenNoteViewer(val note: Note)                               : ProjectDetailEvent
    data class  OpenNoteEditor(val note: Note)                               : ProjectDetailEvent
    data object HideNoteEditor                                                : ProjectDetailEvent
    data class  SaveNote(val id: String?, val title: String, val content: String) : ProjectDetailEvent
    data class  DeleteNoteItem(val noteId: String)                           : ProjectDetailEvent
    data class AddComment(val content: String)                               : ProjectDetailEvent
    data class EditComment(val id: String, val content: String)             : ProjectDetailEvent
    data class DeleteComment(val id: String)                                 : ProjectDetailEvent
    data object ShowBulkMore                                                : ProjectDetailEvent
    data object HideBulkMore                                                : ProjectDetailEvent
    data class BulkStar(val starred: Boolean)                               : ProjectDetailEvent
    data class BulkSetPriority(val priority: TaskPriority)                  : ProjectDetailEvent
    data class BulkAddTag(val tagId: String)                                : ProjectDetailEvent
    data class ToggleProjectTag(val tagId: String)                          : ProjectDetailEvent
    data class BulkSchedule(val date: java.time.LocalDate?)                 : ProjectDetailEvent
    data object StartTimerForSelected                                       : ProjectDetailEvent
    data object AbandonSelected                                             : ProjectDetailEvent
}

class ProjectDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val container     = (app as OnlyFlowsApp).container
    private val taskRepo: TaskRepository             = container.taskRepository
    private val projectRepo: ProjectRepository       = container.projectRepository
    private val tagRepo: TagRepository               = container.tagRepository
    private val noteRepo: NoteRepository             = container.noteRepository
    private val timeRepo: TimeEntryRepository        = container.timeEntryRepository
    private val commentRepo: CommentRepository       = container.commentRepository
    private val appPrefs                             = container.appPreferences
    private val coordinator                          = container.syncthingCoordinator

    val isImporting = coordinator.isImporting

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        // Ticker: only runs while a project timer is active; stops automatically when idle
        viewModelScope.launch {
            _uiState
                .map { it.activeProjectEntry }
                .distinctUntilChanged()
                .collectLatest { activeEntry ->
                    if (activeEntry == null) return@collectLatest
                    while (true) {
                        delay(1000)
                        val elapsed = ChronoUnit.SECONDS.between(activeEntry.startedAt, LocalDateTime.now())
                        _uiState.update { it.copy(totalTrackedSeconds = it.totalCompletedSeconds + elapsed) }
                    }
                }
        }
    }

    fun loadProject(projectId: String) {
        // Load persisted UI prefs for this project
        viewModelScope.launch {
            appPrefs.collapsedSections(projectId).collect { ids ->
                _uiState.update { it.copy(collapsedSectionIds = ids) }
            }
        }
        viewModelScope.launch {
            appPrefs.hideCompleted(projectId).collect { value ->
                _uiState.update { it.copy(hideCompleted = value) }
            }
        }
        // Observe time entries reactively — updates totalCompletedSeconds + activeProjectEntry
        viewModelScope.launch {
            timeRepo.observeByProject(projectId).collect { entries ->
                val completed = entries
                    .filter { it.endedAt != null }
                    .sumOf { ChronoUnit.SECONDS.between(it.startedAt, it.endedAt!!) }
                val active = entries.firstOrNull { it.endedAt == null }
                val elapsed = active?.startedAt?.let {
                    ChronoUnit.SECONDS.between(it, LocalDateTime.now())
                } ?: 0L
                _uiState.update { it.copy(
                    totalCompletedSeconds = completed,
                    activeProjectEntry    = active,
                    totalTrackedSeconds   = completed + elapsed,
                ) }
            }
        }
        // Observe comments
        viewModelScope.launch {
            commentRepo.observeByProject(projectId).collect { comments ->
                _uiState.update { it.copy(comments = comments) }
            }
        }
        viewModelScope.launch {
            val projectAndChildrenFlow = combine(
                projectRepo.observeById(projectId),
                projectRepo.observeChildren(projectId),
            ) { project, children -> Pair(project, children) }

            val coreFlow = combine(
                projectAndChildrenFlow,
                taskRepo.observeByProject(projectId),
                tagRepo.observeAll(),
                projectRepo.observeStatusLog(projectId),
                projectRepo.observeAllProjectsAndLists(),
            ) { projAndChildren, tasks, tags, statusLog, allProjects ->
                arrayOf<Any?>(projAndChildren, tasks, tags, statusLog, allProjects)
            }

            val noteAndArchiveFlow = combine(
                noteRepo.observeByProject(projectId),
                taskRepo.observeArchivedByProject(projectId),
            ) { notes, archived -> Pair(notes, archived) }

            @Suppress("UNCHECKED_CAST")
            combine(coreFlow, noteAndArchiveFlow) { packed, noteAndArchive ->
                val projAndChildren = packed[0] as Pair<*, *>
                val project     = projAndChildren.first as Project?
                val childLists  = projAndChildren.second as List<Project>
                val tasks       = packed[1] as List<Task>
                val tags        = packed[2] as List<Tag>
                val statusLog   = packed[3] as List<StatusLogEntry>
                val allProjects = packed[4] as List<Project>
                val notes       = noteAndArchive.first
                val archived    = noteAndArchive.second

                ProjectDetailUiState(
                    project        = project,
                    tasksBySection = tasks.groupBy { it.sectionId },
                    tags           = tags,
                    statusLog      = statusLog,
                    allProjects    = allProjects,
                    childLists     = childLists,
                    notes          = notes,
                    archivedTasks  = archived,
                    isLoading      = false,
                )
            }.collect { newState ->
                _uiState.update { state ->
                    newState.copy(
                        selectedTaskIds       = state.selectedTaskIds,
                        movingToCompletedIds  = state.movingToCompletedIds,
                        showAddTask           = state.showAddTask,
                        showMoveSheet         = state.showMoveSheet,
                        addTaskSectionId      = state.addTaskSectionId,
                        activeTagFilter       = state.activeTagFilter,
                        activePriorityFilter  = state.activePriorityFilter,
                        editingNote           = state.editingNote,
                        isAddingNote          = state.isAddingNote,
                        // Preserve timer state managed by the time-entries flow + ticker
                        totalCompletedSeconds = state.totalCompletedSeconds,
                        activeProjectEntry    = state.activeProjectEntry,
                        totalTrackedSeconds   = state.totalTrackedSeconds,
                        // Preserve state managed by independent coroutines
                        comments              = state.comments,
                        collapsedSectionIds   = state.collapsedSectionIds,
                        hideCompleted         = state.hideCompleted,
                    )
                }
            }
        }
    }

    fun onEvent(event: ProjectDetailEvent) {
        when (event) {
            is ProjectDetailEvent.ToggleComplete -> viewModelScope.launch {
                val newCompleted = !event.current
                val task = _uiState.value.tasksBySection.values.flatten().find { it.id == event.taskId }
                if (newCompleted && task?.recurrence != null) {
                    taskRepo.completeRecurring(event.taskId)
                } else {
                    taskRepo.setCompleted(event.taskId, newCompleted)
                }

                val project = _uiState.value.project ?: return@launch
                val allTasks = _uiState.value.tasksBySection.values.flatten()
                val updatedTasks = allTasks.map { t ->
                    if (t.id == event.taskId) t.copy(isCompleted = newCompleted) else t
                }

                if (newCompleted) {
                    // Task was just marked complete — check if all tasks are now done
                    if (updatedTasks.isNotEmpty() && updatedTasks.all { it.isCompleted }
                        && project.status != ProjectStatus.DONE
                    ) {
                        projectRepo.updateStatus(project.id, ProjectStatus.DONE)
                    }
                } else {
                    // Task was unchecked — if project was DONE, revert to IN_PROGRESS
                    if (project.status == ProjectStatus.DONE) {
                        projectRepo.updateStatus(project.id, ProjectStatus.IN_PROGRESS)
                    }
                }
            }
            is ProjectDetailEvent.ToggleSelect   -> toggleSelect(event.taskId)
            is ProjectDetailEvent.ClearSelection -> _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            is ProjectDetailEvent.ShowAddTask    -> _uiState.update { it.copy(showAddTask = true, addTaskSectionId = event.sectionId) }
            is ProjectDetailEvent.HideAddTask    -> _uiState.update { it.copy(showAddTask = false) }
            is ProjectDetailEvent.AddTask        -> addTask(event.id, event.title, event.note, event.sectionId, event.priority, event.checklistItems)
            is ProjectDetailEvent.DeleteSelected -> deleteSelected(event.ids)
            is ProjectDetailEvent.ReorderTasks          -> viewModelScope.launch {
                taskRepo.reorderTasks(event.idsInOrder)
            }
            is ProjectDetailEvent.ToggleChecklistItem -> viewModelScope.launch {
                taskRepo.toggleChecklistItem(event.itemId, event.completed)
                // Auto-complete: find parent task, check if all items now done
                val task = _uiState.value.tasksBySection.values.flatten()
                    .find { t -> t.checklistItems.any { it.id == event.itemId } }
                if (task != null) {
                    val updatedItems = task.checklistItems.map {
                        if (it.id == event.itemId) it.copy(isCompleted = event.completed) else it
                    }
                    if (updatedItems.isNotEmpty() && updatedItems.all { it.isCompleted }) {
                        if (task.recurrence != null) taskRepo.completeRecurring(task.id)
                        else taskRepo.setCompleted(task.id, true)
                        _uiState.update { it.copy(movingToCompletedIds = it.movingToCompletedIds + task.id) }
                        delay(800)
                        _uiState.update { it.copy(movingToCompletedIds = it.movingToCompletedIds - task.id) }
                    }
                }
            }
            is ProjectDetailEvent.UpdateStatus -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                projectRepo.updateStatus(project.id, event.status)
            }
            is ProjectDetailEvent.UpdateProject -> viewModelScope.launch {
                val p = _uiState.value.project ?: return@launch
                projectRepo.update(p.copy(
                    title    = event.title,
                    note     = event.note,
                    colorHex = event.colorHex,
                    dueDate  = event.dueDate,
                ))
            }
            is ProjectDetailEvent.AddSection -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                projectRepo.insertSection(
                    Section(
                        id        = "section_${System.currentTimeMillis()}",
                        projectId = project.id,
                        title     = event.title,
                        sortOrder = project.sections.size,
                    )
                )
            }
            is ProjectDetailEvent.RenameSection -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                val section = project.sections.find { it.id == event.sectionId } ?: return@launch
                projectRepo.updateSection(section.copy(title = event.title))
            }
            is ProjectDetailEvent.DeleteSection -> viewModelScope.launch {
                projectRepo.deleteSection(event.sectionId)
            }
            is ProjectDetailEvent.ReorderSections -> viewModelScope.launch {
                val sections = _uiState.value.project?.sections ?: return@launch
                val sectionMap = sections.associateBy { it.id }
                event.orderedIds.forEachIndexed { index, id ->
                    val section = sectionMap[id] ?: return@forEachIndexed
                    if (section.sortOrder != index) {
                        projectRepo.updateSection(section.copy(sortOrder = index))
                    }
                }
            }
            is ProjectDetailEvent.ToggleSectionCollapse -> viewModelScope.launch {
                val projectId = _uiState.value.project?.id ?: return@launch
                val id = event.sectionId
                val newIds = _uiState.value.collapsedSectionIds.let { if (id in it) it - id else it + id }
                _uiState.update { it.copy(collapsedSectionIds = newIds) }
                appPrefs.setCollapsedSections(projectId, newIds)
            }
            ProjectDetailEvent.ToggleHideCompleted -> viewModelScope.launch {
                val projectId = _uiState.value.project?.id ?: return@launch
                val newValue = !_uiState.value.hideCompleted
                _uiState.update { it.copy(hideCompleted = newValue) }
                appPrefs.setHideCompleted(projectId, newValue)
            }
            is ProjectDetailEvent.ToggleTagFilter -> _uiState.update { state ->
                val current = state.activeTagFilter
                val tagId   = event.tagId
                state.copy(activeTagFilter = if (tagId in current) current - tagId else current + tagId)
            }
            is ProjectDetailEvent.SetPriorityFilter -> _uiState.update {
                it.copy(activePriorityFilter = event.priority)
            }
            is ProjectDetailEvent.Archive -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                projectRepo.archive(project.id)
            }
            ProjectDetailEvent.ShowMoveSheet -> _uiState.update { it.copy(showMoveSheet = true) }
            ProjectDetailEvent.HideMoveSheet -> _uiState.update { it.copy(showMoveSheet = false) }
            is ProjectDetailEvent.MoveSelected -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskId ->
                    taskRepo.moveTask(taskId, event.projectId, event.sectionId)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoveSheet = false) }
            }
            is ProjectDetailEvent.MoveTaskToSection -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                taskRepo.moveTask(event.taskId, project.id, event.sectionId)
            }
            is ProjectDetailEvent.MoveSelectedToSection -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskId -> taskRepo.moveTask(taskId, project.id, event.sectionId) }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showMoveSheet = false) }
            }
            is ProjectDetailEvent.ArchiveSelected -> viewModelScope.launch {
                event.ids.forEach { taskRepo.setArchived(it, true) }
                _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            }
            is ProjectDetailEvent.UnarchiveTask -> viewModelScope.launch {
                taskRepo.setArchived(event.taskId, false)
            }
            is ProjectDetailEvent.UpdateNote -> viewModelScope.launch {
                val p = _uiState.value.project ?: return@launch
                projectRepo.update(p.copy(note = event.note))
            }
            is ProjectDetailEvent.CreateChildList -> viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                val child = Project(
                    id              = "project_${System.currentTimeMillis()}",
                    title           = event.title,
                    colorHex        = project.colorHex,
                    type            = ProjectType.LIST,
                    parentProjectId = project.id,
                )
                projectRepo.insert(child)
            }
            // ── Note-list events ──────────────────────────────────────────────
            is ProjectDetailEvent.ShowNewNoteEditor -> _uiState.update { it.copy(isAddingNote = true, editingNote = null, noteViewMode = false) }
            is ProjectDetailEvent.OpenNoteViewer    -> _uiState.update { it.copy(editingNote = event.note, isAddingNote = false, noteViewMode = true) }
            is ProjectDetailEvent.OpenNoteEditor    -> _uiState.update { it.copy(editingNote = event.note, isAddingNote = false, noteViewMode = false) }
            is ProjectDetailEvent.HideNoteEditor    -> _uiState.update { it.copy(editingNote = null, isAddingNote = false, noteViewMode = false) }
            is ProjectDetailEvent.SaveNote -> viewModelScope.launch {
                val projectId = _uiState.value.project?.id ?: return@launch
                val now = LocalDateTime.now()
                if (event.id == null) {
                    noteRepo.insert(Note(
                        id        = "note_${System.currentTimeMillis()}",
                        projectId = projectId,
                        title     = event.title,
                        content   = event.content,
                        createdAt = now,
                        updatedAt = now,
                    ))
                } else {
                    val existing = _uiState.value.notes.find { it.id == event.id } ?: return@launch
                    noteRepo.update(existing.copy(
                        title     = event.title,
                        content   = event.content,
                        updatedAt = now,
                    ))
                }
                _uiState.update { it.copy(editingNote = null, isAddingNote = false) }
            }
            is ProjectDetailEvent.DeleteNoteItem -> viewModelScope.launch {
                noteRepo.delete(event.noteId)
            }
            is ProjectDetailEvent.AddComment -> viewModelScope.launch {
                val projectId = _uiState.value.project?.id ?: return@launch
                if (event.content.isBlank()) return@launch
                commentRepo.add(Comment(
                    id        = "cmt_${System.currentTimeMillis()}",
                    projectId = projectId,
                    content   = event.content.trim(),
                ))
            }
            is ProjectDetailEvent.EditComment -> viewModelScope.launch {
                commentRepo.edit(event.id, event.content.trim())
            }
            is ProjectDetailEvent.DeleteComment -> viewModelScope.launch {
                commentRepo.delete(event.id)
            }
            ProjectDetailEvent.ShowBulkMore -> _uiState.update { it.copy(showBulkMore = true) }
            ProjectDetailEvent.HideBulkMore -> _uiState.update { it.copy(showBulkMore = false) }
            is ProjectDetailEvent.BulkStar -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                ids.forEach { taskRepo.setStarred(it, event.starred) }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is ProjectDetailEvent.BulkSetPriority -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasksBySection.values.flatten().filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(priority = event.priority, updatedAt = LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            is ProjectDetailEvent.ToggleProjectTag -> viewModelScope.launch {
                val p = _uiState.value.project ?: return@launch
                val tags = p.tagIds.toMutableList()
                if (event.tagId in tags) tags.remove(event.tagId) else tags.add(event.tagId)
                projectRepo.update(p.copy(tagIds = tags, updatedAt = LocalDateTime.now()))
            }
            is ProjectDetailEvent.BulkAddTag -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasksBySection.values.flatten().filter { it.id in ids }.forEach { task ->
                    if (event.tagId !in task.tagIds)
                        taskRepo.update(task.copy(tagIds = task.tagIds + event.tagId, updatedAt = LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false) }
            }
            is ProjectDetailEvent.BulkSchedule -> viewModelScope.launch {
                val ids = _uiState.value.selectedTaskIds.toList()
                _uiState.value.tasksBySection.values.flatten().filter { it.id in ids }.forEach { task ->
                    taskRepo.update(task.copy(dueDate = event.date, updatedAt = LocalDateTime.now()))
                }
                _uiState.update { it.copy(showBulkMore = false, selectedTaskIds = emptySet()) }
            }
            ProjectDetailEvent.StartTimerForSelected -> viewModelScope.launch {
                val taskId = _uiState.value.selectedTaskIds.firstOrNull() ?: return@launch
                timeRepo.getActiveEntry(taskId)?.let { timeRepo.stopEntry(it.id) }
                timeRepo.insert(
                    com.onlyflows.domain.model.TimeEntry(
                        id        = "te_${System.currentTimeMillis()}",
                        taskId    = taskId,
                        startedAt = LocalDateTime.now(),
                    )
                )
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showBulkMore = false) }
            }
            ProjectDetailEvent.AbandonSelected -> viewModelScope.launch {
                val ids   = _uiState.value.selectedTaskIds
                val tasks = _uiState.value.tasksBySection.values.flatten()
                ids.forEach { id ->
                    val task = tasks.find { it.id == id } ?: return@forEach
                    if (task.recurrence != null) taskRepo.abandonRecurring(id)
                    else taskRepo.abandonTask(id)
                }
                _uiState.update { it.copy(selectedTaskIds = emptySet(), showBulkMore = false) }
            }
        }
    }

    private fun toggleSelect(taskId: String) {
        _uiState.update { state ->
            val selected = state.selectedTaskIds.toMutableSet()
            if (taskId in selected) selected.remove(taskId) else selected.add(taskId)
            state.copy(selectedTaskIds = selected)
        }
    }

    private fun addTask(id: String, title: String, note: String = "", sectionId: String?, priority: TaskPriority = TaskPriority.NONE, checklistItems: List<String> = emptyList()) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val projectId = _uiState.value.project?.id ?: return@launch
            val newTask = Task(
                id        = id,
                title     = title,
                note      = note,
                priority  = priority,
                projectId = projectId,
                sectionId = sectionId,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
            taskRepo.insert(newTask)
            checklistItems.forEachIndexed { index, itemTitle ->
                taskRepo.addChecklistItem(com.onlyflows.domain.model.ChecklistItem(
                    id        = "cli_${System.currentTimeMillis()}_$index",
                    taskId    = id,
                    title     = itemTitle,
                    sortOrder = index,
                ))
            }
            _uiState.update { it.copy(showAddTask = false) }
        }
    }

    private fun deleteSelected(ids: Set<String>) {
        viewModelScope.launch {
            ids.forEach { taskRepo.softDelete(it) }
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
        }
    }

    fun forceSync() = coordinator.forceSync()
}
