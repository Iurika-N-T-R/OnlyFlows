package com.onlyflows.domain.sync

data class PeerInfo(
    val deviceId: String,
    val displayName: String,
    val host: String,
    val port: Int,
)
