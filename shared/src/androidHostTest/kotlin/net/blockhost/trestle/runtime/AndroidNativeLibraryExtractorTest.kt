package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.metadata.Architecture
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidNativeLibraryExtractorTest {
    @Test
    fun selectsOnlyRequestedAbiAcrossArchiveLayouts() = withTempDirectory { root ->
        val archive = root.resolve("components.aar")
        val arm = elfHeader(183)
        val x64 = elfHeader(62)
        val layouts = listOf(
            "jni" to "libaudio.so",
            "lib" to "libbridge.so",
            "assets/components/lwjgl-3.4.1-natives" to "libgraphics.so",
        )
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            for ((prefix, name) in layouts) {
                for ((abi, bytes) in listOf("arm64-v8a" to arm, "x86_64" to x64)) {
                    zip.putNextEntry(ZipEntry("$prefix/$abi/$name"))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }
        for ((abi, expected) in listOf(AndroidRuntimeAbi.ARM64 to arm, AndroidRuntimeAbi.X64 to x64)) {
            val destination = root.resolve(abi.directoryName)
            AndroidNativeLibraryExtractor.extract(archive.toString().toPath(), destination.toString().toPath(), abi)
            val files = requireNotNull(destination.toFile().listFiles())
            assertEquals(3, files.size)
            files.forEach { assertContentEquals(expected, it.readBytes()) }
        }
    }

    @Test
    fun rejectsMislabeledLibraryInsteadOfTrustingArchivePath() = withTempDirectory { root ->
        val archive = root.resolve("components.aar")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("jni/x86_64/libaudio.so"))
            zip.write(elfHeader(183))
            zip.closeEntry()
        }
        assertFailsWith<LauncherException.InvalidMetadata> {
            AndroidNativeLibraryExtractor.extract(archive.toString().toPath(), root.resolve("natives").toString().toPath(), AndroidRuntimeAbi.X64)
        }
    }

    @Test
    fun rejectsTruncatedAndWrongFormatLibraries() = withTempDirectory { root ->
        val library = root.resolve("libtest.so")
        for (header in listOf(ByteArray(3), elfHeader(62).also { it[4] = 1 }, elfHeader(62).also { it[5] = 2 })) {
            Files.write(library, header)
            assertFailsWith<LauncherException.InvalidMetadata> { AndroidRuntimeAbi.X64.verifyLibrary(library.toString().toPath()) }
        }
    }

    @Test
    fun removesWritePermissionsBeforeNativeLibrariesAreLoaded() = withTempDirectory { root ->
        val library = root.resolve("libtest.so")
        Files.write(library, elfHeader(62))
        Files.setPosixFilePermissions(library, PosixFilePermissions.fromString("rwxrwxrwx"))

        AndroidNativeLibraryPermissions.makeReadOnly(library.toString().toPath())

        assertEquals(
            PosixFilePermissions.fromString("r-xr-xr-x"),
            Files.getPosixFilePermissions(library),
        )
    }

    @Test
    fun rejectsUnsupportedJavaAndArchitectureCombinations() {
        for (architecture in listOf(Architecture.ARM64, Architecture.X86_64)) {
            assertEquals(architecture, AndroidRuntimeArtifact.forPlatform(25, architecture).architecture)
            assertFailsWith<LauncherException.RuntimeUnavailable> { AndroidRuntimeArtifact.forPlatform(21, architecture) }
        }
        for (architecture in listOf(Architecture.ARM32, Architecture.X86, Architecture.UNKNOWN)) {
            assertFailsWith<LauncherException.RuntimeUnavailable> { AndroidRuntimeAbi.forArchitecture(architecture) }
            assertFailsWith<LauncherException.RuntimeUnavailable> { AndroidRuntimeArtifact.forPlatform(25, architecture) }
        }
    }

    private fun elfHeader(machine: Int) = ByteArray(20).apply {
        byteArrayOf(0x7f, 0x45, 0x4c, 0x46, 2, 1, 1).copyInto(this)
        this[18] = machine.toByte()
        this[19] = (machine ushr 8).toByte()
    }

    private fun withTempDirectory(block: (java.nio.file.Path) -> Unit) {
        val root = Files.createTempDirectory("trestle-native-abi-test")
        try { block(root) } finally { root.toFile().deleteRecursively() }
    }
}
