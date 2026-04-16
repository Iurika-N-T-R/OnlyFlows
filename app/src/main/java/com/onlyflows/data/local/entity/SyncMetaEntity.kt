package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val deviceId: String,
    val displayName: String,
    val lastSyncedAt: LocalDateTime,
)
