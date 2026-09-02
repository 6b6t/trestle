package net.blockhost.trestle.resources

import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.platform.useOkio
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip

internal class IosArchiveExtractor(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : ArchiveExtractor {
    override fun extract(archive: Path, destination: Path) {
        try {
            fileSystem.openZip(archive).useOkio { zip ->
                fileSystem.createDirectories(destination)
                var entries = 0
                var extractedBytes = 0L
                zip.listRecursively("/".toPath()).forEach { entry ->
                    entries++
                    if (entries > MAX_ENTRIES) {
                        throw LauncherException.InvalidMetadata("The modpack archive contains too many files.")
                    }
                    val relativeSegments = entry.segments
                    if (relativeSegments.any { it.isBlank() || it == "." || it == ".." }) {
                        throw LauncherException.InvalidMetadata("The modpack contains an unsafe archive path.")
                    }
                    val target = relativeSegments.fold(destination) { current, segment -> current / segment }
                    val metadata = zip.metadata(entry)
                    if (metadata.isDirectory) {
                        fileSystem.createDirectories(target)
                    } else if (metadata.isRegularFile) {
                        extractedBytes += metadata.size ?: 0L
                        if (extractedBytes > MAX_EXTRACTED_BYTES) {
                            throw LauncherException.InvalidMetadata("The modpack archive is too large when extracted.")
                        }
                        fileSystem.createDirectories(requireNotNull(target.parent))
                        zip.source(entry).buffer().useOkio { source ->
                            fileSystem.sink(target).buffer().useOkio { sink -> sink.writeAll(source) }
                        }
                    }
                }
            }
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The modpack archive could not be extracted.", error)
        }
    }

    private companion object {
        const val MAX_ENTRIES = 100_000
        const val MAX_EXTRACTED_BYTES = 20L * 1024L * 1024L * 1024L
    }
}
