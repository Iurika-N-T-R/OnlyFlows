package com.onlyflows.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

data class Quote(val text: String, val author: String)

class QuoteRepository(private val context: Context) {

    private val importedFile: File get() = File(context.filesDir, "quotes_imported.json")

    private fun loadQuotes(): List<Quote> {
        val json = if (importedFile.exists()) {
            importedFile.readText()
        } else {
            try { context.assets.open("quotes.json").bufferedReader().readText() }
            catch (_: Exception) { return emptyList() }
        }
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Quote(text = obj.getString("text"), author = obj.optString("author", ""))
            }
        } catch (_: Exception) { emptyList() }
    }

    private val _quotes = MutableStateFlow(loadQuotes())

    val sessionQuote: Quote? get() = _quotes.value.randomOrNull()

    val importedCount: Int get() = if (importedFile.exists()) _quotes.value.size else -1

    private val _dismissed = MutableStateFlow(false)
    val dismissed = _dismissed.asStateFlow()

    fun dismiss() { _dismissed.value = true }

    suspend fun importFromUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: return@withContext 0
        val arr = JSONArray(json)
        val quotes = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            Quote(text = obj.getString("text"), author = obj.optString("author", ""))
        }
        importedFile.writeText(json)
        _quotes.value = quotes
        quotes.size
    }
}
