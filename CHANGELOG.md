# Changelog — OnlyFlows

Tous les changements notables du projet sont documentés ici.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [0.69.0] — 2026-07-08 · Widget Countdown (Date Tracker)

### Ajouté

- **Nouveau widget 2×2 « Countdown »** : carte affichant le compte à rebours d'un Date Tracker au choix — gros compteur de jours en gras au centre, unité (« days left » / « days ago » / « today! 🎉 ») en dessous, titre (emoji + nom) en bas. Sélection du tracker au placement (reconfigurable par appui long) : trackers de la liste + compteurs intégrés **Fin de mois** et **Fin d'année**. Thème System/Light/Dark comme le widget existant. Trackers annuels et anniversaires comptent vers la prochaine occurrence. Auto-refresh ~10 s (même alarme auto-chaînée) + rebuild sur mutations et après LAN sync.

---

## [0.68.0] — 2026-07-03 · Les rappels suivent les tâches récurrentes

### Ajouté

- **Rappels reportés sur l'occurrence suivante** : compléter (ou abandonner) une tâche récurrente déplace maintenant ses rappels sur la nouvelle occurrence, décalés du même delta que l'échéance (un rappel à 09:00 reste à 09:00 le nouveau jour). Avant, le rappel mourait avec la première occurrence — une seule notification puis plus rien. Les rappels géolocalisés suivent aussi (le geofence, indexé par lieu, reste valide). L'alerte d'imminence (N min avant l'échéance) est réarmée sur la nouvelle occurrence. Fin de récurrence : alarmes annulées et rappels supprimés proprement.
- L'écran de détail ne supprime plus les rappels quand on complète une tâche **récurrente** (comportement conservé pour les tâches simples).

---

## [0.67.0] — 2026-07-03 · Objectifs périodiques + objectifs fiables partout + widget auto-refresh

### Ajouté

- **Réinitialisation périodique des objectifs** : la fréquence (DAILY/WEEKLY/MONTHLY/YEARLY) n'était qu'un libellé — un objectif quotidien atteint restait complété pour toujours. Nouveau champ `periodStart` (migration 37→38, non destructive, synchronisé) : le compteur et l'état complété se réinitialisent au changement de période (minuit / lundi / 1er du mois / 1er janvier), vérifié à l'ouverture des écrans Goals et avant chaque incrément lié à une tâche. ONCE ne se réinitialise jamais ; les objectifs existants sont ancrés sur la période courante sans perte de compteur.

### Corrigé

- **Progression des objectifs sur tous les chemins de complétion** : la mise à jour des objectifs liés vivait uniquement dans l'écran de détail de tâche. Compléter depuis Today/Inbox/Anytime/Someday/Upcoming/Projet, le widget, une notification, l'auto-complétion par checklist ou une **tâche récurrente** ne comptait pas. La logique est déplacée dans `TaskRepository` (`setCompleted` + `completeRecurring`) — chaque complétion incrémente, chaque dé-complétion décrémente, avec garde anti double-comptage sur les appels idempotents.
- **Action « Done » d'une notification sur une tâche récurrente** : marquait la tâche complétée sans créer l'occurrence suivante ni journaliser. Le widget et les notifications passent maintenant par un `complete()` unique du repository qui respecte la récurrence.
- **Limite d'occurrences (endCount) respectée partout** : le contrôle vivait uniquement dans l'écran de détail — compléter une récurrente limitée depuis une liste, le widget ou une notification créait une occurrence au-delà de la limite. Déplacé dans `completeRecurring()` ; la dernière occurrence est maintenant archivée avec son journal « Done » quel que soit l'écran.
- **Widget : rafraîchissement automatique** (~10 s, alarme auto-chaînée inexacte) — couvre le passage de date (retards) et tout chemin sans événement.
- **Widget : rafraîchi après réception d'un delta LAN sync** — les écritures distantes contournent les repositories et ne déclenchaient jamais la mise à jour du widget.

---

## [0.66.0] — 2026-07-02 · Listes en cartes + notes v2 + kanban recentré

### Ajouté

- **Vue projet en onglets** partout : Tasks/Lists/Notes · Archived (n) · Comments (n) — infos et filtre fixés au-dessus, commentaires en panneau messagerie avec saisie en bas.
- **Listes de listes en cartes** : grille adaptative (2 colonnes téléphone, 3/4 tablette), **couverture d'image synchronisée** (stockée dans l'entité, redimensionnée ~600 px), compteur d'items par carte, carte « + » pour créer une liste (appui long sur une carte → définir/retirer la couverture).
- **Notes v2** : groupes de notes (cartes, comme les listes), épingler (tri en tête + icône), archiver/restaurer (onglet Archived), affichage **masonry** (2 colonnes mobile, adaptatif tablette).
- **Markdown complet dans les notes** : blocs de code ```, listes numérotées, citations >, puces imbriquées, liens, titres #–######, ***gras italique***, tableaux, sélection/copie du texte en mode lecture.
- **Écran Projects repensé** : kanban recentré (colonne active au milieu, voisines visibles sur les bords), glisser-déposer des cartes entre statuts (vibration à la prise), échéance sur chaque carte (« No deadline » sinon), vue liste en cartes de statut repliables avec hiérarchie dossier → projet → sous-listes, rangée de stats (compteur par statut), commutateur Board/List flottant en bas, **icônes de statut** (fin des emojis), vue/colonne/repli **persistants**.

### Corrigé

- Alignement des badges/icônes sur la première ligne du titre (ligne de la checkbox) quand le titre passe sur 2 lignes.
- Menu ⋮ des notes aligné en haut de la carte, pas centré.
- Base : migrations 35→37 non destructives (couverture, épingle/archive de note) — données conservées.

---

## [0.65.0] — 2026-07-02 · Journal des récurrentes + widget fiable + grand nettoyage

### Ajouté

- **Journal de suivi des tâches récurrentes** : à chaque complétion/abandon, un commentaire automatique « 🔁 Done — 2 Jul 2026 » (ou Abandoned) est ajouté. L'historique complet des commentaires est copié sur la nouvelle occurrence — le journal reste donc visible sur la tâche vivante, et se synchronise comme n'importe quel commentaire. Fin de récurrence : le journal reste sur la dernière occurrence archivée.
- **Icônes de propriétés dans les listes** : goal, impact, difficulté, date de début, commentaires et suivi du temps sont maintenant visibles sur les lignes de tâches (avant : uniquement dans le détail). Le badge de date affiche la plage horaire début–fin.

### Corrigé

- **Widget — rafraîchissement** : le widget se met à jour après *toutes* les mutations (récurrente complétée, déplacement, suppression, corbeille, étoile, réordonnancement, checklist…) et pas seulement création/édition/complétion simple. Rebind des listes ajouté pour les appareils < Android 12.
- **Widget — détails** : la ligne détail affiche projet, tags et indicateurs de propriétés, plus seulement « date · heure »; visible aussi pour les tâches sans date.
- **Sync** : la dernière complétion/abandon d'une récurrence arrivée à son terme n'était jamais synchronisée (return prématuré).

### Supprimé / Technique

- Suppression du stack Syncthing mort (~1 000 lignes, remplacé par LAN sync depuis longtemps), du worker no-op replanifié toutes les 15 min, des docs obsolètes (doc.md, docs/SYNC.md, docs/ARCHITECTURE.md) et des logs JVM traqués (~46 000 lignes).
- Dépendances retirées : appcompat, adaptive-layout, adaptive-navigation.
- Renommages : fichiers `*RepositoryImpl.kt` → `*Repository.kt`, paramètre `syncthingWriter` → `lanSyncWriter`; MockData réduit à l'usage réel des previews.

## [0.61.0] — 2026-06-30 · App Wear OS (montre) + pont téléphone

### Ajouté

- **Application Wear OS** (`:wear`) inspirée de Tasks.org, branchée sur le système OnlyFlows :
  - Liste de tâches (cartes, complétion, retards en rouge, ajout vocal), sélecteur de liste/filtre (listes intelligentes + projets), édition de tâche (titre vocal, terminée, priorité), réglages.
  - **Rafraîchissement live** : le téléphone notifie la montre à chaque changement.
  - **File hors-ligne** : tâches créées sans le téléphone, synchronisées au retour de connexion.
  - **3 complications** : nombre de tâches actives, progression du jour, ajout rapide.
- **Pont téléphone** : `OnlyFlowsWearService` répond aux requêtes de la montre (getTasks / getLists / getTask / saveTask / complete / getTaskCount) via le Wear Data Layer (MessageClient), en lisant les repos Room existants.

### Technique

- Transport identique à Tasks.org (Data Layer), sans gRPC/protobuf/Hilt : JSON `org.json` + `MessageClient.sendRequest`.

---

## [0.60.0] — 2026-06-30 · Déplacer une tâche (List) + quick-add widget enrichi

### Ajouté

- **Propriété « List » dans le détail de tâche** : la ligne est désormais toujours affichée (avant : masquée si la tâche n'avait pas de projet). Une tâche de l'Inbox peut donc être déplacée vers n'importe quelle liste/projet ou groupe (Today/Anytime/Someday) via le SmartMoveSheet existant.
- **Quick-add widget enrichi** : en plus du titre, choix de **Quand** (Inbox/Today/Anytime/Someday), **Priorité** (Aucune/Basse/Moyenne/Haute) et **Tags** (multi-sélection). La destination par défaut suit le filtre du widget.

---

## [0.59.0] — 2026-06-30 · Nouveau widget (RemoteViews classique, technique Tasks.org)

### Ajouté

- **Widget liste de tâches** reconstruit en RemoteViews classique (`AppWidgetProvider` + `RemoteViewsCompat.setRemoteAdapter` / `RemoteCollectionItems`, sans `RemoteViewsService`). Plus robuste que Glance pour une liste défilante.
- **Filtre** : Today / Anytime / Inbox / Upcoming / Someday / Toutes + projets (écran de config, affiché à la pose et via ⚙).
- **Ajout rapide** sans ouvrir l'app (popup flottante → Inbox).
- **Compléter une tâche** depuis le widget (case à cocher, gère la récurrence).
- **Détails** (date · heure), **retards en rouge**, **thème** (système/clair/sombre), **rafraîchissement auto** à chaque mutation de tâche.

### Technique

- Clics via `setPendingIntentTemplate` + `setOnClickFillInIntent` → activité dispatcher transparente.
- Préférences par widget (SharedPreferences clé `appWidgetId`).
- Ajout dépendance `androidx.core:core-remoteviews:1.1.0`.

---

## [0.58.3] — 2026-06-30 · Widget reconstruit selon les bonnes pratiques Glance officielles

### Corrigé

- **Widget à moitié affiché (root cause)** : `widget_info.xml` n'avait pas d'`android:initialLayout`. Ajout de `@layout/glance_default_loading_layout` (layout de chargement fourni par glance-appwidget) — exigé par la doc, sans lui le widget reste vide jusqu'à un update manuel.

### Modifié

- **Widget reconstruit selon la doc Glance** : `provideGlance` charge les données via `withContext(Dispatchers.IO)` puis `provideContent { GlanceTheme(colors) { … } }`.
- **GlanceTheme (glance-material3)** remplace les couleurs faites main et les drawables de fond ; réutilise les `ColorScheme` clair/sombre de l'app. Thème : suit le système ou force clair/sombre ; retards via `GlanceTheme.colors.error`.
- `SizeMode.Exact`, `actionStartActivity`/`actionRunCallback`, fond via `widgetBackground` + `cornerRadius`.

### Supprimé

- Ancien `OnlyFlowsWidget.kt` (réécrit) et 5 drawables de widget inutilisés.

---

## [0.58.2] — 2026-06-30 · Widget simplifié — quick-add en popup + chooser/réglages en dialog

### Ajouté

- **Ajout rapide sans ouvrir l'app** : le bouton + ouvre une popup flottante (titre → Ajouter) qui crée la tâche dans l'Inbox et se referme, sans entrer dans l'application.

### Modifié

- **Widget allégé** : suppression de la rangée de tris (Date/Priorité/Tag/None). Tri par défaut = date d'échéance.
- **Chooser & réglages en dialog flottant** : la pastille de filtre et l'écran de réglages s'affichent en popup par-dessus l'écran d'accueil au lieu d'un écran plein style « app ».

### Supprimé

- Code mort : `SetSortCallback`, préférence `sortBy`, `keyAddTask`, `priorityRank`, couleurs de chips inutilisées.

---

## [0.58.1] — 2026-06-30 · Fix widget — bouton + (ajout tâche) + sélecteur de filtre

### Corrigé

- **Bouton + du widget** : lançait `MainActivity` sans extra, le flux d'ajout de tâche ne se déclenchait jamais. Passe désormais `EXTRA_ADD_TASK` → ouvre l'ajout de tâche.
- **Choix du filtre** : la pastille cyclait d'un cran par tap (impossible de choisir une liste précise). Elle ouvre maintenant un sélecteur (Toutes / Today / Anytime / Inbox / Upcoming / Someday + projets/sections) — on tape, ça s'applique et se ferme. Réglages (détails / groupe priorité / thème) sur le même écran ; bouton ⚙ retiré (redondant).

---

## [0.58.0] — 2026-06-30 · Widget Glance — filtres listes intelligentes + réglages + retards en rouge

### Ajouté

- **Filtres widget** : la pastille de scope cycle désormais sur les listes intelligentes (All / Today / Anytime / Inbox / Upcoming / Someday) en plus des projets/sections, via les flows DAO existants.
- **Réglages widget** (bouton ⚙ → `WidgetConfigActivity`) : Afficher les détails, Grouper par priorité, Thème (Système / Clair / Sombre).
- **Groupe par priorité** : en-têtes High / Medium / Low / None dans la liste du widget.

### Modifié

- **Thème widget** : la préférence `theme` (définie mais ignorée) est maintenant appliquée (FOLLOW / LIGHT / DARK).

### Corrigé

- **Tâches en retard** : titre affiché en rouge dans le widget quand la date d'échéance est dépassée.

---

## [0.57.0] — 2026-06-30 · Sidebar « Show more » + Archivé en section + alignement TaskItem

### Ajouté

- **Section « Archivé »** : les projets archivés ont désormais leur propre écran (`ArchivedScreen`), accessible comme les autres sections (Trash, Inbox…) au lieu d'un dropdown inline dans la sidebar. Tap ouvre le projet, l'icône désarchive.

### Modifié

- **Sidebar épurée** : seuls Inbox / Today / Upcoming / Anytime / Someday / Logbook restent visibles en haut. Projects, Goals, RSS, Date Tracker, Trash et Archivé sont repliés dans un dropdown « Show more » sous Logbook.

### Corrigé

- **Alignement TaskItem** : badges (récurrence, note, rappel, deadline, checklist) centrés verticalement sur le titre au lieu de flotter au-dessus de la checkbox et du titre.

---

## [0.55.5] — 2026-06-06 · Fix migration DB 30→31

### Corrigé

- **Crash au démarrage** : migration Room 30→31 manquante — `fallbackToDestructiveMigration()` ignoré quand des migrations explicites existent. Ajout de `Migration(30, 31)` avec `ALTER TABLE reminders ADD COLUMN recurrenceType`.

---

## [0.55.4] — 2026-06-06 · Fix rappels récurrents + bouton rappel + navigation backstack

### Corrigé

- **Rappels récurrents** : les rappels quotidiens/hebdomadaires se reprogramment automatiquement après affichage si la tâche n'est pas complétée. Nouveau champ `recurrenceType` persisté en DB (v31), alarmes désormais keyed par `reminderId` (était `taskId`). `BootReceiver` et `ToggleComplete` mis à jour en conséquence.
- **Bouton "Définir ce rappel" toujours grisé** : `persoTarget` était `null` quand la tâche n'avait pas de deadline. Remplacé par un fallback "N jours/semaines à partir d'aujourd'hui".
- **BackStack écrans racines** : `popUpTo(Int)` (API Navigation Fragments, silencieusement no-op en Navigation Compose) remplacé par `popUpTo(findStartDestination().id)` + `saveState`/`restoreState`.

---

## [0.55.2] — 2026-05-15 · Fix widget boutons — clickable premier dans la chaîne

### Corrigé

- **Root cause réel des boutons morts** : dans Glance/RemoteViews, `.clickable()` doit être le **premier** modifier de la chaîne — s'il est après `.background()` / `.padding()`, la vue englobante absorbe l'event et le click n'est jamais reçu. Chaque élément cliquable du widget avait clickable EN DERNIER.
- **Header Box overlay** remplacé par un `Row` — la deuxième Box full-width interceptait silencieusement tous les taps destinés à la scope pill en dessous.

---

## [0.55.1] — 2026-05-15 · Fix widget boutons silencieusement supprimés par R8

### Corrigé

- **ProGuard** — règle `ui.widgets.**` (ancienne tentative) corrigée en `ui.widget.**` ; les `ActionCallback` étaient silencieusement supprimés par R8 en release → aucun bouton ne répondait
- **`@Keep`** ajouté sur `ToggleTaskCallback`, `SetScopeCallback`, `SetSortCallback`
- Règle explicite `-keep class * implements ActionCallback` ajoutée

---

## [0.55.0] — 2026-05-15 · Widget écran d'accueil

### Ajouté

- **Widget Glance 2×2** — widget redimensionnable affichant les tâches actives sur l'écran d'accueil
  - Sélecteur de portée : All / Projet / Section (tap pour cycler)
  - Filtre de tri : Date / Priority / Tag / None
  - Liste scrollable de tâches avec titre, étiquette projet/section, date, heure et indicateur de priorité
  - Bouton `[+]` pour ouvrir l'application
  - Tap sur une tâche → marque comme complétée (avec gestion correcte de la récurrence)
- **Thème** : suit automatiquement le thème système (clair / sombre), zéro coin dur
- **Auto-refresh sans latence** : Room Flow → `updateAll` déclenché à chaque mutation de tâche
- **Récurrence** : le widget appelle `completeRecurring()` au lieu de `setCompleted()` — la prochaine occurrence est correctement créée

---

## [0.50.3] — 2026-05-02 · Correctifs sync Syncthing — mises à jour manquantes

### Corrigé

- **Task** — `setDependsOn`, `setGoals`, `setScheduledGroup` : écriture Syncthing ajoutée après mise à jour DAO
- **Task** — `abandonRecurring` : la nouvelle occurrence et ses items de checklist sont maintenant écrits dans Syncthing ; l'occurrence archivée est également mise à jour
- **Task** — `reorderTasks` : chaque tâche réordonnée est maintenant synchronisée
- **Project** — `archive` / `unarchive` : l'entité est relue puis écrite dans Syncthing après la mise à jour `isArchived`
- **Goal** — `setProgress`, `setCompleted` : écriture Syncthing ajoutée après mise à jour DAO
- **GoalLog** — `setStarred` : écriture Syncthing ajoutée après mise à jour DAO

---

## [0.50.2] — 2026-05-02 · Correctifs sync Syncthing — suppressions manquantes

### Corrigé

- **Syncthing sync — suppressions** — plusieurs types d'entités n'étaient pas correctement propagés lors d'une suppression :
  - `DateTracker` — tombstone Room manquant (`deleted_entities` non alimenté)
  - `ChecklistItem` — tombstone Room manquant
  - `Reminder` (suppression en cascade par tâche) — aucune tombstone fichier ni Room
  - `Attachment` (suppression en cascade par tâche) — aucune tombstone fichier ni Room
  - `TimeEntry` (suppression en cascade par tâche) — aucune tombstone fichier ni Room
  - Tag (suppression groupée `deleteUnused`) — aucune tombstone fichier ni Room
- **Commentaires** — la modification d'un commentaire (`edit`) mettait à jour Room mais pas le fichier JSON Syncthing ; le fichier est désormais réécrit immédiatement.

---

## [0.50.1] — 2026-05-02 · Correctif filtre priorité Today

### Corrigé

- **Filtre priorité Today** — le menu déroulant de priorité disparaissait quand aucune tâche du jour n'avait de tag. La barre de filtres est maintenant toujours affichée ; les chips de tags s'affichent dynamiquement.

---

## [0.50.0] — 2026-05-02 · Archive, filtres dynamiques Today, badge compteur

### Ajouté

- **Archive** — action « Archive » dans le menu contextuel (appui long) de la sidebar pour les types LIST, NOTE et PROJECT. Les éléments archivés disparaissent de la liste active et apparaissent dans une section « Archived (N) » pliable en bas de la sidebar.
- **Restauration d'archive** — bouton « Restore » (icône Unarchive) sur chaque élément archivé pour le remettre dans la liste active.
- **Badge compteur Today** — le titre de la vue Today affiche désormais un badge `complétées/total` (ex. `3/10`) identique aux badges des projets.

### Amélioré

- **Filtres de tags Today** — les chips de filtrage n'affichent plus tous les tags de la base de données : seuls les tags présents sur les tâches visibles du jour (Today + Evening + Overdue) sont proposés. Les filtres actifs qui ne correspondent plus à aucune tâche visible sont automatiquement purgés.

---

## [0.44.0] — 2026-04-25 · Widget Month — navigation mois + headers français

### Amélioré

- **Widget Month** — navigation mois avec boutons `<` / `>` (mois précédent / suivant).
- **Widget Month** — en-têtes jours de la semaine français, lundi en premier : L M M J V S D.
- **Widget Month** — titre du mois en bleu (`AccentBlue`), jour sélectionné en cercle bleu.
- **Widget Month** — couleurs des projets affichées dans la liste des tâches.

---

## [0.43.0] — 2026-04-25 · Widget Clock + Liste

### Ajouté

- **Widget Clock + Liste** — widget rectangulaire 4×2 : horloge numérique (heure + date) à gauche, liste de tâches configurable à droite.
- Badge rouge pour les tâches en retard (overdue), compteur total gris.
- Cercles colorés par projet dans la liste des tâches.
- Config : choix de la liste affichée (Aujourd'hui / Demain / Inbox / Upcoming / Anytime / Someday).

---

## [0.42.0] — 2026-04-25 · Widget Liste — redesign complet

### Amélioré

- **Widget Liste** — nom du projet affiché à droite de chaque tâche (gris, 11sp).
- **Widget Liste** — header : titre `▾` cliquable ouvrant la config + bouton `⋮`.
- **Widget Liste** — `WidgetConfig` étendu : `tagFilter`, `plusDestination` (Today/Inbox), `checkboxAction` (Compléter / Basculer priorité).
- **Widget Config** — interface Things 3 : trois lignes cliquables groupées (Liste / Bouton + / Case à cocher) avec pickers.
- **TogglePriorityCallback** — action de checkbox alternative : bascule priorité HIGH ↔ NONE.

---

## [0.41.1] — 2026-04-25 · Sidebar — visibilité sections (Général > Sidebar)

### Corrigé

- **Sidebar — visibilité** — les toggles sont déplacés dans Général > Sidebar (ils étaient dans les Paramètres principaux et cassaient le scroll).

---

## [0.41.0] — 2026-04-25 · Sidebar — sections masquables + QuoteCard améliorée

### Ajouté

- **Sidebar — sections masquables** — Projects, Goals, RSS, Dates & Comptes, Trash peuvent être masqués/affichés via Paramètres > Général > Sidebar. Persisté dans DataStore.

### Corrigé

- **QuoteCard** — suppression du bouton ✕ (la carte disparaissait définitivement après un tap). La carte est maintenant permanente (nouvelle citation à chaque lancement).
- **QuoteCard position** — déplacée juste au-dessus des tâches (après les filtres/stats), pas sous le titre.

---

## [0.40.0] — 2026-04-25 · QuoteCard dans Projects, ProjectDetail, Logbook

### Ajouté

- **QuoteCard** dans ProjectsScreen (après le sélecteur de vue), ProjectDetailScreen (entre les stats et le filtre), LogbookScreen (sous le header).

---

## [0.32.0] — 2026-04-21 · Bug Fixes + UX Improvements + Collapsible Sections

### Corrigé

- **Commentaires disparaissent** — les commentaires étaient observés dans une coroutine séparée de `loadTask`, créant une condition de course. Fusion dans le même `combine` pour des mises à jour atomiques.
- **Statut projet non synchronisé** — `updateStatus()` ne déclenchait pas `syncthingWriter.writeProject()`. Corrigé.
- **Tâche "Won't do" non synchronisée** — `abandonTask()` manquait un appel `syncWrite(id)`. Corrigé.
- **Réordonnancement de sections non synchronisé** — `section.copy(sortOrder = index)` conservait l'ancien `updatedAt`, forçant le remote à ignorer le changement. Corrigé via `copy(updatedAt = LocalDateTime.now())` dans le repository.
- **Bouton checklist dans AddTask non fonctionnel** — le `onClick` était vide. Implémenté : éditeur inline de checklist dans la fiche de création de tâche.

### Ajouté

- **Sections collapsibles** — tap sur le titre d'une section pour replier/déplier ses tâches. Chevron animé.
- **Sélecteur de section dans TaskDetail** — nouvelle ligne "Section" dans les propriétés d'une tâche (si le projet a des sections).
- **Masquer/afficher les tâches terminées** — nouveau menu "Masquer terminées / Afficher terminées" dans le `...` du projet.

### Amélioré

- **Commentaires triés du plus récent au plus ancien** (DESC).
- **Saisie de commentaire + clavier** — `imePadding()` sur la LazyColumn + scroll automatique vers le bas à chaque nouveau commentaire.
- **Tri auto — tâches terminées en bas** — le comparateur de tri automatique place désormais les tâches `isCompleted` en fin de section.

---

## [0.31.0] — 2026-04-20 · Tags Manager + Bug Fixes

### Ajouté

- **Tags Manager** — nouvel écran accessible via Paramètres → Tags. Permet de créer, renommer, recolorer et supprimer des tags. Bouton "Remove unused" pour nettoyer les tags non utilisés d'un seul tap.
- **Système de couleur des tags** — police vive (colorHex pleine opacité), fond semi-transparent (12 % / 18 % dark), bordure légère (30 % / 45 % dark). Fonctionne en light et dark mode.
- **TagChip cohérent** — tous les affichages de tags (TaskItem, TagsPickerModal, TagsScreen) utilisent le même composant `TagChip` avec le nouveau système de couleur.
- **Log persistant** — les logs sont maintenant écrits dans `filesDir/logs/app.log` (max 1 Mo) ; les crashs en arrière-plan sont conservés après redémarrage.

### Corrigé

- **Suppression de section → tâches orphelines** — supprimer une section ne supprimait que la section (les tâches gardaient leur `projectId` et réapparaissaient dans Anytime/Someday). Corrigé : les tâches sont maintenant soft-deleted avec la section.
- **Statut "Won't do" non synchronisé** — la requête `updateStatus` n'incluait pas `updatedAt`, la sync ignorait le changement. Corrigé.
- **Menu "..." de section à gauche** — le `DropdownMenu` était ancré au coin supérieur gauche de la Row complète. Corrigé via un slot `menuContent` wrappant uniquement le bouton.
- **Bouton "Today" / "Someday" sans effet** — dans `AddTaskSheet`, `onDateSelected` ne mettait pas à jour `group`. "Someday" appelait `onDismiss()` sans `fireResult`. Les deux corrigés.
- **Ouvrir une tâche immédiatement après création** — ajout d'un bouton `OpenInFull` dans `AddTaskSheet` : sauvegarde la tâche et navigue directement vers son détail.

---

## [0.25.0] — 2026-04-16 · UI Bug Fixes — Dark Mode + QuickFind Date Search

### Corrigé

- **Tag badges (dark mode)** — les badges de tags utilisaient `isSystemInDarkTheme()` directement, ignorant le réglage Light/Dark de l'app. Corrigé via `FlowColors.tagChipBg/Fg` injecté par le système de thème.
- **Badges de date `[Today]` / `[5d]` (dark mode)** — couleurs de fond hardcodées en clair (ex. `0xFFF0F4F8`) rendues illisibles en dark mode. Ajout de variantes sombres par état (`DateBadgeOverdueDarkBg/Fg`, `DateBadgeTodayDarkBg/Fg`, `DateBadgeFutureDarkBg/Fg`) dans `FlowColors`.
- **Texte heure événement (dark mode)** — la couleur hex brute du calendrier (ex. bleu marine) appliquée au texte `"21:00-23:59"` devenait illisible en fond sombre. Corrigé avec `lerp(indicatorColor, Color.White, 0.45f)` en dark mode ; la barre colorée garde la couleur d'origine.
- **QuickFind — recherche par date `"17 Apr"` toujours vide** — `LocalDate::from` échouait silencieusement faute de champ année dans le format. Corrigé en extrayant `MONTH_OF_YEAR` + `DAY_OF_MONTH` via `ChronoField` puis en construisant `LocalDate.of(year, month, day)`.
- **QuickFind — `"Cette semaine"` / `"This week"` / `"Ce mois"` sans résultat** — ces mots-clés résolvaient vers une date unique (ex. le lundi), donc la requête SQL cherchait `dueDate = lundi` au lieu d'une plage. Corrigé avec une sealed class `DateQuery` (Single / Range) et une nouvelle méthode `searchByDateRange(start, end)` dans le DAO et le repository.
- **QuickFind — frappe anticipée non supportée** — il fallait saisir le mot en entier pour voir les résultats. Ajout du prefix-matching sur les mots-clés relatifs (min. 2 caractères) : `"auj"` → aujourd'hui, `"dem"` → demain, `"cette"` → cette semaine, `"ce m"` → ce mois, etc.
- **Navigation — retour depuis Today sans confirmation** — appuyer sur Retour depuis l'écran racine quittait l'app silencieusement. Ajout d'un `BackHandler` avec dialog de confirmation `"Quitter OnlyFlows ?"`.

### Ajouté

- **`DateQuery` (sealed class)** — `Single(date)` pour les dates ponctuelles, `Range(start, end)` pour les plages (semaine, mois, semaine N).
- **`TaskDao.searchByDateRange` / `searchByDateRangeWithItems`** — nouvelle requête Room `dueDate >= :start AND dueDate <= :end OR startDate >= :start AND startDate <= :end`.
- **`TaskRepository.searchByDateRange`** — nouvelle méthode dans l'interface et l'implémentation.
- **QuickFind header de plage** — affiche `"16 Apr – 22 Apr 2026"` avec sous-titre `"Show all tasks in this period"` pour les résultats de semaine/mois.
- **Mots-clés FR/EN supplémentaires** — `hier` / `yesterday`, `ce mois-ci`, prefix-matching sur tous les mots-clés relatifs.

---

## [0.24.0] — 2026-04-16 · LAN Sync — Device Authorization

### Ajouté

- **Whitelist d'appareils de confiance** — chaque appareil maintient une liste `trusted_device_ids` (DataStore JSON). Seuls les appareils explicitement autorisés peuvent synchroniser des données.
- **`SyncPreferences`** — nouveaux champs `trustedDeviceIds: Flow<Set<String>>` + méthodes `addTrustedDevice()`, `removeTrustedDevice()`, `isTrusted()`.
- **`SyncServer`** — intercepteur sur toutes les routes sauf `/ping` : vérifie l'en-tête `X-Device-Id` dans la whitelist, retourne **HTTP 403** si absent ou non autorisé. La découverte (ping) reste publique.
- **`SyncClient`** — envoie `X-Device-Id: <thisDeviceId>` sur chaque requête via `defaultRequest`.
- **`SyncRepositoryImpl`** — `syncWithPeer()` abandonne immédiatement si le pair n'est pas dans la whitelist.
- **`SyncOrchestrator`** — la synchronisation automatique (découverte + périodique 30 s) ne déclenche que pour les pairs de confiance.
- **UI (`SyncSettingsScreen`)** — liste des appareils mise à jour :
  - Pair non autorisé : icône cadenas ouvert (gris) + bouton **"Autoriser"**
  - Pair autorisé : icône cadenas fermé (vert) + bouton **"Sync"** + bouton **"Révoquer"** (rouge)
  - En-tête : `"Appareils détectés (N) — X autorisé(s)"`
- **`SyncViewModel`** — `trustedDeviceIds: Set<String>` dans `SyncUiState` + actions `trustDevice()` / `untrustDevice()`.

---

## [0.23.0] — 2026-04-16 · LAN Sync with Conflict Resolution + Attachment File Sync

### Ajouté

- **Synchronisation LAN peer-to-peer** — découverte automatique via Android NSD (`_onlyflows._tcp`), serveur HTTP embarqué Ktor CIO (port 47120), synchronisation bidirectionnelle Last-Write-Wins basée sur `updatedAt`.
- **`updatedAt`** ajouté sur 8 entités Room : `ChecklistItem`, `Section`, `Tag`, `Folder`, `SavedLocation`, `Reminder`, `TimeEntry`, `Attachment` (+ `syncedFrom: String?` sur `Attachment`).
- **`SyncMetaEntity`** (`sync_meta`) — curseur de dernière synchronisation par pair.
- **`DeletedEntityEntity`** (`deleted_entities`) — registre des suppressions (tombstones) ; les suppressions propagées ne peuvent pas être ressuscitées.
- **`SyncDao`** — upserts, requêtes `changedSince`, `maxUpdatedAt`, gestion tombstones, `updateAttachmentUri`.
- **`SyncPreferences`** — `deviceId` (UUID stable), `deviceName`, `syncEnabled`, `syncPort`, `syncAttachments`, `maxAttachmentSizeMb`.
- **`LanDiscoveryManager`** — enregistre et découvre les services NSD ; expose `StateFlow<List<PeerInfo>>`.
- **`SyncServer`** — routes : `GET /ping`, `GET /manifest`, `POST /pull`, `GET /files/{id}` (streaming via ContentResolver).
- **`SyncClient`** — client Ktor CIO ; `ping`, `fetchManifest`, `pull`, `downloadFile`.
- **`ConflictResolver`** — LWW pur : tombstone > entité récente > entité ancienne.
- **`SyncRepositoryImpl`** — session complète : ping → manifest → tombstones → pull delta → résolution conflits → téléchargement pièces jointes → mise à jour curseur.
- **`AttachmentSyncer`** — télécharge les fichiers dans `getExternalFilesDir("attachments")/sync/`, respecte le toggle et la limite de taille.
- **`SyncOrchestrator`** — `SupervisorJob + Dispatchers.IO` ; sync au peer-discovery + périodique 30 s.
- **`SyncForegroundService`** — maintient le serveur Ktor actif en arrière-plan avec notification persistante.
- **`SyncSettingsScreen`** / **`SyncViewModel`** — toggle on/off, nom de l'appareil, statut sync, toggle pièces jointes, limite de taille, liste des pairs découverts.
- **DB version 23 → 24** (`fallbackToDestructiveMigration`).
- **Permissions** `AndroidManifest` : INTERNET, ACCESS_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, READ_MEDIA_*.
- **Dépendances** : Ktor 2.3.12 (server-cio, client-cio, content-negotiation, serialization-json), kotlinx-serialization-json 1.7.3, plugin `kotlin.serialization`.

---

## [0.22.1] — 2026-04-16 · Bug fixes (5)

### Corrigé

- **Bug 1 — Tag badges ignoraient le thème de l'app** : `Theme.kt` — ajout de `tagChipBg`, `tagChipFg` et `isDark` dans `FlowColors`, renseignés correctement dans les branches light/dark ; `TaskItem.kt` — `TagChip` lit désormais `MaterialTheme.flowColors.tagChipBg/Fg` au lieu d'appeler `isSystemInDarkTheme()` directement.
- **Bug 2 — Texte de l'heure illisible en mode sombre** : `AgendaCard.kt` — en mode sombre, la couleur du texte de l'heure est `lerp(indicatorColor, Color.White, 0.45f)` ; la barre de couleur garde la teinte d'origine, seul le texte est éclairci.
- **Bug 3 — La recherche "16 Apr" / "16 Avr" ne retournait aucun résultat** : `QuickFindViewModel.kt` — correction de la logique de capitalisation : chaque token alphabétique est capitalisé individuellement ("16 apr" → "16 Apr") au lieu de mettre en majuscule le premier caractère de la chaîne entière (qui était un chiffre, donc rien ne changeait).
- **Bug 4 — Dates relatives en langage naturel non reconnues** : `QuickFindViewModel.kt` — détection de mots-clés au début de `tryParseDate()` : `aujourd'hui`/`today`, `demain`/`tomorrow`, `hier`/`yesterday`, `cette semaine`/`this week`, `ce mois`/`this month`.
- **Bug 5 — Retour sur Today quittait l'app silencieusement** : `TodayScreen.kt` — ajout d'un second `BackHandler(enabled = true)` affichant un `AlertDialog("Quitter OnlyFlows ?")` ; le premier handler (désélection) garde la priorité car il est enregistré en premier.

