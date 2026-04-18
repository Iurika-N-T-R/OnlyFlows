package com.onlyflows.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyflows.ui.screens.project.ProjectDetailScreen
import com.onlyflows.ui.screens.task.TaskDetailScreen

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
                    onTaskClick = { taskId ->
                        // open task inline in the same pane
                        /* caller can override; for now just navigate into detail via onProjectClick chain
                           — leave as no-op because AdaptiveAppLayout wires onTaskSelected directly */
                    },
                    onListClick = onProjectClick,
                )
            }
        }
    }
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
