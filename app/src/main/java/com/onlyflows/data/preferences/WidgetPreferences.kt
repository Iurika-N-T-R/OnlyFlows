package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class WidgetListType { TODAY, TOMORROW, INBOX, UPCOMING, ANYTIME, SOMEDAY }
enum class WidgetPlusDestination { TODAY, INBOX }
enum class WidgetCheckboxAction  { COMPLETE, TOGGLE_PRIORITY }

data class WidgetConfig(
    val listType: WidgetListType               = WidgetListType.TODAY,
    val projectId: String?                     = null,
    val tagFilter: String?                     = null,
    val plusDestination: WidgetPlusDestination = WidgetPlusDestination.INBOX,
    val checkboxAction: WidgetCheckboxAction   = WidgetCheckboxAction.COMPLETE,
)

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

class WidgetPreferences(private val context: Context) {

    private fun listTypeKey(widgetId: Int)       = stringPreferencesKey("widget_${widgetId}_list")
    private fun projectIdKey(widgetId: Int)      = stringPreferencesKey("widget_${widgetId}_project")
    private fun tagFilterKey(widgetId: Int)      = stringPreferencesKey("widget_${widgetId}_tag")
    private fun plusDestKey(widgetId: Int)       = stringPreferencesKey("widget_${widgetId}_plus")
    private fun checkboxActionKey(widgetId: Int) = stringPreferencesKey("widget_${widgetId}_checkbox")

    fun observeConfig(widgetId: Int): Flow<WidgetConfig> = context.widgetDataStore.data.map { prefs ->
        WidgetConfig(
            listType       = prefs[listTypeKey(widgetId)]?.let { runCatching { WidgetListType.valueOf(it) }.getOrNull() } ?: WidgetListType.TODAY,
            projectId      = prefs[projectIdKey(widgetId)]?.takeIf { it.isNotBlank() },
            tagFilter      = prefs[tagFilterKey(widgetId)]?.takeIf { it.isNotBlank() },
            plusDestination= prefs[plusDestKey(widgetId)]?.let { runCatching { WidgetPlusDestination.valueOf(it) }.getOrNull() } ?: WidgetPlusDestination.INBOX,
            checkboxAction = prefs[checkboxActionKey(widgetId)]?.let { runCatching { WidgetCheckboxAction.valueOf(it) }.getOrNull() } ?: WidgetCheckboxAction.COMPLETE,
        )
    }

    suspend fun getConfig(widgetId: Int): WidgetConfig = observeConfig(widgetId).first()

    suspend fun saveConfig(widgetId: Int, config: WidgetConfig) {
        context.widgetDataStore.edit { prefs ->
            prefs[listTypeKey(widgetId)]       = config.listType.name
            prefs[projectIdKey(widgetId)]      = config.projectId ?: ""
            prefs[tagFilterKey(widgetId)]      = config.tagFilter ?: ""
            prefs[plusDestKey(widgetId)]       = config.plusDestination.name
            prefs[checkboxActionKey(widgetId)] = config.checkboxAction.name
        }
    }

    suspend fun clearConfig(widgetId: Int) {
        context.widgetDataStore.edit { prefs ->
            prefs.remove(listTypeKey(widgetId))
            prefs.remove(projectIdKey(widgetId))
            prefs.remove(tagFilterKey(widgetId))
            prefs.remove(plusDestKey(widgetId))
            prefs.remove(checkboxActionKey(widgetId))
        }
    }

    // Week widget selected day + reference week start
    private fun weekSelectedDayKey(widgetId: Int)  = stringPreferencesKey("week_${widgetId}_day")
    private fun weekRefStartKey(widgetId: Int)      = stringPreferencesKey("week_${widgetId}_ref")

    suspend fun getWeekSelectedDate(widgetId: Int): String =
        context.widgetDataStore.data.map { it[weekSelectedDayKey(widgetId)] ?: "" }.first()

    suspend fun getWeekRefStart(widgetId: Int): String =
        context.widgetDataStore.data.map { it[weekRefStartKey(widgetId)] ?: "" }.first()

    suspend fun saveWeekSelectedDate(widgetId: Int, date: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[weekSelectedDayKey(widgetId)] = date
        }
    }

    suspend fun saveWeekRefStart(widgetId: Int, date: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[weekRefStartKey(widgetId)] = date
        }
    }

    // Month widget selected day + displayed month
    private fun monthSelectedDayKey(widgetId: Int)    = stringPreferencesKey("month_${widgetId}_day")
    private fun monthDisplayedMonthKey(widgetId: Int) = stringPreferencesKey("month_${widgetId}_month")

    suspend fun getMonthSelectedDate(widgetId: Int): String =
        context.widgetDataStore.data.map { it[monthSelectedDayKey(widgetId)] ?: "" }.first()

    suspend fun getMonthDisplayedMonth(widgetId: Int): String =
        context.widgetDataStore.data.map { it[monthDisplayedMonthKey(widgetId)] ?: "" }.first()

    suspend fun saveMonthSelectedDate(widgetId: Int, date: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[monthSelectedDayKey(widgetId)] = date
        }
    }

    suspend fun saveMonthDisplayedMonth(widgetId: Int, yearMonth: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[monthDisplayedMonthKey(widgetId)] = yearMonth
        }
    }

    // Countdown widget keys
    private fun countdownNameKey(widgetId: Int) = stringPreferencesKey("countdown_${widgetId}_name")
    private fun countdownDateKey(widgetId: Int) = stringPreferencesKey("countdown_${widgetId}_date")

    suspend fun getCountdownName(widgetId: Int): String =
        context.widgetDataStore.data.map { it[countdownNameKey(widgetId)] ?: "" }.first()

    suspend fun getCountdownDate(widgetId: Int): String =
        context.widgetDataStore.data.map { it[countdownDateKey(widgetId)] ?: "" }.first()

    suspend fun saveCountdownConfig(widgetId: Int, name: String, date: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[countdownNameKey(widgetId)] = name
            prefs[countdownDateKey(widgetId)] = date
        }
    }
}
