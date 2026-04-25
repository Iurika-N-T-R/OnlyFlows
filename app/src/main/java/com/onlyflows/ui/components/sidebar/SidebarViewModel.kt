package com.onlyflows.ui.components.sidebar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Data models ───────────────────────────────────────────────────────────────

data class ProjectProgress(
    val project: Project,
    val completedCount: Int,
    val totalCount: Int,
) {
    val ratio: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

sealed interface SidebarEntry {
    data class FolderHeader(val folder: Folder) : SidebarEntry
    data class ProjectRow(val progress: ProjectProgress, val indented: Boolean) : SidebarEntry
}

data class SidebarUiState(
    val inboxCount: Int                = 0,
    val todayCount: Int                = 0,
    val overdueCount: Int              = 0,
    val entries: List<SidebarEntry>    = emptyList(),
    val expandedFolderIds: Set<String> = emptySet(),
    val hiddenNavItems: Set<String>    = emptySet(),
)

sealed interface SidebarEvent {
    data class ToggleFolder(val folderId: String)                       : SidebarEvent
    data class RenameFolder(val folderId: String, val title: String)    : SidebarEvent
    data class DeleteFolder(val folderId: String)                       : SidebarEvent
    data class MoveProject(val projectId: String, val folderId: String?): SidebarEvent
    data class DeleteProject(val projectId: String)                     : SidebarEvent
    data class CreateProject(val title: String, val colorHex: String, val folderId: String?, val type: ProjectType = ProjectType.PROJECT) : SidebarEvent
    data class CreateFolder(val name: String) : SidebarEvent
    data class ReorderEntries(
        val folderIds: List<String>,
        val rootProjectIds: List<String>,
    ) : SidebarEvent
    data class SetNavItemHidden(val key: String, val hidden: Boolean) : SidebarEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class SidebarViewModel(app: Application) : AndroidViewModel(app) {

    private val container      = (app as OnlyFlowsApp).container
    private val taskRepo       = container.taskRepository
    private val projectRepo    = container.projectRepository
    private val folderRepo     = container.folderRepository
    private val sidebarPrefs   = container.sidebarPreferences

    // In-memory expanded set — seeded from DataStore on first load
    private val _expanded = MutableStateFlow<Set<String>>(emptySet())
    private val _uiState  = MutableStateFlow(SidebarUiState())
    val uiState: StateFlow<SidebarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed _expanded from persisted collapsed state before starting the main combine.
            // We get all folders first, then exclude the ones the user has collapsed.
            val persistedCollapsed = sidebarPrefs.collapsedFolderIds.first()
            val allFolders         = folderRepo.observeAll().first()
            val allFolderIds       = allFolders.map { it.id }.toSet()
            _expanded.value        = allFolderIds - persistedCollapsed

            // Track which folder IDs have been shown (to detect genuinely new folders).
            var seenFolderIds = allFolderIds

            val innerFlow = combine(
                folderRepo.observeAll(),
                projectRepo.observeAll().flatMapLatest { projects ->
                    if (projects.isEmpty()) flowOf(emptyList())
                    else combine(projects.map { project ->
                        combine(
                            taskRepo.observeCompletedCount(project.id),
                            taskRepo.observeAllCount(project.id),
                        ) { completed, total -> ProjectProgress(project, completed, total) }
                    }) { it.toList() }
                },
                taskRepo.observeInboxCount(),
                taskRepo.observeTodayCount(),
                taskRepo.observeOverdueCount(),
            ) { folders, progresses, inboxCount, todayCount, overdueCount ->
                arrayOf<Any>(folders, progresses, inboxCount, todayCount, overdueCount)
            }

            combine(innerFlow, _expanded, sidebarPrefs.hiddenNavItems) { packed, expanded, hiddenNavItems ->
                @Suppress("UNCHECKED_CAST")
                val folders      = packed[0] as List<Folder>
                @Suppress("UNCHECKED_CAST")
                val progresses   = packed[1] as List<ProjectProgress>
                val inboxCount   = packed[2] as Int
                val todayCount   = packed[3] as Int
                val overdueCount = packed[4] as Int

                // Auto-expand only genuinely NEW folders (never seen before).
                val folderIds    = folders.map { it.id }.toSet()
                val newFolderIds = folderIds - seenFolderIds
                seenFolderIds    = seenFolderIds + folderIds
                val newExpanded  = expanded + newFolderIds

                val entries = mutableListOf<SidebarEntry>()
                folders.sortedBy { it.sortOrder }.forEach { folder ->
                    entries += SidebarEntry.FolderHeader(folder)
                    progresses
                        .filter { it.project.folderId == folder.id }
                        .sortedBy { it.project.sortOrder }
                        .forEach { entries += SidebarEntry.ProjectRow(it, indented = true) }
                }
                progresses
                    .filter { it.project.folderId == null }
                    .sortedBy { it.project.sortOrder }
                    .forEach { entries += SidebarEntry.ProjectRow(it, indented = false) }

                SidebarUiState(
                    inboxCount        = inboxCount,
                    todayCount        = todayCount,
                    overdueCount      = overdueCount,
                    entries           = entries,
                    expandedFolderIds = newExpanded,
                    hiddenNavItems    = hiddenNavItems,
                )
            }.collect { state ->
                _uiState.value  = state
                _expanded.value = state.expandedFolderIds
            }
        }
    }

    fun onEvent(event: SidebarEvent) {
        when (event) {
            is SidebarEvent.ToggleFolder -> {
                val isCurrentlyExpanded = event.folderId in _expanded.value
                // Update memory state
                _expanded.value = if (isCurrentlyExpanded)
                    _expanded.value - event.folderId
                else
                    _expanded.value + event.folderId
                // Persist new collapsed state
                viewModelScope.launch {
                    sidebarPrefs.setCollapsed(event.folderId, collapsed = isCurrentlyExpanded)
                }
            }
            is SidebarEvent.RenameFolder -> viewModelScope.launch {
                val folder = _uiState.value.entries
                    .filterIsInstance<SidebarEntry.FolderHeader>()
                    .firstOrNull { it.folder.id == event.folderId }?.folder ?: return@launch
                folderRepo.update(folder.copy(title = event.title))
            }
            is SidebarEvent.DeleteFolder -> viewModelScope.launch {
                folderRepo.delete(event.folderId)
                // Clean up persisted state for the deleted folder
                sidebarPrefs.setCollapsed(event.folderId, collapsed = false)
            }
            is SidebarEvent.MoveProject -> viewModelScope.launch {
                val project = _uiState.value.entries
                    .filterIsInstance<SidebarEntry.ProjectRow>()
                    .firstOrNull { it.progress.project.id == event.projectId }?.progress?.project ?: return@launch
                projectRepo.update(project.copy(folderId = event.folderId))
            }
            is SidebarEvent.DeleteProject -> viewModelScope.launch {
                projectRepo.delete(event.projectId)
            }
            is SidebarEvent.CreateProject -> viewModelScope.launch {
                val newProject = Project(
                    id       = "project_${System.currentTimeMillis()}",
                    title    = event.title,
                    colorHex = event.colorHex,
                    folderId = event.folderId,
                    type     = event.type,
                )
                projectRepo.insert(newProject)
            }
            is SidebarEvent.CreateFolder -> viewModelScope.launch {
                val newFolder = Folder(
                    id       = "folder_${System.currentTimeMillis()}",
                    title    = event.name,
                    sortOrder = _uiState.value.entries.filterIsInstance<SidebarEntry.FolderHeader>().size,
                )
                folderRepo.insert(newFolder)
            }
            is SidebarEvent.SetNavItemHidden -> viewModelScope.launch {
                sidebarPrefs.setNavItemHidden(event.key, event.hidden)
            }
            is SidebarEvent.ReorderEntries -> viewModelScope.launch {
                val entries = _uiState.value.entries
                event.folderIds.forEachIndexed { index, id ->
                    val folder = entries.filterIsInstance<SidebarEntry.FolderHeader>()
                        .firstOrNull { it.folder.id == id }?.folder ?: return@forEachIndexed
                    folderRepo.update(folder.copy(sortOrder = index))
                }
                event.rootProjectIds.forEachIndexed { index, id ->
                    val project = entries.filterIsInstance<SidebarEntry.ProjectRow>()
                        .firstOrNull { it.progress.project.id == id }?.progress?.project ?: return@forEachIndexed
                    projectRepo.update(project.copy(sortOrder = index))
                }
            }
        }
    }
}
