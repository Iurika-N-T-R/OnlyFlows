package com.onlyflows.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.onlyflows.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Long enough for the checkbox to finish its 600 ms animation, plus a beat to read it. */
const val COMPLETION_HOLD_MS = 750L

/**
 * Keeps a task the user just ticked in its own row for a moment.
 *
 * Every list here is driven by a database query that excludes completed tasks, so the row left
 * the list on the frame the write landed — before the checkbox had drawn a single animation
 * frame. Giving the checkbox its own optimistic state was not enough: the row it lives in was
 * already gone. This holds the row itself.
 *
 * The hold is started by the completion callback rather than inferred from a row vanishing,
 * because rows also vanish when deleted, moved or rescheduled, and none of those should leave
 * a ghost ticked row behind.
 */
@Stable
class CompletionHold(
    private val scope: CoroutineScope,
    private val holdMs: Long,
) {
    // id -> (index it occupied, the task shown as completed while held)
    private val held = mutableStateMapOf<String, Pair<Int, Task>>()

    /** Call from the checkbox callback, with the list the row is currently in. */
    fun onToggle(task: Task, visible: List<Task>) {
        if (task.isCompleted) return   // un-ticking puts the row back, nothing to hold
        val index = visible.indexOfFirst { it.id == task.id }.takeIf { it >= 0 } ?: return
        held[task.id] = index to task.copy(isCompleted = true)
        scope.launch {
            delay(holdMs)
            held.remove(task.id)
        }
    }

    /** [tasks] with every held row put back where it was. */
    fun apply(tasks: List<Task>): List<Task> {
        if (held.isEmpty()) return tasks
        val out = tasks.toMutableList()
        held.values.sortedBy { it.first }.forEach { (index, task) ->
            if (out.none { it.id == task.id }) out.add(index.coerceIn(0, out.size), task)
        }
        return out
    }

    /** True while [id] is being held, so the completed section can leave it out until then. */
    operator fun contains(id: String): Boolean = id in held
}

@Composable
fun rememberCompletionHold(holdMs: Long = COMPLETION_HOLD_MS): CompletionHold {
    val scope = rememberCoroutineScope()
    return remember(scope, holdMs) { CompletionHold(scope, holdMs) }
}
