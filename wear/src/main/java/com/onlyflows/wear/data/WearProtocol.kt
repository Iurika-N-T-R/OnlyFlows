package com.onlyflows.wear.data

import org.json.JSONArray
import org.json.JSONObject

/** Message paths + JSON shapes shared with the phone. ponytail: tiny DTO duplicated on both
 *  sides instead of a shared Gradle module. Keep both copies in sync. */
object Paths {
    const val GET_TASKS = "/onlyflows/getTasks"
    const val COMPLETE  = "/onlyflows/complete"
    const val GET_LISTS = "/onlyflows/getLists"
    const val SAVE_TASK = "/onlyflows/saveTask"
    const val GET_TASK  = "/onlyflows/getTask"
    const val GET_COUNT = "/onlyflows/getTaskCount"
}

data class WearCount(val count: Int, val completed: Int)

fun parseCount(json: String?): WearCount {
    val o = runCatching { JSONObject(json ?: return WearCount(0, 0)) }.getOrNull() ?: return WearCount(0, 0)
    return WearCount(o.optInt("count", 0), o.optInt("completed", 0))
}

fun countBody(filterType: String = "TODAY", filterId: String = ""): String =
    JSONObject().put("filterType", filterType).put("filterId", filterId).toString()

data class WearTaskDetail(val title: String, val completed: Boolean, val priority: Int)

fun parseTaskDetail(json: String?): WearTaskDetail? {
    val o = runCatching { JSONObject(json ?: return null) }.getOrNull() ?: return null
    return WearTaskDetail(o.optString("title"), o.optBoolean("completed", false), o.optInt("priority", 0))
}

fun getTaskBody(id: String): String = JSONObject().put("id", id).toString()

fun updateTaskBody(id: String, title: String, completed: Boolean, priority: Int): String =
    JSONObject().put("id", id).put("title", title).put("completed", completed).put("priority", priority).toString()

data class WearTask(
    val id: String,
    val title: String,
    val due: String?,
    val completed: Boolean,
    val priority: Int,
    val overdue: Boolean,
)

fun parseTasks(json: String?): List<WearTask> {
    if (json.isNullOrBlank()) return emptyList()
    val arr = runCatching { JSONObject(json).getJSONArray("items") }.getOrNull() ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        WearTask(
            id        = o.getString("id"),
            title     = o.getString("title"),
            due       = o.optString("due", "").ifEmpty { null },
            completed = o.optBoolean("completed", false),
            priority  = o.optInt("priority", 0),
            overdue   = o.optBoolean("overdue", false),
        )
    }
}

data class WearList(val type: String, val id: String, val title: String)

fun parseLists(json: String?): List<WearList> {
    if (json.isNullOrBlank()) return emptyList()
    val arr = runCatching { JSONObject(json).getJSONArray("items") }.getOrNull() ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        WearList(o.getString("type"), o.optString("id", ""), o.getString("title"))
    }
}

fun getTasksBody(filterType: String, filterId: String = "", showCompleted: Boolean = false): String =
    JSONObject().put("filterType", filterType).put("filterId", filterId).put("showCompleted", showCompleted).toString()

fun completeBody(id: String, completed: Boolean): String =
    JSONObject().put("id", id).put("completed", completed).toString()

fun saveTaskBody(title: String, filterType: String, filterId: String): String =
    JSONObject().put("title", title).put("filterType", filterType).put("filterId", filterId).toString()

// Unused on watch but kept symmetric with the phone encoder.
internal fun encodeTasks(items: List<JSONObject>): String =
    JSONObject().put("items", JSONArray().apply { items.forEach { put(it) } }).toString()
