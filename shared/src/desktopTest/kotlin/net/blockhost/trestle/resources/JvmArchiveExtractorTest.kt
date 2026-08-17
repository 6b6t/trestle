package net.blockhost.trestle.resources

import net.blockhost.trestle.domain.LauncherException
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class JvmArchiveExtractorTest {
    @Test
    fun extractsNestedFilesInsideDestination() {
        val temporary = createTempDirectory("trestle-archive-test")
        try {
            val archive = temporary.resolve("pack.zip")
            writeArchive(archive, "overrides/config/options.txt" to "enabled")
            val destination = temporary.resolve("output")

            JvmArchiveExtractor().extract(archive.toString().toPath(), destination.toString().toPath())

            assertEquals("enabled", Files.readString(destination.resolve("overrides/config/options.txt")))
        } finally {
            temporary.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsPathsOutsideDestination() {
        val temporary = createTempDirectory("trestle-archive-test")
        try {
            val archive = temporary.resolve("pack.zip")
            writeArchive(archive, "../outside.txt" to "unsafe")
            val destination = temporary.resolve("output")

            assertFailsWith<LauncherException.InvalidMetadata> {
                JvmArchiveExtractor().extract(archive.toString().toPath(), destination.toString().toPath())
            }
            assertFalse(Files.exists(temporary.resolve("outside.txt")))
        } finally {
            temporary.toFile().deleteRecursively()
        }
    }

    private fun writeArchive(path: java.nio.file.Path, entry: Pair<String, String>) {
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            zip.putNextEntry(ZipEntry(entry.first))
            zip.write(entry.second.encodeToByteArray())
            zip.closeEntry()
        }
    }
}
