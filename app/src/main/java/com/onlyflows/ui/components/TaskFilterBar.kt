package com.onlyflows.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
) {
    var showPriorityMenu by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        availableTags.forEach { tag ->
            val selected = tag.id in activeTagFilter
            FilterChip(
                selected = selected,
                onClick  = { onToggleTag(tag.id) },
                label    = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
            )
        }

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
    }
}
