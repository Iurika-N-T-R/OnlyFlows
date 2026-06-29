package com.onlyflows.data.lan

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.lanSyncDataStore by preferencesDataStore("lan_sync_prefs")

class LanSyncPreferences(private val context: Context) {
    private val DEVICE_ID   = stringPreferencesKey("lan_device_id")
    private val DEVICE_NAME = stringPreferencesKey("lan_device_name")
    private val ENABLED     = booleanPreferencesKey("lan_sync_enabled")

    val enabled: Flow<Boolean>     = context.lanSyncDataStore.data.map { it[ENABLED]     ?: false }
    val deviceName: Flow<String>   = context.lanSyncDataStore.data.map { it[DEVICE_NAME] ?: Build.MODEL }

    suspend fun getDeviceId(): String {
        val existing = context.lanSyncDataStore.data.first()[DEVICE_ID]
        if (existing != null) return existing
        val id = UUID.randomUUID().toString()
        context.lanSyncDataStore.edit { it[DEVICE_ID] = id }
        return id
    }

    suspend fun getDeviceName(): String = context.lanSyncDataStore.data.first()[DEVICE_NAME] ?: Build.MODEL

    suspend fun setEnabled(enabled: Boolean) = context.lanSyncDataStore.edit { it[ENABLED] = enabled }
    suspend fun setDeviceName(name: String)  = context.lanSyncDataStore.edit { it[DEVICE_NAME] = name }
}
