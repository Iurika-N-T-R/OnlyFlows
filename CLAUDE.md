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
For build: export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assembleDebug 2>&1 | tail -80

There are no unit or instrumented tests in this project.

## Architecture Overview

OnlyFlows is a Jetpack Compose Android app (minSdk 26, targetSdk 35, Kotlin 2.1.0) following **Clean Architecture with strict Unidirectional Data Flow (UDF)**.

### Layer Structure

```
UI (Compose Screens + ViewModels)
  ↕ StateFlow<UiState> / onEvent()
Domain (Models + RecurrenceScheduler)
  ↕ Repository interfaces
Data (RepositoryImpl + Room DAOs + DataStore)
  ↕
Local DB (Room v2.6.1) + DataStore Preferences
```

### Key Patterns

**UDF** — Each screen has a paired ViewModel that exposes a single `StateFlow<UiState>` (immutable data class). The UI calls `viewModel.onEvent(Event)` for all interactions. Screens receive navigation as lambda callbacks; ViewModels never navigate.

**Repository pattern** — ViewModels call repository interfaces (`domain/repository/`), never DAOs directly. Repository implementations (`data/repository/`) use `flatMapLatest` + `combine` to join Room entities and emit domain models as `Flow<T>`.

**Room as source of truth** — All data is observed as `Flow<List<T>>`. No duplicate state in ViewModels; only transient UI state (e.g., `movingToCompletedIds` for animations).

**Soft delete** — Every deletable entity has `deletedAt: LocalDateTime?`. All DAO queries filter `WHERE deletedAt IS NULL`.

**Manual DI via AppContainer** — `AppContainer.kt` manually constructs all repositories, schedulers, and preferences. Access it from any composable/ViewModel as `(application as OnlyFlowsApp).container`.

### Entry Points

- [OnlyFlowsApp.kt](app/src/main/java/com/onlyflows/OnlyFlowsApp.kt) — Application class; initializes `AppContainer`, notification channels
- [MainActivity.kt](app/src/main/java/com/onlyflows/MainActivity.kt) — Single activity; edge-to-edge, theme, permission requests
- [AppContainer.kt](app/src/main/java/com/onlyflows/AppContainer.kt) — Manual DI root; wires all dependencies
- [AppNavigation.kt](app/src/main/java/com/onlyflows/ui/navigation/AppNavigation.kt) — NavHost; all route registrations and navigation callbacks
- [Screen.kt](app/src/main/java/com/onlyflows/ui/navigation/Screen.kt) — Sealed class; all navigation routes

### Data Layer

- [AppDatabase.kt](app/src/main/java/com/onlyflows/data/local/AppDatabase.kt) — Room DB (v22, 14 entities, `fallbackToDestructiveMigration`, seed callback)
- [Models.kt](app/src/main/java/com/onlyflows/domain/model/Models.kt) — All domain models: `Task`, `Project`, `Section`, `Tag`, `Reminder`, `TimeEntry`, `Comment`, enums (`TaskPriority`, `RecurrenceType`, `ActionType`, …)
- [Converters.kt](app/src/main/java/com/onlyflows/data/local/Converters.kt) — Room TypeConverters for `LocalDate`, `LocalDateTime`, `LocalTime`, JSON

### UI Structure

- `ui/screens/` — 13 screen folders (`today`, `inbox`, `upcoming`, `anytime`, `someday`, `projects`, `project`, `task`, `logbook`, `trash`, `search`, `settings`). Each has `*Screen.kt` + `*ViewModel.kt`.
- `ui/components/` — Reusable composables (`TaskItem`, `SelectionBar`, `TaskFilterBar`, `EmptyState`, `AgendaCard`, …)
- `ui/overlays/` — Modal bottom sheets (`AddTaskSheet`, `DatePickerModal`, `MoveTaskSheet`, `NoteEditorSheet`, `QuickFindPopup`, …)
- `ui/layout/AdaptiveAppLayout.kt` — Phone: `ModalNavigationDrawer`; Tablet (`WindowWidthSizeClass.EXPANDED`): permanent sidebar in a `Row`

### Notifications & Background

- `ReminderScheduler` — `AlarmManager` exact alarms; re-scheduled after boot via `BootReceiver`
- `DailyAlertScheduler` / `GeofenceManager` — location-based and daily summary alerts
- `WorkManager` — Backup export/import

### Recurrence

- [RecurrenceScheduler.kt](app/src/main/java/com/onlyflows/domain/RecurrenceScheduler.kt) — Pure function `nextDueDate()` supporting HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS with optional `skipWeekends`

## Important Files for Context

- [README.md](README.md) — Feature list and architecture conventions
- [doc.md](doc.md) — Detailed folder structure, all DAO methods, navigation routes, domain model fields
- [Todo.md](Todo.md) — Planned and in-progress features
- [CHANGELOG.md](CHANGELOG.md) — Version history
