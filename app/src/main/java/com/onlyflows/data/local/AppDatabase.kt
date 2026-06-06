package com.onlyflows.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.onlyflows.data.local.dao.*
import com.onlyflows.data.local.dao.DateTrackerDao
import com.onlyflows.data.local.dao.GoalDao
import com.onlyflows.data.local.dao.GoalLogDao
import com.onlyflows.data.local.dao.ActionLogDao
import com.onlyflows.data.local.dao.SavedLocationDao
import com.onlyflows.data.local.dao.ReminderDao
import com.onlyflows.data.local.dao.AttachmentDao
import com.onlyflows.data.local.entity.*
import com.onlyflows.data.local.entity.DateTrackerEntity
import com.onlyflows.data.local.entity.RssFeedEntity
import com.onlyflows.data.local.entity.RssArticleEntity
import com.onlyflows.data.local.dao.RssFeedDao
import com.onlyflows.data.local.dao.RssArticleDao
import com.onlyflows.data.local.entity.GoalEntity
import com.onlyflows.data.local.entity.GoalLogEntity
import com.onlyflows.data.local.entity.ActionLogEntity
import com.onlyflows.data.local.entity.SavedLocationEntity
import com.onlyflows.data.local.entity.NoteEntity
import com.onlyflows.data.local.entity.StatusLogEntity
import com.onlyflows.data.local.entity.CommentEntity
import com.onlyflows.data.local.entity.TimeEntryEntity
import com.onlyflows.data.local.entity.ReminderEntity
import com.onlyflows.data.local.entity.AttachmentEntity
import com.onlyflows.data.mock.MockData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        ChecklistItemEntity::class,
        ProjectEntity::class,
        SectionEntity::class,
        TagEntity::class,
        FolderEntity::class,
        StatusLogEntity::class,
        NoteEntity::class,
        TimeEntryEntity::class,
        CommentEntity::class,
        ReminderEntity::class,
        AttachmentEntity::class,
        ActionLogEntity::class,
        SavedLocationEntity::class,
        SyncMetaEntity::class,
        DeletedEntityEntity::class,
        GoalEntity::class,
        GoalLogEntity::class,
        DateTrackerEntity::class,
        RssFeedEntity::class,
        RssArticleEntity::class,
    ],
    version = 31,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun projectDao(): ProjectDao
    abstract fun sectionDao(): SectionDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao
    abstract fun statusLogDao(): StatusLogDao
    abstract fun noteDao(): NoteDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun commentDao(): CommentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun actionLogDao(): ActionLogDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun goalDao(): GoalDao
    abstract fun goalLogDao(): GoalLogDao
    abstract fun dateTrackerDao(): DateTrackerDao
    abstract fun syncDao(): SyncDao
    abstract fun rssFeedDao(): RssFeedDao
    abstract fun rssArticleDao(): RssArticleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val migration27to28 = Migration(27, 28) { db ->
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS date_trackers (
                            id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            emoji TEXT NOT NULL,
                            targetDate TEXT NOT NULL,
                            category TEXT NOT NULL,
                            isAnnual INTEGER NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            createdAt TEXT NOT NULL,
                            deletedAt TEXT
                        )"""
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_date_trackers_deletedAt ON date_trackers(deletedAt)"
                    )
                }
                val migration28to29 = Migration(28, 29) { db ->
                    db.execSQL("""CREATE TABLE IF NOT EXISTS rss_feeds (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        createdAt TEXT NOT NULL,
                        deletedAt TEXT
                    )""")
                    db.execSQL("""CREATE TABLE IF NOT EXISTS rss_articles (
                        id TEXT NOT NULL PRIMARY KEY,
                        feedId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        link TEXT NOT NULL,
                        description TEXT NOT NULL,
                        author TEXT NOT NULL,
                        pubDate TEXT NOT NULL,
                        isRead INTEGER NOT NULL,
                        savedAt TEXT NOT NULL
                    )""")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_rss_articles_feedId ON rss_articles(feedId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_rss_articles_isRead ON rss_articles(isRead)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_rss_articles_pubDate ON rss_articles(pubDate)")
                }
                val migration29to30 = Migration(29, 30) { db ->
                    db.execSQL("ALTER TABLE projects ADD COLUMN tagIds TEXT NOT NULL DEFAULT ''")
                }
                val migration30to31 = Migration(30, 31) { db ->
                    db.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'NONE'")
                }
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "onlyflows.db",
                )
                .addMigrations(migration27to28, migration28to29, migration29to30, migration30to31)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(SeedCallback())
                .build()
                .also { INSTANCE = it }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }

    // ── Seed the DB on first creation ─────────────────────────────────────────

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            doSeed()
        }

        // Called instead of onCreate when fallbackToDestructiveMigration wipes the DB
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            doSeed()
        }

        private fun doSeed() {
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { seedDatabase(it) }
            }
        }
    }
}

suspend fun seedDatabase(db: AppDatabase) {
    // Folders
    db.folderDao().insertAll(MockData.folders.map { it.toEntity() })

    // Tags
    db.tagDao().insertAll(MockData.tags.map { it.toEntity() })

    // Projects + sections
    MockData.projects.forEach { project ->
        db.projectDao().insert(project.toEntity())
        db.sectionDao().insertAll(project.sections.map { it.toEntity() })
    }

    // Tasks + checklist items
    MockData.tasks.forEach { task ->
        db.taskDao().insert(task.toEntity())
        if (task.checklistItems.isNotEmpty()) {
            db.checklistItemDao().insertAll(task.checklistItems.map { it.toEntity() })
        }
    }
}
