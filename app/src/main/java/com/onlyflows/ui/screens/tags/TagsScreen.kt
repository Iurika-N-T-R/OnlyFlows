package com.onlyflows.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import com.onlyflows.ui.components.parseTagColor

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

@OptIn(ExperimentalMaterial3Api::class)
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

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(state.tags, key = { it.id }) { tag ->
                    val usageCount = state.usageCounts[tag.id] ?: 0
                    TagRow(
                        tag        = tag,
                        usageCount = usageCount,
                        onEdit     = { editingTag = tag },
                        onDelete   = { viewModel.onEvent(TagsEvent.DeleteTag(tag.id)) },
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

    if (showCreateDialog) {
        TagEditDialog(
            title      = "New Tag",
            initialName  = "",
            initialColor = TAG_PALETTE[7], // Blue default
            onConfirm  = { name, color ->
                viewModel.onEvent(TagsEvent.CreateTag(name, color))
                showCreateDialog = false
            },
            onDismiss  = { showCreateDialog = false },
        )
    }

    editingTag?.let { tag ->
        TagEditDialog(
            title        = "Edit Tag",
            initialName  = tag.name,
            initialColor = tag.colorHex,
            onConfirm    = { name, color ->
                viewModel.onEvent(TagsEvent.UpdateTag(tag.copy(name = name, colorHex = color)))
                editingTag = null
            },
            onDismiss    = { editingTag = null },
        )
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    usageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
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
    onConfirm: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name          by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

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
                onClick  = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) },
                enabled  = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun parseColor(hex: String): Color = parseTagColor(hex)
