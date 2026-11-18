package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import net.blockhost.trestle.app.LauncherLanguage
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES = "AppleLanguages"

// iOS stores the per-app language from the Settings app in AppleLanguages. Trestle keeps that value here
// while its own language is active and puts it back when the player returns to "System".
private const val SAVED_APPLE_LANGUAGES = "TrestleSavedAppleLanguages"

@Composable
internal actual fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit) {
    applyAppleLanguages(language.tag)
    val languageCode = (NSLocale.preferredLanguages.firstOrNull() as? String)
        ?.let { NSLocale(localeIdentifier = it).languageCode }
        .orEmpty()
    LauncherLayoutDirection(languageCode) {
        key(language) { content() }
    }
}

private fun applyAppleLanguages(tag: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    val overridden = defaults.objectForKey(SAVED_APPLE_LANGUAGES) != null
    if (tag != null) {
        if (!overridden) {
            // Read the app's own domain only. An empty list records that the app had no language of its own.
            val appDomain = NSBundle.mainBundle.bundleIdentifier?.let(defaults::persistentDomainForName)
            defaults.setObject(appDomain?.get(APPLE_LANGUAGES) ?: emptyList<String>(), SAVED_APPLE_LANGUAGES)
        }
        defaults.setObject(listOf(tag), APPLE_LANGUAGES)
    } else if (overridden) {
        val saved = defaults.arrayForKey(SAVED_APPLE_LANGUAGES).orEmpty()
        if (saved.isEmpty()) defaults.removeObjectForKey(APPLE_LANGUAGES) else defaults.setObject(saved, APPLE_LANGUAGES)
        defaults.removeObjectForKey(SAVED_APPLE_LANGUAGES)
    }
}
