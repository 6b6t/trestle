package net.blockhost.trestle.metadata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NeoForgeMetadataClientTest {
    @Test
    fun filtersVersionsAndBuildsVerifiedInstallProfile() = runTest {
        val profileJson = profileJson("21.1.1")
        val profileSha256 = profileJson.encodeUtf8().sha256().hex()
        val engine = MockEngine { request ->
            assertEquals("Trestle test", request.headers[HttpHeaders.UserAgent])
            when (request.url.encodedPath) {
                "/v1/net.neoforged/index.json" -> respond(
                    indexJson(profileSha256),
                    headers = jsonHeaders,
                )
                "/v1/net.neoforged/21.1.1.json" -> respond(profileJson, headers = jsonHeaders)
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val client = NeoForgeMetadataClient(
            HttpClient(engine),
            userAgent = "Trestle test",
            baseUrl = "https://meta.test/v1/net.neoforged",
        )

        val versions = client.loaderVersions("1.21.1")
        val profile = client.profile("1.21.1", versions.single().version)

        assertEquals("21.1.1", versions.single().version)
        assertEquals("neoforge-21.1.1", profile.metadata.id)
        assertEquals("io.example.Wrapper", profile.metadata.mainClass)
        assertEquals("1.21.1", profile.metadata.inheritsFrom)
        assertEquals("example:launch:1.0", profile.metadata.libraries.single().name)
        assertEquals("example:generated:1.0", profile.mavenFiles.single().name)
    }

    @Test
    fun rejectsProfileWithUnexpectedChecksum() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/net.neoforged/index.json" -> respond(indexJson("0".repeat(64)), headers = jsonHeaders)
                else -> respond(profileJson("21.1.1"), headers = jsonHeaders)
            }
        }
        val client = NeoForgeMetadataClient(
            HttpClient(engine),
            userAgent = "Trestle test",
            baseUrl = "https://meta.test/v1/net.neoforged",
        )

        assertFailsWith<LauncherException.ChecksumMismatch> {
            client.profile("1.21.1", "21.1.1")
        }
    }

    private fun indexJson(sha256: String) =
        """{"versions":[{"version":"21.1.1","recommended":true,"releaseTime":"2026-01-02T00:00:00Z","sha256":"$sha256","requires":[{"uid":"net.minecraft","equals":"1.21.1"}]},{"version":"21.0.1","sha256":"unused","requires":[{"uid":"net.minecraft","equals":"1.21"}]}]}"""

    private fun profileJson(version: String) =
        """{"version":"$version","mainClass":"io.example.Wrapper","libraries":[{"name":"example:launch:1.0","downloads":{"artifact":{"url":"https://cdn.test/launch.jar","sha1":"abc","size":1}}}],"mavenFiles":[{"name":"example:generated:1.0","downloads":{"artifact":{"url":"https://cdn.test/generated.jar","sha1":"def","size":2}}}],"minecraftArguments":"--launchTarget neoforgeclient","requires":[{"uid":"net.minecraft","equals":"1.21.1"}]}"""

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
