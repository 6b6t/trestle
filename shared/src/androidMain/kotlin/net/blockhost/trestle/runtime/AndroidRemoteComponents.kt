package net.blockhost.trestle.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadProgress
import okio.Path
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

internal data class RemoteDeflatedComponent(
    val name: String,
    val rangeStart: Long,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val sha256: String,
)

internal class AndroidRemoteComponentInstaller {
    suspend fun install(
        sourceUrl: String,
        components: List<RemoteDeflatedComponent>,
        destination: Path,
        onProgress: suspend (DownloadProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val totalBytes = components.sumOf(RemoteDeflatedComponent::compressedSize)
        var completedBytes = 0L
        var completedFiles = 0
        components.forEach { component ->
            val target = File(destination.toString(), component.name)
            if (target.isFile && target.length() == component.uncompressedSize && sha256(target) == component.sha256) {
                completedBytes += component.compressedSize
                completedFiles++
                onProgress(progress(completedBytes, totalBytes, completedFiles, components.size, component.name))
                return@forEach
            }

            target.parentFile?.mkdirs()
            val staged = File(target.parentFile, ".${component.name}.part")
            staged.delete()
            val rangeEnd = component.rangeStart + component.compressedSize - 1
            val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                setRequestProperty("Range", "bytes=${component.rangeStart}-$rangeEnd")
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw LauncherException.Network(
                        "The Android bridge source did not return the requested byte range " +
                            "(HTTP ${connection.responseCode}).",
                    )
                }
                val contentRange = connection.getHeaderField("Content-Range").orEmpty()
                if (!contentRange.startsWith("bytes ${component.rangeStart}-$rangeEnd/")) {
                    throw LauncherException.Network("The Android bridge source returned an invalid byte range.")
                }
                val digest = MessageDigest.getInstance("SHA-256")
                var extractedBytes = 0L
                var downloadedBytes = 0L
                val countingInput = object : java.io.FilterInputStream(BufferedInputStream(connection.inputStream)) {
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        val count = super.read(buffer, offset, length)
                        if (count > 0) downloadedBytes += count
                        return count
                    }

                    override fun read(): Int {
                        val value = super.read()
                        if (value >= 0) downloadedBytes++
                        return value
                    }
                }
                InflaterInputStream(countingInput, Inflater(true)).use { input ->
                    FileOutputStream(staged).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            extractedBytes += count
                            if (extractedBytes > component.uncompressedSize) {
                                throw LauncherException.InvalidMetadata(
                                    "The ${component.name} bridge component is larger than expected.",
                                )
                            }
                            onProgress(
                                progress(
                                    completedBytes + downloadedBytes.coerceAtMost(component.compressedSize),
                                    totalBytes,
                                    completedFiles,
                                    components.size,
                                    component.name,
                                ),
                            )
                        }
                    }
                }
                val actualHash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                if (
                    downloadedBytes != component.compressedSize ||
                    extractedBytes != component.uncompressedSize ||
                    actualHash != component.sha256
                ) {
                    throw LauncherException.ChecksumMismatch(component.name, component.sha256, actualHash)
                }
                Files.move(
                    staged.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
                completedBytes += component.compressedSize
                completedFiles++
                onProgress(progress(completedBytes, totalBytes, completedFiles, components.size, component.name))
            } finally {
                connection.disconnect()
                staged.delete()
            }
        }
    }

    private fun progress(
        completedBytes: Long,
        totalBytes: Long,
        completedFiles: Int,
        totalFiles: Int,
        name: String,
    ) = DownloadProgress(
        completedBytes = completedBytes,
        totalBytes = totalBytes,
        completedFiles = completedFiles,
        totalFiles = totalFiles,
        activeLabel = "Downloading Android bridge: $name",
    )

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 120_000
    }
}
