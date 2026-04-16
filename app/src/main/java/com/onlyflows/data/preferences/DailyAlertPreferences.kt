package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dailyAlertDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_alert_prefs")

class DailyAlertPreferences(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("daily_alert_enabled")
    private val KEY_TIMES   = stringSetPreferencesKey("daily_alert_times")
    private val KEY_DAYS    = stringSetPreferencesKey("daily_alert_days")

    val enabled: Flow<Boolean>      = context.dailyAlertDataStore.data.map { it[KEY_ENABLED] ?: false }
    val times: Flow<Set<String>>    = context.dailyAlertDataStore.data.map { it[KEY_TIMES]   ?: emptySet() }
    val days: Flow<Set<String>>     = context.dailyAlertDataStore.data.map { it[KEY_DAYS]    ?: emptySet() }

    suspend fun setEnabled(enabled: Boolean) {
        context.dailyAlertDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun addTime(hhmm: String) {
        context.dailyAlertDataStore.edit { prefs ->
            val current = prefs[KEY_TIMES] ?: emptySet()
            prefs[KEY_TIMES] = current + hhmm
        }
    }

    suspend fun removeTime(hhmm: String) {
        context.dailyAlertDataStore.edit { prefs ->
            val current = prefs[KEY_TIMES] ?: emptySet()
            prefs[KEY_TIMES] = current - hhmm
        }
    }

    suspend fun toggleDay(dayName: String) {
        context.dailyAlertDataStore.edit { prefs ->
            val current = prefs[KEY_DAYS] ?: emptySet()
            prefs[KEY_DAYS] = if (dayName in current) current - dayName else current + dayName
        }
    }

    suspend fun snapshot(): Triple<Boolean, Set<String>, Set<String>> {
        val prefs = context.dailyAlertDataStore.data.first()
        return Triple(
            prefs[KEY_ENABLED] ?: false,
            prefs[KEY_TIMES]   ?: emptySet(),
            prefs[KEY_DAYS]    ?: emptySet(),
        )
    }
}
