package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown

private val htmlMarkupPattern = Regex(
    "(?is)</?(?:a|b|blockquote|br|code|details|div|em|h[1-6]|i|img|li|ol|p|pre|section|span|strong|summary|table|td|th|tr|ul)\\b",
)
private val remainingHtmlTagPattern = Regex("(?is)<[^>]+>")
private val htmlEntityPattern = Regex("&(#x[0-9a-fA-F]+|#\\d+|[A-Za-z]+);")

@Composable
internal fun ResourceDescription(content: String, modifier: Modifier = Modifier) {
    val markdown = remember(content) { normalizeResourceDescription(content) }
    Markdown(
        content = markdown,
        modifier = modifier.fillMaxWidth(),
        imageTransformer = Coil3ImageTransformerImpl,
    )
}

internal fun normalizeResourceDescription(value: String): String {
    var content = value
        .replace(Regex("(?is)<!--.*?-->"), "")
        .replace(Regex("(?is)<(?:script|style)\\b[^>]*>.*?</(?:script|style)>"), "")

    if (htmlMarkupPattern.containsMatchIn(content)) {
        content = content
            .replace(
                Regex("(?is)<a\\b[^>]*href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>"),
            ) { match ->
                val label = decodeHtmlEntities(match.groupValues[2].replace(remainingHtmlTagPattern, "")).trim()
                val url = decodeHtmlEntities(match.groupValues[1]).trim()
                if (label.isBlank()) "" else "[$label]($url)"
            }
            .replace(Regex("(?is)<h([1-6])\\b[^>]*>(.*?)</h\\1>")) { match ->
                val level = match.groupValues[1].toInt()
                "\n\n${"#".repeat(level)} ${match.groupValues[2].trim()}\n\n"
            }
            .replace(Regex("(?i)<(?:strong|b)\\b[^>]*>"), "**")
            .replace(Regex("(?i)</(?:strong|b)>"), "**")
            .replace(Regex("(?i)<(?:em|i)\\b[^>]*>"), "*")
            .replace(Regex("(?i)</(?:em|i)>"), "*")
            .replace(Regex("(?i)<code\\b[^>]*>"), "`")
            .replace(Regex("(?i)</code>"), "`")
            .replace(Regex("(?i)<li\\b[^>]*>"), "\n- ")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</?(?:blockquote|details|div|ol|p|pre|section|summary|table|td|th|tr|ul)\\b[^>]*>"), "\n\n")
            .replace(Regex("(?is)<img\\b[^>]*>"), "")
            .replace(remainingHtmlTagPattern, "")
    }

    return decodeHtmlEntities(content)
        .replace(Regex("(?m)[ \\t]+$"), "")
        .replace(Regex("\n[ \\t]+\n"), "\n\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun decodeHtmlEntities(value: String): String = value.replace(htmlEntityPattern) { match ->
    val entity = match.groupValues[1]
    when {
        entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toIntOrNull(16).asEntityCharacter(match.value)
        entity.startsWith('#') -> entity.drop(1).toIntOrNull().asEntityCharacter(match.value)
        else -> when (entity.lowercase()) {
            "amp" -> "&"
            "apos", "#39" -> "'"
            "gt" -> ">"
            "hellip" -> "…"
            "laquo" -> "«"
            "lt" -> "<"
            "mdash" -> "—"
            "nbsp" -> " "
            "ndash" -> "–"
            "quot" -> "\""
            "raquo" -> "»"
            else -> match.value
        }
    }
}

private fun Int?.asEntityCharacter(fallback: String): String =
    if (this != null && this in Char.MIN_VALUE.code..Char.MAX_VALUE.code) this.toChar().toString() else fallback
