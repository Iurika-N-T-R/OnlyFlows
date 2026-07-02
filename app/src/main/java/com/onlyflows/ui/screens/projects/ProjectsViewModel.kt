package com.onlyflows.ui.screens.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.ui.components.sidebar.ProjectProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val columns: Map<ProjectStatus, List<ProjectsCard>> = emptyMap(),
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = true,
)

data class ProjectsCard(
    val progress: ProjectProgress,
    val folderName: String?,   // null if standalone project
)

class ProjectsViewModel(app: Application) : AndroidViewModel(app) {

    private val container   = (app as OnlyFlowsApp).container
    private val projectRepo = container.projectRepository
    private val folderRepo  = container.folderRepository
    private val taskRepo    = container.taskRepository
    private val appPrefs    = container.appPreferences

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    // ── Persisted view state ──────────────────────────────────────────────────
    // null = not loaded yet (avoids flashing the wrong view/page on startup)
    val viewMode: StateFlow<String?> = appPrefs.projectsViewMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)
    val boardPage: StateFlow<Int?> = appPrefs.projectsBoardPage
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)
    val collapsedStatuses: StateFlow<Set<String>> = appPrefs.projectsCollapsedStatuses
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptySet())

    fun setViewMode(mode: String)  = viewModelScope.launch { appPrefs.setProjectsViewMode(mode) }
    fun setBoardPage(page: Int)    = viewModelScope.launch { appPrefs.setProjectsBoardPage(page) }
    fun toggleCollapsed(status: ProjectStatus) = viewModelScope.launch {
        val cur = collapsedStatuses.value
        appPrefs.setProjectsCollapsedStatuses(
            if (status.name in cur) cur - status.name else cur + status.name
        )
    }

    fun moveProject(projectId: String, newStatus: ProjectStatus) {
        viewModelScope.launch { projectRepo.updateStatus(projectId, newStatus) }
    }

    init {
        viewModelScope.launch {
            combine(
                projectRepo.observeAll(),
                taskRepo.observeAllProjectProgress(),
                folderRepo.observeAll(),
            ) { projects, progressList, folders ->
                val progressMap = progressList.associateBy { it.projectId }
                val folderMap   = folders.associateBy { it.id }
                val cards = projects.map { project ->
                    val counts = progressMap[project.id]
                    ProjectsCard(
                        progress   = ProjectProgress(project, counts?.completed ?: 0, counts?.total ?: 0),
                        folderName = project.folderId?.let { folderMap[it]?.title },
                    )
                }
                val columns = ProjectStatus.entries.associateWith { status ->
                    cards.filter { it.progress.project.status == status }
                        .sortedBy { it.progress.project.sortOrder }
                }
                ProjectsUiState(columns = columns, folders = folders, isLoading = false)
            }.distinctUntilChanged().collect { _uiState.value = it }
        }
    }
}
