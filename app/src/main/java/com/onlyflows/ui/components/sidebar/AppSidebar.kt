package com.onlyflows.ui.components.sidebar

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onlyflows.domain.model.Folder
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.onlyflows.ui.navigation.Screen
import com.onlyflows.ui.theme.FlowGold
import com.onlyflows.ui.theme.FlowRed
import com.onlyflows.ui.theme.OnlyFlowsTheme

// ── Top-level sidebar entries (folders + root projects) ──────────────────────

private sealed interface TopLevelEntry {
    val key: String
    data class FolderNode(
        val folder: Folder,
        val children: List<ProjectProgress>,
    ) : TopLevelEntry {
        override val key: String get() = "folder_${folder.id}"
    }
    data class ProjectNode(val progress: ProjectProgress) : TopLevelEntry {
        override val key: String get() = "project_${progress.project.id}"
    }
}

private fun List<SidebarEntry>.toTopLevel(): List<TopLevelEntry> {
    val result         = mutableListOf<TopLevelEntry>()
    var currentFolder  = null as SidebarEntry.FolderHeader?
    val currentChildren = mutableListOf<ProjectProgress>()
    forEach { entry ->
        when (entry) {
            is SidebarEntry.FolderHeader -> {
                currentFolder?.let { result += TopLevelEntry.FolderNode(it.folder, currentChildren.toList()) }
                currentFolder = entry
                currentChildren.clear()
            }
            is SidebarEntry.ProjectRow -> {
                if (entry.indented) {
                    currentChildren += entry.progress
                } else {
                    currentFolder?.let { result += TopLevelEntry.FolderNode(it.folder, currentChildren.toList()) }
                    currentFolder = null
                    currentChildren.clear()
                    result += TopLevelEntry.ProjectNode(entry.progress)
                }
            }
        }
    }
    currentFolder?.let { result += TopLevelEntry.FolderNode(it.folder, currentChildren.toList()) }
    return result
}

