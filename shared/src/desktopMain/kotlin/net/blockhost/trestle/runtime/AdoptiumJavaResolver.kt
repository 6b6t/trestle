package net.blockhost.trestle.runtime

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadRequest
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.checkedContentPath
import net.blockhost.trestle.resources.msg_cannot_set_java_runtime_permissions
import net.blockhost.trestle.resources.msg_downloading_temurin_java_s
import net.blockhost.trestle.resources.msg_invalid_temurin_package_metadata
import net.blockhost.trestle.resources.msg_no_temurin_java_s_runtime_is_available
import net.blockhost.trestle.resources.msg_runtime_symlink_escapes_the_archive
import net.blockhost.trestle.resources.msg_temurin_java_s_for_linux_arm64_could
import net.blockhost.trestle.resources.msg_the_runtime_archive_contains_too_many_entries
import net.blockhost.trestle.resources.msg_the_runtime_archive_has_multiple_roots
import net.blockhost.trestle.resources.msg_the_runtime_archive_is_not_rooted_in
import net.blockhost.trestle.resources.msg_the_runtime_archive_is_too_large
import net.blockhost.trestle.resources.msg_the_temurin_runtime_has_no_executable_java
import net.blockhost.trestle.resources.msg_unsupported_entry_in_the_runtime_archive
import net.blockhost.trestle.resources.msg_unsupported_java_major_version
import net.blockhost.trestle.resources.msg_untrusted_temurin_package_url
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text
import net.blockhost.trestle.text.userError
import okio.FileSystem
import okio.Path
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.file.Files
import java.util.zip.GZIPInputStream

/** Mojang does not publish Linux ARM64 runtimes. Use verified Eclipse Temurin packages there. */
internal class AdoptiumJavaResolver(
    private val directories: LauncherDirectories,
    private val client: HttpClient,
    private val downloads: DownloadPipeline,
    private val endpoint: String = "https://api.adoptium.net/v3/assets/latest",
) {
    private val fileSystem = FileSystem.SYSTEM
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolve(major: Int): String {
        requireUser(major in 8..99) { text(Res.string.msg_unsupported_java_major_version) }
        val cache = directories.runtimes / "temurin" / major.toString() / "linux-arm64"
        if (fileSystem.exists(cache)) {
            fileSystem.list(cache).filter { it.name.matches(Regex("[0-9a-f]{64}")) }
                .sortedByDescending { fileSystem.metadata(it).lastModifiedAtMillis }
                .firstOrNull { fileSystem.exists(it / ".complete") && Files.isExecutable((it / "bin/java").toNioPath()) }
                ?.let { return (it / "bin/java").toString() }
        }
        val response = client.get("$endpoint/$major/hotspot") {
            header("User-Agent", BuildInfo.USER_AGENT)
            parameter("architecture", "aarch64")
            parameter("image_type", "jre")
            parameter("os", "linux")
            parameter("vendor", "eclipse")
        }
        if (!response.status.isSuccess()) {
            throw LauncherException.RuntimeUnavailable(
                text(Res.string.msg_temurin_java_s_for_linux_arm64_could, major),
            )
        }
        val release = json.decodeFromString<List<Release>>(response.bodyAsText()).firstOrNull {
            it.version.major == major && it.binary.architecture == "aarch64" && it.binary.os == "linux" && it.binary.imageType == "jre"
        } ?: throw LauncherException.RuntimeUnavailable(text(Res.string.msg_no_temurin_java_s_runtime_is_available, major))
        val artifact = release.binary.artifact
        requireUser(artifact.checksum.matches(Regex("[0-9a-f]{64}")) && artifact.size in 1..1024L * 1024 * 1024) {
            text(Res.string.msg_invalid_temurin_package_metadata)
        }
        requireUser(artifact.link.startsWith("https://github.com/adoptium/") && artifact.link.endsWith(".tar.gz")) {
            text(Res.string.msg_untrusted_temurin_package_url)
        }
        val staging = directories.staging / "temurin" / artifact.checksum
        val archive = staging / "runtime.tar.gz"
        downloads.download(
            listOf(
                DownloadRequest(
                    artifact.link,
                    archive,
                    sha256 = artifact.checksum,
                    size = artifact.size,
                    progressLabel = text(Res.string.msg_downloading_temurin_java_s, major),
                ),
            ),
            staging / "downloads",
        )
        val extracted = staging / "extracted"
        if (fileSystem.exists(extracted)) fileSystem.deleteRecursively(extracted)
        fileSystem.createDirectories(extracted)
        extractTemurinArchive(archive, extracted)
        val java = extracted / "bin/java"
        requireUser(Files.isRegularFile(java.toNioPath()) && Files.isExecutable(java.toNioPath())) {
            text(Res.string.msg_the_temurin_runtime_has_no_executable_java)
        }
        fileSystem.write(extracted / ".complete") { writeUtf8(artifact.checksum) }
        val target = cache / artifact.checksum
        fileSystem.createDirectories(cache)
        if (fileSystem.exists(target)) fileSystem.deleteRecursively(target)
        fileSystem.atomicMove(extracted, target)
        fileSystem.deleteRecursively(staging)
        return (target / "bin/java").toString()
    }

    @Serializable private data class Release(val binary: Binary, val version: Version)

    @Serializable private data class Version(val major: Int)

    @Serializable private data class Binary(
        val architecture: String,
        val os: String,
        @SerialName("image_type") val imageType: String,
        @SerialName("package") val artifact: Artifact,
    )

    @Serializable private data class Artifact(val link: String, val checksum: String, val size: Long)
}

