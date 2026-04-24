package com.onlyflows.data.local.dao

import androidx.room.*
import com.onlyflows.data.local.entity.*
import java.time.LocalDateTime

@Dao
interface SyncDao {

    // ── Tombstones ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(entity: DeletedEntityEntity)

    @Query("SELECT COUNT(*) > 0 FROM deleted_entities WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun isTombstoned(entityId: String, entityType: String): Boolean

    @Query("SELECT * FROM deleted_entities WHERE deletedAt > :since")
    suspend fun getTombstonesSince(since: LocalDateTime): List<DeletedEntityEntity>

    @Query("SELECT * FROM deleted_entities")
    suspend fun getAllTombstones(): List<DeletedEntityEntity>

    // ── SyncMeta (per-peer cursors) ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMeta(meta: SyncMetaEntity)

    @Query("SELECT * FROM sync_meta WHERE deviceId = :deviceId")
    suspend fun getSyncMeta(deviceId: String): SyncMetaEntity?

    @Query("UPDATE sync_meta SET lastSyncedAt = :time WHERE deviceId = :deviceId")
    suspend fun updateLastSyncedAt(deviceId: String, time: LocalDateTime)

    // ── Upserts per entity type ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(entity: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(entity: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChecklistItem(entity: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(entity: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(entity: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(entity: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(entity: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachment(entity: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeEntry(entity: TimeEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedLocation(entity: SavedLocationEntity)

    // ── Query updatedAt for conflict resolution ────────────────────────────────

    @Query("SELECT updatedAt FROM tasks WHERE id = :id")
    suspend fun getTaskUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM projects WHERE id = :id")
    suspend fun getProjectUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM sections WHERE id = :id")
    suspend fun getSectionUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM checklist_items WHERE id = :id")
    suspend fun getChecklistItemUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM tags WHERE id = :id")
    suspend fun getTagUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM folders WHERE id = :id")
    suspend fun getFolderUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updated_at FROM notes WHERE id = :id")
    suspend fun getNoteUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM comments WHERE id = :id")
    suspend fun getCommentUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM reminders WHERE id = :id")
    suspend fun getReminderUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM attachments WHERE id = :id")
    suspend fun getAttachmentUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM time_entries WHERE id = :id")
    suspend fun getTimeEntryUpdatedAt(id: String): LocalDateTime?

    @Query("SELECT updatedAt FROM saved_locations WHERE id = :id")
    suspend fun getSavedLocationUpdatedAt(id: String): LocalDateTime?

    // ── Fetch entities changed since a cursor (for push) ──────────────────────

    @Query("SELECT * FROM tasks WHERE updatedAt > :since")
    suspend fun getTasksChangedSince(since: LocalDateTime): List<TaskEntity>

    @Query("SELECT * FROM projects WHERE updatedAt > :since")
    suspend fun getProjectsChangedSince(since: LocalDateTime): List<ProjectEntity>

    @Query("SELECT * FROM sections WHERE updatedAt > :since")
    suspend fun getSectionsChangedSince(since: LocalDateTime): List<SectionEntity>

    @Query("SELECT * FROM checklist_items WHERE updatedAt > :since")
    suspend fun getChecklistItemsChangedSince(since: LocalDateTime): List<ChecklistItemEntity>

    @Query("SELECT * FROM tags WHERE updatedAt > :since")
    suspend fun getTagsChangedSince(since: LocalDateTime): List<TagEntity>

    @Query("SELECT * FROM folders WHERE updatedAt > :since")
    suspend fun getFoldersChangedSince(since: LocalDateTime): List<FolderEntity>

    @Query("SELECT * FROM notes WHERE updated_at > :since")
    suspend fun getNotesChangedSince(since: LocalDateTime): List<NoteEntity>

    @Query("SELECT * FROM comments WHERE updatedAt > :since")
    suspend fun getCommentsChangedSince(since: LocalDateTime): List<CommentEntity>

    @Query("SELECT * FROM reminders WHERE updatedAt > :since")
    suspend fun getRemindersChangedSince(since: LocalDateTime): List<ReminderEntity>

    @Query("SELECT * FROM attachments WHERE updatedAt > :since")
    suspend fun getAttachmentsChangedSince(since: LocalDateTime): List<AttachmentEntity>

    @Query("SELECT * FROM time_entries WHERE updatedAt > :since")
    suspend fun getTimeEntriesChangedSince(since: LocalDateTime): List<TimeEntryEntity>

    @Query("SELECT * FROM saved_locations WHERE updatedAt > :since")
    suspend fun getSavedLocationsChangedSince(since: LocalDateTime): List<SavedLocationEntity>

    // ── Manifest helpers (max updatedAt per table) ─────────────────────────────

    @Query("SELECT MAX(updatedAt) FROM tasks")
    suspend fun getTasksMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM projects")
    suspend fun getProjectsMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM sections")
    suspend fun getSectionsMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM checklist_items")
    suspend fun getChecklistItemsMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM tags")
    suspend fun getTagsMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM folders")
    suspend fun getFoldersMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updated_at) FROM notes")
    suspend fun getNotesMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM comments")
    suspend fun getCommentsMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM reminders")
    suspend fun getRemindersMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM attachments")
    suspend fun getAttachmentsMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM time_entries")
    suspend fun getTimeEntriesMaxUpdatedAt(): LocalDateTime?

