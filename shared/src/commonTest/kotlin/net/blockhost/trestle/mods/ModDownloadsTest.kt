package net.blockhost.trestle.mods

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.ModLoader
import kotlin.test.Test
import kotlin.test.assertEquals

class ModDownloadsTest {
    @Test
    fun resolvesPrimaryModrinthFileForLoaderAndGameVersion() = runTest {
        val engine = MockEngine { request ->
            assertEquals("[\"1.21.8\"]", request.url.parameters["game_versions"])
            assertEquals("[\"fabric\"]", request.url.parameters["loaders"])
            respond(
                """[{"id":"version-1","project_id":"project-1","files":[{"hashes":{"sha1":"abc"},"url":"https://cdn.test/mod.jar","filename":"mod.jar","primary":true,"size":42}]}]""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val download = ModrinthDownloadProvider(HttpClient(engine), "Trestle test").resolve(
            "project-1",
            "1.21.8",
            ModLoader.FABRIC,
        )

        assertEquals("mod.jar", download.fileName)
        assertEquals("abc", download.sha1)
    }

    @Test
    fun sendsCurseForgeApiKeyAndSelectsSha1() = runTest {
        val engine = MockEngine { request ->
            assertEquals("secret-key", request.headers["x-api-key"])
            assertEquals("4", request.url.parameters["modLoaderType"])
            respond(
                """{"data":[{"id":7,"fileName":"mod.jar","downloadUrl":"https://cdn.test/mod.jar","fileLength":9,"hashes":[{"value":"def","algo":1}]}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val download = CurseForgeDownloadProvider(HttpClient(engine), "secret-key").resolve(
            "123",
            "1.21.8",
            ModLoader.FABRIC,
        )

        assertEquals("7", download.versionId)
        assertEquals("def", download.sha1)
    }
}
