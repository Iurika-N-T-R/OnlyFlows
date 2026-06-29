# APP BRIEF — OnlyFlows
> Dernière mise à jour : 2026-06-06 · Version 0.55.3 (build 78)

---

## 1. Présentation générale

- **Nom :** OnlyFlows
- **Description :** Gestionnaire de tâches et de projets natif Android — minimal, éditorial, local-first.
- **Objectif principal :** Permettre à l'utilisateur de capturer, organiser et suivre ses tâches et projets sans dépendre d'un cloud tiers, avec une UI épurée inspirée de Things 3.
- **Problème résolu :** Les apps de productivité existantes sont soit trop lourdes (Notion, ClickUp), soit cloud-only (Todoist, Things 3 iOS-only), soit sans sync peer-to-peer. OnlyFlows est 100 % local, synchronisable via Syncthing entre appareils, avec un niveau de détail par tâche (récurrence, checklist, time tracker, pièces jointes, dépendances) que les apps minimalistes n'offrent pas.

---

## 2. Utilisateurs

- **Qui :** Usage personnel, un seul utilisateur (l'auteure). Pas d'équipe, pas de comptes multiples.
- **Nombre d'utilisateurs :** 1 (propriétaire + développeuse = même personne)
- **Niveau technique :** Expert — l'utilisatrice est la développeuse du projet. Elle connaît l'architecture dans ses moindres détails.
- **Cas d'usage principaux :**
  - Capturer rapidement une tâche (AddTaskSheet) sans quitter le contexte
  - Planifier la journée dans la vue Today (groupes Overdue / Today / This Evening)
  - Suivre l'avancement de projets complexes (sections, checklist, dépendances, time tracking)
  - Synchroniser silencieusement entre deux appareils Android via Syncthing (sans compte, sans cloud)
  - Suivre des objectifs personnels (Goals) avec des entrées de log

---

## 3. Fonctionnalités actuelles

### Tâches
- Création rapide via `AddTaskSheet` (titre, priorité, projet/section, date, checklist inline)
- Détail complet : note markdown, checklist, date de début, deadline + heure, créneau de fin, priorité, tags, récurrence, dépendances, rappels, pièces jointes, commentaires, time tracker
- Complétion par swipe (droite) ou checkbox ; auto-complétion à 100 % checklist (délai 800 ms)
- Récurrence complète : HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS, base COMPLETION_DATE / DUE_DATE / SPECIFIC_DATE, option skip weekends
- Dépendances inter-tâches (`dependsOnTaskId`) avec badge cadenas et section "Blocked by"
- Conversion Tâche → Projet (les items de checklist deviennent des tâches du projet)
- Soft delete (Corbeille) avec restauration individuelle ou vidage complet
- Multi-sélection (appui long) : Déplacer, Supprimer, Plus (Star / Priorité / Tag / Planifier / Démarrer timer)
- Réorganisation par drag & drop (bibliothèque `sh.calvin.reorderable`)

### Vues principales
| Vue | Comportement |
|---|---|
| **Today** | Groupes Overdue / Today / This Evening, tri intelligent, badge complétées/total, filtres tags dynamiques |
| **Upcoming** | Scroll infini −14j → +365j, mode semaine/mois, drag-to-reschedule |
| **Inbox** | Tâches non traitées (sans projet et sans groupe planifié) |
| **Anytime** | Tâches projet sans date + avec priorité, ou déplacées explicitement |
| **Someday** | Tâches projet sans date sans priorité, ou déplacées explicitement |
| **Projects** | Kanban 4 colonnes (NOT_STARTED / IN_PROGRESS / WAITING / DONE) + vue liste |
| **Project Detail** | Sections, drag-to-reorder, filtres tag/priorité, pie chart, temps total, notes, commentaires, sous-listes |
| **Task Detail** | Écran édition complet (toutes les propriétés) |
| **Logbook** | Projets archivés |
| **Trash** | Tâches soft-deleted avec restauration |
| **QuickFind** | Recherche temps réel + parsing de dates naturelles FR/EN |

### Projets
- Types : PROJECT, LIST, NOTE
- Statuts : NOT_STARTED / IN_PROGRESS / WAITING / DONE (auto-DONE à 100 %)
- Archive / Désarchive (appui long dans la sidebar)
- Sections avec drag-to-reorder, deadline avec compte à rebours coloré
- Temps total suivi (somme de toutes les TimeEntries des tâches)
- Historique des changements de statut (`StatusLogEntry`)
- Notes éditables inline, timeline de commentaires, sous-projets (child lists)
- Emoji personnalisé comme icône

### Goals (Objectifs)
- Créer des objectifs avec valeur cible, unité, deadline
- Suivre la progression via des entrées de log (GoalLog)
- Lier des tâches à des objectifs
- Carte de progression dans l'UI
- Filtrer le journal d'actions par tag d'objectif
- Synchronisés via Syncthing

### Rappels & Notifications
- Rappels multiples par tâche (AlarmManager exact, exemption Doze)
- Canaux son/silencieux séparés, bypass DND, vibration configurable
- Replanification automatique au reboot (BootReceiver)
- Alerte quotidienne à heure fixe (jours configurables lun–dim)
- Alertes géofence (GPS offline, positions nommées sauvegardées)

### Time Tracking
- Démarrer / Pause / Arrêter par tâche dans TaskDetail
- Ticker temps réel (mise à jour toutes les secondes)
- Démarrage rapide depuis la multi-sélection (Appui long → Plus → "Start Timer")
- Total affiché dans ProjectDetail (somme des entrées complétées)

### Sync Syncthing
- Sync peer-to-peer via Syncthing (pas de cloud, pas de compte)
- Import automatique sur changement de fichier (FileObserver, temps réel)
- Scan périodique en arrière-plan toutes les 15 min via WorkManager (fallback)
- Résolution de conflits Last-Write-Wins sur le champ `updatedAt`
- Propagation des suppressions par tombstones (`_meta/tombstones.json`)
- Export initial lors de la première configuration du dossier
- Bouton "Force sync" dans les paramètres
- Indicateur de statut : FolderNotSet / WaitingToSync / Importing / Ready / Error

### Pièces jointes
- Ajout de fichiers aux tâches (SAF, URI persistable)
- Copie physique dans le dossier Syncthing pour propagation
- JSON de métadonnées par pièce jointe

### Widget écran d'accueil (Glance)
- Widget redimensionnable (Glance 2×2 et plus)
- Sélecteur de portée : All / Projet / Section
- Filtre de tri : Date / Priority / Tag / None
- Tap sur tâche → complétion (récurrence-safe)
- Thème automatique clair/sombre, refresh sans latence via Room Flow

### DateTracker
- Suivi de dates anniversaires / comptes à rebours personnalisés
- Synchronisé via Syncthing

### Lecteur RSS local
- Lecture locale sans WebView, sans polling réseau
- Parseur Atom + RSS
- Entités `RssFeedEntity` + `RssArticleEntity`

### QuoteCard
- Carte de citation affichée dans Today, Projects, ProjectDetail, Logbook
- Citations chargées depuis un fichier asset local (pas de réseau)
- Nouvelle citation à chaque lancement

### Paramètres
| Écran | Contenu |
|---|---|
| Général | Format 24h/12h, URLs cliquables dans les notes, taille du texte, visibilité sections sidebar |
| Apparence | Thème Sombre / Clair / Système |
| Notifications | Son, vibration, bypass DND |
| Calendrier | Intégration calendrier Android local |
| Backup | Export / import JSON |
| Syncthing | Sélecteur de dossier, statut sync, force sync |
| Tags | Gestionnaire de tags (créer, renommer, recolorer, supprimer, purger inutilisés) |
| Statistiques | Stats de complétion des tâches |
| Localisations | Positions sauvegardées pour géofencing |
| Logs | Visionneuse de logs in-app |
| About | Version, stack, licences |

### Tablette
- Sidebar permanente sur grands écrans (`WindowWidthSizeClass.EXPANDED`)
- Raccourcis clavier hardware
- Layout adaptatif (`AdaptiveAppLayout`)

---

## 4. Workflow utilisateur

1. **Capture** — Appuyer sur le FAB → `AddTaskSheet` : titre + projet/section + priorité + date + checklist. Tap "OpenInFull" pour ouvrir le détail immédiatement.
2. **Planification quotidienne** — Ouvrir la vue **Today** : les tâches Overdue remontent automatiquement, les autres sont groupées Today / This Evening. Utiliser les filtres tags pour isoler un contexte.
3. **Travail sur une tâche** — Ouvrir le détail : cocher les items de checklist, démarrer le timer, ouvrir les pièces jointes, lire les commentaires.
4. **Gestion de projets** — Vue **Projects** (Kanban) pour visualiser l'avancement. Vue **Project Detail** pour gérer les sections, voir le pie chart, lire/écrire des notes.
5. **Planification hebdomadaire** — Vue **Upcoming** : glisser-déposer une tâche vers une autre date pour la replanifier.
6. **Archivage** — À la fin d'un projet : appui long dans la sidebar → Archive. Les tâches complétées restent consultables dans **Logbook**.
7. **Suppression** — Supprimer envoie en **Trash** (soft delete). Restaurer ou vider définitivement.
8. **Sync** — Se produit automatiquement en arrière-plan via FileObserver/WorkManager. Forcer depuis Paramètres > Syncthing si besoin.

---

## 5. Interface

- **Nombre d'écrans :** ~25 (13 écrans principaux + 12 écrans de paramètres/détail)
- **Description des écrans principaux :**
  - `TodayScreen` — liste avec groupes Overdue/Today/Evening, badge, filtres chips
  - `InboxScreen` — liste simple avec swipe, multi-sélection
  - `UpcomingScreen` — défilement daté avec drag-to-reschedule
  - `AnytimeScreen` / `SomedayScreen` — listes filtrées par routing logique
  - `ProjectsScreen` — Kanban 4 colonnes ou liste, drag entre colonnes
  - `ProjectDetailScreen` — header coloré + emoji, sections collapsibles, stats, notes, commentaires
  - `TaskDetailScreen` — panneau de propriétés scrollable (12 sections)
  - `LogbookScreen` — projets archivés, QuoteCard
  - `TrashScreen` — tâches supprimées avec date de suppression, Restore / Delete Forever
  - `QuickFindScreen` — overlay de recherche avec parsing de dates naturelles
  - `GoalsScreen` / `GoalDetailScreen` — objectifs avec log d'entrées
  - `DateTrackerScreen` — dates anniversaires / comptes à rebours
  - `RssScreen` / `RssArticleScreen` — lecteur RSS local

- **Captures d'écran :** Non disponibles dans le repo.
- **Points forts UI :**
  - Thème Material 3 sombre/clair cohérent
  - Swipe-to-complete avec animation et snackbar undo
  - Transitions et animations soignées (chevrons animés, délai auto-complétion checklist)
  - Sidebar adaptative phone/tablette
  - Widget Glance avec thème système automatique
- **Problèmes UI connus :**
  - `doc.md` liste quelques notes de MetaRow (trailing + value non mutuellement exclusifs, source de confusion passée)
  - Icons Material non-AutoMirrored (warnings à la compile, cosmétiques, sans impact)

---

## 6. Architecture technique

- **Langage :** Kotlin 2.1.0
- **Framework UI :** Jetpack Compose + Material Design 3
- **Base de données :** Room v2.6.1 (SQLite local), version 30, 21 entités, `fallbackToDestructiveMigration`
- **State management :** ViewModel + StateFlow (UDF strict — immutable UiState + sealed Event)
- **Navigation :** Navigation Compose (type-safe routes, sealed class `Screen`)
- **Async :** Kotlin Coroutines + Flow
- **DI :** Manual — `AppContainer` singleton (pas de Hilt/Koin)
- **Préférences :** DataStore Preferences (thème, sidebar, notifications, Syncthing, backup, calendrier…)
- **Sync :** Syncthing via SAF + FileObserver + WorkManager (pas de cloud, pas de serveur)
- **Notifications :** AlarmManager exact alarms + BroadcastReceiver + BootReceiver
- **Background :** WorkManager (backup, import Syncthing fallback)
- **Géofencing :** Android GeofenceManager + GeofenceReceiver
- **Widget :** Jetpack Glance (Glance AppWidget)
- **Drag & Drop :** `sh.calvin.reorderable`
- **Intégrations Android :** CalendarProvider, FileProvider, SAF (Storage Access Framework), OpenableColumns
- **API externes :** Aucune. 100 % local.
- **Hébergement :** Aucun. APK distribué via GitHub Releases + copie dans `K:\🔁 Sync\Projets\Codes\OnlyFlows\Released\`
- **Authentification :** Aucune. App mono-utilisateur locale.
- **minSdk :** 26 (Android 8.0) — **targetSdk :** 35
- **Build :** Gradle Kotlin DSL, signature via variables d'environnement système (`KEYSTORE_PATH`, `KEY_ALIAS`, etc.)
- **Licence :** Propriétaire

---

## 7. Structure des données (entités principales)

### Task
```
id, title, note, isCompleted, isArchived, projectId?, sectionId?,
dueDate?, dueTime?, endTime?, scheduledGroup?, hasReminders, hasAttachments,
isStarred, tagIds: List<String>, priority (NONE/LOW/MEDIUM/HIGH),
startDate?, recurrence: RecurrenceRule?, sortOrder,
hasNote, hasChecklist, recurrenceDoneCount, recurrenceAbandonedCount,
checklistItems: List<ChecklistItem>, dependsOnTaskId?,
createdAt, updatedAt, deletedAt?  ← null = actif, non-null = Corbeille
```

### Project
```
id, title, note, colorHex, iconEmoji, isCompleted, folderId?,
parentProjectId?, type (PROJECT/LIST/NOTE),
sections: List<Section>, sortOrder, dueDate?,
status (NOT_STARTED/IN_PROGRESS/WAITING/DONE),
isArchived, createdAt, updatedAt
```

### Autres entités
| Entité | Champs clés |
|---|---|
| Section | id, projectId, title, sortOrder |
| Tag | id, name, colorHex |
| Folder | id, title, sortOrder |
| ChecklistItem | id, taskId, title, isCompleted, sortOrder, dueDate?, startDate? |
| Reminder | id, taskId, scheduledAt: LocalDateTime |
| Attachment | id, taskId, fileName, mimeType, uri (content URI persistable), fileSize, createdAt |
| TimeEntry | id, taskId, startedAt, endedAt? (null = en cours) |
| Comment | id, taskId?, projectId?, content, createdAt, updatedAt |
| RecurrenceRule | type, interval, daysOfWeek, basis, skipWeekends |
| Goal | id, title, targetValue, unit, deadline?, isCompleted, createdAt, updatedAt |
| GoalLog | id, goalId, value, note, createdAt, isStarred |
| DateTracker | id, title, date, type (ANNIVERSARY/COUNTDOWN), createdAt |
| RssFeed | id, url, title, lastFetched |
| RssArticle | id, feedId, title, content, publishedAt, isRead |
| ActionLog | id, entityType, entityId, actionType, createdAt |
| SavedLocation | id, name, latitude, longitude, radius |
| SyncMeta | (métadonnées de synchronisation Syncthing) |
| DeletedEntity | id, entityType, entityId, deletedAt (tombstones pour sync) |

**Relations :** Task → Project (FK optionnel), Task → Section (FK optionnel), ChecklistItem → Task, Reminder → Task, Attachment → Task, TimeEntry → Task, Comment → Task ou Project, GoalLog → Goal, RssArticle → RssFeed.

---

## 8. Règles métier

- **Inbox** : tâche sans projet ET sans scheduledGroup ET non complétée ET non archivée ET non supprimée
- **Today** : scheduledGroup IN ('TODAY','THIS_EVENING') ET non archivée ET non supprimée
- **Upcoming** : dueDate non null ET non complétée ET non archivée ET non supprimée
- **Anytime (auto)** : projectId non null ET priority ≠ NONE ET scheduledGroup null ET dueDate null ET active
- **Someday (auto)** : projectId non null ET priority = NONE ET scheduledGroup null ET dueDate null ET active
- **Soft delete obligatoire** : toute suppression user appelle `softDelete()` (sets `deletedAt`). Hard delete uniquement pour : ConvertToProject (supprime la tâche originale) et EmptyTrash.
- **DB version** : toujours bumper `AppDatabase.version` (+1) quand une colonne est ajoutée à une entité
- **Version applicative** : toujours bumper `versionName` + `versionCode` (+1) avant un build release
- **Récurrence** : `completeRecurring()` crée la prochaine occurrence ET archive l'occurrence actuelle. Ne jamais appeler `setCompleted()` sur une tâche récurrente.
- **Projet DONE auto** : quand toutes les tâches sont complétées, le statut passe automatiquement à DONE
- **Section supprimée** : les tâches de la section ont leur `sectionId` mis à NULL (elles ne sont pas supprimées)
- **Syncthing write** : chaque méthode d'écriture dans un RepositoryImpl doit appeler `syncthingWriter` après l'appel DAO
- **AppContainer ordre** : Syncthing components déclarés en premier (SyncthingWriter, SyncthingCoordinator…), puis les repositories qui les reçoivent en paramètre
- **snapshotFlow** : pour observer du state Compose dans un `LaunchedEffect`, toujours utiliser `snapshotFlow { someState }` — jamais `LaunchedEffect(someState)`

---

## 9. Points faibles actuels

- **`doc.md` obsolète** : documente DB v20 alors qu'on est à v30 — ne pas utiliser comme référence pour les champs d'entités.
- **Pas de tests** : aucun test unitaire ni instrumenté. La correction d'un bug se valide uniquement par build + test manuel.
- **`fallbackToDestructiveMigration`** : chaque bump de version DB en dev efface les données locales. Intentionnel pour l'instant, mais empêche tout schéma de migration sûr pour les utilisateurs externes.
- **DI manuelle** : `AppContainer` grossit à chaque nouvelle feature — potentiellement difficile à maintenir à très grande échelle, mais acceptable pour un projet mono-dev.
- **Pas d'état d'erreur structuré** : les erreurs dans les ViewModels ne sont pas toujours exposées à l'UI de façon cohérente.
- **Warnings compile** : icônes Material non-AutoMirrored (cosmétiques, ignorables).

---

## 10. Concurrence

| App | Ce qu'elle fait mieux | Ce qu'elle fait moins bien |
|---|---|---|
| **Things 3** | UX exceptionnelle, polish, iOS-first | iOS uniquement, pas de sync Syncthing, pas de time tracking |
| **Todoist** | Collaboration, NLP de saisie, multi-plateforme | Cloud obligatoire, abonnement payant, moins de détail par tâche |
| **TickTick** | Time tracking intégré, Pomodoro, calendrier | Cloud obligatoire, UI chargée |
| **Obsidian Tasks** | Intégration markdown/PKM | Pas une vraie app de tâches, pas mobile-first |
| **Orgzly** | Local-first, Syncthing-compatible | UI datée, courbe d'apprentissage org-mode |

**Avantage clé d'OnlyFlows** : combinaison unique de local-first + Syncthing peer-to-peer + niveau de détail Things 3 + widget + goals sur Android natif.

---

## 11. Vision V2 / Roadmap

*Basé sur `Todo.md` et CHANGELOG récent*

- **Améliorer le widget** : interactions plus riches, support de plus de tailles
- **IA locale** : suggestions de planification, détection de patterns (hors scope actuel, pas de réseau)
- **Améliorer Syncthing** : meilleure gestion des conflits, indicateur de santé de sync plus clair
- **Statistiques avancées** : temps par projet, vélocité, tendances
- **Notes enrichies** : meilleur support markdown, images inline
- **Raccourcis clavier tablette** : couverture plus complète
- **Export** : formats additionnels (CSV, Markdown, iCal)

---

## 12. Priorités

| Critère | Score /5 |
|---|---|
| Performance | ★★★★☆ — Room + Flow déjà réactif, widget zero-latency |
| Simplicité | ★★★★★ — UI minimaliste, priorité absolue |
| Automatisation | ★★★☆☆ — récurrence, rappels, sync auto ; IA non prioritaire |
| Design | ★★★★★ — inspiré Things 3, Material 3, dark/light soigné |
| IA | ★☆☆☆☆ — hors scope (local-first, pas de réseau) |
| Collaboration | ★☆☆☆☆ — mono-utilisateur, pas prévu |
| Mobile | ★★★★★ — Android-first, widget, tablette supportée |
| Desktop | ★☆☆☆☆ — Android uniquement |

---

## 13. Ressources disponibles

- **Code source complet** : `k:\[F] ARCHIVES\PROGRAMMATION\Android\OnlyFlows\` (repo Git)
- **CLAUDE.md** : guide architecture pour les agents IA
- **doc.md** : référence détaillée des DAOs et modèles (⚠️ partiellement obsolète, DB v20)
- **CHANGELOG.md** : historique complet depuis v0.18.0
- **README.md** : feature list + stack + architecture overview
- **APK releases** : `K:\🔁 Sync\Projets\Codes\OnlyFlows\Released\` + GitHub Releases
- **Maquettes** : aucune (l'UI est définie directement dans le code)
- **Base de données** : SQLite locale sur l'appareil, export JSON via BackupManager

---

## 14. Contraintes

- **Budget :** Zéro — projet personnel, pas de dépenses externes
- **Temps :** Pas de deadline fixe — développement au fil de l'eau
- **Limites techniques :**
  - Android uniquement (pas de iOS, pas de web)
  - Local-first strict — aucun appel réseau permis sauf RSS fetch
  - `fallbackToDestructiveMigration` : pas de migrations de données complexes pour l'instant
  - Pas de compte utilisateur, pas de backend
- **Compatibilité requise :** Android 8.0+ (minSdk 26), téléphones et tablettes Android

---

## 15. Ce que vous attendez de l'agent

- **Développeur Android principal** — implémenter des features, corriger des bugs, optimiser
- **Règles absolues à respecter :**
  1. Toujours builder (`assembleDebug`) avant de déclarer une tâche terminée
  2. Bumper `versionName` + `versionCode` avant chaque release build
  3. Ne jamais utiliser `taskRepo.delete()` depuis une action user — toujours `softDelete()`
  4. Bumper `AppDatabase.version` (+1) à chaque ajout de colonne
  5. Ne jamais ajouter de Co-Authored-By dans les commits
  6. Flow release : build release → commit → push → tag → gh release → copier APK dans Released
