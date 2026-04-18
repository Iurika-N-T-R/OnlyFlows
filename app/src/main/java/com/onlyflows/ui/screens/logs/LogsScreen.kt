package com.onlyflows.ui.screens.logs

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.data.debug.LogEntry
import com.onlyflows.data.debug.LogLevel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit          = {},
    viewModel: LogsViewModel    = viewModel(),
) {
    val state    by viewModel.uiState.collectAsStateWithLifecycle()
    val entries  by viewModel.filtered.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title            = { Text("Clear logs") },
            text             = { Text("All log entries will be removed from memory.") },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.onEvent(LogsEvent.Clear)
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton    = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BugReport, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Logs", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = viewModel.buildShareText()
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type    = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    putExtra(Intent.EXTRA_SUBJECT, "OnlyFlows debug logs")
                                },
                                "Share logs"
                            )
                        )
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share logs")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear logs")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter chips
            LazyRow(
                contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.filter == null,
                        onClick  = { viewModel.onEvent(LogsEvent.SetFilter(null)) },
                        label    = { Text("All") },
                    )
                }
                items(LogLevel.entries) { level ->
                    FilterChip(
                        selected = state.filter == level,
                        onClick  = {
                            viewModel.onEvent(LogsEvent.SetFilter(if (state.filter == level) null else level))
                        },
                        label    = { Text(level.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .then(Modifier),
                            ) {}
                        },
                    )
                }
            }

            HorizontalDivider()

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.BugReport, contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("No logs yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { "${it.timestamp}_${it.tag}_${it.message}" }) { entry ->
                        LogEntryRow(entry)
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val levelColor = when (entry.level) {
        LogLevel.ERROR   -> MaterialTheme.colorScheme.error
        LogLevel.WARN    -> Color(0xFFFF9800)
        LogLevel.INFO    -> MaterialTheme.colorScheme.primary
        LogLevel.DEBUG   -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.throwable != null) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = entry.level.name.take(1),
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.width(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = entry.timestamp.format(timeFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = entry.tag,
                style = MaterialTheme.typography.labelSmall,
                color = levelColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(max = 100.dp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text  = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 84.dp),
        )
        if (expanded && entry.throwable != null) {
            Spacer(Modifier.height(4.dp))
            Surface(
                color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape  = MaterialTheme.shapes.small,
                modifier = Modifier.padding(start = 84.dp).fillMaxWidth(),
            ) {
                Text(
                    text   = entry.throwable,
                    style  = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color  = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