---

## [0.22.0] — 2026-04-15 · Deprecation fixes + Performance pass

### Modifié

- **`@Immutable`** ajouté sur tous les modèles domaine (`Task`, `Project`, `Section`, `Tag`, `Reminder`, `ChecklistItem`, `Folder`, `Comment`, `Attachment`, `TimeEntry`, `Note`, `AgendaEvent`, `CalendarAccount`, `RecurrenceRule`, `SavedLocation`, `ActionLog`, `StatusLogEntry`, `FilterPreset`, `ProjectTaskProgress`) → Compose peut maintenant sauter les recompositions inutiles.
- **`SimpleDateFormat` / `java.util.Date` supprimés** dans `BackupManager` et `BackupSettingsScreen` → remplacés par `DateTimeFormatter` / `LocalDateTime` / `Instant` (`java.time.*`), thread-safe.
- **Filtres mémoïsés** dans `AnytimeScreen`, `TodayScreen`, `InboxScreen` — les blocs `state.tasks.filter{}` sont maintenant dans `remember(...)` et ne recalculent que si leurs entrées changent.
- **Boucle ticker infinie corrigée** (`ProjectDetailViewModel`) — le `while(true)` tournait en permanence même sans timer actif (fuite CPU/mémoire) ; remplacé par un `collectLatest` sur `activeProjectEntry` qui ne tick que pendant l'exécution d'un chrono.
- **`distinctUntilChanged()`** ajouté sur les chaînes `combine(...)` dans `TodayViewModel`, `InboxViewModel`, `ProjectsViewModel` → évite les mises à jour d'état redondantes.
- **Indices Room** ajoutés sur `tasks` (`projectId`, `scheduledGroup`, `dueDate`, `isCompleted`, `deletedAt`, indices composites) → requêtes filtrées 10–100× plus rapides ; DB version 22 → **23**.
- **Requêtes N+1 éliminées** (`TaskRepositoryImpl`) — les 14 patterns `flatMapLatest + combine` (1 requête par tâche pour ses items de checklist) remplacés par des méthodes `@Transaction @Relation` Room → 50 tâches = 2 requêtes au lieu de 51.
- **Fan-out de flows par projet supprimé** (`ProjectsViewModel`) — les `2 × N` observers live (un `COUNT` complété + un `COUNT` total par projet) remplacés par une seule requête agrégée `GROUP BY projectId` → 100+ observers → 1 pour 50 projets.

