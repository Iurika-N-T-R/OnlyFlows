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

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val syncDao = db.syncDao()

    val actionLogRepository: ActionLogRepository       = ActionLogRepositoryImpl(db.actionLogDao())
    val savedLocationRepository: SavedLocationRepository = SavedLocationRepositoryImpl(db.savedLocationDao(), syncDao)
    val geofenceManager: GeofenceManager               = GeofenceManager(context)

    val taskRepository: TaskRepository = TaskRepositoryImpl(
        taskDao      = db.taskDao(),
        checklistDao = db.checklistItemDao(),
        actionLog    = actionLogRepository,
        syncDao      = syncDao,
    )

    val projectRepository: ProjectRepository = ProjectRepositoryImpl(
        projectDao   = db.projectDao(),
        sectionDao   = db.sectionDao(),
        statusLogDao = db.statusLogDao(),
        taskDao      = db.taskDao(),
        actionLog    = actionLogRepository,
        syncDao      = syncDao,
    )

    val goalRepository: GoalRepository = GoalRepositoryImpl(db.goalDao())
    val goalLogRepository: GoalLogRepository = GoalLogRepositoryImpl(db.goalLogDao())
    val folderRepository: FolderRepository = FolderRepositoryImpl(db.folderDao(), syncDao)

    val tagRepository: TagRepository = TagRepositoryImpl(db.tagDao(), syncDao)

    val noteRepository: NoteRepository = NoteRepositoryImpl(db.noteDao(), syncDao)

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
        dao       = db.timeEntryDao(),
        actionLog = actionLogRepository,
    )
    val commentRepository: CommentRepository = CommentRepositoryImpl(
        dao       = db.commentDao(),
        actionLog = actionLogRepository,
        syncDao   = syncDao,
    )
    val reminderRepository: ReminderRepository     = ReminderRepositoryImpl(db.reminderDao(), syncDao)
    val attachmentRepository: AttachmentRepository = AttachmentRepositoryImpl(db.attachmentDao(), syncDao)

}
