package com.onlyflows.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.res.painterResource
import com.onlyflows.R
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.model.TaskDifficulty
import com.onlyflows.domain.model.TaskImpact
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.data.mock.MockData
import com.onlyflows.domain.model.ChecklistItem
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.ui.theme.rememberReducedMotion
import com.onlyflows.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    tags: List<Tag> = emptyList(),
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    /** True only in screens that use longPressDraggableHandle — disables combinedClickable's onLongClick
     *  so the drag gesture isn't consumed before the reorderable library can intercept it. */
    isDraggable: Boolean = false,
    checkboxColor: Color = MaterialTheme.colorScheme.primary,
    dragHandle: (@Composable () -> Unit)? = null,
    projectName: String? = null,
    onToggleComplete: (String) -> Unit = {},
    onToggleChecklistItem: (itemId: String, completed: Boolean) -> Unit = { _, _ -> },
    /** Opens the task detail editor. Click on the item body only folds/unfolds. */
    onEditClick: (String) -> Unit = {},
    onLongClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val taskTags = tags.filter { it.id in task.tagIds }
    val hasExpandable = !task.isCompleted && task.checklistItems.isNotEmpty()
    val haptic = LocalHapticFeedback.current
    // Height of the title's first line — badges/icons center on it (aligned with the
    // checkbox) instead of the whole title block, which looks off when the title wraps.
    val firstLineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp()
    }

    var expanded by remember(task.id) { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)   // longPressDraggableHandle (or empty) applied first → intercepts long press for drag
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent,
            )
            .combinedClickable(
                onClick = {
                    when {
                        isSelectionMode -> onLongClick(task.id)   // selection mode: click = toggle select
                        hasExpandable   -> expanded = !expanded   // fold/unfold
                        else            -> onEditClick(task.id)   // no extra content → edit
                    }
                },
                // When drag is active (longPressDraggableHandle on the modifier), disable
                // onLongClick here so the reorderable library can intercept the gesture first.
                onLongClick = if (isDraggable && !isSelectionMode) null else ({
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(task.id)
                }),
            )
            .padding(
                start  = if (dragHandle != null) 6.dp else 20.dp,
                end    = 20.dp,
                top    = 11.dp,
                bottom = 11.dp,
            ),
    ) {
        // ── Drag handle (optional) ────────────────────────────────────────────
        if (dragHandle != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                dragHandle()
            }
            Spacer(Modifier.width(10.dp))
        }

        // ── Checkbox ──────────────────────────────────────────────────────────
        TaskCheckbox(
            isCompleted = task.isCompleted,
            isSelected  = isSelected,
            accentColor = checkboxColor,
            onToggle    = { onToggleComplete(task.id) },
        )

        Spacer(Modifier.width(14.dp))

        // ── Main content ──────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {

            // Title row — top aligned; badges/icons wrapped in first-line-height boxes so
            // they stay on the checkbox line even when the title wraps to 2 lines
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Checklist progress badge "done/total"
                if (task.checklistItems.isNotEmpty() && !task.isCompleted) {
                    val done    = task.checklistItems.count { it.isCompleted }
                    val total   = task.checklistItems.size
                    val allDone = done == total
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(firstLineHeight)) {
                        Surface(
                            color  = if (allDone) checkboxColor.copy(alpha = 0.15f)
                                     else         MaterialTheme.colorScheme.surfaceVariant,
                            shape  = RoundedCornerShape(5.dp),
                            modifier = Modifier.padding(end = 7.dp),
                        ) {
                            Text(
                                text       = "$done/$total",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (allDone) checkboxColor
                                             else         MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Deadline badge — inline before title
                if (task.dueDate != null && !task.isCompleted) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(firstLineHeight)) {
                        DateBadge(date = task.dueDate!!, dueTime = task.dueTime, endTime = task.endTime)
                    }
                    Spacer(Modifier.width(6.dp))
                }

                Text(
                    text       = task.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    color      = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false),
                )

                // Indicator icons — only when collapsed and not completed
                if (!task.isCompleted && !expanded) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(firstLineHeight)) {
                    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    if (task.hasNote || task.note.isNotBlank()) {
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Outlined.Article, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.hasChecklist || task.checklistItems.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.ChecklistRtl, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.recurrence != null) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.Repeat, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.hasReminders) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.NotificationsNone, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.hasAttachments) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.Attachment, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.goalIds.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.TrackChanges, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.startDate != null) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.PlayArrow, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.hasComments) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = iconTint, modifier = Modifier.size(12.dp))
                    }
                    if (task.hasTimeEntries) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.Timer, null, tint = iconTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.dependsOnTaskId != null) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), modifier = Modifier.size(12.dp))
                    }
                    }
                }

                // Edit icon — only when expanded
                if (expanded) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(firstLineHeight)
                            .width(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = { onEditClick(task.id) }),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Edit,
                            contentDescription = "Edit task",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier           = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Project label — shown when provided (Anytime / Someday)
            if (projectName != null) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text     = projectName,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Note — always visible, 1 line when collapsed
            if (task.note.isNotBlank() && !task.isCompleted) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text     = task.note,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Tags — always visible outside AnimatedVisibility
            if (taskTags.isNotEmpty() && !task.isCompleted) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    taskTags.forEach { tag -> TagChip(tag = tag) }
                }
            }

            // Checklist only — visible when expanded
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    if (task.checklistItems.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        task.checklistItems.forEach { item ->
                            InteractiveChecklistRow(
                                item          = item,
                                accentColor   = checkboxColor,
                                onToggle      = { onToggleChecklistItem(item.id, !item.isCompleted) },
                            )
                        }
                    }
                }
            }
        }

        // ── Right: selection circle OR star + date ────────────────────────────
        Spacer(Modifier.width(10.dp))
        if (isSelectionMode) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(firstLineHeight)) {
                SelectionCircle(isSelected = isSelected, accentColor = checkboxColor)
            }
        } else {
            val hasRight = !task.isCompleted &&
                (task.isStarred || task.priority != TaskPriority.NONE ||
                 task.impact != TaskImpact.NONE || task.difficulty != TaskDifficulty.NONE)
            if (hasRight) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(firstLineHeight),
                ) {
                    if (task.impact != TaskImpact.NONE && !task.isCompleted) {
                        val impactTint = when (task.impact) {
                            TaskImpact.HIGH -> Color(0xFFDC3545)
                            TaskImpact.LOW  -> Color(0xFF4A90D9)
                            TaskImpact.NONE -> Color.Transparent
                        }
                        Icon(Icons.Outlined.Bolt, "Impact", tint = impactTint, modifier = Modifier.size(14.dp))
                    }
                    if (task.difficulty != TaskDifficulty.NONE && !task.isCompleted) {
                        val difficultyTint = when (task.difficulty) {
                            TaskDifficulty.HARD   -> Color(0xFFDC3545)
                            TaskDifficulty.MEDIUM -> Color(0xFFFF8800)
                            TaskDifficulty.EASY   -> Color(0xFF4A90D9)
                            TaskDifficulty.NONE   -> Color.Transparent
                        }
                        Icon(Icons.Outlined.FitnessCenter, "Difficulty", tint = difficultyTint, modifier = Modifier.size(13.dp))
                    }
                    if (task.priority != TaskPriority.NONE && !task.isCompleted) {
                        val flagTint = when (task.priority) {
                            TaskPriority.HIGH   -> Color(0xFFDC3545)
                            TaskPriority.MEDIUM -> Color(0xFFFF8800)
                            TaskPriority.LOW    -> Color(0xFF4A90D9)
                            TaskPriority.NONE   -> Color.Transparent
                        }
                        Icon(Icons.Filled.Flag, "Priority", tint = flagTint, modifier = Modifier.size(14.dp))
                    }
                    if (task.isStarred && !task.isCompleted) {
                        Icon(Icons.Outlined.Star, "Starred", tint = FlowGold, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── Selection circle (right side in selection mode) ───────────────────────────

@Composable
private fun SelectionCircle(
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) Modifier.background(accentColor)
                else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ),
    ) {
        if (isSelected) {
            Icon(
                imageVector        = Icons.Outlined.Check,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(13.dp),
            )
        }
    }
}

