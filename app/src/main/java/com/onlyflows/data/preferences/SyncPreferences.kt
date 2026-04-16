package com.onlyflows.data.preferences

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.util.UUID

private val Context.syncDataStore by preferencesDataStore("sync_prefs")

class SyncPreferences(private val context: Context) {

    private object Keys {
        val DEVICE_ID               = stringPreferencesKey("sync_device_id")
        val DEVICE_NAME             = stringPreferencesKey("sync_device_name")
        val SYNC_ENABLED            = booleanPreferencesKey("sync_enabled")
        val SYNC_PORT               = intPreferencesKey("sync_port")
        val SYNC_ATTACHMENTS        = booleanPreferencesKey("sync_attachments")
        val MAX_ATTACHMENT_SIZE_MB  = intPreferencesKey("max_attachment_size_mb")
        val TRUSTED_DEVICE_IDS      = stringPreferencesKey("trusted_device_ids")
    }

    val syncEnabled: Flow<Boolean>          = context.syncDataStore.data.map { it[Keys.SYNC_ENABLED] ?: false }
    val syncPort: Flow<Int>                 = context.syncDataStore.data.map { it[Keys.SYNC_PORT] ?: 47120 }
    val syncAttachments: Flow<Boolean>      = context.syncDataStore.data.map { it[Keys.SYNC_ATTACHMENTS] ?: true }
    val maxAttachmentSizeMb: Flow<Int>      = context.syncDataStore.data.map { it[Keys.MAX_ATTACHMENT_SIZE_MB] ?: 50 }
    val deviceName: Flow<String>            = context.syncDataStore.data.map {
        it[Keys.DEVICE_NAME] ?: Build.MODEL
    }

    /** Returns a stable UUID for this device, generating one on first access. */
    val deviceId: Flow<String> = context.syncDataStore.data.map { prefs ->
        prefs[Keys.DEVICE_ID] ?: run {
            val id = UUID.randomUUID().toString()
            context.syncDataStore.edit { it[Keys.DEVICE_ID] = id }
            id
        }
    }

    suspend fun getDeviceIdNow(): String {
        return deviceId.first()
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.syncDataStore.edit { it[Keys.SYNC_ENABLED] = enabled }
    }

    suspend fun setSyncPort(port: Int) {
        context.syncDataStore.edit { it[Keys.SYNC_PORT] = port }
    }

    suspend fun setDeviceName(name: String) {
        context.syncDataStore.edit { it[Keys.DEVICE_NAME] = name }
    }

    suspend fun setSyncAttachments(enabled: Boolean) {
        context.syncDataStore.edit { it[Keys.SYNC_ATTACHMENTS] = enabled }
    }

    suspend fun setMaxAttachmentSizeMb(mb: Int) {
        context.syncDataStore.edit { it[Keys.MAX_ATTACHMENT_SIZE_MB] = mb }
    }

    // ── Trusted peers whitelist ───────────────────────────────────────────────

    val trustedDeviceIds: Flow<Set<String>> = context.syncDataStore.data.map { prefs ->
        val json = prefs[Keys.TRUSTED_DEVICE_IDS] ?: "[]"
        val arr = JSONArray(json)
        (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
    }

    suspend fun addTrustedDevice(deviceId: String) {
        context.syncDataStore.edit { prefs ->
            val current = trustedDeviceIds.first().toMutableSet()
            current.add(deviceId)
            prefs[Keys.TRUSTED_DEVICE_IDS] = JSONArray(current.toList()).toString()
        }
    }

    suspend fun removeTrustedDevice(deviceId: String) {
        context.syncDataStore.edit { prefs ->
            val current = trustedDeviceIds.first().toMutableSet()
            current.remove(deviceId)
            prefs[Keys.TRUSTED_DEVICE_IDS] = JSONArray(current.toList()).toString()
        }
    }

    suspend fun isTrusted(deviceId: String): Boolean = trustedDeviceIds.first().contains(deviceId)
}
