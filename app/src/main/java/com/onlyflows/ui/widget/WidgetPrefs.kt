package com.onlyflows.ui.widget

import android.content.Context

/** Per-widget settings keyed by appWidgetId (synchronous SharedPreferences — read inside RemoteViews build). */
class WidgetPrefs(context: Context, private val widgetId: Int) {
    private val sp = context.applicationContext.getSharedPreferences("onlyflows_widget", Context.MODE_PRIVATE)
    private fun k(name: String) = "${name}_$widgetId"

    var filterType: String
        get() = sp.getString(k("filter_type"), "TODAY") ?: "TODAY"
        set(v) = sp.edit().putString(k("filter_type"), v).apply()

    var filterId: String
        get() = sp.getString(k("filter_id"), "") ?: ""
        set(v) = sp.edit().putString(k("filter_id"), v).apply()

    var theme: String
        get() = sp.getString(k("theme"), "FOLLOW") ?: "FOLLOW"
        set(v) = sp.edit().putString(k("theme"), v).apply()

    var showDetails: Boolean
        get() = sp.getBoolean(k("show_details"), true)
        set(v) = sp.edit().putBoolean(k("show_details"), v).apply()

    fun clear() = sp.edit()
        .remove(k("filter_type")).remove(k("filter_id"))
        .remove(k("theme")).remove(k("show_details")).apply()
}