@Composable
fun AppSidebar(
    navController: NavController,
    onProjectClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNewGroup: () -> Unit = {},
    onAddProjectToFolder: (folderId: String) -> Unit = {},
    onItemSelected: () -> Unit = {},
    onQuickFindClick: () -> Unit = {},
    viewModel: SidebarViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute by navController.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route

    var editingFolder         by remember { mutableStateOf<Folder?>(null) }
    var contextProject        by remember { mutableStateOf<Project?>(null) }
    var showCreateProject     by remember { mutableStateOf(false) }
    var createProjectFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolder      by remember { mutableStateOf(false) }

    // ── Drag-to-reorder state ─────────────────────────────────────────────────
    val localTopLevel = remember { mutableStateListOf<TopLevelEntry>() }
    LaunchedEffect(state.entries) {
        val new = state.entries.toTopLevel()
        if (new != localTopLevel.toList()) {
            localTopLevel.clear()
            localTopLevel.addAll(new)
        }
    }
    var pendingReorder by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val reorderState  = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIdx = localTopLevel.indexOfFirst { it.key == from.key }
        val toIdx   = localTopLevel.indexOfFirst { it.key == to.key }
        if (fromIdx != -1 && toIdx != -1) {
            localTopLevel.add(toIdx, localTopLevel.removeAt(fromIdx))
            pendingReorder = true
        }
    }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && pendingReorder) {
            pendingReorder = false
            viewModel.onEvent(SidebarEvent.ReorderEntries(
                folderIds      = localTopLevel.filterIsInstance<TopLevelEntry.FolderNode>().map { it.folder.id },
                rootProjectIds = localTopLevel.filterIsInstance<TopLevelEntry.ProjectNode>().map { it.progress.project.id },
            ))
        }
    }

    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Quick Find ────────────────────────────────────────────────────────
        QuickFindBar(onClick = onQuickFindClick, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))

        // ── Scrollable content (LazyColumn for drag-to-reorder support) ─────
        LazyColumn(
            state    = lazyListState,
            modifier = Modifier.weight(1f),
        ) {
            item(key = "spacer_top") { Spacer(Modifier.height(4.dp)) }

            // Fixed nav items
            item(key = "nav_inbox")    { NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_inbox),    Color(0xFF1AAAF5), "Inbox",    quietCount = state.inboxCount,   isSelected = route == Screen.Inbox.route)    { navigate(navController, Screen.Inbox.route);    onItemSelected() } }
            item(key = "nav_today")    { NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_today),    Color(0xFFFED101), "Today",    alertCount = state.overdueCount, quietCount = state.todayCount, isSelected = route == Screen.Today.route) { navigate(navController, Screen.Today.route); onItemSelected() } }
            item(key = "nav_upcoming") { NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_upcoming), Color(0xFFF82F6B), "Upcoming", isSelected = route == Screen.Upcoming.route)  { navigate(navController, Screen.Upcoming.route); onItemSelected() } }
            item(key = "nav_anytime")  { NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_anytime),  Color(0xFF39A59A), "Anytime",  isSelected = route == Screen.Anytime.route)   { navigate(navController, Screen.Anytime.route);  onItemSelected() } }
            item(key = "nav_someday")  { NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_someday),  Color(0xFFCEC17F), "Someday",  isSelected = route == Screen.Someday.route)   { navigate(navController, Screen.Someday.route);  onItemSelected() } }
            item(key = "nav_logbook")  { NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_logbook),  Color(0xFF4DBE60), "Logbook",  isSelected = route == Screen.Logbook.route)   { navigate(navController, Screen.Logbook.route);  onItemSelected() } }
            val hidden = state.hiddenNavItems
            if ("nav_projects" !in hidden)    item(key = "nav_projects")    { NavItem(Icons.Outlined.GridView,      Color(0xFF8B5CF6),  "Projects",        isSelected = route == Screen.Projects.route)     { navigate(navController, Screen.Projects.route);     onItemSelected() } }
            if ("nav_goals" !in hidden)       item(key = "nav_goals")       { NavItem(Icons.Outlined.TrackChanges, Color(0xFFFF9500),  "Goals",            isSelected = route == Screen.Goals.route)        { navigate(navController, Screen.Goals.route);        onItemSelected() } }
            if ("nav_rss" !in hidden)         item(key = "nav_rss")         { NavItem(Icons.Outlined.RssFeed,      Color(0xFFFF6B35),  "RSS",              isSelected = route == Screen.Rss.route)          { navigate(navController, Screen.Rss.route);          onItemSelected() } }
            if ("nav_datetracker" !in hidden) item(key = "nav_datetracker") { NavItem(Icons.Outlined.Timer,        Color(0xFFFF6B6B),  "Dates & Comptes",  isSelected = route == Screen.DateTracker.route)  { navigate(navController, Screen.DateTracker.route);  onItemSelected() } }
            if ("nav_trash" !in hidden)       item(key = "nav_trash")       { NavItem(Icons.Outlined.Delete, MaterialTheme.colorScheme.onSurfaceVariant, "Trash", isSelected = route == Screen.Trash.route) { navigate(navController, Screen.Trash.route);        onItemSelected() } }

            // Divider before folders + projects section
            item(key = "nav_divider") {
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )
            }

            // ── Folders + Projects (reorderable) ─────────────────────────────
            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
            items(localTopLevel, key = { it.key }) { entry ->
                ReorderableItem(reorderState, key = entry.key) { _ ->
                    val dragMod = Modifier.longPressDraggableHandle()
                    when (entry) {
                        is TopLevelEntry.FolderNode -> {
                            val expanded = entry.folder.id in state.expandedFolderIds
                            Column {
                                FolderItem(
                                    folder             = entry.folder,
                                    isExpanded         = expanded,
                                    projectCount       = entry.children.size,
                                    onToggle           = { viewModel.onEvent(SidebarEvent.ToggleFolder(entry.folder.id)) },
                                    onLongPress        = { editingFolder = entry.folder },
                                    onAddProject       = { createProjectFolderId = entry.folder.id; showCreateProject = true },
                                    dragHandleModifier = dragMod,
                                )
                                entry.children.forEach { progress ->
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter   = expandVertically() + fadeIn(),
                                        exit    = shrinkVertically() + fadeOut(),
                                    ) {
                                        ProjectItem(
                                            progress    = progress,
                                            indented    = true,
                                            isSelected  = route == Screen.ProjectDetail.createRoute(progress.project.id),
                                            onClick     = { onProjectClick(progress.project.id); onItemSelected() },
                                            onLongPress = { contextProject = progress.project },
                                        )
                                    }
                                }
                            }
                        }
                        is TopLevelEntry.ProjectNode -> {
                            ProjectItem(
                                progress           = entry.progress,
                                indented           = false,
                                isSelected         = route == Screen.ProjectDetail.createRoute(entry.progress.project.id),
                                onClick            = { onProjectClick(entry.progress.project.id); onItemSelected() },
                                onLongPress        = { contextProject = entry.progress.project },
                                dragHandleModifier = dragMod,
                            )
                        }
                    }
                }
            }

            item(key = "new_list") {
                Column {
                    Spacer(Modifier.height(8.dp))
                    NewListButton(onClick = { createProjectFolderId = null; showCreateProject = true })
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (state.archivedItems.isNotEmpty()) {
                item(key = "archived_header") {
                    ArchivedSectionHeader(
                        count    = state.archivedItems.size,
                        expanded = state.showArchivedSection,
                        onToggle = { viewModel.onEvent(SidebarEvent.ToggleArchivedSection) },
                    )
                }
                if (state.showArchivedSection) {
                    items(state.archivedItems, key = { "archived_${it.id}" }) { project ->
                        ArchivedProjectItem(
                            project     = project,
                            onUnarchive = { viewModel.onEvent(SidebarEvent.UnarchiveProject(project.id)) },
                        )
                    }
                    item(key = "archived_bottom_spacer") { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // ── Bottom bar: + New Group (left) + Settings gear (right) ───────────
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCreateFolder = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "New Group",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "New Group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }

    // ── Folder edit sheet ─────────────────────────────────────────────────────
    editingFolder?.let { folder ->
        FolderEditSheet(
            folder    = folder,
            folders   = state.entries.filterIsInstance<SidebarEntry.FolderHeader>().map { it.folder },
            onRename  = { viewModel.onEvent(SidebarEvent.RenameFolder(folder.id, it)) },
            onDelete  = { viewModel.onEvent(SidebarEvent.DeleteFolder(folder.id)) },
            onDismiss = { editingFolder = null },
        )
    }

    // ── Project context menu ──────────────────────────────────────────────────
    contextProject?.let { project ->
        ProjectContextSheet(
            project   = project,
            folders   = state.entries.filterIsInstance<SidebarEntry.FolderHeader>().map { it.folder },
            onMove    = { folderId -> viewModel.onEvent(SidebarEvent.MoveProject(project.id, folderId)) },
            onArchive = { viewModel.onEvent(SidebarEvent.ArchiveProject(project.id)) },
            onDelete  = { viewModel.onEvent(SidebarEvent.DeleteProject(project.id)) },
            onDismiss = { contextProject = null },
        )
    }

    if (showCreateProject) {
        ProjectCreateSheet(
            folderId  = createProjectFolderId,
            folderName = createProjectFolderId?.let { fid ->
                state.entries.filterIsInstance<SidebarEntry.FolderHeader>()
                    .firstOrNull { it.folder.id == fid }?.folder?.title
            },
            onCreate  = { title, colorHex, type ->
                viewModel.onEvent(SidebarEvent.CreateProject(title, colorHex, createProjectFolderId, type))
            },
            onDismiss = { showCreateProject = false },
        )
    }

    if (showCreateFolder) {
        FolderCreateSheet(
            onCreate  = { name -> viewModel.onEvent(SidebarEvent.CreateFolder(name)) },
            onDismiss = { showCreateFolder = false },
        )
    }
}

// ── Quick Find bar ────────────────────────────────────────────────────────────

@Composable
private fun QuickFindBar(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Surface(
        color    = MaterialTheme.colorScheme.surfaceVariant,
        shape    = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("Quick Find", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Nav item ──────────────────────────────────────────────────────────────────

/**
 * @param alertCount  shown as a red pill (overdue / urgent). 0 = hidden.
 * @param quietCount  shown as a plain gray number (total count). 0 = hidden.
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    alertCount: Int = 0,
    quietCount: Int = 0,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = NavItem(
    iconContent = { Icon(icon, null, tint = iconTint, modifier = Modifier.size(15.dp)) },
    iconBg      = iconTint,
    label       = label,
    alertCount  = alertCount,
    quietCount  = quietCount,
    isSelected  = isSelected,
    modifier    = modifier,
    onClick     = onClick,
)

@Composable
private fun NavItem(
    painter: Painter,
    iconTint: Color,
    label: String,
    alertCount: Int = 0,
    quietCount: Int = 0,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = NavItem(
    iconContent = { Icon(painter = painter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp)) },
    iconBg      = iconTint,
    label       = label,
    alertCount  = alertCount,
    quietCount  = quietCount,
    isSelected  = isSelected,
    modifier    = modifier,
    onClick     = onClick,
)

@Composable
private fun NavItem(
    iconContent: @Composable () -> Unit,
    iconBg: Color,
    label: String,
    alertCount: Int = 0,
    quietCount: Int = 0,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconBg.copy(alpha = 0.15f)),
        ) {
            iconContent()
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                         else            MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f),
        )
        if (alertCount > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FlowRed)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text       = alertCount.toString(),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(5.dp))
        }
        if (quietCount > 0) {
            Text(
                text  = quietCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Folder header item ────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderItem(
    folder: Folder,
    isExpanded: Boolean,
    projectCount: Int = 0,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onAddProject: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 1.dp, bottom = 1.dp),
    ) {
        // Clickable area: icon + title (toggle expand/collapse)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                imageVector        = if (isExpanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Column {
                Text(
                    text       = folder.title,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text  = if (projectCount == 1) "1 project" else "$projectCount projects",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        // + button: add project to this folder
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAddProject),
        ) {
            Icon(
                imageVector        = Icons.Outlined.Add,
                contentDescription = "Add project",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(16.dp),
            )
        }
        if (dragHandleModifier != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Outlined.DragIndicator,
                contentDescription = "Drag to reorder",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier           = Modifier.size(16.dp).then(dragHandleModifier),
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

// ── Project item with pie chart ───────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProjectItem(
    progress: ProjectProgress,
    indented: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null,
) {
    val project = progress.project
    val color = remember(project.colorHex) {
        try { Color(android.graphics.Color.parseColor(project.colorHex)) }
        catch (e: Exception) { Color(0xFF4A90D9) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start  = if (indented) 20.dp else 8.dp,
                end    = 8.dp,
                top    = 1.dp,
                bottom = 1.dp,
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        when (project.type) {
            ProjectType.PROJECT -> PieProgressIcon(ratio = progress.ratio, color = color, size = 18)
            ProjectType.LIST    -> Icon(Icons.Outlined.FormatListBulleted, null, tint = color, modifier = Modifier.size(18.dp))
            ProjectType.NOTE    -> Icon(Icons.Outlined.Article, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text     = project.title,
            style    = MaterialTheme.typography.bodyMedium,
            color    = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else            MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (dragHandleModifier != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Outlined.DragIndicator,
                contentDescription = "Drag to reorder",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier           = Modifier.size(14.dp).then(dragHandleModifier),
            )
        }
    }
}

// ── Pie chart (camembert) ─────────────────────────────────────────────────────

@Composable
private fun PieProgressIcon(ratio: Float, color: Color, size: Int, modifier: Modifier = Modifier) {
    val strokePx = size * 0.14f          // stroke plus épais
    Canvas(modifier = modifier.size(size.dp)) {
        val diameter = this.size.minDimension
        val inset    = strokePx / 2f
        drawCircle(color = color, radius = diameter / 2f - inset, style = Stroke(width = strokePx))
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

// ── Archived section ──────────────────────────────────────────────────────────

@Composable
private fun ArchivedSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Archive,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text       = "Archived ($count)",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier   = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).rotate(rotation),
        )
    }
}

@Composable
private fun ArchivedProjectItem(project: Project, onUnarchive: () -> Unit) {
    val color = remember(project.colorHex) {
        try { Color(android.graphics.Color.parseColor(project.colorHex)) }
        catch (e: Exception) { Color(0xFF4A90D9) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 1.dp, bottom = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        when (project.type) {
            ProjectType.PROJECT -> PieProgressIcon(ratio = 0f, color = color, size = 18)
            ProjectType.LIST    -> Icon(Icons.Outlined.FormatListBulleted, null, tint = color, modifier = Modifier.size(18.dp))
            ProjectType.NOTE    -> Icon(Icons.Outlined.Article, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text     = project.title,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onUnarchive, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Outlined.Unarchive,
                contentDescription = "Restore",
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── + New List button ─────────────────────────────────────────────────────────

@Composable
private fun NewListButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Icon(Icons.Outlined.Add, "New List", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text("New List", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Folder edit bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderEditSheet(
    folder: Folder,
    folders: List<Folder>,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name              by remember(folder.id) { mutableStateOf(folder.title) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Edit Area", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = { onRename(name); onDismiss() }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save") }
            Spacer(Modifier.height(8.dp))

            if (!showDeleteConfirm) {
                OutlinedButton(
                    onClick  = { showDeleteConfirm = true },
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete Area") }
            } else {
                Text(
                    "Delete \"${folder.title}\"? Projects inside will become standalone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDeleteConfirm = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick  = { onDelete(); onDismiss() },
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Project context sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectContextSheet(
    project: Project,
    folders: List<Folder>,
    onMove: (String?) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showMoveMenu      by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(
                project.title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            when {
                showDeleteConfirm -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Delete \"${project.title}\" and all its tasks?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showDeleteConfirm = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            Button(
                                onClick  = { onDelete(); onDismiss() },
                                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                            ) { Text("Delete") }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                showMoveMenu -> {
                    ContextAction(icon = Icons.Outlined.FolderOff, label = "No area (standalone)", selected = project.folderId == null, onClick = { onMove(null); onDismiss() })
                    folders.forEach { folder ->
                        ContextAction(icon = Icons.Outlined.FolderOpen, label = folder.title, selected = project.folderId == folder.id, onClick = { onMove(folder.id); onDismiss() })
                    }
                }
                else -> {
                    ContextAction(Icons.Outlined.DriveFileMove, "Move to area…", onClick = { showMoveMenu = true })
                    ContextAction(Icons.Outlined.Archive, "Archive", onClick = { onArchive(); onDismiss() })
                    ContextAction(Icons.Outlined.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error, onClick = { showDeleteConfirm = true })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContextAction(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

// ── Create project sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCreateSheet(
    folderId: String?,
    folderName: String?,
    onCreate: (title: String, colorHex: String, type: ProjectType) -> Unit,
    onDismiss: () -> Unit,
) {
    val presetColors = listOf(
        "#4A90D9", "#EC4899", "#F59E0B", "#10B981",
        "#8B5CF6", "#EF4444", "#6366F1", "#0EA5E9",
    )
    var title    by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(presetColors.first()) }
    var type     by remember { mutableStateOf(ProjectType.PROJECT) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text       = if (folderName != null) "New list in $folderName" else "New list",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            // ── Type picker ───────────────────────────────────────────────────
            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    Triple(ProjectType.PROJECT, Icons.Outlined.Folder,              "Project"),
                    Triple(ProjectType.LIST,    Icons.Outlined.FormatListBulleted,  "List"),
                    Triple(ProjectType.NOTE,    Icons.Outlined.Article,             "Note"),
                ).forEach { (t, icon, label) ->
                    val isSelected = type == t
                    Surface(
                        onClick       = { type = t },
                        shape         = RoundedCornerShape(10.dp),
                        color         = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else            MaterialTheme.colorScheme.surfaceVariant,
                        modifier      = Modifier.weight(1f),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                       else            MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else            MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title") },
                singleLine    = true,
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
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selected = hex }
                            .then(if (selected == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                    ) {}
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { if (title.isNotBlank()) { onCreate(title.trim(), selected, type); onDismiss() } },
                enabled  = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Create folder sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderCreateSheet(
    onCreate: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("New Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { if (name.isNotBlank()) { onCreate(name.trim()); onDismiss() } },
                enabled  = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun navigate(navController: NavController, route: String) {
    navController.navigate(route) {
        launchSingleTop = true
        popUpTo(navController.graph.startDestinationId) { inclusive = false }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 280, heightDp = 780)
@Composable
private fun SidebarPreview() {
    OnlyFlowsTheme { SidebarPreviewContent() }
}

@Preview(showBackground = true, widthDp = 280, heightDp = 780, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SidebarDarkPreview() {
    OnlyFlowsTheme(darkTheme = true) { SidebarPreviewContent() }
}

@Composable
private fun SidebarPreviewContent() {
    val folderPersonal = Folder("f1", "Personal")
    val folderWork     = Folder("f2", "Work")
    val entries: List<SidebarEntry> = listOf(
        SidebarEntry.FolderHeader(folderPersonal),
        SidebarEntry.ProjectRow(ProjectProgress(Project("p1", "Vacation in Rome",    colorHex = "#4A90D9", folderId = "f1"), 3, 8), indented = true),
        SidebarEntry.ProjectRow(ProjectProgress(Project("p2", "Throw Party for Eve", colorHex = "#EC4899", folderId = "f1"), 0, 4), indented = true),
        SidebarEntry.ProjectRow(ProjectProgress(Project("p3", "Run a Marathon",      colorHex = "#F59E0B", folderId = "f1"), 0, 3), indented = true),
        SidebarEntry.FolderHeader(folderWork),
        SidebarEntry.ProjectRow(ProjectProgress(Project("p5", "New Zealand 2024",    colorHex = "#10B981"), 3, 3), indented = false),
    )
    val fakeState = SidebarUiState(inboxCount = 4, todayCount = 8, entries = entries, expandedFolderIds = setOf("f1"))

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        QuickFindBar(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(4.dp))
            NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_inbox),    Color(0xFF1AAAF5), "Inbox",    quietCount = 4,             isSelected = false) {}
            NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_today),    Color(0xFFFED101), "Today",    alertCount = 1, quietCount = 8, isSelected = true)  {}
            NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_upcoming), Color(0xFFF82F6B), "Upcoming",                               isSelected = false) {}
            NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_anytime),  Color(0xFF39A59A), "Anytime",                                isSelected = false) {}
            NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_someday),  Color(0xFFCEC17F), "Someday",                                isSelected = false) {}
            NavItem(painterResource(com.onlyflows.R.drawable.ic_nav_logbook),  Color(0xFF4DBE60), "Logbook",                                isSelected = false) {}
            NavItem(Icons.Outlined.Delete,        Color(0xFF9CA3AF), "Trash",                                       isSelected = false) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            entries.forEach { entry ->
                when (entry) {
                    is SidebarEntry.FolderHeader -> {
                        val previewCount = entries.count {
                            it is SidebarEntry.ProjectRow && it.progress.project.folderId == entry.folder.id
                        }
                        FolderItem(
                            folder        = entry.folder,
                            isExpanded    = entry.folder.id in fakeState.expandedFolderIds,
                            projectCount  = previewCount,
                            onToggle      = {},
                            onLongPress   = {},
                            onAddProject  = {},
                        )
                    }
                    is SidebarEntry.ProjectRow -> if (!entry.indented || entry.progress.project.folderId in fakeState.expandedFolderIds) {
                        ProjectItem(entry.progress, entry.indented, false, {}, {})
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            NewListButton({})
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Group", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}
