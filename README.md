<div align="center">

# OnlyFlows

Task & project manager — minimal, editorial, local-first

Jetpack Compose × Room × LAN Sync × WorkManager

[![version](https://img.shields.io/badge/version-0.50.3-blue?style=flat-square)](https://github.com/Iurika-N-T-R/OnlyFlows/releases)
[![kotlin](https://img.shields.io/badge/kotlin-2.1.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![android](https://img.shields.io/badge/android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![minSdk](https://img.shields.io/badge/minSdk-26-green?style=flat-square)](https://developer.android.com/about/versions/oreo)
[![license](https://img.shields.io/badge/license-Proprietary-red?style=flat-square)](LICENSE)

</div>

---

## Features

### Tasks
- Quick creation via AddTaskSheet (title, priority, project/section, date)
- Full task detail: note, checklist, start date, due date (with time slot), priority, tags, recurrence, dependencies, reminders, attachments, comments, time tracker
- Complete via swipe or toggle; auto-complete at 100% checklist (800 ms delay)
- Full recurrence: HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS, basis COMPLETION_DATE / DUE_DATE / SPECIFIC_DATE, skip weekends option
- Task dependencies (`dependsOnTaskId`) with lock badge and "blocked by" section
- Convert Task → Project (checklist items → project tasks)
- Soft delete (Trash) with restore
- Multi-selection (long-press): Move, Delete, More (Star / Priority / Tag / Schedule / **Start Timer**)

### Views
| View | Description |
|---|---|
| **Today** | Tasks for today grouped Overdue / Today / This Evening, smart sort; completed/total badge; dynamic tag filters |
| **Upcoming** | Infinite scroll −14d → +365d, week / month mode, drag-to-reschedule |
| **Inbox** | Unprocessed tasks (no project, no planned group) |
| **Anytime** | Project tasks without date + priority, or explicitly moved here |
| **Someday** | Project tasks without date without priority, or explicitly moved here |
| **Projects** | Kanban (4-column board) + list toggle |
| **Project Detail** | Sections, drag-to-reorder, tag/priority filters, pie chart, total time, notes, comments |
| **Task Detail** | Full edit screen |
| **Logbook** | Archived projects |
| **Trash** | Soft-deleted tasks with restore |
| **QuickFind** | Real-time search + natural date parsing FR/EN |

### Goals
- Create goals with target value, unit, and deadline
- Track progress via goal log entries
- Link tasks to goals
- Goal card UI with progress indicator
- Filter action log by goal tag

### Projects
- Types: PROJECT, LIST, NOTE
- Statuses: NOT_STARTED / IN_PROGRESS / WAITING / DONE (auto-DONE at 100%)
- **Archive / Unarchive** — long-press any LIST, NOTE or PROJECT in the sidebar → Archive; collapsible "Archived" section with one-tap restore
- Sections with drag-to-reorder, deadline with colored countdown
- Total tracked time (sum of all task TimeEntries)
- Status change history (`StatusLogEntry`)
- Inline editable notes, comment timeline, child lists (sub-projects)

### Reminders & Notifications
- Multiple reminders per task (exact AlarmManager, Doze exemption)
- Separate sound/silent channels, bypass DND, configurable vibration
- Auto-reschedule on reboot (BootReceiver)
- Daily alert at fixed times (configurable days Mon–Sun)
- Geofence-based alerts (GPS offline, named saved locations)

### Time Tracking
- Start / Pause / Stop per task in TaskDetailScreen
- Real-time ticker (1-second updates)
- Quick start from multi-selection (long-press → More → "Start Timer")
- Total displayed in ProjectDetailScreen (sum of all completed entries)

### LAN Sync
- Peer-to-peer sync between devices on the local network (no cloud, no account)
- Device discovery + pairing, delta exchange over LAN
- Last-Write-Wins conflict resolution on `updatedAt` field
- Tombstone-based delete propagation
- Force sync button in settings

### Attachments
- Add file attachments to tasks
- Metadata JSON for each attachment (metadata syncs; file transfer not yet supported over LAN sync)

### Tablet Support
- Permanent sidebar on wide screens (`WindowWidthSizeClass.EXPANDED`)
- Hardware keyboard shortcuts
- Adaptive layout (`AdaptiveAppLayout`)

### Settings
| Screen | Options |
|---|---|
| General | 24h/12h time format, clickable URLs in notes, text size |
| Appearance | Dark / Light / System theme |
| Notifications | Sound, vibration, bypass DND |
| Calendar | Local calendar integration |
| Backup | Export / import |
| LAN Sync | Device pairing, sync status, force sync |
| About | Version, stack, licenses |

---

## Architecture

```
app/src/main/java/com/onlyflows/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          Room DB, seed callback
│   │   ├── Converters.kt           LocalDate / LocalDateTime / JSON
│   │   ├── entity/                 TaskEntity, ProjectEntity, GoalEntity…
│   │   └── dao/                    TaskDao, SyncDao, GoalDao…
│   ├── repository/                 Repository classes
│   ├── lan/                        LanSyncCoordinator, LanSyncWriter, EntityJson…
│   ├── notifications/              ReminderScheduler, DailyAlertScheduler, GeofenceManager
│   ├── preferences/                AppPreferences, NotificationPreferences…
│   ├── calendar/                   CalendarRepository
│   └── backup/                     BackupManager
├── domain/
│   ├── model/Models.kt             Task, Project, Goal, TimeEntry, Reminder…
│   └── RecurrenceScheduler.kt      nextDueDate() pure function
├── ui/
│   ├── screens/                    today / inbox / upcoming / anytime / someday /
│   │                               projects / project / task / logbook / trash /
│   │                               search / settings (+ lan sync)
│   ├── components/                 TaskItem, SelectionBar, TaskFilterBar…
│   ├── overlays/                   AddTaskSheet, DatePicker, QuickFindPopup…
│   ├── layout/                     AdaptiveAppLayout (phone + tablet)
│   ├── navigation/                 AppNavigation, Screen
│   └── theme/                      Color tokens, typography, Material 3
├── AppContainer.kt                 Manual DI root
├── OnlyFlowsApp.kt                 Application class
└── MainActivity.kt
```

**Key patterns:**
- **Strict UDF** — immutable `data class UiState`, explicit `sealed interface Event`, no side-effects in composables
- **No navigation in ViewModels** — screens receive navigation lambdas; VMs expose `StateFlow` only
- **Room as source of truth** — UI observes `Flow<List<T>>`; no duplicated local state except visual transitions
- **Systematic soft delete** — `deletedAt: LocalDateTime?` on all deletable entities; all DAO queries filter `WHERE deletedAt IS NULL`
- **Strict repository pattern** — ViewModels never access DAOs directly
- **Manual injection** — `AppContainer` wires all dependencies; ViewModels retrieve via `(app as OnlyFlowsApp).container`

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| State | ViewModel + StateFlow (strict UDF) |
| Persistence | Room v2.6.1 — local-first SQLite |
| Preferences | DataStore Preferences |
| Navigation | Navigation Compose (type-safe routes) |
| Drag & Drop | sh.calvin.reorderable |
| Async | Kotlin Coroutines + Flow |
| DI | AppContainer (manual injection) |
| Notifications | AlarmManager (exact alarms) + BroadcastReceiver |
| Background | WorkManager |
| Sync | LAN sync (device discovery + delta exchange) |
| Integration | CalendarProvider, FileProvider |

---

## Docs

- [docs/BUILD.md](docs/BUILD.md) — How to build from source
- [CLAUDE.md](CLAUDE.md) — Architecture & structure reference

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
