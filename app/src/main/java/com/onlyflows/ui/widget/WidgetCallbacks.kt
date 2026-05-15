package com.onlyflows.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val keyTaskId    = ActionParameters.Key<String>("taskId")
val keyScopeType = ActionParameters.Key<String>("scopeType")
val keyScopeId   = ActionParameters.Key<String>("scopeId")
val keySortBy    = ActionParameters.Key<String>("sortBy")

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

class SetScopeCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val scopeType = parameters[keyScopeType] ?: return
        val scopeId   = parameters[keyScopeId]   ?: ""
        val repo = WidgetPrefsRepository(context)
        repo.setScopeType(scopeType)
        repo.setScopeId(scopeId)
    }
}

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
