package net.blockhost.trestle.install

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
import net.blockhost.trestle.metadata.ForgeMetadataClient
import net.blockhost.trestle.metadata.InstalledVersion
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataResolver
import net.blockhost.trestle.metadata.MojangLibrary
import net.blockhost.trestle.metadata.NeoForgeMetadataClient
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.metadata.QuiltMetadataClient
import net.blockhost.trestle.metadata.downloads
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
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
    val runtimes: Path = root / "runtimes",
    val logging: Path = root / "logging",
    val staging: Path = root / "staging",
    val exports: Path = root / "exports",
)

class MinecraftInstaller(
    private val repository: InstanceRepository,
    private val metadataClient: MinecraftMetadataClient,
    private val fabricMetadataClient: FabricMetadataClient,
    private val neoForgeMetadataClient: NeoForgeMetadataClient,
    private val forgeMetadataClient: ForgeMetadataClient,
    private val quiltMetadataClient: QuiltMetadataClient,
    private val downloadPipeline: DownloadPipeline,
    private val fileSystem: FileSystem,
    private val directories: LauncherDirectories,
    private val environment: PlatformEnvironment,
    private val clock: EpochClock,
    private val logger: LauncherLogger = NoopLauncherLogger,
) {
    suspend fun install(instance: GameInstance, onProgress: suspend (DownloadProgress) -> Unit = {}): GameInstance {
        val previousProgress = instance.installationState as? InstallationState.Interrupted
        var working = instance.copy(
            installationState = InstallationState.Installing(
                completedBytes = previousProgress?.completedBytes ?: 0L,
                totalBytes = previousProgress?.totalBytes,
                completedFiles = previousProgress?.completedFiles ?: 0,
                totalFiles = previousProgress?.totalFiles ?: 0,
            ),
        )
        try {
            repository.update(working)
            logger.info(
                "installer",
                "Installing Minecraft instance",
                mapOf(
                    "instance" to instance.id.value,
                    "version" to instance.minecraftVersionId,
                    "loader" to instance.modLoader,
                ),
            )
            val vanilla = metadataClient.resolveVersion(instance.minecraftVersionId)
            var auxiliaryLibraries = emptyList<MojangLibrary>()
            val effective = when (instance.modLoader) {
                ModLoader.VANILLA -> vanilla
                ModLoader.FABRIC -> {
                    val loaderVersion = instance.loaderVersion
                        ?: fabricMetadataClient.loaderVersions(instance.minecraftVersionId)
                            .firstOrNull { it.stable }?.version
                        ?: throw LauncherException.InvalidMetadata(
                            "No stable Fabric loader supports ${instance.minecraftVersionId}.",
                        )
                    working = working.copy(loaderVersion = loaderVersion)
                    MinecraftMetadataResolver.merge(
                        vanilla,
                        fabricMetadataClient.profile(instance.minecraftVersionId, loaderVersion),
                    )
                }
                ModLoader.NEOFORGE -> {
                    val loaderVersion = instance.loaderVersion
                        ?: neoForgeMetadataClient.loaderVersions(instance.minecraftVersionId)
                            .let { versions ->
                                versions.firstOrNull { it.stable } ?: versions.firstOrNull()
                            }
                            ?.version
                        ?: throw LauncherException.InvalidMetadata(
                            "No NeoForge version supports ${instance.minecraftVersionId}.",
                        )
                    working = working.copy(loaderVersion = loaderVersion)
                    val profile = neoForgeMetadataClient.profile(instance.minecraftVersionId, loaderVersion)
                    auxiliaryLibraries = profile.mavenFiles
                    MinecraftMetadataResolver.merge(vanilla, profile.metadata)
                }
                ModLoader.FORGE -> {
                    val loaderVersion = instance.loaderVersion
                        ?: forgeMetadataClient.loaderVersions(instance.minecraftVersionId)
                            .let { versions ->
                                versions.firstOrNull { it.recommended }
                                    ?: versions.firstOrNull { it.stable }
                                    ?: versions.firstOrNull()
                            }
                            ?.version
                        ?: throw LauncherException.InvalidMetadata(
                            "No Forge version supports ${instance.minecraftVersionId}.",
                        )
                    working = working.copy(loaderVersion = loaderVersion)
                    val profile = forgeMetadataClient.profile(instance.minecraftVersionId, loaderVersion)
                    auxiliaryLibraries = profile.mavenFiles
                    MinecraftMetadataResolver.merge(vanilla, profile.metadata)
                }
                ModLoader.QUILT -> {
                    val loaderVersion = instance.loaderVersion
                        ?: quiltMetadataClient.loaderVersions(instance.minecraftVersionId)
                            .let { versions -> versions.firstOrNull { it.stable } ?: versions.firstOrNull() }
                            ?.version
                        ?: throw LauncherException.InvalidMetadata(
                            "No Quilt Loader version supports ${instance.minecraftVersionId}.",
                        )
                    working = working.copy(loaderVersion = loaderVersion)
                    MinecraftMetadataResolver.merge(
                        vanilla,
                        quiltMetadataClient.profile(instance.minecraftVersionId, loaderVersion),
                    )
                }
            }
            val baseResolved = MinecraftMetadataResolver.resolve(effective, environment)
            val resolved = if (auxiliaryLibraries.isEmpty()) {
                baseResolved
            } else {
                val auxiliary = MinecraftMetadataResolver.resolveLibraries(
                    auxiliaryLibraries,
                    environment,
                    classpath = false,
                )
                baseResolved.copy(libraries = (baseResolved.libraries + auxiliary).distinctBy { it.path })
            }
            val assetIndex = resolved.assetIndex?.let { metadataClient.fetchAssetIndex(it) }
            val requests = buildList {
                add(
                    DownloadRequest(
                        url = resolved.client.url,
                        destination = directories.versions / instance.minecraftVersionId /
                            "${instance.minecraftVersionId}.jar",
                        sha1 = resolved.client.sha1,
                        size = resolved.client.size,
                        progressLabel = "Downloading Minecraft client",
                    ),
                )
                resolved.libraries.forEach { library ->
                    add(
                        DownloadRequest(
                            library.url,
                            directories.libraries / library.path,
                            library.sha1,
                            library.size,
                            progressLabel = "Downloading game libraries",
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
                            progressLabel = "Downloading asset index",
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
                            progressLabel = "Downloading game assets",
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
                            progressLabel = "Downloading logging configuration",
                        ),
                    )
                }
            }

            var lastPersistedBytes = 0L
            var lastPersistedFiles = -1
            var latestProgress: DownloadProgress? = null
            downloadPipeline.download(
                requests,
                directories.staging / instance.id.value,
            ) { progress ->
                latestProgress = progress
                if (
                    lastPersistedFiles < 0 ||
                    progress.completedFiles - lastPersistedFiles >= PROGRESS_PERSIST_INTERVAL_FILES ||
                    progress.completedFiles == progress.totalFiles ||
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

            latestProgress?.let { progress ->
                onProgress(
                    progress.copy(
                        activeLabel = "Finalizing installation",
                        isFinalizing = true,
                    ),
                )
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
            ).also {
                repository.update(it)
                logger.info("installer", "Instance installation completed", mapOf("instance" to instance.id.value))
            }
        } catch (error: CancellationException) {
            val progress = working.installationState as InstallationState.Installing
            withContext(NonCancellable) {
                repository.update(
                    working.copy(
                        installationState = InstallationState.Interrupted(
                            completedBytes = progress.completedBytes,
                            totalBytes = progress.totalBytes,
                            completedFiles = progress.completedFiles,
                            totalFiles = progress.totalFiles,
                        ),
                    ),
                )
            }
            logger.info("installer", "Instance installation paused", mapOf("instance" to instance.id.value))
            throw error
        } catch (error: Exception) {
            val message = error.message ?: "Installation failed."
            repository.update(instance.copy(installationState = InstallationState.Failed(message)))
            logger.error("installer", "Instance installation failed", error, mapOf("instance" to instance.id.value))
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
        const val PROGRESS_PERSIST_INTERVAL_FILES = 25
        const val PROGRESS_PERSIST_INTERVAL_BYTES = 1024L * 1024L
        val installationJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
