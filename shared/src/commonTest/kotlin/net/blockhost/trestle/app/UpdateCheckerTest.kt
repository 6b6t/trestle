package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCheckerTest {
    @Test
    fun selectsAndroidApkForProcessArchitecture() = runTest {
        val artifacts = listOf("arm64" to "apk", "x64" to "apk", "universal" to "apk", "universal" to "aab", "x64" to "deb")
        val manifest = ReleaseManifest(1, "2.0.0", artifacts.map { (arch, format) ->
            ReleaseArtifact(if (format == "deb") "linux" else "android", arch, format,
                "https://example.test/$arch.$format", "a".repeat(64), 100, "Android 8.1")
        })
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("manifest")) {
                respond(Json.encodeToString(ReleaseManifest.serializer(), manifest))
            } else respond("""{"tag_name":"2.0.0","html_url":"https://example.test/release","assets":[{"name":"release-manifest.json","browser_download_url":"https://example.test/manifest"}]}""")
        })
        for ((architecture, expected) in listOf(Architecture.ARM64 to "arm64", Architecture.X86_64 to "x64")) {
            val checker = UpdateChecker(client, environment = PlatformEnvironment(
                OperatingSystem.LINUX, architecture), isMobile = true)
            assertEquals(manifest.artifacts.single { it.architecture == expected && it.format == "apk" },
                checker.availableUpdate("1.0.0")?.downloads?.single())
        }
    }

    @Test
    fun treatsUnpublishedReleasesAsNoUpdate() = runTest {
        val checker = UpdateChecker(HttpClient(MockEngine { respond("", io.ktor.http.HttpStatusCode.NotFound) }))
        assertNull(checker.availableUpdate("0.1.0"))
    }

    @Test
    fun selectsOnlyMatchingInstallersAndRejectsBrokenChecksums() = runTest {
        val release = """{"tag_name":"v2.0.0","html_url":"https://example.test/release","assets":[{"name":"release-manifest.json","browser_download_url":"https://example.test/manifest"}]}"""
        val hash = "a".repeat(64)
        var valid = true
        val checker = UpdateChecker(HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("manifest")) respond("""{"schemaVersion":1,"version":"2.0.0","artifacts":[
                {"platform":"windows","architecture":"x64","format":"msi","url":"https://example.test/x64.msi","sha256":"${if (valid) hash else "bad"}","size":100,"minimumOS":"Windows 10"},
                {"platform":"windows","architecture":"arm64","format":"msi","url":"https://example.test/arm64.msi","sha256":"$hash","size":100,"minimumOS":"Windows 10"}
            ]}""") else respond(release)
        }), environment = PlatformEnvironment(OperatingSystem.WINDOWS, Architecture.ARM64))
        assertEquals("arm64", checker.availableUpdate("1.0.0")?.downloads?.single()?.architecture)
        valid = false
        kotlin.test.assertFailsWith<IllegalArgumentException> { checker.availableUpdate("1.0.0") }
    }

    @Test
    fun skipsDraftsAndOptsIntoPrereleases() = runTest {
        val checker = UpdateChecker(HttpClient(MockEngine { respond("""[
            {"tag_name":"v9.0.0","html_url":"https://example.test/draft","draft":true},
            {"tag_name":"v2.0.0-beta.2","html_url":"https://example.test/beta","prerelease":true},
            {"tag_name":"v1.0.0","html_url":"https://example.test/stable"}
        ]""") }))
        assertEquals("2.0.0-beta.2", checker.availableUpdate("2.0.0-beta.1", includePrereleases = true)?.version)
        assertNull(checker.availableUpdate("2.0.0", includePrereleases = true))
    }

    @Test
    fun returnsOnlyNewerSemanticRelease() = runTest {
        val newer = UpdateChecker(
            HttpClient(MockEngine { respond("""{"tag_name":"v1.2.0","html_url":"https://example.test/1.2.0"}""") }),
            "https://example.test/latest",
        )
        val current = UpdateChecker(
            HttpClient(MockEngine { respond("""{"tag_name":"v1.1.0","html_url":"https://example.test/1.1.0"}""") }),
            "https://example.test/latest",
        )

        assertEquals("1.2.0", newer.availableUpdate("1.1.9")?.version)
        assertNull(current.availableUpdate("1.1.0"))
    }
}
