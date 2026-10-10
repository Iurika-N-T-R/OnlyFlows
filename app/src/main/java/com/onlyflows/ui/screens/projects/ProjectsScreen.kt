@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.onlyflows.ui.screens.projects

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.ui.components.statusIcon
import com.onlyflows.ui.components.statusLabel
import com.onlyflows.ui.components.statusShortLabel
import com.onlyflows.ui.components.statusTint
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ── View mode ─────────────────────────────────────────────────────────────────

private enum class ProjectsViewMode { BOARD, LIST }

// ── Drag & drop helpers ───────────────────────────────────────────────────────

private const val DND_LABEL = "projectId"

private fun Modifier.projectDragSource(
    projectId: String,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
): Modifier =
    this.dragAndDropSource {
        detectTapGestures(onLongPress = {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            startTransfer(DragAndDropTransferData(ClipData.newPlainText(DND_LABEL, projectId)))
        })
    }

private fun DragAndDropEvent.draggedProjectId(): String? =
    runCatching { toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString() }.getOrNull()

private fun acceptsProjectDrag(event: DragAndDropEvent): Boolean =
    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProjectsScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: ProjectsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Persisted view state — null while loading to avoid flashing the wrong view
    val savedViewMode  by viewModel.viewMode.collectAsStateWithLifecycle()
    val savedBoardPage by viewModel.boardPage.collectAsStateWithLifecycle()
    val collapsedNames by viewModel.collapsedStatuses.collectAsStateWithLifecycle()
    val viewMode = savedViewMode?.let { runCatching { ProjectsViewMode.valueOf(it) }.getOrDefault(ProjectsViewMode.BOARD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Open drawer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                StatusStatsRow(state = state)

                if (state.isLoading || viewMode == null || savedBoardPage == null) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                } else when (viewMode) {
                    ProjectsViewMode.BOARD -> BoardView(
                        state          = state,
                        initialPage    = savedBoardPage!!,
                        onPageChanged  = viewModel::setBoardPage,
                        onProjectClick = onProjectClick,
                        onStatusChange = viewModel::moveProject,
                    )
                    ProjectsViewMode.LIST -> ListView(
                        state             = state,
                        collapsedNames    = collapsedNames,
                        onToggleCollapsed = viewModel::toggleCollapsed,
                        onProjectClick    = onProjectClick,
                        onStatusChange    = viewModel::moveProject,
                    )
                }
            }

            // Floating Board/List switcher
            if (viewMode != null) {
                ViewModeSwitcher(
                    mode     = viewMode,
                    onChange = { viewModel.setViewMode(it.name) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                )
            }
        }
    }
}

// ── Stats row (counts per status, single row) ────────────────────────────────

