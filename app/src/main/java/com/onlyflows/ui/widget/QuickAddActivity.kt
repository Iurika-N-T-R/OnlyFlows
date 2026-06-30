package com.onlyflows.ui.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.Task
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.launch

/** Floating popup (dialog theme) to add a task from the widget without opening the app.
 *  New tasks land in Inbox. ponytail: Inbox only; honor the widget's current filter if asked. */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as OnlyFlowsApp).container.taskRepository

        setContent {
            OnlyFlowsTheme {
                val scope = rememberCoroutineScope()
                var text  by remember { mutableStateOf("") }
                val focus = remember { FocusRequester() }

                fun save() {
                    val title = text.trim()
                    if (title.isEmpty()) { finish(); return }
                    scope.launch {
                        repo.insert(Task(id = "task_${System.currentTimeMillis()}", title = title))
                        finish()
                    }
                }

                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
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
                        Spacer(Modifier.height(12.dp))
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
}
