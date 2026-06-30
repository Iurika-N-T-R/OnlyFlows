package com.onlyflows.wear.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.wear.data.Paths
import com.onlyflows.wear.data.PendingQueue
import com.onlyflows.wear.data.PendingTask
import com.onlyflows.wear.data.WearBridge
import com.onlyflows.wear.data.WearRefresh
import com.onlyflows.wear.data.WearTask
import com.onlyflows.wear.data.completeBody
import com.onlyflows.wear.data.WearSettings
import com.onlyflows.wear.data.getTasksBody
import com.onlyflows.wear.data.parseTasks
import com.onlyflows.wear.data.saveTaskBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<WearTask> = emptyList(),
    val loading: Boolean = true,
    val disconnected: Boolean = false,
    val filterLabel: String = "Today",
)

class TaskListViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(TaskListUiState())
    val state = _state.asStateFlow()

    private var filterType = "TODAY"
    private var filterId = ""

    init {
        // Live refresh: phone pings on any task change.
        viewModelScope.launch { WearRefresh.events.collect { load() } }
    }

    fun setFilter(type: String, id: String, label: String) {
        filterType = type
        filterId = id
        _state.value = _state.value.copy(filterLabel = label)
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            flushQueue()
            val showCompleted = WearSettings.showCompleted(getApplication())
            val json = WearBridge.request(getApplication(), Paths.GET_TASKS, getTasksBody(filterType, filterId, showCompleted))
            _state.value = _state.value.copy(
                tasks = parseTasks(json),
                loading = false,
                disconnected = json == null,
            )
        }
    }

    fun complete(task: WearTask) {
        viewModelScope.launch {
            WearBridge.request(getApplication(), Paths.COMPLETE, completeBody(task.id, !task.completed))
            load()
        }
    }

    fun add(title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            val resp = WearBridge.request(getApplication(), Paths.SAVE_TASK, saveTaskBody(t, filterType, filterId))
            if (resp == null) PendingQueue.add(getApplication(), PendingTask(t, filterType, filterId))
            load()
        }
    }

    /** Flush watch-created tasks queued while the phone was unreachable. Stops at the first failure. */
    private suspend fun flushQueue() {
        val pending = PendingQueue.all(getApplication())
        for (p in pending) {
            val resp = WearBridge.request(getApplication(), Paths.SAVE_TASK, saveTaskBody(p.title, p.filterType, p.filterId))
            if (resp != null) PendingQueue.removeFirst(getApplication()) else break
        }
    }
}
