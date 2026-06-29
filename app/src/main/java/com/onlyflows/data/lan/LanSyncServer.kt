package com.onlyflows.data.lan

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDateTime

// ponytail: minimal HTTP server via ServerSocket — no NanoHTTPD dep needed for 4 endpoints
class LanSyncServer(private val port: Int, private val coordinator: LanSyncCoordinator) {

    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        running = true
        val thread = Thread {
            runCatching {
                serverSocket = ServerSocket(port)
                while (running) {
                    runCatching {
                        val client = serverSocket?.accept() ?: return@runCatching
                        Thread { handleClient(client) }.also { it.isDaemon = true }.start()
                    }
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
    }

    private fun handleClient(socket: Socket) = socket.use {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val out    = socket.getOutputStream()

        val requestLine = reader.readLine() ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return
        val method   = parts[0]
        val fullPath = parts[1]
        val (path, query) = if ("?" in fullPath) fullPath.split("?", limit = 2).let { it[0] to it[1] }
                            else fullPath to ""
        val params = query.split("&").filter { "=" in it }.associate {
            it.split("=", limit = 2).let { p -> p[0] to java.net.URLDecoder.decode(p[1], "UTF-8") }
        }

        var contentLength = 0
        var line = reader.readLine()
        while (!line.isNullOrBlank()) {
            if (line.lowercase().startsWith("content-length:"))
                contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
            line = reader.readLine()
        }

        val body = if (contentLength > 0) {
            val buf = CharArray(contentLength); reader.read(buf, 0, contentLength); String(buf)
        } else ""

        val (status, mime, body2) = try {
            when {
                method == "GET"  && path == "/info"  -> Triple("200 OK", "application/json",
                    runBlocking { coordinator.buildInfoJson() }.toString())
                method == "GET"  && path == "/delta" -> {
                    val since = params["since"]?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.MIN
                    Triple("200 OK", "application/json", runBlocking { coordinator.buildDelta(since) }.toString())
                }
                method == "POST" && path == "/apply" -> {
                    runBlocking { coordinator.applyDelta(JSONObject(body)) }
                    Triple("200 OK", "text/plain", "ok")
                }
                method == "POST" && path == "/pair"  -> {
                    val resp = runBlocking { coordinator.handlePairRequest(JSONObject(body)) }
                    if (resp != null) Triple("200 OK", "application/json", resp.toString())
                    else Triple("403 Forbidden", "text/plain", "Invalid code")
                }
                else -> Triple("404 Not Found", "text/plain", "Not found")
            }
        } catch (e: Exception) {
            Triple("500 Internal Server Error", "text/plain", e.message ?: "Error")
        }

        val bytes = body2.toByteArray()
        val header = "HTTP/1.1 $status\r\nContent-Type: $mime\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }
}
