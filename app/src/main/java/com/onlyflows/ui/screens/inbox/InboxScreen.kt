package com.onlyflows.ui.screens.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.FlowFab
import com.onlyflows.ui.components.SelectionBar
import com.onlyflows.ui.components.TaskItem
import com.onlyflows.ui.overlays.AddTaskSheet
import com.onlyflows.ui.overlays.SmartMoveSheet

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InboxScreen(
    onTaskClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit      = {},
    viewModel: InboxViewModel     = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior    = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isSelectionMode   = state.selectedTaskIds.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.pendingDeleteTask) {
        val task = state.pendingDeleteTask ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message     = "\"${task.title}\" supprimée",
            actionLabel = "Annuler",
            duration    = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        else viewModel.confirmDelete()
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.onEvent(InboxEvent.ClearSelection)
    }

    if (state.showAddTask) {
        AddTaskSheet(
            projects       = state.projects,
            onDismiss      = { viewModel.onEvent(InboxEvent.HideAddTask) },
            onAdd          = { id, title, _, _, priority, projectId, sectionId ->
                viewModel.onEvent(InboxEvent.AddTask(id, title, priority, projectId, sectionId))
            },
            onTaskCreated  = { onTaskClick(it) },
        )
    }

    if (state.showMoveSheet) {
        SmartMoveSheet(
            count       = state.selectedTaskIds.size,
            projects    = state.projects,
            hiddenGroup = null,          // no hidden group — show both Anytime and Someday
            onConfirm   = { group, projectId, sectionId ->
                viewModel.onEvent(InboxEvent.MoveSelected(group, projectId, sectionId))
            },
            onDismiss = { viewModel.onEvent(InboxEvent.HideMoveSheet) },
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            "${state.selectedTaskIds.size} selected",
                            style      = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Row {
                            Icon(painter = painterResource(R.drawable.ic_nav_inbox), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Inbox",
                                style      = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, "Menu")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FlowFab(onClick = { viewModel.onEvent(InboxEvent.ShowAddTask) })
            }
        },
        bottomBar = {
            SelectionBar(
                selectedCount = state.selectedTaskIds.size,
                onMove   = { viewModel.onEvent(InboxEvent.ShowMoveSheet) },
                onDelete = { viewModel.onEvent(InboxEvent.DeleteSelected(state.selectedTaskIds)) },
                onMore   = { viewModel.onEvent(InboxEvent.ShowBulkMore) },
            )
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->

        val visibleTasks = remember(state.tasks, state.pendingDeleteTask?.id) {
            state.tasks.filter { it.id != state.pendingDeleteTask?.id }
        }
        if (!state.isLoading && visibleTasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EmptyState(
                    emoji    = "📥",
                    title    = "Inbox vide",
                    subtitle = "Toutes les tâches ont été traitées.",
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
                items(visibleTasks, key = { it.id }) { task ->
                    TaskItem(
                        task             = task,
                        tags             = state.tags,
                        isSelected       = task.id in state.selectedTaskIds,
                        isSelectionMode  = isSelectionMode,
                        onEditClick      = { if (!isSelectionMode) onTaskClick(it) },
                        onLongClick      = { viewModel.onEvent(InboxEvent.ToggleSelect(it)) },
                        onToggleComplete = { _ ->
                            if (isSelectionMode) {
                                viewModel.onEvent(InboxEvent.ToggleSelect(task.id))
                            } else {
                                viewModel.onEvent(InboxEvent.ToggleComplete(task.id, task.isCompleted))
                            }
                        },
                        onToggleChecklistItem = { itemId, done ->
                            viewModel.onEvent(InboxEvent.ToggleChecklistItem(itemId, done))
                        },
                        modifier  = Modifier.animateItem(),
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 54.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }

    if (state.showBulkMore) {
        val selectedTask = state.tasks.find { it.id == state.selectedTaskIds.firstOrNull() }
        com.onlyflows.ui.overlays.BulkMoreSheet(
            selectedCount  = state.selectedTaskIds.size,
            tags           = state.tags,
            isRecurring    = selectedTask?.recurrence != null,
            onStar         = { viewModel.onEvent(InboxEvent.BulkStar(it)) },
            onSetPriority  = { viewModel.onEvent(InboxEvent.BulkSetPriority(it)) },
            onAddTag       = { /* tag bulk not yet wired */ },
            onSchedule     = { viewModel.onEvent(InboxEvent.BulkSchedule(it)) },
            onAbandon      = { viewModel.onEvent(InboxEvent.AbandonSelected) },
            onTrackTask    = { viewModel.onEvent(InboxEvent.StartTimerForSelected) },
            onDismiss      = { viewModel.onEvent(InboxEvent.HideBulkMore) },
        )
    }
}
