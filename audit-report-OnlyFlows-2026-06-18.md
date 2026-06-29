# RAPPORT D'AUDIT TECHNIQUE — OnlyFlows
**Date :** 2026-06-18  
**Version auditée :** 0.55.5 (versionCode 80) — DB v31  
**Auditeur :** Claude Code (lecture seule — aucune modification)

---

## 1. RÉSUMÉ EXÉCUTIF

OnlyFlows est une application Android mature (Kotlin 2.1.0, Jetpack Compose, Room v31, minSdk 26) qui suit une architecture Clean Architecture / UDF cohérente et bien tenue. Le codebase est propre : zéro secret en dur, zéro `TODO` oublié, zéro `runBlocking`, zéro suppression d'avertissement abusive. Les patterns de synchronisation (Syncthing, last-write-wins, tombstones) sont correctement implémentés.

**Principaux risques avant une mise à jour majeure :**

1. **Couverture de tests = 0 %** — aucun filet de sécurité pour valider les régressions lors d'un refactor majeur.
2. **CI/CD inexistant** — la chaîne de livraison est entièrement manuelle, sans gate automatique (lint, build, scan).
3. **`TaskDetailViewModel` de 627 lignes** — monolithe UI-logic difficile à refactoriser sans risque de régression.
4. **`doc.md` daté d'avril 2026 pour une DB v20** — documentation technique partiellement obsolète (DB actuelle v31).
5. **Boucles `forEach` sur des writes en base + Syncthing** — plusieurs points de performance à surveiller lors d'un scaling de données.
6. Globalement, l'app est **prête pour une MINOR**, mais une **MAJOR** nécessite au minimum l'ajout de tests avant d'être sécurisée.

---

## 2. TABLEAU DE SYNTHÈSE

| Domaine | Sévérité max | Nb problèmes | Risque pour mise à jour majeure |
|---|---|---|---|
| 1. Architecture & Design | MOYENNE | 2 | Moyen — VM trop gros, DI manuel non extensible |
| 2. Qualité de code | MOYENNE | 3 | Moyen — VM 627 lignes, boucles forEach, mix logs |
| 3. Sécurité | FAIBLE | 2 | Faible — receivers exportés, Log.d dans ReminderScheduler |
| 4. Performance | MOYENNE | 3 | Moyen — forEach writes, pas d'index sur ProjectEntity |
| 5. Base de données | FAIBLE | 3 | Faible — migrations OK, quelques absences d'index |
| 6. API & Intégrations | INFO | 1 | Faible — dépendance Syncthing externe |
| 7. Tests & QA | CRITIQUE | 1 | CRITIQUE — couverture 0 % |
| 8. Infrastructure / CI/CD | ÉLEVÉE | 1 | Élevé — aucun pipeline automatisé |
| 9. Dépendances & Supply Chain | MOYENNE | 2 | Moyen — Room 2.6.1, Ktor 2.3.12 à vérifier |
| 10. Logging & Observabilité | FAIBLE | 1 | Faible — incohérence AppLog vs Log.d |
| 11. Documentation | MOYENNE | 2 | Moyen — doc.md obsolète, Todo.md stale |
| 12. Dette technique | MOYENNE | 3 | Moyen — VM monolithe, DI manuel, 0 tests |

---

## 3. DÉTAIL PAR DOMAINE

---

### 3.1 Architecture & Design

#### [A1] ViewModel monolithe — MOYENNE
- **Fichier :** `app/src/main/java/com/onlyflows/ui/screens/task/TaskDetailViewModel.kt` — 627 lignes
- **Description :** `TaskDetailViewModel` contient 25+ variantes d'events, un timer interne, la logique de recurrence, et gère les dépendances entre tâches. Sa taille rend toute modification risquée (couplage fort entre les domaines task/time/recurrence/checklist dans un seul fichier).
- **Impact :** Lors d'un refactor majeur impliquant la couche domain ou les modèles de données, une régression ici peut impacter de nombreuses fonctionnalités silencieusement (0 test).
- **Recommandation :** Fragmenter en sous-ViewModels ou delegates : `TaskTimerDelegate`, `TaskRecurrenceDelegate`, `ChecklistDelegate`. Applicable uniquement avant/pendant une MAJOR.

