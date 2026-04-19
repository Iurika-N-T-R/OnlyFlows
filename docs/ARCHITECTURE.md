# OnlyFlows — Architecture Reference

## Layer Overview

```
UI (Compose Screens + ViewModels)
  ↕ StateFlow<UiState> / onEvent()
Domain (Models + RecurrenceScheduler)
  ↕ Repository interfaces
Data (RepositoryImpl + Room DAOs + DataStore + Syncthing)
  ↕
Local DB (Room v2.6.1) + DataStore Preferences + Syncthing folder (SAF)
```

---

## Full File Structure

```
app/src/main/java/com/onlyflows/
│
├── OnlyFlowsApp.kt                     Application class — init AppContainer, start Syncthing coordinator
├── MainActivity.kt                     Single activity — edge-to-edge, theme, permission requests
├── AppContainer.kt                     Manual DI root — constructs all repos, schedulers, preferences
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt             Room DB (v22, 14 entities, fallbackToDestructiveMigration, seed)
│   │   ├── Converters.kt              TypeConverters: LocalDate, LocalDateTime, LocalTime, JSON lists
│   │   ├── entity/
│   │   │   ├── TaskEntity.kt
│   │   │   ├── ProjectEntity.kt
│   │   │   ├── SectionEntity.kt
│   │   │   ├── ChecklistItemEntity.kt
│   │   │   ├── TagEntity.kt
│   │   │   ├── FolderEntity.kt
│   │   │   ├── NoteEntity.kt
│   │   │   ├── CommentEntity.kt
│   │   │   ├── ReminderEntity.kt
│   │   │   ├── TimeEntryEntity.kt
│   │   │   ├── SavedLocationEntity.kt
│   │   │   ├── AttachmentEntity.kt
│   │   │   ├── GoalEntity.kt
│   │   │   ├── GoalLogEntity.kt
│   │   │   ├── SyncMetaEntity.kt      Tracks sync state per entity
│   │   │   └── DeletedEntityEntity.kt  Tombstones for delete propagation
│   │   └── dao/
│   │       ├── TaskDao.kt
│   │       ├── ProjectDao.kt
│   │       ├── SectionDao.kt
│   │       ├── ChecklistItemDao.kt
│   │       ├── TagDao.kt
│   │       ├── FolderDao.kt
│   │       ├── NoteDao.kt
│   │       ├── CommentDao.kt
│   │       ├── ReminderDao.kt
│   │       ├── TimeEntryDao.kt
│   │       ├── SavedLocationDao.kt
│   │       ├── AttachmentDao.kt
│   │       ├── GoalDao.kt
│   │       ├── GoalLogDao.kt
│   │       ├── StatusLogDao.kt
│   │       └── SyncDao.kt             Upsert / getUpdatedAt / delete / getAll for all entities
│   │
│   ├── repository/
│   │   ├── TaskRepositoryImpl.kt
│   │   ├── ProjectRepositoryImpl.kt
│   │   ├── SectionRepositoryImpl.kt (via ProjectRepositoryImpl)
│   │   ├── TagRepositoryImpl.kt
│   │   ├── FolderRepositoryImpl.kt
│   │   ├── NoteRepositoryImpl.kt
│   │   ├── CommentRepositoryImpl.kt
│   │   ├── ReminderRepositoryImpl.kt
│   │   ├── TimeEntryRepositoryImpl.kt
│   │   ├── SavedLocationRepositoryImpl.kt
│   │   ├── AttachmentRepositoryImpl.kt
│   │   ├── GoalRepositoryImpl.kt
│   │   └── GoalLogRepositoryImpl.kt
│   │
│   ├── syncthing/
│   │   ├── SyncthingPreferences.kt    DataStore: folderUri, deviceId, lastScannedAt, enabled
│   │   ├── SyncthingWriter.kt         Writes entity JSON files to Syncthing folder via SAF
│   │   ├── SyncthingImporter.kt       Reads folder, compares updatedAt, upserts Room
│   │   ├── SyncthingExporter.kt       Bulk export of all entities on first folder setup
│   │   ├── ConflictResolver.kt        Resolves .sync-conflict-* files (Last-Write-Wins)
│   │   ├── SyncthingStatusMonitor.kt  Observes Wi-Fi + Syncthing package → emits SyncthingStatus
│   │   ├── SyncthingCoordinator.kt    FileObserver + debounce, starts/stops on status change
│   │   ├── SyncthingImportWorker.kt   WorkManager 15-min periodic fallback scan
│   │   └── EntityJson.kt              toJson() / fromJson() for all entity types
│   │
│   ├── notifications/
│   │   ├── ReminderScheduler.kt       AlarmManager exact alarms per task reminder
│   │   ├── ReminderReceiver.kt        BroadcastReceiver — fires notification on alarm
│   │   ├── BootReceiver.kt            Re-schedules all reminders after device reboot
│   │   ├── DailyAlertScheduler.kt     Scheduled daily summary notifications
│   │   └── GeofenceManager.kt         Location-based (geofence) alert triggers
│   │
│   ├── preferences/
│   │   ├── AppPreferences.kt          Theme, time format, text size, URL links
│   │   ├── NotificationPreferences.kt Sound, vibration, bypass DND
│   │   ├── CalendarPreferences.kt     Selected calendars
│   │   ├── SidebarPreferences.kt      Sidebar visible state (tablet)
│   │   ├── WidgetPreferences.kt       Home screen widget config
│   │   ├── BackupPreferences.kt       Backup folder URI
│   │   ├── DailyAlertPreferences.kt   Alert time + days
│   │   └── SyncthingPreferences.kt    Syncthing folder URI, deviceId, etc.
│   │
│   ├── calendar/
│   │   └── CalendarRepository.kt      Reads system CalendarProvider
│   │
│   ├── backup/
│   │   └── BackupManager.kt           Export/import Room database via WorkManager
│   │
│   └── debug/
│       └── AppLog.kt                  In-memory log ring buffer for debug screen
│
├── domain/
│   ├── model/
│   │   └── Models.kt                  All domain models: Task, Project, Goal, TimeEntry…
│   ├── repository/                    Repository interfaces (one per aggregate)
│   └── RecurrenceScheduler.kt         nextDueDate() pure function — all recurrence logic
│
└── ui/
    ├── screens/
    │   ├── today/           TodayScreen + TodayViewModel
    │   ├── inbox/           InboxScreen + InboxViewModel
    │   ├── upcoming/        UpcomingScreen + UpcomingViewModel
    │   ├── anytime/         AnytimeScreen + AnytimeViewModel
    │   ├── someday/         SomedayScreen + SomedayViewModel
    │   ├── projects/        ProjectsScreen + ProjectsViewModel
    │   ├── project/         ProjectDetailScreen + ProjectDetailViewModel
    │   ├── task/            TaskDetailScreen + TaskDetailViewModel
    │   ├── logbook/         LogbookScreen + LogbookViewModel
    │   ├── trash/           TrashScreen + TrashViewModel
    │   ├── search/          SearchScreen + SearchViewModel (QuickFind)
    │   └── settings/
    │       ├── SettingsScreen.kt
    │       ├── SettingsViewModel.kt
    │       ├── SyncthingSettingsScreen.kt
    │       └── SyncthingViewModel.kt
    │
    ├── components/
    │   ├── TaskItem.kt                Swipeable task row with priority/tag chips
    │   ├── SelectionBar.kt            Multi-select action bar
    │   ├── TaskFilterBar.kt           Tag + priority filter chips
    │   ├── AgendaCard.kt              Card for calendar day view
    │   ├── EmptyState.kt              Placeholder illustrations
    │   └── …
    │
    ├── overlays/
    │   ├── AddTaskSheet.kt            Bottom sheet — quick task creation
    │   ├── DatePickerModal.kt         Date + time picker
    │   ├── MoveTaskSheet.kt           Move task to project/section
    │   ├── NoteEditorSheet.kt         Rich note editor
    │   ├── QuickFindPopup.kt          Full-screen search overlay
    │   └── BulkMoreSheet.kt           Multi-selection More actions
    │
    ├── layout/
    │   └── AdaptiveAppLayout.kt       Phone: ModalNavigationDrawer; Tablet: permanent sidebar Row
    │
    ├── navigation/
    │   ├── AppNavigation.kt           NavHost — all route registrations
    │   └── Screen.kt                  Sealed class — all route definitions
    │
    └── theme/
        ├── Color.kt                   Material 3 color tokens
        ├── Type.kt                    Typography scale
        └── Theme.kt                   OnlyFlowsTheme composable
```

