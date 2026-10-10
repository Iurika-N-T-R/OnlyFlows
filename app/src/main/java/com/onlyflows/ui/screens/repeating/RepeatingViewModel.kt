package com.onlyflows.ui.screens.repeating

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A project (or Inbox, when [project] is null) and the repeating tasks filed under it. */
data class RepeatingGroup(
    val project: Project?,
    val tasks: List<Task>,
)

data class RepeatingUiState(
    val groups: List<RepeatingGroup> = emptyList(),
    val tags: List<Tag>              = emptyList(),
    val totalCount: Int              = 0,
    val isLoading: Boolean           = true,
)

/**
 * Every repeating task in one list, grouped by project. A recurrence rule is a commitment the
 * user made once and then stops seeing: it only ever surfaces as whichever occurrence happens
 * to be due, scattered across Today and Upcoming, so there was no way to review the whole set.
 */
class RepeatingViewModel(app: Application) : AndroidViewModel(app) {

    private val container   = (app as OnlyFlowsApp).container
    private val taskRepo    = container.taskRepository
    private val tagRepo     = container.tagRepository
    private val projectRepo = container.projectRepository

    private val _uiState = MutableStateFlow(RepeatingUiState())
    val uiState: StateFlow<RepeatingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepo.observeRecurring(),
                projectRepo.observeAllProjectsAndLists(),
                tagRepo.observeAll(),
            ) { tasks, projects, tags -> Triple(tasks, projects, tags) }
                .collect { (tasks, projects, tags) ->
                    val byId   = projects.associateBy { it.id }
                    val groups = tasks
                        .groupBy { it.projectId }
                        .map { (projectId, grouped) -> RepeatingGroup(byId[projectId], grouped) }
                        // Inbox last: a repeating task with no project is the exception
                        .sortedWith(
                            compareBy(
                                { it.project == null },
                                { it.project?.sortOrder ?: 0 },
                                { it.project?.title?.lowercase() ?: "" },
                            )
                        )
                    _uiState.update {
                        it.copy(
                            groups     = groups,
                            tags       = tags,
                            totalCount = tasks.size,
                            isLoading  = false,
                        )
                    }
                }
        }
    }

    fun toggleComplete(taskId: String) {
        viewModelScope.launch {
            // Always the recurring path: completing here advances the rule to its next
            // occurrence rather than closing the task for good.
            taskRepo.complete(taskId)
        }
    }
}
