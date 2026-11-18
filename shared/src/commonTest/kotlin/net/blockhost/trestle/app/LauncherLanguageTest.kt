package net.blockhost.trestle.app

import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherLanguageTest {
    @Test
    fun readsSavedLanguageNames() {
        assertEquals(LauncherLanguage.GERMAN, LauncherPreferences(language = "GERMAN").launcherLanguage)
        assertEquals(LauncherLanguage.ENGLISH, LauncherPreferences(language = "ENGLISH").launcherLanguage)
        assertEquals(LauncherLanguage.SPANISH, LauncherPreferences(language = "SPANISH").launcherLanguage)
        assertEquals(LauncherLanguage.SYSTEM, LauncherPreferences().launcherLanguage)
    }

    @Test
    fun readsLabelsSavedByOlderVersions() {
        assertEquals(LauncherLanguage.ENGLISH, LauncherLanguage.fromPreference("English"))
        assertEquals(LauncherLanguage.SYSTEM, LauncherLanguage.fromPreference("System default"))
        assertEquals(LauncherLanguage.SYSTEM, LauncherLanguage.fromPreference("Klingon"))
    }

    @Test
    fun readsPlatformLanguageTags() {
        assertEquals(LauncherLanguage.PORTUGUESE_BRAZIL, LauncherLanguage.fromLanguageTag("pt-BR"))
        assertEquals(LauncherLanguage.CHINESE_SIMPLIFIED, LauncherLanguage.fromLanguageTag("zh-CN"))
        assertEquals(LauncherLanguage.GERMAN, LauncherLanguage.fromLanguageTag("de-AT"))
        assertEquals(LauncherLanguage.HEBREW, LauncherLanguage.fromLanguageTag("iw"))
        assertEquals(LauncherLanguage.HEBREW, LauncherLanguage.fromLanguageTag("he-IL"))
        assertEquals(LauncherLanguage.SYSTEM, LauncherLanguage.fromLanguageTag(""))
        assertEquals(LauncherLanguage.SYSTEM, LauncherLanguage.fromLanguageTag("ko"))
    }

    @Test
    fun everyLanguageReadsBackFromItsOwnTag() {
        LauncherLanguage.entries.filter { it.tag != null }.forEach { language ->
            assertEquals(language, LauncherLanguage.fromLanguageTag(requireNotNull(language.tag)))
        }
    }
}
