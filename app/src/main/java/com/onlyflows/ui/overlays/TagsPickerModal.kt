package com.onlyflows.ui.overlays

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.data.mock.MockData
import com.onlyflows.domain.model.Tag
import com.onlyflows.ui.components.TagChip
import com.onlyflows.ui.components.byFolder
import com.onlyflows.ui.components.matching
import com.onlyflows.ui.theme.ModalDark
import com.onlyflows.ui.theme.OnlyFlowsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsPickerModal(
    allTags: List<Tag>,
    selectedTagIds: List<String>,
    onToggleTag: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateTag: (name: String) -> Unit = {},
    onDeleteTag: (tagId: String) -> Unit = {},
) {
    var newTagName by remember { mutableStateOf("") }
    var query      by remember { mutableStateOf("") }
    // Folders a user explicitly closed. Searching ignores this so no match stays hidden.
    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }

    val visibleTags = remember(allTags, query) { allTags.matching(query) }
    val folders     = remember(visibleTags) { visibleTags.byFolder() }
    val searching   = query.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = ModalDark,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The picker is usually opened from the add-task sheet with the keyboard up.
                // Without this the search field pushes the list behind the IME and the sheet
                // looks empty — the tags are there, just off-screen.
                .imePadding()
                .padding(bottom = 24.dp),
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column {
                    Text(
                        text       = "Tags",
                        style      = MaterialTheme.typography.headlineMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (selectedTagIds.isNotEmpty()) {
                        Text(
                            text  = "${selectedTagIds.size} selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Done", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                }
            }

            // Search — the whole point of folders is a list too long to scroll through
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search tags or folders…", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = Color.White.copy(alpha = 0.45f)) },
                trailingIcon  = if (query.isNotEmpty()) ({
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, "Clear", tint = Color.White.copy(alpha = 0.45f))
                    }
                }) else null,
                singleLine = true,
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color.White.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            if (folders.isEmpty()) {
                Text(
                    text     = if (searching) "No tag matches \"${query.trim()}\"" else "No tags yet",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
            ) {
                folders.forEach { folder ->
                    val folderKey  = folder.name ?: "\u0000ungrouped"
                    val isCollapsed = !searching && folderKey in collapsedFolders

                    // The ungrouped bucket only earns a header once a real folder exists
                    if (folder.name != null || folders.size > 1) {
                        item(key = "folder_$folderKey") {
                            FolderHeaderRow(
                                label       = folder.name ?: "Ungrouped",
                                count       = folder.tags.size,
                                isCollapsed = isCollapsed,
                                enabled     = !searching,
                                onToggle    = {
                                    collapsedFolders = if (folderKey in collapsedFolders)
                                        collapsedFolders - folderKey else collapsedFolders + folderKey
                                },
                            )
                        }
                    }

                    if (isCollapsed) return@forEach

                    items(folder.tags.size, key = { i -> folder.tags[i].id }) { index ->
                        val tag = folder.tags[index]
                        val isSelected = tag.id in selectedTagIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTag(tag.id) }
                                .padding(start = if (folder.name != null) 34.dp else 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                        ) {
                            TagChip(tag = tag)
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    imageVector        = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            IconButton(
                                onClick  = { onDeleteTag(tag.id) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector        = Icons.Outlined.Close,
                                    contentDescription = "Delete tag",
                                    tint               = Color.White.copy(alpha = 0.35f),
                                    modifier           = Modifier.size(16.dp),
                                )
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    }
                }
            }

            // Inline new-tag row — prefilled from the search so a miss turns into a tag
            val createName = newTagName.ifBlank { query.trim().removePrefix("#") }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value         = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder   = {
                        Text(
                            text  = if (query.isNotBlank()) "Create \"${query.trim()}\"…" else "New tag…",
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    },
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.White.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Color.White,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (createName.isNotBlank()) {
                            onCreateTag(createName)
                            newTagName = ""
                            query      = ""
                        }
                    }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick  = {
                        if (createName.isNotBlank()) {
                            onCreateTag(createName)
                            newTagName = ""
                            query      = ""
                        }
                    },
                    enabled  = createName.isNotBlank(),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Add,
                        contentDescription = "Create tag",
                        tint               = if (createName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderHeaderRow(
    label: String,
    count: Int,
    isCollapsed: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val chevronAngle by animateFloatAsState(if (isCollapsed) 0f else 90f, label = "tag_folder_chevron")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint               = Color.White.copy(alpha = if (enabled) 0.45f else 0f),
            modifier           = Modifier.size(16.dp).rotate(chevronAngle),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector        = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.45f),
            modifier           = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}

@Preview
@Composable
private fun TagsPickerModalPreview() {
    OnlyFlowsTheme {
        TagsPickerModal(
            allTags        = MockData.tags,
            selectedTagIds = listOf("tag_important"),
            onToggleTag    = {},
            onDismiss      = {},
        )
    }
}
