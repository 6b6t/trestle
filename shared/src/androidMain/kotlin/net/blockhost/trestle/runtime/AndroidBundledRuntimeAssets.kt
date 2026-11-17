package net.blockhost.trestle.runtime

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_installing_bundled_android_runtime_s
import net.blockhost.trestle.resources.msg_installing_bundled_java_runtime
import net.blockhost.trestle.resources.msg_the_app_does_not_include_a_java
import net.blockhost.trestle.resources.msg_the_app_does_not_include_s_runtime
import net.blockhost.trestle.resources.msg_the_bundled_android_runtime_manifest_is_unsupported
import net.blockhost.trestle.resources.msg_the_bundled_s_asset_is_too_large
import net.blockhost.trestle.resources.msg_the_old_s_runtime_asset_could_not
import net.blockhost.trestle.resources.msg_the_s_runtime_asset_could_not_be
import net.blockhost.trestle.text.text
import okio.FileSystem
import okio.Path
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

internal class AndroidBundledRuntimeAssets(private val assets: AssetManager, private val fileSystem: FileSystem) {
    private val manifest by lazy {
        val content = assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
        JSON.decodeFromString<RuntimeAssetManifest>(content).also {
            if (it.format != MANIFEST_FORMAT || it.amethystRevision != AMETHYST_REVISION) {
                throw LauncherException.InvalidMetadata(text(Res.string.msg_the_bundled_android_runtime_manifest_is_unsupported))
            }
        }
    }

    suspend fun install(platform: String, kind: String, destination: Path, onProgress: suspend (DownloadProgress) -> Unit = {}) =
        withContext(Dispatchers.IO) {
            val selected = manifest.files.filter { it.platform in setOf(COMMON_PLATFORM, platform) && it.kind == kind }
            if (selected.isEmpty()) {
                throw LauncherException.RuntimeUnavailable(text(Res.string.msg_the_app_does_not_include_s_runtime, kind, platform))
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
                        activeLabel = text(Res.string.msg_installing_bundled_android_runtime_s, target.name),
                    ),
                )
            }
        }

    suspend fun copyRuntime(platform: String, destination: Path, onProgress: suspend (DownloadProgress) -> Unit = {}) =
        withContext(Dispatchers.IO) {
            val asset = manifest.files.singleOrNull { it.platform == platform && it.kind == RUNTIME_KIND }
                ?: throw LauncherException.RuntimeUnavailable(
                    text(Res.string.msg_the_app_does_not_include_a_java, platform),
                )
            copyVerified(asset, destination)
            onProgress(
                DownloadProgress(
                    completedBytes = asset.size,
                    totalBytes = asset.size,
                    completedFiles = 1,
                    totalFiles = 1,
                    activeLabel = text(Res.string.msg_installing_bundled_java_runtime),
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
                            throw LauncherException.InvalidMetadata(text(Res.string.msg_the_bundled_s_asset_is_too_large, asset.path))
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
                throw LauncherException.FileSystem(text(Res.string.msg_the_old_s_runtime_asset_could_not, target.name))
            }
            if (!staged.renameTo(target)) {
                throw LauncherException.FileSystem(text(Res.string.msg_the_s_runtime_asset_could_not_be, target.name))
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
private data class RuntimeAssetManifest(val format: Int, val amethystRevision: String, val files: List<RuntimeAssetFile>)

@Serializable
private data class RuntimeAssetFile(val path: String, val platform: String, val kind: String, val size: Long, val sha256: String)
