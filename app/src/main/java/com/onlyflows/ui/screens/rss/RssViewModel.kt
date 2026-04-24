package com.onlyflows.ui.screens.rss

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.domain.model.RssArticle
import com.onlyflows.domain.model.RssFeed
import com.onlyflows.domain.repository.RssRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RssUiState(
    val feeds: List<RssFeed> = emptyList(),
    val articles: List<RssArticle> = emptyList(),
    val selectedFeedId: String? = null,   // null = all feeds
    val showUnreadOnly: Boolean = false,
    val searchQuery: String = "",
    val unreadCount: Int = 0,
    val isSyncing: Boolean = false,
)

sealed interface RssEvent {
    data class SelectFeed(val feedId: String?) : RssEvent
    data class ToggleUnreadOnly(val value: Boolean) : RssEvent
    data class SetSearch(val query: String) : RssEvent
    data class MarkRead(val articleId: String, val read: Boolean) : RssEvent
    data class MarkAllRead(val feedId: String?) : RssEvent
    object Sync : RssEvent
}

class RssViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: RssRepository = (app as OnlyFlowsApp).container.rssRepository

    private val _selectedFeedId  = MutableStateFlow<String?>(null)
    private val _showUnreadOnly  = MutableStateFlow(false)
    private val _searchQuery     = MutableStateFlow("")
    private val _isSyncing       = MutableStateFlow(false)

    val uiState: StateFlow<RssUiState> = combine(
        repo.observeFeeds(),
        _selectedFeedId.flatMapLatest { fid ->
            if (_showUnreadOnly.value) repo.observeUnreadArticles(fid)
            else repo.observeArticles(fid)
        },
        _showUnreadOnly,
        _searchQuery,
        _isSyncing,
        repo.observeUnreadCount(null),
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val feeds       = args[0] as List<RssFeed>
        @Suppress("UNCHECKED_CAST")
        val articles    = args[1] as List<RssArticle>
        val unreadOnly  = args[2] as Boolean
        val query       = args[3] as String
        val syncing     = args[4] as Boolean
        val unreadCount = args[5] as Int

        val filtered = if (query.isBlank()) articles
        else articles.filter { a ->
            a.title.contains(query, ignoreCase = true) ||
            a.description.contains(query, ignoreCase = true) ||
            a.feedName.contains(query, ignoreCase = true)
        }

        RssUiState(
            feeds          = feeds,
            articles       = filtered,
            selectedFeedId = _selectedFeedId.value,
            showUnreadOnly = unreadOnly,
            searchQuery    = query,
            unreadCount    = unreadCount,
            isSyncing      = syncing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RssUiState())

    fun onEvent(event: RssEvent) {
        when (event) {
            is RssEvent.SelectFeed       -> _selectedFeedId.value = event.feedId
            is RssEvent.ToggleUnreadOnly -> _showUnreadOnly.value = event.value
            is RssEvent.SetSearch        -> _searchQuery.value = event.query
            is RssEvent.MarkRead         -> viewModelScope.launch {
                repo.setRead(event.articleId, event.read)
            }
            is RssEvent.MarkAllRead      -> viewModelScope.launch {
                repo.markAllRead(event.feedId)
            }
            is RssEvent.Sync             -> viewModelScope.launch {
                _isSyncing.value = true
                try { repo.syncAll() } finally { _isSyncing.value = false }
            }
        }
    }
}
