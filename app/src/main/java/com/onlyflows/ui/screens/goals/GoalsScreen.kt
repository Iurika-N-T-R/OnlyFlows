package com.onlyflows.ui.screens.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.Goal
import com.onlyflows.domain.model.GoalFrequency
import com.onlyflows.domain.model.GoalStatus
import com.onlyflows.domain.model.computeStatus
import com.onlyflows.ui.components.FlowFab
import com.onlyflows.ui.overlays.AddGoalSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onOpenDrawer: () -> Unit = {},
    onGoalClick: (String) -> Unit = {},
    viewModel: GoalsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.showAddGoal) {
        AddGoalSheet(
            onConfirm = { title, frequency, target, color, emoji, deadline, tags ->
                viewModel.onEvent(GoalsEvent.AddGoal(title, frequency, target, color, emoji, deadline, tags))
            },
            onDismiss = { viewModel.onEvent(GoalsEvent.HideAddGoal) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Goals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        if (state.activeGoals.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "${state.activeGoals.size} active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FlowFab(onClick = { viewModel.onEvent(GoalsEvent.ShowAddGoal) })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.allGoals.none { it.deletedAt == null }) {
            GoalsEmptyState(
                modifier = Modifier.padding(padding),
                onNewGoal = { viewModel.onEvent(GoalsEvent.ShowAddGoal) },
                onTemplate = { emoji, title, freq, target, color ->
                    viewModel.onEvent(GoalsEvent.AddGoal(title, freq, target, color, emoji, null, emptyList()))
                },
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        GoalFilterChip(
                            label = "All ${state.activeGoals.size}",
                            selected = state.filter == GoalFilter.ALL,
                            dotColor = null,
                            onClick = { viewModel.onEvent(GoalsEvent.SetFilter(GoalFilter.ALL)) },
                        )
                    }
                    item {
                        GoalFilterChip(
                            label = "On track",
                            selected = state.filter == GoalFilter.ON_TRACK,
                            dotColor = Color(0xFF4DBE60),
                            onClick = { viewModel.onEvent(GoalsEvent.SetFilter(GoalFilter.ON_TRACK)) },
                        )
                    }
                    item {
                        GoalFilterChip(
                            label = "At risk",
                            selected = state.filter == GoalFilter.AT_RISK,
                            dotColor = Color(0xFFFF9500),
                            onClick = { viewModel.onEvent(GoalsEvent.SetFilter(GoalFilter.AT_RISK)) },
                        )
                    }
                    item {
                        GoalFilterChip(
                            label = "Not started",
                            selected = state.filter == GoalFilter.NOT_STARTED,
                            dotColor = MaterialTheme.colorScheme.outline,
                            onClick = { viewModel.onEvent(GoalsEvent.SetFilter(GoalFilter.NOT_STARTED)) },
                        )
                    }
                }

                var completedExpanded by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.filteredActive, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onClick = { onGoalClick(goal.id) },
                        )
                    }

                    // Completed section
                    if (state.completedGoals.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { completedExpanded = !completedExpanded }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (completedExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Completed (${state.completedGoals.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        if (completedExpanded) {
                            items(state.completedGoals, key = { "done_${it.id}" }) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onClick = { onGoalClick(goal.id) },
                                    dimmed = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalFilterChip(
    label: String,
    selected: Boolean,
    dotColor: Color?,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (dotColor != null) {
                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(dotColor))
                }
                Text(label)
            }
        },
    )
}

@Composable
private fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    dimmed: Boolean = false,
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(goal.colorHex)) }.getOrDefault(Color(0xFF4A90D9))
    val progress = if (goal.targetCount > 0) goal.currentCount.toFloat() / goal.targetCount else 0f
    val pct = (progress * 100).roundToInt()
    val status = goal.computeStatus()

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        tonalElevation = if (dimmed) 0.dp else 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: emoji box + title + fraction + pct
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Emoji box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = if (dimmed) 0.4f else 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(goal.emoji, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (dimmed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    if (goal.note.isNotBlank()) {
                        Text(
                            text = goal.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${goal.currentCount}/${goal.targetCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(MaterialTheme.shapes.small),
                color = if (dimmed) MaterialTheme.colorScheme.outline else color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            )

            // Chips row: deadline · tags · status
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                goal.deadline?.let { dl ->
                    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), dl)
                    val dlLabel = buildString {
                        append(dl.format(DateTimeFormatter.ofPattern("MMM d")))
                        if (daysLeft >= 0) append(" · ${daysLeft}d")
                    }
                    GoalChip(text = "📅 $dlLabel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                goal.tags.take(2).forEach { tag ->
                    GoalChip(text = tag, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (!dimmed) {
                    StatusPill(status)
                }
            }
        }
    }
}

@Composable
private fun GoalChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun StatusPill(status: GoalStatus) {
    val (dotColor, label) = when (status) {
        GoalStatus.ON_TRACK -> Color(0xFF4DBE60) to "On track"
        GoalStatus.AT_RISK -> Color(0xFFFF9500) to "At risk"
        GoalStatus.NOT_STARTED -> Color(0xFF8E8E93) to "Not started"
        GoalStatus.COMPLETED -> Color(0xFF4A90D9) to "Completed"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(dotColor)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = dotColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GoalsEmptyState(
    modifier: Modifier = Modifier,
    onNewGoal: () -> Unit,
    onTemplate: (emoji: String, title: String, freq: GoalFrequency, target: Int, color: String) -> Unit,
) {
    val templates = listOf(
        Triple("🚀", "Ship a project", "Milestones & linked tasks · great for work"),
        Triple("📊", "Hit a number", "Numeric target · books, runs, sales, …"),
        Triple("💪", "Build a habit", "Streaks, repeating tasks · great for health"),
    )
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("🎯", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Turn tasks into progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Group related items under a goal to see how everyday work moves you toward what matters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNewGoal, modifier = Modifier.fillMaxWidth(0.6f)) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("New goal")
        }
        Spacer(Modifier.weight(1f))

        // Templates
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                "Start from a template",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            templates.forEach { (emoji, title, subtitle) ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTemplate(emoji, title, GoalFrequency.MONTHLY, 5, "#4A90D9")
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