#### [A2] DI manuel non scalable — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/AppContainer.kt` — 55 lignes
- **Description :** `AppContainer` est un constructeur manuel ordonné. Chaque nouveau repository exige un ajout en respectant l'ordre de déclaration (Syncthing en premier). Ce pattern casse facilement si un développeur ne connaît pas la règle implicite.
- **Impact :** Pas bloquant pour l'app actuelle mais fragile à l'onboarding ou à l'ajout de dépendances croisées.
- **Recommandation :** Documenter l'ordre dans `AppContainer.kt` via un commentaire de bloc unique (règle d'ordre Syncthing-first) — ou migrer vers Hilt à l'occasion d'une MAJOR.

---

### 3.2 Qualité de code

#### [Q1] TaskDetailViewModel — 627 lignes — MOYENNE
*(voir [A1] ci-dessus)*

#### [Q2] Boucles `forEach` sur writes Room + Syncthing — MOYENNE
- **Fichier 1 :** `app/src/main/java/com/onlyflows/data/repository/TaskRepositoryImpl.kt` — ligne 261  
  `newItems.forEach { syncthingWriter?.writeChecklistItem(it) }` — N writes séquentiels pour N checklist items.
- **Fichier 2 :** `app/src/main/java/com/onlyflows/data/repository/ProjectRepositoryImpl.kt` — lignes 122 et 132  
  Double boucle `forEach` sur sections : 1× DAO insert, 1× Syncthing write.
- **Impact :** Performance dégradée si checklist > 50 items ou projet avec > 20 sections. Chaque itération déclenche une coroutine I/O.
- **Recommandation :** À vérifier : existe-t-il un `insertAll()` en DAO ? Si oui, utiliser `insertAll(items)` + une seule sérialisation JSON. À traiter avant scaling de données.

#### [Q3] Incohérence de logging : `Log.d` vs `AppLog.d` — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/data/notifications/ReminderScheduler.kt` — lignes 33, 42, 45, 49, 53
- **Description :** `ReminderScheduler` utilise le `Log.d` système (visible dans Logcat prod) au lieu du `AppLog` personnalisé (buffer en mémoire + fichier rotatif). Tous les autres modules utilisent `AppLog`.
- **Impact :** Logs de scheduling perdus dans l'in-app log viewer ; impossible de déboguer les problèmes de reminder en prod depuis l'écran Logs de l'app.
- **Recommandation :** Remplacer `Log.d/e/w` par `AppLog.d/e/w` dans `ReminderScheduler.kt`.

---

### 3.3 Sécurité

#### [S1] Aucun secret en dur — INFO ✅
- Grep négatif sur `apiKey`, `password`, `secret`, `Bearer`, `token`, `API_KEY` dans tout le code.
- `local.properties` contient uniquement `sdk.dir` (normal, gitignored).
- `BuildConfig = true` mais aucun champ sensible injecté.
- L'app est local-first : pas de backend cloud, pas de clé d'API réseau.

#### [S2] Receivers exportés sans permission explicite — FAIBLE
- **Fichier :** `app/src/main/AndroidManifest.xml` — lignes correspondant à `BootReceiver` (exported=true) et `GeofenceReceiver` (exported=true).
- **Description :** `BootReceiver` est `exported=true` sans `android:permission` déclarée. Sur Android 12+ (API 31+), les receivers broadcast doivent déclarer explicitement `exported` — c'est fait ici — mais `BootReceiver` sans permission de protection expose potentiellement une surface d'attaque si un tiers envoie une intent simulant `BOOT_COMPLETED`.
- **Impact :** Faible en pratique (BOOT_COMPLETED est protégé système), mais non conforme aux bonnes pratiques Android.
- **Recommandation :** Ajouter `android:permission="android.permission.RECEIVE_BOOT_COMPLETED"` sur le receiver.

