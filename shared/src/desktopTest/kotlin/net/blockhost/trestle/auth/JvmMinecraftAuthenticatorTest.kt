package net.blockhost.trestle.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class JvmMinecraftAuthenticatorTest {
    private val authenticator = JvmMinecraftAuthenticator(
        bedrockConfiguration = OfficialMinecraftApplications.bedrockDesktop,
        nowMillis = { 1_000L },
    )

    @Test
    fun createsStandardOfflineIdentityWithoutCredentials() = runTest {
        val account = authenticator.authenticate(AccountLoginRequest.Offline("Alex")) {}

        assertEquals("36532b5ec4423dbba24cc7e55d0f979a", account.profile.profileId)
        assertEquals(AccountAuthenticationMethod.OFFLINE, account.profile.authenticationMethod)
        assertNull(account.javaSession?.accessToken)
    }

    @Test
    fun rejectsInvalidOfflineUsername() = runTest {
        val result = runCatching {
            authenticator.authenticate(AccountLoginRequest.Offline("name with spaces")) {}
        }

        assertFalse(result.isSuccess)
    }

    @Test
    fun normalizesBrowserCookieExports() {
        val json = """[{"domain":".login.live.com","name":"MSPAuth","value":"one"}]"""
        val netscape = ".login.live.com\tTRUE\t/\tTRUE\t0\tMSPAuth\ttwo"

        assertEquals("MSPAuth=one", normalizeMicrosoftCookieInput(json))
        assertEquals("MSPAuth=two", normalizeMicrosoftCookieInput(netscape))
        assertEquals("MSPAuth=three", normalizeMicrosoftCookieInput("Cookie: MSPAuth=three"))
    }
}
