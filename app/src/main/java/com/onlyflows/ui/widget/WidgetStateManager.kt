package com.onlyflows.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.onlyflows.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WidgetStateManager(
    private val context: Context,
    private val prefsRepo: WidgetPrefsRepository,
) {
    private val taskDao = AppDatabase.getInstance(context).taskDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            prefsRepo.prefs
                .flatMapLatest { prefs ->
                    val projectId = if (prefs.scopeType == "PROJECT") prefs.scopeId.ifEmpty { null } else null
                    val sectionId = if (prefs.scopeType == "SECTION") prefs.scopeId.ifEmpty { null } else null
                    taskDao.observeActiveForWidget(projectId, sectionId).map { prefs }
                }
                .collectLatest {
                    OnlyFlowsWidget().updateAll(context)
                }
        }
    }
}
