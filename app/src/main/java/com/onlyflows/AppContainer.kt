package com.onlyflows

import android.content.Context
import com.onlyflows.data.backup.BackupManager
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.calendar.CalendarRepository
import com.onlyflows.data.notifications.DailyAlertScheduler
import com.onlyflows.data.notifications.ReminderScheduler
import com.onlyflows.data.preferences.DailyAlertPreferences
import com.onlyflows.data.notifications.GeofenceManager
import com.onlyflows.data.repository.ActionLogRepositoryImpl
import com.onlyflows.data.repository.AttachmentRepositoryImpl
import com.onlyflows.data.repository.SavedLocationRepositoryImpl
import com.onlyflows.data.repository.CommentRepositoryImpl
import com.onlyflows.data.repository.ReminderRepositoryImpl
import com.onlyflows.data.repository.TimeEntryRepositoryImpl
import com.onlyflows.domain.repository.ActionLogRepository
import com.onlyflows.domain.repository.AttachmentRepository
import com.onlyflows.domain.repository.SavedLocationRepository
import com.onlyflows.domain.repository.CommentRepository
import com.onlyflows.domain.repository.ReminderRepository
import com.onlyflows.domain.repository.TimeEntryRepository
import com.onlyflows.data.preferences.AppPreferences
import com.onlyflows.data.preferences.BackupPreferences
import com.onlyflows.data.preferences.CalendarPreferences
import com.onlyflows.data.preferences.NotificationPreferences
import com.onlyflows.data.preferences.SidebarPreferences
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.data.repository.GoalRepositoryImpl
import com.onlyflows.data.repository.GoalLogRepositoryImpl
import com.onlyflows.data.repository.FolderRepositoryImpl
import com.onlyflows.data.repository.NoteRepositoryImpl
import com.onlyflows.data.repository.ProjectRepositoryImpl
import com.onlyflows.data.repository.TagRepositoryImpl
import com.onlyflows.data.repository.TaskRepositoryImpl
import com.onlyflows.domain.repository.GoalRepository
import com.onlyflows.domain.repository.GoalLogRepository
import com.onlyflows.domain.repository.FolderRepository
import com.onlyflows.domain.repository.NoteRepository
import com.onlyflows.domain.repository.ProjectRepository
import com.onlyflows.domain.repository.TagRepository
import com.onlyflows.domain.repository.TaskRepository
import com.onlyflows.data.syncthing.ConflictResolver
import com.onlyflows.data.syncthing.SyncthingCoordinator
import com.onlyflows.data.syncthing.SyncthingImporter
import com.onlyflows.data.syncthing.SyncthingPreferences
import com.onlyflows.data.syncthing.SyncthingStatusMonitor
import com.onlyflows.data.syncthing.SyncthingWriter

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val syncDao = db.syncDao()

    // ── Syncthing (declared first — repos depend on it) ───────────────────────
    val syncthingPreferences: SyncthingPreferences   = SyncthingPreferences(context)
    val syncthingWriter: SyncthingWriter              = SyncthingWriter(context, syncthingPreferences)
    val syncthingStatusMonitor: SyncthingStatusMonitor = SyncthingStatusMonitor(context, syncthingPreferences)
    val conflictResolver: ConflictResolver            = ConflictResolver(context, syncDao, syncthingPreferences)
    val syncthingImporter: SyncthingImporter          = SyncthingImporter(context, syncDao, syncthingPreferences)
    val syncthingCoordinator: SyncthingCoordinator    = SyncthingCoordinator(
        context         = context,
        prefs           = syncthingPreferences,
        monitor         = syncthingStatusMonitor,
        importer        = syncthingImporter,
        conflictResolver = conflictResolver,
    )

    // ── Repositories ──────────────────────────────────────────────────────────
    val actionLogRepository: ActionLogRepository        = ActionLogRepositoryImpl(db.actionLogDao())
    val geofenceManager: GeofenceManager                = GeofenceManager(context)

    val taskRepository: TaskRepository = TaskRepositoryImpl(
        taskDao         = db.taskDao(),
        checklistDao    = db.checklistItemDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val projectRepository: ProjectRepository = ProjectRepositoryImpl(
        projectDao      = db.projectDao(),
        sectionDao      = db.sectionDao(),
        statusLogDao    = db.statusLogDao(),
        taskDao         = db.taskDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val goalRepository: GoalRepository       = GoalRepositoryImpl(db.goalDao())
    val goalLogRepository: GoalLogRepository = GoalLogRepositoryImpl(db.goalLogDao())

    val folderRepository: FolderRepository = FolderRepositoryImpl(
        folderDao       = db.folderDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val tagRepository: TagRepository = TagRepositoryImpl(
        tagDao          = db.tagDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val noteRepository: NoteRepository = NoteRepositoryImpl(
        noteDao         = db.noteDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val savedLocationRepository: SavedLocationRepository = SavedLocationRepositoryImpl(
        dao             = db.savedLocationDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val notificationPreferences: NotificationPreferences = NotificationPreferences(context)
    val sidebarPreferences: SidebarPreferences   = SidebarPreferences(context)
    val appPreferences: AppPreferences           = AppPreferences(context)
    val calendarPreferences: CalendarPreferences = CalendarPreferences(context)
    val calendarRepository: CalendarRepository   = CalendarRepository(context)
    val backupPreferences: BackupPreferences     = BackupPreferences(context)
    val backupManager: BackupManager             = BackupManager(context)
    val widgetPreferences: WidgetPreferences     = WidgetPreferences(context)
    val reminderScheduler: ReminderScheduler     = ReminderScheduler(context)
    val dailyAlertPreferences: DailyAlertPreferences = DailyAlertPreferences(context)
    val dailyAlertScheduler: DailyAlertScheduler     = DailyAlertScheduler(context)

    val timeEntryRepository: TimeEntryRepository = TimeEntryRepositoryImpl(
        dao             = db.timeEntryDao(),
        actionLog       = actionLogRepository,
        syncthingWriter = syncthingWriter,
    )

    val commentRepository: CommentRepository = CommentRepositoryImpl(
        dao             = db.commentDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val reminderRepository: ReminderRepository = ReminderRepositoryImpl(
        dao             = db.reminderDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val attachmentRepository: AttachmentRepository = AttachmentRepositoryImpl(
        dao             = db.attachmentDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )
}
