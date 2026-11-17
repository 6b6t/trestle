package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import net.blockhost.trestle.app.LauncherLanguage
import java.util.Locale

private val systemLocale: Locale = Locale.getDefault()

@Composable
internal actual fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit) {
    Locale.setDefault(language.tag?.let(Locale::forLanguageTag) ?: systemLocale)
    key(language) { content() }
}