@Composable
private fun StatusStatsRow(state: ProjectsUiState) {
    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        shape  = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            ProjectStatus.entries.forEach { status ->
                val count = state.columns[status]?.size ?: 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(status.statusIcon, null, tint = status.statusTint, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = "$count",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text     = status.statusShortLabel,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ── Floating view switcher ────────────────────────────────────────────────────

@Composable
private fun ViewModeSwitcher(
    mode: ProjectsViewMode,
    onChange: (ProjectsViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape           = CircleShape,
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation  = 3.dp,
        modifier        = modifier,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            @Composable
            fun segment(target: ProjectsViewMode, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
                val selected = mode == target
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onChange(target) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text       = label,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            segment(ProjectsViewMode.BOARD, Icons.Outlined.ViewKanban, "Board")
            segment(ProjectsViewMode.LIST, Icons.AutoMirrored.Outlined.ViewList, "List")
        }
    }
}

// ── Board view — centered pager, neighbours peeking ──────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BoardView(
    state: ProjectsUiState,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    val statuses   = ProjectStatus.entries
    val pagerState = rememberPagerState(
        initialPage = initialPage.takeIf { it in statuses.indices } ?: statuses.indexOf(ProjectStatus.IN_PROGRESS),
    ) { statuses.size }

    // Persist the active column
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }

    HorizontalPager(
        state             = pagerState,
        contentPadding    = PaddingValues(start = 36.dp, end = 36.dp, top = 4.dp, bottom = 8.dp),
        pageSpacing       = 12.dp,
        verticalAlignment = Alignment.Top,
        modifier          = Modifier.fillMaxSize(),
    ) { page ->
        val status = statuses[page]
        ProjectColumn(
            status         = status,
            // children (nested lists/groups) live inside their parent, not on the board
            cards          = (state.columns[status] ?: emptyList()).filter { it.progress.project.parentProjectId == null },
            onProjectClick = onProjectClick,
            onStatusChange = onStatusChange,
            modifier       = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun ProjectColumn(
    status: ProjectStatus,
    cards: List<ProjectsCard>,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragHover by remember { mutableStateOf(false) }
    val dropTarget = remember(status) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { dragHover = true }
            override fun onExited(event: DragAndDropEvent)  { dragHover = false }
            override fun onEnded(event: DragAndDropEvent)   { dragHover = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragHover = false
                val id = event.draggedProjectId() ?: return false
                onStatusChange(id, status)
                return true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (dragHover) Modifier.border(2.dp, status.statusTint, RoundedCornerShape(12.dp))
                else Modifier
            )
            .dragAndDropTarget(shouldStartDragAndDrop = ::acceptsProjectDrag, target = dropTarget),
    ) {
        // Column header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(status.statusIcon, null, tint = status.statusTint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = status.statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text     = "${cards.size}",
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        // Cards — scrollable, full column height
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(8.dp),
                modifier            = Modifier.fillMaxSize(),
            ) {
                items(cards, key = { it.progress.project.id }) { card ->
                    ProjectBoardCard(card = card, onProjectClick = onProjectClick, onStatusChange = onStatusChange)
                }
                if (cards.isEmpty()) {
                    item {
                        Text(
                            text  = "No projects — drop a card here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectBoardCard(
    card: ProjectsCard,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    val project  = card.progress.project
    val progress = card.progress
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        onClick   = { onProjectClick(project.id) },
        shape     = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier  = Modifier
            .fillMaxWidth()
            .projectDragSource(project.id, haptics),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProjectTypeIcon(type = project.type, colorHex = project.colorHex, ratio = progress.ratio, size = 18.dp, iconEmoji = project.iconEmoji)
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = project.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 2,
                )
                // Count badge
                if (progress.totalCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            text     = "${progress.completedCount}/${progress.totalCount}",
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
                // Status move menu
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Move", modifier = Modifier.size(16.dp))
                    }
                    StatusMoveMenu(
                        expanded  = menuExpanded,
                        current   = project.status,
                        onDismiss = { menuExpanded = false },
                        onSelect  = { onStatusChange(project.id, it) },
                    )
                }
            }
            if (card.folderName != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = card.folderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            if (project.dueDate != null) {
                ProjectDeadlineBadge(
                    dueDate     = project.dueDate!!,
                    completedAt = project.completedAt?.toLocalDate()
                        ?.takeIf { project.status == ProjectStatus.DONE },
                )
            } else {
                Text(
                    text  = "No deadline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun StatusMoveMenu(
    expanded: Boolean,
    current: ProjectStatus,
    onDismiss: () -> Unit,
    onSelect: (ProjectStatus) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        ProjectStatus.entries.filter { it != current }.forEach { status ->
            DropdownMenuItem(
                text        = { Text(status.statusLabel) },
                leadingIcon = { Icon(status.statusIcon, null, tint = status.statusTint, modifier = Modifier.size(18.dp)) },
                onClick     = { onDismiss(); onSelect(status) },
            )
        }
    }
}

// ── List view — collapsible status sections ───────────────────────────────────

@Composable
private fun ListView(
    state: ProjectsUiState,
    collapsedNames: Set<String>,
    onToggleCollapsed: (ProjectStatus) -> Unit,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    // Children (nested lists/groups) render under their parent, never as top-level rows
    val allCards = state.columns.values.flatten()
    val childrenByParent = allCards
        .filter { it.progress.project.parentProjectId != null }
        .groupBy { it.progress.project.parentProjectId!! }

    LazyColumn(
        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.fillMaxSize(),
    ) {
        ProjectStatus.entries.forEach { status ->
            val cards = (state.columns[status] ?: emptyList())
                .filter { it.progress.project.parentProjectId == null }
            item(key = "section_${status.name}") {
                StatusSectionCard(
                    status         = status,
                    cards          = cards,
                    childrenByParent = childrenByParent,
                    isCollapsed    = status.name in collapsedNames,
                    onToggle       = { onToggleCollapsed(status) },
                    onProjectClick = onProjectClick,
                    onStatusChange = onStatusChange,
                )
            }
        }
    }
}

/** One status group = one card: container + title row, then the project rows inside. */
@Composable
private fun StatusSectionCard(
    status: ProjectStatus,
    cards: List<ProjectsCard>,
    childrenByParent: Map<String, List<ProjectsCard>>,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    var dragHover by remember { mutableStateOf(false) }
    val dropTarget = remember(status) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { dragHover = true }
            override fun onExited(event: DragAndDropEvent)  { dragHover = false }
            override fun onEnded(event: DragAndDropEvent)   { dragHover = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragHover = false
                val id = event.draggedProjectId() ?: return false
                onStatusChange(id, status)
                return true
            }
        }
    }

    Surface(
        shape           = RoundedCornerShape(16.dp),
        color           = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp,
        border          = if (dragHover) BorderStroke(2.dp, status.statusTint) else null,
        modifier = Modifier
            .fillMaxWidth()
            .dragAndDropTarget(shouldStartDragAndDrop = ::acceptsProjectDrag, target = dropTarget),
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(status.statusIcon, null, tint = status.statusTint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = status.statusLabel,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text     = "${cards.size}",
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector        = Icons.Outlined.ExpandMore,
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(20.dp).rotate(if (isCollapsed) -90f else 0f),
                )
            }

            if (!isCollapsed && cards.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))

                @Composable
                fun projectWithChildren(card: ProjectsCard, level: Int) {
                    ProjectListRow(card = card, onProjectClick = onProjectClick, onStatusChange = onStatusChange, indentLevel = level)
                    childrenByParent[card.progress.project.id]?.forEach { child ->
                        projectWithChildren(child, level + 1)
                    }
                }

                val byFolder   = cards.groupBy { it.folderName }
                byFolder[null].orEmpty().forEach { card -> projectWithChildren(card, 0) }
                byFolder.entries.filter { it.key != null }.sortedBy { it.key }.forEach { (folder, folderCards) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Folder, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text  = folder!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    folderCards.forEach { card -> projectWithChildren(card, 1) }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ProjectListRow(
    card: ProjectsCard,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
    indentLevel: Int = 0,
) {
    val project  = card.progress.project
    val progress = card.progress
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        onClick  = { onProjectClick(project.id) },
        modifier = Modifier
            .fillMaxWidth()
            .projectDragSource(project.id, haptics),
        color    = Color.Transparent,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (12 + indentLevel * 20).dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            ProjectTypeIcon(type = project.type, colorHex = project.colorHex, ratio = progress.ratio, size = 16.dp, iconEmoji = project.iconEmoji)
            Spacer(Modifier.width(10.dp))
            Text(
                text     = project.title,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (progress.totalCount > 0) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(5.dp),
                ) {
                    Text(
                        text     = "${progress.completedCount}/${progress.totalCount}",
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Move", modifier = Modifier.size(18.dp))
                }
                StatusMoveMenu(
                    expanded  = menuExpanded,
                    current   = project.status,
                    onDismiss = { menuExpanded = false },
                    onSelect  = { onStatusChange(project.id, it) },
                )
            }
        }
    }
}

// ── Type icon (pie for PROJECT, list/note icon for others) ───────────────────

@Composable
private fun ProjectTypeIcon(
    type: ProjectType,
    colorHex: String,
    ratio: Float,
    size: androidx.compose.ui.unit.Dp,
    iconEmoji: String = "",
    modifier: Modifier = Modifier,
) {
    val color = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { Color(0xFF4A90D9) }
    }
    if (type == ProjectType.LIST && iconEmoji.isNotBlank()) {
        Text(iconEmoji, style = MaterialTheme.typography.bodyMedium, modifier = modifier.size(size))
    } else when (type) {
        ProjectType.PROJECT -> ProjectPieIcon(ratio = ratio, colorHex = colorHex, size = size, modifier = modifier)
        ProjectType.LIST    -> Icon(Icons.Outlined.FormatListBulleted, null, tint = color, modifier = modifier.size(size))
        ProjectType.NOTE    -> Icon(Icons.Outlined.Article, null, tint = color, modifier = modifier.size(size))
    }
}

// ── Pie icon ──────────────────────────────────────────────────────────────────

@Composable
private fun ProjectPieIcon(
    ratio: Float,
    colorHex: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val color = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { Color(0xFF4A90D9) }
    }
    val strokePx = 2.8f
    Canvas(modifier = modifier.size(size)) {
        val diameter = this.size.minDimension
        val inset    = strokePx / 2f
        drawCircle(
            color  = color,
            radius = diameter / 2f - inset,
            style  = Stroke(width = strokePx),
        )
        if (ratio > 0f) {
            val innerPad = strokePx * 2.8f
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

// ── Deadline badge ────────────────────────────────────────────────────────────

@Composable
private fun ProjectDeadlineBadge(dueDate: LocalDate, completedAt: LocalDate? = null) {
    val today    = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, dueDate).toInt()
    // A finished project is never overdue: the clock stopped the day it was marked done.
    val overdue  = completedAt == null && daysLeft < 0

    val filled = overdue || completedAt != null
    Surface(
        color  = when {
            completedAt != null -> Color(0xFF2E9E5B)
            overdue             -> Color(0xFFDC3545)
            else                -> Color.Transparent
        },
        shape  = RoundedCornerShape(6.dp),
        border = if (!filled) androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ) else null,
    ) {
        val label = when {
            completedAt != null -> "Done ${completedAt.format(cardDoneFmt)}"
            overdue             -> "${-daysLeft} d overdue"
            else                -> "$daysLeft d left"
        }
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = if (filled) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private val cardDoneFmt: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")
