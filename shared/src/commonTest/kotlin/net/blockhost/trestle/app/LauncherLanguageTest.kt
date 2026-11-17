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
}
