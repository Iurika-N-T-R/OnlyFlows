package com.onlyflows.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.onlyflows.data.local.entity.SavedColorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedColorDao {

    @Query("SELECT * FROM saved_colors ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeAll(): Flow<List<SavedColorEntity>>

    @Query("SELECT * FROM saved_colors ORDER BY sortOrder ASC, updatedAt DESC")
    suspend fun getAll(): List<SavedColorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedColorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SavedColorEntity>)

    @Query("DELETE FROM saved_colors WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM saved_colors")
    suspend fun count(): Int
}
