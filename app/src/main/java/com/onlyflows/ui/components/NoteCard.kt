package com.onlyflows.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Note
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit = {},
    onArchive: () -> Unit = {},
    /** Max preview lines for the content (masonry cells use a higher value) */
    contentMaxLines: Int = 3,
    /** Tighter paddings + smaller action icons for narrow masonry cells */
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
            // Title row — actions pinned to the top, not centered against tall content
            Row(
                verticalAlignment = Alignment.Top,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.isPinned || note.title.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (note.isPinned) {
                                Icon(
                                    Icons.Outlined.PushPin,
                                    contentDescription = "Pinned",
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            if (note.title.isNotBlank()) {
                                Text(
                                    text       = note.title,
                                    style      = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    if (note.content.isNotBlank()) {
                        SimpleMarkdownText(
                            text     = note.content,
                            style    = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = contentMaxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (note.title.isBlank() && note.content.isBlank()) {
                        Text(
                            text  = "Empty note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.width(if (compact) 4.dp else 8.dp))
                val btnSize  = if (compact) 28.dp else 36.dp
                val iconSize = if (compact) 16.dp else 18.dp
                if (!compact) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(btnSize)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(iconSize),
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(btnSize)) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "More",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(iconSize),
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (compact) {
                            DropdownMenuItem(
                                text        = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) },
                                onClick     = { showMenu = false; onEdit() },
                            )
                        }
                        DropdownMenuItem(
                            text        = { Text(if (note.isPinned) "Unpin" else "Pin") },
                            leadingIcon = { Icon(Icons.Outlined.PushPin, null, modifier = Modifier.size(18.dp)) },
                            onClick     = { showMenu = false; onTogglePin() },
                        )
                        DropdownMenuItem(
                            text        = { Text(if (note.isArchived) "Restore" else "Archive") },
                            leadingIcon = { Icon(if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null, modifier = Modifier.size(18.dp)) },
                            onClick     = { showMenu = false; onArchive() },
                        )
                        DropdownMenuItem(
                            text        = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(18.dp)) },
                            onClick     = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text  = note.updatedAt.format(dateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}
