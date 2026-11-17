package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import net.blockhost.trestle.app.LauncherLanguage
import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES = "AppleLanguages"

@Composable
internal actual fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit) {
    val defaults = NSUserDefaults.standardUserDefaults
    val tag = language.tag
    if (tag == null) defaults.removeObjectForKey(APPLE_LANGUAGES) else defaults.setObject(listOf(tag), APPLE_LANGUAGES)
    key(language) { content() }
}
