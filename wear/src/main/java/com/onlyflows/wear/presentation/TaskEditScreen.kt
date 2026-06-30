package com.onlyflows.wear.presentation

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.onlyflows.wear.data.Paths
import com.onlyflows.wear.data.WearBridge
import com.onlyflows.wear.data.WearTaskDetail
import com.onlyflows.wear.data.getTaskBody
import com.onlyflows.wear.data.parseTaskDetail
import com.onlyflows.wear.data.updateTaskBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskEditViewModel(app: Application) : AndroidViewModel(app) {
    private val _detail = MutableStateFlow<WearTaskDetail?>(null)
    val detail = _detail.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _detail.value = parseTaskDetail(WearBridge.request(getApplication(), Paths.GET_TASK, getTaskBody(id)))
    }

    fun save(id: String, title: String, completed: Boolean, priority: Int, onDone: () -> Unit) = viewModelScope.launch {
        WearBridge.request(getApplication(), Paths.SAVE_TASK, updateTaskBody(id, title, completed, priority))
        onDone()
    }
}

@Composable
fun TaskEditScreen(
    taskId: String,
    onDone: () -> Unit,
    viewModel: TaskEditViewModel = viewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    LaunchedEffect(taskId) { viewModel.load(taskId) }

    var title     by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf<Boolean?>(null) }
    var priority  by remember { mutableStateOf<Int?>(null) }

    // Seed local editable state once the detail arrives.
    LaunchedEffect(detail) {
        detail?.let {
            if (title == null) title = it.title
            if (completed == null) completed = it.completed
            if (priority == null) priority = it.priority
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { title = it }
        }
    }
    fun editTitle() {
        runCatching {
            voiceLauncher.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            )
        }
    }

    val curTitle = title ?: detail?.title ?: "…"
    val curDone  = completed ?: false
    val curPrio  = priority ?: 0

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberScalingLazyListState(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Chip(
                onClick = ::editTitle,
                label = { Text(curTitle) },
                secondaryLabel = { Text("Modifier le titre") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ToggleChip(
                checked = curDone,
                onCheckedChange = { completed = it },
                label = { Text("Terminée") },
                toggleControl = { Text(if (curDone) "✓" else "—") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("Priorité", style = MaterialTheme.typography.caption1, modifier = Modifier.padding(top = 4.dp))
        }
        val prios = listOf(0 to "Aucune", 1 to "Basse", 2 to "Moyenne", 3 to "Haute")
        items(prios.size) { i ->
            val (p, label) = prios[i]
            ToggleChip(
                checked = curPrio == p,
                onCheckedChange = { priority = p },
                label = { Text(label) },
                toggleControl = { Text(if (curPrio == p) "●" else "○") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = { viewModel.save(taskId, curTitle, curDone, curPrio, onDone) },
                label = { Text("Enregistrer") },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
