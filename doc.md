# OnlyFlows — Project Context Document
> Load this file at the start of a new session to restore full project context.
> Last updated: 2026-04-14 — DB version 20

---

## 1. Project Overview

**OnlyFlows** is a native Android task/project manager.
- Style: minimal, editorial, local-first. Inspired by Things 3.
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- State: ViewModel + StateFlow (UDF pattern — events in, state out)
- DB: Room (SQLite), version **20**, `fallbackToDestructiveMigration()` enabled
- DI: Manual — `AppContainer` singleton, accessed via `(app as OnlyFlowsApp).container`
- Async: Coroutines + Flow
- Navigation: Navigation Compose
- Drag & Drop: `sh.calvin.reorderable`
- Prefs: DataStore Preferences

Working directory: `h:/PROJET/OnlyFlows`
Package root: `com.onlyflows`
Main source: `app/src/main/java/com/onlyflows/`

---

## 2. Folder Structure

```
app/src/main/java/com/onlyflows/
├── OnlyFlowsApp.kt                   Application class, holds AppContainer
├── AppContainer.kt                   Manual DI — all repos + prefs instantiated here
├── MainActivity.kt
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt            Room DB, version 20, 12 entities, SeedCallback
│   │   ├── Converters.kt             TypeConverters (LocalDate, LocalDateTime, LocalTime, List<String>)
│   │   ├── dao/
│   │   │   ├── TaskDao.kt
│   │   │   ├── ChecklistItemDao.kt
│   │   │   ├── ProjectDao.kt
│   │   │   ├── SectionDao.kt
│   │   │   ├── TagDao.kt
│   │   │   ├── FolderDao.kt
│   │   │   ├── StatusLogDao.kt
│   │   │   ├── NoteDao.kt
│   │   │   ├── TimeEntryDao.kt
│   │   │   ├── CommentDao.kt
│   │   │   ├── ReminderDao.kt
│   │   │   └── AttachmentDao.kt
│   │   └── entity/
│   │       ├── TaskEntity.kt         + toDomain() + toEntity()
│   │       ├── ChecklistItemEntity.kt
│   │       ├── ProjectEntity.kt
│   │       ├── SectionEntity.kt
│   │       ├── TagEntity.kt
│   │       ├── FolderEntity.kt
│   │       ├── StatusLogEntity.kt
│   │       ├── NoteEntity.kt
│   │       ├── TimeEntryEntity.kt
│   │       ├── CommentEntity.kt
│   │       ├── ReminderEntity.kt
│   │       └── AttachmentEntity.kt
│   ├── backup/
│   │   ├── BackupManager.kt
│   │   └── BackupWorker.kt
│   ├── calendar/
│   │   └── CalendarRepository.kt    Android Calendar Provider integration
│   ├── mock/
│   │   └── MockData.kt              Seed data for fresh installs
│   ├── notifications/
│   │   ├── ReminderScheduler.kt     AlarmManager scheduling
│   │   ├── ReminderReceiver.kt      BroadcastReceiver — fires notification
│   │   └── BootReceiver.kt          Re-schedules alarms after reboot
│   ├── preferences/
│   │   ├── AppPreferences.kt        (theme, etc.)
│   │   ├── SidebarPreferences.kt
│   │   ├── NotificationPreferences.kt  (sound on/off, vibration on/off)
│   │   ├── CalendarPreferences.kt
│   │   └── BackupPreferences.kt
│   └── repository/
│       ├── TaskRepositoryImpl.kt
│       ├── ProjectRepositoryImpl.kt
│       ├── FolderRepositoryImpl.kt
│       ├── TagRepositoryImpl.kt
│       ├── NoteRepositoryImpl.kt
│       ├── TimeEntryRepositoryImpl.kt
│       ├── CommentRepositoryImpl.kt
│       ├── ReminderRepositoryImpl.kt
│       └── AttachmentRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   └── Models.kt                All domain models (see Section 4)
│   ├── repository/
│   │   ├── TaskRepository.kt        (interface)
│   │   ├── ProjectRepository.kt
│   │   ├── FolderRepository.kt
│   │   ├── TagRepository.kt
│   │   ├── NoteRepository.kt
│   │   ├── TimeEntryRepository.kt
│   │   ├── CommentRepository.kt
│   │   ├── ReminderRepository.kt
│   │   └── AttachmentRepository.kt
│   └── RecurrenceScheduler.kt       nextDueDate() pure function
│
└── ui/
    ├── components/
    │   ├── TaskItem.kt              TaskItem + SwipeableTaskItem + TaskCheckbox + DateBadge + TagChip + InteractiveChecklistRow
    │   ├── SelectionBar.kt          Bottom bar shown during multi-select (Move / Delete / More)
    │   ├── FlowFab.kt               Floating action button
    │   ├── EmptyState.kt            Centred emoji + title + subtitle
    │   ├── SectionHeader.kt
    │   ├── CompletedSectionHeader.kt
    │   ├── CommentsSection.kt
    │   ├── NoteCard.kt
    │   ├── AgendaCard.kt
    │   ├── TaskFilterBar.kt
    │   └── sidebar/
    │       ├── AppSidebar.kt        Navigation drawer (NavItems + project list)
    │       └── SidebarViewModel.kt  Counts (inbox, today, overdue), folder/project entries
    ├── layout/
    │   └── AdaptiveAppLayout.kt     Drawer + NavRail for phone/tablet
    ├── navigation/
    │   ├── Screen.kt                Sealed class, all routes
    │   └── AppNavigation.kt         NavHost with all composable() registrations
    ├── overlays/
    │   ├── AddTaskSheet.kt          Quick-add bottom sheet (title, project, section, priority, date)
    │   ├── DatePickerModal.kt       Date + optional time + end time picker
    │   ├── SmartMoveSheet.kt        Move to Anytime / Someday / Project / Section
    │   ├── MoveTaskSheet.kt
    │   ├── TagsPickerModal.kt
    │   ├── BulkMoreSheet.kt         Bulk ops: star, priority, schedule
    │   ├── NoteEditorSheet.kt
    │   └── QuickFindOverlay.kt
    ├── screens/
    │   ├── today/        TodayScreen + TodayViewModel
    │   ├── inbox/        InboxScreen + InboxViewModel
    │   ├── upcoming/     UpcomingScreen + UpcomingViewModel
    │   ├── anytime/      AnytimeScreen + AnytimeViewModel
    │   ├── someday/      SomedayScreen + SomedayViewModel
    │   ├── logbook/      LogbookScreen + LogbookViewModel
    │   ├── trash/        TrashScreen + TrashViewModel          ← NEW (v20)
    │   ├── projects/     ProjectsScreen + ProjectsViewModel    (Kanban + List view)
    │   ├── project/      ProjectDetailScreen + ProjectDetailViewModel
    │   ├── task/         TaskDetailScreen + TaskDetailViewModel
    │   └── search/       QuickFindScreen + DateResultsScreen + their ViewModels
    └── theme/
        ├── Color.kt
        ├── Type.kt
        └── Theme.kt
```

