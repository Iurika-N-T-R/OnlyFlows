package com.onlyflows.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.ActionType
import com.onlyflows.domain.model.DailyCount
import com.onlyflows.data.repository.ActionLogRepository
import com.onlyflows.data.repository.ProjectRepository
import com.onlyflows.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StatisticsUiState(
    val projectCount: Int         = 0,
    val activeTaskCount: Int      = 0,
    val completedTaskCount: Int   = 0,
    val abandonedTaskCount: Int   = 0,
    val dailyCompletions: List<DailyCount> = emptyList(),
    val selectedPeriodDays: Int   = 7,
    val dbSizeMb: Float           = 0f,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(app: Application) : AndroidViewModel(app) {

    private val container       = (app as OnlyFlowsApp).container
    private val taskRepo: TaskRepository             = container.taskRepository
    private val projectRepo: ProjectRepository       = container.projectRepository
    private val actionLogRepo: ActionLogRepository   = container.actionLogRepository

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        // Read DB file size once
        val dbFile = app.getDatabasePath("onlyflows.db")
        val sizeMb = if (dbFile.exists()) dbFile.length().toFloat() / (1024f * 1024f) else 0f
        _uiState.update { it.copy(dbSizeMb = sizeMb) }

        // Observe task counts + project count
        viewModelScope.launch {
            combine(
                taskRepo.observeActiveCount(),
                taskRepo.observeAllCompletedCount(),
                taskRepo.observeAllArchivedCount(),
                projectRepo.observeAll(),
            ) { active, completed, archived, projects ->
                _uiState.update { state ->
                    state.copy(
                        activeTaskCount    = active,
                        completedTaskCount = completed,
                        abandonedTaskCount = archived,
                        projectCount       = projects.size,
                    )
                }
            }.collect {}
        }

        // Observe daily completions — reacts to period changes
        viewModelScope.launch {
            _uiState
                .flatMapLatest { state ->
                    val since = LocalDate.now()
                        .minusDays(state.selectedPeriodDays.toLong())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    actionLogRepo.observeCompletedPerDay(since)
                }
                .collect { daily ->
                    _uiState.update { it.copy(dailyCompletions = daily) }
                }
        }
    }

    fun selectPeriod(days: Int) {
        _uiState.update { it.copy(selectedPeriodDays = days) }
    }
}
