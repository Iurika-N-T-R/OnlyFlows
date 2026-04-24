package com.onlyflows.data.rss

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.security.MessageDigest

data class ParsedArticle(
    val title: String,
    val link: String,
    val description: String,
    val author: String,
    val pubDate: String,
)

object RssParser {

    fun parse(stream: InputStream): List<ParsedArticle> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, null)
        }
        return try {
            when {
                skipToTag(parser, "rss") || skipToTag(parser, "feed") -> {
                    if (parser.name == "feed") parseAtom(parser) else parseRss(parser)
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun skipToTag(parser: XmlPullParser, name: String): Boolean {
        val startDepth = parser.depth
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) return false
            if (event == XmlPullParser.START_TAG && parser.name.equals(name, ignoreCase = true)) return true
        }
    }

    // ── RSS 2.0 ────────────────────────────────────────────────────────────────

    private fun parseRss(parser: XmlPullParser): List<ParsedArticle> {
        val articles = mutableListOf<ParsedArticle>()
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                parseRssItem(parser)?.let { articles.add(it) }
            }
        }
        return articles
    }

    private fun parseRssItem(parser: XmlPullParser): ParsedArticle? {
        var title = ""; var link = ""; var desc = ""; var author = ""; var pubDate = ""
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals("item", ignoreCase = true)) break
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event != XmlPullParser.START_TAG) continue
            when (parser.name.lowercase()) {
                "title"          -> title   = readText(parser)
                "link"           -> link    = readText(parser)
                "guid"           -> if (link.isBlank()) link = readText(parser)
                "description"    -> desc    = stripHtml(readText(parser))
                "content:encoded"-> if (desc.isBlank()) desc = stripHtml(readText(parser))
                "author", "dc:creator" -> author = readText(parser)
                "pubdate"        -> pubDate = readText(parser)
            }
        }
        if (link.isBlank() && title.isBlank()) return null
        return ParsedArticle(title.trim(), link.trim(), desc.take(500).trim(), author.trim(), pubDate.trim())
    }

    // ── Atom ──────────────────────────────────────────────────────────────────

    private fun parseAtom(parser: XmlPullParser): List<ParsedArticle> {
        val articles = mutableListOf<ParsedArticle>()
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.START_TAG && parser.name.equals("entry", ignoreCase = true)) {
                parseAtomEntry(parser)?.let { articles.add(it) }
            }
        }
        return articles
    }

    private fun parseAtomEntry(parser: XmlPullParser): ParsedArticle? {
        var title = ""; var link = ""; var desc = ""; var author = ""; var pubDate = ""
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals("entry", ignoreCase = true)) break
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.START_TAG) {
                when (parser.name.lowercase()) {
                    "title"    -> title   = readText(parser)
                    "link"     -> { val href = parser.getAttributeValue(null, "href"); if (href != null) link = href else readText(parser) }
                    "summary", "content" -> desc = stripHtml(readText(parser))
                    "name"     -> author = readText(parser)
                    "published", "updated" -> if (pubDate.isBlank()) pubDate = readText(parser)
                }
            }
        }
        if (link.isBlank() && title.isBlank()) return null
        return ParsedArticle(title.trim(), link.trim(), desc.take(500).trim(), author.trim(), pubDate.trim())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        if (parser.next() == XmlPullParser.TEXT) {
            sb.append(parser.text)
            parser.nextTag()
        }
        return sb.toString()
    }

    private fun stripHtml(html: String): String =
        android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("\\s+"), " ")
            .trim()

    fun articleId(feedId: String, link: String): String {
        val raw = "$feedId|$link"
        return MessageDigest.getInstance("MD5")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
