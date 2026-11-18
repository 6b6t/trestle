package net.blockhost.trestle.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.LayoutDirection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LauncherLayoutDirectionTest {
    private val resources = File("src/commonMain/composeResources")

    @Test
    fun translatedRightToLeftLanguagesUseRightToLeftLayout() {
        listOf("ar", "fa", "he", "iw", "ur").forEach { code ->
            assertEquals(LayoutDirection.Rtl, launcherLayoutDirection(code), code)
        }
    }

    @Test
    fun otherLanguagesUseLeftToRightLayout() {
        // Pashto is written right to left, but Trestle has no Pashto translation and shows English instead.
        listOf("en", "de", "ja", "ps", "").forEach { code ->
            assertEquals(LayoutDirection.Ltr, launcherLayoutDirection(code), code)
        }
    }

    @Test
    fun hebrewResourcesMatchForBothLanguageCodes() {
        // Android reports Hebrew as "iw" while desktop and iOS report "he", so both folders must hold the same text.
        assertEquals(
            File(resources, "values-he/strings.xml").readText(),
            File(resources, "values-iw/strings.xml").readText(),
        )
    }

    @Test
    fun rightToLeftTranslationsKeepTheirDirectionWhenTheyStartWithLatinText() {
        // Text direction follows the first strong letter, so a translation that starts with "Minecraft" or a
        // placeholder needs a leading right-to-left mark. Names that Trestle stores are left alone.
        val storedValues = setOf("ui_instance_copy_name", "ui_player_skin_name")
        val entry = Regex("""<(?:string|item)(?: name="([^"]+)")?[^>]*>(.*?)</(?:string|item)>""", RegexOption.DOT_MATCHES_ALL)
        val missing = listOf("ar", "fa", "he", "ur").flatMap { language ->
            entry.findAll(File(resources, "values-$language/strings.xml").readText())
                .filter { it.groupValues[1] !in storedValues }
                .map { it.groupValues[2] }
                .filter { text -> !text.startsWith(RIGHT_TO_LEFT_MARK) && !startsRightToLeft(text) }
                .map { "$language: $it" }
                .toList()
        }
        assertTrue(missing.isEmpty(), missing.joinToString("\n"))
    }

    @Test
    fun englishTextStaysLeftToRightInRightToLeftLayout() = runComposeUiTest {
        var english: TextLayoutResult? = null
        var arabic: TextLayoutResult? = null
        setContent {
            TrestleTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("Modern rendering engine.", onTextLayout = { english = it })
                    Text("${RIGHT_TO_LEFT_MARK}Minecraft قيد التشغيل", onTextLayout = { arabic = it })
                }
            }
        }
        waitForIdle()
        assertEquals(ResolvedTextDirection.Ltr, english?.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Rtl, arabic?.getParagraphDirection(0))
    }

    private fun startsRightToLeft(text: String): Boolean {
        val withoutPlaceholders = text.replace(Regex("""%\d+\$[sd]"""), "\u0000")
        val first = withoutPlaceholders.firstOrNull { it == '\u0000' || Character.getDirectionality(it).isStrong() }
        return first != null && first != '\u0000' && Character.getDirectionality(first) != Character.DIRECTIONALITY_LEFT_TO_RIGHT
    }

    private fun Byte.isStrong() = this == Character.DIRECTIONALITY_LEFT_TO_RIGHT ||
        this == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
        this == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC

    private companion object {
        const val RIGHT_TO_LEFT_MARK = '‏'
    }
}
