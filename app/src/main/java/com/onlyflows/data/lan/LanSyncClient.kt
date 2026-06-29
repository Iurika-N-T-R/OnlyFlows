package com.onlyflows.data.lan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime

class LanSyncClient {

    suspend fun getInfo(ip: String, port: Int): JSONObject? = get("http://$ip:$port/info")

    suspend fun getDelta(ip: String, port: Int, since: LocalDateTime): JSONObject? =
        get("http://$ip:$port/delta?since=${URLEncoder.encode(since.toString(), "UTF-8")}")

    suspend fun postApply(ip: String, port: Int, delta: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            post("http://$ip:$port/apply", delta.toString())?.also { it.disconnect() }
            true
        }.getOrDefault(false)
    }

    suspend fun postPair(ip: String, port: Int, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = post("http://$ip:$port/pair", body.toString()) ?: return@runCatching null
            if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText()) else null
        }.getOrNull()
    }

    private suspend fun get(url: String): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000; conn.readTimeout = 15000
            if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText()) else null
        }.getOrNull()
    }

    private fun post(url: String, body: String): HttpURLConnection? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 5000; conn.readTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")
        val bytes = body.toByteArray()
        conn.setRequestProperty("Content-Length", bytes.size.toString())
        conn.outputStream.write(bytes)
        return conn
    }
}
