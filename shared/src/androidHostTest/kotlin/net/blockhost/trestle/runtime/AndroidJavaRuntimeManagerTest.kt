package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.LauncherException
import okio.Path.Companion.toPath
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidJavaRuntimeManagerTest {
    @Test
    fun extractsArchiveWithRootDirectoryEntry() {
        val testRoot = Files.createTempDirectory("trestle-android-runtime-test")
        try {
            val archive = testRoot.resolve("runtime.tar.xz")
            val destination = testRoot.resolve("runtime")
            writeTarXz(
                archive,
                entries = listOf(
                    TarEntry(name = "./", type = '5'),
                    TarEntry(name = "./release", content = "JAVA_VERSION=25\n".encodeToByteArray()),
                ),
            )

            AndroidTarXzExtractor.extract(archive.toString().toPath(), destination.toString().toPath())

            assertEquals("JAVA_VERSION=25\n", Files.readString(destination.resolve("release")))
        } finally {
            testRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsUnnamedFileEntry() {
        val testRoot = Files.createTempDirectory("trestle-android-runtime-test")
        try {
            val archive = testRoot.resolve("runtime.tar.xz")
            val destination = testRoot.resolve("runtime")
            writeTarXz(archive, entries = listOf(TarEntry(name = "", content = byteArrayOf(1))))

            assertFailsWith<LauncherException.InvalidMetadata> {
                AndroidTarXzExtractor.extract(archive.toString().toPath(), destination.toString().toPath())
            }
        } finally {
            testRoot.toFile().deleteRecursively()
        }
    }

    private fun writeTarXz(archive: java.nio.file.Path, entries: List<TarEntry>) {
        XZOutputStream(Files.newOutputStream(archive), LZMA2Options()).use { output ->
            entries.forEach { entry ->
                val header = ByteArray(TAR_BLOCK_SIZE)
                header.writeString(0, entry.name)
                header.writeOctal(100, 8, 0b111_101_101)
                header.writeOctal(124, 12, entry.content.size)
                header[156] = entry.type.code.toByte()
                output.write(header)
                output.write(entry.content)
                output.write(ByteArray(padding(entry.content.size)))
            }
            output.write(ByteArray(TAR_BLOCK_SIZE * 2))
        }
    }

    private fun ByteArray.writeString(offset: Int, value: String) {
        value.encodeToByteArray().copyInto(this, destinationOffset = offset)
    }

    private fun ByteArray.writeOctal(offset: Int, length: Int, value: Int) {
        val encoded = value.toString(8).padStart(length - 1, '0').encodeToByteArray()
        encoded.copyInto(this, destinationOffset = offset)
    }

    private fun padding(size: Int): Int = (TAR_BLOCK_SIZE - size % TAR_BLOCK_SIZE) % TAR_BLOCK_SIZE

    private data class TarEntry(
        val name: String,
        val type: Char = '0',
        val content: ByteArray = byteArrayOf(),
    )

    private companion object {
        const val TAR_BLOCK_SIZE = 512
    }
}
