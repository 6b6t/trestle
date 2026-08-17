package net.blockhost.trestle.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftProfileClientTest {
    @Test
    fun loadsActiveSkinWithoutExposingTheAccessToken() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer session-secret", request.headers[HttpHeaders.Authorization])
            respond(
                content = """
                    {
                      "id": "profile-a",
                      "name": "Alex",
                      "skins": [
                        {
                          "id": "skin-a",
                          "state": "ACTIVE",
                          "url": "http://textures.minecraft.net/texture/example",
                          "variant": "SLIM"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
            )
        }
        val client = MinecraftProfileClient(HttpClient(engine), FakeFileSystem())

        val profile = client.fetchProfile(AuthSession("Alex", "profile-a", SecretValue("session-secret")))

        assertEquals("Alex", profile.playerName)
        assertEquals(SkinVariant.SLIM, profile.skin?.variant)
        assertEquals("https://textures.minecraft.net/texture/example", profile.skin?.url)
    }
}
