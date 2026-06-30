package com.onlyflows.wear.data

import android.content.Context

/** Watch-local settings, sent to the phone with task requests. */
object WearSettings {
    private fun prefs(c: Context) = c.getSharedPreferences("onlyflows_wear", Context.MODE_PRIVATE)

    fun showCompleted(c: Context): Boolean = prefs(c).getBoolean("show_completed", false)
    fun setShowCompleted(c: Context, v: Boolean) = prefs(c).edit().putBoolean("show_completed", v).apply()
}
