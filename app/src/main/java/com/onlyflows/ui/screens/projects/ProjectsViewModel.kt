package com.onlyflows.ui.screens.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.ui.components.sidebar.ProjectProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModel(app: Application) : AndroidViewModel(app) {

    private val container   = (app as OnlyFlowsApp).container
    private val projectRepo = container.projectRepository
    private val folderRepo  = container.folderRepository
    private val taskRepo    = container.taskRepository

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun moveProject(projectId: String, newStatus: ProjectStatus) {
        viewModelScope.launch { projectRepo.updateStatus(projectId, newStatus) }
    }

    init {
        viewModelScope.launch {
            combine(
                projectRepo.observeAll().flatMapLatest { projects ->
                    if (projects.isEmpty()) flowOf(emptyList())
                    else combine(projects.map { project ->
                        combine(
                            taskRepo.observeCompletedCount(project.id),
                            taskRepo.observeAllCount(project.id),
                        ) { completed, total -> ProjectProgress(project, completed, total) }
                    }) { it.toList() }
                },
                folderRepo.observeAll(),
            ) { progresses, folders ->
                val folderMap = folders.associateBy { it.id }
                val cards = progresses.map { progress ->
                    ProjectsCard(
                        progress   = progress,
                        folderName = progress.project.folderId?.let { folderMap[it]?.title },
                    )
                }
                val columns = ProjectStatus.entries.associateWith { status ->
                    cards.filter { it.progress.project.status == status }
                        .sortedBy { it.progress.project.sortOrder }
                }
                ProjectsUiState(columns = columns, folders = folders, isLoading = false)
            }.collect { _uiState.value = it }
        }
    }
}
