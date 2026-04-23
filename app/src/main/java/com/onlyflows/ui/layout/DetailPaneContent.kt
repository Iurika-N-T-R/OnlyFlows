package com.onlyflows.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Note
import com.onlyflows.ui.overlays.NoteEditorContent
import com.onlyflows.ui.screens.project.ProjectDetailScreen
import com.onlyflows.ui.screens.task.TaskDetailScreen
import kotlinx.coroutines.launch

@Composable
fun DetailPaneContent(
    pane: DetailPane,
    onClose: () -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // key(pane) ensures a fresh ViewModel is created whenever the selection changes
        key(pane) {
            when (pane) {
                DetailPane.None -> EmptyDetailPane()

                is DetailPane.Task -> TaskDetailScreen(
                    taskId      = pane.id,
                    onBack      = onClose,
                    onDeleted   = onClose,
                    onConverted = { projectId ->
                        onClose()
                        onProjectClick(projectId)
                    },
                    onNavigateToProject = onProjectClick,
                )

                is DetailPane.Project -> ProjectDetailScreen(
                    projectId   = pane.id,
                    onBack      = onClose,
                    onTaskClick = { taskId -> },
                    onListClick = onProjectClick,
                )

                is DetailPane.Note -> NotePaneContent(
                    noteId    = pane.noteId,
                    projectId = pane.projectId,
                    onClose   = onClose,
                )
            }
        }
    }
}

@Composable
private fun NotePaneContent(
    noteId: String,
    projectId: String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val repo    = remember { (context.applicationContext as OnlyFlowsApp).container.noteRepository }
    val scope   = rememberCoroutineScope()
    var note    by remember { mutableStateOf<Note?>(null) }
    var loaded  by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        note   = repo.getById(noteId)
        loaded = true
    }

    if (!loaded) return

    NoteEditorContent(
        note            = note,
        startInViewMode = note != null,
        onSave          = { id, title, content ->
            scope.launch {
                val now = java.time.LocalDateTime.now()
                if (id != null) {
                    val existing = repo.getById(id) ?: return@launch
                    repo.update(existing.copy(title = title, content = content, updatedAt = now))
                } else {
                    repo.insert(Note(
                        id        = "note_${System.currentTimeMillis()}",
                        projectId = projectId,
                        title     = title,
                        content   = content,
                        createdAt = now,
                        updatedAt = now,
                    ))
                }
            }
        },
        onDismiss = onClose,
        modifier  = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    )
}

@Composable
private fun EmptyDetailPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector         = Icons.Outlined.TouchApp,
                contentDescription  = null,
                modifier            = Modifier.size(56.dp),
                tint                = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text  = "Select a task or project",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
