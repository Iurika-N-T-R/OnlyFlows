package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.data.mock.MockData
import com.onlyflows.data.preferences.NotificationPreferences
import com.onlyflows.domain.model.ReminderRecurrence
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.Section
import com.onlyflows.domain.model.Tag
import com.onlyflows.ui.components.suggestFor
import com.onlyflows.ui.screens.task.ReminderPickerSheet
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.theme.FlowGold
import com.onlyflows.ui.theme.OnlyFlowsTheme
import java.time.LocalDate
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    defaultGroup: TaskGroup? = null,
    tags: List<Tag> = emptyList(),
    projects: List<Project> = emptyList(),
    onAdd: (id: String, title: String, note: String, group: TaskGroup?, dueDate: LocalDate?, dueTime: LocalTime?, endTime: LocalTime?, priority: TaskPriority, projectId: String?, sectionId: String?, checklistItems: List<String>, tagIds: List<String>) -> Unit,
    /** Called right after [onAdd] when the user attached a reminder while creating the task. */
    onAddReminderForTask: ((taskId: String, at: LocalDateTime, locationId: String?, recurrence: ReminderRecurrence) -> Unit)? = null,
    onDismiss: () -> Unit,
    onTaskCreated: ((taskId: String) -> Unit)? = null,
) {
    var taskId   by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var title    by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var group    by remember { mutableStateOf(defaultGroup) }
    var dueDate  by remember { mutableStateOf<LocalDate?>(null) }
    var dueTime  by remember { mutableStateOf<LocalTime?>(null) }
    var endTime  by remember { mutableStateOf<LocalTime?>(null) }
    var priority by remember { mutableStateOf(TaskPriority.NONE) }
    var selectedProjectId  by remember { mutableStateOf<String?>(null) }
    var selectedSectionId  by remember { mutableStateOf<String?>(null) }
    var showDatePicker     by remember { mutableStateOf(false) }
    var showTagsPicker     by remember { mutableStateOf(false) }
    var showPriorityMenu   by remember { mutableStateOf(false) }
    var showProjectMenu    by remember { mutableStateOf(false) }
    var showSectionMenu    by remember { mutableStateOf(false) }
    var showChecklist      by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var selectedTagIds     by remember { mutableStateOf(listOf<String>()) }
    var pendingReminder    by remember { mutableStateOf<Triple<LocalDateTime, String?, ReminderRecurrence>?>(null) }
    var checklistItems     by remember { mutableStateOf(listOf<String>()) }
    var checklistDraft     by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val context      = LocalContext.current
    val notifPrefs   = remember { NotificationPreferences(context.applicationContext) }
    val scope        = rememberCoroutineScope()
    val eveningTime  by notifPrefs.thisEveningTime.collectAsState(initial = "18:00")

    fun resetForm() {
        taskId          = UUID.randomUUID().toString()
        title           = ""
        note            = ""
        group           = defaultGroup
        dueDate         = null
        dueTime         = null
        endTime         = null
        pendingReminder = null
        selectedTagIds  = emptyList()
        priority        = TaskPriority.NONE
        checklistItems  = emptyList()
        checklistDraft  = ""
        showChecklist   = false
    }

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
            // Title line — the empty checkbox states what is being written without a label,
            // and the close button gives a way out that does not depend on the scrim.
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.width(12.dp))
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
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onDismiss,
                    shape   = CircleShape,
                    color   = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.padding(4.dp).size(16.dp),
                    )
                }
            }

            // ── Inline #tag completion ───────────────────────────────────────────
            // Typing "#yo" at a word boundary offers the matching tags instead of forcing a
            // trip through the picker, and offers to create the tag when nothing matches.
            val tagToken = remember(title) { trailingTagToken(title) }
            if (tagToken != null) {
                val query       = tagToken.second
                val suggestions = remember(tags, query) { tags.suggestFor(query) }
                val exactExists = tags.any { it.name.equals(query, ignoreCase = true) }

                fun applyTag(tagId: String) {
                    title = title.substring(0, tagToken.first)
                    if (tagId !in selectedTagIds) selectedTagIds = selectedTagIds + tagId
                }

                if (suggestions.isNotEmpty() || query.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        suggestions.forEach { tag ->
                            val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }
                                .getOrDefault(MaterialTheme.colorScheme.primary)
                            SuggestionChip(
                                onClick = { applyTag(tag.id) },
                                label   = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(tagColor),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(tag.name, style = MaterialTheme.typography.labelMedium)
                                        tag.groupName?.let { folder ->
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text  = folder,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        if (query.isNotBlank() && !exactExists) {
                            SuggestionChip(
                                onClick = {
                                    val tag = Tag(id = "tag_${System.currentTimeMillis()}", name = query)
                                    scope.launch {
                                        (context.applicationContext as OnlyFlowsApp)
                                            .container.tagRepository.insert(tag)
                                    }
                                    applyTag(tag.id)
                                },
                                icon  = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp)) },
                                label = { Text("Create \"$query\"", style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                }
            }

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
                modifier   = Modifier.fillMaxWidth().padding(start = 30.dp),
            )

            // ── Checklist items (shown when toggled) ─────────────────────────────
            if (showChecklist) {
                Spacer(Modifier.height(8.dp))
                checklistItems.forEachIndexed { index, item ->
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 30.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 30.dp, top = 7.dp, bottom = 7.dp),
                    ) {
                        // Open circle, same mark the real checklist uses once the task exists
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { checklistItems = checklistItems.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 30.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 30.dp, top = 7.dp, bottom = 2.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
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
            Spacer(Modifier.height(8.dp))

            // ── Chips row ────────────────────────────────────────────────────────
            // Project, date and reminder chips share one scrollable line; the action buttons
            // get the next one. On a single line the last button was clipped once a reminder
            // chip appeared, and splitting the chips apart cost a third line for nothing.
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                if (projects.isNotEmpty()) {
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
                            shape            = MenuShape,
                            containerColor   = MaterialTheme.colorScheme.surface,
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
                                    leadingIcon = { ProjectLeadingIcon(project) },
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
                                shape            = MenuShape,
                                containerColor   = MaterialTheme.colorScheme.surface,
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

                // Selected tags — visible here so they need not be hunted for in the detail
                // screen. Tapping one reopens the picker.
                selectedTagIds.mapNotNull { id -> tags.find { it.id == id } }.forEach { tag ->
                    val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                    FilterChip(
                        selected = true,
                        onClick  = { showTagsPicker = true },
                        label    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tagColor),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tag.name, style = MaterialTheme.typography.labelMedium)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor     = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }

            // ── Footer ───────────────────────────────────────────────────────────
            // Save never scrolls away: the actions take whatever width is left and scroll
            // among themselves, which is what used to clip the last button off the screen.
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                // Date group chip
                FilterChip(
                    selected = true,
                    onClick  = { showDatePicker = true },
                    label    = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (iconRes, chipLabel) = when {
                                group == TaskGroup.TODAY        -> R.drawable.ic_nav_today    to "Today"
                                group == TaskGroup.THIS_EVENING -> R.drawable.ic_nav_evening  to "This Evening"
                                group == TaskGroup.SOMEDAY      -> R.drawable.ic_nav_someday  to "Someday"
                                group == TaskGroup.ANYTIME && dueDate == null ->
                                    R.drawable.ic_nav_anytime to "Anytime"
                                dueDate != null                 -> R.drawable.ic_nav_anytime to
                                    dueDate!!.format(DateTimeFormatter.ofPattern("d MMM"))
                                else                            -> R.drawable.ic_nav_anytime  to "No date"
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


                // Reminder icon — tinted once one is pending, the way the checklist icon works
                IconButton(onClick = { showReminderPicker = true }) {
                    Icon(
                        Icons.Outlined.Notifications,
                        "Reminder",
                        tint = if (pendingReminder != null) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                        shape            = MenuShape,
                        containerColor   = MaterialTheme.colorScheme.surface,
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
                                onAdd(taskId, title.trim(), note.trim(), group, dueDate, dueTime, endTime, priority, selectedProjectId, selectedSectionId, finalItems, selectedTagIds)
                                pendingReminder?.let { (at, locId, rec) ->
                                    onAddReminderForTask?.invoke(taskId, at, locId, rec)
                                }
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
                // Save & add new button
                IconButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            val finalItems = if (checklistDraft.isNotBlank()) checklistItems + checklistDraft.trim() else checklistItems
                            onAdd(taskId, title.trim(), note.trim(), group, dueDate, dueTime, endTime, priority, selectedProjectId, selectedSectionId, finalItems, selectedTagIds)
                                pendingReminder?.let { (at, locId, rec) ->
                                    onAddReminderForTask?.invoke(taskId, at, locId, rec)
                                }
                            resetForm()
                            focusRequester.requestFocus()
                        }
                    },
                    enabled = title.isNotBlank(),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.AddTask,
                        contentDescription = "Save and add new",
                        tint               = if (title.isNotBlank())
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }

                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val finalItems = if (checklistDraft.isNotBlank()) checklistItems + checklistDraft.trim() else checklistItems
                            onAdd(taskId, title.trim(), note.trim(), group, dueDate, dueTime, endTime, priority, selectedProjectId, selectedSectionId, finalItems, selectedTagIds)
                            pendingReminder?.let { (at, locId, rec) ->
                                onAddReminderForTask?.invoke(taskId, at, locId, rec)
                            }
                            onDismiss()
                        }
                    },
                    enabled  = title.isNotBlank(),
                    shape    = CircleShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            initialDate    = dueDate,
            initialDueTime = dueTime,
            initialEndTime = endTime,
            showTimeOption = true,
            onResult       = { picked, pickedDueTime, pickedEndTime ->
                dueDate = picked
                dueTime = pickedDueTime
                endTime = pickedEndTime
                // Clearing the date means "no date", not Someday: Someday is a deliberate
                // choice the user makes in the picker, never a fallback for an empty value.
                group = when {
                    picked == null            -> null
                    picked == LocalDate.now() -> TaskGroup.TODAY
                    else                      -> TaskGroup.ANYTIME
                }
                showDatePicker = false
            },
            onAnytime = {
                group   = TaskGroup.ANYTIME
                dueDate = null
                dueTime = null
                endTime = null
                showDatePicker = false
            },
            onThisEvening = {
                // Without this the shortcut only set today's date, making it identical to
                // "Today". It now carries the group and the configured evening hour.
                group   = TaskGroup.THIS_EVENING
                dueDate = LocalDate.now()
                dueTime = runCatching { LocalTime.parse(eveningTime) }.getOrDefault(LocalTime.of(18, 0))
                endTime = null
                showDatePicker = false
            },
            onAddReminder = {
                // The reminder carries its own absolute date and time, so the task keeps
                // whatever schedule the user chose — forcing a due date here made every
                // reminder silently flip the task to Today.
                showDatePicker     = false
                showReminderPicker = true
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showReminderPicker) {
        ReminderPickerSheet(
            taskDueDate = dueDate,
            onApply     = { at, locId, rec ->
                pendingReminder = Triple(at, locId, rec)
                showReminderPicker = false
            },
            onDismiss   = { showReminderPicker = false },
        )
    }

    if (showTagsPicker) {
        TagsPickerModal(
            allTags        = tags,
            selectedTagIds = selectedTagIds,
            onToggleTag    = { id ->
                selectedTagIds = if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id
            },
            onCreateTag    = { name ->
                scope.launch {
                    val tag = Tag(id = "tag_${System.currentTimeMillis()}", name = name)
                    (context.applicationContext as OnlyFlowsApp).container.tagRepository.insert(tag)
                    selectedTagIds = selectedTagIds + tag.id
                }
            },
            onDeleteTag    = { id ->
                scope.launch {
                    (context.applicationContext as OnlyFlowsApp).container.tagRepository.delete(id)
                    selectedTagIds = selectedTagIds - id
                }
            },
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
            onAdd     = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onDismiss = {},
        )
    }
}

// ── Inline #tag completion ────────────────────────────────────────────────────

/**
 * The `#tag` token the caret is sitting on, as (index of the `#`, text after it), or null when
 * the text does not end in one. Only a `#` at a word boundary counts, so "C#" in a title and
 * "#" inside a URL are left alone.
 */
private val TRAILING_TAG = Regex("(?:^|\\s)#([\\p{L}\\p{N}_-]*)$")

internal fun trailingTagToken(text: String): Pair<Int, String>? {
    val match = TRAILING_TAG.find(text) ?: return null
    val group = match.groups[1] ?: return null
    return (group.range.first - 1) to group.value
}

// ── Menu helpers ──────────────────────────────────────────────────────────────

/** Menus default to CornerExtraSmall (4dp), which reads as square next to the rest of the
 *  sheet. Passing shape and containerColor explicitly also pins the surface away from the
 *  tonal M3 menu container, which rendered lavender against this white sheet. */
private val MenuShape = RoundedCornerShape(14.dp)

@Composable
private fun ProjectLeadingIcon(project: Project) {
    val icon = when (project.type) {
        ProjectType.LIST -> Icons.AutoMirrored.Outlined.FormatListBulleted
        ProjectType.NOTE -> Icons.AutoMirrored.Outlined.Article
        else             -> Icons.Outlined.FolderOpen
    }
    if (project.iconEmoji.isNotBlank()) {
        Text(project.iconEmoji, style = MaterialTheme.typography.bodyMedium)
    } else {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(18.dp),
            tint               = runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }
                .getOrDefault(MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
