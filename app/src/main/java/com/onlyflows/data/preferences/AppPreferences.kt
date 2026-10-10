package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Beyond this the swatch row stops being a palette and becomes a second problem. */
private const val MAX_SAVED_COLORS = 24

private val Context.appDataStore:       DataStore<Preferences> by preferencesDataStore(name = "app_prefs")
private val Context.notifDataStore:     DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")
private val Context.sidebarDataStore:   DataStore<Preferences> by preferencesDataStore(name = "sidebar_prefs")
private val Context.calendarDataStore:  DataStore<Preferences> by preferencesDataStore(name = "calendar_prefs")
private val Context.backupDataStore:    DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")
private val Context.dailyAlertDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_alert_prefs")

// ── AppPreferences ────────────────────────────────────────────────────────────

class AppPreferences(private val context: Context) {

    private val THEME_MODE     = stringPreferencesKey("theme_mode")
    private val USE_24H_FORMAT = booleanPreferencesKey("use_24h_format")
    private val FONT_SCALE     = floatPreferencesKey("font_scale")
    private val RSS_INTERVAL   = longPreferencesKey("rss_refresh_interval_minutes")
    private val SAVED_COLORS   = stringPreferencesKey("saved_colors")

    val themeMode: Flow<ThemeMode> = context.appDataStore.data
        .map { ThemeMode.valueOf(it[THEME_MODE] ?: ThemeMode.SYSTEM.name) }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.appDataStore.edit { it[THEME_MODE] = mode.name }

    val use24hFormat: Flow<Boolean> = context.appDataStore.data.map { it[USE_24H_FORMAT] ?: true }

    suspend fun setUse24hFormat(enabled: Boolean) =
        context.appDataStore.edit { it[USE_24H_FORMAT] = enabled }

    val fontScale: Flow<Float> = context.appDataStore.data.map { it[FONT_SCALE] ?: 1.0f }

    suspend fun setFontScale(scale: Float) =
        context.appDataStore.edit { it[FONT_SCALE] = scale }

    fun collapsedSections(projectId: String): Flow<Set<String>> {
        val key = stringPreferencesKey("collapsed_sections_$projectId")
        return context.appDataStore.data.map { it[key]?.split(",")?.filter(String::isNotBlank)?.toSet() ?: emptySet() }
    }

    suspend fun setCollapsedSections(projectId: String, ids: Set<String>) {
        val key = stringPreferencesKey("collapsed_sections_$projectId")
        context.appDataStore.edit { it[key] = ids.joinToString(",") }
    }

    fun hideCompleted(projectId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("hide_completed_$projectId")
        return context.appDataStore.data.map { it[key] ?: false }
    }

    suspend fun setHideCompleted(projectId: String, value: Boolean) {
        val key = booleanPreferencesKey("hide_completed_$projectId")
        context.appDataStore.edit { it[key] = value }
    }

