# Changelog — OnlyFlows

Tous les changements notables du projet sont documentés ici.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [Unreleased] — en cours

*(aucun changement en attente)*

---

## [0.19.0] — 2026-04-15 · Temps total projet — réactif + ticker live

### Ajouté

- **`TimeEntryDao.observeByProject()`** — variante Flow de `getByProject()` ; Room émet automatiquement à chaque insert/update/delete dans `time_entries` pour les tâches du projet.
- **`TimeEntryRepository.observeByProject()`** — interface + implémentation (`map { toDomain() }`).

### Modifié

- **`ProjectDetailViewModel`** :
  - Remplace le chargement one-shot `getTotalSecondsForProject()` par une collecte réactive de `observeByProject()` : le total se met à jour immédiatement lorsqu'un chronomètre démarre ou s'arrête depuis n'importe quel écran.
  - Ajoute un **ticker 1 s** en `init {}` (même pattern que `TaskDetailViewModel`) : `totalTrackedSeconds = totalCompletedSeconds + elapsed` tant qu'une entrée active existe.
  - `totalCompletedSeconds` et `activeProjectEntry` préservés dans le `combine` collect pour ne pas être écrasés par les mises à jour du graphe de tâches.
- **`ProjectDetailUiState`** : nouveaux champs `totalCompletedSeconds: Long` et `activeProjectEntry: TimeEntry?`.

---

## [0.18.0] — 2026-04-15 · Démarrer le minuteur depuis la sélection multiple

### Ajouté

- **"Démarrer le minuteur" dans `BulkMoreSheet`** — option visible uniquement lorsqu'une seule tâche est sélectionnée (garde `selectedCount == 1` déjà en place dans le composant). Stoppe tout `TimeEntry` actif pour la tâche, crée un nouvel entry, puis quitte le mode sélection.
- **`BulkMoreSheet` câblé dans tous les écrans de tâches** :
  - **Today** — `onTrackTask` désormais transmis (était silencieusement ignoré, callback null).
  - **Inbox** — même correction, `onTrackTask` branché sur `StartTimerForSelected`.
  - **Anytime** — intégration complète : `showBulkMore: Boolean` dans l'état, événements `ShowBulkMore / HideBulkMore / BulkStar / BulkSetPriority / BulkAddTag / BulkSchedule / StartTimerForSelected`, `BulkMoreSheet` rendu, `onMore` câblé dans `SelectionBar`.
  - **Someday** — même intégration qu'Anytime.
  - **ProjectDetail** — `onMore = { /* TODO */ }` remplacé, `showBulkMore` ajouté à l'état, tous les handlers câblés ; utilise le `timeRepo` déjà présent dans le ViewModel.

### Modifié

