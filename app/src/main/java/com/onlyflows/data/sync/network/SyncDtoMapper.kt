package com.onlyflows.data.sync.network

import com.onlyflows.data.local.entity.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ── Entity → DTO ──────────────────────────────────────────────────────────────

fun TaskEntity.toDto() = TaskDto(
    id                     = id,
    title                  = title,
    note                   = note,
    isCompleted            = isCompleted,
    isArchived             = isArchived,
    projectId              = projectId,
    sectionId              = sectionId,
    dueDate                = dueDate?.toString(),
    dueTime                = dueTime?.toString(),
    endTime                = endTime?.toString(),
    scheduledGroup         = scheduledGroup,
    hasReminders           = hasReminders,
    hasAttachments         = hasAttachments,
    isStarred              = isStarred,
    tagIds                 = tagIds,
    priority               = priority,
    startDate              = startDate?.toString(),
    recurrenceJson         = recurrenceJson,
    sortOrder              = sortOrder,
    hasNote                = hasNote,
    hasChecklist           = hasChecklist,
    recurrenceDoneCount    = recurrenceDoneCount,
    recurrenceAbandonedCount = recurrenceAbandonedCount,
    dependsOnTaskId        = dependsOnTaskId,
    createdAt              = createdAt.toString(),
    updatedAt              = updatedAt.toString(),
    deletedAt              = deletedAt?.toString(),
)

fun ProjectEntity.toDto() = ProjectDto(
    id              = id,
    title           = title,
    note            = note,
    colorHex        = colorHex,
    iconEmoji       = iconEmoji,
    isCompleted     = isCompleted,
    folderId        = folderId,
    parentProjectId = parentProjectId,
    type            = type,
    sortOrder       = sortOrder,
    dueDate         = dueDate,
    status          = status,
    isArchived      = isArchived,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
)

fun SectionEntity.toDto() = SectionDto(
    id        = id,
    projectId = projectId,
    title     = title,
    sortOrder = sortOrder,
    updatedAt = updatedAt.toString(),
)

fun ChecklistItemEntity.toDto() = ChecklistItemDto(
    id          = id,
    taskId      = taskId,
    title       = title,
    isCompleted = isCompleted,
    sortOrder   = sortOrder,
    dueDate     = dueDate?.toString(),
    startDate   = startDate?.toString(),
    updatedAt   = updatedAt.toString(),
)

fun TagEntity.toDto() = TagDto(
    id        = id,
    name      = name,
    colorHex  = colorHex,
    updatedAt = updatedAt.toString(),
)

fun FolderEntity.toDto() = FolderDto(
    id        = id,
    title     = title,
    sortOrder = sortOrder,
    updatedAt = updatedAt.toString(),
)