// ── Interactive checklist row (in unfolded mode) ──────────────────────────────

@Composable
fun InteractiveChecklistRow(
    item: ChecklistItem,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 3.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .then(
                    if (item.isCompleted)
                        Modifier.background(accentColor.copy(alpha = 0.18f))
                    else
                        Modifier.border(1.dp, ChecklistCircleOutline, CircleShape)
                ),
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector        = Icons.Outlined.Check,
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(9.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text           = item.title,
            style          = MaterialTheme.typography.bodySmall,
            color          = if (item.isCompleted)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
            maxLines       = 1,
            overflow       = TextOverflow.Ellipsis,
        )
    }
}

// ── Read-only checklist row (for other uses) ──────────────────────────────────

@Composable
fun ChecklistItemRow(item: ChecklistItem, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 1.5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape)
                .border(1.dp, ChecklistCircleOutline, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text     = item.title,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Checkbox ──────────────────────────────────────────────────────────────────

@Composable
fun TaskCheckbox(
    isCompleted: Boolean,
    isSelected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic        = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()
    val checked       = isCompleted || isSelected

    val background by animateColorAsState(
        targetValue = when {
            isSelected  -> accentColor
            isCompleted -> accentColor.copy(alpha = 0.18f)
            else        -> Color.Transparent
        },
        animationSpec = if (reducedMotion) snap() else tween(180),
        label = "checkboxBackground",
    )
    // One driver for both the mark scaling in and the empty border fading out, so they
    // can never fall out of sync. The bouncy spring overshoots 1f, giving the tick its pop.
    val mark by animateFloatAsState(
        targetValue   = if (checked) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "checkboxMark",
    )

    // The 22dp visual sits far under the 48dp minimum touch target. The hit area is an
    // unclipped 48dp box overflowing it, which enlarges the target without taking extra
    // space in the row — so no title or divider shifts.
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(22.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(48.dp)
                .clip(CircleShape)
                .clickable {
                    if (!isCompleted) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(background),
            ) {
                if (mark < 1f) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_task_unchecked),
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.outlineVariant,
                        modifier           = Modifier.size(22.dp).alpha((1f - mark).coerceIn(0f, 1f)),
                    )
                }
                if (mark > 0f) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_task_checkmark),
                        contentDescription = null,
                        tint               = if (isSelected) Color.White else accentColor,
                        modifier           = Modifier.size(14.dp).scale(mark),
                    )
                }
            }
        }
    }
}

