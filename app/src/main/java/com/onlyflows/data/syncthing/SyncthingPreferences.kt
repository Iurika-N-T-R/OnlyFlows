package com.onlyflows.data.syncthing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.syncthingDataStore by preferencesDataStore("syncthing_prefs")

class SyncthingPreferences(private val context: Context) {

    private object Keys {
        val FOLDER_URI      = stringPreferencesKey("syncthing_folder_uri")
        val ENABLED         = booleanPreferencesKey("syncthing_enabled")
        val DEVICE_ID       = stringPreferencesKey("syncthing_device_id")
        val LAST_SCANNED_AT = longPreferencesKey("syncthing_last_scanned_ms")
    }

    val folderUri: Flow<String>  = context.syncthingDataStore.data.map { it[Keys.FOLDER_URI] ?: "" }
    val enabled: Flow<Boolean>   = context.syncthingDataStore.data.map { it[Keys.ENABLED] ?: false }
    val lastScannedAt: Flow<Long> = context.syncthingDataStore.data.map { it[Keys.LAST_SCANNED_AT] ?: 0L }

    val deviceId: Flow<String> = context.syncthingDataStore.data.map { prefs ->
        prefs[Keys.DEVICE_ID] ?: run {
            val id = UUID.randomUUID().toString()
            id
        }
    }

    suspend fun getDeviceIdNow(): String {
        val prefs = context.syncthingDataStore.data.first()
        val existing = prefs[Keys.DEVICE_ID]
        if (existing != null) return existing
        val id = UUID.randomUUID().toString()
        context.syncthingDataStore.edit { it[Keys.DEVICE_ID] = id }
        return id
    }

    suspend fun setFolderUri(uri: String) {
        context.syncthingDataStore.edit { it[Keys.FOLDER_URI] = uri }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.syncthingDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setLastScannedAt(ms: Long) {
        context.syncthingDataStore.edit { it[Keys.LAST_SCANNED_AT] = ms }
    }
}
