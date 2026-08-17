package net.blockhost.trestle.metadata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MinecraftMetadataClientTest {
    @Test
    fun rejectsVersionMetadataWithWrongManifestChecksum() = runTest {
        val client = MinecraftMetadataClient(HttpClient(MockEngine { respond("{}") }))

        assertFailsWith<LauncherException.ChecksumMismatch> {
            client.fetchVersion(
                VersionReference(
                    id = "test",
                    type = "release",
                    url = "https://example.test/version.json",
                    sha1 = "0000000000000000000000000000000000000000",
                ),
            )
        }
    }
}
