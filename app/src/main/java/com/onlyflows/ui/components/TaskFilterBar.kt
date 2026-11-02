package com.onlyflows.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.TaskPriority

@Composable
fun TaskFilterBar(
    availableTags: List<Tag>,
    activeTagFilter: Set<String>,
    activePriorityFilter: TaskPriority?,
    onToggleTag: (String) -> Unit,
    onSetPriority: (TaskPriority?) -> Unit,
    modifier: Modifier = Modifier,
    /** Finished tasks in the view; the "Completed" chip shows only when there are some. */
    doneCount: Int = 0,
    showDone: Boolean = false,
    onToggleDone: (() -> Unit)? = null,
) {
    var showPriorityMenu by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Box {
            val priorityLabel = when (activePriorityFilter) {
                TaskPriority.HIGH   -> "High"
                TaskPriority.MEDIUM -> "Medium"
                TaskPriority.LOW    -> "Low"
                TaskPriority.NONE   -> "No priority"
                null                -> "Priority"
            }
            FilterChip(
                selected = activePriorityFilter != null,
                onClick  = { showPriorityMenu = true },
                label    = { Text(priorityLabel, style = MaterialTheme.typography.labelSmall) },
                trailingIcon = {
                    Icon(
                        imageVector        = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp),
                    )
                },
            )
            DropdownMenu(
                expanded         = showPriorityMenu,
                onDismissRequest = { showPriorityMenu = false },
            ) {
                DropdownMenuItem(text = { Text("All") },    onClick = { onSetPriority(null);                showPriorityMenu = false })
                DropdownMenuItem(text = { Text("High") },   onClick = { onSetPriority(TaskPriority.HIGH);   showPriorityMenu = false })
                DropdownMenuItem(text = { Text("Medium") }, onClick = { onSetPriority(TaskPriority.MEDIUM); showPriorityMenu = false })
                DropdownMenuItem(text = { Text("Low") },    onClick = { onSetPriority(TaskPriority.LOW);    showPriorityMenu = false })
                DropdownMenuItem(text = { Text("None") },   onClick = { onSetPriority(TaskPriority.NONE);   showPriorityMenu = false })
            }
        }

        if (onToggleDone != null) CompletedChip(doneCount, showDone, onToggleDone)
        // Tags last: Priority and Completed stay on screen however many tags there are
        availableTags.forEach { tag ->
            val selected = tag.id in activeTagFilter
            FilterChip(
                selected = selected,
                onClick  = { onToggleTag(tag.id) },
                label    = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
            )
        }

    }
}

/** "Completed N": shows or hides the finished tasks of a list. Nothing to show → no chip. */
@Composable
fun CompletedChip(count: Int, shown: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    if (count == 0) return
    FilterChip(
        selected    = shown,
        onClick     = onToggle,
        label       = { Text("Completed $count", style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                imageVector        = if (shown) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = null,
                modifier           = Modifier.size(14.dp),
            )
        },
        modifier = modifier,
    )
}

/** Whether a list shows its finished tasks, remembered per [screen] across launches. */
@Composable
fun rememberShowDone(screen: String): MutableState<Boolean> {
    val prefs = LocalContext.current.getSharedPreferences("list_ui", Context.MODE_PRIVATE)
    val state = remember { mutableStateOf(prefs.getBoolean("show_done_$screen", false)) }
    LaunchedEffect(state.value) { prefs.edit().putBoolean("show_done_$screen", state.value).apply() }
    return state
}
