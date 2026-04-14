package com.onlyflows.ui.overlays

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    note: Note?,          // null = new note
    onSave: (id: String?, title: String, content: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title   by remember(note?.id) { mutableStateOf(note?.title   ?: "") }
    var content by remember(note?.id) { mutableStateOf(note?.content ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // ── Header bar ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fermer")
                }
                Text(
                    text       = if (note == null) "Nouvelle note" else "Modifier",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f).padding(start = 4.dp),
                )
                TextButton(
                    onClick  = { onSave(note?.id, title.trim(), content.trim()) },
                    enabled  = title.isNotBlank() || content.isNotBlank(),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Enregistrer")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // ── Title field ────────────────────────────────────────────────────
            BasicTextField(
                value         = title,
                onValueChange = { title = it },
                singleLine    = true,
                textStyle     = MaterialTheme.typography.titleLarge.copy(
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            "Titre (optionnel)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )

            // ── Content field ──────────────────────────────────────────────────
            BasicTextField(
                value         = content,
                onValueChange = { content = it },
                textStyle     = MaterialTheme.typography.bodyLarge.copy(
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (content.isEmpty()) {
                        Text(
                            "Contenu de la note…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp)
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }
    }
}
