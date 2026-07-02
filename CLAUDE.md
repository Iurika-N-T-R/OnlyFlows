# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug       # Build debug APK
./gradlew assembleRelease     # Build release APK
./gradlew build               # Full build (all variants)
./gradlew clean               # Clean build outputs
./gradlew lint                # Run Android Lint
```

On Windows with Git Bash (required for JAVA_HOME resolution):
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assembleDebug 2>&1 | tail -80
```

There are no unit or instrumented tests in this project.

## Architecture Overview

OnlyFlows is a Jetpack Compose Android app (minSdk 26, targetSdk 35, Kotlin 2.1.0) following **Clean Architecture with strict Unidirectional Data Flow (UDF)**.

### Layer Structure

```
UI (Compose Screens + ViewModels)
  ↕ StateFlow<UiState> / onEvent()
Domain (Models + RecurrenceScheduler)
  ↕ Repositories
Data (Repositories + Room DAOs + DataStore + LanSyncWriter)
  ↕
Local DB (Room v30, 21 entities) + DataStore Preferences
```

### Key Patterns

**UDF** — Each screen has a paired ViewModel exposing a single `StateFlow<UiState>` (immutable data class). The UI calls `viewModel.onEvent(Event)` for all interactions. Screens receive navigation as lambda callbacks; ViewModels never navigate.

**Repository pattern** — ViewModels call repositories (concrete classes in `data/repository/`, no interfaces), never DAOs directly. Repositories use `flatMapLatest` + `combine` to join Room entities and emit domain models as `Flow<T>`.

**Room as source of truth** — All data is observed as `Flow<List<T>>`. No duplicate state in ViewModels; only transient UI state (e.g., `movingToCompletedIds` for animations).

**Soft delete** — Every deletable entity has `deletedAt: LocalDateTime?`. All DAO queries filter `WHERE deletedAt IS NULL`. Never call `taskRepo.delete()` from user-facing delete actions — use `taskRepo.softDelete()`. Hard delete only for: ConvertToProject (removes original task) and EmptyTrash.

**LAN sync** — Every repository receives `syncDao` + `lanSyncWriter`. All writes call `lanSyncWriter` after the Room DAO call. `LanSyncCoordinator` (declared first in AppContainer) is the central orchestrator.

**Manual DI via AppContainer** — `AppContainer.kt` manually constructs all dependencies in declaration order (LAN sync components first, then repos that depend on them). Access via `(application as OnlyFlowsApp).container`.

### Entry Points

- [OnlyFlowsApp.kt](app/src/main/java/com/onlyflows/OnlyFlowsApp.kt) — Application class; initializes `AppContainer`, notification channels
- [MainActivity.kt](app/src/main/java/com/onlyflows/MainActivity.kt) — Single activity; edge-to-edge, theme, permission requests
- [AppContainer.kt](app/src/main/java/com/onlyflows/AppContainer.kt) — Manual DI root; wires all dependencies
- [AppNavigation.kt](app/src/main/java/com/onlyflows/ui/navigation/AppNavigation.kt) — NavHost; all route registrations
- [Screen.kt](app/src/main/java/com/onlyflows/ui/navigation/Screen.kt) — Sealed class; all navigation routes

### Data Layer

- [AppDatabase.kt](app/src/main/java/com/onlyflows/data/local/AppDatabase.kt) — Room DB (v30, 21 entities, `fallbackToDestructiveMigration`, seed callback)
- [Models.kt](app/src/main/java/com/onlyflows/domain/model/Models.kt) — Core domain models: `Task`, `Project`, `Section`, `Tag`, `Reminder`, `TimeEntry`, `Comment`, enums
- [Goal.kt](app/src/main/java/com/onlyflows/domain/model/Goal.kt) / [GoalLog.kt](app/src/main/java/com/onlyflows/domain/model/GoalLog.kt) — Goals system domain models
- [Converters.kt](app/src/main/java/com/onlyflows/data/local/Converters.kt) — Room TypeConverters for `LocalDate`, `LocalDateTime`, `LocalTime`, JSON

**DB entities (21):** TaskEntity, ChecklistItemEntity, ProjectEntity, SectionEntity, TagEntity, FolderEntity, StatusLogEntity, NoteEntity, TimeEntryEntity, CommentEntity, ReminderEntity, AttachmentEntity, ActionLogEntity, SavedLocationEntity, SyncMetaEntity, DeletedEntityEntity, GoalEntity, GoalLogEntity, DateTrackerEntity, RssFeedEntity, RssArticleEntity

### UI Structure

- `ui/screens/` — Screen folders, each with `*Screen.kt` + `*ViewModel.kt`:
  - Core task screens: `today`, `inbox`, `upcoming`, `anytime`, `someday`, `logbook`, `trash`
  - Project screens: `projects` (Kanban + List), `project` (detail)
  - `task` (detail), `search` (QuickFind + DateResults)
  - `goals` (list + detail), `datetracker`, `rss` (local reader + article)
  - `settings` (appearance, general, calendar, backup, notifications, lan sync, locations, tags, statistics, logs, about)
