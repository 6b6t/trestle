package net.blockhost.trestle

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.LauncherLanguage
import net.blockhost.trestle.ui.LauncherViewModel

private const val LANGUAGE_PREFERENCES = "app-language"
private const val SYNCED_LANGUAGE = "synced"

/**
 * Keeps Trestle's language setting and Android's per-app language (Android 13 and newer) in step. Android then draws
 * the home-screen shortcuts, notifications and the game process in the same language as the launcher.
 *
 * Both sides can change the language. The tag both last agreed on is stored, so a difference tells which side
 * changed: a new Android value came from the system settings and is adopted, and anything else comes from Trestle.
 */
internal fun ComponentActivity.syncAppLanguage(launcher: LauncherViewModel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val localeManager = getSystemService(LocaleManager::class.java) ?: return
    val store = getSharedPreferences(LANGUAGE_PREFERENCES, Context.MODE_PRIVATE)
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launcher.state
                .filterNot { it.isInitializing }
                .map { it.launcherPreferences }
                .distinctUntilChanged { old, new -> old.language == new.language }
                .collect { preferences ->
                    val androidTag = localeManager.applicationLocales.takeUnless { it.isEmpty }?.get(0)?.toLanguageTag().orEmpty()
                    val trestleTag = preferences.launcherLanguage.tag.orEmpty()
                    val synced = store.getString(SYNCED_LANGUAGE, null)
                    when {
                        synced != null && androidTag != synced -> {
                            store.edit { putString(SYNCED_LANGUAGE, androidTag) }
                            launcher.setLauncherPreferences(preferences.copy(language = LauncherLanguage.fromLanguageTag(androidTag).name))
                        }

                        trestleTag != androidTag -> {
                            store.edit { putString(SYNCED_LANGUAGE, trestleTag) }
                            localeManager.applicationLocales = LocaleList.forLanguageTags(trestleTag)
                        }

                        else -> store.edit { putString(SYNCED_LANGUAGE, androidTag) }
                    }
                }
        }
    }
}