#### [S3] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — INFO
- **Fichier :** `app/src/main/AndroidManifest.xml`
- **Description :** Cette permission permet à l'app de demander l'exemption Doze. Google Play peut la rejeter lors d'une soumission si non justifiée dans la déclaration de politique.
- **Impact :** Risque de rejection Play Store si l'usage n'est pas documenté dans la politique de confidentialité ou la fiche Play.
- **Recommandation :** Vérifier que le Play Store listing inclut la justification (reminders géoloc + alarmes exactes).

---

### 3.4 Performance

#### [P1] Boucles forEach sur I/O — MOYENNE
*(voir [Q2] ci-dessus)*

#### [P2] Absence d'index sur `ProjectEntity` — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/data/local/entity/ProjectEntity.kt` — 69 lignes
- **Description :** Aucun `@Index` déclaré sur `isArchived` ou `folderId`. Les queries `ProjectDao` filtrent systématiquement sur `isArchived = 0`. Sur une base avec > 1000 projets, cela génère un full scan.
- **Impact :** Faible sur usage mobile typique (< 200 projets), mais à surveiller pour des power users.
- **Recommandation :** Ajouter `indices = [Index("isArchived"), Index("folderId")]` à l'annotation `@Entity` de `ProjectEntity` + bumper DB version.

#### [P3] `createdAt`/`updatedAt` stockés en String dans `ProjectEntity` — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/data/local/entity/ProjectEntity.kt` — lignes 27-28
- **Description :** Champs temporels stockés en `String` alors que `Converters.kt` gère `LocalDateTime ↔ String`. Le tri ou filtrage par date sur ces champs est lexicographique (correct si format ISO, mais non indexable efficacement par SQLite).
- **Impact :** Faible si le format ISO est respecté partout. Risque d'incohérence si le format change.
- **Recommandation :** Uniformiser avec `LocalDateTime` (déjà utilisé dans `TaskEntity`). Migration simple.

---

### 3.5 Base de données / Données

#### [D1] Migrations explicites présentes — INFO ✅
- 4 migrations versionnées dans `AppDatabase.kt` lignes 95–144 : M27→28, M28→29, M29→30, M30→31.
- `exportSchema = true` : schéma JSON exporté, traçable dans git.
- `fallbackToDestructiveMigration()` présent — intentionnel pour dev, mais à surveiller si des beta-testeurs utilisent l'app (pertes de données sur migration manquante).

#### [D2] `fallbackToDestructiveMigration` en release — MOYENNE
- **Fichier :** `app/src/main/java/com/onlyflows/data/local/AppDatabase.kt` — ligne ~150
- **Description :** Si une migration est oubliée entre deux releases (comme ce fut le cas entre v0.55.4 et v0.55.5, corrigé en v0.55.5), les utilisateurs dont la DB saute la version verront leurs données effacées silencieusement.
- **Impact :** Critique pour la réputation utilisateur si cela arrive en production.
- **Recommandation :** Pour une MAJOR qui bumpe significativement la version DB, envisager de retirer `fallbackToDestructiveMigration` et d'écrire des migrations complètes, ou au minimum d'ajouter un écran d'alerte utilisateur si une migration destructive est déclenchée.

#### [D3] Seed callback actif en release — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/data/local/AppDatabase.kt` — lignes 150–186
- **Description :** `SeedCallback` injecte des données de mock au premier lancement. S'il est actif en release, les nouveaux utilisateurs voient des données fictives.
- **À vérifier :** Si `SeedCallback` est conditionnel à `BuildConfig.DEBUG`. Si non, c'est un problème en production.

---

### 3.6 API & Intégrations

#### [I1] Dépendance forte à Syncthing externe — INFO
- **Fichier :** `app/src/main/AndroidManifest.xml` — section `<queries>` : packages `com.github.catfriend1.syncthingandroid`, `com.nutomic.syncthingandroid`
- **Description :** La sync nécessite une app Syncthing tierce installée. Le fallback WorkManager est prévu (`SyncthingCoordinator.kt` lignes 54–76) mais la feature principale est non-fonctionnelle sans l'app tierce.
- **Impact :** User experience dégradée si Syncthing n'est pas installé — à documenter clairement dans l'UI onboarding.
- **Recommandation :** INFO uniquement. Déjà géré par le fallback. Documenter dans les settings l'exigence Syncthing.

