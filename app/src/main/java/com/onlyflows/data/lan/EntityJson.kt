package com.onlyflows.data.lan

import com.onlyflows.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun JSONObject.putNullable(key: String, value: String?) =
    if (value == null) put(key, JSONObject.NULL) else put(key, value)

private fun JSONObject.putNullable(key: String, value: LocalDateTime?) =
    if (value == null) put(key, JSONObject.NULL) else put(key, value.toString())

private fun JSONObject.putNullable(key: String, value: LocalDate?) =
    if (value == null) put(key, JSONObject.NULL) else put(key, value.toString())

private fun JSONObject.putNullable(key: String, value: LocalTime?) =
    if (value == null) put(key, JSONObject.NULL) else put(key, value.toString())

private fun JSONObject.getString(key: String, default: String) =
    if (isNull(key)) default else optString(key, default)

private fun JSONObject.getLocalDateTime(key: String): LocalDateTime =
    runCatching { LocalDateTime.parse(getString(key)) }.getOrDefault(LocalDateTime.now())

private fun JSONObject.getLocalDateTimeOrNull(key: String): LocalDateTime? =
    if (isNull(key)) null else runCatching { LocalDateTime.parse(getString(key)) }.getOrNull()

private fun JSONObject.getLocalDateOrNull(key: String): LocalDate? =
    if (isNull(key)) null else runCatching { LocalDate.parse(getString(key)) }.getOrNull()

private fun JSONObject.getLocalTimeOrNull(key: String): LocalTime? =
    if (isNull(key)) null else runCatching { LocalTime.parse(getString(key)) }.getOrNull()

private fun JSONObject.getStringList(key: String): List<String> = runCatching {
    val arr = getJSONArray(key)
    (0 until arr.length()).map { arr.getString(it) }
}.getOrDefault(emptyList())

private fun List<String>.toJsonArray(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it) } }

// ── TaskEntity ────────────────────────────────────────────────────────────────

fun TaskEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("note", note)
    put("isCompleted", isCompleted)
    put("isArchived", isArchived)
    putNullable("projectId", projectId)
    putNullable("sectionId", sectionId)
    putNullable("dueDate", dueDate)
    putNullable("dueTime", dueTime)
    putNullable("endTime", endTime)
    putNullable("scheduledGroup", scheduledGroup)
    put("hasReminders", hasReminders)
    put("hasAttachments", hasAttachments)
    put("isStarred", isStarred)
    put("tagIds", tagIds.toJsonArray())
    put("goalIds", goalIds.toJsonArray())
    put("priority", priority)
    put("difficulty", difficulty)
    put("impact", impact)
    putNullable("startDate", startDate)
    putNullable("recurrenceJson", recurrenceJson)
    put("sortOrder", sortOrder)
    put("hasNote", hasNote)
    put("hasChecklist", hasChecklist)
    put("recurrenceDoneCount", recurrenceDoneCount)
    put("recurrenceAbandonedCount", recurrenceAbandonedCount)
    putNullable("dependsOnTaskId", dependsOnTaskId)
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
    putNullable("deletedAt", deletedAt)
    putNullable("completedAt", completedAt)
}

