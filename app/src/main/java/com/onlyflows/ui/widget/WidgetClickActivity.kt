package com.onlyflows.ui.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.onlyflows.MainActivity
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

/** Transparent dispatcher for widget row clicks (collection items can't carry PendingIntents directly).
 *  The list sets a PendingIntent template; each row fills in an action + extras handled here. */
class WidgetClickActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as OnlyFlowsApp).container
        when (intent.action) {
            COMPLETE_TASK -> {
                val taskId    = intent.getStringExtra(EXTRA_TASK_ID)
                val completed = intent.getBooleanExtra(EXTRA_COMPLETED, true)
                if (!taskId.isNullOrEmpty()) {
                    lifecycleScope.launch(NonCancellable) {
                        if (completed) container.taskRepository.complete(taskId)
                        else container.taskRepository.setCompleted(taskId, false)
                    }
                }
                finish()
            }
            OPEN_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrEmpty()) {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
                    )
                }
                finish()
            }
            else -> finish()
        }
    }

    companion object {
        const val COMPLETE_TASK   = "com.onlyflows.widget.COMPLETE_TASK"
        const val OPEN_TASK       = "com.onlyflows.widget.OPEN_TASK"
        const val EXTRA_TASK_ID   = "extra_task_id"
        const val EXTRA_COMPLETED = "extra_completed"
    }
}
