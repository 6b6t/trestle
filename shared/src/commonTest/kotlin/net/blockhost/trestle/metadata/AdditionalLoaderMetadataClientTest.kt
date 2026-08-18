package net.blockhost.trestle.metadata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdditionalLoaderMetadataClientTest {
    @Test
    fun verifiesForgeComponentProfile() = runTest {
        val profile =
            """{"version":"52.1.16","mainClass":"io.example.ForgeWrapper","libraries":[],"mavenFiles":[{"name":"net.minecraftforge:forge:1.21.1-52.1.16:installer"}],"minecraftArguments":"--launchTarget forge_client","requires":[{"uid":"net.minecraft","equals":"1.21.1"}]}"""
        val checksum = profile.encodeUtf8().sha256().hex()
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/forge/index.json" -> respond(
                    """{"versions":[{"version":"52.1.16","recommended":true,"sha256":"$checksum","requires":[{"uid":"net.minecraft","equals":"1.21.1"}]}]}""",
                    headers = jsonHeaders,
                )
                "/forge/52.1.16.json" -> respond(profile, headers = jsonHeaders)
                else -> error("Unexpected request: ${request.url}")
            }
        })
        val metadata = ForgeMetadataClient(client, "Trestle test", "https://meta.test/forge")

        val versions = metadata.loaderVersions("1.21.1")
        val installProfile = metadata.profile("1.21.1", versions.single().version)

        assertTrue(versions.single().recommended)
        assertEquals("forge-52.1.16", installProfile.metadata.id)
        assertEquals("net.minecraftforge:forge:1.21.1-52.1.16:installer", installProfile.mavenFiles.single().name)
    }

    @Test
    fun readsQuiltLoaderAndLaunchProfile() = runTest {
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v3/versions/loader/1.21.1" -> respond(
                    """[{"loader":{"maven":"org.quiltmc:quilt-loader:0.27.1","version":"0.27.1","build":27}}]""",
                    headers = jsonHeaders,
                )
                "/v3/versions/loader/1.21.1/0.27.1/profile/json" -> respond(
                    """{"id":"quilt-loader-0.27.1-1.21.1","inheritsFrom":"1.21.1","mainClass":"org.quiltmc.loader.impl.launch.knot.KnotClient","libraries":[]}""",
                    headers = jsonHeaders,
                )
                else -> error("Unexpected request: ${request.url}")
            }
        })
        val metadata = QuiltMetadataClient(client, "https://meta.test/v3")

        val version = metadata.loaderVersions("1.21.1").single()
        val profile = metadata.profile("1.21.1", version.version)

        assertTrue(version.stable)
        assertEquals("org.quiltmc.loader.impl.launch.knot.KnotClient", profile.mainClass)
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