- `ui/components/` — Reusable composables (`TaskItem`, `SwipeableTaskItem`, `SelectionBar`, `TaskFilterBar`, `EmptyState`, `AgendaCard`, …)
- `ui/overlays/` — Modal bottom sheets (`AddTaskSheet`, `DatePickerModal`, `SmartMoveSheet`, `BulkMoreSheet`, `NoteEditorSheet`, `QuickFindOverlay`, `AddGoalSheet`, `GoalPickerSheet`, …)
- `ui/layout/AdaptiveAppLayout.kt` — Phone: `ModalNavigationDrawer`; Tablet (`WindowWidthSizeClass.EXPANDED`): permanent sidebar in a `Row`
- `ui/widget/` — RemoteViews home screen widget (`OnlyFlowsWidgetProvider`; dark/light, recurrence-safe, refreshed via `WidgetUpdater` on task mutations)

### Notifications & Background

- `ReminderScheduler` — `AlarmManager` exact alarms; re-scheduled after boot via `BootReceiver`
- `DailyAlertScheduler` / `GeofenceManager` — location-based and daily summary alerts
- `WorkManager` — Backup export/import + RSS periodic sync
- Two notification channels: sound channel + silent channel

### LAN Sync

Files: `data/lan/`

- `LanSyncWriter` — repositories call it after each mutation; enqueues entity JSON on the coordinator
- `LanSyncCoordinator` — orchestrates discovery, server, client; exchanges deltas with paired devices (last-write-wins on `updatedAt`)
- `LanSyncServer` / `LanSyncClient` — HTTP-over-LAN transport between devices
- `LanSyncDiscovery` — device discovery on the local network
- `EntityJson.kt` — `toJson()` / `*EntityFromJson()` for every synced entity
- `DeletedEntityEntity` — tombstone table used to propagate deletes across devices

### Recurrence

- [RecurrenceScheduler.kt](app/src/main/java/com/onlyflows/domain/RecurrenceScheduler.kt) — Pure function `nextDueDate()` supporting HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS with optional `skipWeekends`

### Goals System

- `GoalsScreen` + `GoalDetailScreen` — track habits/goals with log entries
- `GoalRepository` / `GoalLogRepository` — both sync via LAN sync
- `GoalPickerSheet` — picker used in TaskDetail to link a task to a goal

### Other Features

- **DateTracker** — tracks custom date anniversaries/countdowns (`DateTrackerEntity`, synced)
- **RSS Reader** — local reader with `RssFeedEntity` + `RssArticleEntity`; no WebView, no network polling at read time; Atom + RSS parser
- **QuoteCard** — quotes loaded from `quoteRepository` (asset file, no network)
- **Statistics** — task completion stats screen
- **AppLog** — in-app debug log viewer (`data/debug/AppLog.kt`, `LogsScreen`)

## Adding a New Feature — Standard Checklist

1. Add field to `Models.kt` domain model
2. Add field to `*Entity.kt`, update `toDomain()` + `toEntity()`
3. **Bump `AppDatabase.version` by 1** (fallbackToDestructiveMigration wipes dev data — intentional)
4. Add query to DAO
5. Add method to the repository (include `lanSyncWriter` call after DAO call)
6. Add to `AppContainer` if new repo (after LAN sync components)
7. Add to UiState + Event sealed interface + `ViewModel.onEvent()`
8. Wire in Screen composable

## Common Gotchas

- **`snapshotFlow` not `LaunchedEffect(key)`** — to observe Compose State inside `LaunchedEffect`, always use `snapshotFlow { someState }.collect { ... }`. `LaunchedEffect` only reruns on key change.
- **Inbox definition** — A task is in Inbox only if `projectId IS NULL AND scheduledGroup IS NULL`. Tasks with a project but no scheduledGroup go to Anytime (priority ≠ NONE) or Someday (priority = NONE).
- **MetaRow trailing + value** — In `TaskDetailScreen`, both `value` and `trailing` render simultaneously — they are NOT mutually exclusive.
- **ViewModel injection** — Always `AndroidViewModel(app: Application)`, access container via `(app as OnlyFlowsApp).container`.
- **AppContainer declaration order** — LAN sync components must be declared before repositories because repos take `lanSyncWriter` as a constructor parameter.
- **Widget recurrence** — The widget calls `completeRecurring()` not `setCompleted()` to correctly create the next occurrence.

## Important Files for Context

- [Todo.md](Todo.md) — Planned and in-progress features
- [CHANGELOG.md](CHANGELOG.md) — Version history (authoritative for what changed and when)