---

## 3. Navigation Routes

File: `ui/navigation/Screen.kt`

| Screen | Route |
|--------|-------|
| Today | `today` |
| Upcoming | `upcoming` |
| Inbox | `inbox` |
| Anytime | `anytime` |
| Someday | `someday` |
| Logbook | `logbook` |
| Trash | `trash` |
| Projects (Kanban) | `projects` |
| ProjectDetail | `project/{projectId}` |
| TaskDetail | `task/{taskId}` |
| Search | `search?q={query}` |
| DateResults | `date_results/{date}` |
| Settings | `settings` |
| Appearance | `settings/appearance` |
| CalendarSettings | `settings/calendar` |
| BackupSettings | `settings/backup` |
| NotificationSettings | `settings/notifications` |

Start destination: `Screen.Today`

---

## 4. Domain Models (`domain/model/Models.kt`)

### Task
```kotlin
data class Task(
    val id: String,
    val title: String,
    val note: String = "",
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val projectId: String? = null,
    val sectionId: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,     // null = all day
    val endTime: LocalTime? = null,
    val scheduledGroup: TaskGroup? = null,
    val hasReminders: Boolean = false,
    val hasAttachments: Boolean = false,
    val isStarred: Boolean = false,
    val tagIds: List<String> = emptyList(),
    val priority: TaskPriority = TaskPriority.NONE,
    val startDate: LocalDate? = null,
    val recurrence: RecurrenceRule? = null,   // @IgnoredOnParcel
    val sortOrder: Int = 0,
    val hasNote: Boolean = false,
    val hasChecklist: Boolean = false,
    val recurrenceDoneCount: Int = 0,
    val recurrenceAbandonedCount: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val dependsOnTaskId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null,     // null = active; non-null = in Trash
)
```

