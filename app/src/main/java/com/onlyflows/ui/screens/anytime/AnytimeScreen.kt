package com.onlyflows.ui.screens.anytime

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
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.ui.components.CompletedSectionHeader
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.SelectionBar
import com.onlyflows.ui.components.TaskItem
import com.onlyflows.ui.components.rememberCompletionHold
import com.onlyflows.ui.components.TaskFilterBar
import com.onlyflows.ui.overlays.BulkMoreSheet
import com.onlyflows.ui.overlays.SmartMoveSheet

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AnytimeScreen(
    onTaskClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit       = {},
    viewModel: AnytimeViewModel    = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior  = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isSelectionMode = state.selectedTaskIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        viewModel.onEvent(AnytimeEvent.ClearSelection)
    }

    if (state.showMoveSheet) {
        SmartMoveSheet(
            count       = state.selectedTaskIds.size,
            projects    = state.projects,
            hiddenGroup = TaskGroup.ANYTIME,
            onConfirm   = { group, projectId, sectionId ->
                viewModel.onEvent(AnytimeEvent.MoveSelected(group, projectId, sectionId))
            },
            onDismiss = { viewModel.onEvent(AnytimeEvent.HideMoveSheet) },
        )
    }

    if (state.showBulkMore) {
        val selectedTask = state.tasks.find { it.id == state.selectedTaskIds.firstOrNull() }
        BulkMoreSheet(
            selectedCount = state.selectedTaskIds.size,
            tags          = state.tags,
            isRecurring   = selectedTask?.recurrence != null,
            onStar        = { viewModel.onEvent(AnytimeEvent.BulkStar(it)) },
            onSetPriority = { viewModel.onEvent(AnytimeEvent.BulkSetPriority(it)) },
            onAddTag      = { viewModel.onEvent(AnytimeEvent.BulkAddTag(it)) },
            onSchedule    = { viewModel.onEvent(AnytimeEvent.BulkSchedule(it)) },
            onAbandon     = { viewModel.onEvent(AnytimeEvent.AbandonSelected) },
            onTrackTask   = { viewModel.onEvent(AnytimeEvent.StartTimerForSelected) },
            onDismiss     = { viewModel.onEvent(AnytimeEvent.HideBulkMore) },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.pendingDeleteTask) {
        val task = state.pendingDeleteTask ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message     = "\"${task.title}\" deleted",
            actionLabel = "Cancel",
            duration    = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        else viewModel.confirmDelete()
    }

    val filteredTasks = remember(state.tasks, state.pendingDeleteTask?.id, state.activeTagFilter, state.activePriorityFilter) {
        state.tasks.filter { task ->
            task.id != state.pendingDeleteTask?.id &&
            (state.activeTagFilter.isEmpty() || task.tagIds.any { it in state.activeTagFilter }) &&
            (state.activePriorityFilter == null || task.priority == state.activePriorityFilter)
        }
    }
    var completedExpanded by remember { mutableStateOf(false) }

    // A ticked row stays in place long enough for the checkbox animation; without it the row
    // leaves the list on the frame the database write lands.
    val completionHold = rememberCompletionHold()
    val taskRows       = completionHold.apply(filteredTasks)

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(R.drawable.ic_nav_anytime), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Anytime",
                                style      = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
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
        bottomBar = {
            SelectionBar(
                selectedCount = state.selectedTaskIds.size,
                onMove   = { viewModel.onEvent(AnytimeEvent.ShowMoveSheet) },
                onDelete = { viewModel.onEvent(AnytimeEvent.DeleteSelected(state.selectedTaskIds)) },
                onMore   = { viewModel.onEvent(AnytimeEvent.ShowBulkMore) },
            )
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!isSelectionMode && state.tags.isNotEmpty()) {
                item(key = "filter_bar") {
                    TaskFilterBar(
                        availableTags        = state.tags,
                        activeTagFilter      = state.activeTagFilter,
                        activePriorityFilter = state.activePriorityFilter,
                        onToggleTag          = { viewModel.onEvent(AnytimeEvent.ToggleTagFilter(it)) },
                        onSetPriority        = { viewModel.onEvent(AnytimeEvent.SetPriorityFilter(it)) },
                    )
                }
            }

            if (!state.isLoading && filteredTasks.isEmpty() && state.completedTasks.isEmpty()) {
                item(key = "empty") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillParentMaxWidth(),
                    ) {
                        EmptyState(
                            emoji    = "✨",
                            title    = "Anytime empty",
                            subtitle = "Tasks with priority and no date appear here.",
                        )
                    }
                }
            } else {
                items(taskRows, key = { it.id }) { task ->
                    TaskItem(
                        task             = task,
                        tags             = state.tags,
                        isSelected       = task.id in state.selectedTaskIds,
                        isSelectionMode  = isSelectionMode,
                        projectName      = task.projectId?.let { pid -> state.projects.find { it.id == pid }?.title },
                        onToggleComplete = { _ ->
                            if (isSelectionMode) viewModel.onEvent(AnytimeEvent.ToggleSelect(task.id))
                            else {
                                completionHold.onToggle(task, taskRows)
                                viewModel.onEvent(AnytimeEvent.ToggleComplete(task.id, task.isCompleted))
                            }
                        },
                        onLongClick = { viewModel.onEvent(AnytimeEvent.ToggleSelect(it)) },
                        onEditClick = { if (!isSelectionMode) onTaskClick(it) },
                        modifier    = Modifier.animateItem(),
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 54.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }

            // ── Completed today ───────────────────────────────────────────────
            if (state.completedTasks.isNotEmpty()) {
                item(key = "completed_header") {
                    CompletedSectionHeader(
                        count    = state.completedTasks.size,
                        expanded = completedExpanded,
                        onToggle = { completedExpanded = !completedExpanded },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (completedExpanded) {
                    items(state.completedTasks.filterNot { it.id in completionHold }, key = { "completed_${it.id}" }) { task ->
                        TaskItem(
                            task             = task,
                            tags             = state.tags,
                            isSelectionMode  = false,
                            projectName      = task.projectId?.let { pid -> state.projects.find { it.id == pid }?.title },
                            onToggleComplete = { viewModel.onEvent(AnytimeEvent.ToggleComplete(task.id, task.isCompleted)) },
                            onEditClick      = { onTaskClick(it) },
                            modifier         = Modifier.animateItem(),
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
    }
}
