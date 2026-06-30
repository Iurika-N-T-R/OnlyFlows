package com.onlyflows.ui.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Floating quick-add from the widget: title + When + Priority + Tags. Destination defaults to the widget's filter. */
class WidgetQuickAddActivity : ComponentActivity() {

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as OnlyFlowsApp).container

        val filterType = intent.getStringExtra(EXTRA_FILTER_TYPE) ?: "INBOX"
        val filterId   = intent.getStringExtra(EXTRA_FILTER_ID) ?: ""
        val defaultGroup = when (filterType) {
            "TODAY"   -> TaskGroup.TODAY
            "ANYTIME" -> TaskGroup.ANYTIME
            "SOMEDAY" -> TaskGroup.SOMEDAY
            else      -> null   // INBOX / ALL / UPCOMING / PROJECT
        }
        val projectId = if (filterType == "PROJECT") filterId.ifEmpty { null } else null

        setContent {
            OnlyFlowsTheme {
                val scope = rememberCoroutineScope()
                var text     by remember { mutableStateOf("") }
                var group    by remember { mutableStateOf(defaultGroup) }
                var priority by remember { mutableStateOf(TaskPriority.NONE) }
                var selectedTags by remember { mutableStateOf(setOf<String>()) }
                var allTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
                val focus = remember { FocusRequester() }

                LaunchedEffect(Unit) { allTags = container.tagRepository.observeAll().first() }

                fun save() {
                    val title = text.trim()
                    if (title.isEmpty()) { finish(); return }
                    scope.launch {
                        container.taskRepository.insert(
                            Task(
                                id             = "task_${System.currentTimeMillis()}",
                                title          = title,
                                scheduledGroup = group,
                                projectId      = projectId,
                                priority       = priority,
                                tagIds         = selectedTags.toList(),
                            )
                        )
                        finish()
                    }
                }

                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState()).padding(20.dp),
                    ) {
                        Text("Nouvelle tâche", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value           = text,
                            onValueChange   = { text = it },
                            placeholder     = { Text("Titre…") },
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { save() }),
                            modifier        = Modifier.fillMaxWidth().focusRequester(focus),
                        )

                        Spacer(Modifier.height(14.dp))
                        Label("Quand")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(null to "Inbox", TaskGroup.TODAY to "Today", TaskGroup.ANYTIME to "Anytime", TaskGroup.SOMEDAY to "Someday")
                                .forEach { (g, label) ->
                                    FilterChip(selected = group == g, onClick = { group = g }, label = { Text(label) })
                                }
                        }

                        Spacer(Modifier.height(10.dp))
                        Label("Priorité")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(TaskPriority.NONE to "Aucune", TaskPriority.LOW to "Basse", TaskPriority.MEDIUM to "Moyenne", TaskPriority.HIGH to "Haute")
                                .forEach { (p, label) ->
                                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(label) })
                                }
                        }

                        if (allTags.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Label("Tags")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                allTags.forEach { tag ->
                                    FilterChip(
                                        selected = tag.id in selectedTags,
                                        onClick  = {
                                            selectedTags = if (tag.id in selectedTags) selectedTags - tag.id else selectedTags + tag.id
                                        },
                                        label = { Text(tag.name) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { finish() }) { Text("Annuler") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { save() }, enabled = text.isNotBlank()) { Text("Ajouter") }
                        }
                    }
                }
                LaunchedEffect(Unit) { focus.requestFocus() }
            }
        }
    }

    companion object {
        const val EXTRA_FILTER_TYPE = "extra_filter_type"
        const val EXTRA_FILTER_ID   = "extra_filter_id"
    }
}

@androidx.compose.runtime.Composable
private fun Label(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
