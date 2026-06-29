package com.onlyflows.data.lan

import com.onlyflows.data.local.entity.*
import com.onlyflows.data.syncthing.*
import java.time.LocalDateTime

class LanSyncWriter(private val coordinator: LanSyncCoordinator) {

    fun writeTask(entity: TaskEntity)               = coordinator.enqueue("tasks", entity.toJson())
    fun writeProject(entity: ProjectEntity)         = coordinator.enqueue("projects", entity.toJson())
    fun writeSection(entity: SectionEntity)         = coordinator.enqueue("sections", entity.toJson())
    fun writeChecklistItem(entity: ChecklistItemEntity) = coordinator.enqueue("checklistItems", entity.toJson())
    fun writeTag(entity: TagEntity)                 = coordinator.enqueue("tags", entity.toJson())
    fun writeFolder(entity: FolderEntity)           = coordinator.enqueue("folders", entity.toJson())
    fun writeNote(entity: NoteEntity)               = coordinator.enqueue("notes", entity.toJson())
    fun writeComment(entity: CommentEntity)         = coordinator.enqueue("comments", entity.toJson())
    fun writeReminder(entity: ReminderEntity)       = coordinator.enqueue("reminders", entity.toJson())
    fun writeTimeEntry(entity: TimeEntryEntity)     = coordinator.enqueue("timeEntries", entity.toJson())
    fun writeSavedLocation(entity: SavedLocationEntity) = coordinator.enqueue("savedLocations", entity.toJson())
    fun writeGoal(entity: GoalEntity)               = coordinator.enqueue("goals", entity.toJson())
    fun writeGoalLog(entity: GoalLogEntity)         = coordinator.enqueue("goalLogs", entity.toJson())
    fun writeDateTracker(entity: DateTrackerEntity) = coordinator.enqueue("dateTrackers", entity.toJson())
    fun writeAttachmentMeta(entity: AttachmentEntity) = coordinator.enqueue("attachments", entity.toJson())

    fun writeTombstone(entityType: String, entityId: String, deletedAt: LocalDateTime) =
        coordinator.enqueueTombstone(entityType, entityId, deletedAt)

    // Attachments: physical file copy not supported in LAN sync v1
    // ponytail: skipped file transfer, add when needed
    suspend fun copyAttachmentFile(id: String, fileName: String, sourceUri: android.net.Uri): String? = null
}
