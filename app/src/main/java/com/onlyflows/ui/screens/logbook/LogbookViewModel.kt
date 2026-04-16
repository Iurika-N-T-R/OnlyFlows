package com.onlyflows.ui.screens.logbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.ActionLog
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.repository.ActionLogRepository
import com.onlyflows.domain.repository.ProjectRepository
import com.onlyflows.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchivedTaskGroup(
    val project: Project?,   // null = Inbox
    val tasks: List<Task>,
)

data class LogbookUiState(
    val archivedProjects: List<Project>           = emptyList(),
    val archivedTaskGroups: List<ArchivedTaskGroup> = emptyList(),
    val expandedTaskGroups: Set<String>           = emptySet(), // projectId or "inbox"
    val journalEntries: List<ActionLog>           = emptyList(),
    val isLoading: Boolean                        = true,
    val selectedTab: Int                          = 0,          // 0=Archived, 1=Journal
)

class LogbookViewModel(app: Application) : AndroidViewModel(app) {

    private val projectRepo: ProjectRepository =
        (app as OnlyFlowsApp).container.projectRepository
    private val taskRepo: TaskRepository =
        (app as OnlyFlowsApp).container.taskRepository
    private val actionLogRepo: ActionLogRepository =
        (app as OnlyFlowsApp).container.actionLogRepository

    private val _uiState = MutableStateFlow(LogbookUiState())
    val uiState: StateFlow<LogbookUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                projectRepo.observeArchived(),
                projectRepo.observeAll(),
                taskRepo.observeAllArchived(),
            ) { archivedProjects, allProjects, archivedTasks ->
                val projectMap = allProjects.associateBy { it.id }
                val grouped = archivedTasks
                    .groupBy { it.projectId }
                    .map { (projectId, tasks) ->
                        ArchivedTaskGroup(
                            project = projectId?.let { projectMap[it] },
                            tasks   = tasks,
                        )
                    }
                    .sortedWith(compareBy(nullsLast()) { it.project?.title })
                Triple(archivedProjects, grouped, false)
            }.collect { (projects, groups, loading) ->
                _uiState.update { it.copy(
                    archivedProjects   = projects,
                    archivedTaskGroups = groups,
                    isLoading          = loading,
                ) }
            }
        }

        actionLogRepo.observeAll()
            .onEach { entries -> _uiState.update { it.copy(journalEntries = entries) } }
            .launchIn(viewModelScope)
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleTaskGroup(key: String) {
        _uiState.update { state ->
            val expanded = state.expandedTaskGroups.toMutableSet()
            if (key in expanded) expanded.remove(key) else expanded.add(key)
            state.copy(expandedTaskGroups = expanded)
        }
    }

    fun unarchiveTask(taskId: String) {
        viewModelScope.launch {
            taskRepo.setArchived(taskId, false)
        }
    }
}
