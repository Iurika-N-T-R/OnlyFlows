package com.onlyflows.ui.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.model.GoalFrequency
import com.onlyflows.domain.model.GoalStatus
import com.onlyflows.domain.model.computeStatus
import com.onlyflows.domain.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class GoalFilter { ALL, ON_TRACK, AT_RISK, NOT_STARTED }

data class GoalsUiState(
    val allGoals: List<Goal> = emptyList(),
    val filter: GoalFilter = GoalFilter.ALL,
    val showAddGoal: Boolean = false,
) {
    val activeGoals: List<Goal> get() = allGoals.filter { !it.isCompleted && it.deletedAt == null }
    val completedGoals: List<Goal> get() = allGoals.filter { it.isCompleted && it.deletedAt == null }
    val filteredActive: List<Goal> get() = when (filter) {
        GoalFilter.ALL -> activeGoals
        GoalFilter.ON_TRACK -> activeGoals.filter { it.computeStatus() == GoalStatus.ON_TRACK }
        GoalFilter.AT_RISK -> activeGoals.filter { it.computeStatus() == GoalStatus.AT_RISK }
        GoalFilter.NOT_STARTED -> activeGoals.filter { it.computeStatus() == GoalStatus.NOT_STARTED }
    }
}

sealed interface GoalsEvent {
    data object ShowAddGoal : GoalsEvent
    data object HideAddGoal : GoalsEvent
    data class AddGoal(
        val title: String,
        val frequency: GoalFrequency,
        val targetCount: Int,
        val colorHex: String,
        val emoji: String,
        val deadline: LocalDate?,
        val tags: List<String>,
    ) : GoalsEvent
    data class IncrementProgress(val goalId: String) : GoalsEvent
    data class DecrementProgress(val goalId: String) : GoalsEvent
    data class DeleteGoal(val goalId: String) : GoalsEvent
    data class ToggleCompleted(val goalId: String, val completed: Boolean) : GoalsEvent
    data class SetFilter(val filter: GoalFilter) : GoalsEvent
}

class GoalsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: GoalRepository = (app as OnlyFlowsApp).container.goalRepository

    private val _showAddGoal = MutableStateFlow(false)
    private val _filter = MutableStateFlow(GoalFilter.ALL)

    val uiState: StateFlow<GoalsUiState> = combine(
        repo.observeAll(),
        _showAddGoal,
        _filter,
    ) { goals, showAdd, filter ->
        GoalsUiState(allGoals = goals, filter = filter, showAddGoal = showAdd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun onEvent(event: GoalsEvent) {
        when (event) {
            GoalsEvent.ShowAddGoal -> _showAddGoal.value = true
            GoalsEvent.HideAddGoal -> _showAddGoal.value = false
            is GoalsEvent.SetFilter -> _filter.value = event.filter
            is GoalsEvent.AddGoal -> {
                _showAddGoal.value = false
                viewModelScope.launch {
                    repo.insert(
                        Goal(
                            id          = UUID.randomUUID().toString(),
                            title       = event.title,
                            frequency   = event.frequency,
                            targetCount = event.targetCount,
                            colorHex    = event.colorHex,
                            emoji       = event.emoji,
                            deadline    = event.deadline,
                            tags        = event.tags,
                        )
                    )
                }
            }
            is GoalsEvent.IncrementProgress -> viewModelScope.launch {
                val goal = uiState.value.allGoals.find { it.id == event.goalId } ?: return@launch
                val next = (goal.currentCount + 1).coerceAtMost(goal.targetCount)
                repo.setProgress(event.goalId, next)
                if (next >= goal.targetCount) repo.setCompleted(event.goalId, true)
            }
            is GoalsEvent.DecrementProgress -> viewModelScope.launch {
                val goal = uiState.value.allGoals.find { it.id == event.goalId } ?: return@launch
                val next = (goal.currentCount - 1).coerceAtLeast(0)
                repo.setProgress(event.goalId, next)
                if (next < goal.targetCount) repo.setCompleted(event.goalId, false)
            }
            is GoalsEvent.DeleteGoal -> viewModelScope.launch {
                repo.softDelete(event.goalId)
            }
            is GoalsEvent.ToggleCompleted -> viewModelScope.launch {
                repo.setCompleted(event.goalId, event.completed)
            }
        }
    }
}
