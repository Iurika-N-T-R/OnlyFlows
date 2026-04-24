package com.onlyflows.data.repository

import android.content.Context
import com.onlyflows.data.local.dao.RssArticleDao
import com.onlyflows.data.local.dao.RssFeedDao
import com.onlyflows.data.local.entity.RssFeedEntity
import com.onlyflows.data.rss.RssSyncWorker
import com.onlyflows.domain.model.RssArticle
import com.onlyflows.domain.model.RssFeed
import com.onlyflows.domain.repository.RssRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.time.LocalDateTime
import java.util.UUID

class RssRepositoryImpl(
    private val feedDao: RssFeedDao,
    private val articleDao: RssArticleDao,
    private val context: Context,
) : RssRepository {

    override fun observeFeeds(): Flow<List<RssFeed>> =
        feedDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeArticles(feedId: String?): Flow<List<RssArticle>> =
        combine(
            feedDao.observeAll(),
            articleDao.observeAll(feedId),
        ) { feeds, articles ->
            val feedMap = feeds.associateBy { it.id }
            articles.mapNotNull { a ->
                val feed = feedMap[a.feedId] ?: return@mapNotNull null
                a.toDomain(feed)
            }
        }

    override fun observeUnreadArticles(feedId: String?): Flow<List<RssArticle>> =
        combine(
            feedDao.observeAll(),
            articleDao.observeUnread(feedId),
        ) { feeds, articles ->
            val feedMap = feeds.associateBy { it.id }
            articles.mapNotNull { a ->
                val feed = feedMap[a.feedId] ?: return@mapNotNull null
                a.toDomain(feed)
            }
        }

    override fun observeUnreadCount(feedId: String?): Flow<Int> =
        articleDao.observeUnreadCount(feedId)

    override suspend fun addFeed(name: String, url: String, keywords: List<String>, colorHex: String) {
        val id = UUID.randomUUID().toString()
        feedDao.insert(
            RssFeedEntity(
                id        = id,
                name      = name.trim(),
                url       = url.trim(),
                colorHex  = colorHex,
                keywords  = JSONArray(keywords).toString(),
                createdAt = LocalDateTime.now().toString(),
            )
        )
        // Immediate first sync
        try { RssSyncWorker.syncFeedNow(context, id, url.trim(), JSONArray(keywords).toString()) }
        catch (_: Exception) {}
    }

    override suspend fun updateFeed(feed: RssFeed) {
        val existing = feedDao.getById(feed.id) ?: return
        feedDao.update(existing.copy(
            name     = feed.name.trim(),
            url      = feed.url.trim(),
            colorHex = feed.colorHex,
            keywords = JSONArray(feed.keywords).toString(),
        ))
    }

    override suspend fun deleteFeed(id: String) {
        feedDao.softDelete(id, LocalDateTime.now().toString())
        articleDao.deleteByFeed(id)
    }

    override suspend fun setRead(articleId: String, read: Boolean) =
        articleDao.setRead(articleId, read)

    override suspend fun markAllRead(feedId: String?) =
        articleDao.markAllRead(feedId)

    override suspend fun syncFeed(feedId: String) {
        val feed = feedDao.getById(feedId) ?: return
        RssSyncWorker.syncFeedNow(context, feed.id, feed.url, feed.keywords)
    }

    override suspend fun syncAll() {
        feedDao.getAll().forEach { feed ->
            try { RssSyncWorker.syncFeedNow(context, feed.id, feed.url, feed.keywords) }
            catch (_: Exception) {}
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    private fun RssFeedEntity.toDomain() = RssFeed(
        id       = id,
        name     = name,
        url      = url,
        colorHex = colorHex,
        keywords = try {
            val arr = JSONArray(keywords)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() },
        sortOrder = sortOrder,
    )

    private fun com.onlyflows.data.local.entity.RssArticleEntity.toDomain(feed: RssFeedEntity) = RssArticle(
        id          = id,
        feedId      = feedId,
        feedName    = feed.name,
        feedColor   = feed.colorHex,
        title       = title,
        link        = link,
        description = description,
        author      = author,
        pubDate     = pubDate,
        isRead      = isRead,
        savedAt     = runCatching { LocalDateTime.parse(savedAt) }.getOrElse { LocalDateTime.now() },
    )
}
