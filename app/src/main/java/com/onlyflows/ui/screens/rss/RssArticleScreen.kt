package com.onlyflows.ui.screens.rss

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.RssArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────

class RssArticleViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as OnlyFlowsApp).container.rssRepository
    private val _article = MutableStateFlow<RssArticle?>(null)
    val article: StateFlow<RssArticle?> = _article

    fun load(articleId: String) {
        viewModelScope.launch {
            repo.observeArticles(null).collect { list ->
                _article.value = list.find { it.id == articleId }
            }
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssArticleScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: RssArticleViewModel = viewModel(),
) {
    val context = LocalContext.current
    val article by viewModel.article.collectAsState()

    LaunchedEffect(articleId) { viewModel.load(articleId) }

    val feedColor = article?.let {
        runCatching { Color(android.graphics.Color.parseColor(it.feedColor)) }.getOrElse { Color(0xFFFF6B35) }
    } ?: Color(0xFFFF6B35)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        article?.feedName ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = feedColor,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    article?.link?.let { link ->
                        IconButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                        }) {
                            Icon(Icons.Outlined.OpenInBrowser, "Open in browser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val a = article
        if (a == null) {
            Box(Modifier.fillMaxSize().padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Title
                Text(
                    text = a.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                // Meta: author + date
                if (a.author.isNotBlank() || a.pubDate.isNotBlank()) {
                    Text(
                        text = listOfNotNull(
                            a.author.takeIf { it.isNotBlank() },
                            a.pubDate.takeIf { it.isNotBlank() }?.let { pubDate ->
                runCatching {
                    val zdt = java.time.ZonedDateTime.parse(pubDate, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                    zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.getDefault()))
                }.getOrElse { pubDate.take(10) }
            },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                    )
                }

                HorizontalDivider()

                // Content
                if (a.description.isNotBlank()) {
                    Text(
                        text = a.description,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    )
                } else {
                    Text(
                        text = "No content available. Open the article in your browser to read the full text.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Open in browser button
                OutlinedButton(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.link))) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Read full article")
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
