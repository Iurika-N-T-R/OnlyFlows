package com.onlyflows.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.screens.search.DateQuery
import com.onlyflows.ui.screens.search.QuickFindViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFindScreen(
    initialQuery: String = "",
    onTaskClick: (String) -> Unit    = {},
    onProjectClick: (String) -> Unit = {},
    onDateClick: (LocalDate) -> Unit = {},
    onBack: () -> Unit               = {},
    viewModel: QuickFindViewModel    = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) viewModel.setQuery(initialQuery)
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                                    "Search tasks, projects…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.query.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    "Search for tasks, projects, or tags",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding(),
                    bottom = 16.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                // ── Date shortcut ─────────────────────────────────────────────
                if (state.dateQuery != null) {
                    item(key = "date_shortcut") {
                        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
                        val fmtShort = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
                        val (dateLabel, subLabel, clickDate) = when (val dq = state.dateQuery) {
                            is DateQuery.Single -> Triple(
                                "📅 ${dq.date.format(fmt)}",
                                "Show all tasks on this date",
                                dq.date,
                            )
                            is DateQuery.Range  -> Triple(
                                "📅 ${dq.start.format(fmtShort)} – ${dq.end.format(fmtShort)} ${dq.end.year}",
                                "Show all tasks in this period",
                                dq.start,
                            )
                            else -> return@item
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDateClick(clickDate) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        ) {
                            Surface(
                                color  = MaterialTheme.colorScheme.primaryContainer,
                                shape  = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = dateLabel,
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
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        HorizontalDivider(
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                        )
                    }
                }

                // ── Date results ──────────────────────────────────────────────
                if (state.dateResults.isNotEmpty()) {
                    item(key = "date_results_header") {
                        Text(
                            "Tâches",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    items(state.dateResults, key = { "date_task_${it.id}" }) { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task.id) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (task.isCompleted)
                                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                        else
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                    ),
                            ) {
                                if (task.isCompleted) {
                                    Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text     = task.title,
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = if (task.isCompleted)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                task.dueDate?.let { date ->
                                    Text(
                                        text  = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 52.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                        )
                    }
                }

                // ── Calendar events ───────────────────────────────────────────
                if (state.eventResults.isNotEmpty()) {
                    item(key = "event_header") {
                        Text(
                            "Events",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    items(state.eventResults, key = { "event_${it.id}" }) { event ->
                        val dotColor = remember(event.colorHex) {
                            runCatching { android.graphics.Color.parseColor(event.colorHex) }
                                .getOrDefault(0xFF4A90D9.toInt())
                                .let { androidx.compose.ui.graphics.Color(it) }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(dotColor),
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
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 35.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                        )
                    }
                }

                // ── Projects ──────────────────────────────────────────────────
                if (state.projectResults.isNotEmpty()) {
                    item(key = "proj_header") {
                        Text(
                            "Projects",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    items(state.projectResults, key = { "proj_${it.id}" }) { project ->
                        val projectColor = remember(project.colorHex) {
                            runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }
                                .getOrDefault(Color(0xFF4A90D9))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProjectClick(project.id) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint     = projectColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text     = project.title,
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 50.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                        )
                    }
                }

                // ── Tasks ─────────────────────────────────────────────────────
                if (state.taskResults.isNotEmpty()) {
                    item(key = "task_header") {
                        Text(
                            "Tasks",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    items(state.taskResults, key = { "task_${it.id}" }) { task ->
                        val flagColor = when (task.priority) {
                            TaskPriority.HIGH   -> Color(0xFFDC3545)
                            TaskPriority.MEDIUM -> Color(0xFFFF8800)
                            TaskPriority.LOW    -> Color(0xFF4A90D9)
                            TaskPriority.NONE   -> null
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task.id) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            // Checkbox
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (task.isCompleted)
                                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                        else
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                    ),
                            ) {
                                if (task.isCompleted) {
                                    Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            // Title + note snippet
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text  = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (task.isCompleted)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                // Note snippet if match is in note
                                val q = state.query
                                val noteSnippet = if (task.note.isNotBlank() && !task.title.contains(q, ignoreCase = true)) {
                                    val idx = task.note.indexOf(q, ignoreCase = true)
                                    if (idx >= 0) "…${task.note.substring(maxOf(0, idx - 15), minOf(task.note.length, idx + 40))}…"
                                    else null
                                } else null
                                if (noteSnippet != null) {
                                    Text(
                                        text     = noteSnippet,
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            // Priority flag
                            if (flagColor != null) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Outlined.Flag,
                                    contentDescription = null,
                                    tint     = flagColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 52.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                        )
                    }
                }

                // ── No results ────────────────────────────────────────────────
                if (state.taskResults.isEmpty() && state.projectResults.isEmpty() && state.dateQuery == null && state.eventResults.isEmpty()) {
                    item(key = "no_results") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                        ) {
                            Text(
                                text  = "No results for \"${state.query}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
