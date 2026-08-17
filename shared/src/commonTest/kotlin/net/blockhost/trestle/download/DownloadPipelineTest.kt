package net.blockhost.trestle.download

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DownloadPipelineTest {
    @Test
    fun validatesSha1Checksums() = runTest {
        val fileSystem = FakeFileSystem()
        val path = "/artifact.jar".toPath()
        fileSystem.write(path) { writeUtf8("hello") }
        val pipeline = DownloadPipeline(HttpClient(MockEngine { respond("") }), fileSystem)

        pipeline.validate(path, "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d")
        assertFailsWith<LauncherException.ChecksumMismatch> {
            pipeline.validate(path, "0000000000000000000000000000000000000000")
        }
    }

    @Test
    fun reportsHttpFailureAndCleansStaging() = runTest {
        val fileSystem = FakeFileSystem()
        val client = HttpClient(MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) })
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1, maxAttempts = 1)
        val staging = "/staging".toPath()

        assertFailsWith<LauncherException.Network> {
            pipeline.download(
                listOf(DownloadRequest("https://example.test/file", "/file".toPath())),
                staging,
            )
        }
        assertFalse(fileSystem.exists(staging))
    }

    @Test
    fun cancellationCleansStaging() = runTest {
        val fileSystem = FakeFileSystem()
        val client = HttpClient(MockEngine {
            delay(Long.MAX_VALUE)
            respond("")
        })
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1)
        val staging = "/staging".toPath()
        val job = async {
            pipeline.download(
                listOf(DownloadRequest("https://example.test/file", "/file".toPath())),
                staging,
            )
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertFalse(fileSystem.exists(staging))
    }
}
