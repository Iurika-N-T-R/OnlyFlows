package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)

    /** Tags whose id doesn't appear in any non-deleted task's tagIds JSON column. */
    @Query("""
        SELECT * FROM tags
        WHERE NOT EXISTS (
            SELECT 1 FROM tasks
            WHERE tagIds LIKE '%' || tags.id || '%'
              AND deletedAt IS NULL
        )
        ORDER BY name ASC
    """)
    fun observeUnused(): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE tagIds LIKE '%' || :tagId || '%' AND deletedAt IS NULL")
    fun observeUsageCount(tagId: String): Flow<Int>

    @Query("SELECT id FROM tags WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE tagIds LIKE '%' || tags.id || '%' AND deletedAt IS NULL)")
    suspend fun getUnusedIds(): List<String>

    @Query("DELETE FROM tags WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE tagIds LIKE '%' || tags.id || '%' AND deletedAt IS NULL)")
    suspend fun deleteUnused()
}
