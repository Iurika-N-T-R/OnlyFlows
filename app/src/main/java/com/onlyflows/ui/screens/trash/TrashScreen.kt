package com.onlyflows.ui.screens.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.ui.components.EmptyState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onOpenDrawer: () -> Unit   = {},
    viewModel: TrashViewModel  = viewModel(),
) {
    val state         by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val use24h          = com.onlyflows.ui.theme.LocalUse24hFormat.current
    val dateFmt        = DateTimeFormatter.ofPattern(if (use24h) "d MMM yyyy, HH:mm" else "d MMM yyyy, hh:mm a")

    if (state.showConfirmEmpty) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(TrashEvent.HideConfirmEmpty) },
            title   = { Text("Vider la corbeille ?") },
            text    = { Text("${state.tasks.size} tâche(s) seront supprimées définitivement. Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(TrashEvent.EmptyTrash) },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Vider") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(TrashEvent.HideConfirmEmpty) }) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Corbeille",
                        style      = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, "Menu")
                    }
                },
                actions = {
                    if (state.tasks.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.onEvent(TrashEvent.ShowConfirmEmpty) },
                            colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("Vider")
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        if (!state.isLoading && state.tasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EmptyState(
                    emoji    = "🗑️",
                    title    = "Corbeille vide",
                    subtitle = "Les tâches supprimées apparaissent ici.",
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.tasks, key = { it.id }) { task ->
                    ListItem(
                        headlineContent = {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        supportingContent = task.deletedAt?.let { deleted ->
                            {
                                Text(
                                    "Supprimée le ${deleted.format(dateFmt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.onEvent(TrashEvent.Restore(task.id)) }) {
                                    Icon(
                                        Icons.Outlined.RestoreFromTrash,
                                        contentDescription = "Restaurer",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { viewModel.onEvent(TrashEvent.DeleteForever(task.id)) }) {
                                    Icon(
                                        Icons.Outlined.DeleteForever,
                                        contentDescription = "Supprimer définitivement",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}
