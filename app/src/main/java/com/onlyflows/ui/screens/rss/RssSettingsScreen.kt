package com.onlyflows.ui.screens.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.RssFeed

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RssSettingsScreen(
    onBack: () -> Unit,
    viewModel: RssSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingFeed by remember { mutableStateOf<com.onlyflows.domain.model.RssFeed?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RSS Feeds", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Outlined.Add, "Add feed")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Refresh interval section
            item {
                Text(
                    "Auto refresh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                RefreshIntervalSelector(
                    currentMinutes = state.refreshIntervalMinutes,
                    onSelect = { viewModel.setRefreshInterval(it) },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "RSS Feeds (${state.feeds.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.feeds.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No feeds. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.feeds, key = { it.id }) { feed ->
                    FeedCard(
                        feed = feed,
                        onDelete = { viewModel.deleteFeed(feed.id) },
                        onSync = { viewModel.syncFeed(feed.id) },
                        onEdit = { editingFeed = feed },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddFeedSheet(
            onAdd = { name, url, keywords, color ->
                viewModel.addFeed(name, url, keywords, color)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }

    editingFeed?.let { feed ->
        EditFeedSheet(
            feed = feed,
            onSave = { updated ->
                viewModel.updateFeed(updated)
                editingFeed = null
            },
            onDismiss = { editingFeed = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshIntervalSelector(
    currentMinutes: Long,
    onSelect: (Long) -> Unit,
) {
    val options = listOf(
        0L to "Manual only",
        15L to "15 minutes",
        30L to "30 minutes",
        60L to "1 hour",
        180L to "3 hours",
        360L to "6 hours",
    )
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == currentMinutes }?.second ?: "$currentMinutes min"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Interval") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (minutes, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(minutes); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun FeedCard(
    feed: RssFeed,
    onDelete: () -> Unit,
    onSync: () -> Unit,
    onEdit: () -> Unit,
) {
    val feedColor = runCatching { Color(android.graphics.Color.parseColor(feed.colorHex)) }
        .getOrElse { Color(0xFFFF6B35) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(feedColor),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(feed.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    feed.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (feed.keywords.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        feed.keywords.take(4).forEach { kw ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(kw, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(22.dp),
                            )
                        }
                        if (feed.keywords.size > 4) {
                            Text(
                                "+${feed.keywords.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onSync) {
                Icon(Icons.Outlined.Refresh, "Sync", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, "Edit", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFeedSheet(
    onAdd: (name: String, url: String, keywords: List<String>, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url  by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf<List<String>>(emptyList()) }

    val colors = listOf("#FF6B35","#4A90D9","#39A59A","#8B5CF6","#FF9500","#FF2D55","#4DBE60","#FF6B6B")
    var selectedColor by remember { mutableStateOf(colors.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add RSS feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Feed name") },
                placeholder = { Text("ex. TechCrunch") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL RSS") },
                placeholder = { Text("https://example.com/feed.xml") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            // Keywords
            OutlinedTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                label = { Text("Filter by keyword (optional)") },
                placeholder = { Text("ex. AI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val kw = keywordInput.trim()
                            if (kw.isNotBlank() && kw !in keywords) {
                                keywords = keywords + kw
                            }
                            keywordInput = ""
                        },
                        enabled = keywordInput.isNotBlank(),
                    ) {
                        Icon(Icons.Outlined.Add, "Add")
                    }
                },
            )

            if (keywords.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    keywords.forEach { kw ->
                        InputChip(
                            selected = false,
                            onClick = { keywords = keywords - kw },
                            label = { Text(kw) },
                            trailingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(14.dp)) },
                        )
                    }
                }
            }

            // Color picker
            Text("Color", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (hex == selectedColor)
                                    Modifier.padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.4f))
                                else Modifier
                            )
                            .clickable { selectedColor = hex },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { onAdd(name.trim(), url.trim(), keywords, selectedColor) },
                    enabled = name.isNotBlank() && url.isNotBlank(),
                ) { Text("Add") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditFeedSheet(
    feed: RssFeed,
    onSave: (RssFeed) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(feed.name) }
    var url  by remember { mutableStateOf(feed.url) }
    var keywordInput by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf(feed.keywords) }

    val colors = listOf("#FF6B35","#4A90D9","#39A59A","#8B5CF6","#FF9500","#FF2D55","#4DBE60","#FF6B6B")
    var selectedColor by remember { mutableStateOf(feed.colorHex) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Feed name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL RSS") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                label = { Text("Filter by keyword (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val kw = keywordInput.trim()
                            if (kw.isNotBlank() && kw !in keywords) keywords = keywords + kw
                            keywordInput = ""
                        },
                        enabled = keywordInput.isNotBlank(),
                    ) { Icon(Icons.Outlined.Add, "Add") }
                },
            )

            if (keywords.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    keywords.forEach { kw ->
                        InputChip(
                            selected = false,
                            onClick = { keywords = keywords - kw },
                            label = { Text(kw) },
                            trailingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(14.dp)) },
                        )
                    }
                }
            }

            Text("Color", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (hex == selectedColor)
                                    Modifier.padding(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f))
                                else Modifier
                            )
                            .clickable { selectedColor = hex },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(feed.copy(name = name.trim(), url = url.trim(), keywords = keywords, colorHex = selectedColor))
                    },
                    enabled = name.isNotBlank() && url.isNotBlank(),
                ) { Text("Save") }
            }
        }
    }
}
