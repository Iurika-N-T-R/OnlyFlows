package com.onlyflows.ui.overlays

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyflows.domain.model.Note
import com.onlyflows.ui.components.buildMarkdownAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    note: Note?,
    startInViewMode: Boolean = false,
    onSave: (id: String?, title: String, content: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = null,
    ) {
        NoteEditorContent(
            note            = note,
            startInViewMode = startInViewMode,
            onSave          = onSave,
            onDismiss       = onDismiss,
            modifier        = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding(),
        )
    }
}

@Composable
fun NoteEditorContent(
    note: Note?,
    startInViewMode: Boolean = false,
    onSave: (id: String?, title: String, content: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNewNote  = note == null
    var isViewMode by remember(note?.id) { mutableStateOf(startInViewMode && !isNewNote) }
    var title      by remember(note?.id) { mutableStateOf(note?.title   ?: "") }
    var contentFv  by remember(note?.id) { mutableStateOf(TextFieldValue(note?.content ?: "")) }

    val handleDismiss = {
        val hasChanges = contentFv.text != (note?.content ?: "") || title != (note?.title ?: "")
        if (isViewMode && hasChanges && note != null) {
            onSave(note.id, title.trim(), contentFv.text.trim())
        } else {
            onDismiss()
        }
    }

    fun wrapSelection(marker: String) {
        val sel  = contentFv.selection
        val text = contentFv.text
        if (sel.collapsed) {
            val newText = text.substring(0, sel.start) + "$marker$marker" + text.substring(sel.start)
            contentFv = contentFv.copy(
                text      = newText,
                selection = TextRange(sel.start + marker.length),
            )
        } else {
            val selected = text.substring(sel.start, sel.end)
            val newText  = text.substring(0, sel.start) + "$marker$selected$marker" + text.substring(sel.end)
            contentFv = contentFv.copy(
                text      = newText,
                selection = TextRange(sel.start + marker.length, sel.end + marker.length),
            )
        }
    }

    fun toggleLinePrefix(prefix: String) {
        val text      = contentFv.text
        val cursor    = contentFv.selection.min
        val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
        val lineEnd   = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line      = text.substring(lineStart, lineEnd)
        val newLine   = if (line.startsWith(prefix)) line.removePrefix(prefix) else "$prefix$line"
        val delta     = newLine.length - line.length
        val newText   = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        contentFv = contentFv.copy(
            text      = newText,
            selection = TextRange((cursor + delta).coerceAtLeast(lineStart)),
        )
    }

    fun toggleCheckboxLine() {
        val text      = contentFv.text
        val cursor    = contentFv.selection.min
        val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
        val lineEnd   = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line      = text.substring(lineStart, lineEnd)
        val newLine = when {
            line.matches(Regex("^- \\[ \\].*")) -> line.replaceFirst("- [ ]", "- [x]")
            line.matches(Regex("^- \\[x\\].*")) -> line.replaceFirst("- [x]", "- [ ]")
            else -> "- [ ] $line"
        }
        val delta   = newLine.length - line.length
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        contentFv = contentFv.copy(
            text      = newText,
            selection = TextRange((cursor + delta).coerceAtLeast(lineStart)),
        )
    }

    fun toggleViewCheckbox(lineIndex: Int, nowChecked: Boolean) {
        val lines = contentFv.text.lines().toMutableList()
        if (lineIndex < lines.size) {
            val line = lines[lineIndex]
            lines[lineIndex] = if (nowChecked)
                line.replaceFirst(Regex("^(\\s*- )\\[ \\]"), "$1[x]")
            else
                line.replaceFirst(Regex("^(\\s*- )\\[x\\]"), "$1[ ]")
            contentFv = TextFieldValue(lines.joinToString("\n"))
        }
    }

    Column(modifier = modifier) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = handleDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Close")
            }
            Text(
                text       = when {
                    isNewNote  -> "New note"
                    isViewMode -> "Note"
                    else       -> "Edit"
                },
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            if (isViewMode) {
                IconButton(onClick = { isViewMode = false }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                }
            } else {
                TextButton(
                    onClick  = { onSave(note?.id, title.trim(), contentFv.text.trim()) },
                    enabled  = title.isNotBlank() || contentFv.text.isNotBlank(),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        if (isViewMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                MarkdownViewer(
                    content          = contentFv.text,
                    onCheckboxToggle = { lineIdx, checked -> toggleViewCheckbox(lineIdx, checked) },
                )
                Spacer(Modifier.height(32.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
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
                BasicTextField(
                    value         = contentFv,
                    onValueChange = { contentFv = it },
                    textStyle     = MaterialTheme.typography.bodyLarge.copy(
                        color      = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                    ),
                    cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (contentFv.text.isEmpty()) {
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
                        .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            MarkdownToolbar(
                onHeading  = { toggleLinePrefix("# ") },
                onH2       = { toggleLinePrefix("## ") },
                onBold     = { wrapSelection("**") },
                onItalic   = { wrapSelection("*") },
                onCode     = { wrapSelection("`") },
                onBullet   = { toggleLinePrefix("- ") },
                onCheckbox = { toggleCheckboxLine() },
            )
            Spacer(Modifier.imePadding())
        }
    }
}

// ── Markdown viewer (view mode) ───────────────────────────────────────────────

@Composable
internal fun MarkdownViewer(
    content: String,
    onCheckboxToggle: (lineIndex: Int, nowChecked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        content.lines().forEachIndexed { index, line ->
            when {
                line.matches(Regex("^- \\[[ xX]\\].*")) -> {
                    val checked     = line.matches(Regex("^- \\[[xX]\\].*"))
                    val itemContent = line.removePrefix("- [x] ").removePrefix("- [X] ")
                        .removePrefix("- [ ] ").removePrefix("- [x]")
                        .removePrefix("- [X]").removePrefix("- [ ]").trimStart()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Checkbox(
                            checked         = checked,
                            onCheckedChange = { onCheckboxToggle(index, it) },
                            modifier        = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text           = buildMarkdownAnnotatedString(itemContent),
                            style          = MaterialTheme.typography.bodyLarge,
                            color          = if (checked)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                            modifier       = Modifier.weight(1f),
                        )
                    }
                }
                line.startsWith("### ") -> Text(
                    text       = buildMarkdownAnnotatedString(line.removePrefix("### ")),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                line.startsWith("## ") -> Text(
                    text       = buildMarkdownAnnotatedString(line.removePrefix("## ")),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(top = 12.dp, bottom = 2.dp),
                )
                line.startsWith("# ") -> Text(
                    text       = buildMarkdownAnnotatedString(line.removePrefix("# ")),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                line.trimStart().startsWith("---") -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                line.matches(Regex("^[-*+] .*")) -> {
                    val bulletContent = line.drop(2)
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    ) {
                        Text(
                            text     = "•",
                            style    = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                        )
                        Text(
                            text     = buildMarkdownAnnotatedString(bulletContent),
                            style    = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                line.isBlank() -> Spacer(Modifier.height(6.dp))
                else -> Text(
                    text     = buildMarkdownAnnotatedString(line),
                    style    = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                )
            }
        }
    }
}

// ── Markdown toolbar (edit mode) ──────────────────────────────────────────────

@Composable
internal fun MarkdownToolbar(
    onHeading:  () -> Unit,
    onH2:       () -> Unit,
    onBold:     () -> Unit,
    onItalic:   () -> Unit,
    onCode:     () -> Unit,
    onBullet:   () -> Unit,
    onCheckbox: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        IconButton(onClick = onHeading, modifier = Modifier.size(40.dp)) {
            Text("H1", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = tint)
        }
        IconButton(onClick = onH2, modifier = Modifier.size(40.dp)) {
            Text("H2", style = MaterialTheme.typography.labelMedium, color = tint)
        }
        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))
        IconButton(onClick = onBold, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.FormatBold, contentDescription = "Gras", tint = tint, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onItalic, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.FormatItalic, contentDescription = "Italique", tint = tint, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onCode, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Code, contentDescription = "Code", tint = tint, modifier = Modifier.size(20.dp))
        }
        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))
        IconButton(onClick = onBullet, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.FormatListBulleted, contentDescription = "Liste", tint = tint, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onCheckbox, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.CheckBoxOutlineBlank, contentDescription = "Checkbox", tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}
