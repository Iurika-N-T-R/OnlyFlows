# OnlyFlows

Application Android native de gestion de tâches et projets.
Style minimal, éditorial, local-first. Inspiré de Things 3, conçu pour Android.

**Version actuelle : 0.18.0**

---

## Stack technique

| Couche | Technologie |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| State | ViewModel + StateFlow (UDF strict) |
| Persistance | Room v20 — SQLite local-first |
| Préférences | DataStore Preferences |
| Navigation | Navigation Compose (routes type-safe) |
| Drag & Drop | sh.calvin.reorderable |
| Async | Kotlin Coroutines + Flow |
| DI | AppContainer (injection manuelle) |
| Notifications | AlarmManager (exact alarms) + BroadcastReceiver |
| Intégration | CalendarProvider, FileProvider, WorkManager |

---

## Fonctionnalités implémentées

### Tâches
- Création rapide (AddTaskSheet) avec titre, priorité, projet/section, date
- Détail complet : note, checklist, date de début, date d'échéance (créneau horaire), priorité, tags, récurrence, dépendances, rappels, pièces jointes, commentaires, chronomètre
- Complétion avec swipe ou toggle ; auto-complétion à 100% checklist (délai 800 ms)
- Récurrence complète : HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS, basis COMPLETION_DATE / DUE_DATE / SPECIFIC_DATE, skip weekends
- Dépendances inter-tâches (`dependsOnTaskId`) avec badge 🔒 et section "interdépendant de"
- Convert Task → Project (checklist → tâches du projet)
- Suppression douce (Corbeille) avec restore
- Multi-sélection (long-press) : Move, Delete, More (Star / Priorité / Tag / Planifier / **Démarrer le minuteur**)

### Vues principales
| Vue | Description |
|---|---|
| **Today** | Tâches du jour groupées Overdue / Today / This Evening, tri intelligent |
| **Upcoming** | Scroll infini −14 j → +365 j, mode semaine / mois, drag-to-reschedule |
| **Inbox** | Tâches non traitées (sans projet ni groupe planifié) |
| **Anytime** | Tâches de projet sans date + priorité, ou déplacées explicitement |
| **Someday** | Tâches de projet sans date sans priorité, ou déplacées explicitement |
| **Projects** | Kanban (board 4 colonnes) + liste toggle |
| **Project Detail** | Sections, drag-to-reorder, filtres tag/priorité, pie chart, chrono total, notes, commentaires |
| **Task Detail** | Écran d'édition complet |
| **Logbook** | Projets archivés |
| **Trash** | Tâches supprimées softement avec restore |
| **QuickFind** | Recherche temps-réel + parsing date naturelle FR/EN |

### Projets
- Types : PROJECT, LIST, NOTE
- Statuts : NOT_STARTED / IN_PROGRESS / WAITING / DONE (auto-DONE à 100%)
- Sections drag-to-reorder, deadline avec countdown coloré
- Total temps suivi (somme des TimeEntries de toutes les tâches)
- Historique des changements de statut (`StatusLogEntry`)
- Notes inline éditables, commentaires timeline, listes enfants (sous-projets)

### Rappels & Notifications
- Rappels multiples par tâche (AlarmManager exact, exemption Doze)
- Canaux séparés son/silencieux, bypass DND, vibration configurable
- Reschedule automatique au reboot (BootReceiver)

### Chronomètre (Time Tracking)
- Start / Pause / Stop par tâche dans TaskDetailScreen
- Ticker temps réel (mise à jour chaque seconde)
- Démarrage rapide depuis la multi-sélection (long-press → More → "Démarrer le minuteur")
- Total affiché dans ProjectDetailScreen (somme de toutes les entrées terminées)

### Paramètres
| Écran | Options |
|---|---|
| Général | Format heure 24 h / 12 h, URLs cliquables dans les notes |
| Apparence | Thème Dark / Light / System |
| Notifications | Son, vibration, bypass DND |
| Calendrier | Intégration agendas locaux |
| Sauvegarde | Export / import |
| À propos | Version, stack, licences |

---

## Architecture

```
app/src/main/java/com/onlyflows/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          Room v20, seed callback
│   │   ├── Converters.kt           LocalDate / LocalDateTime / JSON
│   │   ├── entity/                 TaskEntity, ProjectEntity, TimeEntryEntity…
│   │   └── dao/                    TaskDao, TimeEntryDao, ReminderDao…
│   ├── repository/                 Implémentations (TaskRepositoryImpl…)
│   ├── notifications/              ReminderScheduler, ReminderReceiver, BootReceiver
│   ├── preferences/                AppPreferences, NotificationPreferences…
│   ├── calendar/                   CalendarRepository
│   └── backup/                     BackupManager
├── domain/
│   ├── model/Models.kt             Task, Project, TimeEntry, Reminder, Tag…
│   └── repository/                 Interfaces repository
├── ui/
│   ├── screens/                    today / inbox / upcoming / anytime / someday /
│   │                               projects / project / task / logbook / trash /
│   │                               search / settings
│   ├── components/                 TaskItem, SelectionBar, TaskFilterBar…
│   ├── overlays/                   BulkMoreSheet, AddTaskSheet, DatePicker…
│   ├── layout/                     AdaptiveAppLayout (smartphone + tablette)
│   ├── navigation/                 AppNavigation, Screen
│   └── theme/                      Tokens couleur, typo, Material 3
├── AppContainer.kt                 Injection manuelle de toutes les dépendances
├── OnlyFlowsApp.kt
└── MainActivity.kt
```

---

## Conventions de développement

- **UDF strict** — état immutable (`data class UiState`), événements explicites (`sealed interface Event`), aucun side-effect dans les composables
- **Pas de navigation dans les ViewModels** — les screens reçoivent des callbacks de navigation, le VM expose un `StateFlow` uniquement
- **Room = source de vérité** — l'UI observe des `Flow<List<T>>`, pas d'état local dupliqué sauf transitions purement visuelles (ex : `movingToCompletedIds`)
- **Soft delete systématique** — `deletedAt: LocalDateTime?` sur les tâches ; toutes les queries filtrent `deletedAt IS NULL`
- **Repository pattern strict** — les ViewModels n'accèdent jamais aux DAOs directement
- **Injection manuelle** — `AppContainer` instancie et câble tous les repositories ; les ViewModels le récupèrent via `(app as OnlyFlowsApp).container`

---

## Roadmap

| Priorité | Feature | Complexité |
|---|---|---|
| ✅ Done | About screen | Facile |
| ✅ Done | Paramètres généraux (24 h/12 h, URLs) | Facile–Moyen |
| ✅ Done | Démarrer le minuteur depuis multi-sélection | Moyen |
| 🔜 Next | Total temps suivi par projet (réactif) | Moyen |
| 🔜 Next | Alerte journalière — heures fixes + jours Mo–Di | Moyen–Difficile |
| 🔜 Next | Logbook étendu — journal de toutes les actions | Difficile |
| 🔜 Next | Rappels géolocalisés — GPS offline, lieux nommés | Très difficile |
| 🗓 Tablette | Master-detail ProjectDetail, sidebar permanente, panel Task latéral | — |
| 🗓 Long terme | Auth, sync backend, export JSON/CSV, widget Android | — |
