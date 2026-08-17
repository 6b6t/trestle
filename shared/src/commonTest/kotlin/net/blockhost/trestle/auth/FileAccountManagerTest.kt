package net.blockhost.trestle.auth

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileAccountManagerTest {
    @Test
    fun persistsProfilesAndSelectionWithoutPersistingSessions() = runTest {
        val fileSystem = FakeFileSystem()
        val registry = "/data/accounts.json".toPath()
        val manager = FileAccountManager(fileSystem, registry)
        manager.initialize()
        manager.register(
            AuthSession("Alex", "profile-a", SecretValue("secret-a")),
            LauncherAccount("profile-a", "Alex"),
        )
        manager.register(
            AuthSession("Sam", "profile-b", SecretValue("secret-b")),
            LauncherAccount("profile-b", "Sam"),
        )
        manager.select("profile-b")

        assertEquals("profile-b", manager.currentSession()?.profileId)
        assertFalse(fileSystem.read(registry) { readUtf8() }.contains("secret-a"))
        assertFalse(fileSystem.read(registry) { readUtf8() }.contains("secret-b"))

        val reloaded = FileAccountManager(fileSystem, registry)
        reloaded.initialize()

        assertEquals(2, reloaded.accounts.value.size)
        assertTrue(reloaded.accounts.value.single { it.profile.profileId == "profile-b" }.isActive)
        assertTrue(reloaded.accounts.value.none { it.isAuthenticated })
        assertNull(reloaded.currentSession())
    }
}
