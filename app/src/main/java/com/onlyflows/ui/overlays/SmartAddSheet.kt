package com.onlyflows.ui.overlays

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.smartadd.ContentWriter
import com.onlyflows.data.smartadd.IMPORT_MIME_TYPES
import com.onlyflows.data.smartadd.importFromUri
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.RecurrenceType
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.smartadd.Batch
import com.onlyflows.domain.smartadd.Interpretation
import com.onlyflows.domain.smartadd.NaturalLanguage
import com.onlyflows.domain.smartadd.TaskDraft
import com.onlyflows.ui.theme.LocalUse24hFormat
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Smart add: type or say what you want ("Call John tomorrow at 9 AM", "Add milk, eggs to my
 * shopping list", "Create a project for my website"), check what will be created, add it.
 * One item per line. A question shows up only when a sentence could mean two things.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartAddSheet(
    initialText: String = "",
    startVoice: Boolean = false,
    onDismiss: () -> Unit,
) {
    val context   = LocalContext.current
    val container = remember { (context.applicationContext as OnlyFlowsApp).container }
    val projects by container.projectRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope     = rememberCoroutineScope()
    val use24h    = LocalUse24hFormat.current
    val focus     = remember { FocusRequester() }

    var text    by remember { mutableStateOf(initialText) }
    var saving  by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<ContentWriter.Summary?>(null) }
    val choices = remember { mutableStateMapOf<Int, Int>() }

    val lines   = remember(text) { text.lines().map { it.trim() }.filter { it.isNotEmpty() } }
    val results = remember(lines, projects) { lines.map { NaturalLanguage.interpret(it, projects) } }
    LaunchedEffect(lines) { choices.clear() }

    /** Everything to save, or null while a question is unanswered or nothing is usable. */
    val batch: Batch? = run {
        var b = Batch()
        results.forEachIndexed { i, r ->
            when (r) {
                is Interpretation.Ready   -> b += r.batch
                is Interpretation.Ask     -> b += r.options[choices[i] ?: return@run null].second
                is Interpretation.Invalid -> Unit
            }
        }
        b.takeUnless { it.isEmpty }
    }

    fun save() {
        val b = batch ?: return
        saving = true
        scope.launch {
            val s = ContentWriter(context).write(b)
            Toast.makeText(context, if (s.lines.size == 1) s.lines[0] else s.text, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val heard = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) {
            text = if (text.isBlank()) heard else text.trimEnd() + "\n" + heard
        }
    }
    fun listen() {
        try {
            voice.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a task, a list or a note"),
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Speech recognition isn't available on this device", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) { if (startVoice) listen() else runCatching { focus.requestFocus() } }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { saving = true; summary = importFromUri(context, uri); saving = false }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Smart add", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it; summary = null },
                placeholder   = { Text("Call John tomorrow at 9 AM\nBuy groceries every Monday\nAdd milk, eggs, bread to my shopping list") },
                trailingIcon  = {
                    IconButton(onClick = ::listen) { Icon(Icons.Outlined.Mic, contentDescription = "Speak", tint = MaterialTheme.colorScheme.primary) }
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Text(
                "One item per line. Dates, times, “every Monday”, “remind me”, #tags and !priority are understood.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            results.forEachIndexed { i, r ->
                Spacer(Modifier.height(10.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        when (r) {
                            is Interpretation.Ready   -> BatchPreview(r.batch, projects, use24h)
                            is Interpretation.Invalid -> Text(r.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            is Interpretation.Ask     -> {
                                Text(r.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    r.options.forEachIndexed { j, (label, _) ->
                                        FilterChip(selected = choices[i] == j, onClick = { choices[i] = j }, label = { Text(label) })
                                    }
                                }
                                choices[i]?.let { j -> Spacer(Modifier.height(4.dp)); BatchPreview(r.options[j].second, projects, use24h) }
                            }
                        }
                    }
                }
            }

            summary?.let { s ->
                Spacer(Modifier.height(10.dp))
                ImportSummaryCard(s)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { filePicker.launch(IMPORT_MIME_TYPES) }, enabled = !saving) {
                    Icon(Icons.Outlined.UploadFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import file")
                }
                Spacer(Modifier.weight(1f))
                if (summary != null && batch == null) TextButton(onClick = onDismiss) { Text("Done") }
                else Button(onClick = ::save, enabled = !saving && batch != null) { Text("Add") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── preview ───────────────────────────────────────────────────────────────────

@Composable
private fun BatchPreview(batch: Batch, projects: List<Project>, use24h: Boolean) {
    batch.containers.forEach { c ->
        val exists = projects.any { it.title.trim().equals(c.title.trim(), ignoreCase = true) }
        val word = when (c.type) { ProjectType.LIST -> "list"; ProjectType.NOTE -> "notes list"; ProjectType.PROJECT -> "project" }
        val icon = when (c.type) { ProjectType.LIST -> "☑️"; ProjectType.NOTE -> "📝"; ProjectType.PROJECT -> "📁" }
        Text(
            if (exists) "$icon ${c.title}" else "$icon New $word “${c.title}”",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
        )
        c.tasks.forEach { TaskLine(it, use24h, indent = true) }
        c.notes.forEach { n -> Text("    • ${n.title}", style = MaterialTheme.typography.bodyMedium) }
    }
    batch.tasks.forEach { TaskLine(it, use24h, indent = false) }
}

@Composable
private fun TaskLine(t: TaskDraft, use24h: Boolean, indent: Boolean) {
    Text((if (indent) "    " else "") + "☐ ${t.title}", style = MaterialTheme.typography.bodyMedium)
    val details = describe(t, use24h)
    if (details.isNotEmpty()) Text(
        (if (indent) "        " else "      ") + details,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun day(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "EEE d MMM" else "EEE d MMM yyyy", Locale.getDefault()))
    }
}

private fun describe(t: TaskDraft, use24h: Boolean): String {
    val timeFmt = DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", Locale.getDefault())
    val parts = ArrayList<String>()
    t.dueDate?.let { d -> parts += "📅 " + day(d) + (t.dueTime?.let { " " + it.format(timeFmt) } ?: "") }
    t.recurrence?.let { r ->
        val every = if (r.interval > 1) "every ${r.interval} " else "every "
        parts += "🔁 " + when (r.type) {
            RecurrenceType.WEEKDAYS -> "every weekday"
            RecurrenceType.WEEKLY   -> if (r.daysOfWeek.isNotEmpty()) "every " + r.daysOfWeek.joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) } else every + "week" + if (r.interval > 1) "s" else ""
            RecurrenceType.DAILY    -> every + if (r.interval > 1) "days" else "day"
            RecurrenceType.MONTHLY  -> every + if (r.interval > 1) "months" else "month"
            RecurrenceType.YEARLY   -> every + if (r.interval > 1) "years" else "year"
            RecurrenceType.HOURLY   -> every + "hour"
        }
    }
    t.reminderAt?.let { parts += "🔔 " + day(it.toLocalDate()) + " " + it.toLocalTime().format(timeFmt) }
    when (t.priority) {
        TaskPriority.HIGH -> parts += "🚩 High"; TaskPriority.MEDIUM -> parts += "🚩 Medium"; TaskPriority.LOW -> parts += "🚩 Low"; TaskPriority.NONE -> Unit
    }
    if (t.tagNames.isNotEmpty()) parts += t.tagNames.joinToString(" ") { "#$it" }
    if (t.checklist.isNotEmpty()) parts += "☑ ${t.checklist.size} items"
    return parts.joinToString(" · ")
}

/** Created / updated / skipped, with what happened, after an import. */
@Composable
fun ImportSummaryCard(s: ContentWriter.Summary) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(s.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            s.lines.take(12).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            if (s.lines.size > 12) Text("…and ${s.lines.size - 12} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
