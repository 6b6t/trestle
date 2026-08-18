package net.blockhost.trestle.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadRequest
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.metadata.Architecture
import okio.FileSystem
import okio.Path
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.file.Files

internal data class AndroidJavaRuntime(
    val home: Path,
    val jvmLibrary: Path,
)

internal class AndroidJavaRuntimeManager(
    private val directories: LauncherDirectories,
    private val downloadPipeline: DownloadPipeline,
    private val fileSystem: FileSystem,
    private val logger: LauncherLogger,
) {
    suspend fun resolve(
        requiredMajor: Int,
        architecture: Architecture,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): AndroidJavaRuntime {
        val artifact = AndroidRuntimeArtifact.forPlatform(requiredMajor, architecture)
        val runtimeRoot = directories.runtimes / artifact.id
        val marker = runtimeRoot / COMPLETE_MARKER
        val jvmLibrary = runtimeRoot / "lib/server/libjvm.so"
        if (fileSystem.exists(marker) && fileSystem.exists(jvmLibrary)) {
            return AndroidJavaRuntime(runtimeRoot, jvmLibrary)
        }

        val archive = directories.runtimes / "downloads" / artifact.fileName
        downloadPipeline.download(
            requests = listOf(
                DownloadRequest(
                    url = artifact.url,
                    destination = archive,
                    size = artifact.size,
                    progressLabel = "Downloading Java ${artifact.javaMajor} for Android",
                    sha256 = artifact.sha256,
                ),
            ),
            stagingDirectory = directories.staging / "runtime-download-${artifact.id}",
            onProgress = onProgress,
        )

        withContext(Dispatchers.IO) {
            val extractionRoot = directories.runtimes / ".${artifact.id}.installing"
            deleteTree(extractionRoot)
            fileSystem.createDirectories(extractionRoot)
            try {
                AndroidTarXzExtractor.extract(
                    archive = archive,
                    destination = extractionRoot,
                )
                val extractedJvm = extractionRoot / "lib/server/libjvm.so"
                val releaseFile = extractionRoot / "release"
                if (!fileSystem.exists(extractedJvm) || !fileSystem.exists(releaseFile)) {
                    throw LauncherException.RuntimeUnavailable(
                        "The downloaded Java ${artifact.javaMajor} runtime is incomplete.",
                    )
                }
                deleteTree(runtimeRoot)
                fileSystem.atomicMove(extractionRoot, runtimeRoot)
                fileSystem.write(marker) {
                    writeUtf8("${artifact.id}\n${artifact.sha256}\n")
                    flush()
                }
                runCatching { fileSystem.delete(archive, mustExist = false) }
                logger.info(
                    "runtime",
                    "Installed Android Java runtime",
                    mapOf(
                        "javaMajor" to artifact.javaMajor,
                        "architecture" to artifact.architecture.name,
                        "source" to artifact.sourceRevision,
                    ),
                )
            } catch (error: LauncherException) {
                deleteTree(extractionRoot)
                throw error
            } catch (error: Exception) {
                deleteTree(extractionRoot)
                throw LauncherException.FileSystem("The Android Java runtime could not be installed.", error)
            }
        }
        return AndroidJavaRuntime(runtimeRoot, jvmLibrary)
    }

    private fun deleteTree(path: Path) {
        if (!fileSystem.exists(path)) return
        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

    private companion object {
        const val COMPLETE_MARKER = ".complete"
    }
}

internal data class AndroidRuntimeArtifact(
    val id: String,
    val javaMajor: Int,
    val architecture: Architecture,
    val fileName: String,
    val url: String,
    val size: Long,
    val sha256: String,
    val sourceRevision: String,
) {
    companion object {
        private val java25Arm64 = AndroidRuntimeArtifact(
            id = "java-25-arm64-20260814",
            javaMajor = 25,
            architecture = Architecture.ARM64,
            fileName = "jre25-android-arm64.tar.xz",
            url = "https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/" +
                "download_jre25/jre25-android-arm64.tar.xz",
            size = 38_031_580,
            sha256 = "d3eb7afe2240c26728a1bb440502c5f18ac3883e932d202dd7f0c9bcbbce4c37",
            sourceRevision = "FCL-Team/Android-OpenJDK-Build@7a0266e745d9b4acf400afa189b58e672900f710",
        )

        fun forPlatform(javaMajor: Int, architecture: Architecture): AndroidRuntimeArtifact =
            java25Arm64.takeIf { it.javaMajor == javaMajor && it.architecture == architecture }
                ?: throw LauncherException.RuntimeUnavailable(
                    "The Android MVP supports Java 25 on 64-bit ARM devices only.",
                )
    }
}

internal object AndroidTarXzExtractor {
    private const val BLOCK_SIZE = 512
    private const val MAX_FILES = 12_000
    private const val MAX_EXTRACTED_BYTES = 512L * 1024L * 1024L

    fun extract(archive: Path, destination: Path) {
        val root = java.nio.file.Path.of(destination.toString()).toAbsolutePath().normalize()
        Files.createDirectories(root)
        BufferedInputStream(XZInputStream(FileInputStream(archive.toString()))).use { input ->
            val header = ByteArray(BLOCK_SIZE)
            var files = 0
            var extractedBytes = 0L
            while (readBlock(input, header)) {
                if (header.all { it == 0.toByte() }) break
                val name = entryName(header).removePrefix("./")
                val size = header.octal(124, 12)
                val type = header[156].toInt().toChar()
                if (name.isBlank()) throw LauncherException.InvalidMetadata("The runtime archive has an unnamed entry.")
                files++
                extractedBytes += size
                if (files > MAX_FILES || extractedBytes > MAX_EXTRACTED_BYTES) {
                    throw LauncherException.InvalidMetadata("The runtime archive exceeds the extraction limit.")
                }
                val target = root.resolve(name).normalize()
                if (!target.startsWith(root)) {
                    throw LauncherException.InvalidMetadata("The runtime archive contains an unsafe path.")
                }
                when (type) {
                    '5' -> Files.createDirectories(target)
                    '2' -> createSymlink(root, target, header.string(157, 100))
                    'x', 'g', 'L', 'K' -> skipFully(input, size)
                    else -> {
                        Files.createDirectories(requireNotNull(target.parent))
                        Files.newOutputStream(target).use { output -> copyExactly(input, output, size) }
                        if (header.octal(100, 8) and 0b001_001_001L != 0L) {
                            target.toFile().setExecutable(true, false)
                        }
                    }
                }
                if (type == '5' || type == '2') skipFully(input, size)
                skipFully(input, padding(size))
            }
        }
    }

    private fun createSymlink(root: java.nio.file.Path, target: java.nio.file.Path, rawLink: String) {
        val link = java.nio.file.Path.of(rawLink)
        val resolved = requireNotNull(target.parent).resolve(link).normalize()
        if (!resolved.startsWith(root)) {
            throw LauncherException.InvalidMetadata("The runtime archive contains an unsafe symbolic link.")
        }
        Files.createDirectories(requireNotNull(target.parent))
        Files.deleteIfExists(target)
        Files.createSymbolicLink(target, link)
    }

    private fun entryName(header: ByteArray): String {
        val name = header.string(0, 100)
        val prefix = header.string(345, 155)
        return if (prefix.isBlank()) name else "$prefix/$name"
    }

    private fun ByteArray.string(offset: Int, length: Int): String =
        copyOfRange(offset, offset + length)
            .takeWhile { it != 0.toByte() }
            .toByteArray()
            .toString(Charsets.UTF_8)
            .trim()

    private fun ByteArray.octal(offset: Int, length: Int): Long =
        string(offset, length).trim().ifBlank { "0" }.toLong(8)

    private fun readBlock(input: BufferedInputStream, block: ByteArray): Boolean {
        var offset = 0
        while (offset < block.size) {
            val count = input.read(block, offset, block.size - offset)
            if (count < 0) {
                if (offset == 0) return false
                throw LauncherException.InvalidMetadata("The runtime archive ended unexpectedly.")
            }
            offset += count
        }
        return true
    }

    private fun copyExactly(input: BufferedInputStream, output: java.io.OutputStream, count: Long) {
        var remaining = count
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw LauncherException.InvalidMetadata("The runtime archive ended unexpectedly.")
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun skipFully(input: BufferedInputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else if (input.read() >= 0) {
                remaining--
            } else {
                throw LauncherException.InvalidMetadata("The runtime archive ended unexpectedly.")
            }
        }
    }

    private fun padding(size: Long): Long = (BLOCK_SIZE - size % BLOCK_SIZE) % BLOCK_SIZE
}
