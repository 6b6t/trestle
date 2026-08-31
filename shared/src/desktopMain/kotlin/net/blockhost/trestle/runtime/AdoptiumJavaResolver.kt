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
import net.blockhost.trestle.resources.checkedContentPath
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
        require(major in 8..99) { "Unsupported Java major version." }
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
        if (!response.status.isSuccess()) throw LauncherException.RuntimeUnavailable("Temurin Java $major for Linux ARM64 could not be found. Configure a compatible Java executable in instance settings.")
        val release = json.decodeFromString<List<Release>>(response.bodyAsText()).firstOrNull {
            it.version.major == major && it.binary.architecture == "aarch64" && it.binary.os == "linux" && it.binary.imageType == "jre"
        } ?: throw LauncherException.RuntimeUnavailable("No Temurin Java $major runtime is available for Linux ARM64.")
        val artifact = release.binary.artifact
        require(artifact.checksum.matches(Regex("[0-9a-f]{64}")) && artifact.size in 1..1024L * 1024 * 1024) { "Invalid Temurin package metadata." }
        require(artifact.link.startsWith("https://github.com/adoptium/") && artifact.link.endsWith(".tar.gz")) { "Untrusted Temurin package URL." }
        val staging = directories.staging / "temurin" / artifact.checksum
        val archive = staging / "runtime.tar.gz"
        downloads.download(listOf(DownloadRequest(artifact.link, archive, sha256 = artifact.checksum, size = artifact.size,
            progressLabel = "Downloading Temurin Java $major")), staging / "downloads")
        val extracted = staging / "extracted"
        if (fileSystem.exists(extracted)) fileSystem.deleteRecursively(extracted)
        fileSystem.createDirectories(extracted)
        extractTemurinArchive(archive, extracted)
        val java = extracted / "bin/java"
        require(Files.isRegularFile(java.toNioPath()) && Files.isExecutable(java.toNioPath())) { "The Temurin runtime has no executable Java binary." }
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
    @Serializable private data class Binary(val architecture: String, val os: String,
        @SerialName("image_type") val imageType: String, @SerialName("package") val artifact: Artifact)
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
                require(++entries <= 20000) { "The runtime archive contains too many entries." }
                val name = entry.name.removeSuffix("/")
                require(!name.startsWith('/') && '\\' !in name && name.split('/').none { it == "." || it == ".." }) { "Unsafe runtime archive path." }
                val prefix = name.substringBefore('/')
                if (rootName == null) rootName = prefix
                require(rootName == prefix) { "The runtime archive has multiple roots." }
                if ('/' !in name && entry.isDirectory) continue
                require('/' in name) { "The runtime archive is not rooted in a directory." }
                val target = checkedContentPath(fs, destination, name.substringAfter('/'))
                require(entry.size >= 0 && expanded + entry.size <= 1024L * 1024 * 1024) { "The runtime archive is too large." }
                expanded += entry.size
                when {
                    entry.isDirectory -> fs.createDirectories(target)
                    entry.isSymbolicLink -> links += target to entry.linkName
                    entry.isFile -> {
                        fs.createDirectories(requireNotNull(target.parent))
                        Files.newOutputStream(target.toNioPath()).use { tar.copyTo(it) }
                        if (entry.mode and 0b001001001 != 0) require(target.toNioPath().toFile().setExecutable(true, false)) { "Cannot set Java runtime permissions." }
                    }
                    else -> error("Unsupported entry in the runtime archive.")
                }
            }
        }
    }
    links.forEach { (link, target) ->
        val relative = java.nio.file.Path.of(target)
        val resolved = link.toNioPath().parent.resolve(relative).normalize()
        require(!relative.isAbsolute && resolved.startsWith(destination.toNioPath().normalize())) { "Runtime symlink escapes the archive." }
        fs.createDirectories(requireNotNull(link.parent))
        Files.createSymbolicLink(link.toNioPath(), relative)
    }
}
