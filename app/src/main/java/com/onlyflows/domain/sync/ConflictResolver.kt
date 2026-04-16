package com.onlyflows.domain.sync

import com.onlyflows.data.local.dao.SyncDao
import java.time.LocalDateTime

enum class ConflictResult { ACCEPT, SKIP }

class ConflictResolver {

    /**
     * Decides whether to accept an incoming entity or skip it.
     *
     * Rules (in priority order):
     * 1. If a tombstone exists for this entity → SKIP (tombstone always wins).
     * 2. If no local copy exists → ACCEPT.
     * 3. If incoming.updatedAt > local.updatedAt → ACCEPT.
     * 4. Otherwise → SKIP.
     */
    suspend fun resolve(
        entityId: String,
        entityType: String,
        incomingUpdatedAt: LocalDateTime,
        syncDao: SyncDao,
    ): ConflictResult {
        if (syncDao.isTombstoned(entityId, entityType)) return ConflictResult.SKIP
        val localUpdatedAt = getLocalUpdatedAt(entityId, entityType, syncDao)
            ?: return ConflictResult.ACCEPT
        return if (incomingUpdatedAt > localUpdatedAt) ConflictResult.ACCEPT else ConflictResult.SKIP
    }

    private suspend fun getLocalUpdatedAt(
        entityId: String,
        entityType: String,
        syncDao: SyncDao,
    ): LocalDateTime? = when (entityType) {
        "tasks"           -> syncDao.getTaskUpdatedAt(entityId)
        "projects"        -> syncDao.getProjectUpdatedAt(entityId)
        "sections"        -> syncDao.getSectionUpdatedAt(entityId)
        "checklist_items" -> syncDao.getChecklistItemUpdatedAt(entityId)
        "tags"            -> syncDao.getTagUpdatedAt(entityId)
        "folders"         -> syncDao.getFolderUpdatedAt(entityId)
        "notes"           -> syncDao.getNoteUpdatedAt(entityId)
        "comments"        -> syncDao.getCommentUpdatedAt(entityId)
        "reminders"       -> syncDao.getReminderUpdatedAt(entityId)
        "attachments"     -> syncDao.getAttachmentUpdatedAt(entityId)
        "time_entries"    -> syncDao.getTimeEntryUpdatedAt(entityId)
        "saved_locations" -> syncDao.getSavedLocationUpdatedAt(entityId)
        else              -> null
    }
}
