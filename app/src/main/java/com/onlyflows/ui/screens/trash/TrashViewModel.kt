package com.onlyflows.ui.screens.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val tasks: List<Task>  = emptyList(),
    val isLoading: Boolean = true,
    val showConfirmEmpty: Boolean = false,
)

sealed interface TrashEvent {
    data class Restore(val taskId: String)       : TrashEvent
    data class DeleteForever(val taskId: String) : TrashEvent
    data object ShowConfirmEmpty                 : TrashEvent
    data object HideConfirmEmpty                 : TrashEvent
    data object EmptyTrash                       : TrashEvent
}

class TrashViewModel(app: Application) : AndroidViewModel(app) {

    private val taskRepo = (app as OnlyFlowsApp).container.taskRepository

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            taskRepo.observeTrash().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    fun onEvent(event: TrashEvent) {
        when (event) {
            is TrashEvent.Restore -> viewModelScope.launch {
                taskRepo.restoreFromTrash(event.taskId)
            }
            is TrashEvent.DeleteForever -> viewModelScope.launch {
                taskRepo.delete(event.taskId)
            }
            TrashEvent.ShowConfirmEmpty -> _uiState.update { it.copy(showConfirmEmpty = true) }
            TrashEvent.HideConfirmEmpty -> _uiState.update { it.copy(showConfirmEmpty = false) }
            TrashEvent.EmptyTrash -> viewModelScope.launch {
                taskRepo.emptyTrash()
                _uiState.update { it.copy(showConfirmEmpty = false) }
            }
        }
    }
}
