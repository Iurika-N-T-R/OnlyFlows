package com.onlyflows.ui.screens.archived

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchivedUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean      = true,
)

class ArchivedViewModel(app: Application) : AndroidViewModel(app) {

    private val projectRepo = (app as OnlyFlowsApp).container.projectRepository

    private val _uiState = MutableStateFlow(ArchivedUiState())
    val uiState: StateFlow<ArchivedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepo.observeArchived().collect { projects ->
                _uiState.update { it.copy(projects = projects, isLoading = false) }
            }
        }
    }

    fun unarchive(projectId: String) = viewModelScope.launch {
        projectRepo.unarchive(projectId)
    }
}
