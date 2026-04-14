package com.onlyflows.ui.screens.upcoming

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.ui.components.TaskItem
import java.time.LocalDate
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun UpcomingScreen(
    onTaskClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit      = {},
    viewModel: UpcomingViewModel  = viewModel(),
) {
    val state          by viewModel.uiState.collectAsStateWithLifecycle()
    val listState       = rememberLazyListState()
    val scope           = rememberCoroutineScope()
    val scrollBehavior  = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showViewMenu    by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }

    // Local mutable copy of items for drag-visual purposes
    val haptic = LocalHapticFeedback.current
    val baseItems = remember(state.items, state.pendingDeleteTask, state.hasCalendarPermission) {
        val pendingId = state.pendingDeleteTask?.id
        state.items.filter { item ->
            (pendingId == null || item !is UpcomingItem.TaskRow || item.task.id != pendingId)
        }
    }
    var localItems by remember(baseItems) { mutableStateOf(baseItems) }
    var draggingTaskId by remember { mutableStateOf<String?>(null) }
    var pendingReschedule by remember { mutableStateOf(false) }

    val reorderState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey   = to.key   as? String ?: return@rememberReorderableLazyListState
        // Only the dragged item must be a task; target can be anything
        if (!fromKey.startsWith("task_")) return@rememberReorderableLazyListState
        // Don't drag into week/month headers or the permission banner
        if (toKey.startsWith("week_") || toKey.startsWith("month_") || toKey == "calendar_permission")
            return@rememberReorderableLazyListState

        val fromIdx = localItems.indexOfFirst { it.lazyKey == fromKey }
        if (fromIdx == -1) return@rememberReorderableLazyListState

        if (draggingTaskId == null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        draggingTaskId = fromKey.removePrefix("task_")

        val newItems = localItems.toMutableList()
        val movingItem = newItems.removeAt(fromIdx)

        val insertIdx = when {
            // Over another task or agenda row → insert at that position
            toKey.startsWith("task_") || toKey.startsWith("agenda_") || toKey.startsWith("empty_") -> {
                newItems.indexOfFirst { it.lazyKey == toKey }.takeIf { it != -1 } ?: newItems.size
            }
            // Over a DayHeader → insert as first item of that day (right after header)
            toKey.startsWith("day_") -> {
                val idx = newItems.indexOfFirst { it.lazyKey == toKey }
                if (idx != -1) idx + 1 else newItems.size
            }
            // Over a spacer → insert as last item of the preceding day (right before spacer)
            toKey.startsWith("spacer_") -> {
                newItems.indexOfFirst { it.lazyKey == toKey }.takeIf { it != -1 } ?: newItems.size
            }
            else -> newItems.size
        }

        newItems.add(insertIdx.coerceIn(0, newItems.size), movingItem)
        localItems = newItems
        pendingReschedule = true
    }

    LaunchedEffect(reorderState) {
        snapshotFlow { reorderState.isAnyItemDragging }
            .collect { isDragging ->
                if (!isDragging && pendingReschedule) {
                    pendingReschedule = false
                    val taskId = draggingTaskId ?: return@collect
                    draggingTaskId = null
                    val snapshot = localItems
                    val currentIdx = snapshot.indexOfFirst {
                        it is UpcomingItem.TaskRow && it.task.id == taskId
                    }
                    if (currentIdx < 0) return@collect
                    val targetDate = (currentIdx downTo 0)
                        .mapNotNull { snapshot.getOrNull(it) }
                        .filterIsInstance<UpcomingItem.DayHeader>()
                        .firstOrNull()?.date ?: return@collect
                    val originalDate = baseItems
                        .filterIsInstance<UpcomingItem.TaskRow>()
                        .find { it.task.id == taskId }?.date
                    if (targetDate != originalDate) {
                        viewModel.onEvent(UpcomingEvent.RescheduleTask(taskId, targetDate))
                    }
                }
            }
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

    // Auto-scroll to today once data is ready
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.todayItemIndex > 0) {
            listState.scrollToItem(state.todayItemIndex)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(com.onlyflows.R.drawable.ic_nav_upcoming), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Upcoming",
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
                actions = {
                    // Always-visible Today button
                    IconButton(onClick = {
                        scope.launch { listState.scrollToItem(state.todayItemIndex) }
                    }) {
                        Icon(Icons.Outlined.Today, contentDescription = "Go to today")
                    }
                    // 3-dot menu → view mode toggle
                    Box {
                        IconButton(onClick = { showViewMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "View options")
                        }
                        DropdownMenu(
                            expanded        = showViewMenu,
                            onDismissRequest = { showViewMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Outlined.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Week view")
                                    }
                                },
                                onClick = {
                                    showViewMenu = false
                                    if (state.viewMode != UpcomingViewMode.WEEK) viewModel.onEvent(UpcomingEvent.ToggleViewMode)
                                },
                                trailingIcon = if (state.viewMode == UpcomingViewMode.WEEK) ({
                                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp))
                                }) else null,
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Outlined.CalendarViewMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Month view")
                                    }
                                },
                                onClick = {
                                    showViewMenu = false
                                    if (state.viewMode != UpcomingViewMode.MONTH) viewModel.onEvent(UpcomingEvent.ToggleViewMode)
                                },
                                trailingIcon = if (state.viewMode == UpcomingViewMode.MONTH) ({
                                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp))
                                }) else null,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Calendar permission banner ──
            if (!state.hasCalendarPermission) {
                item(key = "calendar_permission") {
                    UpcomingCalendarPermissionBanner(
                        onRequestPermission = {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (!state.isLoading) {
                items(
                    items = localItems,
                    key         = { it.lazyKey },
                    contentType = { item ->
                        when (item) {
                            is UpcomingItem.WeekHeader  -> "week_header"
                            is UpcomingItem.MonthHeader -> "month_header"
                            is UpcomingItem.DayHeader   -> "day_header"
                            is UpcomingItem.TaskRow     -> "task_row"
                            is UpcomingItem.AgendaRow   -> "agenda_row"
                            is UpcomingItem.EmptyDay    -> "empty_day"
                            is UpcomingItem.DaySpacer   -> "spacer"
                        }
                    },
                ) { item ->
                    when (item) {
                        is UpcomingItem.WeekHeader  -> UpcomingGroupHeader(item.label, isWeek = true)
                        is UpcomingItem.MonthHeader -> UpcomingGroupHeader(item.label, isWeek = false)
                        is UpcomingItem.DayHeader   -> UpcomingDayHeader(date = item.date, label = item.label)
                        is UpcomingItem.EmptyDay    -> {
                            Text(
                                text     = "No tasks",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.padding(start = 54.dp, top = 2.dp, bottom = 4.dp),
                            )
                        }
                        is UpcomingItem.AgendaRow -> {
                            AgendaEventCompactRow(event = item.event)
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 54.dp),
                                color     = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                            )
                        }
                        is UpcomingItem.TaskRow -> {
                            ReorderableItem(reorderState, key = "task_${item.task.id}") { isDragging ->
                                val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "drag_elev")
                                Surface(shadowElevation = elevation) {
                                    TaskItem(
                                        task      = item.task,
                                        tags      = state.tags,
                                        isDraggable = true,
                                        dragHandle = {
                                            // Invisible drag handle — whole row is long-press draggable
                                        },
                                        onToggleComplete = { _ ->
                                            viewModel.toggleComplete(item.task.id, item.task.isCompleted)
                                        },
                                        onToggleChecklistItem = { id, done ->
                                            viewModel.toggleChecklistItem(id, done)
                                        },
                                        onEditClick = { onTaskClick(it) },
                                        modifier = Modifier.longPressDraggableHandle(),
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 54.dp),
                                color     = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                            )
                        }
                        is UpcomingItem.DaySpacer -> Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ── Calendar permission banner ────────────────────────────────────────────────

@Composable
private fun UpcomingCalendarPermissionBanner(
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
                text     = "Activer l'accès au calendrier",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRequestPermission) {
                Text("Autoriser")
            }
        }
    }
}

// ── Agenda event compact row ──────────────────────────────────────────────────

@Composable
private fun AgendaEventCompactRow(event: AgendaEvent) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
    ) {
        val color = runCatching {
            Color(android.graphics.Color.parseColor(event.colorHex))
        }.getOrElse { MaterialTheme.colorScheme.primary }

        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(width = 3.dp, height = 36.dp),
        ) {}
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text  = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (event.time != null) {
                Text(
                    text  = event.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
        if (event.isAllDay) {
            Spacer(Modifier.weight(1f))
            Text(
                text  = "All day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

// ── Group headers ─────────────────────────────────────────────────────────────

@Composable
private fun UpcomingGroupHeader(label: String, isWeek: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
    ) {
        Text(
            text       = if (isWeek) "W  $label" else label,
            style      = if (isWeek) MaterialTheme.typography.labelMedium
                         else        MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Day header ────────────────────────────────────────────────────────────────

@Composable
private fun UpcomingDayHeader(date: LocalDate, label: String) {
    val today   = LocalDate.now()
    val isToday = date == today
    val isPast  = date.isBefore(today)
    val dayColor = when {
        isPast  -> MaterialTheme.colorScheme.error
        isToday -> MaterialTheme.colorScheme.primary
        else    -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Text(
            text       = date.dayOfMonth.toString(),
            style      = MaterialTheme.typography.displaySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color      = dayColor,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPast) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            color    = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.weight(1f),
        )
        if (isToday) {
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    "Today",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
