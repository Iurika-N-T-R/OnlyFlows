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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.onlyflows.domain.smartadd.NaturalLanguage
import com.onlyflows.domain.smartadd.Batch
import com.onlyflows.domain.smartadd.Interpretation
import com.onlyflows.domain.smartadd.TaskDraft
import com.onlyflows.data.smartadd.ContentWriter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
    /** A date picked by hand wins over one read from the title. */
    var dateTouched        by remember { mutableStateOf(false) }
    /** The user asked to keep the title as typed: nothing is read out of it. */
    var nlOff              by remember { mutableStateOf(false) }
    /** Answers to the questions a sentence raised ("Which list?"), by line. */
    val choices            = remember { mutableStateMapOf<Int, Int>() }
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
        dateTouched     = false
        nlOff           = false
        choices.clear()
    }

    // ── Natural language, read as you type ("Call John tomorrow 9am !!" → date, time, priority) ──
    val (parsed, hits) = remember(title, nlOff) { if (nlOff) TaskDraft(title.trim()) to emptyList() else NaturalLanguage.highlight(title) }
    val nl         = hits.isNotEmpty()
    val nlDate     = nl && !dateTouched && parsed.dueDate != null
    val effDate    = if (nlDate) parsed.dueDate else dueDate
    val effTime    = if (nlDate) parsed.dueTime else dueTime
    val effGroup   = if (nlDate) (if (parsed.dueDate == LocalDate.now()) TaskGroup.TODAY else TaskGroup.ANYTIME) else group
    val effPriority = if (priority == TaskPriority.NONE && nl) parsed.priority else priority
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    /** Saves what is in the sheet and returns the new task's id, or null when there is no title. */
    fun submit(): String? {
        if (title.isBlank()) return null
        val id         = taskId
        val finalItems = if (checklistDraft.isNotBlank()) checklistItems + checklistDraft.trim() else checklistItems
        val finalTitle = if (nl) parsed.title else title.trim()
        val finalTags  = selectedTagIds + if (nl) parsed.tagNames.mapNotNull { n -> tags.find { it.name.equals(n, true) }?.id }.filter { it !in selectedTagIds } else emptyList()
        onAdd(id, finalTitle, note.trim(), effGroup, effDate, effTime, endTime, effPriority, selectedProjectId, selectedSectionId, finalItems, finalTags)
        (pendingReminder ?: parsed.reminderAt?.takeIf { nl }?.let { Triple(it, null, ReminderRecurrence.NONE) })
            ?.let { (at, locId, rec) -> onAddReminderForTask?.invoke(id, at, locId, rec) }
        if (nl) parsed.recurrence?.let { rule ->
            val repo = (context.applicationContext as OnlyFlowsApp).container.taskRepository
            // Same wait as ReminderCreator: callers insert the task asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                withTimeoutOrNull(5_000) { repo.observeById(id).filterNotNull().first() }
                    ?.let { repo.update(it.copy(recurrence = rule, updatedAt = LocalDateTime.now())) }
            }
        }
        return id
    }

    // ── Whole sentences: several lines, "Add milk, eggs to my shopping list", "Create a project
    // for my website", "Note: …", "Remind me to …". When the text is more than one plain task,
    // the sheet shows what will be created instead of the date/project chips.
    val allProjects by (context.applicationContext as OnlyFlowsApp).container.projectRepository
        .observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val smart: List<Interpretation>? = remember(title, allProjects, nlOff) {
        if (nlOff) return@remember null
        val lines = title.lines().map { it.trim() }.filter { it.isNotEmpty() }
        lines.map { NaturalLanguage.interpret(it, allProjects) }
            .takeIf { r -> r.size > 1 || r.any { it !is Interpretation.Ready || it.batch.containers.isNotEmpty() } }
    }
    LaunchedEffect(smart?.size) { choices.clear() }
    /** Everything to create, or null while a question is unanswered. */
    val smartBatch: Batch? = smart?.let { results ->
        var b = Batch()
        results.forEachIndexed { i, r ->
            when (r) {
                is Interpretation.Ready   -> b += r.batch
                is Interpretation.Ask     -> b += r.options[choices[i] ?: return@let null].second
                is Interpretation.Invalid -> Unit
            }
        }
        b.takeUnless { it.isEmpty }
    }
    fun submitSmart(): Boolean {
        val b = smartBatch ?: return false
        val appContext = context.applicationContext
        // Outlives the sheet, which closes right away
        CoroutineScope(Dispatchers.Main).launch {
            val sum = ContentWriter(appContext).write(b)
            Toast.makeText(appContext, if (sum.lines.size == 1) sum.lines[0] else sum.text, Toast.LENGTH_SHORT).show()
        }
        return true
    }
    val canSave = if (smart != null) smartBatch != null else title.isNotBlank()

    val dictation = rememberDictation { heard -> title = if (title.isBlank()) heard else title.trimEnd() + " " + heard }

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
                    visualTransformation = {
                        val styled = buildAnnotatedString {
                            append(it.text)
                            hits.forEach { r -> if (r.last < it.text.length) addStyle(SpanStyle(background = highlightColor), r.first, r.last + 1) }
                        }
                        TransformedText(styled, OffsetMapping.Identity)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions(onDone = {
                        val saved = if (smart != null) submitSmart() else submit() != null
                        if (saved) { resetForm(); focusRequester.requestFocus() }
                    }),
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
                // Plain clickable, not Surface(onClick): that one pads itself to a 48dp touch
                // target, which pushed the ✕ below the first line and left a gap under the title.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onDismiss),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(16.dp),
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

            // Note input — a batch of lists and projects has nowhere to put one
            if (smart == null) BasicTextField(
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
            if (smart != null) SmartPreview(smart, choices, onKeepAsText = { nlOff = true })
            else Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                // Date chip — shows what the title says ("tomorrow 9am", "every Monday") as you type
                FilterChip(
                    selected = true,
                    onClick  = { showDatePicker = true },
                    label    = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val today = LocalDate.now()
                            val (iconRes, dayLabel) = when {
                                effGroup == TaskGroup.THIS_EVENING -> R.drawable.ic_nav_evening to "This Evening"
                                effDate == today                   -> R.drawable.ic_nav_today   to "Today"
                                effDate == today.plusDays(1)       -> R.drawable.ic_nav_anytime to "Tomorrow"
                                effDate != null                    -> R.drawable.ic_nav_anytime to effDate.format(DateTimeFormatter.ofPattern("EEE d MMM"))
                                effGroup == TaskGroup.TODAY        -> R.drawable.ic_nav_today   to "Today"
                                effGroup == TaskGroup.SOMEDAY      -> R.drawable.ic_nav_someday to "Someday"
                                effGroup == TaskGroup.ANYTIME      -> R.drawable.ic_nav_anytime to "Anytime"
                                else                               -> R.drawable.ic_nav_anytime to "No date"
                            }
                            val timeLabel = effTime?.takeIf { effGroup != TaskGroup.THIS_EVENING }?.let { " " + it.format(DateTimeFormatter.ofPattern("HH:mm")) }.orEmpty()
                            Icon(painter = painterResource(iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = dayLabel + timeLabel, style = MaterialTheme.typography.labelMedium)
                            if (nl && parsed.recurrence != null) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Outlined.Repeat, contentDescription = "Repeats", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    // Read wrong ("Today's report")? ✕ keeps the words in the title
                    trailingIcon = if (nlDate) ({
                        Icon(
                            Icons.Outlined.Close, contentDescription = "Keep as text",
                            modifier = Modifier.size(14.dp).clickable { nlOff = true },
                        )
                    }) else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (nlDate) highlightColor else MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor     = MaterialTheme.colorScheme.onSurface,
                    ),
                )

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
                            imageVector = if (effPriority != TaskPriority.NONE) Icons.Filled.Flag else Icons.Outlined.Flag,
                            contentDescription = "Priority",
                            tint = when (effPriority) {
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
                // Voice typing, on-device only — hidden when the phone has no offline model
                if (dictation.available) {
                    IconButton(onClick = dictation.toggle) {
                        Icon(
                            if (dictation.listening) Icons.Filled.Mic else Icons.Outlined.Mic,
                            contentDescription = "Voice typing",
                            tint = if (dictation.listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Open in full view button
                if (onTaskCreated != null) {
                    IconButton(
                        onClick = { submit()?.let { onTaskCreated(it); onDismiss() } },
                        enabled = title.isNotBlank() && smart == null,
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.OpenInFull,
                            contentDescription = "Save and open",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (title.isNotBlank() && smart == null) 1f else 0.3f),
                        )
                    }
                }
            }

                Spacer(Modifier.width(8.dp))
                // Enter on the keyboard saves and keeps the sheet open for the next one
                FilledIconButton(
                    onClick  = { if (if (smart != null) submitSmart() else submit() != null) onDismiss() },
                    enabled  = canSave,
                    shape    = CircleShape,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Save", modifier = Modifier.size(20.dp))
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
                dateTouched = true
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
                dateTouched = true
                group   = TaskGroup.ANYTIME
                dueDate = null
                dueTime = null
                endTime = null
                showDatePicker = false
            },
            onThisEvening = {
                // Without this the shortcut only set today's date, making it identical to
                // "Today". It now carries the group and the configured evening hour.
                dateTouched = true
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
            taskDueDate = effDate,
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
        com.onlyflows.ui.components.ProjectGlyph(
            value = project.iconEmoji,
            tint  = runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }
                .getOrDefault(MaterialTheme.colorScheme.onSurfaceVariant),
        )
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

// ── Smart add preview ─────────────────────────────────────────────────────────

/** One short line per thing the text will create; a question as chips when a line is ambiguous. */
@Composable
private fun SmartPreview(results: List<Interpretation>, choices: MutableMap<Int, Int>, onKeepAsText: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        results.forEachIndexed { i, r ->
            when (r) {
                is Interpretation.Ready   -> BatchLines(r.batch)
                is Interpretation.Invalid -> Text(r.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                is Interpretation.Ask     -> {
                    Text(r.question, style = MaterialTheme.typography.bodySmall, color = muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        r.options.forEachIndexed { j, (label, _) ->
                            FilterChip(selected = choices[i] == j, onClick = { choices[i] = j }, label = { Text(label, style = MaterialTheme.typography.labelMedium) })
                        }
                    }
                    choices[i]?.let { BatchLines(r.options[it].second) }
                }
            }
        }
        Text(
            "Keep as plain text", style = MaterialTheme.typography.labelSmall, color = muted.copy(alpha = 0.7f),
            modifier = Modifier.clickable(onClick = onKeepAsText).padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun BatchLines(batch: Batch) {
    val style = MaterialTheme.typography.bodyMedium
    batch.containers.forEach { c ->
        val icon = when (c.type) { ProjectType.LIST -> "☑️"; ProjectType.NOTE -> "📝"; ProjectType.PROJECT -> "📁" }
        val items = (c.tasks.map { it.title } + c.notes.map { it.title })
        Text("$icon ${c.title}" + if (items.isEmpty()) "" else " · " + items.joinToString(", "), style = style, maxLines = 2)
    }
    batch.tasks.forEach { t -> Text("☐ ${t.title}" + draftWhen(t).let { if (it.isEmpty()) "" else " · $it" }, style = style, maxLines = 1) }
}

private fun draftWhen(t: TaskDraft): String {
    val today = LocalDate.now()
    val day = t.dueDate?.let { d -> when (d) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM")) } }
    return listOfNotNull(
        day?.plus(t.dueTime?.let { " " + it.format(DateTimeFormatter.ofPattern("HH:mm")) }.orEmpty()),
        "🔁".takeIf { t.recurrence != null },
        "🔔".takeIf { t.reminderAt != null },
        "⚑".takeIf { t.priority != TaskPriority.NONE },
    ).joinToString(" ")
}
