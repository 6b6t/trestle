package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.app.LauncherLanguage
import net.blockhost.trestle.app.LauncherPreferences
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        onNodeWithText("Desktop-Build ${isolated(BuildInfo.VERSION)}").assertIsDisplayed()
        onNodeWithText("Netzwerk").assertIsDisplayed()
        onNodeWithText("Downloads").assertIsDisplayed()
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
    fun regionSpecificLanguagesLoadTheirTranslation() = runComposeUiTest {
        var language by mutableStateOf(LauncherLanguage.CHINESE_SIMPLIFIED)
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(launcherPreferences = LauncherPreferences(language = language.name)),
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }
        onAllNodesWithText("设置").assertCountEquals(2)

        language = LauncherLanguage.PORTUGUESE_BRAZIL
        waitForIdle()
        onAllNodesWithText("Configurações").assertCountEquals(2)
    }

    @Test
    fun arabicMirrorsTheLayout() = runComposeUiTest {
        val preferences = LauncherPreferences(language = LauncherLanguage.ARABIC.name)
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

        onAllNodesWithText("الإعدادات").assertCountEquals(2)
        val categories = onNodeWithTag(LauncherTestTags.SETTINGS_CATEGORIES).getBoundsInRoot()
        val detail = onNodeWithTag(LauncherTestTags.SETTINGS_DETAIL).getBoundsInRoot()
        assertTrue(categories.left > detail.left, "The category list should sit on the right in Arabic")
    }

    @Test
    fun hebrewLoadsItsTranslationWithItsCurrentLanguageCode() = runComposeUiTest {
        val preferences = LauncherPreferences(language = LauncherLanguage.HEBREW.name)
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

        onAllNodesWithText("הגדרות").assertCountEquals(2)
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
