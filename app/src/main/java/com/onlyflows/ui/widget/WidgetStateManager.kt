package com.onlyflows.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Active tasks for the widget, picked by the current scope/filter. Shared by the
 *  widget snapshot (provideGlance) and the auto-refresh observer below. */
fun widgetTasksFlow(dao: TaskDao, prefs: WidgetPrefs): Flow<List<TaskEntity>> {
    val today = LocalDate.now().toString()
    return when (prefs.scopeType) {
        "TODAY"    -> dao.observeToday(today)
        "ANYTIME"  -> dao.observeAnytime()
        "INBOX"    -> dao.observeInbox()
        "UPCOMING" -> dao.observeUpcoming()
        "SOMEDAY"  -> dao.observeSomeday()
        "PROJECT"  -> dao.observeActiveForWidget(prefs.scopeId.ifEmpty { null }, null)
        "SECTION"  -> dao.observeActiveForWidget(null, prefs.scopeId.ifEmpty { null })
        else       -> dao.observeActiveForWidget(null, null)   // ALL
    }
}

class WidgetStateManager(
    private val context: Context,
    private val prefsRepo: WidgetPrefsRepository,
) {
    private val taskDao = AppDatabase.getInstance(context).taskDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            prefsRepo.prefs
                .flatMapLatest { prefs -> widgetTasksFlow(taskDao, prefs).map { prefs } }
                .collectLatest {
                    OnlyFlowsWidget().updateAll(context)
                }
        }
    }
}