    @Query("SELECT MAX(updatedAt) FROM saved_locations")
    suspend fun getSavedLocationsMaxUpdatedAt(): LocalDateTime?

    // ── Delete local entity (used when applying tombstone from remote) ─────────

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteSection(id: String)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)

    @Query("DELETE FROM time_entries WHERE id = :id")
    suspend fun deleteTimeEntry(id: String)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteSavedLocation(id: String)

    // ── Attachment URI update (after file download) ────────────────────────────

    @Query("UPDATE attachments SET uri = :uri WHERE id = :id")
    suspend fun updateAttachmentUri(id: String, uri: String)

    // ── Goals & GoalLogs ──────────────────────────────────────────────────────

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(entity: com.onlyflows.data.local.entity.GoalEntity)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertGoalLog(entity: com.onlyflows.data.local.entity.GoalLogEntity)

    @Query("SELECT updatedAt FROM goals WHERE id = :id")
    suspend fun getGoalUpdatedAt(id: String): java.time.LocalDateTime?

    @Query("SELECT loggedAt FROM goal_logs WHERE id = :id")
    suspend fun getGoalLogLoggedAt(id: String): java.time.LocalDateTime?

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("DELETE FROM goal_logs WHERE id = :id")
    suspend fun deleteGoalLog(id: String)

    // ── Full-table reads for initial export ───────────────────────────────────

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<com.onlyflows.data.local.entity.TaskEntity>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<com.onlyflows.data.local.entity.ProjectEntity>

    @Query("SELECT * FROM sections")
    suspend fun getAllSections(): List<com.onlyflows.data.local.entity.SectionEntity>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItems(): List<com.onlyflows.data.local.entity.ChecklistItemEntity>

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<com.onlyflows.data.local.entity.TagEntity>

    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<com.onlyflows.data.local.entity.FolderEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<com.onlyflows.data.local.entity.NoteEntity>

    @Query("SELECT * FROM comments")
    suspend fun getAllComments(): List<com.onlyflows.data.local.entity.CommentEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<com.onlyflows.data.local.entity.ReminderEntity>

    @Query("SELECT * FROM time_entries")
    suspend fun getAllTimeEntries(): List<com.onlyflows.data.local.entity.TimeEntryEntity>

    @Query("SELECT * FROM saved_locations")
    suspend fun getAllSavedLocations(): List<com.onlyflows.data.local.entity.SavedLocationEntity>

    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachments(): List<com.onlyflows.data.local.entity.AttachmentEntity>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<com.onlyflows.data.local.entity.GoalEntity>

    @Query("SELECT * FROM goal_logs")
    suspend fun getAllGoalLogs(): List<com.onlyflows.data.local.entity.GoalLogEntity>

    // ── DateTrackerEntity ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDateTracker(entity: com.onlyflows.data.local.entity.DateTrackerEntity)

    @Query("SELECT createdAt FROM date_trackers WHERE id = :id")
    suspend fun getDateTrackerCreatedAt(id: String): LocalDateTime?

    @Query("DELETE FROM date_trackers WHERE id = :id")
    suspend fun deleteDateTracker(id: String)

    @Query("SELECT * FROM date_trackers")
    suspend fun getAllDateTrackers(): List<com.onlyflows.data.local.entity.DateTrackerEntity>
}
