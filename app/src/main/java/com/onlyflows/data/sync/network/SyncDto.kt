package com.onlyflows.data.sync.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── LocalDateTime serializer ──────────────────────────────────────────────────

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeString(value.format(formatter))
    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString(), formatter)
}

typealias LDT = @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime

// ── Handshake ─────────────────────────────────────────────────────────────────

@Serializable
data class PingResponse(
    val deviceId: String,
    val displayName: String,
    val serverVersion: Int = 1,
)

// ── Manifest ──────────────────────────────────────────────────────────────────

@Serializable
data class ManifestRow(
    val entityType: String,
    val maxUpdatedAt: String?,  // ISO-8601 or null if table is empty
)

@Serializable
data class TombstoneDto(
    val entityType: String,
    val entityId: String,
    val deletedAt: String,
)

@Serializable
data class ManifestResponse(
    val deviceId: String,
    val generatedAt: String,
    val tables: List<ManifestRow>,
    val tombstones: List<TombstoneDto>,
)

// ── Pull ──────────────────────────────────────────────────────────────────────

@Serializable
data class PullQuery(val entityType: String, val since: String)

@Serializable
data class PullRequest(val requestingDeviceId: String, val queries: List<PullQuery>)

@Serializable
data class PullResponse(
    val tasks: List<TaskDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val sections: List<SectionDto> = emptyList(),
    val checklistItems: List<ChecklistItemDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val folders: List<FolderDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val comments: List<CommentDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList(),
    val timeEntries: List<TimeEntryDto> = emptyList(),
    val savedLocations: List<SavedLocationDto> = emptyList(),
)

// ── Entity DTOs ───────────────────────────────────────────────────────────────

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val note: String,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val projectId: String?,
    val sectionId: String?,
    val dueDate: String?,
    val dueTime: String?,
    val endTime: String?,
    val scheduledGroup: String?,
    val hasReminders: Boolean,
    val hasAttachments: Boolean,
    val isStarred: Boolean,
    val tagIds: List<String>,
    val priority: String,
    val startDate: String?,
    val recurrenceJson: String?,
    val sortOrder: Int,
    val hasNote: Boolean,
    val hasChecklist: Boolean,
    val recurrenceDoneCount: Int,
    val recurrenceAbandonedCount: Int,
    val dependsOnTaskId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
data class ProjectDto(
    val id: String,
    val title: String,
    val note: String,
    val colorHex: String,
    val iconEmoji: String,
    val isCompleted: Boolean,
    val folderId: String?,
    val parentProjectId: String?,
    val type: String,
    val sortOrder: Int,
    val dueDate: String?,
    val status: String,
    val isArchived: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SectionDto(
    val id: String,
    val projectId: String,
    val title: String,
    val sortOrder: Int,
    val updatedAt: String,
)

@Serializable
data class ChecklistItemDto(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val dueDate: String?,
    val startDate: String?,
    val updatedAt: String,
)

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val updatedAt: String,
)

@Serializable
data class FolderDto(
    val id: String,
    val title: String,
    val sortOrder: Int,
    val updatedAt: String,
)

@Serializable
data class NoteDto(
    val id: String,
    val projectId: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CommentDto(
    val id: String,
    val taskId: String?,
    val projectId: String?,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ReminderDto(
    val id: String,
    val taskId: String,
    val scheduledAt: String,
    val locationId: String?,
    val updatedAt: String,
)

@Serializable
data class AttachmentDto(
    val id: String,
    val taskId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: String,
    val updatedAt: String,
    val syncedFrom: String?,
)

@Serializable
data class TimeEntryDto(
    val id: String,
    val taskId: String,
    val startedAt: String,
    val endedAt: String?,
    val updatedAt: String,
)

@Serializable
data class SavedLocationDto(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val updatedAt: String,
)