**Enums:**
- `TaskGroup { TODAY, THIS_EVENING, SOMEDAY, ANYTIME }`
- `TaskPriority { NONE, LOW, MEDIUM, HIGH }`

### Task routing logic
- **Inbox**: `projectId IS NULL AND scheduledGroup IS NULL AND isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL`
- **Today**: `scheduledGroup IN ('TODAY','THIS_EVENING') AND isArchived=0 AND deletedAt IS NULL`
- **Upcoming**: `dueDate IS NOT NULL AND isCompleted=0 AND isArchived=0 AND deletedAt IS NULL`
- **Anytime (auto)**: `projectId IS NOT NULL AND priority != NONE AND scheduledGroup IS NULL AND dueDate IS NULL AND isCompleted=0 AND isArchived=0 AND deletedAt IS NULL`
- **Someday (auto)**: `projectId IS NOT NULL AND priority = NONE AND scheduledGroup IS NULL AND dueDate IS NULL AND isCompleted=0 AND isArchived=0 AND deletedAt IS NULL`
- **Logbook**: archived projects
- **Trash**: `deletedAt IS NOT NULL`

### Project
```kotlin
data class Project(
    val id: String,
    val title: String,
    val note: String = "",
    val colorHex: String = "#4A90D9",
    val iconEmoji: String = "",
    val isCompleted: Boolean = false,
    val folderId: String? = null,          // null = standalone
    val parentProjectId: String? = null,   // non-null = nested list
    val type: ProjectType = ProjectType.PROJECT,
    val sections: List<Section> = emptyList(),
    val sortOrder: Int = 0,
    val dueDate: LocalDate? = null,
    val status: ProjectStatus = ProjectStatus.NOT_STARTED,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
// ProjectType { PROJECT, LIST, NOTE }
// ProjectStatus { NOT_STARTED, IN_PROGRESS, WAITING, DONE }
```

### Other models
- `Section(id, projectId, title, sortOrder)`
- `Tag(id, name, colorHex)`
- `Folder(id, title, sortOrder)`
- `ChecklistItem(id, taskId, title, isCompleted, sortOrder, dueDate?, startDate?)`
- `Reminder(id, taskId, scheduledAt: LocalDateTime)`
- `Attachment(id, taskId, fileName, mimeType, uri, fileSize, createdAt)` — uri = persistable content URI string
- `TimeEntry(id, taskId, startedAt, endedAt?)` — endedAt null = running
- `Comment(id, taskId?, projectId?, content, createdAt, updatedAt)`
- `RecurrenceRule(type, interval, daysOfWeek, basis, skipWeekends)`
  - `RecurrenceType { HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, WEEKDAYS }`
  - `RecurrenceBasis { COMPLETION_DATE, DUE_DATE, SPECIFIC_DATE }`

---

## 5. Database — AppDatabase.kt

Version: **20**
Strategy: `fallbackToDestructiveMigration()` + `SeedCallback` (seeds MockData on fresh install / destructive migration)

**Entities:** TaskEntity, ChecklistItemEntity, ProjectEntity, SectionEntity, TagEntity, FolderEntity, StatusLogEntity, NoteEntity, TimeEntryEntity, CommentEntity, ReminderEntity, AttachmentEntity

