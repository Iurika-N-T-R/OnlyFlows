package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyflows.domain.model.Goal
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalPickerSheet(
    allGoals: List<Goal>,
    selectedGoalIds: List<String>,
    onDismiss: () -> Unit,
    onAttach: (List<String>) -> Unit,
    onNavigateToGoals: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var activeTag by remember { mutableStateOf<String?>(null) }
    var selected by remember(selectedGoalIds) { mutableStateOf(selectedGoalIds.toSet()) }

    val allTags = remember(allGoals) {
        allGoals.flatMap { it.tags }.distinct().sorted()
    }

    val filtered = remember(allGoals, query, activeTag) {
        allGoals
            .filter { g -> query.isBlank() || g.title.contains(query, ignoreCase = true) }
            .filter { g -> activeTag == null || activeTag in g.tags }
    }

    val deadlineFmt = remember { DateTimeFormatter.ofPattern("MMM d") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            // Title + subtitle
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text       = "Attach to a goal",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "Select one or more goals this task contributes to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Search bar
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = {
                    Text("Search goals...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                leadingIcon   = {
                    Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor   = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor          = MaterialTheme.colorScheme.primary,
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Filter chips
            if (allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        GoalFilterChip(
                            label    = "All · ${allGoals.size}",
                            selected = activeTag == null,
                            onClick  = { activeTag = null },
                        )
                    }
                    items(allTags) { tag ->
                        val count = allGoals.count { tag in it.tags }
                        GoalFilterChip(
                            label    = "$tag · $count",
                            selected = activeTag == tag,
                            onClick  = { activeTag = if (activeTag == tag) null else tag },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Goal list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
            ) {
                items(filtered, key = { it.id }) { goal ->
                    val isChecked = goal.id in selected
                    GoalPickerRow(
                        goal        = goal,
                        isChecked   = isChecked,
                        deadlineFmt = deadlineFmt,
                        onToggle    = {
                            selected = if (isChecked) selected - goal.id else selected + goal.id
                        },
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToGoals() }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = "Create a new goal...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Bottom buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick  = { onAttach(selected.toList()) },
                    enabled  = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (selected.isEmpty()) "Attach" else "Attach (${selected.size})")
                }
            }
        }
    }
}

@Composable
private fun GoalFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick      = onClick,
        shape        = RoundedCornerShape(20.dp),
        color        = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun GoalPickerRow(
    goal: Goal,
    isChecked: Boolean,
    deadlineFmt: DateTimeFormatter,
    onToggle: () -> Unit,
) {
    val pct = if (goal.targetCount > 0) {
        (goal.currentCount.toFloat() / goal.targetCount * 100).roundToInt()
    } else 0
    val barColor = runCatching { Color(android.graphics.Color.parseColor(goal.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        // Emoji icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(barColor.copy(alpha = 0.18f)),
        ) {
            Text(goal.emoji, fontSize = 18.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = goal.title,
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = "${goal.currentCount}/${goal.targetCount} · $pct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Progress bar
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (pct / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor),
                    )
                }
                goal.deadline?.let { d ->
                    Text(
                        text  = d.format(deadlineFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Checkbox(
            checked         = isChecked,
            onCheckedChange = { onToggle() },
            colors          = CheckboxDefaults.colors(
                checkedColor   = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}
