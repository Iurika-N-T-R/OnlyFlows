package com.onlyflows.data.debug

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime

object AppLog {

    private const val MAX_ENTRIES  = 500
    private const val MAX_FILE_BYTES = 1_048_576L // 1 MB

    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val _flow  = MutableSharedFlow<LogEntry>(replay = MAX_ENTRIES)
    val entries: SharedFlow<LogEntry> = _flow.asSharedFlow()

    private var logFile: File? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Call once from Application.onCreate() to enable file persistence. */
    fun init(filesDir: File) {
        val logsDir = File(filesDir, "logs").also { it.mkdirs() }
        logFile = File(logsDir, "app.log")
    }

    fun v(tag: String, msg: String)                        = record(LogLevel.VERBOSE, tag, msg)
    fun d(tag: String, msg: String)                        = record(LogLevel.DEBUG,   tag, msg)
    fun i(tag: String, msg: String)                        = record(LogLevel.INFO,    tag, msg)
    fun w(tag: String, msg: String, t: Throwable? = null)  = record(LogLevel.WARN,   tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null)  = record(LogLevel.ERROR,  tag, msg, t)

    fun snapshot(): List<LogEntry> = synchronized(buffer) { buffer.toList() }

    fun clear() = synchronized(buffer) { buffer.clear() }

    fun getLogFile(): File? = logFile

    private fun record(level: LogLevel, tag: String, msg: String, t: Throwable? = null) {
        val entry = LogEntry(
            level     = level,
            tag       = tag,
            message   = msg,
            timestamp = LocalDateTime.now(),
            throwable = t?.stackTraceToString(),
        )
        synchronized(buffer) {
            if (buffer.size >= MAX_ENTRIES) buffer.removeFirst()
            buffer.addLast(entry)
        }
        _flow.tryEmit(entry)
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, msg)
            LogLevel.DEBUG   -> Log.d(tag, msg)
            LogLevel.INFO    -> Log.i(tag, msg)
            LogLevel.WARN    -> if (t != null) Log.w(tag, msg, t) else Log.w(tag, msg)
            LogLevel.ERROR   -> if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg)
        }
        appendToFile(entry)
    }

    private fun appendToFile(entry: LogEntry) {
        val file = logFile ?: return
        ioScope.launch {
            try {
                trimIfNeeded(file)
                val line = buildString {
                    append(entry.timestamp)
                    append(" ["); append(entry.level.name); append("] ")
                    append(entry.tag); append(": "); append(entry.message)
                    if (entry.throwable != null) { append("\n"); append(entry.throwable) }
                    append("\n")
                }
                file.appendText(line)
            } catch (_: Exception) { /* never crash on logging */ }
        }
    }

    private fun trimIfNeeded(file: File) {
        if (!file.exists() || file.length() < MAX_FILE_BYTES) return
        val lines = file.readLines()
        val trimmed = lines.drop(lines.size / 4)
        file.writeText(trimmed.joinToString("\n") + "\n")
    }
}
