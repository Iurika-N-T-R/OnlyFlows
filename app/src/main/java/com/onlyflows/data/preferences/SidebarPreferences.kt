package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sidebar_prefs")

class SidebarPreferences(private val context: Context) {

    private val COLLAPSED_FOLDERS = stringSetPreferencesKey("collapsed_folder_ids")

    /** Emits the set of folder IDs that the user has explicitly collapsed. */
    val collapsedFolderIds: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[COLLAPSED_FOLDERS] ?: emptySet() }

    suspend fun setCollapsed(folderId: String, collapsed: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[COLLAPSED_FOLDERS] ?: emptySet()
            prefs[COLLAPSED_FOLDERS] = if (collapsed) current + folderId else current - folderId
        }
    }
}
