package net.blockhost.trestle.ui

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {
    private val originalLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun compactCountsFollowTheLauncherLanguage() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("12.4M", formatCompactCount(12_400_000))
        assertEquals("950", formatCompactCount(950))

        Locale.setDefault(Locale.GERMAN)
        assertEquals("12,4\u00A0Mio.", formatCompactCount(12_400_000))

        Locale.forLanguageTag("ru").let(Locale::setDefault)
        assertEquals("12,4\u00A0млн", formatCompactCount(12_400_000))

        Locale.setDefault(Locale.JAPANESE)
        assertEquals("1240万", formatCompactCount(12_400_000))

        // Arabic keeps its own words but uses Western digits, like the numbers in translated messages.
        Locale.setDefault(Locale.forLanguageTag("ar"))
        assertEquals("12.4\u00A0مليون", formatCompactCount(12_400_000))
    }

    @Test
    fun decimalsUseTheLanguageSeparatorAndDropEmptyFractions() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("1.5", formatDecimal(1.5))
        assertEquals("2", formatDecimal(2.0))

        Locale.setDefault(Locale.FRENCH)
        assertEquals("1,5", formatDecimal(1.5))
    }
}