fun taskEntityFromJson(j: JSONObject) = TaskEntity(
    id                       = j.getString("id"),
    title                    = j.optString("title", ""),
    note                     = j.optString("note", ""),
    isCompleted              = j.optBoolean("isCompleted", false),
    isArchived               = j.optBoolean("isArchived", false),
    projectId                = if (j.isNull("projectId")) null else j.optString("projectId"),
    sectionId                = if (j.isNull("sectionId")) null else j.optString("sectionId"),
    dueDate                  = j.getLocalDateOrNull("dueDate"),
    dueTime                  = j.getLocalTimeOrNull("dueTime"),
    endTime                  = j.getLocalTimeOrNull("endTime"),
    scheduledGroup           = if (j.isNull("scheduledGroup")) null else j.optString("scheduledGroup"),
    hasReminders             = j.optBoolean("hasReminders", false),
    hasAttachments           = j.optBoolean("hasAttachments", false),
    isStarred                = j.optBoolean("isStarred", false),
    tagIds                   = j.getStringList("tagIds"),
    goalIds                  = j.getStringList("goalIds"),
    priority                 = j.optString("priority", "NONE"),
    difficulty               = j.optString("difficulty", "NONE"),
    impact                   = j.optString("impact", "NONE"),
    startDate                = j.getLocalDateOrNull("startDate"),
    recurrenceJson           = if (j.isNull("recurrenceJson")) null else j.optString("recurrenceJson"),
    sortOrder                = j.optInt("sortOrder", 0),
    hasNote                  = j.optBoolean("hasNote", false),
    hasChecklist             = j.optBoolean("hasChecklist", false),
    recurrenceDoneCount      = j.optInt("recurrenceDoneCount", 0),
    recurrenceAbandonedCount = j.optInt("recurrenceAbandonedCount", 0),
    dependsOnTaskId          = if (j.isNull("dependsOnTaskId")) null else j.optString("dependsOnTaskId"),
    createdAt                = j.getLocalDateTime("createdAt"),
    updatedAt                = j.getLocalDateTime("updatedAt"),
    deletedAt                = j.getLocalDateTimeOrNull("deletedAt"),
    completedAt              = j.getLocalDateTimeOrNull("completedAt"),
)

// ── ChecklistItemEntity ───────────────────────────────────────────────────────

fun ChecklistItemEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("taskId", taskId)
    put("title", title)
    put("isCompleted", isCompleted)
    put("sortOrder", sortOrder)
    putNullable("dueDate", dueDate)
    putNullable("startDate", startDate)
    putNullable("sectionId", sectionId)
    put("updatedAt", updatedAt.toString())
}

fun checklistItemEntityFromJson(j: JSONObject) = ChecklistItemEntity(
    id          = j.getString("id"),
    taskId      = j.getString("taskId"),
    title       = j.optString("title", ""),
    isCompleted = j.optBoolean("isCompleted", false),
    sortOrder   = j.optInt("sortOrder", 0),
    dueDate     = j.getLocalDateOrNull("dueDate"),
    startDate   = j.getLocalDateOrNull("startDate"),
    sectionId   = if (j.isNull("sectionId")) null else j.optString("sectionId"),
    updatedAt   = j.getLocalDateTime("updatedAt"),
)

// ── ProjectEntity ─────────────────────────────────────────────────────────────

fun ProjectEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("note", note)
    put("colorHex", colorHex)
    put("iconEmoji", iconEmoji)
    putNullable("coverImage", coverImage)
    put("isCompleted", isCompleted)
    putNullable("folderId", folderId)
    putNullable("parentProjectId", parentProjectId)
    put("type", type)
    put("sortOrder", sortOrder)
    putNullable("dueDate", dueDate)
    put("status", status)
    put("tagIds", tagIds.toJsonArray())
    put("isArchived", isArchived)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
}

fun projectEntityFromJson(j: JSONObject) = ProjectEntity(
    id              = j.getString("id"),
    title           = j.optString("title", ""),
    note            = j.optString("note", ""),
    colorHex        = j.optString("colorHex", "#4A90D9"),
    iconEmoji       = j.optString("iconEmoji", ""),
    coverImage      = if (j.isNull("coverImage")) null else j.optString("coverImage"),
    isCompleted     = j.optBoolean("isCompleted", false),
    folderId        = if (j.isNull("folderId")) null else j.optString("folderId"),
    parentProjectId = if (j.isNull("parentProjectId")) null else j.optString("parentProjectId"),
    type            = j.optString("type", "PROJECT"),
    sortOrder       = j.optInt("sortOrder", 0),
    dueDate         = if (j.isNull("dueDate")) null else j.optString("dueDate"),
    status          = j.optString("status", "NOT_STARTED"),
    tagIds          = j.getStringList("tagIds"),
    isArchived      = j.optBoolean("isArchived", false),
    createdAt       = j.optString("createdAt", LocalDateTime.now().toString()),
    updatedAt       = j.optString("updatedAt", LocalDateTime.now().toString()),
)

