package com.onlyflows.ui.screens.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Task
import com.onlyflows.ui.components.CompletedSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit         = {},
    viewModel: LogbookViewModel      = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter            = androidx.compose.ui.res.painterResource(com.onlyflows.R.drawable.ic_nav_logbook),
                            contentDescription = null,
                            tint               = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier           = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Logbook",
                            style      = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->

        val hasContent = state.archivedProjects.isNotEmpty() || state.archivedTaskGroups.isNotEmpty()

        if (!state.isLoading && !hasContent) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    "Nothing archived yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                // ── Archived Projects ─────────────────────────────────────────
                if (state.archivedProjects.isNotEmpty()) {
                    item(key = "projects_header") {
                        Text(
                            text     = "Projects",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    items(state.archivedProjects, key = { "project_${it.id}" }) { project ->
                        LogbookProjectRow(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                        )
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 56.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }
                }

                // ── Archived Tasks ────────────────────────────────────────────
                if (state.archivedTaskGroups.isNotEmpty()) {
                    item(key = "tasks_header") {
                        Text(
                            text     = "Tasks",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start  = 20.dp,
                                end    = 20.dp,
                                top    = if (state.archivedProjects.isNotEmpty()) 16.dp else 8.dp,
                                bottom = 4.dp,
                            ),
                        )
                    }

                    state.archivedTaskGroups.forEach { group ->
                        val groupKey = group.project?.id ?: "inbox"
                        val expanded = groupKey in state.expandedTaskGroups

                        item(key = "group_header_$groupKey") {
                            CompletedSectionHeader(
                                count    = group.tasks.size,
                                expanded = expanded,
                                onToggle = { viewModel.toggleTaskGroup(groupKey) },
                                label    = group.project?.title ?: "Inbox",
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }

                        if (expanded) {
                            items(group.tasks, key = { "archived_task_${it.id}" }) { task ->
                                LogbookTaskRow(
                                    task     = task,
                                    onRestore = { viewModel.unarchiveTask(task.id) },
                                )
                                HorizontalDivider(
                                    modifier  = Modifier.padding(start = 56.dp),
                                    color     = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogbookProjectRow(project: Project, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = project.iconEmoji.ifBlank { project.title.firstOrNull()?.uppercaseChar()?.toString() ?: "P" },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = project.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (project.dueDate != null) {
                Text(
                    text  = "Due ${project.dueDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text     = "Archived",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LogbookTaskRow(task: Task, onRestore: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (task.dueDate != null) {
                Text(
                    text  = task.dueDate.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRestore) {
            Text(
                text  = "Restore",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
