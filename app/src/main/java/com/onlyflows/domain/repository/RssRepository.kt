package com.onlyflows.domain.repository

import com.onlyflows.domain.model.RssArticle
import com.onlyflows.domain.model.RssFeed
import kotlinx.coroutines.flow.Flow

interface RssRepository {
    fun observeFeeds(): Flow<List<RssFeed>>
    fun observeArticles(feedId: String? = null): Flow<List<RssArticle>>
    fun observeUnreadArticles(feedId: String? = null): Flow<List<RssArticle>>
    fun observeUnreadCount(feedId: String? = null): Flow<Int>

    suspend fun addFeed(name: String, url: String, keywords: List<String>, colorHex: String)
    suspend fun updateFeed(feed: RssFeed)
    suspend fun deleteFeed(id: String)

    suspend fun setRead(articleId: String, read: Boolean)
    suspend fun markAllRead(feedId: String? = null)

    suspend fun syncFeed(feedId: String)
    suspend fun syncAll()
}