- Label "Track task — Start timer" renommé en **"Démarrer le minuteur"** (cohérence française avec le reste de l'UI).
- `TimeEntryRepository` injecté dans `TodayViewModel`, `InboxViewModel`, `AnytimeViewModel`, `SomedayViewModel` (déjà présent dans `ProjectDetailViewModel`).

---

## [0.17.0] — 2026-04-15 · Écran À propos · Paramètres généraux

### Ajouté

- **`AboutAppScreen`** — écran accessible depuis `Paramètres → À propos` (`settings/about`) : version de l'app, description, stack technique, mentions légales / licences open-source.
- **`GeneralSettingsScreen`** — écran `settings/general` avec deux réglages persistés en DataStore :
  - **Format d'heure 24 h / 12 h** — bascule AM/PM ; appliqué à tous les affichages d'heure dans l'app (rappels, créneau horaire, TimerRow…).
  - **URLs cliquables dans les notes** — active ou désactive la détection et le rendu des liens hypertexte dans les champs `note` de tâche et de projet.
- Route `Screen.About` et `Screen.GeneralSettings` câblées dans `AppNavigation`.
- Entrées correspondantes dans `SettingsScreen`.

---

## [0.16.0] — 2026-04-14 · Bugfixes — rendu tâches + delete soft + indicateur note

### Corrigé

- **`observeToday()` — tâches invisibles avec deadline passée** — La query SQL incluait `AND (dueDate IS NULL OR dueDate >= :today)` qui excluait silencieusement toute tâche ayant `scheduledGroup = TODAY/THIS_EVENING` **et** une deadline antérieure à aujourd'hui. Ces tâches disparaissaient de la vue Today sans atterrir nulle part si elles étaient aussi complétées. Condition supprimée : le `scheduledGroup` est le seul critère de routing Today.
  - Fichier : `data/local/dao/TaskDao.kt` — `observeToday()`
- **Multi-sélection delete (Today / Someday / Anytime) — suppression définitive au lieu de Trash** — Les trois ViewModels appelaient `taskRepo.deleteAll()` (hard delete) au lieu de `taskRepo.softDelete()`. Les tâches supprimées via la `SelectionBar` étaient effacées de la base de données sans passer par la Corbeille, rendant tout restore impossible. Corrigé par `ids.forEach { taskRepo.softDelete(it) }` — cohérent avec Inbox, ProjectDetail et les swipe-to-delete des mêmes écrans.
  - Fichiers : `ui/screens/today/TodayViewModel.kt`, `ui/screens/someday/SomedayViewModel.kt`, `ui/screens/anytime/AnytimeViewModel.kt`
- **`hasNote` jamais mis à jour — indicateur note toujours absent** — `TaskEntity.toDomain()` copiait `hasNote = hasNote` depuis la valeur stockée en base, sans jamais la recalculer. Saisir ou effacer une note ne mettait pas à jour le flag, donc l'icône note sur `TaskItem` ne s'affichait jamais. Corrigé en calculant `hasNote = note.isNotBlank()` à chaque lecture.
  - Fichier : `data/local/entity/TaskEntity.kt` — `toDomain()`

---

## [0.15.0] — 2026-04-10 · Pièces jointes

### Ajouté

- **Pièces jointes** — section "Pièces jointes" dans `TaskDetailScreen` entre les rappels et le chronomètre :
  - Sélecteur système (`ActivityResultContracts.OpenDocument`) → permission persistante via `takePersistableUriPermission`.
  - Métadonnées extraites via `ContentResolver` (nom, taille, type MIME).
  - Affichage par ligne : icône contextuelle selon le type (image, vidéo, audio, PDF, archive, texte), nom, taille formatée.
  - Tap sur un fichier → `Intent.ACTION_VIEW` avec `FLAG_GRANT_READ_URI_PERMISSION`.
  - Bouton ✕ par pièce jointe + bouton "Ajouter une pièce jointe" quand la liste n'est pas vide.
  - Indicateur 📎 sur `TaskItem` quand `task.hasAttachments = true`.
- **`AttachmentEntity`** — entité Room `attachments` (`id`, `taskId`, `fileName`, `mimeType`, `uri`, `fileSize`, `createdAt`).
- **`AttachmentDao`** — `observeByTask`, `insert`, `delete`, `deleteByTask`, `countByTask`.
- **`AttachmentRepository`** + **`AttachmentRepositoryImpl`** — couche domaine + implémentation.
- **`Attachment`** — modèle domaine dans `Models.kt`.
- `Task.hasAttachments: Boolean` — flag persisté en DB, mis à jour à l'ajout/suppression.
- `TaskDetailEvent.AddAttachment`, `TaskDetailEvent.DeleteAttachment` — nouveaux événements.
- `TaskDetailUiState.attachments: List<Attachment>` + `TaskDetailViewModel.loadAttachments(taskId)`.
- `TaskDetailEvent.Delete` — supprime aussi les pièces jointes via `attachmentRepo.deleteByTask`.

### Modifié

- `AppDatabase` — version 18 → **19** (`AttachmentEntity` + `attachmentDao()`).
- `TaskEntity` — champ `hasAttachments: Boolean = false` ajouté dans `toDomain()` / `toEntity()`.
- `AppContainer` — `attachmentRepository: AttachmentRepository` ajouté.
- `loadTask()` dans `TaskDetailViewModel` — `attachments` préservé dans le `collect` (même pattern que `comments`/`reminders`).

---

## [0.14.0] — 2026-04-10 · Icône adaptative V3 · Sons & notifications

### Ajouté

- **Icône adaptative V3** — refonte complète de `ic_launcher_foreground.xml` :
  - Fond blanc (`#FCFDFD`) couvrant tout le viewport 155×155, logo découpé en `fillType="evenOdd"` (fond gradient bleu visible à travers).
  - Courbe S bleue (`#3D8EF0`), accent droit (`#254572`), zone de remplissage (`#3272E3`), arc bas (`#254572`).
  - `ic_launcher_background.xml` — dégradé bleu linéaire `#3D8EF0 → #2669D5` (inchangé depuis V1).
  - `ic_launcher_monochrome.xml` — chemins blanc pour icônes thématiques Android 13.
- **`NotificationSettingsScreen`** — nouvel écran de paramètres notification (`settings/notifications`) :
  - Bandeau rouge tappable si les notifications sont désactivées système → ouvre `ACTION_APP_NOTIFICATION_SETTINGS`.
  - Toggle **Son** (icône VolumeUp) — sélectionne entre canal `onlyflows_reminders` (son système) et `onlyflows_reminders_silent`.
  - Toggle **Vibration** — `longArrayOf(0, 250, 100, 250)` ou `longArrayOf(0)`.
  - Toggle **Contourner DND** — ajoute `CATEGORY_ALARM` sur la notification.
  - Lien vers les paramètres notification système.
- **`NotificationPreferences`** — `DataStore` avec `soundEnabled`, `vibrationEnabled`, `bypassDnd` (défauts : true / true / false). Méthode `snapshot()` pour lecture synchrone hors Flow.
- **Deux canaux de notification** — `onlyflows_reminders` (IMPORTANCE_HIGH, son + vibration) et `onlyflows_reminders_silent` (IMPORTANCE_LOW). Sélection au moment de la notification selon la préférence son.
- Route `Screen.NotificationSettings` câblée dans `AppNavigation`.
- Entrée "Sons & notifications" dans `SettingsScreen` (icône `NotificationsNone`).

### Modifié

- `ReminderReceiver` — lit les préférences via `notifPrefs.snapshot()` dans un bloc `goAsync()` + coroutine. Applique son/vibration/DND avant de poster la notification.
- `ReminderReceiver.ensureChannels(context)` — remplace `createNotificationChannel()` dans `OnlyFlowsApp`.
- `BootReceiver` — utilise `reminderRepo.getFutureReminders()` (table `reminders`) au lieu du champ `reminderAt` sur la tâche.
- `AndroidManifest.xml` — permissions `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + `WAKE_LOCK` ajoutées.
- `MainActivity` — `requestBatteryOptimizationExemption()` remplace `requestExactAlarmsIfNeeded()` (plus agressive).

---

## [0.13.0] — 2026-04-10 · Reminders multiples · AlarmManager fix · Notification pipeline

### Ajouté

- **Reminders multiples** — une tâche peut avoir N rappels (remplacement du champ `reminderAt: LocalDateTime?` par une table dédiée) :
  - `ReminderEntity` / `ReminderDao` / `ReminderRepository` / `ReminderRepositoryImpl` — couche complète.
  - `RemindersMetaSection` dans `TaskDetailScreen` — liste de rappels (format `EEE, d MMM · HH:mm`), bouton ✕ par rappel, bouton "Add reminder" en bas, état vide = MetaRow tappable.
  - `TaskDetailEvent.AddReminder(at)` + `TaskDetailEvent.DeleteReminder(id)` — remplacent `SetReminder`.
  - `Task.hasReminders: Boolean` — flag maintenu à jour (true si ≥ 1 rappel, false après suppression du dernier).
  - Indicateur 🔔 sur `TaskItem` quand `task.hasReminders = true`.
- **`ReminderPickerSheet`** — shortcuts "Dans 1 minute (test)", "Ce soir · 18:00" + date+heure custom. Bannière d'avertissement sur API 31+ sans permission `SCHEDULE_EXACT_ALARM`.
- **`DatePickerModal` — bouton "Add Reminder" fonctionnel** — callback `onAddReminder` : sauvegarde d'abord la date puis ouvre le `ReminderPickerSheet`.
- **Exemption battery optimization** — `requestBatteryOptimizationExemption()` dans `MainActivity` : invite l'utilisateur à exempter l'app d'optimisation batterie (Android Doze).

### Modifié

- **`ReminderScheduler`** — logique AlarmManager corrigée :
  - API ≥ 31 : `canScheduleExactAlarms()` → `setExactAndAllowWhileIdle` ou fallback `setAndAllowWhileIdle`.
  - API < 31 : `setExactAndAllowWhileIdle` directement (permission non requise).
  - Try/catch `SecurityException` avec fallback.
  - Logs `Log.d/w/e` à chaque étape.
- **`ReminderReceiver.completeTask`** — coroutine protégée par `goAsync()` + `pendingResult.finish()` en `finally`.
- **`TaskDetailViewModel.loadTask()`** — `_uiState.update { current -> newState.copy(comments = current.comments, reminders = current.reminders) }` pour ne pas écraser les listes chargées séparément.
- `AppDatabase` — version 16 → 17 (table `reminders`) → 18 (champ `hasReminders` sur `tasks`).
- `Task` — `reminderAt: LocalDateTime?` supprimé, `hasReminders: Boolean` ajouté.
- `MockData`, `TodayViewModel` — références à `reminderAt` nettoyées.

---

## [0.12.0] — 2026-04-03 · Convert task → Project · AddTask priority picker

### Ajouté

- **Convert Task → Project** — menu `⋯` dans la `TopBar` de `TaskDetailScreen` → "Convert to Project" :
  - Crée un `Project` avec le titre, la note et la `dueDate` de la tâche source.
  - Convertit chaque item de la checklist en tâche dans le nouveau projet (avec `isCompleted` conservé).
  - Supprime la tâche originale.
  - Navigue automatiquement vers le `ProjectDetailScreen` du projet créé.
- **AddTask — priority picker** — le bouton flag (🔴/🟠/🔵/⬜) dans `AddTaskSheet` ouvre un `DropdownMenu` pour choisir la priorité avant d'envoyer la tâche. La priorité sélectionnée est transmise aux événements `AddTask` de Inbox, Today et ProjectDetail.
- `TaskDetailEvent.ConvertToProject` — nouvel événement dans `TaskDetailViewModel`.
- `TaskDetailUiState.convertedProjectId: String?` — déclenche la navigation via `LaunchedEffect` dans `TaskDetailScreen`.
- `onConverted: (String) -> Unit` — nouveau callback sur `TaskDetailScreen`, câblé dans `AppNavigation`.

### Modifié

- `AddTaskSheet.onAdd` — signature étendue : `(title, group, dueDate, priority: TaskPriority)`.
- `InboxEvent.AddTask`, `TodayEvent.AddTask`, `ProjectDetailEvent.AddTask` — champ `priority: TaskPriority` ajouté.
- `InboxViewModel`, `TodayViewModel`, `ProjectDetailViewModel` — transmettent la priorité lors de l'insertion de la tâche.

---

## [0.11.0] — 2026-04-03 · Inbox multi-sélection · Upcoming drag · Anytime/Someday redesign

### Ajouté

- **Anytime redesigné** — affiche : ① les tâches **appartenant à un projet**, sans date, avec priorité (auto) ; ② toute tâche **explicitement déplacée** (`scheduledGroup = ANYTIME`). Les tâches standalone (Inbox) n'y apparaissent que via move manuel. Tri par priorité décroissante.
- **Someday redesigné** — affiche : ① les tâches **appartenant à un projet**, sans date, sans priorité (auto) ; ② toute tâche **explicitement déplacée** (`scheduledGroup = SOMEDAY`). Même règle : pas d'auto-transfer depuis l'Inbox.
- **SmartMoveSheet** (`overlays/SmartMoveSheet.kt`) — nouveau composant de déplacement unifié pour Inbox / Anytime / Someday :
  - Options rapides en haut : ✨ Anytime, 🌊 Someday, 📥 Inbox (selon contexte)
  - La vue courante est masquée (ex: depuis Anytime, Anytime n'apparaît pas)
  - Puis liste de projets avec drill-down vers les sections (identique à `MoveTaskSheet`)
- **Multi-sélection Anytime & Someday** — long-press → mode sélection, `SelectionBar` (Move / Delete / More), `BackHandler` pour désélectionner
- **Upcoming — drag-to-reschedule** — long-press sur une tâche → glisser vers une autre date. Sur le relâché, détecte le `DayHeader` le plus proche et met à jour `dueDate` via `RescheduleTask` event.
- `TaskDao.observeAnytime()` — query SQL dédiée : `projectId IS NULL AND dueDate IS NULL AND isCompleted = 0 AND (scheduledGroup = 'ANYTIME' OR (priority != 'NONE' AND scheduledGroup IS NULL))`
- `TaskDao.observeSomeday()` — query SQL dédiée : similaire pour `priority = 'NONE'`
- `TaskDao.setScheduledGroup(id, group, now)` — `UPDATE` ciblé sur `scheduledGroup`
- `TaskRepository.observeAnytime()`, `observeSomeday()`, `setScheduledGroup()` — ajoutés à l'interface et à `TaskRepositoryImpl`
- `UpcomingEvent.RescheduleTask(taskId, newDate)` — déclenche `taskRepo.update(task.copy(dueDate = newDate))`

### Modifié

- **Inbox** — query mise à jour : `WHERE projectId IS NULL AND scheduledGroup IS NULL AND isCompleted = 0`. Les tâches déjà routées vers Today/Anytime/Someday n'encombrent plus l'Inbox.
- **Badge Inbox sidebar** — count mis à jour : ne compte plus les tâches avec `scheduledGroup` défini.
- **Inbox SelectionBar** — `MoveTaskSheet` remplacé par `SmartMoveSheet` (inclut Anytime et Someday comme destinations).
- `InboxEvent.MoveSelected` — signature étendue : `(scheduledGroup: TaskGroup?, projectId: String?, sectionId: String?)`.
- `AnytimeViewModel` et `SomedayViewModel` — entièrement réécrits : utilisent les nouvelles queries `observeAnytime()`/`observeSomeday()`, observent les projets pour le MoveSheet, multi-sélection complète.
- `UpcomingScreen` — `SwipeableTaskItem` remplacé par `TaskItem` + `ReorderableItem` + `longPressDraggableHandle` pour le drag. `localItems` miroir de `state.items` pour les animations de drag visuelles.

---

## [0.10.0] — 2026-04-03 · Task interdépendante (DB + UI complet)

### Ajouté

- **`Task.dependsOnTaskId: String?`** — nouveau champ nullable en base (Room v10). Une tâche peut bloquer sur la complétion d'une autre.
- **TaskDetailScreen — "Dépend de"** — `MetaRow` tappable ouvrant `DependencyPickerSheet` : liste searchable de toutes les tâches, coche sur la sélection courante, option "Supprimer la dépendance".
- **TaskDetailScreen — "Interdépendant de"** — section read-only listant les tâches qui dépendent de la tâche courante (arrows ↳ + titre).
- **`DependencyPickerSheet`** — bottom sheet avec champ de recherche ; le titre de la tâche bloquante s'affiche en rouge dans la `MetaRow` si elle n'est pas encore complétée.
- **Badge 🔒 sur `TaskItem`** — icône cadenas affichée dans les indicateurs (collapsed mode) si `task.dependsOnTaskId != null`.
- `TaskDetailUiState` — ajout de `blockingTask: Task?`, `dependingTasks: List<Task>`, `allTasks: List<Task>`, `showDependencyPicker: Boolean`.
- `TaskDetailEvent.SetDependsOn`, `ShowDependencyPicker`, `HideDependencyPicker` — nouveaux événements.
- `TaskDao.observeAnytime()` — observé via `flatMapLatest` + checklist combine dans `loadTask()`.
- `TaskDao.observeDependents(taskId)` — renvoie les tâches dont `dependsOnTaskId = taskId`.
- `TaskDao.setDependsOn(id, dependsOnId, now)` — `UPDATE` ciblé.
- `TaskRepository.observeAll()`, `observeDependents()`, `setDependsOn()` — ajoutés à l'interface.
- `TaskRepositoryImpl.observeAll()`, `observeDependents()`, `setDependsOn()` — implémentés.
- `loadTask()` dans `TaskDetailViewModel` — étendu à un `combine` 4 sources : task, tags, dependingTasks, allTasks, avec `flatMapLatest` sur projectFlow + blockingFlow.

### Modifié

- **Room DB version** : 9 → 10 (ajout colonne `dependsOnTaskId` sur `tasks`, `fallbackToDestructiveMigration`).
- `TaskEntity` — champ `dependsOnTaskId: String? = null` ajouté dans `toDomain()` et `toEntity()`.

---

## [0.9.0] — 2026-03-30 · Upcoming scroll infini · QuickFind date FR/EN · Projects rename

### Ajouté

- **Upcoming — scroll infini** — fenêtre `today − 14j → today + 365j`, liste plate `UpcomingItem` (WeekHeader / MonthHeader / DayHeader / TaskRow / EmptyDay / DaySpacer) calculée dans `recompute()`.
- **Upcoming — mode semaine / mois** — toggle dans le menu 3-dot de la TopBar (pas d'encombrement des actions) ; checkmark sur le mode actif.
- **Upcoming — bouton Today** — `IconButton` toujours visible dans la TopBar → `listState.scrollToItem(todayItemIndex)`.
- **Upcoming — auto-scroll** — `LaunchedEffect(state.isLoading)` → scroll au chargement initial.
- **QuickFind — recherche date naturelle FR + EN** — formats supportés : `dd/MM`, `dd/MM/yyyy`, `semaine N` / `week N`, `d MMM`, `MMM d`, `d MMMM`, `MMMM d` (locales FR + EN avec normalisation des mois français).
- **`DateResultsScreen`** — écran dédié listé via `Screen.DateResults.createRoute(date)` ; affiche toutes les tâches pour une date donnée avec `SwipeableTaskItem`.
- **`DateResultsViewModel`** — charge `taskRepo.searchByDate(date)` + tags.
- `QuickFindOverlay` — `DateShortcutRow` en tête des résultats si une date est parsée (icône calendrier + "Show all tasks on this date").

### Modifié

- **"Kanban" → "Projects"** — label sidebar `AppSidebar` + titre TopBar `KanbanScreen` renommés ; route `Screen.Kanban` conservée.
- `UpcomingViewModel` — entièrement réécrit ; `allTasks` / `allTags` locaux + `recompute()` ; événements `GoToToday` (no-op VM), `ToggleViewMode`.
- `UpcomingScreen` — entièrement réécrit ; `rememberLazyListState` + `rememberCoroutineScope` ; `DropdownMenu` 3-dot.
- `QuickFindViewModel.tryParseDate()` — étendu avec regex numériques + normaliseur FR → EN.
- `AppNavigation` — ajout route `DateResults` avec `navArgument("date")`.
- `AdaptiveAppLayout` — câblage `onDateClick` → `Screen.DateResults`.

---

## [0.8.0] — 2026-03-28 · Swipe gestes · Récurrence exécution · Kanban moves · Cross-section drag

### Ajouté

- **`SwipeableTaskItem`** — composable wrappant `SwipeToDismissBox` (`@ExperimentalMaterial3Api`) :
  - Swipe droit (StartToEnd) → `onToggleComplete`, snap-back automatique.
  - Swipe gauche (EndToStart) → `AlertDialog` de confirmation avant suppression.
  - Désactivé quand `isSelectionMode = true`.
  - Utilisé dans Inbox, Today, Upcoming, Someday, Anytime, DateResults.
- **`AddTaskSheet` — bouton submit** — `IconButton` (Send) à droite de l'icône flag, activé uniquement si le titre n'est pas vide.
- **Kanban — déplacer un projet entre colonnes** — menu `MoreVert` sur chaque `KanbanCard` (board) et `ProjectListRow` (liste) → `DropdownMenu` avec les autres statuts ; event `onStatusChange` propagé jusqu'au `KanbanViewModel`.
- **ProjectDetail — cross-section drag fix** — le `LazyColumn` rend désormais `localItems` (et non `displayItems`) ; les items filtrés sont masqués inline par `return@items` ; la callback de drag n'exige plus `toItem is PdItem.TaskRow`, ce qui permet le passage par-dessus les headers.

### Modifié

- `completeRecurring` câblé dans tous les ViewModels qui gèrent `ToggleComplete` (Today, Inbox, Upcoming, Someday, Anytime, ProjectDetail, TaskDetail).
- `KanbanScreen` — ajout `onStatusChange: (String, ProjectStatus) -> Unit` jusqu'aux feuilles `KanbanCard` et `ProjectListRow`.
- `ProjectDetailScreen` — `visibleTaskIds` / `visibleSectionIds` pour le filtrage inline.

---

## [0.7.0] — 2026-03-27 · QuickFind popup flottant

### Ajouté

- **QuickFind popup flottant** — overlay `Dialog` sur l'écran courant (marges 16 dp h / 52 dp v, coins arrondis, ombre portée).
- Résultats groupés : **Projects**, **Tags** (préfixe `#`), **Tasks** (flag priorité), **On X date**.
- Recherche par date — patterns `d MMM`, `MMM d`, `d MMMM`, etc. (locale EN).
- "Show all →" par section (max 3 résultats en popup) → `QuickFindScreen` pleine page.
- Focus automatique + bouton Cancel / Clear.
- `TaskDao.searchByDate(date: String)`, `TaskRepository.searchByDate(date: LocalDate)`.

### Modifié

- `QuickFindViewModel` — `tagResults`, `dateResults`, `hasMore*`, `parsedDate` ; max 3 par section.
- `AdaptiveAppLayout` — `showQuickFind` géré localement.

---

## [0.6.0] — 2026-03-27 · Priorité, Récurrence, QuickFind, Rappels

### Ajouté

- **Priorité** — flag coloré 🔴/🟠/🔵 sur `TaskItem` + `PriorityPickerSheet` dans `TaskDetailScreen`.
- **Start Date** — `startDate: LocalDate?` sur `Task` + MetaRow + date picker.
- **Récurrence** — `RecurrenceRule` (type, interval, jours, basis, skip weekends) + `RecurrencePickerSheet` + badge `0× done / 0× abandoned`.
- **Reminders** — `reminderAt: LocalDateTime?` + `ReminderPickerSheet` (shortcuts + custom date+heure).
- **Date sur items checklist** — `dueDate: LocalDate?` sur `ChecklistItem`, badge rouge si en retard.
- **QuickFind fonctionnel** — requêtes Room réelles, debounce 200ms, sections Projects/Tasks.
- DB version : 5 → 6.

---

## [0.5.0] — 2026-03-27 · Kanban, filtres et métadonnées

### Ajouté

- **Vue Kanban** — Board (4 colonnes) + List toggle ; cartes avec pie chart, badge tâches, deadline.
- **Tags toujours visibles** dans `TaskItem`.
- **Filtres rapides ProjectDetail** — tags + priorité cumulatifs.
- **Bouton "Add Section +"** dans ProjectDetail.
- **Badge deadline inline** — `X d left / X d overdue / Y days elapsed`.
- **`StatusLogEntity`** — historique des changements de statut projet.
- DB version : 4 → 5.

---

## [0.4.0] — 2026-03-27 · Projets enrichis & Sidebar fonctionnelle

### Ajouté

- **Statut de projet** + auto-statut (DONE si 100% complété).
- **Deadline projet** avec countdown coloré.
- **`ProjectEditSheet`** — édition titre, note, couleur, deadline.
- **Créer projet / folder** depuis la sidebar.
- DB version : 3 → 4.

---

## [0.3.0] — 2026-03-27 · Interactions & Comportements

### Ajouté

- **Section "Completed"** repliable (Today + ProjectDetail).
- **Auto-complétion par checklist** (800 ms délai de transition).
- **Mode sélection multiple** — long press, cercle de sélection, `BackHandler`.
- **Drag-to-reorder** sans handle explicite (long press).
- **Tri automatique Today** — dueDate → reminder → étoilé → sortOrder.

---

## [0.2.0] — 2026-03-26 · TaskItem enrichi + Sidebar v2

### Ajouté

- **TaskItem fold/unfold**, badge checklist, indicateurs collapsed.
- **Drag-to-reorder ProjectDetail** — `sh.calvin.reorderable`.
- **Sidebar v2** — `+` par folder, `+ New Group`, persistance DataStore, badges dual.
- **`SelectionBar`** — Move / Delete / More.

---

## [0.1.0] — 2026-03-26 · Scaffold initial

### Ajouté

- Architecture data / domain / ui, UDF pattern.
- Room avec entités Task, ChecklistItem, Project, Section, Tag, Folder.
- Navigation, layout adaptatif, MockData, thème dark.
- Écrans Today, Inbox, Upcoming, ProjectDetail, TaskDetail, Sidebar v1.
