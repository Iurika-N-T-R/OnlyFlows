package com.onlyflows.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Tag
import com.onlyflows.ui.components.TagChip
import com.onlyflows.ui.components.byFolder
import com.onlyflows.ui.components.folderNames
import com.onlyflows.ui.components.parseTagColor
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val TAG_PALETTE = listOf(
    "#EF4444", // Red
    "#F97316", // Orange
    "#F59E0B", // Amber
    "#EAB308", // Yellow
    "#84CC16", // Lime
    "#10B981", // Emerald
    "#14B8A6", // Teal
    "#3B82F6", // Blue
    "#6366F1", // Indigo
    "#8B5CF6", // Violet
    "#EC4899", // Pink
    "#6B7280", // Gray
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit = {},
    viewModel: TagsViewModel = viewModel(
        factory = TagsViewModel.factory(
            LocalContext.current.applicationContext as OnlyFlowsApp
        )
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTag       by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Manage Tags", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Outlined.Add, "New tag")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Unused tags banner
            if (state.unusedCount > 0) {
                Surface(
                    color  = MaterialTheme.colorScheme.errorContainer,
                    shape  = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text  = "${state.unusedCount} unused tag${if (state.unusedCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(
                            onClick = { viewModel.onEvent(TagsEvent.DeleteUnused) },
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint     = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Remove all",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            // Folder headers and tags share one flat list so a tag can be dragged into
            // another folder: on drop each tag takes the folder of the header above it.
            val serverRows: List<Any> = remember(state.tags) {
                buildList {
                    state.tags.byFolder().forEach { folder ->
                        add(FolderRowKey(folder.name))
                        addAll(folder.tags)
                    }
                }
            }
            var rows by remember(serverRows) { mutableStateOf(serverRows) }

            val listState = rememberLazyListState()
            // Every tag starts at sortOrder 0, so without this flag the initial `false` from
            // snapshotFlow would renumber — and LAN-push — the whole list on screen open.
            var hasReordered by remember { mutableStateOf(false) }
            val reorderState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
                val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
                val toKey   = to.key   as? String ?: return@rememberReorderableLazyListState
                if (!fromKey.startsWith("tag_row_")) return@rememberReorderableLazyListState
                val fromIdx = rows.indexOfFirst { tagsRowKey(it) == fromKey }
                val toIdx   = rows.indexOfFirst { tagsRowKey(it) == toKey }
                if (fromIdx == -1 || toIdx == -1) return@rememberReorderableLazyListState
                rows = rows.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
                hasReordered = true
            }

            LaunchedEffect(reorderState) {
                snapshotFlow { reorderState.isAnyItemDragging }.collect { dragging ->
                    if (dragging || !hasReordered) return@collect
                    hasReordered = false
                    var folder: String? = null
                    var position = 0
                    val updated = mutableListOf<Tag>()
                    rows.forEach { row ->
                        when (row) {
                            is FolderRowKey -> folder = row.name
                            is Tag -> {
                                updated += row.copy(groupName = folder, sortOrder = position)
                                position++
                            }
                        }
                    }
                    if (updated.isNotEmpty()) viewModel.onEvent(TagsEvent.ApplyOrder(updated))
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                rows.forEach { row ->
                    when (row) {
                        is FolderRowKey -> item(key = tagsRowKey(row)) {
                            TagFolderHeader(
                                name     = row.name,
                                onRename = { newName ->
                                    row.name?.let { viewModel.onEvent(TagsEvent.RenameFolder(it, newName)) }
                                },
                            )
                        }
                        is Tag -> item(key = tagsRowKey(row)) {
                            ReorderableItem(reorderState, key = tagsRowKey(row)) { isDragging ->
                                Surface(
                                    shadowElevation = if (isDragging) 6.dp else 0.dp,
                                    color           = MaterialTheme.colorScheme.background,
                                ) {
                                    Column {
                                        TagRow(
                                            tag        = row,
                                            usageCount = state.usageCounts[row.id] ?: 0,
                                            onEdit     = { editingTag = row },
                                            onDelete   = { viewModel.onEvent(TagsEvent.DeleteTag(row.id)) },
                                            dragHandle = {
                                                Icon(
                                                    imageVector        = Icons.Outlined.DragHandle,
                                                    contentDescription = "Reorder",
                                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier           = Modifier
                                                        .size(20.dp)
                                                        .draggableHandle(),
                                                )
                                            },
                                        )
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color     = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val existingFolders = remember(state.tags) { state.tags.folderNames() }

    if (showCreateDialog) {
        TagEditDialog(
            title         = "New Tag",
            initialName   = "",
            initialColor  = TAG_PALETTE[7], // Blue default
            initialFolder = "",
            knownFolders  = existingFolders,
            onConfirm  = { name, color, folder ->
                viewModel.onEvent(TagsEvent.CreateTag(name, color, folder))
                showCreateDialog = false
            },
            onDismiss  = { showCreateDialog = false },
        )
    }

    editingTag?.let { tag ->
        TagEditDialog(
            title         = "Edit Tag",
            initialName   = tag.name,
            initialColor  = tag.colorHex,
            initialFolder = tag.groupName ?: "",
            knownFolders  = existingFolders,
            onConfirm     = { name, color, folder ->
                viewModel.onEvent(TagsEvent.UpdateTag(tag.copy(
                    name      = name,
                    colorHex  = color,
                    groupName = folder,
                )))
                editingTag = null
            },
            onDismiss    = { editingTag = null },
        )
    }
}

/** Marks a folder header inside the flat drag list. */
private data class FolderRowKey(val name: String?)

private fun tagsRowKey(row: Any): String = when (row) {
    is FolderRowKey -> "tag_folder_" + (row.name ?: "~none")
    is Tag          -> "tag_row_" + row.id
    else            -> error("unreachable")
}

@Composable
private fun TagFolderHeader(name: String?, onRename: (String?) -> Unit) {
    var showRename by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 18.dp, bottom = 4.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier           = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text       = name ?: "Ungrouped",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier   = Modifier.weight(1f),
        )
        if (name != null) {
            IconButton(onClick = { showRename = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    "Rename folder",
                    modifier = Modifier.size(15.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }

    if (showRename && name != null) {
        var draft by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename folder") },
            text  = {
                OutlinedTextField(
                    value          = draft,
                    onValueChange  = { draft = it },
                    label          = { Text("Folder name") },
                    singleLine     = true,
                    supportingText = { Text("Leave empty to dissolve the folder") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(draft.trim().takeIf { it.isNotEmpty() })
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    usageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        if (dragHandle != null) {
            dragHandle()
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        // Color dot
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(parseColor(tag.colorHex)),
        )
        Spacer(Modifier.width(12.dp))
        // Tag chip preview
        TagChip(tag = tag)
        Spacer(Modifier.width(12.dp))
        // Usage count
        Text(
            text     = if (usageCount == 0) "unused" else "$usageCount task${if (usageCount > 1) "s" else ""}",
            style    = MaterialTheme.typography.labelMedium,
            color    = if (usageCount == 0)
                MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.Edit,
                "Edit",
                modifier = Modifier.size(18.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.Delete,
                "Delete",
                modifier = Modifier.size(18.dp),
                tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TagEditDialog(
    title: String,
    initialName: String,
    initialColor: String,
    initialFolder: String,
    knownFolders: List<String>,
    onConfirm: (name: String, colorHex: String, folder: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name          by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var folder        by remember { mutableStateOf(initialFolder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(title, fontWeight = FontWeight.SemiBold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Tag name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )

                // Live preview
                if (name.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Preview: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        TagChip(tag = Tag(id = "preview", name = name, colorHex = selectedColor))
                    }
                }

                // Folder - free text, so naming a new one is all it takes to create it
                OutlinedTextField(
                    value         = folder,
                    onValueChange = { folder = it },
                    label         = { Text("Folder (optional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                if (knownFolders.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        knownFolders.forEach { existing ->
                            FilterChip(
                                selected = folder.trim().equals(existing, ignoreCase = true),
                                onClick  = { folder = if (folder.trim() == existing) "" else existing },
                                label    = { Text(existing, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                }

                // Color palette grid
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val rows = TAG_PALETTE.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { hex ->
                                val base = parseColor(hex)
                                val isSelected = hex.equals(selectedColor, ignoreCase = true)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(base)
                                        .then(
                                            if (isSelected)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { selectedColor = hex },
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            null,
                                            tint     = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor, folder.trim().takeIf { it.isNotEmpty() }) },
                enabled  = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun parseColor(hex: String): Color = parseTagColor(hex)
