package com.onlyflows.wear

import com.google.android.gms.tasks.Task as GmsTask
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.WearableListenerService
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.local.AppDatabase
import com.onlyflows.data.local.dao.TaskDao
import com.onlyflows.data.local.entity.TaskEntity
import com.onlyflows.data.local.entity.toDomain
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Phone-side bridge for the Wear app. Answers MessageClient.sendRequest() calls over the
 * Wear Data Layer using OnlyFlows' existing Room repos. JSON via org.json (no extra toolchain).
 */
class OnlyFlowsWearService : WearableListenerService() {

    override fun onRequest(nodeId: String, path: String, data: ByteArray): GmsTask<ByteArray>? {
        val body = runCatching { JSONObject(String(data, Charsets.UTF_8)) }.getOrNull() ?: JSONObject()
        val container = (application as OnlyFlowsApp).container
        val db = AppDatabase.getInstance(this)

        val result: String? = runBlocking {
            when (path) {
                "/onlyflows/getTasks" -> tasksJson(loadTasks(db.taskDao(), body))
                "/onlyflows/getLists" -> listsJson(db)
                "/onlyflows/getTaskCount" -> {
                    val today = LocalDate.now().toString()
                    val active = activeTasks(db.taskDao(), body).size
                    val completed = if (body.optString("filterType", "TODAY") == "TODAY")
                        db.taskDao().observeTodayCompleted(today).first().size else 0
                    JSONObject().put("count", active).put("completed", completed).toString()
                }
                "/onlyflows/getTask" -> {
                    val e = db.taskDao().getById(body.optString("id"))
                    if (e == null) "{}" else JSONObject()
                        .put("title", e.title)
                        .put("completed", e.isCompleted)
                        .put("priority", priorityInt(e.priority))
                        .toString()
                }
                "/onlyflows/complete" -> {
                    val id = body.optString("id")
                    val completed = body.optBoolean("completed", true)
                    if (id.isNotEmpty()) {
                        val e = db.taskDao().getById(id)
                        when {
                            !completed                -> container.taskRepository.setCompleted(id, false)
                            e?.recurrenceJson != null -> container.taskRepository.completeRecurring(id)
                            else                      -> container.taskRepository.setCompleted(id, true)
                        }
                    }
                    "{\"ok\":true}"
                }
                "/onlyflows/saveTask" -> {
                    val id = body.optString("id")
                    val title = body.optString("title").trim()
                    if (id.isNotEmpty()) {
                        // Update existing task (from the watch edit screen)
                        db.taskDao().getById(id)?.let { e ->
                            container.taskRepository.update(
                                e.toDomain().copy(
                                    title    = title.ifEmpty { e.title },
                                    isCompleted = body.optBoolean("completed", e.isCompleted),
                                    priority = priorityEnum(body.optInt("priority", priorityInt(e.priority))),
                                )
                            )
                        }
                    } else if (title.isNotEmpty()) {
                        container.taskRepository.insert(
                            Task(
                                id = "task_${System.currentTimeMillis()}",
                                title = title,
                                scheduledGroup = groupFor(body.optString("filterType")),
                                projectId = if (body.optString("filterType") == "PROJECT")
                                    body.optString("filterId").ifEmpty { null } else null,
                            )
                        )
                    }
                    "{\"ok\":true}"
                }
                else -> null
            }
        }
        return result?.let { Tasks.forResult(it.toByteArray(Charsets.UTF_8)) }
    }

    private suspend fun activeTasks(dao: TaskDao, body: JSONObject): List<TaskEntity> {
        val today = LocalDate.now().toString()
        val flow = when (body.optString("filterType", "TODAY")) {
            "TODAY"    -> dao.observeToday(today)
            "ANYTIME"  -> dao.observeAnytime()
            "INBOX"    -> dao.observeInbox()
            "UPCOMING" -> dao.observeUpcoming()
            "SOMEDAY"  -> dao.observeSomeday()
            "PROJECT"  -> dao.observeActiveForWidget(body.optString("filterId").ifEmpty { null }, null)
            else       -> dao.observeActiveForWidget(null, null)
        }
        return flow.first().sortedWith(compareBy(nullsLast()) { it.dueDate })
    }

    private suspend fun loadTasks(dao: TaskDao, body: JSONObject): List<TaskEntity> {
        val today = LocalDate.now().toString()
        val active = activeTasks(dao, body)
        // ponytail: showCompleted only wired for Today (only plain completed flow); no-op elsewhere.
        val completed = if (body.optBoolean("showCompleted", false) && body.optString("filterType", "TODAY") == "TODAY")
            dao.observeTodayCompleted(today).first() else emptyList()
        return (active + completed).take(100)
    }

    private fun tasksJson(tasks: List<TaskEntity>): String {
        val items = JSONArray()
        val today = LocalDate.now()
        tasks.forEach { t ->
            val overdue = !t.isCompleted && t.dueDate?.isBefore(today) == true
            items.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("due", dueText(t) ?: "")
                    .put("completed", t.isCompleted)
                    .put("priority", priorityInt(t.priority))
                    .put("overdue", overdue)
            )
        }
        return JSONObject().put("items", items).toString()
    }

    private fun listsJson(db: AppDatabase): String = runBlocking {
        val items = JSONArray()
        listOf("TODAY" to "Today", "ANYTIME" to "Anytime", "INBOX" to "Inbox", "UPCOMING" to "Upcoming", "SOMEDAY" to "Someday", "ALL" to "Toutes")
            .forEach { (type, label) -> items.put(JSONObject().put("type", type).put("id", "").put("title", label)) }
        db.projectDao().observeActiveProjectsAndLists().first().forEach { p ->
            items.put(JSONObject().put("type", "PROJECT").put("id", p.id).put("title", p.title))
        }
        JSONObject().put("items", items).toString()
    }

    private fun groupFor(filterType: String): TaskGroup? = when (filterType) {
        "TODAY"   -> TaskGroup.TODAY
        "ANYTIME" -> TaskGroup.ANYTIME
        "SOMEDAY" -> TaskGroup.SOMEDAY
        else      -> null
    }

    private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private fun dueText(t: TaskEntity): String? {
        val d = t.dueDate ?: return null
        val day = d.format(dateFmt)
        return t.dueTime?.let { "$day · ${it.format(timeFmt)}" } ?: day
    }

    private fun priorityInt(p: String) = when (p) { "HIGH" -> 3; "MEDIUM" -> 2; "LOW" -> 1; else -> 0 }
    private fun priorityEnum(i: Int) = when (i) { 3 -> TaskPriority.HIGH; 2 -> TaskPriority.MEDIUM; 1 -> TaskPriority.LOW; else -> TaskPriority.NONE }
}
