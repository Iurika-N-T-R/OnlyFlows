package com.onlyflows.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
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

    private var _quotes = loadQuotes()

    val sessionQuote: Quote? get() = _quotes.randomOrNull()

    val importedCount: Int get() = if (importedFile.exists()) _quotes.size else -1

    suspend fun importFromUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: return@withContext 0
        val arr = JSONArray(json)
        val quotes = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            Quote(text = obj.getString("text"), author = obj.optString("author", ""))
        }
        importedFile.writeText(json)
        _quotes = quotes
        quotes.size
    }
}
