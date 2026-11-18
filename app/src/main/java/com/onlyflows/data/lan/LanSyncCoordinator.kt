package com.onlyflows.data.lan

import android.content.Context
import com.onlyflows.data.local.AttachmentStore
import com.onlyflows.data.local.dao.PairedDeviceDao
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.local.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
        private const val CODE_TRIES = 5 // wrong codes before the shown code is dropped
    }

    private var pendingCode: String? = null
    private var pendingCodeExpiry = 0L
    private var wrongCodes = 0

    /** Paired devices to talk to: none while the "LAN sync active" switch is off. */
    private suspend fun peers() = if (prefs.enabled.first()) pairedDeviceDao.getAll() else emptyList()

    val isImporting: StateFlow<Boolean> = MutableStateFlow(false)

    // (ip, port, name) — discovered on LAN but not yet paired
    private val _discoveredPeers = MutableStateFlow<List<Triple<String, Int, String>>>(emptyList())
    val discoveredPeers: StateFlow<List<Triple<String, Int, String>>> = _discoveredPeers

    val pairingCode: StateFlow<String?> get() = _pairingCode
    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCodeExpiryMs: StateFlow<Long> get() = _pairingCodeExpiryMs
    private val _pairingCodeExpiryMs = MutableStateFlow(0L)

    fun forceSync() {
        scope.launch(Dispatchers.IO) {
            peers().forEach { peer -> runCatching { syncWithPeer(peer) } }
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

    private var server: LanSyncServer? = null
    private var discovery: LanSyncDiscovery? = null

    // Server, discovery and polling run only while the switch is on; turning it off stops them at once
    fun start() {
        scope.launch {
            prefs.enabled.distinctUntilChanged().collectLatest { on ->
                if (!on) return@collectLatest
                try {
                    server = LanSyncServer(PORT, this@LanSyncCoordinator).also { it.start() }
                    discovery = LanSyncDiscovery(
                        context      = context,
                        port         = PORT,
                        deviceName   = prefs.getDeviceName(),
                        onPeerFound  = { ip, port -> scope.launch { onPeerDiscovered(ip, port) } },
                        onPeerLost   = { },
                    ).also { it.start() }
                    // Poll every 3s — full-state sync, no cursor. LWW guard in applyDelta makes it idempotent.
                    while (true) {
                        delay(3_000)
                        withContext(Dispatchers.IO) { peers().forEach { peer -> runCatching { syncWithPeer(peer) } } }
                    }
                } finally {
                    stop()
                }
            }
        }
    }

    fun stop() {
        server?.stop(); server = null
        discovery?.stop(); discovery = null
    }

    // Each mutation launches its own coroutine — no Channel, no consumer to die
    fun enqueue(entityType: String, json: JSONObject) {
        val delta = JSONObject().put(entityType, JSONArray().put(json))
        scope.launch(Dispatchers.IO) {
            val peers = peers()
            if (peers.isEmpty()) return@launch
            val me = prefs.getDeviceId()
            Log.d(TAG, "push $entityType to ${peers.size} peers")
            peers.forEach { peer ->
                val ok = client.postApply(peer, me, delta)
                if (ok) Log.d(TAG, "push OK → ${peer.lastKnownIp}") else Log.w(TAG, "push FAIL → ${peer.lastKnownIp}")
            }
        }
    }

    fun enqueueTombstone(entityType: String, entityId: String, deletedAt: LocalDateTime) {
        val delta = JSONObject().put("tombstones",
            JSONArray().put(JSONObject().put("entityType", entityType).put("entityId", entityId).put("deletedAt", deletedAt.toString())))
        scope.launch(Dispatchers.IO) {
            val me = prefs.getDeviceId()
            peers().forEach { peer -> client.postApply(peer, me, delta) }
        }
    }

    fun generatePairingCode(): String {
        val code = (100000..999999).random().toString()
        pendingCode = code
        wrongCodes = 0
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
        val secret   = resp.optString("secret").takeIf { it.isNotBlank() } ?: return false // a peer still on the old protocol
        val peer = PairedDeviceEntity(peerId, peerName, ip, port, LocalDateTime.now(), secret)
        pairedDeviceDao.upsert(peer)
        _discoveredPeers.update { it.filter { p -> p.first != ip } }
        scope.launch(Dispatchers.IO) { syncWithPeer(peer) }
        return true
    }

    suspend fun unpairDevice(id: String) = pairedDeviceDao.delete(id)

    suspend fun handlePairRequest(json: JSONObject): JSONObject? {
        val code = json.optString("code")
        if (pendingCode == null || System.currentTimeMillis() > pendingCodeExpiry) return null
        if (code != pendingCode) {
            // a 6-digit code can't take guessing: after a few misses it's gone, make a new one
            if (++wrongCodes >= CODE_TRIES) { pendingCode = null; _pairingCode.value = null }
            return null
        }
        pendingCode = null
        _pairingCode.value = null

        val peerId   = json.optString("deviceId")
        val peerName = json.optString("deviceName")
        val peerIp   = json.optString("ip")
        val peerPort = json.optInt("port", PORT)

        if (peerId.isBlank()) return null
        val peer = PairedDeviceEntity(peerId, peerName, peerIp, peerPort, LocalDateTime.now(), LanCrypto.newSecret())
        pairedDeviceDao.upsert(peer)
        scope.launch(Dispatchers.IO) { syncWithPeer(peer) }
        return buildInfoJson().put("secret", peer.secret)
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
            put("attachments",    syncDao.getAttachmentsChangedSince(since).toJsonArray { it.toJson() })
            put("savedColors",    syncDao.getSavedColorsChangedSince(since).toJsonArray { it.toJson() })
            put("tombstones",     syncDao.getTombstonesSince(since).toJsonArray {
                JSONObject().put("entityType", it.entityType).put("entityId", it.entityId).put("deletedAt", it.deletedAt.toString())
            })
        }
    }

    /**
     * A /sync request: opened with the key of the paired device it claims to come from, else
     * refused (null). Unpairing a device deletes its key, so its requests stop working at once.
     */
    suspend fun handleSecure(deviceId: String, ip: String, sealed: ByteArray): ByteArray? {
        val peer = pairedDeviceDao.getById(deviceId)?.takeIf { it.secret.isNotEmpty() } ?: return null
        val req = LanCrypto.open(peer.secret, sealed)?.let { JSONObject(String(it, Charsets.UTF_8)) } ?: return null
        if (peer.lastKnownIp != ip) pairedDeviceDao.updateIp(peer.id, ip)
        val here = peer.copy(lastKnownIp = ip)
        val answer = when (req.optString("p")) {
            "delta"      -> buildDelta(LocalDateTime.MIN).toString().toByteArray()
            "apply"      -> { applyDelta(req.getJSONObject("d"), here); "ok".toByteArray() }
            // first byte: 1 = found, 0 = no such file here
            "attachment" -> readAttachmentBytes(req.optString("d"))?.let { byteArrayOf(1) + it } ?: byteArrayOf(0)
            else         -> return null
        }
        return LanCrypto.seal(peer.secret, answer)
    }

    /** Local bytes for an attachment the peer is asking for, or null when we hold no file. */
    suspend fun readAttachmentBytes(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = AttachmentStore.existingFile(context, id) ?: return@withContext null
        runCatching { file.readBytes() }.getOrNull()
    }

    suspend fun applyDelta(delta: JSONObject, sender: PairedDeviceEntity? = null) = withContext(Dispatchers.IO) {
        // Tombstones FIRST so a delete always beats a stale re-create in the same batch
        delta.forEachArray("tombstones") { obj ->
            val type      = obj.optString("entityType")
            val id        = obj.optString("entityId")
            val deletedAt = runCatching { LocalDateTime.parse(obj.optString("deletedAt")) }.getOrNull() ?: return@forEachArray
            if (syncDao.isTombstoned(id, type)) return@forEachArray
            applyDelete(type, id)
            syncDao.insertTombstone(DeletedEntityEntity("tomb_$id", type, id, deletedAt))
        }
        var alarmsDirty = false
        delta.forEachArray("tasks")          { o -> if (upsertIfNewer(o, "tasks", "updatedAt", syncDao::getTaskUpdatedAt) { syncDao.upsertTask(taskEntityFromJson(o)) }) alarmsDirty = true }
        delta.forEachArray("projects")       { o -> upsertIfNewer(o, "projects", "updatedAt", syncDao::getProjectUpdatedAt) { syncDao.upsertProject(projectEntityFromJson(o)) } }
        delta.forEachArray("sections")       { o -> upsertIfNewer(o, "sections", "updatedAt", syncDao::getSectionUpdatedAt) { syncDao.upsertSection(sectionEntityFromJson(o)) } }
        delta.forEachArray("checklistItems") { o -> upsertIfNewer(o, "checklist_items", "updatedAt", syncDao::getChecklistItemUpdatedAt) { syncDao.upsertChecklistItem(checklistItemEntityFromJson(o)) } }
        delta.forEachArray("tags")           { o -> upsertIfNewer(o, "tags", "updatedAt", syncDao::getTagUpdatedAt) { syncDao.upsertTag(tagEntityFromJson(o)) } }
        delta.forEachArray("folders")        { o -> upsertIfNewer(o, "folders", "updatedAt", syncDao::getFolderUpdatedAt) { syncDao.upsertFolder(folderEntityFromJson(o)) } }
        delta.forEachArray("notes")          { o -> upsertIfNewer(o, "notes", "updatedAt", syncDao::getNoteUpdatedAt) { syncDao.upsertNote(noteEntityFromJson(o)) } }
        delta.forEachArray("comments")       { o -> upsertIfNewer(o, "comments", "updatedAt", syncDao::getCommentUpdatedAt) { syncDao.upsertComment(commentEntityFromJson(o)) } }
        delta.forEachArray("reminders")      { o -> if (upsertIfNewer(o, "reminders", "updatedAt", syncDao::getReminderUpdatedAt) { syncDao.upsertReminder(reminderEntityFromJson(o)) }) alarmsDirty = true }
        delta.forEachArray("timeEntries")    { o -> upsertIfNewer(o, "time_entries", "updatedAt", syncDao::getTimeEntryUpdatedAt) { syncDao.upsertTimeEntry(timeEntryEntityFromJson(o)) } }
        delta.forEachArray("savedLocations") { o -> upsertIfNewer(o, "saved_locations", "updatedAt", syncDao::getSavedLocationUpdatedAt) { syncDao.upsertSavedLocation(savedLocationEntityFromJson(o)) } }
        delta.forEachArray("goals")          { o -> upsertIfNewer(o, "goals", "updatedAt", syncDao::getGoalUpdatedAt) { syncDao.upsertGoal(goalEntityFromJson(o)) } }
        delta.forEachArray("goalLogs")       { o -> upsertIfNewer(o, "goal_logs", "loggedAt", syncDao::getGoalLogLoggedAt) { syncDao.upsertGoalLog(goalLogEntityFromJson(o)) } }
        delta.forEachArray("dateTrackers")   { o -> upsertIfNewer(o, "date_trackers", "updatedAt", syncDao::getDateTrackerCreatedAt) { syncDao.upsertDateTracker(dateTrackerEntityFromJson(o)) } }
        // Attachments carry no URI across the wire: the bytes are fetched from the peer and
        // stored locally, and the row is only written once the file is actually on disk, so a
        // failed transfer retries on the next poll instead of leaving a dead attachment.
        delta.forEachArray("savedColors")    { o -> upsertIfNewer(o, "saved_colors", "updatedAt", syncDao::getSavedColorUpdatedAt) { syncDao.upsertSavedColor(savedColorEntityFromJson(o)) } }
        delta.forEachArray("attachments")    { o -> applyAttachment(o, sender) }
        // A remote project tombstone only drops the project row; take its tasks with it
        syncDao.softDeleteOfDeletedProjects(LocalDateTime.now().toString())
        // Remote deltas bypass the repositories, so refresh widgets + re-arm alarms explicitly
        com.onlyflows.ui.widget.WidgetUpdater.updateAll(context)
        if (alarmsDirty) com.onlyflows.data.notifications.AlarmReconciler.reconcile(context)
    }

    // Write only if not tombstoned and incoming is strictly newer than local — last-write-wins,
    // no resurrection, no flicker (steady state does zero writes). Returns true if it wrote.
    private suspend fun upsertIfNewer(
        json: JSONObject,
        type: String,
        tsKey: String,
        localTs: suspend (String) -> LocalDateTime?,
        write: suspend () -> Unit,
    ): Boolean {
        val id = json.optString("id")
        if (id.isBlank() || syncDao.isTombstoned(id, type)) return false
        val incoming = runCatching { LocalDateTime.parse(json.optString(tsKey)) }.getOrNull()
        val local = localTs(id)
        if (incoming != null && local != null && !incoming.isAfter(local)) return false
        write()
        return true
    }

    private suspend fun applyAttachment(json: JSONObject, sender: PairedDeviceEntity?) {
        val id = json.optString("id")
        if (id.isBlank() || syncDao.isTombstoned(id, "attachments")) return
        val fileName = json.optString("fileName", "file")

        val existing = AttachmentStore.existingFile(context, id)
        val localFile = if (existing != null) existing else {
            val peer  = sender ?: return
            val bytes = client.getAttachment(peer, prefs.getDeviceId(), id) ?: return
            AttachmentStore.writeBytes(context, id, fileName, bytes) ?: return
        }

        val incoming = runCatching { LocalDateTime.parse(json.optString("updatedAt")) }.getOrNull()
        val local    = syncDao.getAttachmentUpdatedAt(id)
        if (local != null && incoming != null && !incoming.isAfter(local)) return
        syncDao.upsertAttachment(
            attachmentEntityFromJson(json, AttachmentStore.shareUri(context, localFile).toString())
        )
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
        "reminders"       -> {
            syncDao.deleteReminder(id)
            // Cancel the ghost alarm a remotely-deleted reminder leaves behind
            com.onlyflows.data.notifications.ReminderScheduler(context).cancelByReminderId(id)
        }
        "time_entries"    -> syncDao.deleteTimeEntry(id)
        "saved_locations" -> syncDao.deleteSavedLocation(id)
        "goals"           -> syncDao.deleteGoal(id)
        "goal_logs"       -> syncDao.deleteGoalLog(id)
        "date_trackers"   -> syncDao.deleteDateTracker(id)
        "saved_colors"    -> syncDao.deleteSavedColor(id)
        "attachments"     -> {
            syncDao.deleteAttachment(id)
            AttachmentStore.deleteFor(context, id)
        }
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
            if (prefs.enabled.first()) syncWithPeer(paired.copy(lastKnownIp = ip))
        } else {
            // Track for the pairing UI
            _discoveredPeers.update { current ->
                if (current.none { it.first == ip }) current + Triple(ip, port, peerName) else current
            }
        }
    }

    // Full-state bidirectional sync. No cursor — LWW guard in applyDelta dedupes/protects.
    private suspend fun syncWithPeer(peer: PairedDeviceEntity) {
        val me = prefs.getDeviceId()
        val delta = client.getDelta(peer, me) ?: return
        applyDelta(delta, peer)
        if (!client.postApply(peer, me, buildDelta(LocalDateTime.MIN))) Log.w(TAG, "sync push refused or failed → ${peer.lastKnownIp}")
    }

    // Helpers
    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray =
        JSONArray().also { arr -> forEach { arr.put(mapper(it)) } }

    private suspend fun JSONObject.forEachArray(key: String, action: suspend (JSONObject) -> Unit) {
        val arr = optJSONArray(key) ?: return
        for (i in 0 until arr.length()) runCatching { action(arr.getJSONObject(i)) }
    }
}