#### [I2] `FileObserver` deprecated — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/data/syncthing/SyncthingCoordinator.kt` — ligne 65
- **Description :** `FileObserver` est déprécié depuis API 29 (Android 10). La version single-argument est remplacée par `FileObserver(File, Int)`.
- **Impact :** Fonctionnel mais marqué deprecated dans les outils d'analyse statique. Sur Android 26+ (minSdk de l'app), l'ancienne API est encore présente.
- **Recommandation :** Migrer vers le constructeur non-deprecated `FileObserver(File)` lors du prochain refactor Syncthing.

---

### 3.7 Tests & Qualité Assurance

#### [T1] Couverture de tests = 0 % — CRITIQUE
- **Preuve :** Glob `**/*Test*.kt` + `**/test/**/*.kt` + `**/androidTest/**/*.kt` — aucun fichier trouvé.
- **Description :** L'application n'a aucun test unitaire, aucun test d'intégration, aucun test instrumenté. Zéro.
- **Impact pour la mise à jour majeure :** CRITIQUE. Tout refactor, toute migration de modèle, tout changement de logique métier (recurrence, soft delete, syncthing) sera effectué à l'aveugle. Il est impossible de garantir l'absence de régression sans filet.
- **Zones les plus critiques sans test :**
  - `RecurrenceScheduler.kt` (logique pure, idéale pour tests unitaires)
  - `ConflictResolver.kt` (LWW, cas de tiebreak)
  - `SyncthingImporter.kt` (parsing JSON + upserts)
  - `TaskRepositoryImpl.kt` (soft delete, recurrence, queries complexes)
- **Recommandation :** Avant toute MAJOR, écrire a minima des tests unitaires sur `RecurrenceScheduler` et `ConflictResolver` (fonctions pures sans dépendances Android). Utiliser `junit4` + `kotlinx-coroutines-test`. Effort estimé : 2–3 jours.

---

### 3.8 Infrastructure, CI/CD & Déploiement

#### [C1] Aucun pipeline CI/CD — ÉLEVÉE
- **Preuve :** Glob `**/.github/workflows/*.yml`, `**/.gitlab-ci.yml`, `**/Jenkinsfile` — aucun fichier trouvé.
- **Description :** Le processus de build et de release est entièrement manuel (build local, signature manuelle, copie APK vers dossier Sync). Aucun gate automatique : pas de lint forcé, pas de build de validation sur PR, pas de scan de sécurité des dépendances.
- **Impact :** Une régression de build ou une faille de sécurité dans une dépendance peut être shippée sans être détectée.
- **Recommandation :** Ajouter a minima un workflow GitHub Actions basique :
  ```yaml
  # .github/workflows/ci.yml
  on: [push, pull_request]
  jobs:
    build:
      steps:
        - ./gradlew lint assembleDebug
  ```
  Ajouter `./gradlew dependencyCheckAnalyze` (OWASP) pour les scans CVE.

#### [C2] Release entièrement manuelle — MOYENNE
- Le flow de release (tag git → `gh release` → copie APK) est documenté en mémoire projet mais absent du repo. Si un nouveau développeur rejoint, le processus n'est pas reproductible sans transmission orale.
- **Recommandation :** Ajouter un `RELEASING.md` (ou section dans `BUILD.md`) décrivant les étapes exactes.

---

### 3.9 Dépendances & Supply Chain

#### [Dep1] Room 2.6.1 — à vérifier — FAIBLE
- **Fichier :** `gradle/libs.versions.toml` — ligne 10
- **Description :** Room 2.6.1 est la dernière stable connue au moment de l'audit. Room 2.7.x (avec KMP support) est en route. À vérifier si des CVE existent sur 2.6.1.
- **Recommandation :** Exécuter `./gradlew dependencyUpdates` pour identifier les upgrades disponibles.

