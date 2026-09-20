package com.onlyflows.ui.screens.project

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.StatusLogEntry
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.statusIcon
import com.onlyflows.ui.components.statusTint
import com.onlyflows.ui.components.FlowFab
import com.onlyflows.ui.components.NoteCard
import com.onlyflows.ui.components.SelectionBar
import com.onlyflows.ui.components.SectionHeader
import com.onlyflows.ui.components.TaskFilterBar
import com.onlyflows.ui.components.TaskItem
import com.onlyflows.ui.overlays.AddTaskSheet
import com.onlyflows.ui.overlays.BulkMoreSheet
import com.onlyflows.ui.overlays.MoveTaskSheet
import com.onlyflows.ui.overlays.NoteEditorSheet
import com.onlyflows.ui.theme.OnlyFlowsTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.onlyflows.domain.model.Project
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── Flat list item types ───────────────────────────────────────────────────────

private sealed interface PdItem {
    data class Header(val section: Section) : PdItem
    data class TaskRow(val task: Task)       : PdItem
}

private val PdItem.pdKey: String get() = when (this) {
    is PdItem.Header  -> "section_${this.section.id}"
    is PdItem.TaskRow -> this.task.id
}

private val priorityOrder = mapOf(
    TaskPriority.HIGH   to 0,
    TaskPriority.MEDIUM to 1,
    TaskPriority.LOW    to 2,
    TaskPriority.NONE   to 3,
)

