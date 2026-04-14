package com.onlyflows.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Comment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── Section header + timeline + input ────────────────────────────────────────

@Composable
fun CommentsSection(
    comments: List<Comment>,
    onAdd: (String) -> Unit,
    onEdit: (id: String, content: String) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft       by remember { mutableStateOf("") }
    var editingId   by remember { mutableStateOf<String?>(null) }
    var editDraft   by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = if (comments.isEmpty()) "Comments" else "Comments (${comments.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Timeline
        comments.forEach { comment ->
            if (editingId == comment.id) {
                CommentEditRow(
                    initial   = editDraft,
                    onConfirm = { content ->
                        onEdit(comment.id, content)
                        editingId = null
                    },
                    onCancel  = { editingId = null },
                )
            } else {
                CommentRow(
                    comment  = comment,
                    onEdit   = { editingId = comment.id; editDraft = comment.content },
                    onDelete = { onDelete(comment.id) },
                )
            }
        }

        // Add comment input
        Spacer(Modifier.height(8.dp))
        CommentInput(
            draft    = draft,
            onChange = { draft = it },
            onSend   = {
                onAdd(draft)
                draft = ""
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ── Single comment row ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentRow(
    comment: Comment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = { showMenu = true }, onClick = {})
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        // Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .padding(4.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize(),
            ) {}
            Text(
                text  = "M",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = formatRelativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Context menu
        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text        = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                onClick     = { showMenu = false; onEdit() },
            )
            DropdownMenuItem(
                text        = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick     = { showMenu = false; onDelete() },
            )
        }
    }
}

// ── Inline edit field ─────────────────────────────────────────────────────────

@Composable
private fun CommentEditRow(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value         = text,
                onValueChange = { text = it },
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                modifier      = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { onConfirm(text) }) {
            Icon(Icons.Outlined.Check, "Confirm", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Outlined.Close, "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Add comment input ─────────────────────────────────────────────────────────

@Composable
private fun CommentInput(
    draft: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value         = draft,
                onValueChange = onChange,
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox   = { inner ->
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (draft.isEmpty()) {
                            Text(
                                text  = "Add a comment…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        if (draft.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatRelativeTime(dt: LocalDateTime): String {
    val now     = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(dt, now)
    val hours   = ChronoUnit.HOURS.between(dt, now)
    val days    = ChronoUnit.DAYS.between(dt, now)
    return when {
        minutes < 1  -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24   -> "${hours}h ago"
        days < 7     -> "${days}d ago"
        else         -> dt.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}
