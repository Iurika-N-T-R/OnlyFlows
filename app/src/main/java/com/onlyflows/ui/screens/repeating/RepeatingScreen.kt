package com.onlyflows.ui.screens.repeating

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.R
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.TaskItem

private val RepeatBlue = Color(0xFF4A90D9)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RepeatingScreen(
    onTaskClick: (String) -> Unit = {},
    onBack: () -> Unit            = {},
    viewModel: RepeatingViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Outlined.Autorenew,
                            contentDescription = null,
                            tint               = RepeatBlue,
                            modifier           = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text       = "Repeating",
                            style      = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        if (state.totalCount > 0) {
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text       = "${state.totalCount}",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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

        if (!state.isLoading && state.groups.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                EmptyState(
                    icon     = Icons.Outlined.Autorenew,
                    iconTint = RepeatBlue.copy(alpha = 0.6f),
                    title    = "No repeating to-dos",
                    subtitle = "Tasks you set to repeat gather here, so you can review the whole set at once.",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.groups.forEach { group ->
                item(key = "group_${group.project?.id ?: "inbox"}") {
                    GroupRow(group.project)
                }
                items(
                    count = group.tasks.size,
                    key   = { i -> "repeat_${group.tasks[i].id}" },
                ) { index ->
                    val task = group.tasks[index]
                    TaskItem(
                        task             = task,
                        tags             = state.tags,
                        checkboxColor    = group.project?.let { parseProjectColor(it.colorHex) } ?: RepeatBlue,
                        onToggleComplete = { viewModel.toggleComplete(task.id) },
                        onEditClick      = { onTaskClick(it) },
                        modifier         = Modifier.animateItem(),
                    )
                    if (index < group.tasks.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 54.dp, end = 20.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(project: Project?) {
    val color = project?.let { parseProjectColor(it.colorHex) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
        ) {
            if (project == null) {
                Icon(
                    painter            = painterResource(R.drawable.ic_nav_inbox),
                    contentDescription = null,
                    tint               = Color.Unspecified,
                    modifier           = Modifier.size(18.dp),
                )
            } else if (project.iconEmoji.isNotBlank()) {
                com.onlyflows.ui.components.ProjectGlyph(value = project.iconEmoji, tint = color, size = 18.dp)
            } else {
                Icon(
                    imageVector = when (project.type) {
                        ProjectType.LIST -> Icons.AutoMirrored.Outlined.FormatListBulleted
                        ProjectType.NOTE -> Icons.AutoMirrored.Outlined.Article
                        else             -> Icons.Outlined.FolderOpen
                    },
                    contentDescription = null,
                    tint               = color,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text       = project?.title ?: "Inbox",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(
            modifier  = Modifier.padding(horizontal = 20.dp),
            color     = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
        )
    }
}

private fun parseProjectColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(RepeatBlue)
