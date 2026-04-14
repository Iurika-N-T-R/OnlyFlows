package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

class NotificationPreferences(private val context: Context) {

    private val KEY_SOUND     = booleanPreferencesKey("sound_enabled")
    private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    private val KEY_BYPASS_DND = booleanPreferencesKey("bypass_dnd")

    val soundEnabled: Flow<Boolean>     = context.notifDataStore.data.map { it[KEY_SOUND]      ?: true }
    val vibrationEnabled: Flow<Boolean> = context.notifDataStore.data.map { it[KEY_VIBRATION]  ?: true }
    val bypassDnd: Flow<Boolean>        = context.notifDataStore.data.map { it[KEY_BYPASS_DND] ?: false }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.notifDataStore.edit { it[KEY_SOUND] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.notifDataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun setBypassDnd(enabled: Boolean) {
        context.notifDataStore.edit { it[KEY_BYPASS_DND] = enabled }
    }

    // Synchronous snapshot for use in BroadcastReceiver (runs on IO thread)
    suspend fun snapshot(): Triple<Boolean, Boolean, Boolean> {
        val prefs = context.notifDataStore.data.first()
        return Triple(
            prefs[KEY_SOUND]      ?: true,
            prefs[KEY_VIBRATION]  ?: true,
            prefs[KEY_BYPASS_DND] ?: false,
        )
    }
}