// ── SectionEntity ─────────────────────────────────────────────────────────────

fun SectionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("projectId", projectId)
    put("title", title)
    put("sortOrder", sortOrder)
    put("updatedAt", updatedAt.toString())
}

fun sectionEntityFromJson(j: JSONObject) = SectionEntity(
    id        = j.getString("id"),
    projectId = j.getString("projectId"),
    title     = j.optString("title", ""),
    sortOrder = j.optInt("sortOrder", 0),
    updatedAt = j.getLocalDateTime("updatedAt"),
)

// ── TagEntity ─────────────────────────────────────────────────────────────────

fun TagEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("colorHex", colorHex)
    put("updatedAt", updatedAt.toString())
}

fun tagEntityFromJson(j: JSONObject) = TagEntity(
    id        = j.getString("id"),
    name      = j.optString("name", ""),
    colorHex  = j.optString("colorHex", "#4A90D9"),
    updatedAt = j.getLocalDateTime("updatedAt"),
)

// ── FolderEntity ──────────────────────────────────────────────────────────────

fun FolderEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("sortOrder", sortOrder)
    put("updatedAt", updatedAt.toString())
}

fun folderEntityFromJson(j: JSONObject) = FolderEntity(
    id        = j.getString("id"),
    title     = j.optString("title", ""),
    sortOrder = j.optInt("sortOrder", 0),
    updatedAt = j.getLocalDateTime("updatedAt"),
)

// ── NoteEntity ────────────────────────────────────────────────────────────────

fun NoteEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("projectId", projectId)
    put("title", title)
    put("content", content)
    put("isPinned", isPinned)
    put("isArchived", isArchived)
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
}

fun noteEntityFromJson(j: JSONObject) = NoteEntity(
    id         = j.getString("id"),
    projectId  = j.getString("projectId"),
    title      = j.optString("title", ""),
    content    = j.optString("content", ""),
    isPinned   = j.optBoolean("isPinned", false),
    isArchived = j.optBoolean("isArchived", false),
    createdAt  = j.getLocalDateTime("createdAt"),
    updatedAt  = j.getLocalDateTime("updatedAt"),
)

// ── CommentEntity ─────────────────────────────────────────────────────────────

fun CommentEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    putNullable("taskId", taskId)
    putNullable("projectId", projectId)
    put("content", content)
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
}

fun commentEntityFromJson(j: JSONObject) = CommentEntity(
    id        = j.getString("id"),
    taskId    = if (j.isNull("taskId")) null else j.optString("taskId"),
    projectId = if (j.isNull("projectId")) null else j.optString("projectId"),
    content   = j.optString("content", ""),
    createdAt = j.getLocalDateTime("createdAt"),
    updatedAt = j.getLocalDateTime("updatedAt"),
)

// ── ReminderEntity ────────────────────────────────────────────────────────────

fun ReminderEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("taskId", taskId)
    put("scheduledAt", scheduledAt.toString())
    putNullable("locationId", locationId)
    put("updatedAt", updatedAt.toString())
}

fun reminderEntityFromJson(j: JSONObject) = ReminderEntity(
    id          = j.getString("id"),
    taskId      = j.getString("taskId"),
    scheduledAt = j.getLocalDateTime("scheduledAt"),
    locationId  = if (j.isNull("locationId")) null else j.optString("locationId"),
    updatedAt   = j.getLocalDateTime("updatedAt"),
)

// ── TimeEntryEntity ───────────────────────────────────────────────────────────

