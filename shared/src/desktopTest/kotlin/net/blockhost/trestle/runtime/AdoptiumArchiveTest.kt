package net.blockhost.trestle.runtime

import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import okio.Path.Companion.toPath
import kotlin.test.*

class AdoptiumArchiveTest {
    @Test fun extractsExecutablesAndConfinesSymlinksToTheRuntime() {
        val root = Files.createTempDirectory("trestle-runtime-test")
        try {
            fun archive(link: String): java.nio.file.Path {
                val path = root.resolve("runtime.tar.gz")
                TarArchiveOutputStream(GZIPOutputStream(Files.newOutputStream(path))).use { tar ->
                    val java = TarArchiveEntry("jdk/bin/java").apply { size = 1; mode = 493 }
                    tar.putArchiveEntry(java); tar.write(byteArrayOf(1)); tar.closeArchiveEntry()
                    tar.putArchiveEntry(TarArchiveEntry("jdk/lib/java-link", TarConstants.LF_SYMLINK).apply { linkName = link })
                    tar.closeArchiveEntry()
                }
                return path
            }
            val valid = root.resolve("valid")
            Files.createDirectories(valid)
            extractTemurinArchive(archive("../bin/java").toString().toPath(), valid.toString().toPath())
            assertTrue(Files.isExecutable(valid.resolve("bin/java")))
            assertTrue(Files.isSymbolicLink(valid.resolve("lib/java-link")))
            val invalid = root.resolve("invalid")
            Files.createDirectories(invalid)
            assertFailsWith<IllegalArgumentException> {
                extractTemurinArchive(archive("../../../outside").toString().toPath(), invalid.toString().toPath())
            }
            assertFalse(Files.exists(root.resolve("outside")))
        } finally { root.toFile().deleteRecursively() }
    }
}
