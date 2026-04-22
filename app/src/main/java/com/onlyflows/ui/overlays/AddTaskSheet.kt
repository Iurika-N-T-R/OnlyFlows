package com.onlyflows.ui.overlays

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import com.onlyflows.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.data.mock.MockData
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.theme.FlowGold
import com.onlyflows.ui.theme.OnlyFlowsTheme
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    defaultGroup: TaskGroup = TaskGroup.TODAY,
    tags: List<Tag> = emptyList(),
    projects: List<Project> = emptyList(),
    onAdd: (id: String, title: String, group: TaskGroup, dueDate: LocalDate?, priority: TaskPriority, projectId: String?, sectionId: String?, checklistItems: List<String>) -> Unit,
    onDismiss: () -> Unit,
    onTaskCreated: ((taskId: String) -> Unit)? = null,
) {
    val taskId = remember { UUID.randomUUID().toString() }
    var title    by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var group    by remember { mutableStateOf(defaultGroup) }
    var dueDate  by remember { mutableStateOf<LocalDate?>(null) }
    var priority by remember { mutableStateOf(TaskPriority.NONE) }
    var selectedProjectId  by remember { mutableStateOf<String?>(null) }
    var selectedSectionId  by remember { mutableStateOf<String?>(null) }
    var showDatePicker     by remember { mutableStateOf(false) }
    var showTagsPicker     by remember { mutableStateOf(false) }
    var showPriorityMenu   by remember { mutableStateOf(false) }
    var showProjectMenu    by remember { mutableStateOf(false) }
    var showSectionMenu    by remember { mutableStateOf(false) }
    var showChecklist      by remember { mutableStateOf(false) }
    var checklistItems     by remember { mutableStateOf(listOf<String>()) }
    var checklistDraft     by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val selectedProject: Project? = projects.find { it.id == selectedProjectId }
    val selectedSection: Section? = selectedProject?.sections?.find { it.id == selectedSectionId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle       = null,
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Title input
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            "New To-Do",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                },
                singleLine = false,
                modifier   = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Spacer(Modifier.height(6.dp))

            // Note input
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (note.isEmpty()) {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                singleLine = false,
                modifier   = Modifier.fillMaxWidth(),
            )

            // ── Checklist items (shown when toggled) ─────────────────────────────
            if (showChecklist) {
                Spacer(Modifier.height(8.dp))
                checklistItems.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Icon(Icons.Outlined.CheckBoxOutlineBlank, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { checklistItems = checklistItems.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = checklistDraft,
                        onValueChange = { checklistDraft = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (checklistDraft.isNotBlank()) {
                                checklistItems = checklistItems + checklistDraft.trim()
                                checklistDraft = ""
                            }
                        }),
                        decorationBox = { inner ->
                            if (checklistDraft.isEmpty()) Text("Add item…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            inner()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Project / section picker row (only when projects are provided) ──
            if (projects.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Project chip
                    Box {
                        FilterChip(
                            selected = selectedProjectId != null,
                            onClick  = { showProjectMenu = true },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedProjectId == null) {
                                        Icon(painter = painterResource(R.drawable.ic_nav_inbox), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(text = selectedProject?.title ?: "Inbox", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            leadingIcon = if (selectedProjectId != null) ({
                                Icon(
                                    Icons.Outlined.FolderOpen,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                )
                            }) else null,
                            trailingIcon = if (selectedProjectId != null) ({
                                IconButton(
                                    onClick  = { selectedProjectId = null; selectedSectionId = null },
                                    modifier = Modifier.size(16.dp),
                                ) {
                                    Icon(Icons.Outlined.Close, null, modifier = Modifier.size(12.dp))
                                }
                            }) else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                        DropdownMenu(
                            expanded         = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false },
                        ) {
                            DropdownMenuItem(
                                text    = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(painter = painterResource(R.drawable.ic_nav_inbox), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Inbox") } },
                                onClick = {
                                    selectedProjectId = null
                                    selectedSectionId = null
                                    showProjectMenu   = false
                                },
                            )
                            HorizontalDivider()
                            projects.forEach { project ->
                                DropdownMenuItem(
                                    text    = { Text(project.title) },
                                    onClick = {
                                        selectedProjectId = project.id
                                        selectedSectionId = null
                                        showProjectMenu   = false
                                    },
                                )
                            }
                        }
                    }

                    // Section chip (only if selected project has sections)
                    val projectSections = selectedProject?.sections?.sortedBy { it.sortOrder } ?: emptyList()
                    if (projectSections.isNotEmpty()) {
                        Box {
                            FilterChip(
                                selected = selectedSectionId != null,
                                onClick  = { showSectionMenu = true },
                                label    = {
                                    Text(
                                        text  = selectedSection?.title ?: "Section…",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor     = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                            DropdownMenu(
                                expanded         = showSectionMenu,
                                onDismissRequest = { showSectionMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text    = { Text("No section") },
                                    onClick = { selectedSectionId = null; showSectionMenu = false },
                                )
                                projectSections.forEach { section ->
                                    DropdownMenuItem(
                                        text    = { Text(section.title) },
                                        onClick = { selectedSectionId = section.id; showSectionMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Group chip + action buttons row ──────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Date group chip
                FilterChip(
                    selected = true,
                    onClick  = { showDatePicker = true },
                    label    = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (iconRes, chipLabel) = when (group) {
                                TaskGroup.TODAY        -> R.drawable.ic_nav_today    to "Today"
                                TaskGroup.THIS_EVENING -> R.drawable.ic_nav_evening  to "This Evening"
                                else                   -> R.drawable.ic_nav_someday  to "Someday"
                            }
                            Icon(painter = painterResource(iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = chipLabel, style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor     = MaterialTheme.colorScheme.onSurface,
                    ),
                )

                Spacer(Modifier.weight(1f))

                // Tag icon
                IconButton(onClick = { showTagsPicker = true }) {
                    Icon(Icons.Outlined.Label, "Tags", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Checklist icon
                IconButton(onClick = { showChecklist = !showChecklist }) {
                    Icon(Icons.Outlined.ChecklistRtl, "Checklist", tint = if (showChecklist || checklistItems.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Priority flag
                Box {
                    IconButton(onClick = { showPriorityMenu = true }) {
                        Icon(
                            imageVector = if (priority != TaskPriority.NONE) Icons.Filled.Flag else Icons.Outlined.Flag,
                            contentDescription = "Priority",
                            tint = when (priority) {
                                TaskPriority.HIGH   -> Color(0xFFDC3545)
                                TaskPriority.MEDIUM -> Color(0xFFFF8800)
                                TaskPriority.LOW    -> Color(0xFF4A90D9)
                                TaskPriority.NONE   -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    DropdownMenu(
                        expanded         = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false },
                    ) {
                        listOf(
                            TaskPriority.HIGH   to "🔴 High",
                            TaskPriority.MEDIUM to "🟠 Medium",
                            TaskPriority.LOW    to "🔵 Low",
                            TaskPriority.NONE   to "⬜ None",
                        ).forEach { (p, label) ->
                            DropdownMenuItem(
                                text    = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                onClick = { priority = p; showPriorityMenu = false },
                            )
                        }
                    }
                }
                // Open in full view button
                if (onTaskCreated != null) {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val finalItems = if (checklistDraft.isNotBlank()) checklistItems + checklistDraft.trim() else checklistItems
                                onAdd(taskId, title.trim(), group, dueDate, priority, selectedProjectId, selectedSectionId, finalItems)
                                onTaskCreated(taskId)
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.OpenInFull,
                            contentDescription = "Save and open",
                            tint               = if (title.isNotBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }
                }
                // Submit button
                IconButton(
                    onClick  = {
                        if (title.isNotBlank()) {
                            val finalItems = if (checklistDraft.isNotBlank()) checklistItems + checklistDraft.trim() else checklistItems
                            onAdd(taskId, title.trim(), group, dueDate, priority, selectedProjectId, selectedSectionId, finalItems)
                            onDismiss()
                        }
                    },
                    enabled  = title.isNotBlank(),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Send,
                        contentDescription = "Add task",
                        tint               = if (title.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            initialDate    = dueDate,
            onDateSelected = { picked ->
                dueDate = picked
                group = when {
                    picked == null              -> TaskGroup.SOMEDAY
                    picked == java.time.LocalDate.now() -> TaskGroup.TODAY
                    else                        -> TaskGroup.SOMEDAY
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTagsPicker) {
        TagsPickerModal(
            allTags        = tags,
            selectedTagIds = emptyList(),
            onToggleTag    = {},
            onDismiss      = { showTagsPicker = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddTaskSheetPreview() {
    OnlyFlowsTheme {
        AddTaskSheet(
            tags      = MockData.tags,
            onAdd     = { _, _, _, _, _, _, _, _ -> },
            onDismiss = {},
        )
    }
}
