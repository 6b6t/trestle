package net.blockhost.trestle.app

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherPreferencesStoreTest {
    private val fileSystem = FakeFileSystem()
    private val path = "/trestle/preferences.json".toPath()

    @Test
    fun keepsReadableSettingsWhenOneValueIsUnknown() {
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) {
            writeUtf8("""{"theme":"NEON","folders":{"instances":"/games/minecraft"},"language":"GERMAN"}""")
        }

        val preferences = LauncherPreferencesStore(fileSystem, path).read()

        assertEquals("/games/minecraft", preferences.folders.instances)
        assertEquals(LauncherLanguage.GERMAN, preferences.launcherLanguage)
        assertEquals(ThemePreference.SYSTEM, preferences.theme)
    }

    @Test
    fun usesDefaultsForAFileThatIsNotJson() {
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) { writeUtf8("not json") }

        assertEquals(LauncherPreferences(), LauncherPreferencesStore(fileSystem, path).read())
    }
}
