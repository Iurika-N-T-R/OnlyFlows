package com.onlyflows.ui.screens.rss

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.RssArticle
import com.onlyflows.domain.model.RssFeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssScreen(
    onOpenDrawer: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onArticleClick: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: RssViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("RSS", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, "Menu")
                    }
                },
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.onEvent(RssEvent.Sync) }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Sync")
                        }
                    }
                    if (state.unreadCount > 0) {
                        IconButton(onClick = { viewModel.onEvent(RssEvent.MarkAllRead(state.selectedFeedId)) }) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all read")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "RSS Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(RssEvent.SetSearch(it)) },
                placeholder = { Text("Rechercher…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onEvent(RssEvent.SetSearch("")) }) {
                            Icon(Icons.Outlined.Close, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
            )

            // Feed filter chips
            if (state.feeds.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FeedChip(
                            label = "Tous",
                            color = MaterialTheme.colorScheme.primary,
                            selected = state.selectedFeedId == null,
                            unreadCount = state.unreadCount,
                            onClick = { viewModel.onEvent(RssEvent.SelectFeed(null)) },
                        )
                    }
                    items(state.feeds) { feed ->
                        FeedChip(
                            label = feed.name,
                            color = parseColor(feed.colorHex),
                            selected = state.selectedFeedId == feed.id,
                            unreadCount = 0,
                            onClick = { viewModel.onEvent(RssEvent.SelectFeed(feed.id)) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Unread toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.articles.size} article${if (state.articles.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Non lus seulement",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = state.showUnreadOnly,
                    onCheckedChange = { viewModel.onEvent(RssEvent.ToggleUnreadOnly(it)) },
                    modifier = Modifier.height(24.dp),
                )
            }

            HorizontalDivider()

            if (state.articles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.RssFeed,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (state.feeds.isEmpty())
                                "Ajoutez des flux RSS dans les paramètres"
                            else "Aucun article",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.articles, key = { it.id }) { article ->
                        ArticleRow(
                            article      = article,
                            onToggleRead = { viewModel.onEvent(RssEvent.MarkRead(article.id, !article.isRead)) },
                            onClick      = {
                                viewModel.onEvent(RssEvent.MarkRead(article.id, true))
                                onArticleClick(article.link, article.title)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedChip(
    label: String,
    color: Color,
    selected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg",
    )
    val borderColor by animateColorAsState(
        if (selected) color else Color.Transparent,
        label = "chip_border",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.height(32.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
            if (unreadCount > 0) {
                Badge { Text(unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun ArticleRow(
    article: RssArticle,
    onToggleRead: () -> Unit,
    onClick: () -> Unit = {},
) {
    val alpha = if (article.isRead) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Feed color dot
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(parseColor(article.feedColor).copy(alpha = alpha)),
        )

        Column(modifier = Modifier.weight(1f)) {
            // Feed name + date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = article.feedName,
                    style = MaterialTheme.typography.labelSmall,
                    color = parseColor(article.feedColor).copy(alpha = alpha),
                    maxLines = 1,
                )
                if (article.pubDate.isNotBlank()) {
                    Text(
                        text = "·  ${formatDate(article.pubDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!article.isRead) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (article.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Mark read / unread icon button
        IconButton(
            onClick = onToggleRead,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (article.isRead) Icons.Outlined.RadioButtonUnchecked
                              else Icons.Outlined.CheckCircleOutline,
                contentDescription = if (article.isRead) "Marquer non lu" else "Marquer lu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 38.dp))
}

private fun parseColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { Color(0xFFFF6B35) }

private fun formatDate(raw: String): String {
    // Best-effort: show just the date part if parseable, else return raw trimmed
    return runCatching {
        val zdt = java.time.ZonedDateTime.parse(raw, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
        zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault()))
    }.getOrElse {
        runCatching {
            val ldt = java.time.LocalDateTime.parse(raw)
            ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
        }.getOrElse { raw.take(10) }
    }
}
