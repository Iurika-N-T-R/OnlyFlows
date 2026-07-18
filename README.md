<div align="center">

# OnlyFlows

Task & project manager — minimal, editorial, local-first

Jetpack Compose × Room × LAN Sync × WorkManager

[![version](https://img.shields.io/badge/version-0.68.0-blue?style=flat-square)](https://github.com/Iurika-N-T-R/OnlyFlows/releases)
[![kotlin](https://img.shields.io/badge/kotlin-2.1.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![android](https://img.shields.io/badge/android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![minSdk](https://img.shields.io/badge/minSdk-26-green?style=flat-square)](https://developer.android.com/about/versions/oreo)
[![license](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square)](LICENSE)

</div>

---

## About

OnlyFlows is inspired by **Things 3**, but goes further: a broader feature set (goals with periodic
tracking, RSS reader, date tracker, time tracking, geofence reminders, home screen widget, statistics)
and is **fully offline by design** — no account, no cloud, no telemetry. Sync between devices happens
entirely over **LAN**, peer-to-peer, with no server in between.

Licensed under **GPLv3** — free to use, study, modify, and redistribute under the same license.
Commercial resale of this app or its source, even though technically permitted by GPLv3, is not
something the author endorses; please respect the spirit of a free, offline-first project.

---

## Features

### Tasks
- Quick creation via AddTaskSheet (title, priority, project/section, date, checklist)
- Full task detail: note, checklist (with sections + per-item reminders), start date, due date (with time slot), priority, difficulty, impact, tags, recurrence, dependencies, reminders, attachments, comments, time tracker, linked goals
- Complete via swipe or toggle; auto-complete at 100% checklist (800 ms delay)
- Full recurrence: HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS, basis COMPLETION_DATE / DUE_DATE / SPECIFIC_DATE, skip weekends, end date / occurrence limit (enforced on every completion path)
- Recurring task journal: each completion/abandon adds an auto comment ("🔁 Done — 3 Jul 2026"); full history carried onto each new occurrence
- Reminders follow recurring tasks: completing an occurrence moves its reminders (and imminence alert) onto the next one, shifted with the due date
- Task dependencies (`dependsOnTaskId`) with lock badge and "blocked by" section
- Convert Task → Project (checklist items → project tasks)
- Soft delete (Trash) with restore
- Multi-selection (long-press): Move, Delete, More (Star / Priority / Tag / Schedule / Start Timer)

### Views
| View | Description |
|---|---|
| **Today** | Tasks for today grouped Overdue / Today / This Evening, smart sort; completed/total badge; dynamic tag filters |
| **Upcoming** | Infinite scroll −14d → +365d, week / month mode, drag-to-reschedule |
| **Inbox** | Unprocessed tasks (no project, no planned group) |
| **Anytime** | Project tasks without date + priority, or explicitly moved here |
| **Someday** | Project tasks without date without priority, or explicitly moved here |
| **Projects** | Centered kanban (active column in the middle, neighbours peeking) with drag & drop between statuses, per-status stat row, status icons; collapsible status-card list view with folder → project → sub-list hierarchy; Board/List switch; view/column/collapse state persisted |
| **Project Detail** | Tabs Tasks/Lists/Notes · Archived (n) · Comments (n); sections, drag-to-reorder, tag/priority filters, pie chart, total time; comments as a messaging panel |
| **Task Detail** | Full edit screen |
| **Logbook** | Archived tasks & projects + action journal |
| **Trash** | Soft-deleted tasks with restore |
| **QuickFind** | Real-time search + natural date parsing FR/EN + date-range results |

### Goals
- Five frequencies: **ONCE, DAILY, WEEKLY, MONTHLY, YEARLY** — periodic goals reset their counter at each period rollover (midnight / Monday / 1st of month / Jan 1); ONCE never resets
- Link tasks to goals: completing a linked task auto-increments the goal from **every** completion path (lists, task detail, widget, notification action, checklist auto-complete, recurring occurrences); un-completing decrements
- Target count, emoji, color, tags, optional deadline; auto-completed at target
- Status: NOT_STARTED / ON_TRACK / AT_RISK (pace vs. deadline) / COMPLETED, with filter chips
- Journal per goal (log entries with notes), goal picker in task detail

### Projects, Lists & Notes
- Types: PROJECT, LIST, NOTE, organized in folders with child lists (sub-projects)
- Statuses: NOT_STARTED / IN_PROGRESS / WAITING / DONE (auto-DONE at 100%) with status change history
- Lists of lists as card grids (adaptive 2–4 columns) with synced image covers and item counters
- **Notes v2**: note groups as cards, pin, archive/restore, masonry layout, full Markdown (code blocks, tables, nested lists, quotes, links, headings)
- Archive / Unarchive from the sidebar; collapsible "Archived" section with one-tap restore
- Sections with drag-to-reorder, deadline with colored countdown, total tracked time

### Reminders & Notifications
- Multiple reminders per task (exact AlarmManager alarms, Doze exemption); one-shot or self-recurring DAILY/WEEKLY
- Imminence alert N minutes before due time (configurable lead, default 30 min)
- Checklist item reminders (one-shot, per item)
- Daily agenda alerts at fixed times (configurable days Mon–Sun) + weekly recap
- Geofence-based alerts (GPS offline, named saved locations)
- Notification actions: Complete (recurrence-aware) / Snooze 15 min
- Separate sound/silent channels, bypass DND, configurable vibration
- Auto-reschedule on reboot (BootReceiver)

### Home Screen Widget
- Classic RemoteViews scrollable list — dark/light theme, per-widget filter (Today / Inbox / Anytime / Upcoming / Someday / Project / All)
- Complete tasks from the widget (recurrence-aware), tap to open, quick-add button
- Task metadata row (date, project, tags, indicators), overdue in red
- Auto-refresh: instant on any in-app mutation or LAN sync delta + ~10 s self-chaining alarm

### Time Tracking
- Start / Pause / Stop per task in TaskDetailScreen, real-time ticker
- Quick start from multi-selection (long-press → More → "Start Timer")
- Total displayed in ProjectDetailScreen (sum of all completed entries)

### LAN Sync
- Peer-to-peer sync between devices on the local network (no cloud, no account)
- Device discovery + pairing, delta exchange over LAN (HTTP)
- Last-Write-Wins conflict resolution on `updatedAt`, tombstone-based delete propagation
- Everything syncs: tasks, checklists, projects, notes, comments, reminders, goals + logs, date trackers, time entries, tags, folders, locations
- Force sync button in settings

### More
- **DateTracker** — custom date anniversaries / countdowns
- **RSS Reader** — local reader (Atom + RSS), no WebView, background sync via WorkManager
- **Statistics** — task completion stats screen
- **QuoteCard** — offline quotes in sidebar and project screens
- **AppLog** — in-app debug log viewer
- **Attachments** — file attachments per task (metadata syncs; file transfer over LAN not yet supported)
- **Backup** — export / import via WorkManager

### Tablet Support
- Permanent sidebar on wide screens (`WindowWidthSizeClass.EXPANDED`)
- Hardware keyboard shortcuts
- Adaptive layout (`AdaptiveAppLayout`)

### Settings
| Screen | Options |
|---|---|
| General | 24h/12h time format, clickable URLs in notes, text size, sidebar sections |
| Appearance | Dark / Light / System theme |
| Notifications | Sound, vibration, bypass DND, imminence lead, daily alerts, weekly recap |
| Calendar | Local calendar integration |
| Backup | Export / import |
| LAN Sync | Device pairing, sync status, force sync |
| Locations | Saved locations for geofence reminders |
| Tags | Tags manager |
| Statistics | Completion stats |
| Logs | In-app debug log |
| About | Version, stack, licenses |

---

## Architecture

Clean Architecture with strict Unidirectional Data Flow (UDF).

```
app/src/main/java/com/onlyflows/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          Room DB (v38, 24 entities), non-destructive migrations
│   │   ├── Converters.kt           LocalDate / LocalDateTime / LocalTime / JSON
│   │   ├── entity/                 TaskEntity, ProjectEntity, GoalEntity, RssFeedEntity…
│   │   └── dao/                    TaskDao, SyncDao, GoalDao…
│   ├── repository/                 Repository classes (no interfaces, Flow-based)
│   ├── lan/                        LanSyncCoordinator, LanSyncWriter, EntityJson…
│   ├── notifications/              ReminderScheduler, DailyAlertScheduler, GeofenceManager,
│   │                               ReminderReceiver, ImminenceReceiver, BootReceiver
│   ├── preferences/                AppPreferences, NotificationPreferences…
│   ├── calendar/                   CalendarRepository
│   ├── backup/                     BackupManager
│   └── debug/                      AppLog
├── domain/
│   ├── model/Models.kt             Task, Project, Section, Tag, Reminder, TimeEntry…
│   ├── model/Goal.kt · GoalLog.kt  Goals system (frequencies, period rollover)
│   └── RecurrenceScheduler.kt      nextDueDate() pure function
├── ui/
│   ├── screens/                    today / inbox / upcoming / anytime / someday /
│   │                               projects / project / task / logbook / trash / search /
│   │                               goals / datetracker / rss / settings (11 sub-screens)
│   ├── components/                 TaskItem, SwipeableTaskItem, SelectionBar, AgendaCard…
│   ├── overlays/                   AddTaskSheet, SmartMoveSheet, NoteEditorSheet,
│   │                               QuickFindOverlay, AddGoalSheet, GoalPickerSheet…
│   ├── widget/                     OnlyFlowsWidgetProvider, WidgetUpdater, quick-add & config
│   ├── layout/                     AdaptiveAppLayout (phone + tablet)
│   ├── navigation/                 AppNavigation, Screen
│   └── theme/                      Color tokens, typography, Material 3
├── AppContainer.kt                 Manual DI root
├── OnlyFlowsApp.kt                 Application class
└── MainActivity.kt
```

**Key patterns:**
- **Strict UDF** — one immutable `StateFlow<UiState>` per screen, `sealed interface Event` + `onEvent()`, no side-effects in composables
- **No navigation in ViewModels** — screens receive navigation lambdas; VMs expose `StateFlow` only
- **Room as source of truth** — UI observes `Flow<List<T>>`; no duplicated local state except visual transitions
- **Business rules live in repositories** — goal progress, recurrence occurrence limit, reminder carry-over all sit in `TaskRepository`, so every caller (screens, widget, notifications) behaves identically
- **Systematic soft delete** — `deletedAt: LocalDateTime?` on all deletable entities; all DAO queries filter `WHERE deletedAt IS NULL`
- **Strict repository pattern** — ViewModels never access DAOs directly; every write also feeds `LanSyncWriter`
- **Manual injection** — `AppContainer` wires all dependencies (LAN sync components first); ViewModels retrieve via `(app as OnlyFlowsApp).container`

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| State | ViewModel + StateFlow (strict UDF) |
| Persistence | Room 2.6.1 — local-first SQLite (schema v38, 24 entities) |
| Preferences | DataStore Preferences |
| Navigation | Navigation Compose |
| Drag & Drop | sh.calvin.reorderable |
| Async | Kotlin Coroutines + Flow |
| DI | AppContainer (manual injection) |
| Notifications | AlarmManager (exact alarms) + BroadcastReceiver + geofences |
| Widget | Classic RemoteViews collection (RemoteViewsCompat) |
| Background | WorkManager (backup, RSS sync) |
| Sync | LAN sync (device discovery + HTTP delta exchange) |
| Integration | CalendarProvider, FileProvider |

Kotlin 2.1.0 · AGP 8.7.3 · minSdk 26 · targetSdk 35

---

## Docs

- [docs/BUILD.md](docs/BUILD.md) — How to build from source
- [CLAUDE.md](CLAUDE.md) — Architecture & structure reference
- [BRIEF.md](BRIEF.md) — Project brief (goals, users, scope)

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
