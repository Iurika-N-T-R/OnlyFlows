package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rss_articles",
    indices = [Index("feedId"), Index("isRead"), Index("pubDate")],
)
data class RssArticleEntity(
    @PrimaryKey val id: String,          // md5(feedId + link) de-duplicate
    val feedId: String,
    val title: String,
    val link: String,
    val description: String = "",        // stripped HTML snippet
    val author: String = "",
    val pubDate: String = "",            // ISO-8601 string
    val isRead: Boolean = false,
    val savedAt: String,                 // when we inserted it
)
