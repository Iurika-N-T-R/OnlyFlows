package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calendarDataStore: DataStore<Preferences> by preferencesDataStore(name = "calendar_prefs")

class CalendarPreferences(private val context: Context) {

    private val DISABLED_IDS = stringSetPreferencesKey("disabled_calendar_ids")

    /** Calendar IDs the user has explicitly disabled. Empty = all enabled (default). */
    val disabledCalendarIds: Flow<Set<String>> = context.calendarDataStore.data
        .map { prefs -> prefs[DISABLED_IDS] ?: emptySet() }

    suspend fun setEnabled(calendarId: String, enabled: Boolean) {
        context.calendarDataStore.edit { prefs ->
            val current = prefs[DISABLED_IDS] ?: emptySet()
            prefs[DISABLED_IDS] = if (!enabled) current + calendarId else current - calendarId
        }
    }
}