fun TimeEntryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("taskId", taskId)
    put("startedAt", startedAt.toString())
    putNullable("endedAt", endedAt)
    put("updatedAt", updatedAt.toString())
}

fun timeEntryEntityFromJson(j: JSONObject) = TimeEntryEntity(
    id        = j.getString("id"),
    taskId    = j.getString("taskId"),
    startedAt = j.getLocalDateTime("startedAt"),
    endedAt   = j.getLocalDateTimeOrNull("endedAt"),
    updatedAt = j.getLocalDateTime("updatedAt"),
)

// ── SavedLocationEntity ───────────────────────────────────────────────────────

fun SavedLocationEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("label", label)
    put("latitude", latitude)
    put("longitude", longitude)
    put("radiusMeters", radiusMeters.toDouble())
    put("updatedAt", updatedAt.toString())
}

fun savedLocationEntityFromJson(j: JSONObject) = SavedLocationEntity(
    id           = j.getString("id"),
    label        = j.optString("label", ""),
    latitude     = j.optDouble("latitude", 0.0),
    longitude    = j.optDouble("longitude", 0.0),
    radiusMeters = j.optDouble("radiusMeters", 100.0).toFloat(),
    updatedAt    = j.getLocalDateTime("updatedAt"),
)

// ── AttachmentEntity ──────────────────────────────────────────────────────────

fun AttachmentEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("taskId", taskId)
    put("fileName", fileName)
    put("mimeType", mimeType)
    put("fileSize", fileSize)
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
    putNullable("syncedFrom", syncedFrom)
    // uri is device-specific, not synced
}

fun attachmentEntityFromJson(j: JSONObject, localUri: String) = AttachmentEntity(
    id         = j.getString("id"),
    taskId     = j.getString("taskId"),
    fileName   = j.optString("fileName", ""),
    mimeType   = j.optString("mimeType", "application/octet-stream"),
    uri        = localUri,
    fileSize   = j.optLong("fileSize", 0L),
    createdAt  = j.getLocalDateTime("createdAt"),
    updatedAt  = j.getLocalDateTime("updatedAt"),
    syncedFrom = if (j.isNull("syncedFrom")) null else j.optString("syncedFrom"),
)

// ── Tombstone updatedAt helpers ───────────────────────────────────────────────

fun JSONObject.getUpdatedAt(): LocalDateTime = runCatching {
    LocalDateTime.parse(getString("updatedAt"))
}.getOrDefault(LocalDateTime.MIN)

fun projectJsonUpdatedAt(j: JSONObject): LocalDateTime = runCatching {
    LocalDateTime.parse(j.optString("updatedAt"))
}.getOrDefault(LocalDateTime.MIN)

// ── GoalEntity ────────────────────────────────────────────────────────────────

fun GoalEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("note", note)
    put("emoji", emoji)
    put("colorHex", colorHex)
    put("frequency", frequency)
    put("targetCount", targetCount)
    put("currentCount", currentCount)
    put("sortOrder", sortOrder)
    put("isCompleted", isCompleted)
    putNullable("deadline", deadline)
    put("tagsJson", tagsJson)
    putNullable("periodStart", periodStart)
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
    putNullable("deletedAt", deletedAt)
}

fun goalEntityFromJson(j: JSONObject) = GoalEntity(
    id           = j.getString("id"),
    title        = j.optString("title", ""),
    note         = j.optString("note", ""),
    emoji        = j.optString("emoji", "🎯"),
    colorHex     = j.optString("colorHex", "#4A90D9"),
    frequency    = j.optString("frequency", "DAILY"),
    targetCount  = j.optInt("targetCount", 1),
    currentCount = j.optInt("currentCount", 0),
    sortOrder    = j.optInt("sortOrder", 0),
    isCompleted  = j.optBoolean("isCompleted", false),
    deadline     = j.getLocalDateOrNull("deadline"),
    tagsJson     = j.optString("tagsJson", ""),
    periodStart  = j.getLocalDateOrNull("periodStart"),
    createdAt    = j.getLocalDateTime("createdAt"),
    updatedAt    = j.getLocalDateTime("updatedAt"),
    deletedAt    = j.getLocalDateTimeOrNull("deletedAt"),
)

