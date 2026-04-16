package com.onlyflows.data.sync.network

import android.util.Log
import com.onlyflows.domain.sync.PeerInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val TAG = "SyncClient"
const val HEADER_DEVICE_ID = "X-Device-Id"

class SyncClient(private val thisDeviceId: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        engine {
            requestTimeout = 30_000
        }
        defaultRequest {
            header(HEADER_DEVICE_ID, thisDeviceId)
        }
    }

    private fun PeerInfo.baseUrl() = "http://${host}:${port}/api/v1"

    suspend fun ping(peer: PeerInfo): PingResponse? = runCatching {
        client.get("${peer.baseUrl()}/ping").body<PingResponse>()
    }.onFailure { Log.w(TAG, "ping failed for ${peer.host}", it) }.getOrNull()

    suspend fun fetchManifest(peer: PeerInfo): ManifestResponse? = runCatching {
        client.get("${peer.baseUrl()}/manifest").body<ManifestResponse>()
    }.onFailure { Log.w(TAG, "manifest failed for ${peer.host}", it) }.getOrNull()

    suspend fun pull(peer: PeerInfo, request: PullRequest): PullResponse? = runCatching {
        client.post("${peer.baseUrl()}/pull") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<PullResponse>()
    }.onFailure { Log.w(TAG, "pull failed for ${peer.host}", it) }.getOrNull()

    suspend fun downloadFile(peer: PeerInfo, attachmentId: String): ByteArray? = runCatching {
        val response = client.get("${peer.baseUrl()}/files/$attachmentId")
        if (response.status.isSuccess()) response.body<ByteArray>() else null
    }.onFailure { Log.w(TAG, "file download failed for $attachmentId", it) }.getOrNull()

    fun close() = client.close()
}
