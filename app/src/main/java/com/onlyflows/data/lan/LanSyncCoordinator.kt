package com.onlyflows.data.lan

import android.content.Context
import com.onlyflows.data.local.dao.PairedDeviceDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.*
import com.onlyflows.data.syncthing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import java.net.NetworkInterface
import java.time.LocalDateTime

private const val TAG = "LanSync"

class LanSyncCoordinator(
    private val context: Context,
    private val prefs: LanSyncPreferences,
    private val syncDao: SyncDao,
    private val pairedDeviceDao: PairedDeviceDao,
    private val client: LanSyncClient,
    private val scope: CoroutineScope,
) {
    companion object {
        const val PORT = 47891
        private const val CODE_TTL_MS = 5 * 60 * 1000L
    }

    private var pendingCode: String? = null
    private var pendingCodeExpiry = 0L

    val isImporting: StateFlow<Boolean> = MutableStateFlow(false)

    // (ip, port, name) — discovered on LAN but not yet paired
    private val _discoveredPeers = MutableStateFlow<List<Triple<String, Int, String>>>(emptyList())
    val discoveredPeers: StateFlow<List<Triple<String, Int, String>>> = _discoveredPeers

    val pairingCode: StateFlow<String?> get() = _pairingCode
    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCodeExpiryMs: StateFlow<Long> get() = _pairingCodeExpiryMs
    private val _pairingCodeExpiryMs = MutableStateFlow(0L)

    fun forceSync() {
        scope.launch {
            pairedDeviceDao.getAll().forEach { peer ->
                val since = syncDao.getSyncMeta(peer.id)?.lastSyncedAt ?: LocalDateTime.MIN
                runCatching { syncWithPeer(peer.lastKnownIp, peer.port, peer.id, since) }
            }
        }
    }

    fun rescan() {
        _discoveredPeers.value = emptyList()
        // Restart NSD (passive)
        discovery?.stop()
        scope.launch {
            val deviceName = prefs.getDeviceName()
            discovery = LanSyncDiscovery(
                context     = context,
                port        = PORT,
                deviceName  = deviceName,
                onPeerFound = { ip, port -> scope.launch { onPeerDiscovered(ip, port) } },
                onPeerLost  = { },
            ).also { it.start() }
        }
        // Active subnet sweep — finds devices even when mDNS is blocked by router
        scope.launch(Dispatchers.IO) {
            Log.d(TAG, "rescan: starting subnet sweep")
            scanSubnet()
            Log.d(TAG, "rescan: subnet sweep done, peers=${_discoveredPeers.value.size}")
        }
    }

    private fun getLocalIp(): String? {
        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull() ?: return null
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is java.net.Inet4Address)
                    return addr.hostAddress
            }
        }
        return null
    }

    private suspend fun scanSubnet() {
        val myIp = getLocalIp()
        Log.d(TAG, "scanSubnet: myIp=$myIp")
        if (myIp == null) { Log.w(TAG, "scanSubnet: no local IP found, aborting"); return }
        val myId = prefs.getDeviceId()
        val prefix = myIp.substringBeforeLast(".")
        Log.d(TAG, "scanSubnet: scanning $prefix.1-254")

        // Probe all 254 addresses in parallel batches of 30, 300ms connect timeout
        (1..254)
            .map { "$prefix.$it" }
            .filter { it != myIp }
            .chunked(30)
            .forEach { batch ->
                batch.map { ip ->
                    scope.async(Dispatchers.IO) {
                        val info = client.getInfo(ip, PORT, connectTimeoutMs = 300)
                        if (info == null) return@async
                        Log.d(TAG, "scanSubnet: HIT $ip → $info")
                        val peerId = info.optString("deviceId").takeIf { it.isNotBlank() } ?: return@async
                        if (peerId == myId) return@async   // own device
                        val peerName = info.optString("deviceName")
                        _discoveredPeers.update { current ->
                            if (current.none { it.first == ip })
                                current + Triple(ip, PORT, peerName.ifBlank { ip })
                            else current
                        }
                        onPeerDiscovered(ip, PORT)
                    }
                }.awaitAll()
            }
    }

    private val queue = Channel<JSONObject>(Channel.UNLIMITED)
    private var server: LanSyncServer? = null
    private var discovery: LanSyncDiscovery? = null

    fun start() {
        server = LanSyncServer(PORT, this).also { it.start() }
        scope.launch {
            val deviceName = prefs.getDeviceName()
            discovery = LanSyncDiscovery(
                context      = context,
                port         = PORT,
                deviceName   = deviceName,
                onPeerFound  = { ip, port -> scope.launch { onPeerDiscovered(ip, port) } },
                onPeerLost   = { },
            ).also { it.start() }
        }
        // Process mutation queue — each peer gets its own fire-and-forget coroutine
        // so an offline peer never blocks mutations to online peers
        scope.launch {
            for (delta in queue) {
                val peers = pairedDeviceDao.getAll()
                peers.forEach { peer ->
                    scope.launch(Dispatchers.IO) {
                        runCatching { client.postApply(peer.lastKnownIp, peer.port, delta) }
                    }
                }
            }
        }
    }

    fun stop() {
        server?.stop()
        discovery?.stop()
    }

    // Called from LanSyncWriter — fire and forget onto the queue
    fun enqueue(entityType: String, json: JSONObject) {
        queue.trySend(JSONObject().put(entityType, JSONArray().put(json)))
    }

    fun enqueueTombstone(entityType: String, entityId: String, deletedAt: LocalDateTime) {
        val t = JSONObject().put("entityType", entityType).put("entityId", entityId).put("deletedAt", deletedAt.toString())
        queue.trySend(JSONObject().put("tombstones", JSONArray().put(t)))
    }

    fun generatePairingCode(): String {
        val code = (100000..999999).random().toString()
        pendingCode = code
        pendingCodeExpiry = System.currentTimeMillis() + CODE_TTL_MS
        _pairingCode.value = code
        _pairingCodeExpiryMs.value = pendingCodeExpiry
        return code
    }

    // Called from Device B — pair with a discovered peer using the code shown on Device A
    suspend fun pairWithPeer(ip: String, port: Int, code: String): Boolean {
        val myId   = prefs.getDeviceId()
        val myName = prefs.getDeviceName()
        val body   = JSONObject()
            .put("code",       code)
            .put("deviceId",   myId)
            .put("deviceName", myName)
            .put("ip",         "")   // Device A resolves the IP from the TCP connection
            .put("port",       PORT)
        val resp = client.postPair(ip, port, body) ?: return false
        val peerId   = resp.optString("deviceId")
        val peerName = resp.optString("deviceName")
        pairedDeviceDao.upsert(PairedDeviceEntity(peerId, peerName, ip, port, LocalDateTime.now()))
        _discoveredPeers.update { it.filter { p -> p.first != ip } }
        scope.launch { syncWithPeer(ip, port, peerId, LocalDateTime.MIN) }
        return true
    }

    suspend fun unpairDevice(id: String) = pairedDeviceDao.delete(id)

    suspend fun handlePairRequest(json: JSONObject): JSONObject? {
        val code = json.optString("code")
        if (code != pendingCode || System.currentTimeMillis() > pendingCodeExpiry) return null
        pendingCode = null

        val peerId   = json.optString("deviceId")
        val peerName = json.optString("deviceName")
        val peerIp   = json.optString("ip")
        val peerPort = json.optInt("port", PORT)

        pairedDeviceDao.upsert(PairedDeviceEntity(peerId, peerName, peerIp, peerPort, LocalDateTime.now()))
        scope.launch { syncWithPeer(peerIp, peerPort, peerId, LocalDateTime.MIN) }
        return buildInfoJson()
    }

    suspend fun buildInfoJson(): JSONObject {
        return JSONObject()
            .put("deviceId",   prefs.getDeviceId())
            .put("deviceName", prefs.getDeviceName())
    }

    suspend fun buildDelta(since: LocalDateTime): JSONObject = withContext(Dispatchers.IO) {
        JSONObject().apply {
            put("tasks",          syncDao.getTasksChangedSince(since).toJsonArray { it.toJson() })
            put("projects",       syncDao.getProjectsChangedSince(since).toJsonArray { it.toJson() })
            put("sections",       syncDao.getSectionsChangedSince(since).toJsonArray { it.toJson() })
            put("checklistItems", syncDao.getChecklistItemsChangedSince(since).toJsonArray { it.toJson() })
            put("tags",           syncDao.getTagsChangedSince(since).toJsonArray { it.toJson() })
            put("folders",        syncDao.getFoldersChangedSince(since).toJsonArray { it.toJson() })
            put("notes",          syncDao.getNotesChangedSince(since).toJsonArray { it.toJson() })
            put("comments",       syncDao.getCommentsChangedSince(since).toJsonArray { it.toJson() })
            put("reminders",      syncDao.getRemindersChangedSince(since).toJsonArray { it.toJson() })
            put("timeEntries",    syncDao.getTimeEntriesChangedSince(since).toJsonArray { it.toJson() })
            put("savedLocations", syncDao.getSavedLocationsChangedSince(since).toJsonArray { it.toJson() })
            put("goals",          syncDao.getGoalsChangedSince(since).toJsonArray { it.toJson() })
            put("goalLogs",       syncDao.getGoalLogsChangedSince(since).toJsonArray { it.toJson() })
            put("dateTrackers",   syncDao.getDateTrackersChangedSince(since).toJsonArray { it.toJson() })
            put("tombstones",     syncDao.getTombstonesSince(since).toJsonArray {
                JSONObject().put("entityType", it.entityType).put("entityId", it.entityId).put("deletedAt", it.deletedAt.toString())
            })
        }
    }

    // Called when server receives /apply — also keeps the sender's IP fresh
    suspend fun applyDeltaFrom(senderIp: String, delta: JSONObject) {
        // Update IP for whichever paired device sent this push
        val paired = pairedDeviceDao.getAll()
        paired.find { it.lastKnownIp == senderIp }
            ?: paired.firstOrNull()?.also { pairedDeviceDao.updateIp(it.id, senderIp) }
        applyDelta(delta)
    }

    suspend fun applyDelta(delta: JSONObject) = withContext(Dispatchers.IO) {
        delta.forEachArray("tasks")          { syncDao.upsertTask(taskEntityFromJson(it)) }
        delta.forEachArray("projects")       { syncDao.upsertProject(projectEntityFromJson(it)) }
        delta.forEachArray("sections")       { syncDao.upsertSection(sectionEntityFromJson(it)) }
        delta.forEachArray("checklistItems") { syncDao.upsertChecklistItem(checklistItemEntityFromJson(it)) }
        delta.forEachArray("tags")           { syncDao.upsertTag(tagEntityFromJson(it)) }
        delta.forEachArray("folders")        { syncDao.upsertFolder(folderEntityFromJson(it)) }
        delta.forEachArray("notes")          { syncDao.upsertNote(noteEntityFromJson(it)) }
        delta.forEachArray("comments")       { syncDao.upsertComment(commentEntityFromJson(it)) }
        delta.forEachArray("reminders")      { syncDao.upsertReminder(reminderEntityFromJson(it)) }
        delta.forEachArray("timeEntries")    { syncDao.upsertTimeEntry(timeEntryEntityFromJson(it)) }
        delta.forEachArray("savedLocations") { syncDao.upsertSavedLocation(savedLocationEntityFromJson(it)) }
        delta.forEachArray("goals")          { syncDao.upsertGoal(goalEntityFromJson(it)) }
        delta.forEachArray("goalLogs")       { syncDao.upsertGoalLog(goalLogEntityFromJson(it)) }
        delta.forEachArray("dateTrackers")   { syncDao.upsertDateTracker(dateTrackerEntityFromJson(it)) }
        delta.forEachArray("tombstones")     { obj ->
            val type      = obj.optString("entityType")
            val id        = obj.optString("entityId")
            val deletedAt = runCatching { LocalDateTime.parse(obj.optString("deletedAt")) }.getOrNull() ?: return@forEachArray
            if (syncDao.isTombstoned(id, type)) return@forEachArray
            applyDelete(type, id)
            syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", type, id, deletedAt))
        }
    }

    private suspend fun applyDelete(type: String, id: String) = when (type) {
        "tasks"           -> syncDao.deleteTask(id)
        "projects"        -> syncDao.deleteProject(id)
        "sections"        -> syncDao.deleteSection(id)
        "checklist_items" -> syncDao.deleteChecklistItem(id)
        "tags"            -> syncDao.deleteTag(id)
        "folders"         -> syncDao.deleteFolder(id)
        "notes"           -> syncDao.deleteNote(id)
        "comments"        -> syncDao.deleteComment(id)
        "reminders"       -> syncDao.deleteReminder(id)
        "time_entries"    -> syncDao.deleteTimeEntry(id)
        "saved_locations" -> syncDao.deleteSavedLocation(id)
        "goals"           -> syncDao.deleteGoal(id)
        "goal_logs"       -> syncDao.deleteGoalLog(id)
        "date_trackers"   -> syncDao.deleteDateTracker(id)
        else              -> Unit
    }

    private suspend fun onPeerDiscovered(ip: String, port: Int) {
        val info     = client.getInfo(ip, port) ?: return
        val peerId   = info.optString("deviceId")
        val peerName = info.optString("deviceName")
        if (peerId == prefs.getDeviceId()) return // own device showing up

        val paired = pairedDeviceDao.getAll().find { it.id == peerId }
        if (paired != null) {
            pairedDeviceDao.updateIp(peerId, ip)
            val since = syncDao.getSyncMeta(peerId)?.lastSyncedAt ?: LocalDateTime.MIN
            syncWithPeer(ip, paired.port, peerId, since)
        } else {
            // Track for the pairing UI
            _discoveredPeers.update { current ->
                if (current.none { it.first == ip }) current + Triple(ip, port, peerName) else current
            }
        }
    }

    private suspend fun syncWithPeer(ip: String, port: Int, peerId: String, since: LocalDateTime) {
        val delta = client.getDelta(ip, port, since) ?: return
        applyDelta(delta)
        val ourDelta = buildDelta(since)
        client.postApply(ip, port, ourDelta)
        syncDao.upsertSyncMeta(SyncMetaEntity(peerId, "", LocalDateTime.now()))
    }

    // Helpers
    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray =
        JSONArray().also { arr -> forEach { arr.put(mapper(it)) } }

    private suspend fun JSONObject.forEachArray(key: String, action: suspend (JSONObject) -> Unit) {
        val arr = optJSONArray(key) ?: return
        for (i in 0 until arr.length()) runCatching { action(arr.getJSONObject(i)) }
    }
}
