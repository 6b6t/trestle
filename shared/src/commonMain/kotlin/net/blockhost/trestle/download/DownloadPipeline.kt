package net.blockhost.trestle.download

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
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

        try {
            coroutineScope {
                required.mapIndexed { index, request ->
                    async {
                        semaphore.withPermit {
                            if (isValid(request.destination, request.sha1)) {
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
                            validate(staged, request.sha1, request.destination.name)
                            StagedDownload(request, staged)
                        }
                    }
                }.awaitAll().filterIsInstance<StagedDownload>().also { stagedDownloads ->
                    for (download in stagedDownloads) {
                        fileSystem.createDirectories(requireNotNull(download.request.destination.parent))
                        fileSystem.atomicMove(download.stagedPath, download.request.destination)
                        completedFiles++
                        onProgress(
                            DownloadProgress(
                                completedBytes,
                                totalBytes,
                                completedFiles,
                                required.size,
                                download.request.destination.name,
                            ),
                        )
                    }
                }
            }
            deleteTree(stagingDirectory)
        } catch (error: CancellationException) {
            deleteTree(stagingDirectory)
            throw error
        } catch (error: LauncherException) {
            deleteTree(stagingDirectory)
            throw error
        } catch (error: Exception) {
            deleteTree(stagingDirectory)
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
        repeat(maxAttempts) { attempt ->
            try {
                if (fileSystem.exists(stagedPath)) fileSystem.delete(stagedPath)
                val response = httpClient.get(request.url)
                if (!response.status.isSuccess()) {
                    val status = response.status.value
                    if (status !in TRANSIENT_HTTP_CODES) throw PermanentDownloadException("HTTP $status")
                    throw TransientDownloadException("HTTP $status")
                }
                fileSystem.createDirectories(requireNotNull(stagedPath.parent))
                fileSystem.sink(stagedPath).buffer().use { sink ->
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = channel.readAvailable(buffer)
                        if (count == -1) break
                        if (count == 0) continue
                        sink.write(buffer, 0, count)
                        onBytes(count.toLong())
                    }
                    sink.flush()
                }
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: PermanentDownloadException) {
                throw LauncherException.Network("Download failed with ${error.message}.", error)
            } catch (error: LauncherException.ChecksumMismatch) {
                throw error
            } catch (error: LauncherException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
            if (attempt < maxAttempts - 1) delay(250L shl attempt)
        }
        throw LauncherException.Network("Download failed after $maxAttempts attempts.", lastError)
    }

    private suspend fun isValid(path: Path, sha1: String?): Boolean {
        if (!fileSystem.exists(path)) return false
        if (sha1 == null) return true
        return try {
            validate(path, sha1)
            true
        } catch (_: LauncherException.ChecksumMismatch) {
            false
        }
    }

    private fun deleteTree(path: Path) {
        if (!fileSystem.exists(path)) return
        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

    private data class StagedDownload(val request: DownloadRequest, val stagedPath: Path)
    private class TransientDownloadException(message: String) : Exception(message)
    private class PermanentDownloadException(message: String) : Exception(message)

    private companion object {
        val TRANSIENT_HTTP_CODES = setOf(408, 425, 429, 500, 502, 503, 504)
        const val DEFAULT_BUFFER_SIZE = 32 * 1024
    }
}
