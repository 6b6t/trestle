package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.install.MinecraftInstaller
import net.blockhost.trestle.instance.FileInstanceRepository
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.instance.InstanceRepository
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.mods.CurseForgeDownloadProvider
import net.blockhost.trestle.mods.ModInstaller
import net.blockhost.trestle.mods.ModrinthDownloadProvider
import net.blockhost.trestle.runtime.MinecraftRuntime
import okio.FileSystem
import okio.Path

class LauncherServices private constructor(
    val repository: InstanceRepository,
    val metadataClient: MinecraftMetadataClient,
    val fabricMetadataClient: FabricMetadataClient,
    val installer: MinecraftInstaller,
    val runtime: MinecraftRuntime,
    val directories: LauncherDirectories,
    val environment: PlatformEnvironment,
    private val httpClient: HttpClient,
) {
    val modrinthDownloads = ModrinthDownloadProvider(
        httpClient = httpClient,
        userAgent = "Trestle/0.1.0 (net.blockhost.trestle)",
    )
    val modInstaller = ModInstaller(DownloadPipeline(httpClient, FileSystem.SYSTEM))

    fun curseForgeDownloads(apiKey: String) = CurseForgeDownloadProvider(httpClient, apiKey)

    fun close() = httpClient.close()

    companion object {
        fun create(
            root: Path,
            httpClient: HttpClient,
            environment: PlatformEnvironment,
            idFactory: InstanceIdFactory,
            clock: EpochClock,
            runtimeFactory: (LauncherDirectories, MinecraftInstaller) -> MinecraftRuntime,
        ): LauncherServices {
            val fileSystem = FileSystem.SYSTEM
            val directories = LauncherDirectories(root)
            val repository = FileInstanceRepository(
                fileSystem = fileSystem,
                registryPath = root / "instances.json",
                instancesDirectory = directories.instances,
                idFactory = idFactory,
            )
            val metadataClient = MinecraftMetadataClient(httpClient)
            val fabricMetadataClient = FabricMetadataClient(httpClient)
            val downloadPipeline = DownloadPipeline(httpClient, fileSystem)
            val installer = MinecraftInstaller(
                repository,
                metadataClient,
                fabricMetadataClient,
                downloadPipeline,
                fileSystem,
                directories,
                environment,
                clock,
            )
            return LauncherServices(
                repository,
                metadataClient,
                fabricMetadataClient,
                installer,
                runtimeFactory(directories, installer),
                directories,
                environment,
                httpClient,
            )
        }
    }
}
