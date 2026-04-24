package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.RssArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssArticleDao {

    @Query("""
        SELECT * FROM rss_articles
        WHERE (:feedId IS NULL OR feedId = :feedId)
        ORDER BY pubDate DESC, savedAt DESC
    """)
    fun observeAll(feedId: String? = null): Flow<List<RssArticleEntity>>

    @Query("""
        SELECT * FROM rss_articles
        WHERE (:feedId IS NULL OR feedId = :feedId)
          AND isRead = 0
        ORDER BY pubDate DESC, savedAt DESC
    """)
    fun observeUnread(feedId: String? = null): Flow<List<RssArticleEntity>>

    @Query("SELECT * FROM rss_articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RssArticleEntity?

    @Query("SELECT id FROM rss_articles WHERE feedId = :feedId")
    suspend fun getIdsByFeed(feedId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)  // IGNORE = skip already-saved articles
    suspend fun insertAll(articles: List<RssArticleEntity>)

    @Query("UPDATE rss_articles SET isRead = :read WHERE id = :id")
    suspend fun setRead(id: String, read: Boolean)

    @Query("UPDATE rss_articles SET isRead = 1 WHERE feedId = :feedId OR :feedId IS NULL")
    suspend fun markAllRead(feedId: String? = null)

    @Query("DELETE FROM rss_articles WHERE feedId = :feedId")
    suspend fun deleteByFeed(feedId: String)

    @Query("SELECT COUNT(*) FROM rss_articles WHERE isRead = 0 AND (:feedId IS NULL OR feedId = :feedId)")
    fun observeUnreadCount(feedId: String? = null): Flow<Int>
}