#### [Dep2] Ktor 2.3.12 — dernière version 2.x — FAIBLE
- **Fichier :** `gradle/libs.versions.toml`
- **Description :** Ktor 3.x est sorti. Ktor 2.x est en maintenance. La migration vers 3.x est un breaking change (packages renommés). À planifier pour une MAJOR.
- **Impact :** Si l'app évolue pour ajouter des features réseau, rester sur 2.x sera une dette croissante.
- **Recommandation :** Évaluer le périmètre d'usage de Ktor dans l'app (RSS parser ? autre ?) et planifier la migration 3.x lors de la prochaine MAJOR.

#### [Dep3] Compose BOM 2024.12.01 — INFO
- **Description :** Compose BOM de décembre 2024. La BOM 2025.x est disponible. Pas de CVE connue, mais des performances d'animation et de recomposition améliorées dans les versions récentes.
- **Recommandation :** Upgrade de BOM à l'occasion d'une MINOR — faible risque.

---

### 3.10 Logging & Observabilité

#### [L1] Double système de log — FAIBLE
- **Fichier :** `app/src/main/java/com/onlyflows/data/debug/AppLog.kt` (90 lignes, buffer 500 entrées, fichier rotatif 1 MB)  
  vs `app/src/main/java/com/onlyflows/data/notifications/ReminderScheduler.kt` — lignes 33, 42, 45, 49, 53 (`Log.d` système)
- **Description :** L'app dispose d'un système de log custom (`AppLog`) bien conçu (buffer mémoire + fichier rotatif + viewer in-app). Mais `ReminderScheduler` utilise le `Log` système Android directement, rendant ses logs invisibles dans le viewer in-app.
- **Recommandation :** Unifier sur `AppLog` dans tous les modules.

#### [L2] Niveau de logs — INFO ✅
- `AppLog` implémente correctement les niveaux d/i/w/e/v.
- Pas de spam détecté (MAX_ENTRIES=500, rotation 1 MB).
- Logs contextuels suffisants dans Syncthing (deviceId, timestamps).

---

### 3.11 Documentation

#### [Doc1] `doc.md` obsolète — MOYENNE
- **Fichier :** `doc.md` — daté 2026-04-14, décrit la DB à "v20"
- **Description :** La documentation technique principale (`doc.md`) est 11 versions DB en retard (v20 documentée vs v31 actuelle). Les sections sur les DAO methods et les entity fields ne reflètent pas les entités ajoutées depuis (GoalEntity, GoalLogEntity, DateTrackerEntity, RssFeedEntity, RssArticleEntity — ajoutées entre v20 et v31).
- **Impact :** Un développeur qui onboard avec `doc.md` aura une compréhension incorrecte du schéma.
- **Recommandation :** Mettre à jour `doc.md` ou le supprimer au profit de `CLAUDE.md` (déjà plus à jour). Effort : faible.

#### [Doc2] `Todo.md` stale — FAIBLE
- **Fichier :** `Todo.md` — 3 lignes
- **Description :** Contient `"[ ] Widget Android (résumé du jour)"` — feature déjà implémentée depuis v0.55+.
- **Recommandation :** Nettoyer ou supprimer `Todo.md` ; les features à venir sont probablement mieux trackées ailleurs.

#### [Doc3] Documentation positive — INFO ✅
- `README.md` (183 lignes) : complet, features listées, architecture décrite, tech stack tabulé.
- `docs/BUILD.md` (102 lignes) : requirements, commandes, signing config expliqués.
- `CLAUDE.md` (projet) : instructions Claude Code à jour, patterns documentés, gotchas listés. Excellente pratique.
- `CHANGELOG.md` : historique versionné depuis v0.50.0.

---

### 3.12 Dette technique & Risques pour la mise à jour majeure

#### Zones fragiles consolidées :

