package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import net.blockhost.trestle.app.LauncherLanguage

/**
 * Shows [content] in [language]. The platform locale changes and [content] is composed again, so
 * string resources load from the matching translation.
 */
@Composable
internal expect fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit)

// Languages that Trestle translates and that are written right to left. Android still reports Hebrew as "iw".
private val rightToLeftLanguages = setOf("ar", "fa", "he", "iw", "ur")

/**
 * Returns the layout direction for the translation shown for [languageCode]. A right-to-left system language
 * without a Trestle translation shows English, so it keeps a left-to-right layout.
 */
fun launcherLayoutDirection(languageCode: String): LayoutDirection =
    if (languageCode.lowercase() in rightToLeftLanguages) LayoutDirection.Rtl else LayoutDirection.Ltr

/** Lays out [content] in the direction of the language with [languageCode]. */
@Composable
internal fun LauncherLayoutDirection(languageCode: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides launcherLayoutDirection(languageCode), content = content)
}
