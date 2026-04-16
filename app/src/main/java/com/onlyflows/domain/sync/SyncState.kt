package com.onlyflows.domain.sync

sealed class SyncState {
    object Idle : SyncState()
    object Discovering : SyncState()
    data class Syncing(val peerName: String) : SyncState()
    data class Done(val peersCount: Int, val conflictsSkipped: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}
