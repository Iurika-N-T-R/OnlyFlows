package com.onlyflows

import android.content.Context
import com.onlyflows.data.backup.BackupManager
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.calendar.CalendarRepository
import com.onlyflows.data.notifications.DailyAlertScheduler
import com.onlyflows.data.notifications.ReminderScheduler
import com.onlyflows.data.preferences.DailyAlertPreferences
import com.onlyflows.data.notifications.GeofenceManager
import com.onlyflows.data.repository.ActionLogRepository
import com.onlyflows.data.repository.AttachmentRepository
import com.onlyflows.data.repository.SavedLocationRepository
import com.onlyflows.data.repository.CommentRepository
import com.onlyflows.data.repository.ReminderRepository
import com.onlyflows.data.repository.TimeEntryRepository
import com.onlyflows.data.preferences.AppPreferences
import com.onlyflows.data.preferences.BackupPreferences
import com.onlyflows.data.preferences.CalendarPreferences
import com.onlyflows.data.preferences.NotificationPreferences
import com.onlyflows.data.preferences.SidebarPreferences
import com.onlyflows.data.local.QuoteRepository
import com.onlyflows.data.repository.DateTrackerRepository
import com.onlyflows.data.repository.GoalRepository
import com.onlyflows.data.repository.GoalLogRepository
import com.onlyflows.data.repository.FolderRepository
import com.onlyflows.data.repository.NoteRepository
import com.onlyflows.data.repository.ProjectRepository
import com.onlyflows.data.repository.TagRepository
import com.onlyflows.data.repository.TaskRepository
import com.onlyflows.data.syncthing.ConflictResolver
import com.onlyflows.data.syncthing.SyncthingCoordinator
import com.onlyflows.data.syncthing.SyncthingExporter
import com.onlyflows.data.syncthing.SyncthingImporter
import com.onlyflows.data.syncthing.SyncthingPreferences
import com.onlyflows.data.syncthing.SyncthingStatusMonitor
import com.onlyflows.data.syncthing.SyncthingWriter
import com.onlyflows.ui.widget.WidgetPrefsRepository
import com.onlyflows.ui.widget.WidgetStateManager

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val syncDao = db.syncDao()

    // ── Syncthing (declared first — repos depend on it) ───────────────────────
    val syncthingPreferences: SyncthingPreferences   = SyncthingPreferences(context)
    val syncthingWriter: SyncthingWriter              = SyncthingWriter(context, syncthingPreferences)
    val syncthingStatusMonitor: SyncthingStatusMonitor = SyncthingStatusMonitor(context, syncthingPreferences)
    val conflictResolver: ConflictResolver            = ConflictResolver(context, syncDao, syncthingPreferences)
    val syncthingImporter: SyncthingImporter          = SyncthingImporter(context, syncDao, syncthingPreferences)
    val syncthingExporter: SyncthingExporter          = SyncthingExporter(syncDao, syncthingWriter)
    val syncthingCoordinator: SyncthingCoordinator    = SyncthingCoordinator(
        context          = context,
        prefs            = syncthingPreferences,
        monitor          = syncthingStatusMonitor,
        importer         = syncthingImporter,
        conflictResolver = conflictResolver,
        writer           = syncthingWriter,
    )

    // ── Repositories ──────────────────────────────────────────────────────────
    val actionLogRepository: ActionLogRepository        = ActionLogRepository(db.actionLogDao())
    val geofenceManager: GeofenceManager                = GeofenceManager(context)

    val taskRepository: TaskRepository = TaskRepository(
        taskDao         = db.taskDao(),
        checklistDao    = db.checklistItemDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
        onTaskMutated   = {},
    )

    val projectRepository: ProjectRepository = ProjectRepository(
        projectDao      = db.projectDao(),
        sectionDao      = db.sectionDao(),
        statusLogDao    = db.statusLogDao(),
        taskDao         = db.taskDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val goalRepository: GoalRepository       = GoalRepository(db.goalDao(), syncDao, syncthingWriter)
    val goalLogRepository: GoalLogRepository = GoalLogRepository(db.goalLogDao(), syncDao, syncthingWriter)

    val folderRepository: FolderRepository = FolderRepository(
        folderDao       = db.folderDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val tagRepository: TagRepository = TagRepository(
        tagDao          = db.tagDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val noteRepository: NoteRepository = NoteRepository(
        noteDao         = db.noteDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val savedLocationRepository: SavedLocationRepository = SavedLocationRepository(
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
    val quoteRepository: QuoteRepository         = QuoteRepository(context)
    val dateTrackerRepository: DateTrackerRepository = DateTrackerRepository(db.dateTrackerDao(), syncDao, syncthingWriter)
    val rssRepository: com.onlyflows.data.repository.RssRepository =
        com.onlyflows.data.repository.RssRepository(db.rssFeedDao(), db.rssArticleDao(), context)
    val reminderScheduler: ReminderScheduler     = ReminderScheduler(context)
    val dailyAlertPreferences: DailyAlertPreferences = DailyAlertPreferences(context)
    val dailyAlertScheduler: DailyAlertScheduler     = DailyAlertScheduler(context)

    val timeEntryRepository: TimeEntryRepository = TimeEntryRepository(
        dao             = db.timeEntryDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val commentRepository: CommentRepository = CommentRepository(
        dao             = db.commentDao(),
        actionLog       = actionLogRepository,
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val reminderRepository: ReminderRepository = ReminderRepository(
        dao             = db.reminderDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val attachmentRepository: AttachmentRepository = AttachmentRepository(
        dao             = db.attachmentDao(),
        syncDao         = syncDao,
        syncthingWriter = syncthingWriter,
    )

    val widgetPrefsRepository: WidgetPrefsRepository = WidgetPrefsRepository(context)
    val widgetStateManager: WidgetStateManager       = WidgetStateManager(context, widgetPrefsRepository)
}