---

## Key Design Decisions

### UDF (Unidirectional Data Flow)
Every screen has a paired ViewModel exposing a single `StateFlow<UiState>`. The UI calls `viewModel.onEvent(Event)`. No business logic in composables.

### Manual DI
`AppContainer` constructs every dependency in explicit order. ViewModels access it via `(application as OnlyFlowsApp).container`. No Hilt/Dagger — intentional to keep the dependency graph visible and debuggable.

### Room as Source of Truth
All data is observed as `Flow<List<T>>`. Repository implementations use `flatMapLatest` + `combine` to join entities and emit domain models. No cache or local state in ViewModels beyond transient animation state.

### Soft Delete
Every deletable entity has `deletedAt: LocalDateTime?`. All DAO queries filter `WHERE deletedAt IS NULL`. Hard delete is only available from the Trash screen (or via tombstone sync).

### Syncthing Sync
Each repository write immediately writes a JSON file to the Syncthing folder via `SyncthingWriter`. `SyncthingCoordinator` watches the folder with `FileObserver` and triggers import on change. `WorkManager` provides a 15-min periodic fallback. See [SYNC.md](SYNC.md) for full details.

---

## Database

Room v2.6.1, version 22, `fallbackToDestructiveMigration` (development phase).

**14 entities:** Task, Project, Section, ChecklistItem, Tag, Folder, Note, Comment, Reminder, TimeEntry, SavedLocation, Attachment, Goal, GoalLog

**Supporting entities:** SyncMetaEntity, DeletedEntityEntity (tombstones), StatusLogEntry

**SyncDao** is the central DAO for sync: upsert/getUpdatedAt/delete/getAll for every entity type, plus tombstone insert/query.
