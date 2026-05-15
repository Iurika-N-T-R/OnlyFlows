package com.onlyflows.ui.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetPrefsDataStore by preferencesDataStore(name = "widget_prefs")

data class WidgetPrefs(
    val scopeType: String = "ALL",
    val scopeId: String = "",
    val sortBy: String = "NONE",
    val theme: String = "FOLLOW",
)

class WidgetPrefsRepository(private val context: Context) {

    companion object {
        val SCOPE_TYPE = stringPreferencesKey("widget_scope_type")
        val SCOPE_ID   = stringPreferencesKey("widget_scope_id")
        val SORT_BY    = stringPreferencesKey("widget_sort_by")
        val THEME      = stringPreferencesKey("widget_theme")
    }

    val prefs: Flow<WidgetPrefs> = context.widgetPrefsDataStore.data.map { p ->
        WidgetPrefs(
            scopeType = p[SCOPE_TYPE] ?: "ALL",
            scopeId   = p[SCOPE_ID]   ?: "",
            sortBy    = p[SORT_BY]    ?: "NONE",
            theme     = p[THEME]      ?: "FOLLOW",
        )
    }

    suspend fun setScopeType(type: String) {
        context.widgetPrefsDataStore.edit { it[SCOPE_TYPE] = type }
    }

    suspend fun setScopeId(id: String) {
        context.widgetPrefsDataStore.edit { it[SCOPE_ID] = id }
    }

    suspend fun setSortBy(sort: String) {
        context.widgetPrefsDataStore.edit { it[SORT_BY] = sort }
    }

    suspend fun setTheme(theme: String) {
        context.widgetPrefsDataStore.edit { it[THEME] = theme }
    }
}
