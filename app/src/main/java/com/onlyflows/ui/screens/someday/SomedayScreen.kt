package com.onlyflows.ui.screens.someday

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
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.ui.components.CompletedSectionHeader
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.QuoteCard
import com.onlyflows.ui.components.SelectionBar
import com.onlyflows.ui.components.TaskItem
import com.onlyflows.ui.components.TaskFilterBar
import androidx.compose.ui.platform.LocalContext
import com.onlyflows.ui.overlays.BulkMoreSheet
import com.onlyflows.ui.overlays.SmartMoveSheet

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SomedayScreen(
    onTaskClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit       = {},
    viewModel: SomedayViewModel    = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context        = LocalContext.current
    val quoteRepo      = remember { (context.applicationContext as OnlyFlowsApp).container.quoteRepository }
    val scrollBehavior  = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isSelectionMode = state.selectedTaskIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        viewModel.onEvent(SomedayEvent.ClearSelection)
    }

    if (state.showBulkMore) {
        val selectedTask = state.tasks.find { it.id == state.selectedTaskIds.firstOrNull() }
        BulkMoreSheet(
            selectedCount = state.selectedTaskIds.size,
            tags          = state.tags,
            isRecurring   = selectedTask?.recurrence != null,
            onStar        = { viewModel.onEvent(SomedayEvent.BulkStar(it)) },
            onSetPriority = { viewModel.onEvent(SomedayEvent.BulkSetPriority(it)) },
            onAddTag      = { viewModel.onEvent(SomedayEvent.BulkAddTag(it)) },
            onSchedule    = { viewModel.onEvent(SomedayEvent.BulkSchedule(it)) },
            onAbandon     = { viewModel.onEvent(SomedayEvent.AbandonSelected) },
            onTrackTask   = { viewModel.onEvent(SomedayEvent.StartTimerForSelected) },
            onDismiss     = { viewModel.onEvent(SomedayEvent.HideBulkMore) },
        )
    }

    if (state.showMoveSheet) {
        SmartMoveSheet(
            count       = state.selectedTaskIds.size,
            projects    = state.projects,
            hiddenGroup = TaskGroup.SOMEDAY,
            onConfirm   = { group, projectId, sectionId ->
                viewModel.onEvent(SomedayEvent.MoveSelected(group, projectId, sectionId))
            },
            onDismiss = { viewModel.onEvent(SomedayEvent.HideMoveSheet) },
        )
    }

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

    val filteredTasks = state.tasks.filter { task ->
        task.id != state.pendingDeleteTask?.id &&
        (state.activeTagFilter.isEmpty() || task.tagIds.any { it in state.activeTagFilter }) &&
        (state.activePriorityFilter == null || task.priority == state.activePriorityFilter)
    }
    var completedExpanded by remember { mutableStateOf(false) }

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
                            Icon(painter = painterResource(R.drawable.ic_nav_someday), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Someday",
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
                onMove   = { viewModel.onEvent(SomedayEvent.ShowMoveSheet) },
                onDelete = { viewModel.onEvent(SomedayEvent.DeleteSelected(state.selectedTaskIds)) },
                onMore   = { viewModel.onEvent(SomedayEvent.ShowBulkMore) },
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
            if (quoteRepo.sessionQuote != null) {
                item(key = "quote_card") {
                    QuoteCard(
                        quote     = quoteRepo.sessionQuote!!,
                    )
                }
            }
            if (!isSelectionMode && state.tags.isNotEmpty()) {
                item(key = "filter_bar") {
                    TaskFilterBar(
                        availableTags        = state.tags,
                        activeTagFilter      = state.activeTagFilter,
                        activePriorityFilter = state.activePriorityFilter,
                        onToggleTag          = { viewModel.onEvent(SomedayEvent.ToggleTagFilter(it)) },
                        onSetPriority        = { viewModel.onEvent(SomedayEvent.SetPriorityFilter(it)) },
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
                            emoji    = "🌊",
                            title    = "Rien en Someday",
                            subtitle = "Les tâches sans date ni priorité apparaissent ici.",
                        )
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskItem(
                        task             = task,
                        tags             = state.tags,
                        isSelected       = task.id in state.selectedTaskIds,
                        isSelectionMode  = isSelectionMode,
                        projectName      = task.projectId?.let { pid -> state.projects.find { it.id == pid }?.title },
                        onToggleComplete = { _ ->
                            if (isSelectionMode) viewModel.onEvent(SomedayEvent.ToggleSelect(task.id))
                            else viewModel.onEvent(SomedayEvent.ToggleComplete(task.id, task.isCompleted))
                        },
                        onLongClick = { viewModel.onEvent(SomedayEvent.ToggleSelect(it)) },
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
                    items(state.completedTasks, key = { "completed_${it.id}" }) { task ->
                        TaskItem(
                            task             = task,
                            tags             = state.tags,
                            isSelectionMode  = false,
                            projectName      = task.projectId?.let { pid -> state.projects.find { it.id == pid }?.title },
                            onToggleComplete = { viewModel.onEvent(SomedayEvent.ToggleComplete(task.id, task.isCompleted)) },
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
