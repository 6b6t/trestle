package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadRequest
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RestrictedDownloadsTest {
    @Test fun acceptsOnlyTheRequestedArtifactAndRevalidatesBeforeInstallation() = runTest {
        val fs = FakeFileSystem()
        val bytes = byteArrayOf(1, 2, 3, 4)
        val download = RestrictedDownload("mod.jar", bytes.toByteString().sha1().hex(), "https://example.test/download")
        val cache = RestrictedDownloads(fs, "/manual".toPath())
        assertFailsWith<IllegalArgumentException> { cache.accept(download, byteArrayOf(9)) }
        assertNull(cache.find(download.sha1))
        cache.accept(download, bytes)
        val source = assertNotNull(cache.find(download.sha1))
        val pipeline = DownloadPipeline(HttpClient(MockEngine { error("A verified manual download must not use the network.") }), fs)
        val target = "/game/mod.jar".toPath()
        pipeline.download(listOf(DownloadRequest("", target, sha1 = download.sha1, localSource = source)), "/staging".toPath())
        assertContentEquals(bytes, fs.read(target) { readByteArray() })
        fs.write(source) { writeByte(9) }
        assertNull(cache.find(download.sha1))
        assertFailsWith<LauncherException.ChecksumMismatch> {
            pipeline.download(listOf(DownloadRequest("", "/other/mod.jar".toPath(), sha1 = download.sha1, localSource = source)), "/staging".toPath())
        }
        assertFalse(fs.exists("/other/mod.jar".toPath()))
    }

    @Test fun collectsEveryMissingFileAndMatchesSelectedFilesByChecksum() {
        val fs = FakeFileSystem()
        val cache = RestrictedDownloads(fs, "/manual".toPath())
        val first = byteArrayOf(1, 2, 3)
        val second = byteArrayOf(4, 5, 6)
        val missing = mutableListOf<RestrictedDownload>()
        listOf("first.jar" to first, "second.jar" to second).forEach { (name, bytes) ->
            val file = ResourceFile(id = null, fileName = name, url = null, sha1 = bytes.toByteString().sha1().hex(), size = bytes.size.toLong(), primary = true)
            assertNull(cache.locate(file, "https://example.test/$name", missing))
        }

        assertEquals(listOf("first.jar", "second.jar"), missing.map { it.fileName })
        assertEquals("second.jar", cache.match(missing, second)?.fileName)
        assertNull(cache.match(missing, byteArrayOf(7)))

        cache.accept(missing[0], first)
        val stillMissing = mutableListOf<RestrictedDownload>()
        val firstFile = ResourceFile(id = null, fileName = "first.jar", url = null, sha1 = missing[0].sha1, size = 3, primary = true)
        assertNotNull(cache.locate(firstFile, "https://example.test/first.jar", stillMissing))
        assertEquals(emptyList(), stillMissing)
    }
}
