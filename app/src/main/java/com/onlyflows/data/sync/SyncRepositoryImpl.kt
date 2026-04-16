package com.onlyflows.data.sync

import android.util.Log
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.DeletedEntityEntity
import com.onlyflows.data.local.entity.SyncMetaEntity
import com.onlyflows.data.preferences.SyncPreferences
import com.onlyflows.data.sync.network.*
import com.onlyflows.domain.sync.ConflictResolver
import com.onlyflows.domain.sync.PeerInfo
import com.onlyflows.domain.sync.SyncRepository
import com.onlyflows.domain.sync.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

private const val TAG = "SyncRepo"
private val EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0, 0)

class SyncRepositoryImpl(
    private val syncDao: SyncDao,
    private val syncPreferences: SyncPreferences,
    private val discoveryManager: LanDiscoveryManager,
    private val syncServer: SyncServer,
    private val syncClient: SyncClient,
    private val conflictResolver: ConflictResolver,
    private val attachmentSyncer: AttachmentSyncer,
) : SyncRepository {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    override fun observePeers(): StateFlow<List<PeerInfo>> = discoveryManager.peers
    override fun observeSyncState(): StateFlow<SyncState> = _syncState.asStateFlow()
    override fun observeTrustedDeviceIds(): Flow<Set<String>> = syncPreferences.trustedDeviceIds
    override suspend fun trustDevice(deviceId: String) = syncPreferences.addTrustedDevice(deviceId)
    override suspend fun untrustDevice(deviceId: String) = syncPreferences.removeTrustedDevice(deviceId)

    override suspend fun startServer() {
        val deviceId = syncPreferences.getDeviceIdNow()
        val deviceName = syncPreferences.deviceName.first()
        val port = syncPreferences.syncPort.first()
        syncServer.start()
        discoveryManager.start(deviceId, deviceName, port)
        _syncState.value = SyncState.Discovering
    }

    override suspend fun stopServer() {
        syncServer.stop()
        discoveryManager.stop()
        _syncState.value = SyncState.Idle
    }

    override suspend fun syncWithPeer(peer: PeerInfo) {
        if (!syncPreferences.isTrusted(peer.deviceId)) {
            Log.d(TAG, "Skipping sync with untrusted peer ${peer.displayName} (${peer.deviceId})")
            return
        }
        val thisDeviceId = syncPreferences.getDeviceIdNow()
        _syncState.value = SyncState.Syncing(peer.displayName)
        var conflictsSkipped = 0

        try {
            // 1. Ping to confirm peer is compatible and get real deviceId
            val ping = syncClient.ping(peer)
            if (ping == null) {
                Log.w(TAG, "Peer ${peer.host} not reachable, skipping")
                _syncState.value = SyncState.Idle
                return
            }
            val confirmedPeer = peer.copy(deviceId = ping.deviceId, displayName = ping.displayName)
            discoveryManager.updatePeer(peer, confirmedPeer)

            // 2. Ensure sync meta for this peer
            val meta = syncDao.getSyncMeta(confirmedPeer.deviceId)
            val lastSyncedAt = meta?.lastSyncedAt ?: EPOCH
            if (meta == null) {
                syncDao.upsertSyncMeta(
                    SyncMetaEntity(confirmedPeer.deviceId, confirmedPeer.displayName, lastSyncedAt)
                )
            }

            // 3. Fetch manifest from peer
            val manifest = syncClient.fetchManifest(confirmedPeer) ?: run {
                Log.w(TAG, "Failed to fetch manifest from ${confirmedPeer.host}")
                _syncState.value = SyncState.Idle
                return
            }

            // 4. Apply remote tombstones
            for (tomb in manifest.tombstones) {
                val deletedAt = runCatching { LocalDateTime.parse(tomb.deletedAt) }.getOrDefault(LocalDateTime.now())
                applyTombstone(tomb.entityType, tomb.entityId, deletedAt)
            }

            // 5. Build pull queries for tables with remote changes newer than our last sync
            val queries = manifest.tables
                .filter { row ->
                    row.maxUpdatedAt != null &&
                    runCatching { LocalDateTime.parse(row.maxUpdatedAt) }.getOrNull()?.isAfter(lastSyncedAt) == true
                }
                .map { row -> PullQuery(row.entityType, lastSyncedAt.toString()) }

            // 6. Pull remote changes
            if (queries.isNotEmpty()) {
                val pullResponse = syncClient.pull(confirmedPeer, PullRequest(thisDeviceId, queries))
                if (pullResponse != null) {
                    conflictsSkipped += applyPullResponse(pullResponse, confirmedPeer)
                }
            }

            // 7. Push our own changes to the peer
            // Build queries from peer's perspective: what have we changed since they last synced with us?
            val pushQueries = listOf(
                "tasks", "projects", "sections", "checklist_items", "tags", "folders",
                "notes", "comments", "reminders", "attachments", "time_entries", "saved_locations"
            ).map { type -> PullQuery(type, lastSyncedAt.toString()) }

            val ourPullResponse = syncClient.pull(
                confirmedPeer,
                PullRequest(confirmedPeer.deviceId, pushQueries.map {
                    PullQuery(it.entityType, lastSyncedAt.toString())
                })
            )
            // Actually for pushing OUR changes, we serve them — the peer pulls from us.
            // The bidirectionality is handled by both sides running syncWithPeer.
            // So step 7 is just updating the cursor; nothing more needed here.

            // 8. Update sync cursor
            syncDao.updateLastSyncedAt(confirmedPeer.deviceId, LocalDateTime.now())
            Log.d(TAG, "Sync with ${confirmedPeer.displayName} done. Skipped=$conflictsSkipped")
            _syncState.value = SyncState.Done(1, conflictsSkipped)

        } catch (e: Exception) {
            Log.e(TAG, "Sync error with ${peer.host}", e)
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    private suspend fun applyPullResponse(response: PullResponse, peer: PeerInfo): Int {
        var skipped = 0

        suspend fun <T> applyList(
            items: List<T>,
            entityType: String,
            getId: (T) -> String,
            getUpdatedAt: (T) -> LocalDateTime,
            upsert: suspend (T) -> Unit,
        ) {
            for (item in items) {
                val result = conflictResolver.resolve(getId(item), entityType, getUpdatedAt(item), syncDao)
                if (result == com.onlyflows.domain.sync.ConflictResult.ACCEPT) {
                    upsert(item)
                } else {
                    skipped++
                }
            }
        }

        applyList(response.tasks, "tasks",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertTask(it.toEntity()) }
        )
        applyList(response.projects, "projects",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertProject(it.toEntity()) }
        )
        applyList(response.sections, "sections",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertSection(it.toEntity()) }
        )
        applyList(response.checklistItems, "checklist_items",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertChecklistItem(it.toEntity()) }
        )
        applyList(response.tags, "tags",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertTag(it.toEntity()) }
        )
        applyList(response.folders, "folders",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertFolder(it.toEntity()) }
        )
        applyList(response.notes, "notes",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertNote(it.toEntity()) }
        )
        applyList(response.comments, "comments",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertComment(it.toEntity()) }
        )
        applyList(response.reminders, "reminders",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertReminder(it.toEntity()) }
        )
        applyList(response.timeEntries, "time_entries",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertTimeEntry(it.toEntity()) }
        )
        applyList(response.savedLocations, "saved_locations",
            { it.id },
            { runCatching { LocalDateTime.parse(it.updatedAt) }.getOrDefault(LocalDateTime.MIN) },
            { syncDao.upsertSavedLocation(it.toEntity()) }
        )

        // Attachments: upsert metadata first, then download files
        val acceptedAttachments = mutableListOf<AttachmentDto>()
        for (dto in response.attachments) {
            val incomingUpdatedAt = runCatching { LocalDateTime.parse(dto.updatedAt) }.getOrDefault(LocalDateTime.MIN)
            val result = conflictResolver.resolve(dto.id, "attachments", incomingUpdatedAt, syncDao)
            if (result == com.onlyflows.domain.sync.ConflictResult.ACCEPT) {
                syncDao.upsertAttachment(dto.toEntity())
                acceptedAttachments.add(dto)
            } else {
                skipped++
            }
        }
        if (acceptedAttachments.isNotEmpty()) {
            attachmentSyncer.syncAttachments(peer, acceptedAttachments)
        }

        return skipped
    }

    private suspend fun applyTombstone(entityType: String, entityId: String, deletedAt: LocalDateTime) {
        // Only apply tombstone if we don't have a newer local version
        val localUpdatedAt = when (entityType) {
            "tasks"           -> syncDao.getTaskUpdatedAt(entityId)
            "projects"        -> syncDao.getProjectUpdatedAt(entityId)
            "sections"        -> syncDao.getSectionUpdatedAt(entityId)
            "checklist_items" -> syncDao.getChecklistItemUpdatedAt(entityId)
            "tags"            -> syncDao.getTagUpdatedAt(entityId)
            "folders"         -> syncDao.getFolderUpdatedAt(entityId)
            "notes"           -> syncDao.getNoteUpdatedAt(entityId)
            "comments"        -> syncDao.getCommentUpdatedAt(entityId)
            "reminders"       -> syncDao.getReminderUpdatedAt(entityId)
            "attachments"     -> syncDao.getAttachmentUpdatedAt(entityId)
            "time_entries"    -> syncDao.getTimeEntryUpdatedAt(entityId)
            "saved_locations" -> syncDao.getSavedLocationUpdatedAt(entityId)
            else              -> null
        }
        if (localUpdatedAt != null && localUpdatedAt > deletedAt) return // local is newer, keep it

        // Delete locally and record tombstone
        when (entityType) {
            "tasks"           -> syncDao.deleteTask(entityId)
            "projects"        -> syncDao.deleteProject(entityId)
            "sections"        -> syncDao.deleteSection(entityId)
            "checklist_items" -> syncDao.deleteChecklistItem(entityId)
            "tags"            -> syncDao.deleteTag(entityId)
            "folders"         -> syncDao.deleteFolder(entityId)
            "notes"           -> syncDao.deleteNote(entityId)
            "comments"        -> syncDao.deleteComment(entityId)
            "reminders"       -> syncDao.deleteReminder(entityId)
            "attachments"     -> syncDao.deleteAttachment(entityId)
            "time_entries"    -> syncDao.deleteTimeEntry(entityId)
            "saved_locations" -> syncDao.deleteSavedLocation(entityId)
        }
        syncDao.insertTombstone(
            DeletedEntityEntity("tomb_$entityId", entityType, entityId, deletedAt)
        )
    }
}
