package com.onlyflows.ui.overlays

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Comment
import com.onlyflows.ui.components.CommentEditRow
import com.onlyflows.ui.components.CommentInput
import com.onlyflows.ui.components.CommentRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<Comment>,
    onAdd: (String) -> Unit,
    onEdit: (id: String, content: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft     by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // DAO returns newest-first (DESC); reverse so oldest is at top (chat style)
    val chronological = remember(comments) { comments.reversed() }

    // Auto-scroll to bottom on open and on new comment
    LaunchedEffect(chronological.size) {
        if (chronological.isNotEmpty()) {
            listState.animateScrollToItem(chronological.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(
                    text     = if (comments.isEmpty()) "Comments" else "Comments (${comments.size})",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Scrollable comment list
            LazyColumn(
                state    = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                itemsIndexed(chronological, key = { _, c -> c.id }) { _, comment ->
                    if (editingId == comment.id) {
                        CommentEditRow(
                            initial   = editDraft,
                            onConfirm = { content -> onEdit(comment.id, content); editingId = null },
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
                item { Spacer(Modifier.height(8.dp)) }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Pinned input bar
            Spacer(Modifier.height(8.dp))
            CommentInput(
                draft    = draft,
                onChange = { draft = it },
                onSend   = { onAdd(draft); draft = "" },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
