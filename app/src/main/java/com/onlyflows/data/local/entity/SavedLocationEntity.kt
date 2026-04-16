package com.onlyflows.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onlyflows.domain.model.SavedLocation
import java.time.LocalDateTime

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

fun SavedLocationEntity.toDomain() = SavedLocation(
    id           = id,
    label        = label,
    latitude     = latitude,
    longitude    = longitude,
    radiusMeters = radiusMeters,
    updatedAt    = updatedAt,
)

fun SavedLocation.toEntity() = SavedLocationEntity(
    id           = id,
    label        = label,
    latitude     = latitude,
    longitude    = longitude,
    radiusMeters = radiusMeters,
    updatedAt    = updatedAt,
)