// Completed tasks last, then dated tasks first (by date), then by priority.
private val taskAutoSort: Comparator<Task> = compareBy<Task>(
    { if (it.isCompleted) 1 else 0 },               // completed last
    { if (it.dueDate != null) 0 else 1 },            // dated first
    { it.dueDate?.toEpochDay() ?: Long.MAX_VALUE },  // date ASC
    { priorityOrder[it.priority] ?: 3 },             // priority HIGH→NONE
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onTaskClick: (String) -> Unit = {},
    onTaskWithReminderClick: ((String) -> Unit)? = null,
    onListClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onNoteSelected: ((noteId: String, projectId: String) -> Unit)? = null,
    viewModel: ProjectDetailViewModel = viewModel(),
) {
    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    BackHandler(enabled = state.selectedTaskIds.isNotEmpty()) {
        viewModel.onEvent(ProjectDetailEvent.ClearSelection)
    }
    var showStatusMenu       by remember { mutableStateOf(false) }
    var showProjectMenu     by remember { mutableStateOf(false) }
    var showEditProject     by remember { mutableStateOf(false) }
    var showAddSection        by remember { mutableStateOf(false) }
    var showReorderSections   by remember { mutableStateOf(false) }

    // Cover picker for child list cards
    var coverTargetListId by remember { mutableStateOf<String?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = coverTargetListId
        if (uri != null && target != null) viewModel.onEvent(ProjectDetailEvent.SetChildListCover(target, uri))
        coverTargetListId = null
    }
    // Card grid columns: 2 on phones, 3 on tablet portrait, 4 on tablet landscape
    val gridColumns = when {
        LocalConfiguration.current.screenWidthDp >= 840 -> 4
        LocalConfiguration.current.screenWidthDp >= 600 -> 3
        else                                            -> 2
    }
    val project = state.project
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    val projectColor = remember(project?.colorHex) {
        try { Color(android.graphics.Color.parseColor(project?.colorHex ?: "#4A90D9")) }
        catch (e: Exception) { Color(0xFF4A90D9) }
    }

    // Ordered section list (null = unsectioned group)
    val sections = remember(project) {
        buildList<Section?> {
            add(null)
            project?.sections?.sortedBy { it.sortOrder }?.let { addAll(it) }
        }
    }

    // ── Flat sorted list (auto-sort: date ASC + priority, then priority only if no date) ───

    val localItems: List<PdItem> = remember(state.tasksBySection, project) {
        buildList {
            sections.forEach { section ->
                val tasks = (state.tasksBySection[section?.id] ?: emptyList())
                    .sortedWith(taskAutoSort)
                if (section == null && tasks.isEmpty()) return@forEach
                if (section != null) add(PdItem.Header(section))
                tasks.forEach { add(PdItem.TaskRow(it)) }
            }
        }
    }

    // Progress
    val allTasks      = state.tasksBySection.values.flatten()
    val completedCount = allTasks.count { it.isCompleted }
    val totalCount     = allTasks.size
    val progressRatio  = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

    // Tags available in this project's tasks
    val availableTags = remember(state.tasksBySection, state.tags) {
        val tagIdSet = state.tasksBySection.values.flatten().flatMap { it.tagIds }.toSet()
        state.tags.filter { it.id in tagIdSet }
    }

    // Which task IDs are visible under the current filter (used inline, not to rebuild the list)
    val visibleTaskIds: Set<String> = remember(localItems.toList(), state.activeTagFilter, state.activePriorityFilter) {
        if (state.activeTagFilter.isEmpty() && state.activePriorityFilter == null) return@remember emptySet()
        localItems.filterIsInstance<PdItem.TaskRow>().filter { row ->
            val tagOk = state.activeTagFilter.isEmpty() || row.task.tagIds.any { it in state.activeTagFilter }
            val priOk = state.activePriorityFilter == null || row.task.priority == state.activePriorityFilter
            tagOk && priOk
        }.map { it.task.id }.toSet()
    }

    // Filtered displayItems derived from localItems — only used for header visibility logic
    val displayItems: List<PdItem> = remember(localItems.toList(), state.activeTagFilter, state.activePriorityFilter) {
        val noFilter = state.activeTagFilter.isEmpty() && state.activePriorityFilter == null
        if (noFilter) return@remember localItems.toList()

        // Find matching task IDs
        val matchingIds = localItems.filterIsInstance<PdItem.TaskRow>().filter { row ->
            val tagOk = state.activeTagFilter.isEmpty() || row.task.tagIds.any { it in state.activeTagFilter }
            val priOk = state.activePriorityFilter == null || row.task.priority == state.activePriorityFilter
            tagOk && priOk
        }.map { it.task.id }.toSet()

        // Rebuild list: show headers only if they have >= 1 visible task
        val result = mutableListOf<PdItem>()
        var pendingHeader: PdItem.Header? = null
        localItems.forEach { item ->
            when (item) {
                is PdItem.Header  -> { pendingHeader = item }
                is PdItem.TaskRow -> {
                    if (item.task.id in matchingIds) {
                        pendingHeader?.let { result.add(it); pendingHeader = null }
                        result.add(item)
                    }
                }
            }
        }
        result
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    if (project != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            if (project.type == ProjectType.LIST && project.iconEmoji.isNotBlank()) {
                                Text(project.iconEmoji, style = MaterialTheme.typography.headlineSmall)
                            } else when (project.type) {
                                ProjectType.PROJECT -> ProjectPieIcon(ratio = progressRatio, color = projectColor)
                                ProjectType.LIST    -> Icon(Icons.Outlined.FormatListBulleted, null, tint = projectColor, modifier = Modifier.size(24.dp))
                                ProjectType.NOTE    -> Icon(Icons.Outlined.Article, null, tint = projectColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text       = project.title,
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                modifier   = Modifier.weight(1f),
                            )
                            if (project.type == ProjectType.PROJECT && totalCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text     = "$completedCount/$totalCount",
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            if (state.totalTrackedSeconds > 0L) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text     = "⏱ ${formatProjectDuration(state.totalTrackedSeconds)}",
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showProjectMenu = true }) {
                            Icon(Icons.Outlined.MoreHoriz, "Project menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded         = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false },
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Edit") },
                                onClick = { showProjectMenu = false; showEditProject = true },
                            )
                            if (project != null && project.type != ProjectType.NOTE && project.sections.isNotEmpty()) {
                                DropdownMenuItem(
                                    text    = { Text("Reorder sections") },
                                    onClick = { showProjectMenu = false; showReorderSections = true },
                                )
                            }
                            DropdownMenuItem(
                                text    = { Text(if (state.hideCompleted) "Show completed" else "Hide completed") },
                                onClick = { showProjectMenu = false; viewModel.onEvent(ProjectDetailEvent.ToggleHideCompleted) },
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
        floatingActionButton = {
            if (state.selectedTaskIds.isEmpty()) {
                when (project?.type) {
                    ProjectType.NOTE -> FlowFab(onClick = { viewModel.onEvent(ProjectDetailEvent.ShowNewNoteEditor) })
                    else             -> FlowFab(onClick = { viewModel.onEvent(ProjectDetailEvent.ShowAddTask(null)) })
                }
            }
        },
        bottomBar = {
            SelectionBar(
                selectedCount = state.selectedTaskIds.size,
                onMove        = { viewModel.onEvent(ProjectDetailEvent.ShowMoveSheet) },
                onDelete      = { viewModel.onEvent(ProjectDetailEvent.DeleteSelected(state.selectedTaskIds)) },
                onArchive     = { viewModel.onEvent(ProjectDetailEvent.ArchiveSelected(state.selectedTaskIds)) },
                onMore        = { viewModel.onEvent(ProjectDetailEvent.ShowBulkMore) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->

        if (state.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                CircularProgressIndicator(color = projectColor)
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = isImporting,
            onRefresh    = { viewModel.forceSync() },
            modifier     = Modifier.fillMaxSize(),
        ) {

        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {

        // Project info header (status, countdown, note) — fixed above the tabs
        if (project != null) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            ) {
                // Status chip + deadline badge inline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (project.type == ProjectType.PROJECT) {
                        Box {
                            ProjectStatusChip(
                                status  = project.status,
                                onClick = { showStatusMenu = true },
                            )
                            DropdownMenu(
                                expanded         = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false },
                            ) {
                                ProjectStatus.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text        = { Text(s.label) },
                                        leadingIcon = { Icon(s.statusIcon, null, tint = s.statusTint, modifier = Modifier.size(18.dp)) },
                                        onClick     = {
                                            viewModel.onEvent(ProjectDetailEvent.UpdateStatus(s))
                                            showStatusMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    project.dueDate?.let { due ->
                        DeadlineBadge(dueDate = due, createdAt = project.createdAt.toLocalDate())
                    }
                }
                // Note/description
                if (project.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = project.note,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        // Filter bar — fixed between info and tabs
        if (project != null && project.type != ProjectType.NOTE && availableTags.isNotEmpty()) {
            TaskFilterBar(
                availableTags        = availableTags,
                activeTagFilter      = state.activeTagFilter,
                activePriorityFilter = state.activePriorityFilter,
                onToggleTag          = { viewModel.onEvent(ProjectDetailEvent.ToggleTagFilter(it)) },
                onSetPriority        = { viewModel.onEvent(ProjectDetailEvent.SetPriorityFilter(it)) },
            )
        }

        // LIST projects get a Lists tab; NOTE projects a Groups tab. Page kinds below:
        // 0 = child card grid (LIST/NOTE only), 1 = tasks/notes, 2 = archived, 3 = comments
        val isNoteType   = project?.type == ProjectType.NOTE
        val hasChildGrid = project?.type == ProjectType.LIST || isNoteType
        val archivedCount = if (isNoteType) state.archivedNotes.size else state.archivedTasks.size
        val tabTitles = buildList {
            if (hasChildGrid) {
                val label = if (isNoteType) "Groups" else "Lists"
                add(label + if (state.childLists.isNotEmpty()) " (${state.childLists.size})" else "")
            }
            add(if (isNoteType) "Notes" else "Tasks")
            add("Archived" + if (archivedCount > 0) " ($archivedCount)" else "")
            add("Comments" + if (state.comments.isNotEmpty()) " (${state.comments.size})" else "")
        }
        val pagerState = rememberPagerState(initialPage = 0) { tabTitles.size }
        val pagerScope = rememberCoroutineScope()

        TabRow(selectedTabIndex = pagerState.currentPage.coerceAtMost(tabTitles.size - 1)) {
            tabTitles.forEachIndexed { i, title ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick  = { pagerScope.launch { pagerState.animateScrollToPage(i) } },
                    text     = { Text(title) },
                )
            }
        }

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
        when (if (hasChildGrid) page else page + 1) {

        // ── Lists / Groups tab — child card grid ──────────────────────────────
        0 -> LazyColumn(
            contentPadding = PaddingValues(
                top    = 4.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            // cells = child lists + trailing "add" slot (null)
            val cells: List<Project?> = state.childLists + null
            items(cells.chunked(gridColumns), key = { row -> "cardrow_" + row.joinToString("_") { it?.id ?: "add" } }) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                ) {
                    row.forEach { child ->
                        if (child == null) {
                            AddListCard(
                                onCreate = { title -> viewModel.onEvent(ProjectDetailEvent.CreateChildList(title)) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            ChildListCard(
                                list          = child,
                                count         = if (child.type == ProjectType.NOTE) state.noteCountsByProject[child.id] ?: 0
                                                else state.childListCounts[child.id] ?: 0,
                                onClick       = { onListClick(child.id) },
                                onSetCover    = {
                                    coverTargetListId = child.id
                                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onRemoveCover = { viewModel.onEvent(ProjectDetailEvent.SetChildListCover(child.id, null)) },
                                modifier      = Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ── Notes tab — masonry grid (NOTE type) ──────────────────────────────
        1 -> if (isNoteType) {
            if (!state.isLoading && state.notes.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    EmptyState(
                        emoji    = "📝",
                        title    = "No notes",
                        subtitle = "Tap + to add a note.",
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns             = StaggeredGridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    contentPadding      = PaddingValues(
                        start  = 12.dp,
                        end    = 12.dp,
                        top    = 8.dp,
                        bottom = padding.calculateBottomPadding() + 88.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.notes, key = { "note_${it.id}" }) { note ->
                        NoteCard(
                            note     = note,
                            onClick  = {
                                if (onNoteSelected != null) onNoteSelected(note.id, projectId)
                                else viewModel.onEvent(ProjectDetailEvent.OpenNoteViewer(note))
                            },
                            onEdit   = {
                                if (onNoteSelected != null) onNoteSelected(note.id, projectId)
                                else viewModel.onEvent(ProjectDetailEvent.OpenNoteEditor(note))
                            },
                            onDelete        = { viewModel.onEvent(ProjectDetailEvent.DeleteNoteItem(note.id)) },
                            onTogglePin     = { viewModel.onEvent(ProjectDetailEvent.TogglePinNote(note)) },
                            onArchive       = { viewModel.onEvent(ProjectDetailEvent.SetNoteArchived(note, true)) },
                            contentMaxLines = 10,
                            compact         = true,
                            modifier        = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        // ── Tasks tab ─────────────────────────────────────────────────────────
        else LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top    = 4.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            // Empty state when project has no tasks at all
            if (!state.isLoading && localItems.none { it is PdItem.TaskRow }) {
                item(key = "empty_state") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillParentMaxWidth(),
                    ) {
                        EmptyState(
                            emoji    = "📋",
                            title    = "No tasks",
                            subtitle = "Add a task to start this project.",
                        )
                    }
                }
            }

            // Use localItems (not displayItems) so that reorder indices stay in sync.
            // Headers inside the filter are hidden via displayItems set; here we always
            // render localItems and skip items that are filtered out.
            val hasFilter = state.activeTagFilter.isNotEmpty() || state.activePriorityFilter != null
            // Sections that have at least one visible task (used to decide whether to show headers)
            val visibleSectionIds: Set<String?> = if (!hasFilter) emptySet() else run {
                var sectionId: String? = null
                val result = mutableSetOf<String?>()
                localItems.forEach { item ->
                    when (item) {
                        is PdItem.Header  -> sectionId = item.section.id
                        is PdItem.TaskRow -> if (item.task.id in visibleTaskIds) result += sectionId
                    }
                }
                result
            }

            items(
                items = localItems,
                key   = { it.pdKey },
            ) { item ->
                when (item) {
                    is PdItem.Header -> {
                        if (hasFilter && item.section.id !in visibleSectionIds) return@items
                        val sectionCollapsed = item.section.id in state.collapsedSectionIds
                        SectionMenuHeader(
                            section          = item.section,
                            accentColor      = projectColor,
                            isCollapsed      = sectionCollapsed,
                            onToggleCollapse = { viewModel.onEvent(ProjectDetailEvent.ToggleSectionCollapse(item.section.id)) },
                            onAddTask        = { viewModel.onEvent(ProjectDetailEvent.ShowAddTask(item.section.id)) },
                            onRename    = { newTitle ->
                                viewModel.onEvent(ProjectDetailEvent.RenameSection(item.section.id, newTitle))
                            },
                            onDelete    = {
                                viewModel.onEvent(ProjectDetailEvent.DeleteSection(item.section.id))
                            },
                        )
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 20.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }

                    is PdItem.TaskRow -> {
                        if (hasFilter && item.task.id !in visibleTaskIds) return@items
                        // Skip tasks belonging to a collapsed section
                        val taskSectionId = item.task.sectionId
                        if (taskSectionId != null && taskSectionId in state.collapsedSectionIds) return@items
                        // Skip completed tasks when hideCompleted is on
                        if (state.hideCompleted && item.task.isCompleted) return@items
                        val isSelectionMode = state.selectedTaskIds.isNotEmpty()
                        TaskItem(
                            task            = item.task,
                            tags            = state.tags,
                            isSelected      = item.task.id in state.selectedTaskIds,
                            isSelectionMode = isSelectionMode,
                            checkboxColor   = projectColor,
                            onToggleComplete      = { viewModel.onEvent(ProjectDetailEvent.ToggleComplete(it, item.task.isCompleted)) },
                            onToggleChecklistItem = { id, done -> viewModel.onEvent(ProjectDetailEvent.ToggleChecklistItem(id, done)) },
                            onEditClick           = { onTaskClick(it) },
                            onLongClick           = { viewModel.onEvent(ProjectDetailEvent.ToggleSelect(it)) },
                        )
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 56.dp, end = 20.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            thickness = 0.5.dp,
                        )
                    }
                }
            }

            item(key = "add_section_btn") {
                TextButton(
                    onClick  = { showAddSection = true },
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Section", style = MaterialTheme.typography.labelMedium)
                }
            }

        }

        // ── Archived tab (NOTE projects: masonry of archived notes) ──────────
        2 -> if (isNoteType) {
            if (state.archivedNotes.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    EmptyState(
                        emoji    = "🗂️",
                        title    = "No archived notes",
                        subtitle = "Archived notes land here.",
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns             = StaggeredGridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    contentPadding      = PaddingValues(
                        start  = 12.dp,
                        end    = 12.dp,
                        top    = 8.dp,
                        bottom = padding.calculateBottomPadding() + 88.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.archivedNotes, key = { "archived_note_${it.id}" }) { note ->
                        NoteCard(
                            note            = note,
                            onClick         = { viewModel.onEvent(ProjectDetailEvent.OpenNoteViewer(note)) },
                            onEdit          = { viewModel.onEvent(ProjectDetailEvent.OpenNoteEditor(note)) },
                            onDelete        = { viewModel.onEvent(ProjectDetailEvent.DeleteNoteItem(note.id)) },
                            onTogglePin     = { viewModel.onEvent(ProjectDetailEvent.TogglePinNote(note)) },
                            onArchive       = { viewModel.onEvent(ProjectDetailEvent.SetNoteArchived(note, false)) },
                            contentMaxLines = 10,
                            compact         = true,
                            modifier        = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        else LazyColumn(
            contentPadding = PaddingValues(
                top    = 4.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.archivedTasks.isEmpty()) {
                item(key = "archived_empty") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillParentMaxWidth().padding(top = 48.dp),
                    ) {
                        EmptyState(
                            emoji    = "🗂️",
                            title    = "No archived tasks",
                            subtitle = "Abandoned and archived tasks land here.",
                        )
                    }
                }
            } else {
                items(state.archivedTasks, key = { "archived_${it.id}" }) { task ->
                    TaskItem(
                        task            = task,
                        tags            = state.tags,
                        checkboxColor   = projectColor,
                        onToggleComplete      = {},
                        onToggleChecklistItem = { _, _ -> },
                        onEditClick     = {},
                        onLongClick     = {},
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    ) {
                        TextButton(onClick = { viewModel.onEvent(ProjectDetailEvent.UnarchiveTask(task.id)) }) {
                            Text("Restore", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 56.dp, end = 20.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        thickness = 0.5.dp,
                    )
                }
            }
        }

        // ── Comments tab — message-style layout, input pinned at the bottom ──
        else -> {
            var draft     by remember { mutableStateOf("") }
            var editingId by remember { mutableStateOf<String?>(null) }
            var editDraft by remember { mutableStateOf("") }

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    reverseLayout  = true,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                    modifier       = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    if (state.comments.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                "No comments",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    } else {
                        items(state.comments, key = { it.id }) { comment ->
                            if (editingId == comment.id) {
                                com.onlyflows.ui.components.CommentEditRow(
                                    initial   = editDraft,
                                    onConfirm = { content -> viewModel.onEvent(ProjectDetailEvent.EditComment(comment.id, content)); editingId = null },
                                    onCancel  = { editingId = null },
                                )
                            } else {
                                com.onlyflows.ui.components.CommentRow(
                                    comment  = comment,
                                    onEdit   = { editingId = comment.id; editDraft = comment.content },
                                    onDelete = { viewModel.onEvent(ProjectDetailEvent.DeleteComment(comment.id)) },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                com.onlyflows.ui.components.CommentInput(
                    draft    = draft,
                    onChange = { draft = it },
                    onSend   = { viewModel.onEvent(ProjectDetailEvent.AddComment(draft)); draft = "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .padding(bottom = padding.calculateBottomPadding()),
                )
            }
        }
        }
        } // end HorizontalPager

        } // end Column

        } // end PullToRefreshBox
    }

    if (state.showAddTask) {
        AddTaskSheet(
            tags          = state.tags,
            onAdd         = { id, title, note, group, dueDate, dueTime, endTime, priority, _, _, checklistItems ->
                viewModel.onEvent(ProjectDetailEvent.AddTask(id, title, note, state.addTaskSectionId, priority, checklistItems, group, dueDate, dueTime, endTime))
            },
            onDismiss     = { viewModel.onEvent(ProjectDetailEvent.HideAddTask) },
            onTaskCreated = { onTaskClick(it) },
            onTaskCreatedWithReminder = onTaskWithReminderClick,
        )
    }

    if (showAddSection) {
        var sectionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSection = false },
            title   = { Text("New Section") },
            text    = {
                OutlinedTextField(
                    value         = sectionName,
                    onValueChange = { sectionName = it },
                    label         = { Text("Section name") },
                    singleLine    = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        if (sectionName.isNotBlank()) {
                            viewModel.onEvent(ProjectDetailEvent.AddSection(sectionName.trim()))
                            showAddSection = false
                        }
                    },
                    enabled = sectionName.isNotBlank(),
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSection = false }) { Text("Cancel") }
            },
        )
    }

    if (showReorderSections && project != null && project.sections.isNotEmpty()) {
        ReorderSectionsSheet(
            sections  = project.sections.sortedBy { it.sortOrder },
            onConfirm = { orderedIds ->
                viewModel.onEvent(ProjectDetailEvent.ReorderSections(orderedIds))
                showReorderSections = false
            },
            onDismiss = { showReorderSections = false },
        )
    }

    if (showEditProject && project != null) {
        ProjectEditSheet(
            project    = project,
            statusLog  = state.statusLog,
            allTags    = state.tags,
            onSave     = { title, note, colorHex, dueDate, iconEmoji ->
                viewModel.onEvent(ProjectDetailEvent.UpdateProject(title, note, colorHex, dueDate, iconEmoji))
                showEditProject = false
            },
            onToggleTag = { viewModel.onEvent(ProjectDetailEvent.ToggleProjectTag(it)) },
            onArchive  = {
                viewModel.onEvent(ProjectDetailEvent.Archive)
                showEditProject = false
                onBack()
            },
            onDismiss  = { showEditProject = false },
        )
    }

    if (state.showMoveSheet) {
        MoveTaskSheet(
            count                   = state.selectedTaskIds.size,
            projects                = state.allProjects,
            currentProjectSections  = project?.sections?.sortedBy { it.sortOrder } ?: emptyList(),
            onConfirm               = { projectId, sectionId ->
                viewModel.onEvent(ProjectDetailEvent.MoveSelected(projectId, sectionId))
            },
            onMoveToSection         = { sectionId ->
                viewModel.onEvent(ProjectDetailEvent.MoveSelectedToSection(sectionId))
            },
            onDismiss               = { viewModel.onEvent(ProjectDetailEvent.HideMoveSheet) },
        )
    }

    if (state.showBulkMore) {
        val allTasks     = state.tasksBySection.values.flatten()
        val selectedTask = allTasks.find { it.id == state.selectedTaskIds.firstOrNull() }
        BulkMoreSheet(
            selectedCount = state.selectedTaskIds.size,
            tags          = state.tags,
            isRecurring   = selectedTask?.recurrence != null,
            onStar        = { viewModel.onEvent(ProjectDetailEvent.BulkStar(it)) },
            onSetPriority = { viewModel.onEvent(ProjectDetailEvent.BulkSetPriority(it)) },
            onAddTag      = { viewModel.onEvent(ProjectDetailEvent.BulkAddTag(it)) },
            onSchedule    = { viewModel.onEvent(ProjectDetailEvent.BulkSchedule(it)) },
            onAbandon     = { viewModel.onEvent(ProjectDetailEvent.AbandonSelected) },
            onTrackTask   = { viewModel.onEvent(ProjectDetailEvent.StartTimerForSelected) },
            onDismiss     = { viewModel.onEvent(ProjectDetailEvent.HideBulkMore) },
        )
    }

    // ── Note editor/viewer sheet (NOTE-type projects) ─────────────────────────
    if (state.isAddingNote || state.editingNote != null) {
        NoteEditorSheet(
            note            = state.editingNote,
            startInViewMode = state.noteViewMode,
            onSave          = { id, title, content ->
                viewModel.onEvent(ProjectDetailEvent.SaveNote(id, title, content))
            },
            onDismiss       = { viewModel.onEvent(ProjectDetailEvent.HideNoteEditor) },
        )
    }
}

// ── Pie chart icon ─────────────────────────────────────────────────────────────

@Composable
private fun ProjectPieIcon(
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val strokePx = 3.8f                  // stroke plus épais

    Canvas(modifier = modifier.size(28.dp)) {
        val diameter = size.minDimension
        val inset    = strokePx / 2f

        drawCircle(
            color  = color,
            radius = diameter / 2f - inset,
            style  = Stroke(width = strokePx),
        )

        if (ratio > 0f) {
            val innerPad = strokePx * 2.8f   // gap plus large entre cercle et camembert
            drawArc(
                color      = color,
                startAngle = -90f,
                sweepAngle = 360f * ratio.coerceIn(0f, 1f),
                useCenter  = true,
                topLeft    = Offset(innerPad, innerPad),
                size       = Size(diameter - innerPad * 2f, diameter - innerPad * 2f),
            )
        }
    }
}

// ── Status chip helpers ────────────────────────────────────────────────────────

private val ProjectStatus.label: String get() = when (this) {
    ProjectStatus.NOT_STARTED -> "Not Started"
    ProjectStatus.IN_PROGRESS -> "In Progress"
    ProjectStatus.WAITING     -> "Waiting"
    ProjectStatus.DONE        -> "Done"
    ProjectStatus.PAUSED      -> "Paused"
}

private val ProjectStatus.chipColor: Color @Composable get() = when (this) {
    ProjectStatus.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant
    ProjectStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
    ProjectStatus.WAITING     -> Color(0xFFFFF3CD)
    ProjectStatus.DONE        -> Color(0xFFD4EDDA)
    ProjectStatus.PAUSED      -> Color(0xFFE0E0E0)
}

private val ProjectStatus.chipTextColor: Color @Composable get() = when (this) {
    ProjectStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
    ProjectStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
    ProjectStatus.WAITING     -> Color(0xFF856404)
    ProjectStatus.DONE        -> Color(0xFF155724)
    ProjectStatus.PAUSED      -> Color(0xFF616161)
}

@Composable
private fun ProjectStatusChip(status: ProjectStatus, onClick: () -> Unit) {
    Surface(
        color    = status.chipColor,
        shape    = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(status.statusIcon, null, tint = status.chipTextColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text       = status.label,
                style      = MaterialTheme.typography.labelMedium,
                color      = status.chipTextColor,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint               = status.chipTextColor,
                modifier           = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun DeadlineBadge(dueDate: LocalDate, createdAt: LocalDate) {
    val today    = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, dueDate).toInt()
    val elapsed  = ChronoUnit.DAYS.between(createdAt, today).toInt().coerceAtLeast(0)

    val overdue = daysLeft < 0

    val label: String
    val containerColor: Color
    val borderColor: Color
    val textColor: Color

    if (overdue) {
        val n = -daysLeft
        label          = if (n == 1) "1 d overdue" else "$n d overdue"
        containerColor = Color(0xFFDC3545)
        borderColor    = Color(0xFFDC3545)
        textColor      = Color.White
    } else {
        val leftStr    = if (daysLeft == 0) "today" else if (daysLeft == 1) "1 d left" else "$daysLeft d left"
        val elapsedStr = if (elapsed == 1) "1 day elapsed" else "$elapsed days elapsed"
        label          = "$leftStr  |  $elapsedStr"
        containerColor = Color.Transparent
        borderColor    = MaterialTheme.colorScheme.outline
        textColor      = MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color  = containerColor,
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            color      = textColor,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

// ── Reorder sections sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ReorderSectionsSheet(
    sections: List<Section>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val localSections = remember { mutableStateListOf<Section>().also { it.addAll(sections) } }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState     = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        localSections.apply { add(to.index, removeAt(from.index)) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        Text(
            text       = "Reorder sections",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state    = listState,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(localSections, key = { it.id }) { section ->
                ReorderableItem(reorderState, key = section.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "sec_drag")
                    Surface(
                        shadowElevation = elevation,
                        color           = MaterialTheme.colorScheme.background,
                        modifier        = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text     = section.title,
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector         = Icons.Outlined.DragIndicator,
                                contentDescription  = "Drag",
                                tint                = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier            = Modifier
                                    .size(20.dp)
                                    .longPressDraggableHandle(),
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick  = { onConfirm(localSections.map { it.id }) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text("Confirm")
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ── Project edit sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectEditSheet(
    project: com.onlyflows.domain.model.Project,
    statusLog: List<StatusLogEntry> = emptyList(),
    allTags: List<Tag> = emptyList(),
    onSave: (title: String, note: String, colorHex: String, dueDate: LocalDate?, iconEmoji: String) -> Unit,
    onToggleTag: (String) -> Unit = {},
    onArchive: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val presetColors = listOf(
        "#4A90D9", "#EC4899", "#F59E0B", "#10B981",
        "#8B5CF6", "#EF4444", "#6366F1", "#0EA5E9",
    )
    var title       by remember(project.id) { mutableStateOf(project.title) }
    var note        by remember(project.id) { mutableStateOf(project.note) }
    var colorHex    by remember(project.id) { mutableStateOf(project.colorHex) }
    var dueDate     by remember(project.id) { mutableStateOf(project.dueDate) }
    var iconEmoji   by remember(project.id) { mutableStateOf(project.iconEmoji) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            val editTitle = when (project.type) {
                ProjectType.NOTE -> "Edit note list"
                ProjectType.LIST -> "Edit list"
                else             -> "Edit project"
            }
            Text(editTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
            if (project.type == ProjectType.LIST) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = iconEmoji,
                    onValueChange = { new ->
                        val emoji = new.trim()
                        if (emoji.length <= 2) iconEmoji = emoji
                    },
                    label         = { Text("Icon (emoji)") },
                    placeholder   = { Text("e.g. 📋") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("Description") },
                minLines      = 3,
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presetColors.forEach { hex ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .clickable { colorHex = hex }
                            .then(if (colorHex == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, androidx.compose.foundation.shape.CircleShape) else Modifier),
                    ) {}
                }
            }
            Spacer(Modifier.height(12.dp))
            // Deadline row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDatePicker = true }
                    .padding(vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = dueDate?.toString() ?: "No deadline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null }) { Text("Clear") }
                }
            }
            // Tags row
            if (allTags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Label, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tags", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allTags.forEach { tag ->
                        val selected = tag.id in project.tagIds
                        FilterChip(
                            selected = selected,
                            onClick  = { onToggleTag(tag.id) },
                            label    = { Text(tag.name, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Project Info ──────────────────────────────────────────────────
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Text(
                "Project Info",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
            Text(
                "Created:  ${project.createdAt.format(dateFmt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Modified: ${project.updatedAt.format(dateFmt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (statusLog.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    "Status History",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                val use24h = com.onlyflows.ui.theme.LocalUse24hFormat.current
                val logFmt = remember(use24h) { DateTimeFormatter.ofPattern(if (use24h) "MMM d · HH:mm" else "MMM d · hh:mm a") }
                statusLog.forEach { entry ->
                    val statusEmoji = when (entry.status) {
                        com.onlyflows.domain.model.ProjectStatus.NOT_STARTED -> "\u2B1C"
                        com.onlyflows.domain.model.ProjectStatus.IN_PROGRESS -> "\uD83D\uDD35"
                        com.onlyflows.domain.model.ProjectStatus.WAITING     -> "\uD83D\uDFE1"
                        com.onlyflows.domain.model.ProjectStatus.DONE        -> "\u2705"
                        com.onlyflows.domain.model.ProjectStatus.PAUSED      -> "\u23F8"
                    }
                    val statusLabel = when (entry.status) {
                        com.onlyflows.domain.model.ProjectStatus.NOT_STARTED -> "Not Started"
                        com.onlyflows.domain.model.ProjectStatus.IN_PROGRESS -> "In Progress"
                        com.onlyflows.domain.model.ProjectStatus.WAITING     -> "Waiting"
                        com.onlyflows.domain.model.ProjectStatus.DONE        -> "Done"
                        com.onlyflows.domain.model.ProjectStatus.PAUSED      -> "Paused"
                    }
                    Text(
                        "$statusEmoji $statusLabel · ${entry.changedAt.format(logFmt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { onSave(title.trim(), note.trim(), colorHex, dueDate, iconEmoji.trim()) },
                enabled  = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick  = onArchive,
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border   = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Archive Project") }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let {
                it.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── Section menu header ───────────────────────────────────────────────────────

@Composable
private fun SectionMenuHeader(
    section: com.onlyflows.domain.model.Section,
    accentColor: Color,
    isCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded  by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    SectionHeader(
        title            = section.title,
        accentColor      = accentColor,
        showMenu         = true,
        onMenuClick      = { menuExpanded = true },
        isCollapsed      = isCollapsed,
        onToggleCollapse = onToggleCollapse,
        onAddTask        = onAddTask,
        menuContent = {
            DropdownMenu(
                expanded         = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text    = { Text("Rename") },
                    onClick = { menuExpanded = false; showRenameDialog = true },
                )
                DropdownMenuItem(
                    text    = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        },
    )

    if (showRenameDialog) {
        var draft by remember { mutableStateOf(section.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title   = { Text("Rename section") },
            text    = {
                OutlinedTextField(
                    value         = draft,
                    onValueChange = { draft = it },
                    singleLine    = true,
                    label         = { Text("Section name") },
                    modifier      = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = { showRenameDialog = false; if (draft.isNotBlank()) onRename(draft) },
                    enabled  = draft.isNotBlank(),
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProjectDetailPreview() {
    OnlyFlowsTheme {
        ProjectDetailScreen(projectId = "proj_vacation_rome")
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProjectDetailDarkPreview() {
    OnlyFlowsTheme(darkTheme = true) {
        ProjectDetailScreen(projectId = "proj_work")
    }
}

private fun formatProjectDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

// ── Child list cards (LIST-type projects) ─────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChildListCard(
    list: Project,
    count: Int,
    onClick: () -> Unit,
    onSetCover: () -> Unit,
    onRemoveCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val listColor = remember(list.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(list.colorHex)) }.getOrDefault(Color(0xFF4A90D9))
    }
    val cover = remember(list.coverImage) {
        list.coverImage?.let {
            runCatching {
                val bytes = android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(modifier = modifier) {
        Surface(
            shape  = RoundedCornerShape(14.dp),
            color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
        ) {
            Column {
                // Cover — image if set, otherwise tinted block with the list icon/emoji
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                    if (cover != null) {
                        Image(
                            bitmap             = cover,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().background(listColor.copy(alpha = 0.14f)),
                        ) {
                            if (list.iconEmoji.isNotBlank()) {
                                Text(list.iconEmoji, style = MaterialTheme.typography.headlineMedium)
                            } else {
                                Icon(Icons.Outlined.FormatListBulleted, null, tint = listColor, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(
                        text       = list.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f),
                    )
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text  = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text    = { Text(if (list.coverImage == null) "Set cover" else "Change cover") },
                onClick = { showMenu = false; onSetCover() },
            )
            if (list.coverImage != null) {
                DropdownMenuItem(
                    text    = { Text("Remove cover") },
                    onClick = { showMenu = false; onRemoveCover() },
                )
            }
        }
    }
}

@Composable
private fun AddListCard(
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { showDialog = true },
    ) {
        Column {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Add,
                    contentDescription = "Add list",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier           = Modifier.size(36.dp),
                )
            }
            Text(
                text     = "Add list",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New list") },
            text  = {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    singleLine    = true,
                    placeholder   = { Text("List name") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = { onCreate(title.trim()); showDialog = false },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}