// ── GoalLogEntity ─────────────────────────────────────────────────────────────

fun GoalLogEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("goalId", goalId)
    put("label", label)
    put("note", note)
    put("tagsJson", tagsJson)
    put("isStarred", isStarred)
    put("loggedAt", loggedAt.toString())
    putNullable("deletedAt", deletedAt)
}

fun goalLogEntityFromJson(j: JSONObject) = GoalLogEntity(
    id        = j.getString("id"),
    goalId    = j.getString("goalId"),
    label     = j.optString("label", ""),
    note      = j.optString("note", ""),
    tagsJson  = j.optString("tagsJson", ""),
    isStarred = j.optBoolean("isStarred", false),
    loggedAt  = j.getLocalDateTime("loggedAt"),
    deletedAt = j.getLocalDateTimeOrNull("deletedAt"),
)

// ── DateTrackerEntity ─────────────────────────────────────────────────────────

fun com.onlyflows.data.local.entity.DateTrackerEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("emoji", emoji)
    put("targetDate", targetDate.toString())
    put("category", category)
    put("isAnnual", isAnnual)
    put("sortOrder", sortOrder)
    put("updatedAt", createdAt.toString())  // createdAt used as updatedAt for sync
    put("createdAt", createdAt.toString())
    putNullable("deletedAt", deletedAt)
}

fun dateTrackerEntityFromJson(j: JSONObject) = com.onlyflows.data.local.entity.DateTrackerEntity(
    id         = j.getString("id"),
    name       = j.optString("name", ""),
    emoji      = j.optString("emoji", "📅"),
    targetDate = runCatching { java.time.LocalDate.parse(j.getString("targetDate")) }.getOrDefault(java.time.LocalDate.now()),
    category   = j.optString("category", "COUNTDOWN"),
    isAnnual   = j.optBoolean("isAnnual", false),
    sortOrder  = j.optInt("sortOrder", 0),
    createdAt  = j.getLocalDateTime("createdAt"),
    deletedAt  = j.getLocalDateTimeOrNull("deletedAt"),
)

// ── ChecklistSectionEntity ────────────────────────────────────────────────────

fun com.onlyflows.data.local.entity.ChecklistSectionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("taskId", taskId)
    put("title", title)
    put("sortOrder", sortOrder)
    put("updatedAt", updatedAt.toString())
}

fun checklistSectionEntityFromJson(j: JSONObject) = com.onlyflows.data.local.entity.ChecklistSectionEntity(
    id        = j.getString("id"),
    taskId    = j.getString("taskId"),
    title     = j.optString("title", ""),
    sortOrder = j.optInt("sortOrder", 0),
    updatedAt = runCatching { java.time.LocalDateTime.parse(j.getString("updatedAt")) }.getOrDefault(java.time.LocalDateTime.now()),
)

// ── ChecklistItemReminderEntity ───────────────────────────────────────────────

fun com.onlyflows.data.local.entity.ChecklistItemReminderEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("checklistItemId", checklistItemId)
    put("scheduledAt", scheduledAt.toString())
    put("updatedAt", updatedAt.toString())
}

fun checklistItemReminderEntityFromJson(j: JSONObject) = com.onlyflows.data.local.entity.ChecklistItemReminderEntity(
    id              = j.getString("id"),
    checklistItemId = j.getString("checklistItemId"),
    scheduledAt     = runCatching { java.time.LocalDateTime.parse(j.getString("scheduledAt")) }.getOrDefault(java.time.LocalDateTime.now()),
    updatedAt       = runCatching { java.time.LocalDateTime.parse(j.getString("updatedAt")) }.getOrDefault(java.time.LocalDateTime.now()),
)
