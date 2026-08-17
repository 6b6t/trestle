package net.blockhost.trestle.resources

import net.blockhost.trestle.domain.LauncherException
import okio.Path
import java.nio.file.Files
import java.util.zip.ZipInputStream

class JvmArchiveExtractor : ArchiveExtractor {
    override fun extract(archive: Path, destination: Path) {
        val destinationPath = java.nio.file.Path.of(destination.toString()).toAbsolutePath().normalize()
        try {
            Files.createDirectories(destinationPath)
            ZipInputStream(Files.newInputStream(java.nio.file.Path.of(archive.toString()))).use { zip ->
                var entries = 0
                var extractedBytes = 0L
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    if (entries > MAX_ENTRIES) {
                        throw LauncherException.InvalidMetadata("The modpack archive contains too many files.")
                    }
                    val target = destinationPath.resolve(entry.name.replace('\\', '/')).normalize()
                    if (!target.startsWith(destinationPath)) {
                        throw LauncherException.InvalidMetadata("The modpack contains an unsafe archive path.")
                    }
                    if (entry.isDirectory) {
                        Files.createDirectories(target)
                    } else {
                        Files.createDirectories(requireNotNull(target.parent))
                        Files.newOutputStream(target).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                extractedBytes += count
                                if (extractedBytes > MAX_EXTRACTED_BYTES) {
                                    throw LauncherException.InvalidMetadata("The modpack archive is too large when extracted.")
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
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
