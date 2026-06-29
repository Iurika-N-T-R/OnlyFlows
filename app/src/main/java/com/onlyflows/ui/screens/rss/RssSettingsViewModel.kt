package com.onlyflows.ui.screens.rss

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.preferences.AppPreferences
import com.onlyflows.data.rss.RssSyncWorker
import com.onlyflows.domain.model.RssFeed
import com.onlyflows.data.repository.RssRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RssSettingsUiState(
    val feeds: List<RssFeed> = emptyList(),
    val refreshIntervalMinutes: Long = 30L,
)

class RssSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as OnlyFlowsApp).container
    private val repo: RssRepository = container.rssRepository
    private val prefs: AppPreferences = container.appPreferences

    val uiState: StateFlow<RssSettingsUiState> = combine(
        repo.observeFeeds(),
        prefs.rssRefreshIntervalMinutes,
    ) { feeds: List<RssFeed>, interval: Long ->
        RssSettingsUiState(feeds = feeds, refreshIntervalMinutes = interval)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RssSettingsUiState())

    fun addFeed(name: String, url: String, keywords: List<String>, colorHex: String) {
        viewModelScope.launch { repo.addFeed(name, url, keywords, colorHex) }
    }

    fun updateFeed(feed: com.onlyflows.domain.model.RssFeed) {
        viewModelScope.launch { try { repo.updateFeed(feed) } catch (_: Exception) {} }
    }

    fun deleteFeed(id: String) {
        viewModelScope.launch { repo.deleteFeed(id) }
    }

    fun syncFeed(id: String) {
        viewModelScope.launch { try { repo.syncFeed(id) } catch (_: Exception) {} }
    }

    fun setRefreshInterval(minutes: Long) {
        viewModelScope.launch {
            prefs.setRssRefreshInterval(minutes)
            if (minutes > 0L) {
                RssSyncWorker.schedule(getApplication(), minutes)
            } else {
                RssSyncWorker.cancel(getApplication())
            }
        }
    }
}
