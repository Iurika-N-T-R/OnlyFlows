package com.onlyflows.ui.widgets.countdown

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.theme.OnlyFlowsTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CountdownConfigActivity : ComponentActivity() {

    private val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        // Return RESULT_CANCELED by default so widget isn't placed if user backs out
        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            OnlyFlowsTheme {
                CountdownConfigScreen(
                    onSave = { name, date ->
                        lifecycleScope.launch {
                            val manager = GlanceAppWidgetManager(this@CountdownConfigActivity)
                            val glanceId = manager.getGlanceIdBy(appWidgetId)
                            val glanceIdHash = glanceId.hashCode()
                            WidgetPreferences(this@CountdownConfigActivity)
                                .saveCountdownConfig(glanceIdHash, name, date.toString())
                            CountdownWidgetProvider().update(this@CountdownConfigActivity, glanceId)
                            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(RESULT_OK, result)
                            finish()
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownConfigScreen(
    onSave: (name: String, date: LocalDate) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Countdown") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                        onClick = { selectedDate?.let { onSave(name.trim(), it) } },
                        enabled = name.isNotBlank() && selectedDate != null,
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Event name") },
                placeholder = { Text("e.g. Fin d'année") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    selectedDate?.let {
                        cal.set(it.year, it.monthValue - 1, it.dayOfMonth)
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            selectedDate = LocalDate.of(year, month + 1, day)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH),
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedDate?.format(displayFormatter) ?: "Pick a date")
            }

            if (selectedDate != null && name.isNotBlank()) {
                val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), selectedDate!!)
                Text(
                    text = when {
                        days > 0  -> "$days jours restants"
                        days == 0L -> "Aujourd'hui !"
                        else       -> "${-days} jours passés"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