---

## [0.21.0] — 2026-04-15 · Location-based reminders (GPS offline)

### Ajouté

- **`SavedLocationEntity`** — entité Room `saved_locations` (`id`, `label`, `latitude`, `longitude`, `radiusMeters`).
- **`SavedLocationDao`** — `observeAll`, `getAll`, `getById`, `insert`, `delete`.
- **`SavedLocationRepository`** + **`SavedLocationRepositoryImpl`** — couche domaine + implémentation.
- **`SavedLocation`** — modèle domaine dans `Models.kt`.
- **`GeofenceManager`** — wraps Android `GeofencingClient` (offline GPS) : `register(locationId, lat, lng, radius)` ENTER transition, `unregister`, `unregisterAll`.
- **`GeofenceReceiver`** — `BroadcastReceiver` qui reçoit les événements geofence et poste une notification de rappel pour chaque reminder lié à la location.
- **`LocationsSettingsScreen`** — nouvel écran `settings/locations` : liste des locations sauvegardées, FAB → dialog d'ajout (label + lat/lng + rayon), suppression avec désenregistrement du geofence.
- **Permissions** — `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` dans `AndroidManifest.xml` ; flow de demande de permission dans `LocationsSettingsScreen`.
- **Entrée "Locations"** dans `SettingsScreen` (icône `LocationOn`).
- **Location picker dans `ReminderPickerSheet`** — dropdown des locations sauvegardées (affiché uniquement si ≥ 1 location existe) ; sélectionner une location enregistre un geofence au moment de la sauvegarde du rappel.
- Route `Screen.LocationsSettings` câblée dans `AppNavigation`.

