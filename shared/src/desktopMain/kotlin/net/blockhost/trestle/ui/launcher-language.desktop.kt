package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import net.blockhost.trestle.app.LauncherLanguage
import java.util.Locale

private val systemLocale: Locale = Locale.getDefault()

/**
 * Sets the JVM default locale that Compose resources read. The desktop app calls this before it builds
 * the tray and notifications, which are outside [LauncherLanguageEnvironment].
 */
fun applyDesktopLauncherLanguage(language: LauncherLanguage) {
    Locale.setDefault(language.tag?.let(Locale::forLanguageTag) ?: systemLocale)
}

@Composable
internal actual fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit) {
    applyDesktopLauncherLanguage(language)
    LauncherLayoutDirection(Locale.getDefault().language) {
        ReloadStringsOnLanguageChange(language, content)
    }
}
