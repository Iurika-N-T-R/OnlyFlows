package com.onlyflows.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DateResultsUiState(
    val date: LocalDate? = null,
    val tasks: List<Task> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
)

class DateResultsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as OnlyFlowsApp).container
    private val taskRepo  = container.taskRepository
    private val tagRepo   = container.tagRepository

    private val _uiState = MutableStateFlow(DateResultsUiState())
    val uiState: StateFlow<DateResultsUiState> = _uiState.asStateFlow()

    fun load(dateStr: String) {
        val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return
        _uiState.update { it.copy(date = date) }
        viewModelScope.launch {
            combine(
                taskRepo.searchByDate(date),
                tagRepo.observeAll(),
            ) { tasks, tags -> Pair(tasks, tags) }.collect { (tasks, tags) ->
                _uiState.update { it.copy(tasks = tasks, tags = tags, isLoading = false) }
            }
        }
    }
}