**TaskEntity key fields** (mirrors Task, stored as primitives):
- `scheduledGroup: String?` (TaskGroup.name)
- `priority: String` (TaskPriority.name, default "NONE")
- `recurrenceJson: String?` (serialised JSON)
- `deletedAt: LocalDateTime?` — **null = active, non-null = soft-deleted (Trash)**
- All `LocalDate`/`LocalTime`/`LocalDateTime` stored via `Converters` (ISO string)

---

## 6. AppContainer.kt — Dependency Graph

```kotlin
class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)

    val taskRepository       = TaskRepositoryImpl(db.taskDao(), db.checklistItemDao())
    val projectRepository    = ProjectRepositoryImpl(db.projectDao(), db.sectionDao(), db.statusLogDao(), db.taskDao())
    val folderRepository     = FolderRepositoryImpl(db.folderDao())
    val tagRepository        = TagRepositoryImpl(db.tagDao())
    val noteRepository       = NoteRepositoryImpl(db.noteDao())
    val timeEntryRepository  = TimeEntryRepositoryImpl(db.timeEntryDao())
    val commentRepository    = CommentRepositoryImpl(db.commentDao())
    val reminderRepository   = ReminderRepositoryImpl(db.reminderDao())
    val attachmentRepository = AttachmentRepositoryImpl(db.attachmentDao())

    val notificationPreferences  = NotificationPreferences(context)   // sound + vibration toggles
    val sidebarPreferences       = SidebarPreferences(context)
    val appPreferences           = AppPreferences(context)
    val calendarPreferences      = CalendarPreferences(context)
    val calendarRepository       = CalendarRepository(context)
    val backupPreferences        = BackupPreferences(context)
    val backupManager            = BackupManager(context)
    val reminderScheduler        = ReminderScheduler(context)         // AlarmManager
}
```

Access pattern in ViewModel:
```kotlin
class FooViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as OnlyFlowsApp).container
    private val taskRepo  = container.taskRepository
    // …
}
```

---

## 7. TaskRepository Interface (key methods)

```kotlin
interface TaskRepository {
    // Observe
    fun observeAll(): Flow<List<Task>>
    fun observeToday(): Flow<List<Task>>
    fun observeInbox(): Flow<List<Task>>
    fun observeByGroup(group: TaskGroup): Flow<List<Task>>
    fun observeOverdue(): Flow<List<Task>>
    fun observeUpcoming(): Flow<List<Task>>
    fun observeByProject(projectId: String): Flow<List<Task>>
    fun observeArchivedByProject(projectId: String): Flow<List<Task>>
    fun observeAllArchived(): Flow<List<Task>>
    fun observeById(id: String): Flow<Task?>
    fun observeAnytime(): Flow<List<Task>>
    fun observeSomeday(): Flow<List<Task>>
    fun observeDependents(taskId: String): Flow<List<Task>>
    fun observeTrash(): Flow<List<Task>>
    fun search(query: String): Flow<List<Task>>
    fun searchByDate(date: LocalDate): Flow<List<Task>>

    // Writes
    suspend fun insert(task: Task)
    suspend fun update(task: Task)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun completeRecurring(id: String)       // creates next recurrence occurrence
    suspend fun setStarred(id: String, starred: Boolean)
    suspend fun moveTask(id: String, projectId: String?, sectionId: String?)
    suspend fun setScheduledGroup(id: String, group: TaskGroup?)
    suspend fun setDependsOn(id: String, dependsOnId: String?)
    suspend fun softDelete(id: String)              // sets deletedAt = now (Trash)
    suspend fun delete(id: String)                  // permanent hard delete
    suspend fun deleteAll(ids: List<String>)        // permanent hard delete multiple
    suspend fun restoreFromTrash(id: String)        // clears deletedAt
    suspend fun emptyTrash()                        // DELETE WHERE deletedAt IS NOT NULL
    suspend fun reorderTasks(idsInOrder: List<String>)

    // Checklist
    suspend fun addChecklistItem(item: ChecklistItem)
    suspend fun toggleChecklistItem(id: String, completed: Boolean)
    suspend fun deleteChecklistItem(id: String)
    suspend fun updateChecklistItem(item: ChecklistItem)

    // Counts (sidebar badges)
    fun observeInboxCount(): Flow<Int>
    fun observeTodayCount(): Flow<Int>
    fun observeOverdueCount(): Flow<Int>
    fun observeAllCount(projectId: String): Flow<Int>
    fun observeCompletedCount(projectId: String): Flow<Int>
}
```

