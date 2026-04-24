package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {

    private val THEME_MODE    = stringPreferencesKey("theme_mode")
    private val USE_24H_FORMAT = booleanPreferencesKey("use_24h_format")
    private val FONT_SCALE    = floatPreferencesKey("font_scale")

    val themeMode: Flow<ThemeMode> = context.appDataStore.data
        .map { prefs -> ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { prefs -> prefs[THEME_MODE] = mode.name }
    }

    val use24hFormat: Flow<Boolean> = context.appDataStore.data
        .map { prefs -> prefs[USE_24H_FORMAT] ?: true }

    suspend fun setUse24hFormat(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[USE_24H_FORMAT] = enabled }
    }

    val fontScale: Flow<Float> = context.appDataStore.data
        .map { prefs -> prefs[FONT_SCALE] ?: 1.0f }

    suspend fun setFontScale(scale: Float) {
        context.appDataStore.edit { prefs -> prefs[FONT_SCALE] = scale }
    }

    fun collapsedSections(projectId: String): Flow<Set<String>> {
        val key = stringPreferencesKey("collapsed_sections_$projectId")
        return context.appDataStore.data.map { prefs ->
            prefs[key]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        }
    }

    suspend fun setCollapsedSections(projectId: String, ids: Set<String>) {
        val key = stringPreferencesKey("collapsed_sections_$projectId")
        context.appDataStore.edit { prefs -> prefs[key] = ids.joinToString(",") }
    }

    fun hideCompleted(projectId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("hide_completed_$projectId")
        return context.appDataStore.data.map { prefs -> prefs[key] ?: false }
    }

    suspend fun setHideCompleted(projectId: String, value: Boolean) {
        val key = booleanPreferencesKey("hide_completed_$projectId")
        context.appDataStore.edit { prefs -> prefs[key] = value }
    }

    // RSS refresh interval in minutes (0 = manual only)
    private val RSS_INTERVAL = longPreferencesKey("rss_refresh_interval_minutes")

    val rssRefreshIntervalMinutes: Flow<Long> = context.appDataStore.data
        .map { prefs -> prefs[RSS_INTERVAL] ?: 30L }

    suspend fun setRssRefreshInterval(minutes: Long) {
        context.appDataStore.edit { prefs -> prefs[RSS_INTERVAL] = minutes }
    }
}
