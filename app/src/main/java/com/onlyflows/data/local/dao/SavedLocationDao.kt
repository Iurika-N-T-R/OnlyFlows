package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.SavedLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loc: SavedLocationEntity)

    @Delete
    suspend fun delete(loc: SavedLocationEntity)

    @Query("SELECT * FROM saved_locations ORDER BY label ASC")
    fun observeAll(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedLocationEntity?

    @Query("SELECT * FROM saved_locations ORDER BY label ASC")
    suspend fun getAll(): List<SavedLocationEntity>
}
