package com.onlyflows.data.sync.network

import android.content.ContentResolver
import android.util.Log
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.dao.SyncDao
import com.onlyflows.data.preferences.SyncPreferences
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

private const val TAG = "SyncServer"

class SyncServer(
    private val db: AppDatabase,
    private val syncDao: SyncDao,
    private val syncPreferences: SyncPreferences,
    private val contentResolver: ContentResolver,
) {
    private var server: ApplicationEngine? = null

    suspend fun start() {
        if (server != null) return
        val port = syncPreferences.syncPort.first()
        val deviceId = syncPreferences.getDeviceIdNow()
        val deviceName = syncPreferences.deviceName.first()

        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            intercept(ApplicationCallPipeline.Plugins) {
                val path = call.request.uri
                if (!path.endsWith("/ping")) {
                    val callDeviceId = call.request.headers[HEADER_DEVICE_ID]
                    if (callDeviceId == null || !syncPreferences.isTrusted(callDeviceId)) {
                        call.respond(HttpStatusCode.Forbidden, "Device not authorized")
                        finish()
                    }
                }
            }
            routing {
                get("/api/v1/ping") {
                    call.respond(PingResponse(deviceId = deviceId, displayName = deviceName))
                }

                get("/api/v1/manifest") {
                    val now = LocalDateTime.now().toString()
                    val tables = listOf(
                        ManifestRow("tasks",          syncDao.getTasksMaxUpdatedAt()?.toString()),
                        ManifestRow("projects",       syncDao.getProjectsMaxUpdatedAt()?.toString()),
                        ManifestRow("sections",       syncDao.getSectionsMaxUpdatedAt()?.toString()),
                        ManifestRow("checklist_items",syncDao.getChecklistItemsMaxUpdatedAt()?.toString()),
                        ManifestRow("tags",           syncDao.getTagsMaxUpdatedAt()?.toString()),
                        ManifestRow("folders",        syncDao.getFoldersMaxUpdatedAt()?.toString()),
                        ManifestRow("notes",          syncDao.getNotesMaxUpdatedAt()?.toString()),
                        ManifestRow("comments",       syncDao.getCommentsMaxUpdatedAt()?.toString()),
                        ManifestRow("reminders",      syncDao.getRemindersMaxUpdatedAt()?.toString()),
                        ManifestRow("attachments",    syncDao.getAttachmentsMaxUpdatedAt()?.toString()),
                        ManifestRow("time_entries",   syncDao.getTimeEntriesMaxUpdatedAt()?.toString()),
                        ManifestRow("saved_locations",syncDao.getSavedLocationsMaxUpdatedAt()?.toString()),
                    )
                    val tombstones = syncDao.getAllTombstones().map {
                        TombstoneDto(it.entityType, it.entityId, it.deletedAt.toString())
                    }
                    call.respond(ManifestResponse(deviceId, now, tables, tombstones))
                }

                post("/api/v1/pull") {
                    val request = call.receive<PullRequest>()
                    val response = buildPullResponse(request)
                    call.respond(response)
                }

                get("/api/v1/files/{attachmentId}") {
                    val attachmentId = call.parameters["attachmentId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val attachment = db.attachmentDao().getById(attachmentId)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    if (attachment.uri.isBlank())
                        return@get call.respond(HttpStatusCode.NotFound)
                    try {
                        val uri = android.net.Uri.parse(attachment.uri)
                        val stream = contentResolver.openInputStream(uri)
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                        call.respondOutputStream(
                            contentType = ContentType.parse(attachment.mimeType),
                            status      = HttpStatusCode.OK,
                        ) {
                            stream.use { it.copyTo(this) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "File serve error for $attachmentId", e)
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }.start(wait = false)
        Log.d(TAG, "Sync server started on port $port")
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
        server = null
        Log.d(TAG, "Sync server stopped")
    }

    private suspend fun buildPullResponse(request: PullRequest): PullResponse {
        var tasks          = emptyList<TaskDto>()
        var projects       = emptyList<ProjectDto>()
        var sections       = emptyList<SectionDto>()
        var checklistItems = emptyList<ChecklistItemDto>()
        var tags           = emptyList<TagDto>()
        var folders        = emptyList<FolderDto>()
        var notes          = emptyList<NoteDto>()
        var comments       = emptyList<CommentDto>()
        var reminders      = emptyList<ReminderDto>()
        var attachments    = emptyList<AttachmentDto>()
        var timeEntries    = emptyList<TimeEntryDto>()
        var savedLocations = emptyList<SavedLocationDto>()

        for (query in request.queries) {
            val since = runCatching { LocalDateTime.parse(query.since) }.getOrDefault(
                LocalDateTime.MIN
            )
            when (query.entityType) {
                "tasks"           -> tasks          = syncDao.getTasksChangedSince(since).map { it.toDto() }
                "projects"        -> projects       = syncDao.getProjectsChangedSince(since).map { it.toDto() }
                "sections"        -> sections       = syncDao.getSectionsChangedSince(since).map { it.toDto() }
                "checklist_items" -> checklistItems = syncDao.getChecklistItemsChangedSince(since).map { it.toDto() }
                "tags"            -> tags           = syncDao.getTagsChangedSince(since).map { it.toDto() }
                "folders"         -> folders        = syncDao.getFoldersChangedSince(since).map { it.toDto() }
                "notes"           -> notes          = syncDao.getNotesChangedSince(since).map { it.toDto() }
                "comments"        -> comments       = syncDao.getCommentsChangedSince(since).map { it.toDto() }
                "reminders"       -> reminders      = syncDao.getRemindersChangedSince(since).map { it.toDto() }
                "attachments"     -> attachments    = syncDao.getAttachmentsChangedSince(since).map { it.toDto() }
                "time_entries"    -> timeEntries    = syncDao.getTimeEntriesChangedSince(since).map { it.toDto() }
                "saved_locations" -> savedLocations = syncDao.getSavedLocationsChangedSince(since).map { it.toDto() }
            }
        }
        return PullResponse(
            tasks, projects, sections, checklistItems, tags, folders,
            notes, comments, reminders, attachments, timeEntries, savedLocations,
        )
    }
}
