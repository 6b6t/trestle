package net.blockhost.trestle.download

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import net.blockhost.trestle.platform.useOkio
import okio.FileSystem
import okio.Path
import okio.buffer

data class DownloadRequest(
    val url: String,
    val destination: Path,
    val sha1: String? = null,
    val size: Long? = null,
    val progressLabel: String? = null,
    val sha256: String? = null,
    val sha512: String? = null,
    val md5: String? = null,
    val localSource: Path? = null,
)

data class DownloadProgress(
    val completedBytes: Long,
    val totalBytes: Long?,
    val completedFiles: Int,
    val totalFiles: Int,
    val activeLabel: String? = null,
    val isFinalizing: Boolean = false,
)

class DownloadPipeline(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    maxConcurrency: Int = 6,
    maxAttempts: Int = 3,
    private val logger: LauncherLogger = NoopLauncherLogger,
) {
    @Volatile
    private var maxConcurrency: Int = maxConcurrency

    @Volatile
    private var maxAttempts: Int = maxAttempts

    init {
        require(maxConcurrency > 0)
        require(maxAttempts > 0)
    }

    fun configure(maxConcurrency: Int, maxAttempts: Int) {
        require(maxConcurrency > 0)
        require(maxAttempts > 0)
        this.maxConcurrency = maxConcurrency
        this.maxAttempts = maxAttempts
    }

    suspend fun download(
        requests: List<DownloadRequest>,
        stagingDirectory: Path,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ) {
        fileSystem.createDirectories(stagingDirectory)
        val required = requests.distinctBy { it.destination }
        val totalBytes = required.mapNotNull { it.size }.takeIf { it.size == required.size }?.sum()
        val lock = Mutex()
        val rangeSupport = HostRangeSupport()
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
                                            request.progressLabel ?: request.destination.name,
                                        ),
                                    )
                                } finally {
                                    lock.unlock()
                                }
                                return@withPermit
                            }

                            val staged = stagingDirectory / "$index.part"
                            downloadWithRetry(request, staged, rangeSupport) { delta ->
                                lock.lock()
                                try {
                                    completedBytes += delta
                                    onProgress(
                                        DownloadProgress(
                                            completedBytes,
                                            totalBytes,
                                            completedFiles,
                                            required.size,
                                            request.progressLabel ?: request.destination.name,
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
                                        request.progressLabel ?: request.destination.name,
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

    private suspend fun validateMd5(path: Path, expectedMd5: String?, artifactName: String = path.name) {
        if (expectedMd5 == null) return
        val actual = try {
            fileSystem.read(path) { readByteString().md5().hex() }
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The checksum for $artifactName could not be read.", error)
        }
        if (!actual.equals(expectedMd5, ignoreCase = true)) {
            throw LauncherException.ChecksumMismatch(artifactName, expectedMd5, actual)
        }
    }

    private suspend fun downloadWithRetry(
        request: DownloadRequest,
        stagedPath: Path,
        rangeSupport: HostRangeSupport,
        onBytes: suspend (Long) -> Unit,
    ) {
        request.localSource?.let { source ->
            require(request.sha1 != null || request.sha256 != null || request.sha512 != null) { "Local download substitutions need a checksum." }
            fileSystem.copy(source, stagedPath)
            validateCompleted(stagedPath, request)
            onBytes(fileSize(stagedPath))
            return
        }
        var lastError: Throwable? = null
        val host = Url(request.url).host
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
                if (existingBytes > 0L && !rangeSupport.canAttempt(host)) {
                    resetStaged(stagedPath, accountedBytes, onBytes)
                    accountedBytes = 0L
                    existingBytes = 0L
                }
                val requestedRange = existingBytes > 0L
                val response = httpClient.get(request.url) {
                    if (requestedRange) header(HttpHeaders.Range, "bytes=$existingBytes-")
                }
                if (!response.status.isSuccess()) {
                    val status = response.status.value
                    if (status == HttpStatusCode.RequestedRangeNotSatisfiable.value && requestedRange) {
                        rangeSupport.disable(host)
                        resetStaged(stagedPath, accountedBytes, onBytes)
                        accountedBytes = 0L
                        throw InvalidRangeResponseException("The server rejected the saved byte range.")
                    }
                    if (status !in TRANSIENT_HTTP_CODES) throw PermanentDownloadException("HTTP $status")
                    throw TransientDownloadException("HTTP $status")
                }
                val appending = requestedRange && response.status == HttpStatusCode.PartialContent
                if (appending) {
                    val contentRange = response.headers[HttpHeaders.ContentRange]
                    if (contentRange == null || !contentRange.startsWith("bytes $existingBytes-")) {
                        rangeSupport.disable(host)
                        resetStaged(stagedPath, accountedBytes, onBytes)
                        accountedBytes = 0L
                        throw InvalidRangeResponseException("The server returned an invalid byte range.")
                    }
                } else if (requestedRange) {
                    rangeSupport.disable(host)
                    resetStaged(stagedPath, accountedBytes, onBytes)
                    accountedBytes = 0L
                    existingBytes = 0L
                }
                fileSystem.createDirectories(requireNotNull(stagedPath.parent))
                val rawSink = if (appending) fileSystem.appendingSink(stagedPath) else fileSystem.sink(stagedPath)
                rawSink.buffer().useOkio { sink ->
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
            if (attempt < maxAttempts - 1) {
                if (lastError is InvalidRangeResponseException) {
                    logger.debug(
                        "downloads",
                        "Restarting artifact without byte-range resume",
                        mapOf("file" to request.destination.name, "host" to host),
                    )
                } else {
                    delay(250L shl attempt)
                    logger.warn(
                        "downloads",
                        "Retrying artifact download",
                        lastError,
                        mapOf("file" to request.destination.name, "attempt" to attempt + 2),
                    )
                }
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
        if (
            request.md5 == null &&
            request.sha1 == null &&
            request.sha256 == null &&
            request.sha512 == null &&
            request.size == null
        ) return allowUnverified
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
        validateMd5(path, request.md5, request.destination.name)
        validate(path, request.sha1, request.destination.name)
        validateSha256(path, request.sha256, request.destination.name)
        validateSha512(path, request.sha512, request.destination.name)
    }

    private fun validateSha256(path: Path, expectedSha256: String?, artifactName: String) {
        if (expectedSha256 == null) return
        val actual = try {
            fileSystem.read(path) { readByteString().sha256().hex() }
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The checksum for $artifactName could not be read.", error)
        }
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            throw LauncherException.ChecksumMismatch(artifactName, expectedSha256, actual)
        }
    }

    private fun validateSha512(path: Path, expectedSha512: String?, artifactName: String) {
        if (expectedSha512 == null) return
        val actual = try {
            fileSystem.read(path) { readByteString().sha512().hex() }
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The checksum for $artifactName could not be read.", error)
        }
        if (!actual.equals(expectedSha512, ignoreCase = true)) {
            throw LauncherException.ChecksumMismatch(artifactName, expectedSha512, actual)
        }
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

    private class HostRangeSupport {
        private val mutex = Mutex()
        private val unsupportedHosts = mutableSetOf<String>()

        suspend fun canAttempt(host: String): Boolean = mutex.withLock { host !in unsupportedHosts }

        suspend fun disable(host: String) {
            mutex.withLock { unsupportedHosts += host }
        }
    }

    private companion object {
        val TRANSIENT_HTTP_CODES = setOf(408, 425, 429, 500, 502, 503, 504)
        const val DEFAULT_BUFFER_SIZE = 32 * 1024
    }
}
