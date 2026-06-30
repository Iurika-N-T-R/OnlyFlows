package com.onlyflows.wear.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PendingTask(val title: String, val filterType: String, val filterId: String)

/** Tasks created on the watch while the phone was unreachable. Flushed on the next reachable load. */
object PendingQueue {
    private fun prefs(c: Context) = c.getSharedPreferences("onlyflows_wear_queue", Context.MODE_PRIVATE)

    @Synchronized
    fun add(c: Context, task: PendingTask) {
        val arr = read(c)
        arr.put(JSONObject().put("title", task.title).put("ft", task.filterType).put("fi", task.filterId))
        prefs(c).edit().putString("q", arr.toString()).apply()
    }

    @Synchronized
    fun all(c: Context): List<PendingTask> {
        val arr = read(c)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PendingTask(o.getString("title"), o.optString("ft", "INBOX"), o.optString("fi", ""))
        }
    }

    @Synchronized
    fun removeFirst(c: Context) {
        val arr = read(c)
        if (arr.length() == 0) return
        val next = JSONArray()
        for (i in 1 until arr.length()) next.put(arr.get(i))
        prefs(c).edit().putString("q", next.toString()).apply()
    }

    private fun read(c: Context): JSONArray =
        runCatching { JSONArray(prefs(c).getString("q", "[]")) }.getOrDefault(JSONArray())
}
