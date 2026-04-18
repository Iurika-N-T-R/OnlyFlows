package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.onlyflows.domain.model.GoalFrequency

private val goalColors = listOf(
    "#4A90D9", "#4DBE60", "#F82F6B", "#FF9500", "#8B5CF6", "#CEC17F",
)

private val goalEmojis = listOf("🎯", "🚀", "📊", "💪", "📚", "✈️", "🏆", "🌱", "💡", "🎵")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    onConfirm: (
        title: String,
        frequency: GoalFrequency,
        targetCount: Int,
        colorHex: String,
        emoji: String,
        deadline: LocalDate?,
        tags: List<String>,
    ) -> Unit,
    onDismiss: () -> Unit,
    initialEmoji: String = "🎯",
    initialTitle: String = "",
) {
    var title       by remember { mutableStateOf(initialTitle) }
    var frequency   by remember { mutableStateOf(GoalFrequency.ONCE) }
    var targetCount by remember { mutableIntStateOf(1) }
    var colorHex    by remember { mutableStateOf(goalColors.first()) }
    var emoji       by remember { mutableStateOf(initialEmoji) }
    var tagsText    by remember { mutableStateOf("") }
    var deadline    by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = (deadline ?: LocalDate.now().plusMonths(3))
                .toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        deadline = LocalDate.ofEpochDay(ms / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New Goal", style = MaterialTheme.typography.titleLarge)

            // Emoji picker
            Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                goalEmojis.forEach { e ->
                    val selected = emoji == e
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier      = Modifier.fillMaxWidth(),
            )

            // Frequency chips
            Text("Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick  = { frequency = freq },
                        label    = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // Target count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (targetCount > 1) targetCount-- }) {
                    Text("−", style = MaterialTheme.typography.titleMedium)
                }
                Text("$targetCount", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { targetCount++ }) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Deadline
            Text("Deadline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(deadline?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Set deadline (optional)")
            }

            // Tags
            OutlinedTextField(
                value         = tagsText,
                onValueChange = { tagsText = it },
                label         = { Text("Tags (comma-separated)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            // Color picker
            Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                goalColors.forEach { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                    Box(
                        modifier = Modifier
                            .size(if (colorHex == hex) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable { colorHex = hex },
                    )
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onConfirm(title.trim(), frequency, targetCount, colorHex, emoji, deadline, tags)
                    }
                },
                enabled  = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add Goal")
            }
        }
    }
}