| Zone | Risque | Raison |
|---|---|---|
| `TaskDetailViewModel.kt` (627 lignes) | Élevé | Monolithe, 0 test, 25+ events |
| Système de tests (0%) | CRITIQUE | Pas de filet pour valider les régressions |
| `fallbackToDestructiveMigration` | Moyen | Risque perte données utilisateur si migration oubliée |
| `doc.md` obsolète | Faible | Onboarding incorrect |
| CI/CD absent | Élevé | Aucun gate automatique |
| `forEach` writes Syncthing | Faible | Perf à charge (acceptable aujourd'hui) |
| Ktor 2.x | Faible | Migration 3.x breaking si features réseau ajoutées |

---

## 4. PLAN D'ACTION PRIORISÉ

Actions ordonnées par criticité pour préparer une mise à jour majeure :

| Priorité | Action | Sévérité | Effort | Risque si non fait |
|---|---|---|---|---|
| 1 | **Écrire tests unitaires** sur `RecurrenceScheduler`, `ConflictResolver`, `SyncthingImporter` | CRITIQUE | Moyen (2–3j) | Régressions invisibles sur la logique métier centrale |
| 2 | **Vérifier `SeedCallback`** : conditionnel à `BuildConfig.DEBUG` ? | CRITIQUE (à vérifier) | Faible (1h) | Données de mock en production |
| 3 | **Ajouter CI GitHub Actions** : lint + assembleDebug sur chaque push | ÉLEVÉE | Faible (2h) | Regressions de build non détectées |
| 4 | **Fragmenter `TaskDetailViewModel`** avant refactor majeur | MOYENNE | Élevé (3–5j) | Couplage fort, refactor impossible à sécuriser sans tests |
| 5 | **Supprimer `fallbackToDestructiveMigration`** ou ajouter alerte utilisateur | MOYENNE | Moyen (1j) | Perte de données silencieuse sur migration manquante |
| 6 | **Unifier les logs** : remplacer `Log.d/e` par `AppLog` dans `ReminderScheduler` | FAIBLE | Faible (30min) | Debug impossible en prod sur les reminders |
| 7 | **Ajouter `@Index` sur `ProjectEntity`** (`isArchived`, `folderId`) | FAIBLE | Faible (30min + migration DB) | Perf dégradée à grande échelle |
| 8 | **Mettre à jour `doc.md`** (DB v20 → v31) ou le supprimer | MOYENNE | Faible (2h) | Onboarding incorrect |
| 9 | **Migrer `FileObserver`** vers constructeur non-deprecated | FAIBLE | Faible (30min) | Warning lint croissant |
| 10 | **Planifier migration Ktor 3.x** pour la prochaine MAJOR | FAIBLE | Moyen (1–2j) | Dette croissante si features réseau ajoutées |

---

## 5. RECOMMANDATION DE VERSIONING

### La mise à jour prévue : de quel type s'agit-il ?

Sans connaître le périmètre exact de la "mise à jour majeure" visée, voici la grille de décision selon semver strict :

| Type de changement | Bump recommandé |
|---|---|
| Nouvelle feature, nouveaux écrans, nouveaux modèles (non-breaking pour les données existantes) | **MINOR** (0.55.x → 0.56.0) |
| Refactor architecture, changement de DI, restructuration des packages, breaking change de DB sans migration | **MAJOR** (0.x.x → 1.0.0) |
| Bugfix, tweak UI, optimisation | **PATCH** (0.55.5 → 0.55.6) |

### Recommandation :

Si la "mise à jour majeure" implique un **refactor de `TaskDetailViewModel`**, une **migration vers Hilt**, ou un **changement de schéma DB sans migration complète**, c'est une **MAJOR** → bump vers **1.0.0**.

Si c'est l'ajout de features significatives (nouveaux écrans, nouvelles entités avec migrations) sans breaking change, c'est une **MINOR** → bump vers **0.56.0**.

**Avant toute MAJOR : l'action prioritaire absolue est l'ajout de tests unitaires sur la logique métier (priorité 1 du plan ci-dessus). Sans eux, une MAJOR est une opération à haut risque.**

---

*Rapport généré le 2026-06-18. Aucune modification de code effectuée dans le cadre de cet audit.*
