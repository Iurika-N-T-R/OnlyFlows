package com.onlyflows.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupDataStore by preferencesDataStore("backup_prefs")

class BackupPreferences(private val context: Context) {

    private object Keys {
        val FOLDER_URI           = stringPreferencesKey("backup_folder_uri")
        val AUTO_BACKUP_ENABLED  = booleanPreferencesKey("auto_backup_enabled")
        val INTERVAL_DAYS        = intPreferencesKey("backup_interval_days")
        val LAST_BACKUP_MS       = longPreferencesKey("last_backup_ms")
    }

    val folderUri: Flow<String>         = context.backupDataStore.data.map { it[Keys.FOLDER_URI] ?: "" }
    val autoBackupEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[Keys.AUTO_BACKUP_ENABLED] ?: false }
    val intervalDays: Flow<Int>          = context.backupDataStore.data.map { it[Keys.INTERVAL_DAYS] ?: 1 }
    val lastBackupMs: Flow<Long>         = context.backupDataStore.data.map { it[Keys.LAST_BACKUP_MS] ?: 0L }

    suspend fun setFolderUri(uri: String) {
        context.backupDataStore.edit { it[Keys.FOLDER_URI] = uri }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setIntervalDays(days: Int) {
        context.backupDataStore.edit { it[Keys.INTERVAL_DAYS] = days }
    }

    suspend fun setLastBackupMs(ms: Long) {
        context.backupDataStore.edit { it[Keys.LAST_BACKUP_MS] = ms }
    }
}
