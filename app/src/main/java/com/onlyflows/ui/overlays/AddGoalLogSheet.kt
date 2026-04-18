package com.onlyflows.ui.overlays

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalLogSheet(
    onConfirm: (label: String, note: String, tags: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var label    by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Log entry", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value         = label,
                onValueChange = { label = it },
                label         = { Text("Title") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier      = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("Note (optional)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value         = tagsText,
                onValueChange = { tagsText = it },
                label         = { Text("Tags (comma-separated)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    if (label.isNotBlank()) {
                        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onConfirm(label.trim(), note.trim(), tags)
                    }
                },
                enabled  = label.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add")
            }
        }
    }
}
