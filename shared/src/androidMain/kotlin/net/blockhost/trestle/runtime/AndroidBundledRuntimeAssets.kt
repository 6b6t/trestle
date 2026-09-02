package net.blockhost.trestle.runtime

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadProgress
import okio.FileSystem
import okio.Path
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

internal class AndroidBundledRuntimeAssets(
    private val assets: AssetManager,
    private val fileSystem: FileSystem,
) {
    private val manifest by lazy {
        val content = assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
        JSON.decodeFromString<RuntimeAssetManifest>(content).also {
            if (it.format != MANIFEST_FORMAT || it.amethystRevision != AMETHYST_REVISION) {
                throw LauncherException.InvalidMetadata("The bundled Android runtime manifest is unsupported.")
            }
        }
    }

    suspend fun install(
        platform: String,
        kind: String,
        destination: Path,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val selected = manifest.files.filter { it.platform in setOf(COMMON_PLATFORM, platform) && it.kind == kind }
        if (selected.isEmpty()) {
            throw LauncherException.RuntimeUnavailable("The app does not include $kind runtime assets for Android $platform.")
        }
        fileSystem.createDirectories(destination)
        val totalBytes = selected.sumOf(RuntimeAssetFile::size)
        var completedBytes = 0L
        selected.forEachIndexed { index, asset ->
            val target = destination / asset.path.substringAfterLast('/')
            copyVerified(asset, target)
            completedBytes += asset.size
            onProgress(
                DownloadProgress(
                    completedBytes = completedBytes,
                    totalBytes = totalBytes,
                    completedFiles = index + 1,
                    totalFiles = selected.size,
                    activeLabel = "Installing bundled Android runtime: ${target.name}",
                ),
            )
        }
    }

    suspend fun copyRuntime(
        platform: String,
        destination: Path,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val asset = manifest.files.singleOrNull { it.platform == platform && it.kind == RUNTIME_KIND }
            ?: throw LauncherException.RuntimeUnavailable(
                "The app does not include a Java runtime for Android $platform.",
            )
        copyVerified(asset, destination)
        onProgress(
            DownloadProgress(
                completedBytes = asset.size,
                totalBytes = asset.size,
                completedFiles = 1,
                totalFiles = 1,
                activeLabel = "Installing bundled Java runtime",
            ),
        )
    }

    private fun copyVerified(asset: RuntimeAssetFile, destination: Path) {
        val target = File(destination.toString())
        if (target.isFile && target.length() == asset.size && sha256(target) == asset.sha256) return
        target.parentFile?.mkdirs()
        val staged = File(target.parentFile, ".${target.name}.part")
        staged.delete()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            assets.open("$ASSET_ROOT/${asset.path}").use { input ->
                FileOutputStream(staged).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        copied += count
                        if (copied > asset.size) {
                            throw LauncherException.InvalidMetadata("The bundled ${asset.path} asset is too large.")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val actualHash = digest.digest().toHexString()
            if (copied != asset.size || actualHash != asset.sha256) {
                throw LauncherException.ChecksumMismatch(asset.path, asset.sha256, actualHash)
            }
            if (target.exists() && !target.delete()) {
                throw LauncherException.FileSystem("The old ${target.name} runtime asset could not be replaced.")
            }
            if (!staged.renameTo(target)) {
                throw LauncherException.FileSystem("The ${target.name} runtime asset could not be installed.")
            }
        } finally {
            staged.delete()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val MANIFEST_FORMAT = 1
        const val AMETHYST_REVISION = "d8a195640a7e0929f2ee532d7784de2b980c6c48"
        const val ASSET_ROOT = "amethyst/android"
        const val MANIFEST_PATH = "$ASSET_ROOT/manifest.json"
        const val COMMON_PLATFORM = "common"
        const val RUNTIME_KIND = "runtime"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class RuntimeAssetManifest(
    val format: Int,
    val amethystRevision: String,
    val files: List<RuntimeAssetFile>,
)

@Serializable
private data class RuntimeAssetFile(
    val path: String,
    val platform: String,
    val kind: String,
    val size: Long,
    val sha256: String,
)