---

## 8. UDF Pattern (how every screen works)

Every screen follows the same 3-part pattern:

**UiState data class** — single source of truth, emitted from `_uiState: MutableStateFlow`

**Event sealed interface** — all user actions as typed events

**ViewModel.onEvent(event)** — pure state machine: `when (event) { ... }`

Example pattern:
```kotlin
// State
data class FooUiState(val tasks: List<Task> = emptyList(), val isLoading: Boolean = true)

// Events
sealed interface FooEvent {
    data class ToggleComplete(val taskId: String, val current: Boolean) : FooEvent
    data object ShowAddTask : FooEvent
}

// ViewModel
class FooViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    fun onEvent(event: FooEvent) { when (event) { ... } }
}

// Screen
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

---

## 9. Key UI Components

### TaskItem (`ui/components/TaskItem.kt`)
Main task row. Supports:
- Checkbox (custom SVG via `ic_task_checkmark` / `ic_task_unchecked`)
- Checklist progress badge `done/total`
- Date badge (overdue/today/future colour-coded)
- Note preview (1 line collapsed, full when expanded)
- Tag chips
- Indicator icons: note, checklist, recurrence, reminder, attachment (paperclip), dependency (lock)
- Star + priority flag (right side)
- Expandable checklist on tap (when not completed)
- Selection mode via `isSelectionMode` / `isSelected`
- Optional `dragHandle` slot for reorderable lists

### SwipeableTaskItem
Wraps `TaskItem` in `SwipeToDismissBox`:
- Swipe right → `onToggleComplete` (green bg + check)
- Swipe left → `onDelete` (red bg + delete icon) → snackbar with undo

### SelectionBar (`ui/components/SelectionBar.kt`)
Bottom bar appearing when tasks are selected:
- Shows count
- Buttons: Move, Delete, More (`BulkMoreSheet`)

### MetaRow (`ui/screens/task/TaskDetailScreen.kt` — private)
Properties row in TaskDetail:
```kotlin
MetaRow(
    icon     = Icons.Outlined.CalendarToday,
    label    = "Deadline",
    value    = "Tue, 15 Apr · Today",   // shown left of trailing
    onClick  = { ... },
    trailing = { /* X button — shown AFTER value, both visible simultaneously */ },
)
```
**Important**: `value` and `trailing` are NOT mutually exclusive — both render if set.

### BulkMoreSheet (`ui/overlays/BulkMoreSheet.kt`)
Bottom sheet for bulk operations:
- Star / Unstar
- Set priority (None/Low/Medium/High)
- Add tag
- Schedule (date picker)

### SmartMoveSheet (`ui/overlays/SmartMoveSheet.kt`)
Bottom sheet to move tasks:
- Anytime / Someday (scheduledGroup)
- Move to Project + optional Section

---

## 10. Trash System (DB v20)

**Soft delete** — no task is ever permanently deleted from user-visible UI flows.
- `Task.deletedAt: LocalDateTime?` — null = active, non-null = in Trash
- `TaskEntity.deletedAt` mirrors this field
- ALL active-task queries have `AND deletedAt IS NULL`
- `taskRepo.softDelete(id)` — sets `deletedAt = now()`
- `taskRepo.restoreFromTrash(id)` — sets `deletedAt = NULL`
- `taskRepo.emptyTrash()` — `DELETE WHERE deletedAt IS NOT NULL`
- `taskRepo.delete(id)` — still available for hard-delete use cases (ConvertToProject)

**Delete paths (all now use softDelete):**
- `InboxViewModel.confirmDelete()` → `softDelete`
- `InboxViewModel.onEvent(DeleteSelected)` → `softDelete` for each
- `TodayViewModel.confirmDelete()` → `softDelete`
- `UpcomingViewModel.confirmDelete()` → `softDelete`
- `SomedayViewModel.confirmDelete()` → `softDelete`
- `AnytimeViewModel.confirmDelete()` → `softDelete`
- `ProjectDetailViewModel.deleteSelected()` → `softDelete` for each
- `TaskDetailViewModel.onEvent(Delete)` → `softDelete`

**TrashScreen** (`ui/screens/trash/`):
- Lists all `deletedAt IS NOT NULL` tasks, sorted by deletedAt DESC
- Each row: title + "Supprimée le DD MMM YYYY, HH:mm" + Restore button + DeleteForever button
- Top bar "Vider" button → `AlertDialog` confirmation → `emptyTrash()`
- Route: `Screen.Trash.route = "trash"`
- Sidebar nav item wired (was previously dead)

---

## 11. TaskDetailScreen — Properties Panel

Properties shown in `LazyColumn` in this order:
1. **When** (scheduledGroup) — `WhenPickerSheet`
2. **Deadline** — `DatePickerModal`, shows date text AND ✕ clear button (both visible simultaneously)
3. **Start Date** — `DatePickerModal`
4. **Priority** — `PriorityPickerSheet`
5. **Project** — project name (read-only; editing requires SmartMoveSheet)
6. **Tags** — `TagsPickerModal`, shows tag chips
7. **Recurrence** — `RecurrencePickerSheet`
8. **Dependency** — "Blocked by" task
9. **Reminders** — list of scheduled reminders + add new
10. **Attachments** — file list (icon+name+size+delete+open) + "Add file" via `OpenDocument`
11. **Time Tracker** — Start/Pause/Stop + total elapsed display
12. **Comments** — chronological list + add new

---

## 12. Reminders System

- `ReminderScheduler` — wraps Android `AlarmManager`
- `ReminderReceiver` — `BroadcastReceiver`, fires notification on alarm
- `BootReceiver` — re-schedules all pending reminders after device reboot
- Two notification channels: sound channel + silent channel
- `NotificationPreferences` — DataStore toggles for sound and vibration

---

## 13. Attachments System

- Uses `ActivityResultContracts.OpenDocument()` (persisted URI permission)
- `context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`
- Metadata queried via `OpenableColumns.DISPLAY_NAME` + `OpenableColumns.SIZE`
- Stored as `Attachment(uri = contentUri.toString(), ...)`
- `AttachmentEntity` table, `AttachmentDao`, `AttachmentRepository`
- `Task.hasAttachments: Boolean` — paperclip indicator in `TaskItem`
- File opened via `Intent(Intent.ACTION_VIEW)` with URI + mimeType

MIME icon mapping:
- `image/*` → `Icons.Outlined.Image`
- `video/*` → `Icons.Outlined.Videocam`
- `audio/*` → `Icons.Outlined.MusicNote`
- `application/pdf` → `Icons.Outlined.Description`
- `*` → `Icons.Outlined.Attachment`

---

## 14. Projects / Kanban Screen

File: `ui/screens/projects/ProjectsScreen.kt`

Two view modes: **Board** (Kanban columns) and **List**.

Column headers use dark-mode-aware colours:
```kotlin
private val ProjectStatus.columnHeaderColor: Color
    @Composable get() {
        val dark = isSystemInDarkTheme()
        return when (this) {
            NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant
            IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
            WAITING -> if (dark) Color(0xFF4D3A00) else Color(0xFFFFF3CD)
            DONE    -> if (dark) Color(0xFF1A3D2A) else Color(0xFFD4EDDA)
        }
    }

private val ProjectStatus.columnHeaderContentColor: Color
    @Composable get() {
        val dark = isSystemInDarkTheme()
        return when (this) {
            NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
            IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
            WAITING -> if (dark) Color(0xFFFFE082) else Color(0xFF5D4200)
            DONE    -> if (dark) Color(0xFF81C995) else Color(0xFF1B5E35)
        }
    }
```

---

## 15. Upcoming Screen — Drag to Reschedule

File: `ui/screens/upcoming/UpcomingScreen.kt`

Uses `sh.calvin.reorderable`:
- `rememberReorderableLazyListState`
- `ReorderableItem` + `longPressDraggableHandle()`

Cross-date drag detect pattern (must use `snapshotFlow`, NOT `LaunchedEffect(key)`):
```kotlin
LaunchedEffect(reorderState) {
    snapshotFlow { reorderState.isAnyItemDragging }
        .collect { isDragging ->
            if (!isDragging && pendingReschedule) {
                // find targetDate from current position, dispatch RescheduleTask event
            }
        }
}
```

---

## 16. Section Delete — Orphan Fix

`ProjectRepositoryImpl.deleteSection()`:
```kotlin
override suspend fun deleteSection(sectionId: String) {
    taskDao.clearSection(sectionId, LocalDateTime.now().toString())  // SET sectionId=NULL
    sectionDao.delete(sectionId)
}
```
`TaskDao.clearSection`: `UPDATE tasks SET sectionId = NULL, updatedAt = :now WHERE sectionId = :sectionId`

`ProjectRepositoryImpl` constructor now takes `taskDao` as a parameter, wired in `AppContainer`.

---

## 17. Inbox Multi-Select & Bulk Ops

`InboxUiState`:
- `selectedTaskIds: Set<String>` — long-press to enter selection mode
- `showMoveSheet`, `showBulkMore`, `showAddTask`, `pendingDeleteTask`

`InboxEvent`:
- `ToggleSelect`, `ClearSelection`, `DeleteSelected`
- `ShowMoveSheet` / `HideMoveSheet` → `SmartMoveSheet`
- `ShowBulkMore` / `HideBulkMore` → `BulkMoreSheet`
- `BulkStar(starred)`, `BulkSetPriority(priority)`, `BulkSchedule(date?)`
- `MoveSelected(scheduledGroup?, projectId?, sectionId?)`

BackHandler dismisses selection mode (clears `selectedTaskIds`).

---

## 18. Common Patterns & Pitfalls

### Adding a new feature (standard pattern)
1. Add field to `Models.kt` domain model
2. Add field to `*Entity.kt`, update `toDomain()` + `toEntity()`
3. Bump `AppDatabase.version` by 1
4. Add query to DAO
5. Add method to repository interface + impl
6. Add to `AppContainer` if new repo
7. Add to UiState + Event + ViewModel.onEvent()
8. Wire in Screen composable

### Common gotchas
- **DB version**: Always bump when adding a column to any entity
- **`fallbackToDestructiveMigration()`**: Existing data wiped on version bump in dev — this is intentional
- **`snapshotFlow` not `LaunchedEffect(key)`**: For observing Compose State inside `LaunchedEffect`, always use `snapshotFlow { someState }` — LaunchedEffect only reruns on key change, not on every state change
- **MetaRow trailing + value**: Both `value` and `trailing` render simultaneously (value first, trailing after). They are NOT mutually exclusive.
- **Soft delete everywhere**: Never call `taskRepo.delete()` from user-facing delete actions. Use `taskRepo.softDelete()`. Hard delete only for: ConvertToProject (removes original task), EmptyTrash (permanent wipe).
- **Inbox definition**: A task is in Inbox only if `projectId IS NULL AND scheduledGroup IS NULL`. Tasks with a project but no scheduledGroup go to Anytime or Someday based on priority.

### ViewModel injection
Always `AndroidViewModel(app: Application)`, access container via `(app as OnlyFlowsApp).container`

### Icon drawables
Custom SVGs used (in `res/drawable/`):
- `ic_nav_inbox`, `ic_nav_today`, `ic_nav_upcoming`, `ic_nav_anytime`, `ic_nav_someday`, `ic_nav_logbook`
- `ic_task_checkmark`, `ic_task_unchecked`

---

## 19. Build Info

./gradlew assembleDebug
```
Last successful build: DB v20, 0 errors, deprecation warnings only (AutoMirrored icons — cosmetic, harmless).

Deprecation warnings to ignore (pre-existing):
- `Icons.Outlined.FormatListBulleted` → use AutoMirrored
- `Icons.Outlined.Article` → use AutoMirrored
- `Icons.Outlined.OpenInNew` → use AutoMirrored
- `Icons.Outlined.VolumeUp` → use AutoMirrored
- `Icons.Outlined.Label` → use AutoMirrored
