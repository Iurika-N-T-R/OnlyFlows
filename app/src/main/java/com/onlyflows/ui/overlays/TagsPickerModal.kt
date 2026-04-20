package com.onlyflows.ui.overlays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.data.mock.MockData
import com.onlyflows.domain.model.Tag
import com.onlyflows.ui.components.TagChip
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = ModalDark,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                Text(
                    text       = "Tags",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onDismiss) {
                    Text("Done", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                }
            }

            // Tag list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                items(allTags, key = { it.id }) { tag ->
                    val isSelected = tag.id in selectedTagIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTag(tag.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        // Colored chip instead of plain icon
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

            // Inline new-tag row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value         = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder   = { Text("New tag…", color = Color.White.copy(alpha = 0.4f)) },
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
                        if (newTagName.isNotBlank()) {
                            onCreateTag(newTagName.trim())
                            newTagName = ""
                        }
                    }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick  = {
                        if (newTagName.isNotBlank()) {
                            onCreateTag(newTagName.trim())
                            newTagName = ""
                        }
                    },
                    enabled  = newTagName.isNotBlank(),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Add,
                        contentDescription = "Create tag",
                        tint               = if (newTagName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }
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