### Modifié

- **`ReminderEntity`** — champ `locationId: String? = null` ajouté ; `toDomain()` / `toEntity()` mis à jour.
- **`Reminder`** — champ `locationId: String? = null` ajouté.
- **`TaskDetailEvent.AddReminder`** — paramètre `locationId: String? = null` ajouté.
- **`TaskDetailViewModel`** — `locationRepo` + `geofenceManager` injectés ; enregistre le geofence à l'ajout du rappel si `locationId` renseigné.
- **`BootReceiver`** — ré-enregistre les geofences des reminders avec `locationId` au démarrage.
- **`AppContainer`** — `savedLocationRepository`, `geofenceManager` ajoutés ; dépendance Gradle `play-services-location:21.3.0`.
- **`AppDatabase`** — version 21 → **22** (`SavedLocationEntity` + `savedLocationDao()`).

---

## [0.20.0] — 2026-04-15 · Logbook étendu — Journal d'actions

### Ajouté

- **`ActionLogEntity`** — entité Room `action_log` (`id`, `actionType`, `entityId`, `entityType`, `label`, `extra`, `occurredAt`).
- **`ActionLogDao`** — `insert`, `observeAll`, `observeByEntity`, `deleteOlderThan`.
- **`ActionLogRepository`** + **`ActionLogRepositoryImpl`** — couche domaine + implémentation.
- **`ActionType`** enum — `TASK_CREATED`, `TASK_COMPLETED`, `TASK_UNCOMPLETED`, `TASK_ARCHIVED`, `TASK_DELETED`, `TASK_RESTORED`, `TIMER_STARTED`, `TIMER_STOPPED`, `PROJECT_STATUS_CHANGED`, `COMMENT_ADDED`.
- **`ActionLog`** — modèle domaine dans `Models.kt`.
- **Instrumentation des repositories** — chaque action significative écrit une entrée dans `action_log` :
  - `TaskRepositoryImpl` : create, complete/uncomplete, archive/restore, soft-delete, restore from trash.
  - `TimeEntryRepositoryImpl` : timer start, timer stop (avec durée en `extra`).
  - `ProjectRepositoryImpl` : changement de statut.
  - `CommentRepositoryImpl` : commentaire ajouté.
- **Onglet "Journal" dans `LogbookScreen`** — `TabRow` avec deux onglets : **Archived** (existant, inchangé) + **Journal** (log chronologique groupé par jour, icône+libellé par type d'action).

### Modifié

- **`LogbookUiState`** — champs `journalEntries: List<ActionLog>`, `selectedTab: Int` ajoutés.
- **`LogbookViewModel`** — collecte `actionLogRepo.observeAll()` en plus des archives ; `selectTab()`.
- **`AppContainer`** — `actionLogRepository` instancié ; injecté dans `TaskRepositoryImpl`, `TimeEntryRepositoryImpl`, `ProjectRepositoryImpl`, `CommentRepositoryImpl`.
- **`AppDatabase`** — version 20 → **21** (`ActionLogEntity` + `actionLogDao()`).

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
