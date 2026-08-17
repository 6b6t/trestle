package net.blockhost.trestle.install

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.download.DownloadRequest
import net.blockhost.trestle.instance.InstanceRepository
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.InstalledVersion
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataResolver
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.metadata.downloads
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

fun interface EpochClock {
    fun nowMillis(): Long
}

data class LauncherDirectories(
    val root: Path,
    val instances: Path = root / "instances",
    val libraries: Path = root / "libraries",
    val assets: Path = root / "assets",
    val versions: Path = root / "versions",
    val logging: Path = root / "logging",
    val staging: Path = root / "staging",
)

class MinecraftInstaller(
    private val repository: InstanceRepository,
    private val metadataClient: MinecraftMetadataClient,
    private val fabricMetadataClient: FabricMetadataClient,
    private val downloadPipeline: DownloadPipeline,
    private val fileSystem: FileSystem,
    private val directories: LauncherDirectories,
    private val environment: PlatformEnvironment,
    private val clock: EpochClock,
) {
    suspend fun install(instance: GameInstance, onProgress: suspend (DownloadProgress) -> Unit = {}): GameInstance {
        var working = instance.copy(
            installationState = InstallationState.Installing(0, null, 0, 0),
        )
        repository.update(working)
        try {
            val vanilla = metadataClient.resolveVersion(instance.minecraftVersionId)
            val effective = when (instance.modLoader) {
                ModLoader.VANILLA -> vanilla
                ModLoader.FABRIC -> {
                    val loaderVersion = instance.loaderVersion
                        ?: fabricMetadataClient.loaderVersions(instance.minecraftVersionId)
                            .firstOrNull { it.stable }?.version
                        ?: throw LauncherException.InvalidMetadata(
                            "No stable Fabric loader supports ${instance.minecraftVersionId}.",
                        )
                    MinecraftMetadataResolver.merge(
                        vanilla,
                        fabricMetadataClient.profile(instance.minecraftVersionId, loaderVersion),
                    )
                }
                else -> throw LauncherException.UnsupportedLoader(instance.modLoader)
            }
            val resolved = MinecraftMetadataResolver.resolve(effective, environment)
            val assetIndex = resolved.assetIndex?.let { metadataClient.fetchAssetIndex(it) }
            val requests = buildList {
                add(
                    DownloadRequest(
                        url = resolved.client.url,
                        destination = directories.versions / instance.minecraftVersionId /
                            "${instance.minecraftVersionId}.jar",
                        sha1 = resolved.client.sha1,
                        size = resolved.client.size,
                    ),
                )
                resolved.libraries.forEach { library ->
                    add(
                        DownloadRequest(
                            library.url,
                            directories.libraries / library.path,
                            library.sha1,
                            library.size,
                        ),
                    )
                }
                resolved.assetIndex?.let { index ->
                    add(
                        DownloadRequest(
                            index.url,
                            directories.assets / "indexes" / "${index.id}.json",
                            index.sha1,
                            index.size,
                        ),
                    )
                }
                assetIndex?.downloads()?.forEach { asset ->
                    add(
                        DownloadRequest(
                            asset.url,
                            directories.assets / requireNotNull(asset.path),
                            asset.sha1,
                            asset.size,
                        ),
                    )
                }
                resolved.logging?.file?.let { logging ->
                    add(
                        DownloadRequest(
                            logging.url,
                            directories.logging / (logging.path ?: "${resolved.metadata.id}-client.xml"),
                            logging.sha1,
                            logging.size,
                        ),
                    )
                }
            }

            var lastPersistedBytes = 0L
            var lastPersistedFiles = -1
            downloadPipeline.download(
                requests,
                directories.staging / instance.id.value,
            ) { progress ->
                if (
                    progress.completedFiles != lastPersistedFiles ||
                    progress.completedBytes - lastPersistedBytes >= PROGRESS_PERSIST_INTERVAL_BYTES
                ) {
                    working = working.copy(
                        requiredJavaMajor = resolved.requiredJavaMajor,
                        installationState = InstallationState.Installing(
                            progress.completedBytes,
                            progress.totalBytes,
                            progress.completedFiles,
                            progress.totalFiles,
                        ),
                    )
                    repository.update(working)
                    lastPersistedBytes = progress.completedBytes
                    lastPersistedFiles = progress.completedFiles
                }
                onProgress(progress)
            }

            val manifest = InstalledVersion(
                metadata = resolved.metadata,
                libraries = resolved.libraries,
                requiredJavaMajor = resolved.requiredJavaMajor,
                gameArguments = resolved.gameArguments,
                jvmArguments = resolved.jvmArguments,
                assetIndexId = resolved.assetIndex?.id,
                loggingPath = resolved.logging?.file?.let { it.path ?: "${resolved.metadata.id}-client.xml" },
            )
            writeInstalledVersion(instance, manifest)
            return working.copy(
                requiredJavaMajor = resolved.requiredJavaMajor,
                installationState = InstallationState.Installed(clock.nowMillis()),
            ).also { repository.update(it) }
        } catch (error: CancellationException) {
            repository.update(instance.copy(installationState = InstallationState.NotInstalled))
            throw error
        } catch (error: Exception) {
            val message = error.message ?: "Installation failed."
            repository.update(instance.copy(installationState = InstallationState.Failed(message)))
            throw error
        }
    }

    fun readInstalledVersion(instance: GameInstance): InstalledVersion {
        val path = installedVersionPath(instance)
        if (!fileSystem.exists(path)) {
            throw LauncherException.FileSystem("The installed version manifest is missing.")
        }
        return try {
            installationJson.decodeFromString(fileSystem.read(path) { readUtf8() })
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The installed version manifest could not be read.", error)
        }
    }

    private fun writeInstalledVersion(instance: GameInstance, installedVersion: InstalledVersion) {
        val destination = installedVersionPath(instance)
        val temporary = destination.parent!! / ".${destination.name}.tmp"
        fileSystem.createDirectories(requireNotNull(destination.parent))
        fileSystem.write(temporary) {
            writeUtf8(installationJson.encodeToString(InstalledVersion.serializer(), installedVersion))
            flush()
        }
        fileSystem.atomicMove(temporary, destination)
    }

    private fun installedVersionPath(instance: GameInstance): Path =
        instance.instanceDirectory.toPath() / ".trestle" / "installed-version.json"

    private companion object {
        const val PROGRESS_PERSIST_INTERVAL_BYTES = 1024L * 1024L
        val installationJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
