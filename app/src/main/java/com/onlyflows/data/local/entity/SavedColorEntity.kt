package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * A colour the user saved for reuse.
 *
 * These lived in DataStore, which meant a palette built on the phone did not exist on the
 * tablet — a shared vocabulary of colours that only half the devices knew. Room makes them an
 * entity like any other, so they travel over LAN sync.
 *
 * The hex *is* the id: the same colour saved on two devices is the same row, so the two
 * palettes converge instead of accumulating duplicates.
 */
@Entity(tableName = "saved_colors")
data class SavedColorEntity(
    @PrimaryKey val id: String,
    /** Position in the palette; 0 is the most recently saved. */
    val sortOrder: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
