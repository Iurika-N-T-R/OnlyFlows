package com.onlyflows.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.heightIn
import com.onlyflows.domain.model.TaskPriority
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.Attachment
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.RecurrenceBasis
import com.onlyflows.domain.model.RecurrenceRule
import com.onlyflows.domain.model.RecurrenceType
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.ui.components.TagChip
import com.onlyflows.ui.overlays.DatePickerModal
import com.onlyflows.ui.overlays.SmartMoveSheet
import com.onlyflows.ui.overlays.TagsPickerModal
import com.onlyflows.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit = {},
    onDeleted: () -> Unit = {},
    onConverted: (String) -> Unit = {},
    onNavigateToProject: (String) -> Unit = {},
    viewModel: TaskDetailViewModel = viewModel(),
) {
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
        viewModel.loadTimer(taskId)
        viewModel.loadComments(taskId)
        viewModel.loadReminders(taskId)
        viewModel.loadAttachments(taskId)
        viewModel.loadProjects()
    }

    val state       by viewModel.uiState.collectAsStateWithLifecycle()
    val timerState  by viewModel.timerState.collectAsStateWithLifecycle()
    val task  = state.task ?: return

    LaunchedEffect(state.convertedProjectId) {
        val projId = state.convertedProjectId ?: return@LaunchedEffect
        onConverted(projId)
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        var fileName = uri.lastPathSegment ?: "fichier"
        var fileSize = 0L
        cursor?.use {
            val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (it.moveToFirst()) {
                if (nameIdx >= 0) fileName = it.getString(nameIdx) ?: fileName
                if (sizeIdx >= 0) fileSize = it.getLong(sizeIdx)
            }
        }
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        viewModel.onEvent(TaskDetailEvent.AddAttachment(
            uri      = uri.toString(),
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // More options
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded         = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Convert to Project") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.onEvent(TaskDetailEvent.ConvertToProject)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (timerState.activeEntry != null) "Pause Timer" else "Start Timer") },
                                onClick = {
                                    showMoreMenu = false
                                    if (timerState.activeEntry != null)
                                        viewModel.onEvent(TaskDetailEvent.PauseTimer)
                                    else
                                        viewModel.onEvent(TaskDetailEvent.StartTimer)
                                },
                                leadingIcon = {
                                    Icon(
                                        if (timerState.activeEntry != null) Icons.Outlined.Pause else Icons.Outlined.Timer,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                    // Done / collapse
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ExpandCircleDown,
                            contentDescription = "Done",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        // Bottom toolbar
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    // Delete
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.Delete); onDeleted() }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Star / flag
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ToggleStar) }) {
                        Icon(
                            imageVector = if (task.isStarred) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star",
                            tint = if (task.isStarred) FlowGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Priority
                    val flagTint = when (task.priority) {
                        TaskPriority.HIGH   -> Color(0xFFDC3545)
                        TaskPriority.MEDIUM -> Color(0xFFFF8800)
                        TaskPriority.LOW    -> Color(0xFF4A90D9)
                        TaskPriority.NONE   -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ShowPriorityPicker) }) {
                        Icon(
                            if (task.priority != TaskPriority.NONE) Icons.Filled.Flag else Icons.Outlined.Flag,
                            contentDescription = "Priority",
                            tint = flagTint,
                        )
                    }
                    // Tags
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ShowTagsPicker) }) {
                        Icon(
                            Icons.Outlined.Label,
                            contentDescription = "Tags",
                            tint = if (task.tagIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Date
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ShowDatePicker) }) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = "Deadline",
                            tint = if (task.dueDate != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Reminder bell
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ShowReminderPicker) }) {
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            contentDescription = "Reminder",
                            tint = if (state.reminders.isNotEmpty()) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Recurrence
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ShowRecurrencePicker) }) {
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = "Recurrence",
                            tint = if (task.recurrence != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // When
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.ShowWhenPicker) }) {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = "Schedule",
                            tint = if (task.scheduledGroup != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {

            // ── Checkbox + Title ─────────────────────────────────────────────
            item(key = "title") {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    // Checkbox
                    val shape = RoundedCornerShape(5.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(24.dp)
                            .clip(shape)
                            .then(
                                if (task.isCompleted)
                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                else
                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                            )
                            .clickable { viewModel.onEvent(TaskDetailEvent.ToggleComplete) },
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                Icons.Outlined.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    // Editable title
                    var titleText by remember(task.id) { mutableStateOf(task.title) }
                    BasicTextField(
                        value = titleText,
                        onValueChange = {
                            titleText = it
                            viewModel.onEvent(TaskDetailEvent.UpdateTitle(it))
                        },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color      = if (task.isCompleted)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 28.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier    = Modifier.weight(1f),
                    )
                }
            }

            // ── Note ─────────────────────────────────────────────────────────
            item(key = "note") {
                var noteText      by remember(task.id) { mutableStateOf(task.note) }
                var isEditMode    by remember(task.id) { mutableStateOf(false) }
                val noteFocus     = remember { FocusRequester() }
                val urlColor      = MaterialTheme.colorScheme.primary
                val hasUrls       = remember(noteText) { URL_REGEX.containsMatchIn(noteText) }
                val noteModifier  = Modifier
                    .fillMaxWidth()
                    .padding(start = 58.dp, end = 20.dp, bottom = 16.dp)

                if (isEditMode) {
                    LaunchedEffect(Unit) { noteFocus.requestFocus() }
                    BasicTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            viewModel.onEvent(TaskDetailEvent.UpdateNote(it))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (noteText.isEmpty()) {
                                Text(
                                    "Notes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                            inner()
                        },
                        modifier = noteModifier
                            .focusRequester(noteFocus)
                            .onFocusChanged { if (!it.isFocused) isEditMode = false },
                    )
                } else {
                    Box(modifier = noteModifier.clickable { isEditMode = true }) {
                        when {
                            noteText.isEmpty() -> Text(
                                "Notes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            hasUrls -> {
                                val annotated = remember(noteText, urlColor) {
                                    buildNoteAnnotated(noteText, urlColor)
                                }
                                Text(
                                    text  = annotated,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 22.sp,
                                    ),
                                )
                            }
                            else -> Text(
                                text  = noteText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp,
                                ),
                            )
                        }
                    }
                }
            }

            // ── Checklist ─────────────────────────────────────────────────────
            if (task.checklistItems.isNotEmpty()) {
                item(key = "checklist_divider") {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 20.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
                items(task.checklistItems, key = { it.id }) { item ->
                    ChecklistDetailRow(
                        item        = item,
                        onToggle    = {
                            viewModel.onEvent(TaskDetailEvent.ToggleChecklistItem(item.id, item.isCompleted))
                        },
                        onDateClick = {
                            viewModel.onEvent(TaskDetailEvent.ShowChecklistDatePicker(item.id))
                        },
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 54.dp, end = 20.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }

            // ── Add checklist item ────────────────────────────────────────────
            item(key = "add_checklist") {
                var newItem by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                    )
                    Spacer(Modifier.width(14.dp))
                    BasicTextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newItem.isNotBlank()) {
                                viewModel.onEvent(TaskDetailEvent.AddChecklistItem(newItem))
                                newItem = ""
                            }
                        }),
                        decorationBox = { inner ->
                            if (newItem.isEmpty()) {
                                Text(
                                    "New item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                )
                            }
                            inner()
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    )
                }
            }

            // ── Metadata section ──────────────────────────────────────────────
            item(key = "meta_divider") {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )
            }

            // Project
            if (state.projectName != null) {
                item(key = "project") {
                    MetaRow(
                        icon  = Icons.Outlined.FolderOpen,
                        label = "Project",
                        value = state.projectName,
                        onClick = { viewModel.onEvent(TaskDetailEvent.ShowSmartMove) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }

            // Priority
            item(key = "priority") {
                val priorityValue = when (task.priority) {
                    TaskPriority.HIGH   -> "High"
                    TaskPriority.MEDIUM -> "Medium"
                    TaskPriority.LOW    -> "Low"
                    TaskPriority.NONE   -> null
                }
                val priorityColor = when (task.priority) {
                    TaskPriority.HIGH   -> Color(0xFFDC3545)
                    TaskPriority.MEDIUM -> Color(0xFFFF8800)
                    TaskPriority.LOW    -> Color(0xFF4A90D9)
                    TaskPriority.NONE   -> null
                }
                MetaRow(
                    icon       = Icons.Outlined.Flag,
                    label      = "Priority",
                    value      = priorityValue,
                    valueColor = priorityColor,
                    onClick    = { viewModel.onEvent(TaskDetailEvent.ShowPriorityPicker) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Depends on (blocking task)
            item(key = "depends_on") {
                MetaRow(
                    icon  = Icons.Outlined.Lock,
                    label = "Dépend de",
                    value = state.blockingTask?.title,
                    valueColor = if (state.blockingTask?.isCompleted == false)
                        MaterialTheme.colorScheme.error else null,
                    onClick = { viewModel.onEvent(TaskDetailEvent.ShowDependencyPicker) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Interdépendant de (tasks that depend on this one) — read-only
            if (state.dependingTasks.isNotEmpty()) {
                item(key = "dependents") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.AccountTree,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text  = "Interdépendant de",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            state.dependingTasks.forEach { dep ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector        = Icons.Outlined.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier           = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text  = dep.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // When (schedule)
            item(key = "when") {
                MetaRow(
                    icon     = Icons.Outlined.WbSunny,
                    label    = "When",
                    value    = when (task.scheduledGroup) {
                        TaskGroup.TODAY        -> "Today"
                        TaskGroup.THIS_EVENING -> "This Evening"
                        TaskGroup.SOMEDAY      -> "Someday"
                        TaskGroup.ANYTIME      -> "Anytime"
                        null                   -> null
                    },
                    onClick  = { viewModel.onEvent(TaskDetailEvent.ShowWhenPicker) },
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 52.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )
            }

            // Time tracker
            item(key = "time_tracker") {
                TimerRow(
                    timerState = timerState,
                    onStart    = { viewModel.onEvent(TaskDetailEvent.StartTimer) },
                    onPause    = { viewModel.onEvent(TaskDetailEvent.PauseTimer) },
                    onStop     = { viewModel.onEvent(TaskDetailEvent.StopTimer) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Start Date
            item(key = "start_date") {
                MetaRow(
                    icon    = Icons.Outlined.PlayArrow,
                    label   = "Start Date",
                    value   = task.startDate?.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                    onClick = { viewModel.onEvent(TaskDetailEvent.ShowStartDatePicker) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Deadline (due date)
            item(key = "deadline") {
                val use24h   = com.onlyflows.ui.theme.LocalUse24hFormat.current
                val timePat  = if (use24h) "HH:mm" else "hh:mm a"
                MetaRow(
                    icon    = Icons.Outlined.CalendarToday,
                    label   = "Deadline",
                    value   = task.dueDate?.let { date ->
                        val days = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                        val formatted = date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                        val timePart = task.dueTime?.let { t ->
                            val tf = t.format(DateTimeFormatter.ofPattern(timePat))
                            task.endTime?.let { e -> " · $tf → ${e.format(DateTimeFormatter.ofPattern(timePat))}" } ?: " · $tf"
                        } ?: ""
                        when {
                            days < 0  -> "$formatted$timePart · Overdue"
                            days == 0 -> "$formatted$timePart · Today"
                            else      -> "$formatted$timePart"
                        }
                    },
                    valueColor = task.dueDate?.let { date ->
                        val days = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                        if (days < 0) MaterialTheme.colorScheme.error else null
                    },
                    onClick = { viewModel.onEvent(TaskDetailEvent.ShowDatePicker) },
                    trailing = if (task.dueDate != null) {
                        {
                            IconButton(
                                onClick  = { viewModel.onEvent(TaskDetailEvent.SetDueDate(null)) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close, null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    } else null,
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 52.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )
            }

            // Recurrence
            item(key = "recurrence") {
                MetaRow(
                    icon    = Icons.Outlined.Repeat,
                    label   = "Repeat",
                    value   = task.recurrence?.toLabel(),
                    onClick = { viewModel.onEvent(TaskDetailEvent.ShowRecurrencePicker) },
                )
                if (task.recurrence != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 52.dp, end = 20.dp, bottom = 8.dp),
                    ) {
                        Surface(
                            color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape  = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text     = "${task.recurrenceDoneCount}× done",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Surface(
                            color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape  = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text     = "${task.recurrenceAbandonedCount}× abandoned",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Reminders
            item(key = "reminders") {
                RemindersMetaSection(
                    reminders = state.reminders,
                    onAdd     = { viewModel.onEvent(TaskDetailEvent.ShowReminderPicker) },
                    onDelete  = { viewModel.onEvent(TaskDetailEvent.DeleteReminder(it)) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Tags
            item(key = "tags") {
                val taskTags = state.tags.filter { it.id in task.tagIds }
                MetaRow(
                    icon    = Icons.Outlined.Label,
                    label   = "Tags",
                    value   = null,
                    onClick = { viewModel.onEvent(TaskDetailEvent.ShowTagsPicker) },
                    trailing = if (taskTags.isNotEmpty()) {
                        { TagsRow(taskTags) }
                    } else null,
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 52.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )
            }

            // Attachments
            item(key = "attachments") {
                AttachmentsMetaSection(
                    attachments = state.attachments,
                    onAdd       = { filePicker.launch(arrayOf("*/*")) },
                    onDelete    = { viewModel.onEvent(TaskDetailEvent.DeleteAttachment(it)) },
                    onOpen      = { att ->
                        val uri  = Uri.parse(att.uri)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, att.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(intent) }
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }

            // Comments
            item(key = "comments") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                com.onlyflows.ui.components.CommentsSection(
                    comments = state.comments,
                    onAdd    = { viewModel.onEvent(TaskDetailEvent.AddComment(it)) },
                    onEdit   = { id, content -> viewModel.onEvent(TaskDetailEvent.EditComment(id, content)) },
                    onDelete = { viewModel.onEvent(TaskDetailEvent.DeleteComment(it)) },
                )
            }
        }
    }

    // ── Overlays ─────────────────────────────────────────────────────────────

    if (state.showPriorityPicker) {
        PriorityPickerSheet(
            current   = task.priority,
            onSelect  = { viewModel.onEvent(TaskDetailEvent.SetPriority(it)) },
            onDismiss = { viewModel.onEvent(TaskDetailEvent.HidePriorityPicker) },
        )
    }

    if (state.showWhenPicker) {
        WhenPickerSheet(
            current  = task.scheduledGroup,
            onSelect = { viewModel.onEvent(TaskDetailEvent.SetScheduledGroup(it)) },
            onDismiss = { viewModel.onEvent(TaskDetailEvent.HideWhenPicker) },
        )
    }

    if (state.showDatePicker) {
        DatePickerModal(
            initialDate    = task.dueDate,
            initialDueTime = task.dueTime,
            initialEndTime = task.endTime,
            showTimeOption = true,
            onResult       = { date, dueTime, endTime ->
                viewModel.onEvent(TaskDetailEvent.SetDueDate(date, dueTime, endTime))
            },
            onAddReminder  = {
                viewModel.onEvent(TaskDetailEvent.HideDatePicker)
                viewModel.onEvent(TaskDetailEvent.ShowReminderPicker)
            },
            onDismiss      = { viewModel.onEvent(TaskDetailEvent.HideDatePicker) },
        )
    }

    if (state.showTagsPicker) {
        TagsPickerModal(
            allTags        = state.tags,
            selectedTagIds = task.tagIds,
            onToggleTag    = { viewModel.onEvent(TaskDetailEvent.ToggleTag(it)) },
            onDismiss      = { viewModel.onEvent(TaskDetailEvent.HideTagsPicker) },
            onCreateTag    = { viewModel.onEvent(TaskDetailEvent.CreateTag(it)) },
            onDeleteTag    = { viewModel.onEvent(TaskDetailEvent.DeleteTag(it)) },
        )
    }

    if (state.showStartDatePicker) {
        DatePickerModal(
            initialDate    = task.startDate,
            onDateSelected = { viewModel.onEvent(TaskDetailEvent.SetStartDate(it)) },
            onDismiss      = { viewModel.onEvent(TaskDetailEvent.HideStartDatePicker) },
        )
    }

    if (state.showRecurrencePicker) {
        RecurrencePickerSheet(
            current   = task.recurrence,
            onApply   = { viewModel.onEvent(TaskDetailEvent.SetRecurrence(it)) },
            onDismiss = { viewModel.onEvent(TaskDetailEvent.HideRecurrencePicker) },
        )
    }

    if (state.showReminderPicker) {
        ReminderPickerSheet(
            onApply   = { viewModel.onEvent(TaskDetailEvent.AddReminder(it)) },
            onDismiss = { viewModel.onEvent(TaskDetailEvent.HideReminderPicker) },
        )
    }

    state.checklistDatePickerItemId?.let { itemId ->
        val checklistItem = task.checklistItems.find { it.id == itemId }
        if (checklistItem != null) {
            ChecklistItemDateSheet(
                item      = checklistItem,
                onConfirm = { start, due ->
                    viewModel.onEvent(TaskDetailEvent.SetChecklistItemDates(itemId, start, due))
                },
                onDismiss = { viewModel.onEvent(TaskDetailEvent.HideChecklistDatePicker) },
            )
        }
    }

    if (state.showDependencyPicker) {
        DependencyPickerSheet(
            currentTaskId   = task.id,
            allTasks        = state.allTasks,
            selectedTaskId  = task.dependsOnTaskId,
            onSelect        = { viewModel.onEvent(TaskDetailEvent.SetDependsOn(it)) },
            onClear         = { viewModel.onEvent(TaskDetailEvent.SetDependsOn(null)) },
            onDismiss       = { viewModel.onEvent(TaskDetailEvent.HideDependencyPicker) },
        )
    }

    if (state.showSmartMove) {
        SmartMoveSheet(
            count       = 1,
            projects    = state.projects,
            hiddenGroup = null,
            onConfirm   = { group, projectId, sectionId ->
                viewModel.onEvent(TaskDetailEvent.MoveTask(group, projectId, sectionId))
            },
            onDismiss   = { viewModel.onEvent(TaskDetailEvent.HideSmartMove) },
        )
    }
}

// ── URL helpers ──────────────────────────────────────────────────────────────

private val URL_REGEX = Regex("""https?://[^\s]+""")

private fun buildNoteAnnotated(text: String, urlColor: Color): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        URL_REGEX.findAll(text).forEach { match ->
            if (match.range.first > last) append(text.substring(last, match.range.first))
            withLink(
                LinkAnnotation.Url(
                    url    = match.value,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color          = urlColor,
                            textDecoration = TextDecoration.Underline,
                        )
                    ),
                )
            ) { append(match.value) }
            last = match.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }

// ── Reminders section ────────────────────────────────────────────────────────

@Composable
private fun RemindersMetaSection(
    reminders: List<com.onlyflows.domain.model.Reminder>,
    onAdd: () -> Unit,
    onDelete: (id: String) -> Unit,
) {
    val use24h = com.onlyflows.ui.theme.LocalUse24hFormat.current
    val fmt = DateTimeFormatter.ofPattern(if (use24h) "EEE, d MMM · HH:mm" else "EEE, d MMM · hh:mm a")
    Column(modifier = Modifier.fillMaxWidth()) {
        if (reminders.isEmpty()) {
            // Empty state — looks like a normal MetaRow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.NotificationsNone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(14.dp))
                Text("Reminder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
            }
        } else {
            reminders.forEachIndexed { idx, reminder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Icon(
                        if (idx == 0) Icons.Outlined.NotificationsNone else Icons.Outlined.NotificationsNone,
                        null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text     = reminder.scheduledAt.format(fmt),
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick  = { onDelete(reminder.id) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
                if (idx < reminders.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
            // Add another reminder
            TextButton(
                onClick  = onAdd,
                modifier = Modifier.padding(start = 40.dp),
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add reminder", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Attachments section ───────────────────────────────────────────────────────

@Composable
private fun AttachmentsMetaSection(
    attachments: List<Attachment>,
    onAdd: () -> Unit,
    onDelete: (id: String) -> Unit,
    onOpen: (Attachment) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (attachments.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.Attachment, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(14.dp))
                Text("Pièces jointes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
            }
        } else {
            attachments.forEachIndexed { idx, att ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(att) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector        = attachmentIcon(att.mimeType),
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = att.fileName,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        if (att.fileSize > 0L) {
                            Text(
                                text  = formatFileSize(att.fileSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick  = { onDelete(att.id) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
                if (idx < attachments.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
            TextButton(
                onClick  = onAdd,
                modifier = Modifier.padding(start = 40.dp),
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ajouter une pièce jointe", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun attachmentIcon(mimeType: String): androidx.compose.ui.graphics.vector.ImageVector = when {
    mimeType.startsWith("image/")       -> Icons.Outlined.Image
    mimeType.startsWith("video/")       -> Icons.Outlined.Videocam
    mimeType.startsWith("audio/")       -> Icons.Outlined.MusicNote
    mimeType == "application/pdf"       -> Icons.Outlined.Description
    mimeType.contains("zip") ||
    mimeType.contains("archive")        -> Icons.Outlined.FolderOpen
    mimeType.startsWith("text/")        -> Icons.Outlined.Description
    else                                -> Icons.Outlined.Attachment
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024L              -> "$bytes o"
    bytes < 1024L * 1024L      -> "${bytes / 1024} Ko"
    bytes < 1024L * 1024L * 1024L -> String.format("%.1f Mo", bytes / (1024.0 * 1024.0))
    else                       -> String.format("%.1f Go", bytes / (1024.0 * 1024.0 * 1024.0))
}

// ── Metadata row ──────────────────────────────────────────────────────────────

@Composable
private fun MetaRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
    valueColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            )
        }
        if (trailing != null) {
            trailing()
        }
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Outlined.ChevronRight, null,
            tint     = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TagsRow(tags: List<Tag>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tags.take(3).forEach { TagChip(tag = it) }
        if (tags.size > 3) {
            Text(
                text  = "+${tags.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Checklist row (full interactive) ──────────────────────────────────────────

@Composable
private fun ChecklistDetailRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onDateClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(
                    if (item.isCompleted)
                        Modifier.background(ChecklistCircleFill)
                    else
                        Modifier.border(1.5.dp, ChecklistCircleOutline, CircleShape)
                ),
        ) {
            if (item.isCompleted) {
                Icon(
                    Icons.Outlined.Check, null,
                    tint     = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text  = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isCompleted)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        if (item.startDate != null || item.dueDate != null) {
            val label = when {
                item.startDate != null && item.dueDate != null ->
                    "${item.startDate!!.format(fmt)} → ${item.dueDate!!.format(fmt)}"
                item.startDate != null -> "From ${item.startDate!!.format(fmt)}"
                else -> item.dueDate!!.format(fmt)
            }
            val overdue = item.dueDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() < 0 } == true
            Text(
                text     = label,
                style    = MaterialTheme.typography.labelSmall,
                color    = if (overdue) Color(0xFFDC3545) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onDateClick() },
            )
        } else {
            IconButton(onClick = onDateClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.CalendarToday, null,
                    tint     = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Icon(
            Icons.Outlined.DragHandle, null,
            tint     = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── Checklist item date sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistItemDateSheet(
    item: ChecklistItem,
    onConfirm: (startDate: LocalDate?, dueDate: LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var startDate by remember(item.id) { mutableStateOf(item.startDate) }
    var dueDate   by remember(item.id) { mutableStateOf(item.dueDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showDuePicker   by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                item.title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
            )
            Spacer(Modifier.height(16.dp))

            // Start date row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartPicker = true }
                    .padding(vertical = 12.dp),
            ) {
                Text("Start date", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text  = startDate?.format(fmt) ?: "None",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (startDate != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            // Due date row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDuePicker = true }
                    .padding(vertical = 12.dp),
            ) {
                Text("Due date", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text  = dueDate?.format(fmt) ?: "None",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dueDate != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick  = { onConfirm(startDate, dueDate) },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showStartPicker) {
        DatePickerModal(
            initialDate    = startDate,
            onDateSelected = { showStartPicker = false; startDate = it },
            onDismiss      = { showStartPicker = false },
        )
    }
    if (showDuePicker) {
        DatePickerModal(
            initialDate    = dueDate,
            onDateSelected = { showDuePicker = false; dueDate = it },
            onDismiss      = { showDuePicker = false },
        )
    }
}

// ── Dependency picker sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DependencyPickerSheet(
    currentTaskId: String,
    allTasks: List<com.onlyflows.domain.model.Task>,
    selectedTaskId: String?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allTasks, query) {
        allTasks
            .filter { it.id != currentTaskId }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .sortedBy { it.title }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                "Dépend de",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            // Search field
            OutlinedTextField(
                value       = query,
                onValueChange = { query = it },
                placeholder = { Text("Rechercher une tâche…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
                singleLine  = true,
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Clear option
            if (selectedTaskId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClear() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Outlined.LockOpen, null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Supprimer la dépendance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Aucune tâche trouvée",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }
                items(filtered, key = { it.id }) { t ->
                    val isSelected = t.id == selectedTaskId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(t.id) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = if (t.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint     = if (t.isCompleted) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text  = t.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (t.isCompleted)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Outlined.Check, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                }
            }
        }
    }
}

// ── Priority picker sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityPickerSheet(
    current: TaskPriority,
    onSelect: (TaskPriority) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
        ) {
            Text("Priority", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

            listOf(
                TaskPriority.HIGH   to Triple("High",   Color(0xFFDC3545), Icons.Filled.Flag),
                TaskPriority.MEDIUM to Triple("Medium", Color(0xFFFF8800), Icons.Filled.Flag),
                TaskPriority.LOW    to Triple("Low",    Color(0xFF4A90D9), Icons.Filled.Flag),
                TaskPriority.NONE   to Triple("None",   MaterialTheme.colorScheme.onSurfaceVariant, Icons.Outlined.Flag),
            ).forEach { (priority, triple) ->
                val (label, color, icon) = triple
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(priority); onDismiss() }.padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (current == priority) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── When picker sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenPickerSheet(
    current: TaskGroup?,
    onSelect: (TaskGroup?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text     = "Schedule",
                style    = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            WhenOption(
                icon     = Icons.Outlined.Star,
                iconTint = FlowGold,
                label    = "Today",
                selected = current == TaskGroup.TODAY,
                onClick  = { onSelect(TaskGroup.TODAY) },
            )
            WhenOption(
                icon     = Icons.Outlined.NightlightRound,
                iconTint = Color(0xFF8B8FC9),
                label    = "This Evening",
                selected = current == TaskGroup.THIS_EVENING,
                onClick  = { onSelect(TaskGroup.THIS_EVENING) },
            )
            WhenOption(
                icon     = Icons.Outlined.Layers,
                iconTint = Color(0xFF10B981),
                label    = "Anytime",
                selected = current == TaskGroup.ANYTIME,
                onClick  = { onSelect(TaskGroup.ANYTIME) },
            )
            WhenOption(
                icon     = Icons.Outlined.Archive,
                iconTint = Color(0xFFA07850),
                label    = "Someday",
                selected = current == TaskGroup.SOMEDAY,
                onClick  = { onSelect(TaskGroup.SOMEDAY) },
            )

            if (current != null) {
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    thickness = 0.5.dp,
                )
                WhenOption(
                    icon     = Icons.Outlined.Close,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    label    = "Clear",
                    selected = false,
                    onClick  = { onSelect(null) },
                )
            }
        }
    }
}

@Composable
private fun WhenOption(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text   = label,
            style  = MaterialTheme.typography.bodyLarge,
            color  = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── RecurrenceRule label helper ───────────────────────────────────────────────

private fun RecurrenceRule.toLabel(): String = when (type) {
    RecurrenceType.DAILY    -> if (interval == 1) "Daily" else "Every $interval days"
    RecurrenceType.HOURLY   -> if (interval == 1) "Hourly" else "Every $interval hours"
    RecurrenceType.WEEKLY   -> {
        val days = daysOfWeek.mapNotNull { d -> listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").getOrNull(d - 1) }
        if (days.isEmpty()) "Weekly" else "Every ${days.joinToString(", ")}"
    }
    RecurrenceType.MONTHLY  -> "Monthly"
    RecurrenceType.YEARLY   -> "Yearly"
    RecurrenceType.WEEKDAYS -> "Weekdays"
}

// ── Recurrence picker sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrencePickerSheet(
    current: RecurrenceRule?,
    onApply: (RecurrenceRule?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableStateOf(current?.type ?: RecurrenceType.DAILY) }
    var interval     by remember { mutableStateOf(current?.interval ?: 1) }
    var selectedDays by remember { mutableStateOf(current?.daysOfWeek?.toSet() ?: emptySet<Int>()) }
    var basis        by remember { mutableStateOf(current?.basis ?: RecurrenceBasis.COMPLETION_DATE) }
    var skipWeekends by remember { mutableStateOf(current?.skipWeekends ?: false) }
    var enabled      by remember { mutableStateOf(current != null) }

    val types = RecurrenceType.entries

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Repeat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // Enabled toggle
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Repeat this task", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))

                // Type chips
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types.size) { i ->
                        val t = types[i]
                        val typeLabel = when (t) {
                            RecurrenceType.HOURLY   -> "Hourly"
                            RecurrenceType.DAILY    -> "Daily"
                            RecurrenceType.WEEKLY   -> "Weekly"
                            RecurrenceType.MONTHLY  -> "Monthly"
                            RecurrenceType.YEARLY   -> "Yearly"
                            RecurrenceType.WEEKDAYS -> "Weekdays"
                        }
                        FilterChip(selected = selectedType == t, onClick = { selectedType = t }, label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                // Interval (HOURLY or DAILY)
                if (selectedType == RecurrenceType.HOURLY || selectedType == RecurrenceType.DAILY) {
                    Spacer(Modifier.height(10.dp))
                    val unit = if (selectedType == RecurrenceType.HOURLY) "hours" else "days"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Every", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { if (interval > 1) interval-- }, modifier = Modifier.size(32.dp)) { Text("−") }
                        Text(
                            "$interval",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = { interval++ }, modifier = Modifier.size(32.dp)) { Text("+") }
                        Spacer(Modifier.width(4.dp))
                        Text(unit, style = MaterialTheme.typography.bodyMedium)
                    }
                    // Skip weekends (DAILY only)
                    if (selectedType == RecurrenceType.DAILY) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Skip weekends", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = skipWeekends, onCheckedChange = { skipWeekends = it })
                        }
                    }
                }

                // Day of week (WEEKLY)
                if (selectedType == RecurrenceType.WEEKLY) {
                    val dayLabels = listOf("M","T","W","T","F","S","S")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { idx, label ->
                            val dayNum = idx + 1
                            val sel = dayNum in selectedDays
                            FilterChip(
                                selected = sel,
                                onClick  = { selectedDays = if (sel) selectedDays - dayNum else selectedDays + dayNum },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Based on", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                listOf(
                    RecurrenceBasis.COMPLETION_DATE to "Completion date",
                    RecurrenceBasis.DUE_DATE        to "Due date",
                    RecurrenceBasis.SPECIFIC_DATE   to "Specific date",
                ).forEach { (b, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { basis = b }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = basis == b, onClick = { basis = b })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onApply(null); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(
                    onClick = {
                        if (enabled) {
                            onApply(RecurrenceRule(
                                type         = selectedType,
                                interval     = interval,
                                daysOfWeek   = selectedDays.sorted(),
                                basis        = basis,
                                skipWeekends = skipWeekends,
                            ))
                        } else {
                            onApply(null)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Reminder picker sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPickerSheet(
    onApply: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDate     by remember { mutableStateOf(LocalDate.now()) }
    var pickedHour     by remember { mutableStateOf(9) }
    var pickedMinute   by remember { mutableStateOf(0) }

    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm")
    val preview   = LocalDateTime.of(pickedDate, java.time.LocalTime.of(pickedHour, pickedMinute))

    // Check if exact alarms are available (Android 12+)
    val canExact = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(android.app.AlarmManager::class.java))?.canScheduleExactAlarms() == true
        } else true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text("Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))

            // Exact alarm warning banner
            if (!canExact) {
                Surface(
                    color  = MaterialTheme.colorScheme.tertiaryContainer,
                    shape  = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    android.net.Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsNone, null,
                            tint     = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tap to enable exact timing in Settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Test shortcut (fires in ~1 min) — for dev/debug verification
            ReminderRow(
                icon    = Icons.Outlined.Schedule,
                tint    = MaterialTheme.colorScheme.primary,
                label   = "In 1 minute (test)",
                sublabel = "Fires immediately to test notifications",
                onClick = {
                    onApply(LocalDateTime.now().plusMinutes(1))
                    onDismiss()
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Today evening shortcut
            ReminderRow(
                icon    = Icons.Outlined.NightlightRound,
                tint    = Color(0xFF8B8FC9),
                label   = "This evening · 18:00",
                onClick = {
                    onApply(LocalDate.now().atTime(18, 0))
                    onDismiss()
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Tomorrow morning shortcut
            ReminderRow(
                icon    = Icons.Outlined.WbSunny,
                tint    = Color(0xFFFF8800),
                label   = "Tomorrow morning · 9:00",
                onClick = {
                    onApply(LocalDate.now().plusDays(1).atTime(9, 0))
                    onDismiss()
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Custom date + time
            Spacer(Modifier.height(12.dp))
            Text("Custom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(preview.format(formatter), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(pickedDate.format(DateTimeFormatter.ofPattern("d MMM")))
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(String.format("%02d:%02d", pickedHour, pickedMinute))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = {
                    onApply(preview)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Set Reminder") }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = pickedDate
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        pickedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = pickedHour,
            initialMinute = pickedMinute,
            is24Hour      = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedHour   = timePickerState.hour
                    pickedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

@Composable
private fun ReminderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    sublabel: String? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (sublabel != null) {
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TaskDetailPreview() {
    OnlyFlowsTheme {
        TaskDetailScreen(taskId = "task_book_hotels")
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TaskDetailDarkPreview() {
    OnlyFlowsTheme(darkTheme = true) {
        TaskDetailScreen(taskId = "task_design_search")
    }
}

// ── Timer row ────────────────────────────────────────────────────────────────

@Composable
internal fun TimerRow(
    timerState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = timerState.activeEntry != null
    val hasTime   = timerState.displaySeconds > 0L

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.Timer,
            contentDescription = null,
            tint               = if (isRunning) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Time Tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (hasTime) {
                Text(
                    text  = formatDuration(timerState.displaySeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRunning) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Controls
        if (isRunning) {
            IconButton(onClick = onPause) {
                Icon(Icons.Outlined.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Outlined.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
            }
        } else {
            IconButton(onClick = onStart) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, s)
    else "%dm %02ds".format(m, s)
}
