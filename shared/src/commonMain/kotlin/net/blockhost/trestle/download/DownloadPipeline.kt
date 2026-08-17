package net.blockhost.trestle.download

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import okio.FileSystem
import okio.Path
import okio.buffer

data class DownloadRequest(
    val url: String,
    val destination: Path,
    val sha1: String? = null,
    val size: Long? = null,
)

data class DownloadProgress(
    val completedBytes: Long,
    val totalBytes: Long?,
    val completedFiles: Int,
    val totalFiles: Int,
    val activeFile: String? = null,
)

class DownloadPipeline(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    private val maxConcurrency: Int = 6,
    private val maxAttempts: Int = 3,
    private val logger: LauncherLogger = NoopLauncherLogger,
) {
    init {
        require(maxConcurrency > 0)
        require(maxAttempts > 0)
    }

    suspend fun download(
        requests: List<DownloadRequest>,
        stagingDirectory: Path,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ) {
        fileSystem.createDirectories(stagingDirectory)
        val required = requests.distinctBy { it.destination }
        val totalBytes = required.mapNotNull { it.size }.takeIf { it.size == required.size }?.sum()
        val lock = kotlinx.coroutines.sync.Mutex()
        var completedBytes = 0L
        var completedFiles = 0
        val semaphore = Semaphore(maxConcurrency)
        logger.info(
            "downloads",
            "Starting artifact download",
            mapOf("files" to required.size, "concurrency" to maxConcurrency),
        )

        try {
            coroutineScope {
                required.mapIndexed { index, request ->
                    async {
                        semaphore.withPermit {
                            if (isValid(request.destination, request, allowUnverified = true)) {
                                lock.lock()
                                try {
                                    completedBytes += request.size ?: fileSystem.metadata(request.destination).size ?: 0L
                                    completedFiles++
                                    onProgress(
                                        DownloadProgress(
                                            completedBytes,
                                            totalBytes,
                                            completedFiles,
                                            required.size,
                                            request.destination.name,
                                        ),
                                    )
                                } finally {
                                    lock.unlock()
                                }
                                return@withPermit
                            }

                            val staged = stagingDirectory / "$index.part"
                            downloadWithRetry(request, staged) { delta ->
                                lock.lock()
                                try {
                                    completedBytes += delta
                                    onProgress(
                                        DownloadProgress(
                                            completedBytes,
                                            totalBytes,
                                            completedFiles,
                                            required.size,
                                            request.destination.name,
                                        ),
                                    )
                                } finally {
                                    lock.unlock()
                                }
                            }
                            fileSystem.createDirectories(requireNotNull(request.destination.parent))
                            fileSystem.atomicMove(staged, request.destination)
                            lock.lock()
                            try {
                                completedFiles++
                                onProgress(
                                    DownloadProgress(
                                        completedBytes,
                                        totalBytes,
                                        completedFiles,
                                        required.size,
                                        request.destination.name,
                                    ),
                                )
                            } finally {
                                lock.unlock()
                            }
                        }
                    }
                }.awaitAll()
            }
            deleteTree(stagingDirectory)
            logger.info("downloads", "Artifact download completed", mapOf("files" to required.size))
        } catch (error: CancellationException) {
            logger.info("downloads", "Artifact download paused", mapOf("files" to required.size))
            throw error
        } catch (error: LauncherException) {
            logger.error(
                "downloads",
                "Artifact download failed",
                error,
                mapOf("files" to required.size),
            )
            throw error
        } catch (error: Exception) {
            logger.error(
                "downloads",
                "Artifact download could not be activated",
                error,
                mapOf("files" to required.size),
            )
            throw LauncherException.FileSystem("The download could not be activated.", error)
        }
    }

    suspend fun validate(path: Path, expectedSha1: String?, artifactName: String = path.name) {
        if (expectedSha1 == null) return
        val actual = try {
            fileSystem.read(path) { readByteString().sha1().hex() }
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The checksum for $artifactName could not be read.", error)
        }
        if (!actual.equals(expectedSha1, ignoreCase = true)) {
            throw LauncherException.ChecksumMismatch(artifactName, expectedSha1, actual)
        }
    }

    private suspend fun downloadWithRetry(
        request: DownloadRequest,
        stagedPath: Path,
        onBytes: suspend (Long) -> Unit,
    ) {
        var lastError: Throwable? = null
        if (isValid(stagedPath, request, allowUnverified = false)) {
            onBytes(fileSize(stagedPath))
            return
        }
        var accountedBytes = resumableBytes(stagedPath, request)
        if (accountedBytes > 0L) onBytes(accountedBytes)
        repeat(maxAttempts) { attempt ->
            try {
                var existingBytes = fileSize(stagedPath)
                if (request.size != null && existingBytes >= request.size) {
                    resetStaged(stagedPath, accountedBytes, onBytes)
                    accountedBytes = 0L
                    existingBytes = 0L
                }
                val response = httpClient.get(request.url) {
                    if (existingBytes > 0L) header(HttpHeaders.Range, "bytes=$existingBytes-")
                }
                if (!response.status.isSuccess()) {
                    val status = response.status.value
                    if (status == HttpStatusCode.RequestedRangeNotSatisfiable.value && existingBytes > 0L) {
                        resetStaged(stagedPath, accountedBytes, onBytes)
                        accountedBytes = 0L
                        throw InvalidRangeResponseException("The server rejected the saved byte range.")
                    }
                    if (status !in TRANSIENT_HTTP_CODES) throw PermanentDownloadException("HTTP $status")
                    throw TransientDownloadException("HTTP $status")
                }
                val appending = existingBytes > 0L && response.status == HttpStatusCode.PartialContent
                if (appending) {
                    val contentRange = response.headers[HttpHeaders.ContentRange]
                    if (contentRange == null || !contentRange.startsWith("bytes $existingBytes-")) {
                        resetStaged(stagedPath, accountedBytes, onBytes)
                        accountedBytes = 0L
                        throw InvalidRangeResponseException("The server returned an invalid byte range.")
                    }
                } else if (existingBytes > 0L) {
                    resetStaged(stagedPath, accountedBytes, onBytes)
                    accountedBytes = 0L
                    existingBytes = 0L
                }
                fileSystem.createDirectories(requireNotNull(stagedPath.parent))
                val rawSink = if (appending) fileSystem.appendingSink(stagedPath) else fileSystem.sink(stagedPath)
                rawSink.buffer().use { sink ->
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = channel.readAvailable(buffer)
                        if (count == -1) break
                        if (count == 0) continue
                        sink.write(buffer, 0, count)
                        accountedBytes += count.toLong()
                        onBytes(count.toLong())
                    }
                    sink.flush()
                }
                validateCompleted(stagedPath, request)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: PermanentDownloadException) {
                throw LauncherException.Network("Download failed with ${error.message}.", error)
            } catch (error: LauncherException.ChecksumMismatch) {
                lastError = error
                resetStaged(stagedPath, accountedBytes, onBytes)
                accountedBytes = 0L
            } catch (error: IncompleteDownloadException) {
                lastError = error
                resetStaged(stagedPath, accountedBytes, onBytes)
                accountedBytes = 0L
            } catch (error: LauncherException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
            if (attempt < maxAttempts - 1) delay(250L shl attempt)
            if (attempt < maxAttempts - 1) {
                logger.warn(
                    "downloads",
                    "Retrying artifact download",
                    lastError,
                    mapOf("file" to request.destination.name, "attempt" to attempt + 2),
                )
            }
        }
        throw LauncherException.Network("Download failed after $maxAttempts attempts.", lastError)
    }

    private suspend fun isValid(
        path: Path,
        request: DownloadRequest,
        allowUnverified: Boolean,
    ): Boolean {
        if (!fileSystem.exists(path)) return false
        if (request.sha1 == null && request.size == null) return allowUnverified
        return try {
            validateCompleted(path, request)
            true
        } catch (_: LauncherException) {
            false
        } catch (_: IncompleteDownloadException) {
            false
        }
    }

    private suspend fun validateCompleted(path: Path, request: DownloadRequest) {
        request.size?.let { expectedSize ->
            val actualSize = fileSize(path)
            if (actualSize != expectedSize) {
                throw IncompleteDownloadException(expectedSize, actualSize)
            }
        }
        validate(path, request.sha1, request.destination.name)
    }

    private suspend fun resetStaged(
        path: Path,
        accountedBytes: Long,
        onBytes: suspend (Long) -> Unit,
    ) {
        if (accountedBytes > 0L) onBytes(-accountedBytes)
        fileSystem.delete(path, mustExist = false)
    }

    private fun resumableBytes(path: Path, request: DownloadRequest): Long {
        val size = fileSize(path)
        if (size <= 0L) return 0L
        return if (request.size == null || size < request.size) size else 0L
    }

    private fun fileSize(path: Path): Long =
        if (fileSystem.exists(path)) fileSystem.metadata(path).size ?: 0L else 0L

    private fun deleteTree(path: Path) {
        if (!fileSystem.exists(path)) return
        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

    private class TransientDownloadException(message: String) : Exception(message)
    private class PermanentDownloadException(message: String) : Exception(message)
    private class InvalidRangeResponseException(message: String) : Exception(message)
    private class IncompleteDownloadException(expected: Long, actual: Long) :
        Exception("Expected $expected bytes but received $actual.")

    private companion object {
        val TRANSIENT_HTTP_CODES = setOf(408, 425, 429, 500, 502, 503, 504)
        const val DEFAULT_BUFFER_SIZE = 32 * 1024
    }
}
