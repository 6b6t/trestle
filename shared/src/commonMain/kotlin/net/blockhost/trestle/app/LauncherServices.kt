package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import net.blockhost.trestle.auth.AccountManager
import net.blockhost.trestle.auth.AccountCredentialStore
import net.blockhost.trestle.auth.FileAccountManager
import net.blockhost.trestle.auth.MinecraftProfileClient
import net.blockhost.trestle.auth.MinecraftAuthenticator
import net.blockhost.trestle.auth.SessionProvider
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
import net.blockhost.trestle.logging.BufferedLauncherLogger
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.LogSink
import net.blockhost.trestle.mods.CurseForgeDownloadProvider
import net.blockhost.trestle.mods.ModInstaller
import net.blockhost.trestle.mods.ModrinthDownloadProvider
import net.blockhost.trestle.runtime.MinecraftRuntime
import net.blockhost.trestle.runtime.SystemProfile
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
    val systemProfile: SystemProfile,
    val accounts: AccountManager,
    val credentialStore: AccountCredentialStore,
    val profileClient: MinecraftProfileClient,
    val logger: LauncherLogger,
    private val httpClient: HttpClient,
) {
    val modrinthDownloads = ModrinthDownloadProvider(
        httpClient = httpClient,
        userAgent = BuildInfo.USER_AGENT,
    )
    val modInstaller = ModInstaller(DownloadPipeline(httpClient, FileSystem.SYSTEM, logger = logger))

    fun curseForgeDownloads(apiKey: String) = CurseForgeDownloadProvider(httpClient, apiKey)

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
            val downloadPipeline = DownloadPipeline(httpClient, fileSystem, logger = logger)
            val installer = MinecraftInstaller(
                repository,
                metadataClient,
                fabricMetadataClient,
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
                installer,
                runtimeFactory(directories, installer, accounts, logger, downloadPipeline),
                directories,
                environment,
                systemProfile,
                accounts,
                credentialStore,
                MinecraftProfileClient(httpClient, fileSystem, logger = logger),
                logger,
                httpClient,
            )
        }
    }
}
