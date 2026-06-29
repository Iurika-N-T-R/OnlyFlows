package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastKnownIp: String,
    val port: Int,
    val pairedAt: LocalDateTime,
)
