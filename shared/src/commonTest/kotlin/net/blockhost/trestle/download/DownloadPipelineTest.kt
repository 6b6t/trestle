package net.blockhost.trestle.download

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.BufferedLauncherLogger
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.logging.LogLevel
import net.blockhost.trestle.logging.LogSink
import okio.Path.Companion.toPath
import okio.ByteString.Companion.encodeUtf8
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun replacesCachedFileWhenSha512DoesNotMatch() = runTest {
        val fileSystem = FakeFileSystem()
        val destination = "/artifact.jar".toPath()
        fileSystem.write(destination) { writeUtf8("stale") }
        var requestCount = 0
        val pipeline = DownloadPipeline(
            HttpClient(MockEngine {
                requestCount++
                respond("fresh")
            }),
            fileSystem,
        )

        pipeline.download(
            requests = listOf(
                DownloadRequest(
                    url = "https://example.test/artifact",
                    destination = destination,
                    sha512 = "fresh".encodeUtf8().sha512().hex(),
                ),
            ),
            stagingDirectory = "/staging".toPath(),
        )

        assertEquals(1, requestCount)
        assertEquals("fresh", fileSystem.read(destination) { readUtf8() })
    }

    @Test
    fun replacesCachedFileWhenSha256DoesNotMatch() = runTest {
        val fileSystem = FakeFileSystem()
        val destination = "/runtime.tar.xz".toPath()
        fileSystem.write(destination) { writeUtf8("stale") }
        var requestCount = 0
        val pipeline = DownloadPipeline(
            HttpClient(MockEngine {
                requestCount++
                respond("fresh")
            }),
            fileSystem,
        )

        pipeline.download(
            requests = listOf(
                DownloadRequest(
                    url = "https://example.test/runtime",
                    destination = destination,
                    sha256 = "fresh".encodeUtf8().sha256().hex(),
                ),
            ),
            stagingDirectory = "/staging".toPath(),
        )

        assertEquals(1, requestCount)
        assertEquals("fresh", fileSystem.read(destination) { readUtf8() })
    }

    @Test
    fun reportsHttpFailureAndPreservesStaging() = runTest {
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
        assertTrue(fileSystem.exists(staging))
    }

    @Test
    fun cancellationPreservesStaging() = runTest {
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

        assertTrue(fileSystem.exists(staging))
    }

    @Test
    fun resumesFromCommittedArtifacts() = runTest {
        val fileSystem = FakeFileSystem()
        val cached = "/downloads/cached.jar".toPath()
        val missing = "/downloads/missing.jar".toPath()
        fileSystem.createDirectories(requireNotNull(cached.parent))
        fileSystem.write(cached) { writeUtf8("cached") }
        val requested = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requested += request.url.toString()
            respond("missing")
        })
        val progress = mutableListOf<DownloadProgress>()
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1)

        pipeline.download(
            requests = listOf(
                DownloadRequest("https://example.test/cached", cached, size = 6),
                DownloadRequest(
                    "https://example.test/missing",
                    missing,
                    size = 7,
                    progressLabel = "Downloading game assets",
                ),
            ),
            stagingDirectory = "/staging".toPath(),
            onProgress = progress::add,
        )

        assertEquals(listOf("https://example.test/missing"), requested)
        assertEquals("missing", fileSystem.read(missing) { readUtf8() })
        assertEquals(2, progress.last().completedFiles)
        assertEquals(13, progress.last().completedBytes)
        assertEquals("Downloading game assets", progress.last().activeLabel)
        assertFalse(fileSystem.exists("/staging".toPath()))
    }

    @Test
    fun commitsACompletedStagedArtifactWithoutDownloadingItAgain() = runTest {
        val fileSystem = FakeFileSystem()
        val staging = "/staging".toPath()
        fileSystem.createDirectories(staging)
        fileSystem.write(staging / "0.part") { writeUtf8("cached") }
        var requestCount = 0
        val client = HttpClient(MockEngine {
            requestCount++
            respond("unexpected")
        })
        val destination = "/downloads/cached.jar".toPath()
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1)

        pipeline.download(
            requests = listOf(
                DownloadRequest("https://example.test/cached", destination, size = 6),
            ),
            stagingDirectory = staging,
        )

        assertEquals(0, requestCount)
        assertEquals("cached", fileSystem.read(destination) { readUtf8() })
        assertFalse(fileSystem.exists(staging))
    }

    @Test
    fun resumesAPartialStagedArtifactWithAByteRange() = runTest {
        val fileSystem = FakeFileSystem()
        val staging = "/staging".toPath()
        fileSystem.createDirectories(staging)
        fileSystem.write(staging / "0.part") { writeUtf8("part") }
        val requestedRanges = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            requestedRanges += request.headers[HttpHeaders.Range]
            respond(
                content = "ial",
                status = HttpStatusCode.PartialContent,
                headers = headersOf(HttpHeaders.ContentRange, "bytes 4-6/7"),
            )
        })
        val destination = "/downloads/partial.jar".toPath()
        val progress = mutableListOf<DownloadProgress>()
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1)

        pipeline.download(
            requests = listOf(
                DownloadRequest("https://example.test/partial", destination, size = 7),
            ),
            stagingDirectory = staging,
            onProgress = progress::add,
        )

        assertEquals(listOf<String?>("bytes=4-"), requestedRanges)
        assertEquals("partial", fileSystem.read(destination) { readUtf8() })
        assertEquals(7L, progress.last().completedBytes)
        assertEquals(1, progress.last().completedFiles)
    }

    @Test
    fun restartsTheArtifactWhenTheServerIgnoresAByteRange() = runTest {
        val fileSystem = FakeFileSystem()
        val staging = "/staging".toPath()
        fileSystem.createDirectories(staging)
        fileSystem.write(staging / "0.part") { writeUtf8("old") }
        val requestedRanges = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            requestedRanges += request.headers[HttpHeaders.Range]
            respond("replacement")
        })
        val destination = "/downloads/replacement.jar".toPath()
        val progress = mutableListOf<DownloadProgress>()
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1)

        pipeline.download(
            requests = listOf(
                DownloadRequest("https://example.test/replacement", destination, size = 11),
            ),
            stagingDirectory = staging,
            onProgress = progress::add,
        )

        assertEquals(listOf<String?>("bytes=3-"), requestedRanges)
        assertEquals("replacement", fileSystem.read(destination) { readUtf8() })
        assertEquals(11L, progress.last().completedBytes)
    }

    @Test
    fun stopsUsingRangesForAHostThatRejectsThem() = runTest {
        val fileSystem = FakeFileSystem()
        val staging = "/staging".toPath()
        fileSystem.createDirectories(staging)
        fileSystem.write(staging / "0.part") { writeUtf8("old") }
        fileSystem.write(staging / "1.part") { writeUtf8("old") }
        val requestedRanges = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            requestedRanges += request.headers[HttpHeaders.Range]
            if (requestedRanges.size == 1) {
                respond("", HttpStatusCode.RequestedRangeNotSatisfiable)
            } else {
                respond(if (request.url.encodedPath.endsWith("first")) "replacement" else "second-file")
            }
        })
        val first = "/downloads/replacement.jar".toPath()
        val second = "/downloads/second.jar".toPath()
        val progress = mutableListOf<DownloadProgress>()
        val logEntries = mutableListOf<LogEntry>()
        val logger = BufferedLauncherLogger(
            nowMillis = { 0L },
            sink = LogSink { entry, _ -> logEntries += entry },
        )
        val pipeline = DownloadPipeline(client, fileSystem, maxConcurrency = 1, logger = logger)

        pipeline.download(
            requests = listOf(
                DownloadRequest("https://example.test/first", first, size = 11),
                DownloadRequest("https://example.test/second", second, size = 11),
            ),
            stagingDirectory = staging,
            onProgress = progress::add,
        )

        assertEquals(listOf("bytes=3-", null, null), requestedRanges)
        assertEquals("replacement", fileSystem.read(first) { readUtf8() })
        assertEquals("second-file", fileSystem.read(second) { readUtf8() })
        assertEquals(22L, progress.last().completedBytes)
        assertFalse(logEntries.any { it.level == LogLevel.WARN })
        assertTrue(logEntries.any { it.level == LogLevel.DEBUG })
    }
}
