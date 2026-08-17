package net.blockhost.trestle.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val client = MinecraftProfileClient(HttpClient(engine))

        val profile = client.fetchProfile(AuthSession("Alex", "profile-a", SecretValue("session-secret")))

        assertEquals("Alex", profile.playerName)
        assertEquals(SkinVariant.SLIM, profile.skin?.variant)
        assertEquals("https://textures.minecraft.net/texture/example", profile.skin?.url)
    }

    @Test
    fun uploadsAValidatedSkinWithTheSelectedModel() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("Bearer session-secret", request.headers[HttpHeaders.Authorization])
            assertTrue(request.body.contentType?.toString()?.startsWith("multipart/form-data") == true)
            respond(
                content = """
                    {
                      "id": "profile-a",
                      "name": "Alex",
                      "skins": [{"id":"skin-b","state":"ACTIVE","url":"https://textures.minecraft.net/texture/new","variant":"SLIM"}]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
            )
        }
        val client = MinecraftProfileClient(HttpClient(engine))

        val profile = client.uploadSkin(
            AuthSession("Alex", "profile-a", SecretValue("session-secret")),
            minecraftSkinPng(),
            SkinVariant.SLIM,
        )

        assertEquals("skin-b", profile.skin?.id)
        assertEquals(SkinVariant.SLIM, profile.skin?.variant)
    }

    @Test
    fun resetsTheActiveSkinAndReloadsTheProfile() = runTest {
        var requestNumber = 0
        val engine = MockEngine { request ->
            requestNumber++
            assertEquals("Bearer session-secret", request.headers[HttpHeaders.Authorization])
            when (requestNumber) {
                1 -> {
                    assertEquals(HttpMethod.Delete, request.method)
                    assertTrue(request.url.encodedPath.endsWith("/minecraft/profile/skins/active"))
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
                else -> {
                    assertEquals(HttpMethod.Get, request.method)
                    respond(
                        content = """
                            {
                              "id": "profile-a",
                              "name": "Alex",
                              "skins": [{"id":"default","state":"ACTIVE","url":"https://textures.minecraft.net/texture/default","variant":"CLASSIC"}]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                    )
                }
            }
        }
        val client = MinecraftProfileClient(HttpClient(engine))

        val profile = client.resetActiveSkin(AuthSession("Alex", "profile-a", SecretValue("session-secret")))

        assertEquals(2, requestNumber)
        assertEquals("default", profile.skin?.id)
    }
}
