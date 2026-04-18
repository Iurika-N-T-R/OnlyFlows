package com.onlyflows.ui.screens.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.onlyflows.domain.model.GoalLog
import com.onlyflows.domain.model.GoalStatus
import com.onlyflows.domain.model.computeStatus
import com.onlyflows.ui.overlays.AddGoalLogSheet
import com.onlyflows.ui.overlays.AddGoalSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {},
    viewModel: GoalDetailViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goal = state.goal ?: return

    if (state.showAddLog) {
        AddGoalLogSheet(
            onConfirm = { label, note, tags ->
                viewModel.onEvent(GoalDetailEvent.AddLog(label, note, tags))
            },
            onDismiss = { viewModel.onEvent(GoalDetailEvent.HideAddLog) },
        )
    }

    if (state.showEditGoal) {
        AddGoalSheet(
            onConfirm = { title, frequency, target, color, emoji, deadline, tags ->
                viewModel.onEvent(GoalDetailEvent.UpdateGoal(title, frequency, target, color, emoji, deadline, tags))
            },
            onDismiss = { viewModel.onEvent(GoalDetailEvent.HideEditGoal) },
            initialTitle       = goal.title,
            initialEmoji       = goal.emoji,
            initialFrequency   = goal.frequency,
            initialTargetCount = goal.targetCount,
            initialColorHex    = goal.colorHex,
            initialDeadline    = goal.deadline,
            initialTags        = goal.tags,
            confirmLabel       = "Save Changes",
        )
    }

    val accentColor = runCatching { Color(android.graphics.Color.parseColor(goal.colorHex)) }.getOrDefault(Color(0xFF4A90D9))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit goal") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(GoalDetailEvent.ShowEditGoal)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete goal", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(GoalDetailEvent.DeleteGoal)
                                    onDeleted()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(GoalDetailEvent.ShowAddLog) },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add log", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        // Collect all unique tags from log entries for the filter bar
        val logTags = remember(state.logs) {
            state.logs.filter { it.deletedAt == null }.flatMap { it.tags }.distinct().sorted()
        }
        var selectedLogTag by remember { mutableStateOf<String?>(null) }
        val filteredLogs = remember(state.logs, selectedLogTag) {
            val active = state.logs.filter { it.deletedAt == null }
            if (selectedLogTag == null) active else active.filter { selectedLogTag in it.tags }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { GoalHeaderCard(goal = goal, accentColor = accentColor) }

            // Goal's own tags (category labels)
            if (goal.tags.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        goal.tags.forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }

            // Stats row
            item { GoalStatsRow(goal = goal, logs = state.logs, accentColor = accentColor) }

            // Sparkline
            if (state.logs.isNotEmpty()) {
                item { GoalSparkline(logs = state.logs, accentColor = accentColor) }
            }

            // Logged section
            if (state.logs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Logged", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accentColor)
                        Text(
                            "${filteredLogs.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Tag filter chips for log entries
                if (logTags.isNotEmpty()) {
                    item(key = "log_tag_filter") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedLogTag == null,
                                    onClick  = { selectedLogTag = null },
                                    label    = { Text("All") },
                                )
                            }
                            items(logTags) { tag ->
                                FilterChip(
                                    selected = selectedLogTag == tag,
                                    onClick  = { selectedLogTag = if (selectedLogTag == tag) null else tag },
                                    label    = { Text(tag) },
                                )
                            }
                        }
                    }
                }

                items(filteredLogs, key = { it.id }) { log ->
                    GoalLogItem(
                        log = log,
                        onToggleStar = { viewModel.onEvent(GoalDetailEvent.ToggleStar(log.id, !log.isStarred)) },
                    )
                }
            }

            // Comment input
            item {
                OutlinedTextField(
                    value = state.commentDraft,
                    onValueChange = { viewModel.onEvent(GoalDetailEvent.SetComment(it)) },
                    placeholder = { Text("💬 Add a comment…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun GoalHeaderCard(goal: Goal, accentColor: Color) {
    val progress = if (goal.targetCount > 0) goal.currentCount.toFloat() / goal.targetCount else 0f
    val status = goal.computeStatus()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(goal.emoji, fontSize = 28.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (goal.note.isNotBlank()) {
                        Text(
                            goal.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                Text(
                    "${goal.currentCount}/${goal.targetCount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                goal.deadline?.let { dl ->
                    Text(
                        "📅 ${dl.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val (dotColor, statusLabel) = when (status) {
                    GoalStatus.ON_TRACK -> accentColor to "On pace"
                    GoalStatus.AT_RISK -> Color(0xFFFF9500) to "At risk"
                    GoalStatus.NOT_STARTED -> Color(0xFF8E8E93) to "Not started"
                    GoalStatus.COMPLETED -> Color(0xFF4DBE60) to "Completed"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = dotColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun GoalStatsRow(goal: Goal, logs: List<GoalLog>, accentColor: Color) {
    val monthsElapsed = run {
        val months = ChronoUnit.MONTHS.between(goal.createdAt.toLocalDate(), LocalDate.now()).toInt()
        max(1, months)
    }
    val pace = goal.currentCount.toFloat() / monthsElapsed

    // Streak: count of distinct months with at least one log entry
    val streak = logs
        .filter { it.deletedAt == null }
        .map { it.loggedAt.month to it.loggedAt.year }
        .toSet()
        .size

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            label = "PACE",
            value = "${"%.1f".format(pace)}/mo",
            detail = "↑ on track",
            accentColor = accentColor,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "STREAK",
            value = "$streak month${if (streak != 1) "s" else ""}",
            detail = if (streak >= 3) "🔥 nice" else "keep going",
            accentColor = accentColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    detail: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = accentColor)
        }
    }
}

@Composable
private fun GoalSparkline(logs: List<GoalLog>, accentColor: Color) {
    val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    // Build cumulative count per month (Jan=1..Dec=12) for this year
    val monthLabels = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    val currentMonth = LocalDate.now().monthValue

    val countByMonth = logs
        .filter { it.deletedAt == null && it.loggedAt.year == year }
        .groupBy { it.loggedAt.monthValue }
        .mapValues { it.value.size }

    val months = (1..currentMonth).toList()
    val points = months.map { m -> countByMonth[m] ?: 0 }
    val maxVal = points.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f

    val lineColor = accentColor
    val trackColor = accentColor.copy(alpha = 0.15f)
    val dotColor = accentColor
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("Entries per month", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(year.toString(), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }

            if (points.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    val padTop = 8f
                    val padBottom = 16f
                    val chartH = h - padTop - padBottom
                    val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w

                    fun xOf(i: Int) = i * stepX
                    fun yOf(v: Int) = padTop + chartH * (1f - v / maxVal)

                    // Filled area under curve
                    val fillPath = Path().apply {
                        moveTo(xOf(0), yOf(points[0]))
                        for (i in 1 until points.size) {
                            val cx = (xOf(i - 1) + xOf(i)) / 2f
                            cubicTo(cx, yOf(points[i - 1]), cx, yOf(points[i]), xOf(i), yOf(points[i]))
                        }
                        lineTo(xOf(points.lastIndex), h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(fillPath, color = trackColor)

                    // Line
                    val linePath = Path().apply {
                        moveTo(xOf(0), yOf(points[0]))
                        for (i in 1 until points.size) {
                            val cx = (xOf(i - 1) + xOf(i)) / 2f
                            cubicTo(cx, yOf(points[i - 1]), cx, yOf(points[i]), xOf(i), yOf(points[i]))
                        }
                    }
                    drawPath(linePath, color = lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Dots on each point
                    for (i in points.indices) {
                        drawCircle(color = dotColor, radius = 4f, center = Offset(xOf(i), yOf(points[i])))
                        drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 2f, center = Offset(xOf(i), yOf(points[i])))
                    }
                }

                // Month labels
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    months.forEach { m ->
                        Text(
                            monthLabels[m - 1],
                            style    = MaterialTheme.typography.labelSmall,
                            color    = labelColor,
                            fontSize = 9.sp,
                        )
                    }
                }
            } else {
                Text(
                    "Log your first entry to see progress here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor,
                )
            }
        }
    }
}

@Composable
private fun GoalLogItem(log: GoalLog, onToggleStar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4DBE60)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                log.label,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (log.note.isNotBlank()) {
                    Text(
                        log.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val dateStr = log.loggedAt.format(DateTimeFormatter.ofPattern("MMM d"))
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                log.tags.firstOrNull()?.let { tag ->
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
            Icon(
                if (log.isStarred) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star",
                tint = if (log.isStarred) Color(0xFFFF9500) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
