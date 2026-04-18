package com.onlyflows.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.onlyflows.data.local.dao.*
import com.onlyflows.data.local.dao.GoalDao
import com.onlyflows.data.local.dao.GoalLogDao
import com.onlyflows.data.local.dao.ActionLogDao
import com.onlyflows.data.local.dao.SavedLocationDao
import com.onlyflows.data.local.dao.ReminderDao
import com.onlyflows.data.local.dao.AttachmentDao
import com.onlyflows.data.local.entity.*
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
    ],
    version = 27,
    exportSchema = false,
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
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "onlyflows.db",
                )
                .fallbackToDestructiveMigration()
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