internal fun extractTemurinArchive(archive: Path, destination: Path) {
    val fs = FileSystem.SYSTEM
    val links = mutableListOf<Pair<Path, String>>()
    var rootName: String? = null
    GZIPInputStream(Files.newInputStream(archive.toNioPath())).use { gzip ->
        TarArchiveInputStream(gzip).use { tar ->
            var entries = 0
            var expanded = 0L
            while (true) {
                val entry = tar.nextEntry ?: break
                requireUser(++entries <= 20000) { text(Res.string.msg_the_runtime_archive_contains_too_many_entries) }
                val name = entry.name.removeSuffix("/")
                require(
                    !name.startsWith('/') && '\\' !in name && name.split('/').none {
                        it == "." || it == ".."
                    },
                ) { "Unsafe runtime archive path." }
                val prefix = name.substringBefore('/')
                if (rootName == null) rootName = prefix
                requireUser(rootName == prefix) { text(Res.string.msg_the_runtime_archive_has_multiple_roots) }
                if ('/' !in name && entry.isDirectory) continue
                requireUser('/' in name) { text(Res.string.msg_the_runtime_archive_is_not_rooted_in) }
                val target = checkedContentPath(fs, destination, name.substringAfter('/'))
                requireUser(entry.size >= 0 && expanded + entry.size <= 1024L * 1024 * 1024) {
                    text(Res.string.msg_the_runtime_archive_is_too_large)
                }
                expanded += entry.size
                when {
                    entry.isDirectory -> fs.createDirectories(target)

                    entry.isSymbolicLink -> links += target to entry.linkName

                    entry.isFile -> {
                        fs.createDirectories(requireNotNull(target.parent))
                        Files.newOutputStream(target.toNioPath()).use { tar.copyTo(it) }
                        if (entry.mode and 0b001001001 !=
                            0
                        ) {
                            requireUser(target.toNioPath().toFile().setExecutable(true, false)) {
                                text(Res.string.msg_cannot_set_java_runtime_permissions)
                            }
                        }
                    }

                    else -> userError(text(Res.string.msg_unsupported_entry_in_the_runtime_archive))
                }
            }
        }
    }
    links.forEach { (link, target) ->
        val relative = java.nio.file.Path.of(target)
        val resolved = link.toNioPath().parent.resolve(relative).normalize()
        requireUser(!relative.isAbsolute && resolved.startsWith(destination.toNioPath().normalize())) {
            text(Res.string.msg_runtime_symlink_escapes_the_archive)
        }
        fs.createDirectories(requireNotNull(link.parent))
        Files.createSymbolicLink(link.toNioPath(), relative)
    }
}
