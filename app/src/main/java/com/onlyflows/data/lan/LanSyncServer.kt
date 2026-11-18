package com.onlyflows.data.lan

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

// ponytail: minimal HTTP server via ServerSocket — no NanoHTTPD dep needed for 3 endpoints
// Public: GET /info (discovery), POST /pair (needs the code shown on screen).
// Everything else goes through POST /sync, sealed with a paired device's key (see LanCrypto).
class LanSyncServer(private val port: Int, private val coordinator: LanSyncCoordinator) {

    companion object {
        private const val MAX_HEADER = 16 * 1024
        private const val MAX_BODY = 64 * 1024 * 1024 // a whole state delta, attachments excluded
        private const val MAX_CLIENTS = 8
        private const val TIMEOUT_MS = 20_000
    }

    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null
    private val clients = java.util.concurrent.atomic.AtomicInteger()

    fun start() {
        running = true
        val thread = Thread {
            runCatching {
                serverSocket = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(port)) }
                while (running) {
                    runCatching {
                        val client = serverSocket?.accept() ?: return@runCatching
                        if (clients.incrementAndGet() > MAX_CLIENTS) { clients.decrementAndGet(); client.close(); return@runCatching }
                        Thread { try { handleClient(client) } finally { clients.decrementAndGet() } }.also { it.isDaemon = true }.start()
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
        serverSocket = null
    }

    private fun handleClient(socket: Socket) = socket.use {
        socket.soTimeout = TIMEOUT_MS
        val clientIp = socket.inetAddress.hostAddress ?: ""
        val input = socket.getInputStream()
        val out   = socket.getOutputStream()

        // Read header bytes (ASCII) until CRLF CRLF — body is read separately as raw bytes
        val header = StringBuilder()
        while (!header.endsWith("\r\n\r\n")) {
            if (header.length > MAX_HEADER) return
            val b = input.read()
            if (b == -1) break
            header.append(b.toChar())
        }
        val lines = header.toString().split("\r\n")
        val parts = (lines.firstOrNull() ?: return).split(" ")
        if (parts.size < 2) return
        val method = parts[0]
        val path   = parts[1].substringBefore('?')
        fun headerValue(name: String) = lines.find { it.lowercase().startsWith("$name:") }?.substringAfter(":")?.trim()

        val contentLength = headerValue("content-length")?.toIntOrNull() ?: 0
        if (contentLength !in 0..MAX_BODY) return respond(out, "413 Payload Too Large", "text/plain", "Too large".toByteArray())
        val body = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val r = input.read(body, read, contentLength - read)
            if (r == -1) break
            read += r
        }
        if (read < contentLength) return

        try {
            when {
                method == "GET" && path == "/info" ->
                    respond(out, "200 OK", "application/json", runBlocking { coordinator.buildInfoJson() }.toString().toByteArray())
                method == "POST" && path == "/pair" -> {
                    // Inject real client IP — device B sends "" because it doesn't know its own IP
                    val json = JSONObject(String(body, Charsets.UTF_8)).put("ip", clientIp)
                    val resp = runBlocking { coordinator.handlePairRequest(json) }
                    if (resp != null) respond(out, "200 OK", "application/json", resp.toString().toByteArray())
                    else respond(out, "403 Forbidden", "text/plain", "Invalid code".toByteArray())
                }
                method == "POST" && path == "/sync" -> {
                    val sealed = runBlocking { coordinator.handleSecure(headerValue("x-device").orEmpty(), clientIp, body) }
                    if (sealed != null) respond(out, "200 OK", "application/octet-stream", sealed)
                    else respond(out, "403 Forbidden", "text/plain", "Not paired".toByteArray())
                }
                else -> respond(out, "404 Not Found", "text/plain", "Not found".toByteArray())
            }
        } catch (e: Exception) {
            respond(out, "500 Internal Server Error", "text/plain", "Error".toByteArray())
        }
    }

    private fun respond(out: java.io.OutputStream, status: String, mime: String, bytes: ByteArray) {
        out.write("HTTP/1.1 $status\r\nContent-Type: $mime\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes)
        out.flush()
    }
}
