package com.onlyflows.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

data class Quote(val text: String, val author: String)

class QuoteRepository(private val context: Context) {

    private val quotes: List<Quote> by lazy {
        try {
            val json = context.assets.open("quotes.json").bufferedReader().readText()
            val arr  = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Quote(
                    text   = obj.getString("text"),
                    author = obj.optString("author", ""),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val sessionQuote: Quote? by lazy { quotes.randomOrNull() }

    private val _dismissed = MutableStateFlow(false)
    val dismissed = _dismissed.asStateFlow()

    fun dismiss() { _dismissed.value = true }
}
