package net.blockhost.trestle.auth

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountAuthenticationTest {
    @Test
    fun usesOfficialMinecraftTitleConfigurations() {
        assertEquals("00000000402b5328", OfficialMinecraftApplications.java.clientId)
        assertEquals("0000000040159362", OfficialMinecraftApplications.bedrockDesktop.clientId)
        assertEquals("0000000048183522", OfficialMinecraftApplications.bedrockAndroid.clientId)
        assertEquals(
            OfficialMinecraftApplications.TITLE_AUTH_SCOPE,
            OfficialMinecraftApplications.java.scope,
        )
    }

    @Test
    fun restoresJavaSessionFromCredentialStore() = runTest {
        val fileSystem = FakeFileSystem()
        val registry = "/data/accounts.json".toPath()
        val store = FakeCredentialStore()
        val authenticator = FakeAuthenticator()
        val manager = FileAccountManager(
            fileSystem,
            registry,
            credentialStore = store,
            authenticator = authenticator,
        )
        manager.initialize()

        manager.addAccount(AccountLoginRequest.DeviceCode(AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE)) {}

        assertEquals("profile-a", manager.currentSession()?.profileId)
        assertTrue(store.states.keys.any { it.first == "profile-a" })

        val restored = FileAccountManager(
            fileSystem,
            registry,
            credentialStore = store,
            authenticator = authenticator,
        )
        restored.initialize()

        assertEquals("profile-a", restored.currentSession()?.profileId)
        assertTrue(restored.accounts.value.single().isAuthenticated)

        restored.signOut("profile-a")

        assertNull(restored.currentSession())
        assertTrue(store.states.isEmpty())
    }

    @Test
    fun keepsOfflineAccountsReadyWithoutSavingCredentials() = runTest {
        val fileSystem = FakeFileSystem()
        val store = FakeCredentialStore()
        val manager = FileAccountManager(
            fileSystem,
            "/data/accounts.json".toPath(),
            credentialStore = store,
            authenticator = FakeAuthenticator(),
        )
        manager.initialize()

        manager.addAccount(AccountLoginRequest.Offline("Alex_Offline")) {}

        val account = manager.accounts.value.single()
        assertEquals(AccountAuthenticationMethod.OFFLINE, account.profile.authenticationMethod)
        assertTrue(account.isReady)
        assertTrue(!account.isAuthenticated)
        assertTrue(store.states.isEmpty())
        assertNull(manager.currentSession()?.accessToken)
    }
}

private class FakeCredentialStore : AccountCredentialStore {
    override val protection = CredentialProtection(true, "HARDWARE", "HARDWARE", emptyList())
    val states = mutableMapOf<Pair<String, MinecraftEdition>, SecretValue>()

    override suspend fun read(profileId: String, edition: MinecraftEdition): SecretValue? =
        states[profileId to edition]

    override suspend fun write(profileId: String, edition: MinecraftEdition, state: SecretValue) {
        states[profileId to edition] = state
    }

    override suspend fun remove(profileId: String, edition: MinecraftEdition) {
        states.remove(profileId to edition)
    }
}

private class FakeAuthenticator : MinecraftAuthenticator {
    override suspend fun authenticate(
        request: AccountLoginRequest,
        onDeviceAuthorization: (DeviceAuthorization) -> Unit,
    ): AuthenticatedMinecraftAccount = if (request is AccountLoginRequest.Offline) {
        AuthenticatedMinecraftAccount(
            edition = MinecraftEdition.JAVA,
            profile = LauncherAccount(
                "offline-profile",
                request.username,
                authenticationMethod = AccountAuthenticationMethod.OFFLINE,
            ),
            javaSession = AuthSession(
                request.username,
                "offline-profile",
                null,
            ),
            serializedState = SecretValue("offline-state"),
        )
    } else {
        result(request.edition, request.method)
    }

    override suspend fun restore(
        profile: LauncherAccount,
        serializedState: SecretValue,
    ): AuthenticatedMinecraftAccount = result(profile.edition, profile.authenticationMethod)

    private fun result(
        edition: MinecraftEdition,
        method: AccountAuthenticationMethod = AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
    ) = AuthenticatedMinecraftAccount(
        edition = edition,
        profile = LauncherAccount("profile-a", "Alex", edition, method),
        javaSession = AuthSession(
            "Alex",
            "profile-a",
            SecretValue("minecraft-token"),
        ),
        serializedState = SecretValue("serialized-auth-state"),
    )
}
