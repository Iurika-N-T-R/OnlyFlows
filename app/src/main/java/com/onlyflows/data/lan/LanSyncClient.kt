package com.onlyflows.data.lan

import com.onlyflows.data.local.entity.PairedDeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LanSyncClient {

    /** Public: device id and name, for discovery. */
    suspend fun getInfo(ip: String, port: Int, connectTimeoutMs: Int = 3000): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("http://$ip:$port/info").openConnection() as HttpURLConnection
            conn.connectTimeout = connectTimeoutMs; conn.readTimeout = 3000
            if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText()) else null
        }.getOrNull()
    }

    suspend fun postPair(ip: String, port: Int, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = open("http://$ip:$port/pair", body.toString().toByteArray(), 5000)
            if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText()) else null
        }.getOrNull()
    }

    /** The peer's whole state. */
    suspend fun getDelta(peer: PairedDeviceEntity, myId: String): JSONObject? =
        secure(peer, myId, "delta", null)?.let { JSONObject(String(it, Charsets.UTF_8)) }

    /** True only when the peer confirmed it applied [delta]. */
    suspend fun postApply(peer: PairedDeviceEntity, myId: String, delta: JSONObject): Boolean =
        secure(peer, myId, "apply", delta)?.let { String(it, Charsets.UTF_8) == "ok" } == true

    /** Raw attachment bytes from a peer, or null when the peer has no file for [id]. */
    suspend fun getAttachment(peer: PairedDeviceEntity, myId: String, id: String): ByteArray? =
        secure(peer, myId, "attachment", id, 30_000)?.takeIf { it.isNotEmpty() && it[0] == 1.toByte() }?.let { it.copyOfRange(1, it.size) }

    // One sealed request to /sync, one sealed answer. Null: unreachable, refused (403) or unreadable.
    private suspend fun secure(peer: PairedDeviceEntity, myId: String, op: String, data: Any?, readTimeoutMs: Int = 15_000): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = LanCrypto.seal(peer.secret, JSONObject().put("p", op).put("d", data ?: JSONObject.NULL).toString().toByteArray())
                val conn = open("http://${peer.lastKnownIp}:${peer.port}/sync", body, readTimeoutMs, myId)
                if (conn.responseCode != 200) return@runCatching null
                LanCrypto.open(peer.secret, conn.inputStream.use { it.readBytes() })
            }.getOrNull()
        }

    private fun open(url: String, body: ByteArray, readTimeoutMs: Int, deviceId: String? = null): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 2000; conn.readTimeout = readTimeoutMs
        deviceId?.let { conn.setRequestProperty("X-Device", it) }
        conn.setFixedLengthStreamingMode(body.size)
        conn.outputStream.use { it.write(body) }
        return conn
    }
}
