package com.onlyflows.wear.presentation

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.onlyflows.wear.data.WearTask

@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel = viewModel(),
    onOpenMenu: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.let { viewModel.add(it) }
        }
    }
    fun startVoiceAdd() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Nouvelle tâche")
        runCatching { voiceLauncher.launch(intent) }
    }

    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Chip(
                onClick = onOpenMenu,
                label = { Text(state.filterLabel) },
                secondaryLabel = { Text("Changer de liste") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = ::startVoiceAdd,
                label = { Text("Ajouter") },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            state.loading -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.disconnected -> item {
                Text(
                    "Téléphone injoignable",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            state.tasks.isEmpty() -> item {
                Text(
                    "Aucune tâche",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> items(state.tasks.size) { i ->
                val task = state.tasks[i]
                TaskCard(task = task, onToggle = { viewModel.complete(task) }, onOpen = { onOpenTask(task.id) })
            }
        }
    }
}

@Composable
private fun TaskCard(task: WearTask, onToggle: () -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (task.completed) "✓" else "○",
                color = MaterialTheme.colors.primary,
                fontSize = 18.sp,
                modifier = Modifier.size(22.dp).clickable(onClick = onToggle),
            )
            Box(Modifier.width(8.dp))
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.button,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.overdue) Color(0xFFE04444) else MaterialTheme.colors.onSurface,
                )
                if (task.due != null) {
                    Text(
                        text = task.due,
                        style = MaterialTheme.typography.caption2,
                        color = if (task.overdue) Color(0xFFE04444) else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
