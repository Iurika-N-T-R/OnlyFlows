package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.screens.search.DateQuery
import com.onlyflows.ui.screens.search.QuickFindViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun QuickFindPopup(
    onDismiss: () -> Unit,
    onTaskClick: (String) -> Unit    = {},
    onProjectClick: (String) -> Unit = {},
    onShowMore: (String) -> Unit     = {},
    onDateClick: (LocalDate) -> Unit = {},
    viewModel: QuickFindViewModel    = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        viewModel.setQuery("")
        focusRequester.requestFocus()
    }

    // Read status-bar height directly from the View — avoids Xiaomi/HyperOS inset mis-reporting
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarTopDp = remember(view) {
        val px = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        with(density) { px.toDp() }
    }
    // Guarantee at least 24dp so the card never slides under the status bar
    val topPaddingDp = max(statusBarTopDp, 24.dp)

    // Scrim — fills the screen; tap outside the card to dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onDismiss() },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPaddingDp, start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape           = RoundedCornerShape(16.dp),
            shadowElevation = 16.dp,
            color           = MaterialTheme.colorScheme.surface,
            modifier        = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { /* consume — prevent scrim dismiss */ },
        ) {
                Column {
                    // ── Search field ──────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value         = state.query,
                            onValueChange = { viewModel.setQuery(it) },
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine  = true,
                            decorationBox = { inner ->
                                if (state.query.isEmpty()) {
                                    Text(
                                        "Search tasks, projects, tags…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                        )
                        Spacer(Modifier.width(4.dp))
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick  = { viewModel.setQuery("") },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Clear,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            TextButton(
                                onClick          = onDismiss,
                                contentPadding   = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(
                                    "Cancel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                    )

                    // ── Body ──────────────────────────────────────────────────
                    if (state.query.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Find tasks, projects and tags",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Type \"5 Feb\" to search by date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                        }
                    } else {
                        val hasResults = state.projectResults.isNotEmpty()
                            || state.tagResults.isNotEmpty()
                            || state.taskResults.isNotEmpty()
                            || state.dateResults.isNotEmpty()
                            || state.eventResults.isNotEmpty()
                            || state.dateQuery != null

                        if (!hasResults) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                            ) {
                                Text(
                                    "No results for \"${state.query}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 440.dp),
                            ) {
                                // ── Date shortcut (shown first when date is detected) ──
                                if (state.dateQuery != null) {
                                    item(key = "date_shortcut") {
                                        DateShortcutRow(
                                            dateQuery = state.dateQuery!!,
                                            onClick   = {
                                                val clickDate = when (val dq = state.dateQuery) {
                                                    is DateQuery.Single -> dq.date
                                                    is DateQuery.Range  -> dq.start
                                                    else                -> return@DateShortcutRow
                                                }
                                                onDateClick(clickDate); onDismiss()
                                            },
                                        )
                                    }
                                    item(key = "date_shortcut_divider") { GroupDivider() }
                                }

                                // Projects
                                if (state.projectResults.isNotEmpty()) {
                                    item(key = "proj_label") { SectionLabel("Projects") }
                                    items(state.projectResults, key = { "proj_${it.id}" }) { project ->
                                        ProjectRow(project) { onProjectClick(project.id); onDismiss() }
                                    }
                                    if (state.hasMoreProjects) {
                                        item(key = "proj_more") { ShowMoreRow { onShowMore(state.query) } }
                                    }
                                    item(key = "proj_divider") { GroupDivider() }
                                }

                                // Tags
                                if (state.tagResults.isNotEmpty()) {
                                    item(key = "tag_label") { SectionLabel("Tags") }
                                    items(state.tagResults, key = { "tag_${it.id}" }) { tag ->
                                        TagRow(tag) {
                                            onShowMore(tag.name)
                                        }
                                    }
                                    item(key = "tag_divider") { GroupDivider() }
                                }

                                // Tasks
                                if (state.taskResults.isNotEmpty()) {
                                    item(key = "task_label") { SectionLabel("Tasks") }
                                    items(state.taskResults, key = { "task_${it.id}" }) { task ->
                                        TaskRow(task) { onTaskClick(task.id); onDismiss() }
                                    }
                                    if (state.hasMoreTasks) {
                                        item(key = "task_more") { ShowMoreRow { onShowMore(state.query) } }
                                    }
                                }

                                // Date results (tasks)
                                if (state.dateResults.isNotEmpty() && state.dateQuery != null) {
                                    item(key = "date_divider") { GroupDivider() }
                                    item(key = "date_label") {
                                        val fmt   = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
                                        val label = when (val dq = state.dateQuery) {
                                            is DateQuery.Single -> "On ${dq.date.format(fmt)}"
                                            is DateQuery.Range  -> "${dq.start.format(fmt)} – ${dq.end.format(fmt)}"
                                            else                -> "Date"
                                        }
                                        SectionLabel(label)
                                    }
                                    items(state.dateResults, key = { "date_${it.id}" }) { task ->
                                        TaskRow(task) { onTaskClick(task.id); onDismiss() }
                                    }
                                }

                                // Calendar events
                                if (state.eventResults.isNotEmpty()) {
                                    item(key = "event_divider") { GroupDivider() }
                                    item(key = "event_label") { SectionLabel("Events") }
                                    items(state.eventResults, key = { "event_${it.id}" }) { event ->
                                        EventRow(event)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

// ── Date shortcut row ─────────────────────────────────────────────────────────

@Composable
private fun DateShortcutRow(dateQuery: DateQuery, onClick: () -> Unit) {
    val fmtFull  = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    val fmtShort = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val label = when (dateQuery) {
        is DateQuery.Single -> dateQuery.date.format(fmtFull)
        is DateQuery.Range  -> "${dateQuery.start.format(fmtShort)} – ${dateQuery.end.format(fmtShort)} ${dateQuery.end.year}"
    }
    val subLabel = when (dateQuery) {
        is DateQuery.Single -> "Show all tasks on this date"
        is DateQuery.Range  -> "Show all tasks in this period"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Surface(
            color  = MaterialTheme.colorScheme.primaryContainer,
            shape  = RoundedCornerShape(8.dp),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "📅 $label",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── Shared sub-composables ─────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text.uppercase(),
        style      = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp,
        modifier  = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ShowMoreRow(onClick: () -> Unit) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            "Show all",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ProjectRow(project: Project, onClick: () -> Unit) {
    val dotColor = remember(project.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }
            .getOrDefault(Color(0xFF4A90D9))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = dotColor.copy(alpha = 0.18f),
                modifier = Modifier.size(18.dp),
            ) {}
            Icon(
                Icons.Outlined.List,
                contentDescription = null,
                tint     = dotColor,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text     = project.title,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TagRow(tag: Tag, onClick: () -> Unit) {
    val tagColor = remember(tag.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }
            .getOrDefault(Color(0xFF8B5CF6))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Text(
                "#",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = tagColor,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text  = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            color = tagColor,
        )
    }
}

@Composable
private fun TaskRow(task: Task, onClick: () -> Unit) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH   -> Color(0xFFDC3545)
        TaskPriority.MEDIUM -> Color(0xFFFF8800)
        TaskPriority.LOW    -> Color(0xFF4A90D9)
        TaskPriority.NONE   -> null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(
                if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isCompleted)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text     = task.title,
            style    = MaterialTheme.typography.bodyMedium,
            color    = if (task.isCompleted)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (priorityColor != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Outlined.Flag,
                contentDescription = null,
                tint     = priorityColor,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun EventRow(event: AgendaEvent) {
    val dotColor = remember(event.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(event.colorHex)) }
            .getOrDefault(Color(0xFF4A90D9))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .background(dotColor, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = event.title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.time != null) {
                Text(
                    text  = event.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
