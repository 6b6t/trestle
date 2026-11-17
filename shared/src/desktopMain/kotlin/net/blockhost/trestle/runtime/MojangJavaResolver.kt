package net.blockhost.trestle.runtime

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadRequest
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.DownloadReference
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_downloading_java_runtime
import net.blockhost.trestle.resources.msg_ios_runtimes_are_provided_by_the_ios
import net.blockhost.trestle.resources.msg_java_runtime_metadata_request_failed_with_http
import net.blockhost.trestle.resources.msg_minecraft_metadata_does_not_name_a_mojang
import net.blockhost.trestle.resources.msg_mojang_does_not_provide_a_desktop_java
import net.blockhost.trestle.resources.msg_mojang_does_not_provide_a_java_runtime
import net.blockhost.trestle.resources.msg_mojang_does_not_provide_s_for_s
import net.blockhost.trestle.resources.msg_mojang_java_runtime_metadata_could_not_be
import net.blockhost.trestle.resources.msg_mojang_runtime_file_s_has_no_raw
import net.blockhost.trestle.resources.msg_mojang_runtime_link_s_escapes_the_runtime
import net.blockhost.trestle.resources.msg_mojang_runtime_link_s_has_no_target
import net.blockhost.trestle.resources.msg_mojang_runtime_metadata_contains_an_unsafe_path
import net.blockhost.trestle.resources.msg_mojang_runtime_metadata_contains_an_unsafe_path_2
import net.blockhost.trestle.resources.msg_mojang_runtime_wrong_java
import net.blockhost.trestle.resources.msg_mojang_s_java_executable_is_not_usable
import net.blockhost.trestle.resources.msg_the_managed_java_executable_permission_could_not
import net.blockhost.trestle.resources.msg_the_managed_java_s_runtime_is_incomplete
import net.blockhost.trestle.text.text
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class MojangJavaResolver(
    private val environment: PlatformEnvironment,
    private val directories: LauncherDirectories,
    private val httpClient: HttpClient,
    private val downloadPipeline: DownloadPipeline,
    private val runtimeIndexUrl: String = OFFICIAL_RUNTIME_INDEX,
    private val logger: LauncherLogger = NoopLauncherLogger,
) : JavaResolver {
    private val platform by lazy { mojangRuntimePlatform(environment) }
    private val mutex = Mutex()

    override suspend fun resolve(component: String?, requiredMajor: Int): String = mutex.withLock {
        if (environment.operatingSystem == OperatingSystem.LINUX && environment.architecture == Architecture.ARM64) {
            return@withLock AdoptiumJavaResolver(directories, httpClient, downloadPipeline).resolve(requiredMajor)
        }
        val runtimeComponent = component ?: defaultComponent(requiredMajor)
        val platformRoot = directories.runtimes.toNioPath()
            .resolve(runtimeComponent)
            .resolve(platform)

        findInstalledExecutable(platformRoot)?.let { return@withLock it.toString() }

        val index = getJson<JavaRuntimeIndex>(runtimeIndexUrl)
        val release = index[platform]
            ?.get(runtimeComponent)
            .orEmpty()
            .filter { it.availability.progress == 100 }
            .maxByOrNull { it.version.released }
            ?: throw LauncherException.RuntimeUnavailable(
                text(Res.string.msg_mojang_does_not_provide_s_for_s, runtimeComponent, platform),
            )
        val actualMajor = release.version.name.substringBefore('.').substringBefore('u').toIntOrNull()
        if (actualMajor != requiredMajor) {
            throw LauncherException.InvalidMetadata(
                text(Res.string.msg_mojang_runtime_wrong_java, runtimeComponent, actualMajor?.toString() ?: "?", requiredMajor),
            )
        }

        val runtimeRoot = platformRoot.resolve(release.manifest.sha1)
        installedExecutable(runtimeRoot)?.let { return@withLock it.toString() }

        logger.info(
            "runtime",
            "Provisioning Mojang Java runtime",
            mapOf(
                "component" to runtimeComponent,
                "platform" to platform,
                "version" to release.version.name,
            ),
        )
        val manifest = getJson<JavaRuntimeManifest>(
            release.manifest.url,
            release.manifest.sha1,
            "$runtimeComponent manifest",
        )
        provision(runtimeRoot, release.manifest.sha1, manifest)
        installedExecutable(runtimeRoot)?.let { executable ->
            logger.info(
                "runtime",
                "Mojang Java runtime is ready",
                mapOf(
                    "component" to runtimeComponent,
                    "platform" to platform,
                    "version" to release.version.name,
                ),
            )
            return@withLock executable.toString()
        }
        throw LauncherException.RuntimeUnavailable(text(Res.string.msg_the_managed_java_s_runtime_is_incomplete, requiredMajor))
    }

    private suspend fun provision(runtimeRoot: Path, manifestSha1: String, manifest: JavaRuntimeManifest) {
        Files.createDirectories(runtimeRoot)
        manifest.files.forEach { (name, entry) ->
            if (entry.type == JavaRuntimeFileType.DIRECTORY) {
                Files.createDirectories(safePath(runtimeRoot, name))
            }
        }
        val downloads = manifest.files.mapNotNull { (name, entry) ->
            if (entry.type != JavaRuntimeFileType.FILE) return@mapNotNull null
            val raw = entry.downloads?.raw
                ?: throw LauncherException.InvalidMetadata(text(Res.string.msg_mojang_runtime_file_s_has_no_raw, name))
            DownloadRequest(
                url = raw.url,
                destination = safePath(runtimeRoot, name).toString().toPath(),
                sha1 = raw.sha1,
                size = raw.size,
                progressLabel = text(Res.string.msg_downloading_java_runtime),
            )
        }
        downloadPipeline.download(
            downloads,
            directories.staging / "runtimes" / manifestSha1,
        )
        manifest.files.forEach { (name, entry) ->
            if (entry.executable && entry.type == JavaRuntimeFileType.FILE) {
                makeExecutable(safePath(runtimeRoot, name))
            }
        }
        manifest.files.forEach { (name, entry) ->
            if (entry.type != JavaRuntimeFileType.LINK) return@forEach
            val targetValue = entry.target
                ?: throw LauncherException.InvalidMetadata(text(Res.string.msg_mojang_runtime_link_s_has_no_target, name))
            val link = safePath(runtimeRoot, name)
            val resolvedTarget = link.parent.resolve(targetValue).normalize()
            if (!resolvedTarget.startsWith(runtimeRoot)) {
                throw LauncherException.InvalidMetadata(text(Res.string.msg_mojang_runtime_link_s_escapes_the_runtime, name))
            }
            Files.createDirectories(link.parent)
            if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                Files.createSymbolicLink(link, Path.of(targetValue))
            }
        }
        val executable = executablePath(runtimeRoot)
        if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
            throw LauncherException.RuntimeUnavailable(text(Res.string.msg_mojang_s_java_executable_is_not_usable, executable))
        }
        Files.writeString(
            runtimeRoot.resolve(COMPLETE_MARKER),
            manifestSha1,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
        )
    }

    private fun findInstalledExecutable(platformRoot: Path): Path? {
        if (!Files.isDirectory(platformRoot)) return null
        return Files.newDirectoryStream(platformRoot).use { entries ->
            entries.mapNotNull { runtimeRoot ->
                installedExecutable(runtimeRoot)?.let { executable -> runtimeRoot to executable }
            }.maxByOrNull { (runtimeRoot) ->
                Files.getLastModifiedTime(runtimeRoot.resolve(COMPLETE_MARKER)).toMillis()
            }?.second
        }
    }

    private fun installedExecutable(runtimeRoot: Path): Path? {
        if (!Files.isRegularFile(runtimeRoot.resolve(COMPLETE_MARKER))) return null
        return executablePath(runtimeRoot).takeIf { Files.isRegularFile(it) && Files.isExecutable(it) }
    }

    private fun executablePath(runtimeRoot: Path): Path = when {
        platform.startsWith("mac-os") -> runtimeRoot.resolve("jre.bundle/Contents/Home/bin/java")
        platform.startsWith("windows") -> runtimeRoot.resolve("bin/java.exe")
        else -> runtimeRoot.resolve("bin/java")
    }

    private suspend inline fun <reified T> getJson(
        url: String,
        expectedSha1: String? = null,
        artifactName: String = "Java runtime metadata",
    ): T {
        try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                throw LauncherException.Network(
                    text(Res.string.msg_java_runtime_metadata_request_failed_with_http, response.status.value),
                )
            }
            val body = response.bodyAsText()
            if (expectedSha1 != null) {
                val actual = body.encodeUtf8().sha1().hex()
                if (!actual.equals(expectedSha1, ignoreCase = true)) {
                    throw LauncherException.ChecksumMismatch(artifactName, expectedSha1, actual)
                }
            }
            return runtimeJson.decodeFromString(body)
        } catch (error: CancellationException) {
            throw error
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.Network(text(Res.string.msg_mojang_java_runtime_metadata_could_not_be), error)
        }
    }

    private fun defaultComponent(requiredMajor: Int): String = when (requiredMajor) {
        8 -> "jre-legacy"

        16 -> "java-runtime-alpha"

        17 -> "java-runtime-gamma"

        21 -> "java-runtime-delta"

        25 -> "java-runtime-epsilon"

        else -> throw LauncherException.RuntimeUnavailable(
            text(Res.string.msg_minecraft_metadata_does_not_name_a_mojang, requiredMajor),
        )
    }

    private fun makeExecutable(path: Path) {
        if (!path.toFile().setExecutable(true, false) && !Files.isExecutable(path)) {
            throw LauncherException.FileSystem(text(Res.string.msg_the_managed_java_executable_permission_could_not, path))
        }
    }

    private fun safePath(root: Path, name: String): Path {
        if (name.isBlank() || '\\' in name) {
            throw LauncherException.InvalidMetadata(text(Res.string.msg_mojang_runtime_metadata_contains_an_unsafe_path))
        }
        val relative = Path.of(name)
        val target = root.resolve(relative).normalize()
        if (relative.isAbsolute || relative.any { it.toString() == ".." } || !target.startsWith(root)) {
            throw LauncherException.InvalidMetadata(text(Res.string.msg_mojang_runtime_metadata_contains_an_unsafe_path_2, name))
        }
        return target
    }

    private companion object {
        const val OFFICIAL_RUNTIME_INDEX =
            "https://piston-meta.mojang.com/v1/products/java-runtime/" +
                "2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json"
        const val COMPLETE_MARKER = ".complete"
        val runtimeJson = Json { ignoreUnknownKeys = true }
    }
}

