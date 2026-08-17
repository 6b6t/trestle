package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import net.blockhost.trestle.auth.AccountManager
import net.blockhost.trestle.auth.AccountCredentialStore
import net.blockhost.trestle.auth.FileAccountManager
import net.blockhost.trestle.auth.MinecraftProfileClient
import net.blockhost.trestle.auth.MinecraftAuthenticator
import net.blockhost.trestle.auth.SessionProvider
import net.blockhost.trestle.auth.SkinLibrary
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.install.MinecraftInstaller
import net.blockhost.trestle.instance.FileInstanceRepository
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.instance.InstanceRepository
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.NeoForgeMetadataClient
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.logging.BufferedLauncherLogger
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.LogSink
import net.blockhost.trestle.resources.ArchiveExtractor
import net.blockhost.trestle.resources.CurseForgeResourcePlatform
import net.blockhost.trestle.resources.ModpackInstaller
import net.blockhost.trestle.resources.ModrinthResourcePlatform
import net.blockhost.trestle.resources.ResourceInstaller
import net.blockhost.trestle.resources.ResourcePlatformRegistry
import net.blockhost.trestle.runtime.MinecraftRuntime
import net.blockhost.trestle.runtime.SystemProfile
import okio.FileSystem
import okio.Path

class LauncherServices private constructor(
    val repository: InstanceRepository,
    val metadataClient: MinecraftMetadataClient,
    val fabricMetadataClient: FabricMetadataClient,
    val neoForgeMetadataClient: NeoForgeMetadataClient,
    val installer: MinecraftInstaller,
    val runtime: MinecraftRuntime,
    val directories: LauncherDirectories,
    val environment: PlatformEnvironment,
    val systemProfile: SystemProfile,
    val accounts: AccountManager,
    val credentialStore: AccountCredentialStore,
    val profileClient: MinecraftProfileClient,
    val skinLibrary: SkinLibrary,
    val logger: LauncherLogger,
    val clock: EpochClock,
    private val httpClient: HttpClient,
    curseForgeApiKey: String,
    archiveExtractor: ArchiveExtractor,
) {
    private val resourceDownloadPipeline = DownloadPipeline(httpClient, FileSystem.SYSTEM, logger = logger)
    private val modrinthResources = ModrinthResourcePlatform(
        httpClient = httpClient,
        userAgent = BuildInfo.USER_AGENT,
    )
    private val curseForgeResources = CurseForgeResourcePlatform(httpClient, curseForgeApiKey)
    val resourcePlatforms = ResourcePlatformRegistry(listOf(modrinthResources, curseForgeResources))
    val resourceInstaller = ResourceInstaller(resourcePlatforms, resourceDownloadPipeline, FileSystem.SYSTEM)
    val modpackInstaller = ModpackInstaller(
        platforms = resourcePlatforms,
        repository = repository,
        metadataClient = metadataClient,
        fabricMetadataClient = fabricMetadataClient,
        neoForgeMetadataClient = neoForgeMetadataClient,
        minecraftInstaller = installer,
        downloadPipeline = resourceDownloadPipeline,
        fileSystem = FileSystem.SYSTEM,
        directories = directories,
        archiveExtractor = archiveExtractor,
        systemProfile = systemProfile,
    )

    fun close() = httpClient.close()

    companion object {
        fun create(
            root: Path,
            httpClient: HttpClient,
            environment: PlatformEnvironment,
            idFactory: InstanceIdFactory,
            clock: EpochClock,
            systemProfile: SystemProfile,
            logSink: LogSink,
            credentialStore: AccountCredentialStore,
            authenticator: MinecraftAuthenticator,
            curseForgeApiKey: String,
            archiveExtractor: ArchiveExtractor,
            runtimeFactory: (
                LauncherDirectories,
                MinecraftInstaller,
                SessionProvider,
                LauncherLogger,
                DownloadPipeline,
            ) -> MinecraftRuntime,
        ): LauncherServices {
            val fileSystem = FileSystem.SYSTEM
            val directories = LauncherDirectories(root)
            val logger = BufferedLauncherLogger(clock::nowMillis, logSink)
            val credentialProtection = credentialStore.protection
            if (credentialProtection.encryptionOperational) {
                logger.info(
                    "accounts",
                    "Credential vault is ready",
                    mapOf(
                        "effectiveProtection" to credentialProtection.effectiveLevel,
                        "intendedProtection" to credentialProtection.intendedLevel,
                        "notes" to credentialProtection.notes.joinToString(),
                    ),
                )
            } else {
                logger.error(
                    "accounts",
                    "Credential vault is not available",
                    details = mapOf("notes" to credentialProtection.notes.joinToString()),
                )
            }
            val accounts = FileAccountManager(
                fileSystem,
                root / "accounts.json",
                logger,
                credentialStore,
                authenticator,
            )
            val repository = FileInstanceRepository(
                fileSystem = fileSystem,
                registryPath = root / "instances.json",
                instancesDirectory = directories.instances,
                idFactory = idFactory,
                logger = logger,
            )
            val metadataClient = MinecraftMetadataClient(httpClient, logger = logger)
            val fabricMetadataClient = FabricMetadataClient(httpClient)
            val neoForgeMetadataClient = NeoForgeMetadataClient(httpClient, BuildInfo.USER_AGENT)
            val downloadPipeline = DownloadPipeline(httpClient, fileSystem, logger = logger)
            val installer = MinecraftInstaller(
                repository,
                metadataClient,
                fabricMetadataClient,
                neoForgeMetadataClient,
                downloadPipeline,
                fileSystem,
                directories,
                environment,
                clock,
                logger,
            )
            return LauncherServices(
                repository,
                metadataClient,
                fabricMetadataClient,
                neoForgeMetadataClient,
                installer,
                runtimeFactory(directories, installer, accounts, logger, downloadPipeline),
                directories,
                environment,
                systemProfile,
                accounts,
                credentialStore,
                MinecraftProfileClient(httpClient, logger = logger),
                SkinLibrary(fileSystem, root / "skins", clock::nowMillis),
                logger,
                clock,
                httpClient,
                curseForgeApiKey,
                archiveExtractor,
            )
        }
    }
}
