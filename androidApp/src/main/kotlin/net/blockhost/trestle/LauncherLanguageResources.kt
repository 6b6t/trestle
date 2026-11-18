package net.blockhost.trestle

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Returns resources in the launcher language. The launcher UI sets the default locale to that language,
 * while Android resources outside Compose keep the system language.
 */
internal fun Context.launcherLanguageResources(locale: Locale = Locale.getDefault()): Resources {
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(configuration).resources
}
