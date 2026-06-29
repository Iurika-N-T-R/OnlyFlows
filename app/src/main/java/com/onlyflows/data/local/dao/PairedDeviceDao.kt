package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.PairedDeviceEntity

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices")
    suspend fun getAll(): List<PairedDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_devices WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE paired_devices SET lastKnownIp = :ip WHERE id = :id")
    suspend fun updateIp(id: String, ip: String)
}
