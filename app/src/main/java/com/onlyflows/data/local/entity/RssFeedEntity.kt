package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_feeds")
data class RssFeedEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val colorHex: String = "#FF6B35",
    val keywords: String = "[]",   // JSON array of keyword strings
    val sortOrder: Int = 0,
    val createdAt: String,
    val deletedAt: String? = null,
)
