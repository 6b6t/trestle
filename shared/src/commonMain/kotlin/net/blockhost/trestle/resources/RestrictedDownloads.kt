package net.blockhost.trestle.resources

import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path

data class RestrictedDownload(val fileName: String, val sha1: String, val websiteUrl: String)
class RestrictedDownloadRequired(val download: RestrictedDownload) : Exception(
    "${download.fileName} requires a download from its publisher. Select the downloaded file to continue.",
)

/** Only exact publisher-provided SHA-1 matches may substitute for a restricted download. */
class RestrictedDownloads(private val fileSystem: FileSystem, private val directory: Path) {
    fun find(sha1: String?): Path? {
        if (sha1 == null || !sha1.matches(Regex("[a-fA-F0-9]{40}"))) return null
        val path = checkedContentPath(fileSystem, directory, sha1.lowercase())
        return path.takeIf { fileSystem.exists(it) && fileSystem.sha1(it).equals(sha1, ignoreCase = true) }
    }

    fun requireFile(file: ResourceFile, websiteUrl: String): Path {
        find(file.sha1)?.let { return it }
        val sha1 = file.sha1?.takeIf { it.matches(Regex("[a-fA-F0-9]{40}")) }
            ?: error("${file.fileName} requires a manual download, but the publisher supplied no checksum. It cannot be imported safely.")
        throw RestrictedDownloadRequired(RestrictedDownload(file.fileName, sha1, websiteUrl))
    }

    fun accept(download: RestrictedDownload, bytes: ByteArray) {
        require(bytes.size in 1..512 * 1024 * 1024) { "Choose a file smaller than 512 MiB." }
        require(bytes.toByteString().sha1().hex().equals(download.sha1, ignoreCase = true)) {
            "This file does not match the version requested by the pack. Download ${download.fileName} from the publisher."
        }
        fileSystem.createDirectories(directory)
        val path = checkedContentPath(fileSystem, directory, download.sha1.lowercase())
        val temporary = directory / "${path.name}.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.atomicMove(temporary, path)
    }
}
