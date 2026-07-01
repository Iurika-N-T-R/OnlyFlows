package com.onlyflows.ui.screens.archived

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit         = {},
    viewModel: ArchivedViewModel     = viewModel(),
) {
    val state          by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Archived",
                        style      = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, "Menu")
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
        if (!state.isLoading && state.projects.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EmptyState(
                    emoji    = "🗄️",
                    title    = "No archived projects",
                    subtitle = "Archived projects appear here.",
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
                items(state.projects, key = { it.id }) { project ->
                    val color = remember(project.colorHex) {
                        try { Color(android.graphics.Color.parseColor(project.colorHex)) }
                        catch (e: Exception) { Color(0xFF4A90D9) }
                    }
                    ListItem(
                        leadingContent = {
                            Canvas(Modifier.size(12.dp)) { drawCircle(color) }
                        },
                        headlineContent = {
                            Text(project.title, style = MaterialTheme.typography.bodyMedium)
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.unarchive(project.id) }) {
                                Icon(
                                    Icons.Outlined.Unarchive,
                                    contentDescription = "Unarchive",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier.clickable { onProjectClick(project.id) },
                        colors   = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
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
