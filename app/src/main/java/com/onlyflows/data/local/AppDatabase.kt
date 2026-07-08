package com.onlyflows.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
        PairedDeviceEntity::class,
        ChecklistSectionEntity::class,
        ChecklistItemReminderEntity::class,
    ],
    version = 39,
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
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun checklistSectionDao(): ChecklistSectionDao
    abstract fun checklistItemReminderDao(): ChecklistItemReminderDao

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
                val migration31to32 = Migration(31, 32) { db ->
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS paired_devices (
                            id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            lastKnownIp TEXT NOT NULL,
                            port INTEGER NOT NULL,
                            pairedAt TEXT NOT NULL
                        )"""
                    )
                }
                val migration32to33 = Migration(32, 33) { db ->
                    db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt TEXT")
                    // Backfill: existing completed tasks get updatedAt as completedAt so they
                    // don't all flood "completed today" sections forever.
                    db.execSQL("UPDATE tasks SET completedAt = updatedAt WHERE isCompleted = 1")
                }
                val migration33to34 = Migration(33, 34) { db ->
                    db.execSQL("ALTER TABLE tasks ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'NONE'")
                    db.execSQL("ALTER TABLE tasks ADD COLUMN impact TEXT NOT NULL DEFAULT 'NONE'")
                }
                val migration34to35 = Migration(34, 35) { db ->
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS checklist_sections (
                            id TEXT NOT NULL PRIMARY KEY,
                            taskId TEXT NOT NULL,
                            title TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            updatedAt TEXT NOT NULL,
                            FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                        )"""
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_sections_taskId ON checklist_sections(taskId)")
                    db.execSQL("ALTER TABLE checklist_items ADD COLUMN sectionId TEXT")
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS checklist_item_reminders (
                            id TEXT NOT NULL PRIMARY KEY,
                            checklistItemId TEXT NOT NULL,
                            scheduledAt TEXT NOT NULL,
                            updatedAt TEXT NOT NULL,
                            FOREIGN KEY(checklistItemId) REFERENCES checklist_items(id) ON DELETE CASCADE
                        )"""
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_item_reminders_checklistItemId ON checklist_item_reminders(checklistItemId)")
                }
                val migration35to36 = Migration(35, 36) { db ->
                    db.execSQL("ALTER TABLE projects ADD COLUMN coverImage TEXT")
                }
                val migration36to37 = Migration(36, 37) { db ->
                    db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                }
                val migration37to38 = Migration(37, 38) { db ->
                    // NULL = not yet anchored; first rollover check anchors without resetting
                    db.execSQL("ALTER TABLE goals ADD COLUMN periodStart TEXT")
                }
                val migration38to39 = Migration(38, 39) { db ->
                    db.execSQL("ALTER TABLE tasks ADD COLUMN startTime TEXT")
                }
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "onlyflows.db",
                )
                .addMigrations(migration27to28, migration28to29, migration29to30, migration30to31, migration31to32, migration32to33, migration33to34, migration34to35, migration35to36, migration36to37, migration37to38, migration38to39)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
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

}
