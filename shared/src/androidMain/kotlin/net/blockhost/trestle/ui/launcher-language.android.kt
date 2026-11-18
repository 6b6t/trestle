package net.blockhost.trestle.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import net.blockhost.trestle.app.LauncherLanguage
import java.util.Locale

@Composable
internal actual fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit) {
    // The activity configuration always holds the system locale, so "System" follows later system changes.
    val configuration = LocalConfiguration.current
    val locale = language.tag?.let(Locale::forLanguageTag) ?: configuration.locales[0]
    Locale.setDefault(locale)
    val localized = remember(configuration, locale) { Configuration(configuration).apply { setLocale(locale) } }
    CompositionLocalProvider(LocalConfiguration provides localized) {
        LauncherLayoutDirection(locale.language) {
            key(language) { content() }
        }
    }
}
