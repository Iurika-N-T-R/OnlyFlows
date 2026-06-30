package com.onlyflows.wear.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Process-global refresh signal: the listener service pings, the active ViewModel reloads. */
object WearRefresh {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    fun ping() { _events.tryEmit(Unit) }
}
