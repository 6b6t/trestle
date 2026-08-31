package net.blockhost.trestle.auth

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.blockhost.trestle.domain.LauncherException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileAccountManagerTest {
    @Test
    fun skipsUnsupportedAuthenticationWithoutLosingSupportedAccountsOrSelection() = runTest {
        val supported = listOf(
            LauncherAccount("profile-a", "Alex"),
            LauncherAccount("profile-b", "Sam", authenticationMethod = AccountAuthenticationMethod.OFFLINE),
        )
        val unsupported = LauncherAccount("profile-c", "Removed")
        val records = supported.map { Json.encodeToJsonElement(it) } + JsonObject(
            Json.encodeToJsonElement(unsupported).jsonObject +
                ("authenticationMethod" to JsonPrimitive("REMOVED_PROVIDER")),
        )

        for (selected in supported + unsupported) {
            val fileSystem = FakeFileSystem()
            val registry = "/data/accounts.json".toPath()
            fileSystem.createDirectories(requireNotNull(registry.parent))
            fileSystem.write(registry) {
                writeUtf8(buildJsonObject {
                    put("activeProfileId", selected.profileId)
                    put("accounts", JsonArray(records))
                }.toString())
            }

            val manager = FileAccountManager(fileSystem, registry)
            manager.initialize()

            assertEquals(supported, manager.accounts.value.map { it.profile })
            assertEquals(
                selected.takeIf { it in supported }?.profileId,
                manager.accounts.value.singleOrNull { it.isActive }?.profile?.profileId,
            )
            assertNull(manager.currentSession(unsupported.profileId))
        }
    }

    @Test
    fun rejectsMalformedSupportedAccounts() = runTest {
        val fileSystem = FakeFileSystem()
        val registry = "/data/accounts.json".toPath()
        fileSystem.createDirectories(requireNotNull(registry.parent))
        fileSystem.write(registry) {
            writeUtf8("""{"accounts":[{"authenticationMethod":"OFFLINE"}]}""")
        }

        assertFailsWith<LauncherException.FileSystem> {
            FileAccountManager(fileSystem, registry).initialize()
        }
    }

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
        assertEquals("profile-a", manager.currentSession("profile-a")?.profileId)
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
