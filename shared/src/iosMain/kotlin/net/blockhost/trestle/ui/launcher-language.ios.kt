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
private const val WRITTEN_LANGUAGE = "TrestleWrittenLanguage"

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
        // Read the app's own domain only. An empty list records that the app had no language of its own.
        val current = NSBundle.mainBundle.bundleIdentifier
            ?.let(defaults::persistentDomainForName)
            ?.get(APPLE_LANGUAGES) as? List<*>
        // Save the player's own iOS choice the first time, and again when it no longer holds the value Trestle last
        // wrote: the player then changed Trestle's language in the iOS Settings app, and "System" should return to it.
        val written = defaults.stringForKey(WRITTEN_LANGUAGE)
        if (!overridden || current?.map { it.toString() } != listOfNotNull(written)) {
            defaults.setObject(current ?: emptyList<String>(), SAVED_APPLE_LANGUAGES)
        }
        defaults.setObject(listOf(tag), APPLE_LANGUAGES)
        defaults.setObject(tag, WRITTEN_LANGUAGE)
    } else if (overridden) {
        val saved = defaults.arrayForKey(SAVED_APPLE_LANGUAGES).orEmpty()
        if (saved.isEmpty()) defaults.removeObjectForKey(APPLE_LANGUAGES) else defaults.setObject(saved, APPLE_LANGUAGES)
        defaults.removeObjectForKey(SAVED_APPLE_LANGUAGES)
        defaults.removeObjectForKey(WRITTEN_LANGUAGE)
    }
}