fun NoteEntity.toDto() = NoteDto(
    id        = id,
    projectId = projectId,
    title     = title,
    content   = content,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CommentEntity.toDto() = CommentDto(
    id        = id,
    taskId    = taskId,
    projectId = projectId,
    content   = content,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun ReminderEntity.toDto() = ReminderDto(
    id          = id,
    taskId      = taskId,
    scheduledAt = scheduledAt.toString(),
    locationId  = locationId,
    updatedAt   = updatedAt.toString(),
)

fun AttachmentEntity.toDto() = AttachmentDto(
    id         = id,
    taskId     = taskId,
    fileName   = fileName,
    mimeType   = mimeType,
    fileSize   = fileSize,
    createdAt  = createdAt.toString(),
    updatedAt  = updatedAt.toString(),
    syncedFrom = syncedFrom,
)

fun TimeEntryEntity.toDto() = TimeEntryDto(
    id        = id,
    taskId    = taskId,
    startedAt = startedAt.toString(),
    endedAt   = endedAt?.toString(),
    updatedAt = updatedAt.toString(),
)

fun SavedLocationEntity.toDto() = SavedLocationDto(
    id           = id,
    label        = label,
    latitude     = latitude,
    longitude    = longitude,
    radiusMeters = radiusMeters,
    updatedAt    = updatedAt.toString(),
)

// ── DTO → Entity ──────────────────────────────────────────────────────────────

fun TaskDto.toEntity() = TaskEntity(
    id                     = id,
    title                  = title,
    note                   = note,
    isCompleted            = isCompleted,
    isArchived             = isArchived,
    projectId              = projectId,
    sectionId              = sectionId,
    dueDate                = dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    dueTime                = dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    endTime                = endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    scheduledGroup         = scheduledGroup,
    hasReminders           = hasReminders,
    hasAttachments         = hasAttachments,
    isStarred              = isStarred,
    tagIds                 = tagIds,
    priority               = priority,
    startDate              = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    recurrenceJson         = recurrenceJson,
    sortOrder              = sortOrder,
    hasNote                = hasNote,
    hasChecklist           = hasChecklist,
    recurrenceDoneCount    = recurrenceDoneCount,
    recurrenceAbandonedCount = recurrenceAbandonedCount,
    dependsOnTaskId        = dependsOnTaskId,
    createdAt              = runCatching { LocalDateTime.parse(createdAt) }.getOrDefault(LocalDateTime.now()),
    updatedAt              = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
    deletedAt              = deletedAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
)

fun ProjectDto.toEntity() = ProjectEntity(
    id              = id,
    title           = title,
    note            = note,
    colorHex        = colorHex,
    iconEmoji       = iconEmoji,
    isCompleted     = isCompleted,
    folderId        = folderId,
    parentProjectId = parentProjectId,
    type            = type,
    sortOrder       = sortOrder,
    dueDate         = dueDate,
    status          = status,
    isArchived      = isArchived,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
)

fun SectionDto.toEntity() = SectionEntity(
    id        = id,
    projectId = projectId,
    title     = title,
    sortOrder = sortOrder,
    updatedAt = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun ChecklistItemDto.toEntity() = ChecklistItemEntity(
    id          = id,
    taskId      = taskId,
    title       = title,
    isCompleted = isCompleted,
    sortOrder   = sortOrder,
    dueDate     = dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    startDate   = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    updatedAt   = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun TagDto.toEntity() = TagEntity(
    id        = id,
    name      = name,
    colorHex  = colorHex,
    updatedAt = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun FolderDto.toEntity() = FolderEntity(
    id        = id,
    title     = title,
    sortOrder = sortOrder,
    updatedAt = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun NoteDto.toEntity() = NoteEntity(
    id        = id,
    projectId = projectId,
    title     = title,
    content   = content,
    createdAt = runCatching { LocalDateTime.parse(createdAt) }.getOrDefault(LocalDateTime.now()),
    updatedAt = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun CommentDto.toEntity() = CommentEntity(
    id        = id,
    taskId    = taskId,
    projectId = projectId,
    content   = content,
    createdAt = runCatching { LocalDateTime.parse(createdAt) }.getOrDefault(LocalDateTime.now()),
    updatedAt = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun ReminderDto.toEntity() = ReminderEntity(
    id          = id,
    taskId      = taskId,
    scheduledAt = runCatching { LocalDateTime.parse(scheduledAt) }.getOrDefault(LocalDateTime.now()),
    locationId  = locationId,
    updatedAt   = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun AttachmentDto.toEntity() = AttachmentEntity(
    id         = id,
    taskId     = taskId,
    fileName   = fileName,
    mimeType   = mimeType,
    uri        = "",   // will be filled in by AttachmentSyncer after downloading the file
    fileSize   = fileSize,
    createdAt  = runCatching { LocalDateTime.parse(createdAt) }.getOrDefault(LocalDateTime.now()),
    updatedAt  = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
    syncedFrom = syncedFrom,
)

fun TimeEntryDto.toEntity() = TimeEntryEntity(
    id        = id,
    taskId    = taskId,
    startedAt = runCatching { LocalDateTime.parse(startedAt) }.getOrDefault(LocalDateTime.now()),
    endedAt   = endedAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
    updatedAt = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)

fun SavedLocationDto.toEntity() = SavedLocationEntity(
    id           = id,
    label        = label,
    latitude     = latitude,
    longitude    = longitude,
    radiusMeters = radiusMeters,
    updatedAt    = runCatching { LocalDateTime.parse(updatedAt) }.getOrDefault(LocalDateTime.now()),
)
