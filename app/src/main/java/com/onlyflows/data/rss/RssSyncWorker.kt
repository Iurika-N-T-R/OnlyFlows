package com.onlyflows.data.rss

import android.content.Context
import androidx.work.*
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.entity.RssArticleEntity
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class RssSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val feeds = db.rssFeedDao().getAll()
            feeds.forEach { feed ->
                try { syncFeed(db, feed.id, feed.url, feed.keywords) } catch (_: Exception) {}
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "rss_sync"

        fun schedule(context: Context, intervalMinutes: Long) {
            val request = PeriodicWorkRequestBuilder<RssSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        suspend fun syncFeedNow(context: Context, feedId: String, url: String, keywords: String) {
            syncFeed(AppDatabase.getInstance(context), feedId, url, keywords)
        }

        private suspend fun syncFeed(db: AppDatabase, feedId: String, url: String, keywordsJson: String) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout    = 15_000
            connection.setRequestProperty("User-Agent", "OnlyFlows/1.0 RSS Reader")
            try {
                val stream = connection.inputStream
                val parsed = RssParser.parse(stream)
                stream.close()

                val keywords = parseKeywords(keywordsJson)
                val now = LocalDateTime.now().toString()

                val existing = db.rssArticleDao().getIdsByFeed(feedId).toHashSet()
                val toInsert = parsed
                    .filter { a -> keywords.isEmpty() || keywords.any { kw ->
                        a.title.contains(kw, ignoreCase = true) ||
                        a.description.contains(kw, ignoreCase = true)
                    }}
                    .map { a ->
                        RssArticleEntity(
                            id          = RssParser.articleId(feedId, a.link.ifBlank { a.title }),
                            feedId      = feedId,
                            title       = a.title,
                            link        = a.link,
                            description = a.description,
                            author      = a.author,
                            pubDate     = a.pubDate,
                            isRead      = false,
                            savedAt     = now,
                        )
                    }
                    .filter { it.id !in existing }

                if (toInsert.isNotEmpty()) {
                    db.rssArticleDao().insertAll(toInsert)
                }
            } finally {
                connection.disconnect()
            }
        }

        private fun parseKeywords(json: String): List<String> {
            return try {
                val arr = org.json.JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
            } catch (_: Exception) { emptyList() }
        }
    }
}