    /** The user's own colour swatches, newest first. Stored as one ordered string because a
     *  Set would lose the order, and the order is the whole point of a palette. */
    val savedColors: Flow<List<String>> = context.appDataStore.data.map { prefs ->
        prefs[SAVED_COLORS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveColor(hex: String) {
        val clean = hex.uppercase()
        context.appDataStore.edit { prefs ->
            val current = prefs[SAVED_COLORS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            prefs[SAVED_COLORS] = (listOf(clean) + current.filterNot { it.equals(clean, true) })
                .take(MAX_SAVED_COLORS)
                .joinToString(",")
        }
    }

    suspend fun removeSavedColor(hex: String) {
        context.appDataStore.edit { prefs ->
            val current = prefs[SAVED_COLORS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            prefs[SAVED_COLORS] = current.filterNot { it.equals(hex, true) }.joinToString(",")
        }
    }

    val rssRefreshIntervalMinutes: Flow<Long> = context.appDataStore.data.map { it[RSS_INTERVAL] ?: 30L }

    suspend fun setRssRefreshInterval(minutes: Long) =
        context.appDataStore.edit { it[RSS_INTERVAL] = minutes }

    // ── Projects screen view state (persisted) ────────────────────────────────

    private val PROJECTS_VIEW_MODE     = stringPreferencesKey("projects_view_mode")          // BOARD | LIST
    private val PROJECTS_BOARD_PAGE    = intPreferencesKey("projects_board_page")            // active kanban column
    private val PROJECTS_COLLAPSED     = stringSetPreferencesKey("projects_collapsed_statuses")

    val projectsViewMode: Flow<String> = context.appDataStore.data.map { it[PROJECTS_VIEW_MODE] ?: "BOARD" }
    suspend fun setProjectsViewMode(mode: String) =
        context.appDataStore.edit { it[PROJECTS_VIEW_MODE] = mode }

    val projectsBoardPage: Flow<Int> = context.appDataStore.data.map { it[PROJECTS_BOARD_PAGE] ?: -1 }
    suspend fun setProjectsBoardPage(page: Int) =
        context.appDataStore.edit { it[PROJECTS_BOARD_PAGE] = page }

    val projectsCollapsedStatuses: Flow<Set<String>> =
        context.appDataStore.data.map { it[PROJECTS_COLLAPSED] ?: emptySet() }
    suspend fun setProjectsCollapsedStatuses(names: Set<String>) =
        context.appDataStore.edit { it[PROJECTS_COLLAPSED] = names }
}

// ── NotificationPreferences ───────────────────────────────────────────────────

class NotificationPreferences(private val context: Context) {

    private val KEY_SOUND            = booleanPreferencesKey("sound_enabled")
    private val KEY_VIBRATION        = booleanPreferencesKey("vibration_enabled")
    private val KEY_BYPASS_DND       = booleanPreferencesKey("bypass_dnd")
    private val KEY_IMMINENCE_LEAD   = intPreferencesKey("imminence_lead_minutes")
    private val KEY_WEEKLY_ENABLED   = booleanPreferencesKey("weekly_recap_enabled")
    private val KEY_WEEKLY_TIME      = stringPreferencesKey("weekly_recap_time")
    private val KEY_EVENING_TIME     = stringPreferencesKey("this_evening_time")

    val soundEnabled: Flow<Boolean>        = context.notifDataStore.data.map { it[KEY_SOUND]          ?: true }
    val vibrationEnabled: Flow<Boolean>    = context.notifDataStore.data.map { it[KEY_VIBRATION]      ?: true }
    val bypassDnd: Flow<Boolean>           = context.notifDataStore.data.map { it[KEY_BYPASS_DND]     ?: false }
    val imminenceLeadMinutes: Flow<Int>    = context.notifDataStore.data.map { it[KEY_IMMINENCE_LEAD] ?: 30 }
    val weeklyRecapEnabled: Flow<Boolean>  = context.notifDataStore.data.map { it[KEY_WEEKLY_ENABLED] ?: false }
    val weeklyRecapTime: Flow<String>      = context.notifDataStore.data.map { it[KEY_WEEKLY_TIME]    ?: "20:00" }
    /** Hour "This Evening" resolves to, for the due time and its automatic reminder. */
    val thisEveningTime: Flow<String>      = context.notifDataStore.data.map { it[KEY_EVENING_TIME]   ?: "18:00" }

    suspend fun setSoundEnabled(enabled: Boolean)       = context.notifDataStore.edit { it[KEY_SOUND]          = enabled }
    suspend fun setVibrationEnabled(enabled: Boolean)   = context.notifDataStore.edit { it[KEY_VIBRATION]      = enabled }
    suspend fun setBypassDnd(enabled: Boolean)          = context.notifDataStore.edit { it[KEY_BYPASS_DND]     = enabled }
    suspend fun setImminenceLeadMinutes(minutes: Int)   = context.notifDataStore.edit { it[KEY_IMMINENCE_LEAD] = minutes }
    suspend fun setWeeklyRecapEnabled(enabled: Boolean) = context.notifDataStore.edit { it[KEY_WEEKLY_ENABLED] = enabled }
    suspend fun setWeeklyRecapTime(time: String)        = context.notifDataStore.edit { it[KEY_WEEKLY_TIME]    = time }
    suspend fun setThisEveningTime(time: String)        = context.notifDataStore.edit { it[KEY_EVENING_TIME]   = time }

    suspend fun snapshot(): Triple<Boolean, Boolean, Boolean> {
        val p = context.notifDataStore.data.first()
        return Triple(p[KEY_SOUND] ?: true, p[KEY_VIBRATION] ?: true, p[KEY_BYPASS_DND] ?: false)
    }
}

// ── SidebarPreferences ────────────────────────────────────────────────────────

class SidebarPreferences(private val context: Context) {

    private val COLLAPSED_FOLDERS = stringSetPreferencesKey("collapsed_folder_ids")
    private val HIDDEN_NAV_ITEMS  = stringSetPreferencesKey("hidden_nav_items")

    val collapsedFolderIds: Flow<Set<String>> = context.sidebarDataStore.data.map { it[COLLAPSED_FOLDERS] ?: emptySet() }

    suspend fun setCollapsed(folderId: String, collapsed: Boolean) {
        context.sidebarDataStore.edit { prefs ->
            val current = prefs[COLLAPSED_FOLDERS] ?: emptySet()
            prefs[COLLAPSED_FOLDERS] = if (collapsed) current + folderId else current - folderId
        }
    }

    val hiddenNavItems: Flow<Set<String>> = context.sidebarDataStore.data.map { it[HIDDEN_NAV_ITEMS] ?: emptySet() }

    suspend fun setNavItemHidden(key: String, hidden: Boolean) {
        context.sidebarDataStore.edit { prefs ->
            val current = prefs[HIDDEN_NAV_ITEMS] ?: emptySet()
            prefs[HIDDEN_NAV_ITEMS] = if (hidden) current + key else current - key
        }
    }
}

// ── CalendarPreferences ───────────────────────────────────────────────────────

class CalendarPreferences(private val context: Context) {

    private val DISABLED_IDS = stringSetPreferencesKey("disabled_calendar_ids")

    val disabledCalendarIds: Flow<Set<String>> = context.calendarDataStore.data.map { it[DISABLED_IDS] ?: emptySet() }

    suspend fun setEnabled(calendarId: String, enabled: Boolean) {
        context.calendarDataStore.edit { prefs ->
            val current = prefs[DISABLED_IDS] ?: emptySet()
            prefs[DISABLED_IDS] = if (!enabled) current + calendarId else current - calendarId
        }
    }
}

// ── BackupPreferences ─────────────────────────────────────────────────────────

class BackupPreferences(private val context: Context) {

    private val FOLDER_URI          = stringPreferencesKey("backup_folder_uri")
    private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    private val INTERVAL_DAYS       = intPreferencesKey("backup_interval_days")
    private val LAST_BACKUP_MS      = longPreferencesKey("last_backup_ms")

    val folderUri: Flow<String>          = context.backupDataStore.data.map { it[FOLDER_URI]          ?: "" }
    val autoBackupEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[AUTO_BACKUP_ENABLED] ?: false }
    val intervalDays: Flow<Int>          = context.backupDataStore.data.map { it[INTERVAL_DAYS]       ?: 1 }
    val lastBackupMs: Flow<Long>         = context.backupDataStore.data.map { it[LAST_BACKUP_MS]      ?: 0L }

    suspend fun setFolderUri(uri: String)          = context.backupDataStore.edit { it[FOLDER_URI]          = uri }
    suspend fun setAutoBackupEnabled(enabled: Boolean) = context.backupDataStore.edit { it[AUTO_BACKUP_ENABLED] = enabled }
    suspend fun setIntervalDays(days: Int)          = context.backupDataStore.edit { it[INTERVAL_DAYS]       = days }
    suspend fun setLastBackupMs(ms: Long)           = context.backupDataStore.edit { it[LAST_BACKUP_MS]      = ms }
}

// ── DailyAlertPreferences ─────────────────────────────────────────────────────

class DailyAlertPreferences(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("daily_alert_enabled")
    private val KEY_TIMES   = stringSetPreferencesKey("daily_alert_times")
    private val KEY_DAYS    = stringSetPreferencesKey("daily_alert_days")

    val enabled: Flow<Boolean>   = context.dailyAlertDataStore.data.map { it[KEY_ENABLED] ?: false }
    val times: Flow<Set<String>> = context.dailyAlertDataStore.data.map { it[KEY_TIMES]   ?: emptySet() }
    val days: Flow<Set<String>>  = context.dailyAlertDataStore.data.map { it[KEY_DAYS]    ?: emptySet() }

    suspend fun setEnabled(enabled: Boolean) = context.dailyAlertDataStore.edit { it[KEY_ENABLED] = enabled }

    suspend fun addTime(hhmm: String) {
        context.dailyAlertDataStore.edit { prefs ->
            prefs[KEY_TIMES] = (prefs[KEY_TIMES] ?: emptySet()) + hhmm
        }
    }

    suspend fun removeTime(hhmm: String) {
        context.dailyAlertDataStore.edit { prefs ->
            prefs[KEY_TIMES] = (prefs[KEY_TIMES] ?: emptySet()) - hhmm
        }
    }

    suspend fun toggleDay(dayName: String) {
        context.dailyAlertDataStore.edit { prefs ->
            val current = prefs[KEY_DAYS] ?: emptySet()
            prefs[KEY_DAYS] = if (dayName in current) current - dayName else current + dayName
        }
    }

    suspend fun snapshot(): Triple<Boolean, Set<String>, Set<String>> {
        val p = context.dailyAlertDataStore.data.first()
        return Triple(p[KEY_ENABLED] ?: false, p[KEY_TIMES] ?: emptySet(), p[KEY_DAYS] ?: emptySet())
    }
}
