# OnlyFlows — Syncthing Sync System

## Overview

OnlyFlows syncs data between devices using [Syncthing](https://syncthing.net/) as the transport layer. Syncthing handles Wi-Fi discovery, file transfer, and relay. OnlyFlows handles reading/writing JSON files and keeping Room in sync.

```
Device A                              Device B
┌──────────────────┐                 ┌──────────────────┐
│   OnlyFlows      │                 │   OnlyFlows      │
│  FileObserver    │                 │  FileObserver    │
│  ↕ JSON R/W      │                 │  ↕ JSON R/W      │
│ /chosen-folder/  │◄── Syncthing ──►│ /chosen-folder/  │
└──────────────────┘                 └──────────────────┘
```

No cloud, no account, no server. Everything stays on your devices.

---

## Setup

1. Install [Syncthing-Fork](https://github.com/Catfriend1/syncthing-android) on both devices
2. Configure a shared folder in Syncthing on both devices (same folder ID)
3. In OnlyFlows → Settings → Syncthing → pick the same folder
4. OnlyFlows will export all existing data to the folder on first setup

---

## Folder Structure

```
/chosen-folder/
  tasks/                 <uuid>.json
  projects/              <uuid>.json
  sections/              <uuid>.json
  checklist_items/       <uuid>.json
  tags/                  <uuid>.json
  folders/               <uuid>.json
  notes/                 <uuid>.json
  comments/              <uuid>.json
  reminders/             <uuid>.json
  time_entries/          <uuid>.json
  saved_locations/       <uuid>.json
  goals/                 <uuid>.json
  goal_logs/             <uuid>.json
  attachments/
    metadata/            <uuid>.json    ← attachment metadata
    files/               <uuid>.<ext>   ← physical file
  _meta/
    tombstones.json                     ← delete log [{entityType, entityId, deletedAt}]
    device.json                         ← this device info {deviceId, deviceName, appVersion}
```

Each `<uuid>.json` contains all Room entity fields serialized to JSON, with `updatedAt` in ISO-8601.

---

## Write Flow (Room → Disk)

Every `insert` / `update` in a repository immediately writes the corresponding JSON file via `SyncthingWriter` using the Android SAF (Storage Access Framework) `DocumentFile` API.

Every `delete` appends a tombstone entry to `_meta/tombstones.json`.

Attachment files are copied from their `content://` URI to `attachments/files/<uuid>.<ext>`.

---

## Read Flow (Disk → Room)

### 1. FileObserver (real-time)

`SyncthingCoordinator` runs a `FileObserver` on the Syncthing folder (primary external storage only; SAF URIs are not watchable). Events: `CLOSE_WRITE`, `MOVED_TO`, `CREATE`, `DELETE`.

On each event:
1. Skip `.sync-conflict-*` files — handled by `ConflictResolver`
2. Parse JSON → compare `updatedAt` with `SyncDao.getXxxUpdatedAt(id)`
3. If file is newer → `SyncDao.upsertXxx(entity)`
4. After each scan → apply tombstones from `_meta/tombstones.json`

The observer starts when `SyncthingStatus` becomes `Ready` and stops otherwise.

### 2. WorkManager fallback (every 15 min)

`SyncthingImportWorker` runs a full folder scan every 15 minutes. This covers cases where `FileObserver` missed an event (background kill, reboot, SD card).

### 3. Force Sync

Settings → Syncthing → "Force Sync Now" triggers an immediate full scan + conflict resolution.

---

## Conflict Resolution

### Syncthing conflict files

When two devices edit the same entity before Syncthing can sync, Syncthing creates:
```
<uuid>.sync-conflict-20260419-123456-DEVICEID.json
```

`ConflictResolver.resolveAll()` handles these:
1. Find original file (`<uuid>.json`) and conflict file
2. Compare `updatedAt` field
3. The newer version wins → overwrite `<uuid>.json`
4. Delete the conflict file
5. Upsert winner into Room

**Tiebreaker:** If `updatedAt` is identical, compare `deviceId` alphabetically (deterministic, no infinite loop).

### Delete vs. modify

If device A deletes an entity (tombstone) and device B modifies it:
- Compare `tombstone.deletedAt` vs `entity.updatedAt`
- The more recent operation wins
- If modify > delete → entity is kept (or resurrected)
- If delete > modify → entity is deleted

---

## Status States

| Status | Meaning |
|---|---|
| `FolderNotSet` | No folder configured |
| `NotInstalled` | Syncthing APK not found |
| `NotRunning` | Syncthing installed but not active |
| `NoWifi` | Wi-Fi disconnected |
| `WaitingToSync` | Folder set, waiting for Syncthing to be ready |
| `Importing` | Currently reading files and updating Room |
| `Ready` | Synced and up to date |
| `Error(msg)` | Import or conflict resolution failed |

---

## Classes

| Class | Role |
|---|---|
| `SyncthingPreferences` | DataStore: folder URI, deviceId, lastScannedAt, enabled |
| `SyncthingWriter` | Writes JSON files to folder via SAF DocumentFile |
| `SyncthingImporter` | Full folder scan → upserts Room |
| `SyncthingExporter` | One-time bulk export of all Room entities on first setup |
| `ConflictResolver` | Resolves `.sync-conflict-*` files, Last-Write-Wins |
| `SyncthingStatusMonitor` | Observes Wi-Fi + Syncthing package → emits `SyncthingStatus` |
| `SyncthingCoordinator` | FileObserver lifecycle, debounce (3s), starts `triggerImport()` |
| `SyncthingImportWorker` | WorkManager periodic worker (15 min) |
| `EntityJson.kt` | `toJson()` / `fromJson()` for all 14 entity types |
| `SyncDao` | Room DAO: upsert/getUpdatedAt/delete/getAll for every entity |

---

## Limitations

- FileObserver only works on primary external storage (`/sdcard/`). On SD card paths, the app falls back to one immediate SAF-based import and relies on WorkManager for subsequent updates.
- Syncthing must be installed separately (not bundled). Recommended: [Syncthing-Fork](https://github.com/Catfriend1/syncthing-android).
- Attachment files sync only if both the metadata JSON and the physical file are present in the folder.
