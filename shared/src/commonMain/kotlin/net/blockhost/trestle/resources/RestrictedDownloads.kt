package net.blockhost.trestle.resources

import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path

data class RestrictedDownload(val fileName: String, val sha1: String, val websiteUrl: String)

/** Every file the install still needs from its publisher, reported together so they can be fetched in one pass. */
class RestrictedDownloadRequired(val downloads: List<RestrictedDownload>) :
    Exception(
        if (downloads.size == 1) {
            "${downloads.single().fileName} requires a download from its publisher. Select the downloaded file to continue."
        } else {
            "${downloads.size} files require downloads from their publishers. Select the downloaded files to continue."
        },
    )

/** Only exact publisher-provided SHA-1 matches may substitute for a restricted download. */
class RestrictedDownloads(private val fileSystem: FileSystem, private val directory: Path) {
    fun find(sha1: String?): Path? {
        if (sha1 == null || !sha1.matches(Regex("[a-fA-F0-9]{40}"))) return null
        val path = checkedContentPath(fileSystem, directory, sha1.lowercase())
        return path.takeIf { fileSystem.exists(it) && fileSystem.sha1(it).equals(sha1, ignoreCase = true) }
    }

    /** Returns the verified local copy of [file], or records it in [missing] when the player still has to download it. */
    fun locate(file: ResourceFile, websiteUrl: String, missing: MutableList<RestrictedDownload>): Path? {
        find(file.sha1)?.let { return it }
        val sha1 = file.sha1?.takeIf { it.matches(Regex("[a-fA-F0-9]{40}")) }
            ?: error("${file.fileName} requires a manual download, but the publisher supplied no checksum. It cannot be imported safely.")
        missing += RestrictedDownload(file.fileName, sha1, websiteUrl)
        return null
    }

    /** Finds which of [downloads] the selected [bytes] are, by checksum, so files can be chosen in any order. */
    fun match(downloads: List<RestrictedDownload>, bytes: ByteArray): RestrictedDownload? {
        val sha1 = bytes.toByteString().sha1().hex()
        return downloads.firstOrNull { it.sha1.equals(sha1, ignoreCase = true) }
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