internal fun mojangRuntimePlatform(environment: PlatformEnvironment): String = when (environment.operatingSystem) {
    OperatingSystem.LINUX -> when (environment.architecture) {
        Architecture.X86_64 -> "linux"
        Architecture.X86 -> "linux-i386"
        else -> unsupportedPlatform("Linux", environment.architecture)
    }

    OperatingSystem.MACOS -> when (environment.architecture) {
        Architecture.ARM64 -> "mac-os-arm64"
        Architecture.X86_64 -> "mac-os"
        else -> unsupportedPlatform("macOS", environment.architecture)
    }

    OperatingSystem.IOS -> throw LauncherException.RuntimeUnavailable(
        text(Res.string.msg_ios_runtimes_are_provided_by_the_ios),
    )

    OperatingSystem.WINDOWS -> when (environment.architecture) {
        Architecture.ARM64 -> "windows-arm64"
        Architecture.X86_64 -> "windows-x64"
        Architecture.X86 -> "windows-x86"
        else -> unsupportedPlatform("Windows", environment.architecture)
    }

    OperatingSystem.UNKNOWN -> throw LauncherException.RuntimeUnavailable(
        text(Res.string.msg_mojang_does_not_provide_a_java_runtime),
    )
}

private fun unsupportedPlatform(os: String, architecture: Architecture): Nothing = throw LauncherException.RuntimeUnavailable(
    text(Res.string.msg_mojang_does_not_provide_a_desktop_java, os, architecture.name.lowercase()),
)

private typealias JavaRuntimeIndex = Map<String, Map<String, List<JavaRuntimeRelease>>>

@Serializable
private data class JavaRuntimeRelease(
    val availability: JavaRuntimeAvailability,
    val manifest: JavaRuntimeManifestReference,
    val version: JavaRuntimeVersion,
)

@Serializable
private data class JavaRuntimeManifestReference(val sha1: String, val size: Long? = null, val url: String)

@Serializable
private data class JavaRuntimeAvailability(val progress: Int = 0)

@Serializable
private data class JavaRuntimeVersion(val name: String, val released: String = "")

@Serializable
private data class JavaRuntimeManifest(val files: Map<String, JavaRuntimeFile>)

@Serializable
private data class JavaRuntimeFile(
    val downloads: JavaRuntimeDownloads? = null,
    val executable: Boolean = false,
    val target: String? = null,
    val type: JavaRuntimeFileType,
)

@Serializable
private data class JavaRuntimeDownloads(val raw: DownloadReference? = null)

@Serializable
private enum class JavaRuntimeFileType {
    @SerialName("directory")
    DIRECTORY,

    @SerialName("file")
    FILE,

    @SerialName("link")
    LINK,
}
