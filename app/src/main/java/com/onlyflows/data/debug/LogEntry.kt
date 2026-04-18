package com.onlyflows.data.debug

import java.time.LocalDateTime

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestamp: LocalDateTime,
    val throwable: String? = null,
)
