package com.onlyflows.data.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDateTime

object AppLog {

    private const val MAX_ENTRIES = 500

    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val _flow  = MutableSharedFlow<LogEntry>(replay = MAX_ENTRIES)
    val entries: SharedFlow<LogEntry> = _flow.asSharedFlow()

    fun v(tag: String, msg: String)                        = record(LogLevel.VERBOSE, tag, msg)
    fun d(tag: String, msg: String)                        = record(LogLevel.DEBUG,   tag, msg)
    fun i(tag: String, msg: String)                        = record(LogLevel.INFO,    tag, msg)
    fun w(tag: String, msg: String, t: Throwable? = null)  = record(LogLevel.WARN,   tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null)  = record(LogLevel.ERROR,  tag, msg, t)

    fun snapshot(): List<LogEntry> = synchronized(buffer) { buffer.toList() }

    fun clear() = synchronized(buffer) { buffer.clear() }

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
    }
}
