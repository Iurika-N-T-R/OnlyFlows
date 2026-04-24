package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.DateTrackerEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface DateTrackerDao {

    @Query("SELECT * FROM date_trackers WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<DateTrackerEntity>>

    @Query("SELECT * FROM date_trackers WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): DateTrackerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DateTrackerEntity)

    @Update
    suspend fun update(entity: DateTrackerEntity)

    @Query("UPDATE date_trackers SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: LocalDateTime = LocalDateTime.now())

    @Query("DELETE FROM date_trackers WHERE id = :id")
    suspend fun delete(id: String)
}
