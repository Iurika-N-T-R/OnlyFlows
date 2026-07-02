package com.onlyflows.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Renders a subset of Markdown as styled Compose text (no library dependency).
 * Supported syntax:
 *   **bold**  or  __bold__
 *   *italic*  or  _italic_
 *   ~~strikethrough~~
 *   `inline code`
 *   # H1  ## H2  ### H3  (treated as bold, slightly larger)
 *   - / * / + bullet list items (bullet symbol prepended)
 */
@Composable
fun SimpleMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotated = buildMarkdownAnnotatedString(text)
    Text(
        text     = annotated,
        modifier = modifier,
        style    = style,
        color    = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

fun buildMarkdownAnnotatedString(input: String): AnnotatedString = buildAnnotatedString {
    val lines = input.lines()
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()

        // Heading: # … ######
        val headingMatch = Regex("""^(#{1,6})\s+(.*)""").matchEntire(line)
        if (headingMatch != null) {
            val level   = headingMatch.groupValues[1].length
            val content = headingMatch.groupValues[2]
            val fontSize = when (level) { 1 -> 20.sp; 2 -> 17.sp; else -> 15.sp }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
                appendInlineMarkdown(content)
            }
        } else {
            // Bullet list: - / * / + at start
            val bulletMatch = Regex("""^[-*+]\s+(.*)""").matchEntire(line)
            if (bulletMatch != null) {
                append("• ")
                appendInlineMarkdown(bulletMatch.groupValues[1])
            } else {
                appendInlineMarkdown(line)
            }
        }

        if (index < lines.lastIndex) append("\n")
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    // Tokenise inline spans: ***bolditalic***, **bold**, __bold__, *italic*, _italic_,
    // ~~strike~~, `code`, [text](url)
    val pattern = Regex("""(\*\*\*|___)(.+?)\1|(\*\*|__|\*|_|~~|`)(.+?)\3|\[([^\]]+)]\(([^)]+)\)""")
    var cursor  = 0
    for (match in pattern.findAll(text)) {
        // Append literal text before this match
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val tripleContent = match.groupValues[2]
        val marker        = match.groupValues[3]
        val content       = match.groupValues[4]
        when {
            match.groupValues[1].isNotEmpty() ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(tripleContent) }
            marker == "**" || marker == "__" ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(content) }
            marker == "*" || marker == "_" ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content) }
            marker == "~~" ->
                withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) { append(content) }
            marker == "`" ->
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) { append(content) }
            // [text](url) — show the label styled as a link (URL kept as annotation)
            else -> {
                val label = match.groupValues[5]
                val url   = match.groupValues[6]
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(SpanStyle(color = Color(0xFF4A90D9), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    append(label)
                }
                pop()
            }
        }
        cursor = match.range.last + 1
    }
    // Append any remaining literal text
    if (cursor < text.length) append(text.substring(cursor))
}
