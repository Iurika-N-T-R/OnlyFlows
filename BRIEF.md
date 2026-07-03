# APP BRIEF — OnlyFlows
> Dernière mise à jour : 2026-07-03 · Version 0.68.0 (build 99)

---

## 1. Présentation générale

- **Nom :** OnlyFlows
- **Description :** Gestionnaire de tâches et de projets natif Android — minimal, éditorial, local-first.
- **Objectif principal :** Permettre à l'utilisateur de capturer, organiser et suivre ses tâches et projets sans dépendre d'un cloud tiers, avec une UI épurée inspirée de Things 3.
- **Problème résolu :** Les apps de productivité existantes sont soit trop lourdes (Notion, ClickUp), soit cloud-only (Todoist, Things 3 iOS-only), soit sans sync peer-to-peer. OnlyFlows est 100 % local, synchronisable en pair-à-pair sur le réseau local (LAN sync intégré, sans compte ni serveur), avec un niveau de détail par tâche (récurrence, checklist, time tracker, pièces jointes, dépendances, objectifs liés) que les apps minimalistes n'offrent pas.

---

## 2. Utilisateurs

- **Qui :** Usage personnel, un seul utilisateur (l'auteure). Pas d'équipe, pas de comptes multiples.
- **Nombre d'utilisateurs :** 1 (propriétaire + développeuse = même personne)
- **Niveau technique :** Expert — l'utilisatrice est la développeuse du projet. Elle connaît l'architecture dans ses moindres détails.
- **Cas d'usage principaux :**
  - Capturer rapidement une tâche (AddTaskSheet) sans quitter le contexte
  - Planifier la journée dans la vue Today (groupes Overdue / Today / This Evening)
  - Suivre l'avancement de projets complexes (sections, checklist, dépendances, time tracking)
  - Synchroniser silencieusement entre deux appareils Android via le LAN sync intégré (sans compte, sans cloud)
  - Suivre des objectifs personnels (Goals) périodiques ou ponctuels, alimentés par les tâches liées

---

## 3. Fonctionnalités actuelles

### Tâches
- Création rapide via `AddTaskSheet` (titre, priorité, projet/section, date, checklist inline)
- Détail complet : note markdown, checklist (sections + rappels par item), date de début, deadline + heure, créneau de fin, priorité, difficulté, impact, tags, récurrence, dépendances, rappels, pièces jointes, commentaires, time tracker, objectifs liés
- Complétion par swipe (droite) ou checkbox ; auto-complétion à 100 % checklist (délai 800 ms)
- Récurrence complète : HOURLY / DAILY / WEEKLY / MONTHLY / YEARLY / WEEKDAYS, base COMPLETION_DATE / DUE_DATE / SPECIFIC_DATE, skip weekends, date de fin / limite d'occurrences (endCount, appliquée sur tous les chemins de complétion)
- Journal des récurrentes : chaque complétion/abandon ajoute un commentaire auto « 🔁 Done — 3 Jul 2026 » ; l'historique complet est copié sur chaque nouvelle occurrence
- Les rappels suivent les récurrentes : compléter une occurrence déplace ses rappels (et l'alerte d'imminence) sur la suivante, décalés avec l'échéance
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
| **Projects** | Kanban recentré (colonne active au milieu, voisines visibles), drag & drop entre statuts, rangée de stats, icônes de statut ; vue liste en cartes de statut repliables (dossier → projet → sous-listes) ; commutateur Board/List ; vue/colonne/repli persistants |
| **Project Detail** | Onglets Tasks/Lists/Notes · Archived (n) · Comments (n) ; sections, drag-to-reorder, filtres tag/priorité, pie chart, temps total ; commentaires en panneau messagerie |
| **Task Detail** | Écran édition complet (toutes les propriétés) |
| **Logbook** | Tâches & projets archivés + journal d'actions |
| **Trash** | Tâches soft-deleted avec restauration |
| **QuickFind** | Recherche temps réel + parsing de dates naturelles FR/EN + résultats par plage de dates |

### Projets, Listes & Notes
- Types : PROJECT, LIST, NOTE, organisés en dossiers avec sous-listes
- Statuts : NOT_STARTED / IN_PROGRESS / WAITING / DONE (auto-DONE à 100 %), historique des changements (`StatusLogEntry`)
- Listes de listes en cartes : grille adaptative (2–4 colonnes), couverture d'image synchronisée, compteur d'items
- **Notes v2** : groupes de notes en cartes, épingler, archiver/restaurer, affichage masonry, markdown complet (blocs de code, tableaux, listes imbriquées, citations, liens, titres)
- Archive / Désarchive (appui long dans la sidebar), section « Archived » repliable
- Sections avec drag-to-reorder, deadline avec compte à rebours coloré
- Temps total suivi (somme de toutes les TimeEntries des tâches)
- Emoji personnalisé comme icône

### Goals (Objectifs)
- Cinq fréquences : **ONCE, DAILY, WEEKLY, MONTHLY, YEARLY** — les objectifs périodiques réinitialisent leur compteur au changement de période (minuit / lundi / 1er du mois / 1er janvier) ; ONCE ne se réinitialise jamais
- Lier des tâches à des objectifs : compléter une tâche liée incrémente l'objectif depuis **tous** les chemins de complétion (listes, détail, widget, notification, auto-complétion checklist, occurrences récurrentes) ; dé-compléter décrémente
- Compteur cible, emoji, couleur, tags, deadline optionnelle ; auto-complété à la cible
- Statut : NOT_STARTED / ON_TRACK / AT_RISK (rythme vs deadline) / COMPLETED, avec filtres
- Journal par objectif (entrées de log avec notes), picker d'objectifs dans TaskDetail
- Synchronisés via LAN sync (y compris l'ancre de période)

### Rappels & Notifications
- Rappels multiples par tâche (AlarmManager exact, exemption Doze) ; ponctuels ou auto-récurrents DAILY/WEEKLY
- Alerte d'imminence N minutes avant l'heure d'échéance (délai configurable, 30 min par défaut)
- Rappels par item de checklist (ponctuels)
- Actions de notification : Compléter (récurrence-aware) / Snooze 15 min
- Canaux son/silencieux séparés, bypass DND, vibration configurable
- Replanification automatique au reboot (BootReceiver)
- Alerte quotidienne à heure fixe (jours configurables lun–dim) + récap hebdomadaire
- Alertes géofence (GPS offline, positions nommées sauvegardées)

### Time Tracking
- Démarrer / Pause / Arrêter par tâche dans TaskDetail
- Ticker temps réel (mise à jour toutes les secondes)
- Démarrage rapide depuis la multi-sélection (Appui long → Plus → "Start Timer")
- Total affiché dans ProjectDetail (somme des entrées complétées)

### LAN Sync
- Sync pair-à-pair entre appareils sur le réseau local (pas de cloud, pas de compte)
- Découverte des appareils + appairage par code, échange de deltas en HTTP sur le LAN
- Résolution de conflits Last-Write-Wins sur le champ `updatedAt`
- Propagation des suppressions par tombstones (table `DeletedEntityEntity`)
- Tout se synchronise : tâches, checklists, projets, notes, commentaires, rappels, objectifs + logs, date trackers, time entries, tags, dossiers, positions
- Rafraîchit le widget après application d'un delta
- Bouton "Force sync" dans les paramètres

### Pièces jointes
- Ajout de fichiers aux tâches (SAF, URI persistable)
- JSON de métadonnées par pièce jointe (les métadonnées se synchronisent ; le transfert de fichier via LAN sync n'est pas encore supporté)

### Widget écran d'accueil (RemoteViews classique)
- Liste scrollable redimensionnable, thème clair/sombre (auto ou forcé)
- Filtre par widget : Today / Inbox / Anytime / Upcoming / Someday / Projet / All
- Complétion depuis le widget (récurrence-aware), tap → ouvrir la tâche, bouton quick-add
- Ligne de métadonnées (date, projet, tags, indicateurs), retards en rouge
- Auto-refresh : instantané sur mutation in-app ou delta LAN sync + alarme auto-chaînée ~10 s

### DateTracker
- Suivi de dates anniversaires / comptes à rebours personnalisés
- Synchronisé via LAN sync

### Lecteur RSS local
- Lecture locale sans WebView ; sync périodique en arrière-plan via WorkManager
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
| Notifications | Son, vibration, bypass DND, délai d'imminence, alertes quotidiennes, récap hebdo |
| Calendrier | Intégration calendrier Android local |
| Backup | Export / import JSON |
| LAN Sync | Appairage des appareils, statut sync, force sync |
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
4. **Gestion de projets** — Vue **Projects** (kanban recentré) pour visualiser l'avancement. Vue **Project Detail** (onglets) pour gérer sections, notes, commentaires, archivées.
5. **Planification hebdomadaire** — Vue **Upcoming** : glisser-déposer une tâche vers une autre date pour la replanifier.
6. **Objectifs** — Lier les tâches récurrentes aux Goals : la progression se remplit toute seule à chaque complétion, et se remet à zéro à chaque période.
7. **Archivage** — À la fin d'un projet : appui long dans la sidebar → Archive. Les tâches complétées restent consultables dans **Logbook**.
8. **Suppression** — Supprimer envoie en **Trash** (soft delete). Restaurer ou vider définitivement.
9. **Sync** — Automatique dès que deux appareils appairés sont sur le même réseau. Forcer depuis Paramètres > LAN Sync si besoin.

---

## 5. Interface

- **Nombre d'écrans :** ~28 (15 écrans principaux + 13 écrans de paramètres/détail)
- **Description des écrans principaux :**
  - `TodayScreen` — liste avec groupes Overdue/Today/Evening, badge, filtres chips
  - `InboxScreen` — liste simple avec swipe, multi-sélection
  - `UpcomingScreen` — défilement daté avec drag-to-reschedule
  - `AnytimeScreen` / `SomedayScreen` — listes filtrées par routing logique
  - `ProjectsScreen` — kanban recentré avec drag & drop, ou liste en cartes de statut
  - `ProjectDetailScreen` — header coloré + emoji, onglets, sections collapsibles, stats, notes, commentaires
  - `TaskDetailScreen` — panneau de propriétés scrollable (12+ sections)
  - `LogbookScreen` — archivés + journal d'actions, QuoteCard
  - `TrashScreen` — tâches supprimées avec date de suppression, Restore / Delete Forever
  - `QuickFindScreen` — overlay de recherche avec parsing de dates naturelles
  - `GoalsScreen` / `GoalDetailScreen` — objectifs périodiques avec journal
  - `DateTrackerScreen` — dates anniversaires / comptes à rebours
  - `RssScreen` / `RssArticleScreen` — lecteur RSS local
- **Captures d'écran :** Non disponibles dans le repo.
- **Points forts UI :**
  - Thème Material 3 sombre/clair cohérent
  - Swipe-to-complete avec animation et snackbar undo
  - Transitions et animations soignées (chevrons animés, délai auto-complétion checklist)
  - Sidebar adaptative phone/tablette
  - Widget RemoteViews avec thème système automatique
- **Problèmes UI connus :**
  - MetaRow : trailing + value non mutuellement exclusifs (source de confusion passée)
  - Icons Material non-AutoMirrored (warnings à la compile, cosmétiques, sans impact)

---

## 6. Architecture technique

- **Langage :** Kotlin 2.1.0
- **Framework UI :** Jetpack Compose + Material Design 3
- **Base de données :** Room v2.6.1 (SQLite local), schéma v38, 24 entités, **migrations non destructives** depuis la v27
- **State management :** ViewModel + StateFlow (UDF strict — immutable UiState + sealed Event)
- **Navigation :** Navigation Compose (type-safe routes, sealed class `Screen`)
- **Async :** Kotlin Coroutines + Flow
- **DI :** Manual — `AppContainer` singleton (pas de Hilt/Koin)
- **Préférences :** DataStore Preferences (thème, sidebar, notifications, LAN sync, backup, calendrier…)
- **Sync :** LAN sync intégré — découverte + appairage + échange de deltas HTTP en pair-à-pair (pas de cloud, pas de serveur)
- **Notifications :** AlarmManager exact alarms + BroadcastReceivers (rappels, imminence, checklist, alertes quotidiennes) + BootReceiver
- **Background :** WorkManager (backup, sync RSS)
- **Géofencing :** GeofenceManager + GeofenceReceiver (géofences indexées par position)
- **Widget :** RemoteViews classique (RemoteViewsCompat, technique Tasks.org)
- **Drag & Drop :** `sh.calvin.reorderable`
- **Intégrations Android :** CalendarProvider, FileProvider, SAF (Storage Access Framework), OpenableColumns
- **API externes :** Aucune (seul le fetch RSS sort sur le réseau, à la demande)
- **Hébergement :** Aucun. APK distribué via tags GitHub + copie dans `K:\🔁 Sync\Projets\Codes\OnlyFlows\Released\`
- **Authentification :** Aucune. App mono-utilisateur locale.
- **minSdk :** 26 (Android 8.0) — **targetSdk :** 35
- **Build :** Gradle Kotlin DSL (AGP 8.7.3, Gradle 8.9), signature via propriétés Gradle / variables d'environnement (`KEYSTORE_PATH`, `KEY_ALIAS`, etc.)
- **Licence :** Propriétaire

---

## 7. Structure des données (entités principales)

### Task
```
id, title, note, isCompleted, isArchived, projectId?, sectionId?,
dueDate?, dueTime?, endTime?, scheduledGroup?, hasReminders, hasAttachments,
isStarred, tagIds: List<String>, goalIds: List<String>,
priority (NONE/LOW/MEDIUM/HIGH), difficulty, impact,
startDate?, recurrence: RecurrenceRule?, sortOrder,
hasNote, hasChecklist, recurrenceDoneCount, recurrenceAbandonedCount,
checklistItems: List<ChecklistItem>, dependsOnTaskId?,
createdAt, updatedAt, deletedAt?  ← null = actif, non-null = Corbeille
```

### Project
```
id, title, note, colorHex, iconEmoji, coverImage?, isCompleted, folderId?,
parentProjectId?, type (PROJECT/LIST/NOTE),
sections: List<Section>, sortOrder, dueDate?,
status (NOT_STARTED/IN_PROGRESS/WAITING/DONE),
isArchived, tagIds, createdAt, updatedAt
```

### Goal
```
id, title, note, emoji, colorHex, frequency (ONCE/DAILY/WEEKLY/MONTHLY/YEARLY),
targetCount, currentCount, periodStart?  ← ancre de la période courante,
sortOrder, isCompleted, deadline?, tags, createdAt, updatedAt, deletedAt?
```

### Autres entités
| Entité | Champs clés |
|---|---|
| Section | id, projectId, title, sortOrder |
| Tag | id, name, colorHex |
| Folder | id, title, sortOrder |
| ChecklistItem | id, taskId, sectionId?, title, isCompleted, sortOrder, dueDate?, startDate? |
| ChecklistSection | id, taskId, title, sortOrder |
| ChecklistItemReminder | id, checklistItemId, scheduledAt |
| Reminder | id, taskId, scheduledAt, locationId?, recurrenceType (NONE/DAILY/WEEKLY) |
| Attachment | id, taskId, fileName, mimeType, uri, fileSize, createdAt |
| TimeEntry | id, taskId, startedAt, endedAt? (null = en cours) |
| Comment | id, taskId?, projectId?, content, createdAt, updatedAt |
| RecurrenceRule | type, interval, daysOfWeek, basis, skipWeekends, endDate?, endCount? |
| GoalLog | id, goalId, label, note, tags, loggedAt, isStarred |
| DateTracker | id, name, emoji, targetDate, category, isAnnual |
| RssFeed | id, name, url, colorHex, keywords |
| RssArticle | id, feedId, title, link, description, pubDate, isRead |
| ActionLog | id, entityType, entityId, actionType, label, occurredAt |
| SavedLocation | id, name, latitude, longitude, radiusMeters |
| PairedDevice | id, name, lastKnownIp, port, pairedAt |
| SyncMeta | métadonnées de synchronisation LAN |
| DeletedEntity | id, entityType, entityId, deletedAt (tombstones pour sync) |

**Relations :** Task → Project (FK optionnel), Task → Section (FK optionnel), Task → Goals (liste d'ids), ChecklistItem → Task/ChecklistSection, Reminder → Task, Attachment → Task, TimeEntry → Task, Comment → Task ou Project, GoalLog → Goal, RssArticle → RssFeed.

---

## 8. Règles métier

- **Inbox** : tâche sans projet ET sans scheduledGroup ET non complétée ET non archivée ET non supprimée
- **Today** : scheduledGroup IN ('TODAY','THIS_EVENING') ET non archivée ET non supprimée
- **Upcoming** : dueDate non null ET non complétée ET non archivée ET non supprimée
- **Anytime (auto)** : projectId non null ET priority ≠ NONE ET scheduledGroup null ET dueDate null ET active
- **Someday (auto)** : projectId non null ET priority = NONE ET scheduledGroup null ET dueDate null ET active
- **Soft delete obligatoire** : toute suppression user appelle `softDelete()` (sets `deletedAt`). Hard delete uniquement pour : ConvertToProject (supprime la tâche originale) et EmptyTrash.
- **DB version** : bumper `AppDatabase.version` (+1) ET ajouter une `Migration(n, n+1)` non destructive à chaque changement de schéma
- **Version applicative** : toujours bumper `versionName` + `versionCode` (+1) avant un build release
- **Récurrence** : `completeRecurring()` crée la prochaine occurrence, archive l'actuelle, applique la limite d'occurrences (endCount), déplace les rappels et la progression des objectifs. Les appelants sans `Task` domaine (widget, notification) passent par `taskRepo.complete(id)`. Ne jamais appeler `setCompleted(id, true)` en direct sur une récurrente.
- **Objectifs** : la progression liée aux tâches vit dans `TaskRepository.applyGoalProgress()` — jamais dans les ViewModels. Le rollover de période (`rolloverIfExpired`) se vérifie à l'ouverture des écrans Goals et avant chaque incrément.
- **Projet DONE auto** : quand toutes les tâches sont complétées, le statut passe automatiquement à DONE
- **Section supprimée** : les tâches de la section ont leur `sectionId` mis à NULL (elles ne sont pas supprimées)
- **LAN sync write** : chaque méthode d'écriture dans un repository doit appeler `lanSyncWriter` après l'appel DAO
- **AppContainer ordre** : composants LAN sync déclarés en premier (LanSyncWriter, LanSyncCoordinator…), puis les repositories qui les reçoivent en paramètre (goalRepository/reminderRepository/reminderScheduler avant taskRepository)
- **snapshotFlow** : pour observer du state Compose dans un `LaunchedEffect`, toujours utiliser `snapshotFlow { someState }` — jamais `LaunchedEffect(someState)`

---

## 9. Points faibles actuels

- **Pas de tests** : aucun test unitaire ni instrumenté. La correction d'un bug se valide uniquement par build + test manuel.
- **DI manuelle** : `AppContainer` grossit à chaque nouvelle feature — acceptable pour un projet mono-dev.
- **Pas d'état d'erreur structuré** : les erreurs dans les ViewModels ne sont pas toujours exposées à l'UI de façon cohérente.
- **Compteurs et sync** : la résolution Last-Write-Wins peut perdre un incrément d'objectif si deux appareils complètent des tâches liées au même instant.
- **Statut ON_TRACK/AT_RISK** : le calcul de rythme est pensé pour les objectifs ONCE avec deadline ; peu signifiant pour les objectifs périodiques.
- **Warnings compile** : icônes Material non-AutoMirrored (cosmétiques, ignorables).

---

## 10. Concurrence

| App | Ce qu'elle fait mieux | Ce qu'elle fait moins bien |
|---|---|---|
| **Things 3** | UX exceptionnelle, polish, iOS-first | iOS uniquement, pas de sync P2P, pas de time tracking |
| **Todoist** | Collaboration, NLP de saisie, multi-plateforme | Cloud obligatoire, abonnement payant, moins de détail par tâche |
| **TickTick** | Time tracking intégré, Pomodoro, calendrier | Cloud obligatoire, UI chargée |
| **Obsidian Tasks** | Intégration markdown/PKM | Pas une vraie app de tâches, pas mobile-first |
| **Orgzly** | Local-first, sync fichier | UI datée, courbe d'apprentissage org-mode |

**Avantage clé d'OnlyFlows** : combinaison unique de local-first + LAN sync pair-à-pair + niveau de détail Things 3 + widget + objectifs périodiques sur Android natif.

---

## 11. Vision V2 / Roadmap

*Basé sur `Todo.md` et CHANGELOG récent*

- **Transfert de fichiers via LAN sync** : synchroniser le contenu des pièces jointes, pas seulement les métadonnées
- **Statistiques avancées** : temps par projet, vélocité, tendances, streaks d'objectifs
- **Notes enrichies** : images inline
- **Raccourcis clavier tablette** : couverture plus complète
- **Export** : formats additionnels (CSV, Markdown, iCal)

---

## 12. Priorités

| Critère | Score /5 |
|---|---|
| Performance | ★★★★☆ — Room + Flow réactif, widget event-driven |
| Simplicité | ★★★★★ — UI minimaliste, priorité absolue |
| Automatisation | ★★★★☆ — récurrence, rappels qui suivent, objectifs auto-alimentés, sync auto |
| Design | ★★★★★ — inspiré Things 3, Material 3, dark/light soigné |
| IA | ★☆☆☆☆ — hors scope (local-first, pas de réseau) |
| Collaboration | ★☆☆☆☆ — mono-utilisateur, pas prévu |
| Mobile | ★★★★★ — Android-first, widget, tablette supportée |
| Desktop | ★☆☆☆☆ — Android uniquement |

---

## 13. Ressources disponibles

- **Code source complet** : `k:\[F] ARCHIVES\PROGRAMMATION\Android\OnlyFlows\` (repo Git)
- **CLAUDE.md** : guide architecture pour les agents IA
- **CHANGELOG.md** : historique complet des versions (référence autoritaire)
- **README.md** : feature list + stack + architecture overview
- **docs/BUILD.md** : instructions de build
- **APK releases** : `K:\🔁 Sync\Projets\Codes\OnlyFlows\Released\` + tags GitHub
- **Maquettes** : aucune (l'UI est définie directement dans le code)
- **Base de données** : SQLite locale sur l'appareil, export JSON via BackupManager

---

## 14. Contraintes

- **Budget :** Zéro — projet personnel, pas de dépenses externes
- **Temps :** Pas de deadline fixe — développement au fil de l'eau
- **Limites techniques :**
  - Android uniquement (pas de iOS, pas de web)
  - Local-first strict — aucun appel réseau permis sauf RSS fetch et LAN sync
  - Pas de compte utilisateur, pas de backend
- **Compatibilité requise :** Android 8.0+ (minSdk 26), téléphones et tablettes Android

---

## 15. Ce que vous attendez de l'agent

- **Développeur Android principal** — implémenter des features, corriger des bugs, optimiser
- **Règles absolues à respecter :**
  1. Toujours builder (`assembleDebug`) avant de déclarer une tâche terminée
  2. Bumper `versionName` + `versionCode` avant chaque release build
  3. Ne jamais utiliser `taskRepo.delete()` depuis une action user — toujours `softDelete()`
  4. Bumper `AppDatabase.version` (+1) ET écrire une migration non destructive à chaque changement de schéma
  5. Ne jamais ajouter de Co-Authored-By dans les commits
  6. Flow release : build release → commit → push → tag → copier l'APK dans `Released\` (pas de `gh release` sauf demande explicite)
