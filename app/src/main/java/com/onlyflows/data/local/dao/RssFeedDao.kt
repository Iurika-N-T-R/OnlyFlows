package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.RssFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssFeedDao {
    @Query("SELECT * FROM rss_feeds WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<RssFeedEntity>>

    @Query("SELECT * FROM rss_feeds WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getAll(): List<RssFeedEntity>

    @Query("SELECT * FROM rss_feeds WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RssFeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feed: RssFeedEntity)

    @Update
    suspend fun update(feed: RssFeedEntity)

    @Query("UPDATE rss_feeds SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)
}
