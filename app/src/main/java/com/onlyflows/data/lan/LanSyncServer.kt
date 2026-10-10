package com.onlyflows.data.lan

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
        val clientIp = socket.inetAddress.hostAddress ?: ""
        val input = socket.getInputStream()
        val out   = socket.getOutputStream()

        // Read header bytes (ASCII) until CRLF CRLF — body is read separately as raw UTF-8 bytes
        val header = StringBuilder()
        while (!header.endsWith("\r\n\r\n")) {
            val b = input.read()
            if (b == -1) break
            header.append(b.toChar())
        }
        val lines = header.toString().split("\r\n")
        val requestLine = lines.firstOrNull() ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return
        val method   = parts[0]
        val fullPath = parts[1]
        val (path, query) = if ("?" in fullPath) fullPath.split("?", limit = 2).let { it[0] to it[1] }
                            else fullPath to ""
        val params = query.split("&").filter { "=" in it }.associate {
            it.split("=", limit = 2).let { p -> p[0] to java.net.URLDecoder.decode(p[1], "UTF-8") }
        }

        val contentLength = lines.find { it.lowercase().startsWith("content-length:") }
            ?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0

        // Read exactly contentLength BYTES then decode UTF-8 (chars != bytes for accents)
        val body = if (contentLength > 0) {
            val buf = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val r = input.read(buf, read, contentLength - read)
                if (r == -1) break
                read += r
            }
            String(buf, 0, read, Charsets.UTF_8)
        } else ""

        // Attachment bytes are binary and can be large, so they bypass the JSON/String path
        // below and stream straight onto the socket.
        if (method == "GET" && path == "/attachment") {
            val bytes = params["id"]?.let { id -> runBlocking { coordinator.readAttachmentBytes(id) } }
            if (bytes == null) {
                val msg = "Not found".toByteArray()
                out.write(httpHeader("404 Not Found", "text/plain", msg.size).toByteArray())
                out.write(msg)
            } else {
                out.write(httpHeader("200 OK", "application/octet-stream", bytes.size).toByteArray())
                out.write(bytes)
            }
            out.flush()
            return
        }

        val (status, mime, body2) = try {
            when {
                method == "GET"  && path == "/info"  -> Triple("200 OK", "application/json",
                    runBlocking { coordinator.buildInfoJson() }.toString())
                method == "GET"  && path == "/delta" -> {
                    val since = params["since"]?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.MIN
                    Triple("200 OK", "application/json", runBlocking { coordinator.buildDelta(since) }.toString())
                }
                method == "POST" && path == "/apply" -> {
                    runBlocking { coordinator.applyDeltaFrom(clientIp, JSONObject(body)) }
                    Triple("200 OK", "text/plain", "ok")
                }
                method == "POST" && path == "/pair"  -> {
                    // Inject real client IP — device B sends "" because it doesn't know its own IP
                    val json = JSONObject(body).put("ip", clientIp)
                    val resp = runBlocking { coordinator.handlePairRequest(json) }
                    if (resp != null) Triple("200 OK", "application/json", resp.toString())
                    else Triple("403 Forbidden", "text/plain", "Invalid code")
                }
                else -> Triple("404 Not Found", "text/plain", "Not found")
            }
        } catch (e: Exception) {
            Triple("500 Internal Server Error", "text/plain", e.message ?: "Error")
        }

        val bytes = body2.toByteArray()
        val respHeader = "HTTP/1.1 $status\r\nContent-Type: $mime; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(respHeader.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun httpHeader(status: String, mime: String, length: Int): String =
        "HTTP/1.1 $status\r\nContent-Type: $mime\r\nContent-Length: $length\r\nConnection: close\r\n\r\n"
}
