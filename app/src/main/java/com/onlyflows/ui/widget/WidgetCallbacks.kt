package com.onlyflows.ui.widget

import android.content.Context
import androidx.annotation.Keep
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val keyTaskId    = ActionParameters.Key<String>("taskId")
val keySortBy    = ActionParameters.Key<String>("sortBy")
// Maps to MainActivity.EXTRA_ADD_TASK — Glance forwards the key name as an intent extra.
val keyAddTask   = ActionParameters.Key<Boolean>("extra_add_task")

@Keep
class ToggleTaskCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[keyTaskId] ?: return
        val app = context.applicationContext as OnlyFlowsApp
        withContext(Dispatchers.IO) {
            val entity = AppDatabase.getInstance(context).taskDao().getById(taskId) ?: return@withContext
            if (entity.recurrenceJson != null) {
                app.container.taskRepository.completeRecurring(taskId)
            } else {
                app.container.taskRepository.setCompleted(taskId, true)
            }
        }
    }
}

@Keep
class SetSortCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val sortBy = parameters[keySortBy] ?: return
        WidgetPrefsRepository(context).setSortBy(sortBy)
    }
}
