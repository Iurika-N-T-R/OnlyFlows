package com.onlyflows.ui.screens.projects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.ProjectStatus
import com.onlyflows.domain.model.ProjectType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ── View mode ─────────────────────────────────────────────────────────────────

private enum class ProjectsViewMode { BOARD, LIST }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: ProjectsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ProjectsViewMode.BOARD) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // View toggle chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = viewMode == ProjectsViewMode.BOARD,
                    onClick  = { viewMode = ProjectsViewMode.BOARD },
                    label    = { Text("Board") },
                )
                FilterChip(
                    selected = viewMode == ProjectsViewMode.LIST,
                    onClick  = { viewMode = ProjectsViewMode.LIST },
                    label    = { Text("List") },
                )
            }

            if (state.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            when (viewMode) {
                ProjectsViewMode.BOARD -> BoardView(state = state, onProjectClick = onProjectClick, onStatusChange = viewModel::moveProject)
                ProjectsViewMode.LIST  -> ListView(state = state, onProjectClick = onProjectClick, onStatusChange = viewModel::moveProject)
            }
        }
    }
}

// ── Board view ────────────────────────────────────────────────────────────────

@Composable
private fun BoardView(
    state: ProjectsUiState,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(ProjectStatus.entries) { status ->
            val cards = state.columns[status] ?: emptyList()
            ProjectColumn(
                status         = status,
                cards          = cards,
                onProjectClick = onProjectClick,
                onStatusChange = onStatusChange,
            )
        }
    }
}

@Composable
private fun ProjectColumn(
    status: ProjectStatus,
    cards: List<ProjectsCard>,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    Column(
        modifier = Modifier.width(240.dp),
    ) {
        // Column header
        Surface(
            color = status.columnHeaderColor,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text  = status.labelWithEmoji,
                    style = MaterialTheme.typography.labelMedium,
                    color = status.columnHeaderContentColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                // Count badge
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

        // Cards
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp),
            ) {
                cards.forEach { card ->
                    ProjectBoardCard(card = card, onProjectClick = onProjectClick, onStatusChange = onStatusChange)
                }
                if (cards.isEmpty()) {
                    Text(
                        text  = "No projects",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    )
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

    Surface(
        onClick   = { onProjectClick(project.id) },
        shape     = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProjectTypeIcon(type = project.type, colorHex = project.colorHex, ratio = progress.ratio, size = 18.dp)
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
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        ProjectStatus.entries.filter { it != project.status }.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.labelWithEmoji) },
                                onClick = { menuExpanded = false; onStatusChange(project.id, status) },
                            )
                        }
                    }
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
            project.dueDate?.let { dueDate ->
                Spacer(Modifier.height(4.dp))
                ProjectDeadlineBadge(dueDate = dueDate)
            }
        }
    }
}

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
private fun ListView(
    state: ProjectsUiState,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        ProjectStatus.entries.forEach { status ->
            val cards = state.columns[status] ?: emptyList()
            item(key = "header_${status.name}") {
                // Group header
                Surface(
                    color = status.columnHeaderColor,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text       = status.labelWithEmoji,
                        style      = MaterialTheme.typography.labelMedium,
                        color      = status.columnHeaderContentColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                HorizontalDivider()
            }
            items(cards, key = { "card_${it.progress.project.id}" }) { card ->
                ProjectListRow(card = card, onProjectClick = onProjectClick, onStatusChange = onStatusChange)
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 56.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                )
            }
        }
    }
}

@Composable
private fun ProjectListRow(
    card: ProjectsCard,
    onProjectClick: (String) -> Unit,
    onStatusChange: (String, ProjectStatus) -> Unit,
) {
    val project  = card.progress.project
    val progress = card.progress
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick  = { onProjectClick(project.id) },
        modifier = Modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            ProjectTypeIcon(type = project.type, colorHex = project.colorHex, ratio = progress.ratio, size = 16.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text     = project.title,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (card.folderName != null) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text     = card.folderName,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
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
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    ProjectStatus.entries.filter { it != project.status }.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.labelWithEmoji) },
                            onClick = { menuExpanded = false; onStatusChange(project.id, status) },
                        )
                    }
                }
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
    modifier: Modifier = Modifier,
) {
    val color = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { Color(0xFF4A90D9) }
    }
    when (type) {
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
private fun ProjectDeadlineBadge(dueDate: LocalDate) {
    val today    = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, dueDate).toInt()
    val overdue  = daysLeft < 0

    Surface(
        color  = if (overdue) Color(0xFFDC3545) else Color.Transparent,
        shape  = RoundedCornerShape(6.dp),
        border = if (!overdue) androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ) else null,
    ) {
        val label = if (overdue) "${-daysLeft} d overdue" else "$daysLeft d left"
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = if (overdue) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ── Status color + label helpers ──────────────────────────────────────────────

private val ProjectStatus.columnHeaderColor: Color
    @Composable get() {
        val dark = isSystemInDarkTheme()
        return when (this) {
            ProjectStatus.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant
            ProjectStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
            ProjectStatus.WAITING     -> if (dark) Color(0xFF4D3A00) else Color(0xFFFFF3CD)
            ProjectStatus.DONE        -> if (dark) Color(0xFF1A3D2A) else Color(0xFFD4EDDA)
        }
    }

private val ProjectStatus.columnHeaderContentColor: Color
    @Composable get() {
        val dark = isSystemInDarkTheme()
        return when (this) {
            ProjectStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
            ProjectStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
            ProjectStatus.WAITING     -> if (dark) Color(0xFFFFE082) else Color(0xFF5D4200)
            ProjectStatus.DONE        -> if (dark) Color(0xFF81C995) else Color(0xFF1B5E35)
        }
    }

private val ProjectStatus.labelWithEmoji: String get() = when (this) {
    ProjectStatus.NOT_STARTED -> "\u2B1C Not Started"
    ProjectStatus.IN_PROGRESS -> "\uD83D\uDD35 In Progress"
    ProjectStatus.WAITING     -> "\uD83D\uDFE1 Waiting"
    ProjectStatus.DONE        -> "\u2705 Done"
}
