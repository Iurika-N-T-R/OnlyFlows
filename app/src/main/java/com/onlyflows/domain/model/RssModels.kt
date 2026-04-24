package com.onlyflows.domain.model

import java.time.LocalDateTime

data class RssFeed(
    val id: String,
    val name: String,
    val url: String,
    val colorHex: String,
    val keywords: List<String>,
    val sortOrder: Int,
)

data class RssArticle(
    val id: String,
    val feedId: String,
    val feedName: String,
    val feedColor: String,
    val title: String,
    val link: String,
    val description: String,
    val author: String,
    val pubDate: String,
    val isRead: Boolean,
    val savedAt: LocalDateTime,
)