// ── Date badge ────────────────────────────────────────────────────────────────

@Composable
fun DateBadge(date: LocalDate, dueTime: java.time.LocalTime? = null, endTime: java.time.LocalTime? = null, modifier: Modifier = Modifier) {
    val today     = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, date).toInt()

    val dateText = when {
        daysUntil < 0  -> "Overdue"
        daysUntil == 0 -> "Today"
        daysUntil <= 7 -> "${daysUntil}d"
        else           -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
    val use24h   = com.onlyflows.ui.theme.LocalUse24hFormat.current
    val timePat  = if (use24h) "HH:mm" else "hh:mm a"
    val timeText = dueTime?.let { start ->
        val s = start.format(DateTimeFormatter.ofPattern(timePat))
        endTime?.let { e -> "$s–${e.format(DateTimeFormatter.ofPattern(timePat))}" } ?: s
    }
    val text = if (timeText != null) "$dateText $timeText" else dateText

    val fc = MaterialTheme.flowColors
    val (bgColor, fgColor) = when {
        daysUntil < 0  -> fc.dateBadgeOverdueBg to fc.dateBadgeOverdueFg
        daysUntil == 0 -> fc.dateBadgeTodayBg   to fc.dateBadgeTodayFg
        else           -> fc.dateBadgeFutureBg   to fc.dateBadgeFutureFg
    }

    Surface(color = bgColor, shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = fgColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

// ── Tag chip ──────────────────────────────────────────────────────────────────

@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val base = parseTagColor(tag.colorHex)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(base.copy(alpha = if (isDark) 0.18f else 0.12f))
            .border(0.8.dp, base.copy(alpha = if (isDark) 0.45f else 0.30f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text       = tag.name,
            style      = MaterialTheme.typography.labelSmall,
            color      = base,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun Color.luminance(): Float {
    if (this == Color.Unspecified) return 0f
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TaskItemPreview() {
    OnlyFlowsTheme {
        Column {
            TaskItem(task = MockData.tasks[0], tags = MockData.tags)
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
            TaskItem(task = MockData.tasks[1], tags = MockData.tags)
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
            TaskItem(task = MockData.tasks[5].copy(isStarred = true, dueDate = LocalDate.now()), tags = MockData.tags)
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
            TaskItem(task = MockData.tasks[2].copy(isCompleted = true), tags = MockData.tags)
        }
    }
}
