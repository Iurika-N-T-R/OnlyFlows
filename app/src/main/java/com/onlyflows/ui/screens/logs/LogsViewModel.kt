package com.onlyflows.ui.screens.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.data.debug.AppLog
import com.onlyflows.data.debug.LogEntry
import com.onlyflows.data.debug.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogsUiState(
    val allEntries: List<LogEntry>     = emptyList(),
    val filter: LogLevel?              = null,
)

sealed interface LogsEvent {
    data class SetFilter(val level: LogLevel?) : LogsEvent
    data object Clear                          : LogsEvent
}

class LogsViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(LogsUiState(allEntries = AppLog.snapshot()))

    val uiState: StateFlow<LogsUiState> = _state

    val filtered: StateFlow<List<LogEntry>> = combine(
        _state
    ) { states ->
        val s = states[0]
        if (s.filter == null) s.allEntries.reversed()
        else s.allEntries.reversed().filter { it.level == s.filter }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppLog.snapshot().reversed())

    init {
        viewModelScope.launch {
            AppLog.entries.collect {
                _state.update { s -> s.copy(allEntries = AppLog.snapshot()) }
            }
        }
    }

    fun onEvent(event: LogsEvent) {
        when (event) {
            is LogsEvent.SetFilter -> _state.update { it.copy(filter = event.level) }
            LogsEvent.Clear        -> { AppLog.clear(); _state.update { it.copy(allEntries = emptyList()) } }
        }
    }

    fun buildShareText(): String =
        AppLog.snapshot().reversed().joinToString("\n") { e ->
            val ts = e.timestamp.toString().replace("T", " ").take(19)
            val base = "$ts  [${e.level}]  ${e.tag}: ${e.message}"
            if (e.throwable != null) "$base\n${e.throwable}" else base
        }
}
