package com.onlyflows.ui.screens.today

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturbAlt
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.onlyflows.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.ui.components.AgendaCard
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.CompletedSectionHeader
import com.onlyflows.ui.components.rememberCompletionHold
import com.onlyflows.ui.components.FlowFab
import com.onlyflows.ui.components.GroupHeader
import com.onlyflows.ui.components.QuoteCard
import com.onlyflows.ui.components.SelectionBar
import com.onlyflows.ui.components.TaskFilterBar
import com.onlyflows.ui.components.TaskItem
import com.onlyflows.ui.overlays.AddTaskSheet
import com.onlyflows.ui.overlays.BulkMoreSheet
import com.onlyflows.ui.overlays.MoveTaskSheet
import com.onlyflows.ui.theme.OnlyFlowsTheme
import com.onlyflows.ui.theme.flowColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Today's automatic sections. Declaration order is display order. */
private enum class DaySlot(val label: String, val emoji: String) {
    UNTIMED("", ""),
    MORNING("This Morning", "\uD83C\uDF05"),
    AFTERNOON("This Afternoon", "\u2600\uFE0F"),
    EVENING("This Evening", ""),
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    onTaskClick: (String) -> Unit   = {},
    onSearchClick: () -> Unit       = {},
    onOpenDrawer: () -> Unit        = {},
    viewModel: TodayViewModel       = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context        = LocalContext.current
    val quoteRepo      = remember { (context.applicationContext as OnlyFlowsApp).container.quoteRepository }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }

    BackHandler(enabled = state.selectedTaskIds.isNotEmpty()) {
        viewModel.onEvent(TodayEvent.ClearSelection)
    }

    val activity = LocalContext.current as Activity
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = true) { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title  = { Text("Quit OnlyFlows?") },
            confirmButton  = {
                TextButton(onClick = { activity.finish() }) { Text("Quit") }
            },
            dismissButton  = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            },
        )
    }

    val scrollBehavior    = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fc                = MaterialTheme.flowColors
    var completedExpanded by remember { mutableStateOf(false) }
    var abandonedExpanded by remember { mutableStateOf(false) }
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

    fun List<com.onlyflows.domain.model.Task>.applyFilters() = filter { task ->
        task.id != state.pendingDeleteTask?.id &&
        (state.activeTagFilter.isEmpty() || task.tagIds.any { it in state.activeTagFilter }) &&
        (state.activePriorityFilter == null || task.priority == state.activePriorityFilter)
    }

    val pendingToday = remember(state.todayTasks, state.pendingDeleteTask?.id, state.activeTagFilter, state.activePriorityFilter, state.movingToCompletedIds) {
        state.todayTasks.applyFilters().filter { !it.isCompleted || it.id in state.movingToCompletedIds }
    }
    val pendingEvening = remember(state.eveningTasks, state.pendingDeleteTask?.id, state.activeTagFilter, state.activePriorityFilter, state.movingToCompletedIds) {
        state.eveningTasks.applyFilters().filter { !it.isCompleted || it.id in state.movingToCompletedIds }
    }
    val filteredOverdue = remember(state.overdueTasks, state.pendingDeleteTask?.id, state.activeTagFilter, state.activePriorityFilter) {
        state.overdueTasks.applyFilters()
    }
    val completionHold = rememberCompletionHold()
    val completedTasks = remember(state.completedTasks, state.pendingDeleteTask?.id, state.activeTagFilter, state.activePriorityFilter) {
        state.completedTasks.filter { it.id != state.pendingDeleteTask?.id }
    }.filterNot { it.id in completionHold }
    val abandonedTasks = remember(state.abandonedTasks, state.pendingDeleteTask?.id) {
        state.abandonedTasks.filter { it.id != state.pendingDeleteTask?.id }
    }

    // A row the user just ticked stays in place for a moment, otherwise it leaves the list on
    // the frame the write lands — before the checkbox has drawn its first animation frame.
    val overdueRows = completionHold.apply(filteredOverdue)
    // Applied to the union before bucketing, so a held row keeps the slot it was already in
    // rather than reappearing somewhere else for the duration.
    val dayTasks    = completionHold.apply(pendingToday + pendingEvening)

    // Nothing left to act on today. Done and Won't-do are then the record of the day rather
    // than an aside, so they open on their own instead of sitting there as collapsed rows.
    val nothingPending = overdueRows.isEmpty() && dayTasks.isEmpty()

    // The day splits itself by the hour each task carries. Tasks with no hour are not "early",
    // they are unscheduled, so they lead the list under no header at all — a section called
    // "Anytime" would be one more thing to read for no information. An explicit This Evening
    // choice outranks the clock: the user already said when they mean to do it.
    val daySlots = remember(dayTasks) {
        dayTasks.groupBy { task ->
            when {
                task.scheduledGroup == TaskGroup.THIS_EVENING -> DaySlot.EVENING
                task.dueTime == null                          -> DaySlot.UNTIMED
                task.dueTime!!.hour < 12                      -> DaySlot.MORNING
                task.dueTime!!.hour < 18                      -> DaySlot.AFTERNOON
                else                                          -> DaySlot.EVENING
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_nav_today),
                            contentDescription = null,
                            tint               = Color.Unspecified,
                            modifier           = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = "Today",
                            style      = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        val completedCount = state.completedTasks.size
                        val totalCount = state.todayTasks.size + state.eveningTasks.size + completedCount
                        if (totalCount > 0) {
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text     = "$completedCount/$totalCount",
                                    style    = MaterialTheme.typography.labelMedium,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
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
            FlowFab(
                onClick = { viewModel.onEvent(TodayEvent.ShowAddTask) },
                visible = state.selectedTaskIds.isEmpty(),
            )
        },
        bottomBar = {
            SelectionBar(
                selectedCount = state.selectedTaskIds.size,
                onMove        = { viewModel.onEvent(TodayEvent.ShowMoveSheet) },
                onDelete      = { viewModel.onEvent(TodayEvent.DeleteSelected(state.selectedTaskIds)) },
                onMore        = { viewModel.onEvent(TodayEvent.ShowMoreSheet) },
            )
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->

        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Quote card ──
            if (quoteRepo.sessionQuote != null) {
                item(key = "quote_card") {
                    QuoteCard(
                        quote     = quoteRepo.sessionQuote!!,
                    )
                }
            }

            // ── Filter bar — always show (priority filter), tag chips only when tasks have tags ──
            item(key = "filter_bar") {
                TaskFilterBar(
                    availableTags        = state.tags,
                    activeTagFilter      = state.activeTagFilter,
                    activePriorityFilter = state.activePriorityFilter,
                    onToggleTag          = { viewModel.onEvent(TodayEvent.ToggleTagFilter(it)) },
                    onSetPriority        = { viewModel.onEvent(TodayEvent.SetPriorityFilter(it)) },
                )
            }

            // ── Calendar permission banner ──
            if (!state.hasCalendarPermission) {
                item(key = "calendar_permission") {
                    CalendarPermissionBanner(
                        onRequestPermission = {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Agenda card ──
            if (state.agendaEvents.isNotEmpty()) {
                item(key = "agenda") {
                    AgendaCard(
                        events   = state.agendaEvents,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Closing state ──
            // Finishing the day and never having anything planned are not the same thing, and
            // the old copy congratulated nobody while telling someone who just cleared eight
            // tasks to "enjoy your free time".
            if (!state.isLoading && nothingPending) {
                item(key = "empty_state") {
                    val finishedSomething = completedTasks.isNotEmpty() || abandonedTasks.isNotEmpty()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillParentMaxWidth(),
                    ) {
                        // The finished-day state uses an icon rather than a party emoji:
                        // it takes the theme's colour and renders the same on every device.
                        EmptyState(
                            title    = if (finishedSomething) "All done for today" else "Nothing for today",
                            subtitle = if (finishedSomething) "Everything you planned is behind you."
                                       else "Enjoy your free time, or add a task.",
                            emoji    = if (finishedSomething) null else "⭐",
                            icon     = if (finishedSomething) Icons.Outlined.CheckCircle else null,
                            iconTint = if (finishedSomething) MaterialTheme.colorScheme.primary else null,
                        )
                    }
                }
            }

            // ── Overdue group ──
            if (overdueRows.isNotEmpty()) {
                item(key = "overdue_header") {
                    GroupHeader(title = "Overdue", emoji = "🔴", iconSize = 24.dp)
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 20.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
                items(items = overdueRows, key = { "overdue_${it.id}" }) { task ->
                    TaskItem(
                        task                  = task,
                        tags                  = state.tags,
                        isSelected            = task.id in state.selectedTaskIds,
                        isSelectionMode       = state.selectedTaskIds.isNotEmpty(),
                        checkboxColor         = MaterialTheme.colorScheme.error,
                        onToggleComplete      = {
                            completionHold.onToggle(task, overdueRows)
                            viewModel.onEvent(TodayEvent.ToggleComplete(it, task.isCompleted))
                        },
                        onToggleChecklistItem = { id, done -> viewModel.onEvent(TodayEvent.ToggleChecklistItem(id, done)) },
                        onEditClick           = { onTaskClick(it) },
                        onLongClick           = { viewModel.onEvent(TodayEvent.ToggleSelect(it)) },
                        modifier              = Modifier.animateItem(),
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 54.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }

            // ── Time-of-day slots ────────────────────────────────────────────
            DaySlot.entries.forEach { slot ->
                val slotTasks = daySlots[slot].orEmpty()
                if (slotTasks.isEmpty()) return@forEach

                if (slot != DaySlot.UNTIMED) {
                    item(key = "slot_header_${slot.name}") {
                        if (slot == DaySlot.EVENING) {
                            GroupHeader(title = slot.label, painter = painterResource(R.drawable.ic_nav_evening))
                        } else {
                            GroupHeader(title = slot.label, emoji = slot.emoji, iconSize = 24.dp)
                        }
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 20.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }
                }

                items(items = slotTasks, key = { it.id }) { task ->
                    TaskItem(
                        task                  = task,
                        tags                  = state.tags,
                        isSelected            = task.id in state.selectedTaskIds,
                        isSelectionMode       = state.selectedTaskIds.isNotEmpty(),
                        onToggleComplete      = {
                            completionHold.onToggle(task, slotTasks)
                            viewModel.onEvent(TodayEvent.ToggleComplete(it, task.isCompleted))
                        },
                        onToggleChecklistItem = { id, done -> viewModel.onEvent(TodayEvent.ToggleChecklistItem(id, done)) },
                        onEditClick           = { onTaskClick(it) },
                        onLongClick           = { viewModel.onEvent(TodayEvent.ToggleSelect(it)) },
                        modifier              = Modifier.animateItem(),
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 54.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }

            // ── Completed section ─────────────────────────────────────────────
            if (completedTasks.isNotEmpty()) {
                item(key = "completed_header") {
                    if (nothingPending) {
                        GroupHeader(title = "Completed")
                    } else {
                        CompletedSectionHeader(
                            count    = completedTasks.size,
                            expanded = completedExpanded,
                            onToggle = { completedExpanded = !completedExpanded },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (nothingPending || completedExpanded) {
                    items(
                        items = completedTasks,
                        key   = { "completed_${it.id}" },
                    ) { task ->
                        TaskItem(
                            task                  = task,
                            tags                  = state.tags,
                            isSelected            = task.id in state.selectedTaskIds,
                            isSelectionMode       = state.selectedTaskIds.isNotEmpty(),
                            onToggleComplete      = { viewModel.onEvent(TodayEvent.ToggleComplete(it, task.isCompleted)) },
                            onToggleChecklistItem = { id, done -> viewModel.onEvent(TodayEvent.ToggleChecklistItem(id, done)) },
                            onEditClick           = { onTaskClick(it) },
                            onLongClick           = { viewModel.onEvent(TodayEvent.ToggleSelect(it)) },
                            modifier              = Modifier.animateItem(),
                        )
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 54.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }

            // ── Won't do section ──────────────────────────────────────────────
            if (abandonedTasks.isNotEmpty()) {
                item(key = "abandoned_header") {
                    if (nothingPending) {
                        GroupHeader(title = "Won't do")
                    } else {
                        CompletedSectionHeader(
                            count    = abandonedTasks.size,
                            expanded = abandonedExpanded,
                            onToggle = { abandonedExpanded = !abandonedExpanded },
                            label    = "Won't do",
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                if (nothingPending || abandonedExpanded) {
                    items(
                        items = abandonedTasks,
                        key   = { "abandoned_${it.id}" },
                    ) { task ->
                        TaskItem(
                            task                  = task,
                            tags                  = state.tags,
                            isSelected            = task.id in state.selectedTaskIds,
                            isSelectionMode       = state.selectedTaskIds.isNotEmpty(),
                            // A won't-do task cannot be ticked, so it shows a marker rather
                            // than a checkbox that does nothing when tapped.
                            leadingAction         = {
                                Icon(
                                    imageVector        = Icons.Outlined.DoNotDisturbAlt,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                    modifier           = Modifier.size(18.dp),
                                )
                            },
                            onToggleComplete      = {},
                            onToggleChecklistItem = { _, _ -> },
                            onEditClick           = { onTaskClick(it) },
                            onLongClick           = { viewModel.onEvent(TodayEvent.ToggleSelect(it)) },
                            modifier              = Modifier.animateItem(),
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

    // Add task sheet
    if (state.showAddTask) {
        val reminderCtx = LocalContext.current.applicationContext
        AddTaskSheet(
            defaultGroup  = com.onlyflows.domain.model.TaskGroup.TODAY,
            tags          = state.allTags,
            projects      = state.allProjects,
            onAdd         = { id, title, note, group, dueDate, dueTime, endTime, priority, projectId, sectionId, checklistItems, tagIds ->
                viewModel.onEvent(TodayEvent.AddTask(id, title, note, priority, projectId, sectionId, checklistItems, group, dueDate, dueTime, endTime, tagIds))
            },
            onDismiss     = { viewModel.onEvent(TodayEvent.HideAddTask) },
            onTaskCreated = { onTaskClick(it) },
            onAddReminderForTask = { id, at, locId, rec ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.onlyflows.data.notifications.ReminderCreator.attach(reminderCtx, id, at, locId, rec)
                }
            },
        )
    }

    if (state.showMoveSheet) {
        MoveTaskSheet(
            count     = state.selectedTaskIds.size,
            projects  = state.allProjects,
            onConfirm = { projectId, sectionId ->
                viewModel.onEvent(TodayEvent.MoveSelected(projectId, sectionId))
            },
            onDismiss = { viewModel.onEvent(TodayEvent.HideMoveSheet) },
        )
    }

    if (state.showMoreSheet) {
        val allTasks = state.todayTasks + state.eveningTasks + state.overdueTasks
        val selectedTask = allTasks.find { it.id == state.selectedTaskIds.firstOrNull() }
        BulkMoreSheet(
            selectedCount = state.selectedTaskIds.size,
            tags          = state.tags,
            isRecurring   = selectedTask?.recurrence != null,
            onStar        = { viewModel.onEvent(TodayEvent.StarSelected(it)) },
            onSetPriority = { viewModel.onEvent(TodayEvent.SetPrioritySelected(it)) },
            onAddTag      = { viewModel.onEvent(TodayEvent.AddTagSelected(it)) },
            onSchedule    = { viewModel.onEvent(TodayEvent.ScheduleSelected(it)) },
            onAbandon     = { viewModel.onEvent(TodayEvent.AbandonSelected) },
            onTrackTask   = { viewModel.onEvent(TodayEvent.StartTimerForSelected) },
            onDismiss     = { viewModel.onEvent(TodayEvent.HideMoreSheet) },
        )
    }
}

@Composable
private fun CalendarPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text     = "Enable calendar access",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRequestPermission) {
                Text("Allow")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TodayScreenPreview() {
    OnlyFlowsTheme {
        TodayScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TodayScreenDarkPreview() {
    OnlyFlowsTheme(darkTheme = true) {
        TodayScreen()
    }
}
