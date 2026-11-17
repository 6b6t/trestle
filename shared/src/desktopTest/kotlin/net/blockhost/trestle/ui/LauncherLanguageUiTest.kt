package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.app.LauncherLanguage
import net.blockhost.trestle.app.LauncherPreferences
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LauncherLanguageUiTest {
    private val originalLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun germanPreferenceShowsGermanText() = runComposeUiTest {
        val preferences = LauncherPreferences(language = LauncherLanguage.GERMAN.name)
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(launcherPreferences = preferences),
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onAllNodesWithText("Einstellungen").assertCountEquals(2)
        onNodeWithText("Sprache").performClick()
        onNodeWithText("Sprache der Oberfläche").assertIsDisplayed()
        onNodeWithText("Deutsch").assertIsDisplayed()
    }

    @Test
    fun spanishPreferenceShowsSpanishText() = runComposeUiTest {
        val preferences = LauncherPreferences(language = LauncherLanguage.SPANISH.name)
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(launcherPreferences = preferences),
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onAllNodesWithText("Ajustes").assertCountEquals(2)
        onNodeWithText("Idioma").performClick()
        onNodeWithText("Idioma de la interfaz").assertIsDisplayed()
        onNodeWithText("Español").assertIsDisplayed()
    }

    @Test
    fun choosingGermanSavesTheLanguageName() = runComposeUiTest {
        val saved = mutableListOf<LauncherPreferences>()
        val actions = object : LauncherUiActions {
            override fun setLauncherPreferences(value: LauncherPreferences) {
                saved += value
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(launcherPreferences = LauncherPreferences(language = LauncherLanguage.ENGLISH.name)),
                    actions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }
        onNodeWithText("Language").performClick()
        onNodeWithText("English").performClick()
        onNodeWithText("Deutsch").performClick()

        assertEquals(LauncherLanguage.GERMAN.name, saved.single().language)
    }
}
