package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
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
import net.blockhost.trestle.instance.InstanceExporter
import net.blockhost.trestle.instance.GameDataManager
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.ForgeMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.NeoForgeMetadataClient
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.metadata.QuiltMetadataClient
import net.blockhost.trestle.logging.BufferedLauncherLogger
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.LogSink
import net.blockhost.trestle.resources.ArchiveExtractor
import net.blockhost.trestle.resources.AtLauncherResourcePlatform
import net.blockhost.trestle.resources.CurseForgeResourcePlatform
import net.blockhost.trestle.resources.FtbResourcePlatform
import net.blockhost.trestle.resources.LegacyFtbResourcePlatform
import net.blockhost.trestle.resources.ModpackInstaller
import net.blockhost.trestle.resources.ModrinthResourcePlatform
import net.blockhost.trestle.resources.ResourceInstaller
import net.blockhost.trestle.resources.ResourcePlatformRegistry
import net.blockhost.trestle.resources.TechnicResourcePlatform
import net.blockhost.trestle.runtime.MinecraftRuntime
import net.blockhost.trestle.runtime.SystemProfile
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class LauncherServices private constructor(
    val repository: InstanceRepository,
    val metadataClient: MinecraftMetadataClient,
    val fabricMetadataClient: FabricMetadataClient,
    val neoForgeMetadataClient: NeoForgeMetadataClient,
    val forgeMetadataClient: ForgeMetadataClient,
    val quiltMetadataClient: QuiltMetadataClient,
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
    val instanceExporter: InstanceExporter,
    val gameDataManager: GameDataManager,
    val preferences: LauncherPreferencesStore,
    val updateChecker: UpdateChecker,
    private val httpClient: HttpClient,
    private val coreDownloadPipeline: DownloadPipeline,
    curseForgeApiKey: String,
    technicClientId: String,
    archiveExtractor: ArchiveExtractor,
) {
    private val resourceDownloadPipeline = DownloadPipeline(httpClient, FileSystem.SYSTEM, logger = logger)
    private val modrinthResources = ModrinthResourcePlatform(
        httpClient = httpClient,
        userAgent = BuildInfo.USER_AGENT,
    )
    private val curseForgeResources = CurseForgeResourcePlatform(httpClient, curseForgeApiKey)
    private val atLauncherResources = AtLauncherResourcePlatform(httpClient, BuildInfo.USER_AGENT)
    private val ftbResources = FtbResourcePlatform(httpClient)
    private val legacyFtbResources = LegacyFtbResourcePlatform(httpClient)
    private val technicResources = TechnicResourcePlatform(httpClient, technicClientId)
    val resourcePlatforms = ResourcePlatformRegistry(
        listOf(
            modrinthResources,
            curseForgeResources,
            atLauncherResources,
            ftbResources,
            legacyFtbResources,
            technicResources,
        ),
    )
    val resourceInstaller = ResourceInstaller(resourcePlatforms, resourceDownloadPipeline, FileSystem.SYSTEM)
    val modpackInstaller = ModpackInstaller(
        platforms = resourcePlatforms,
        repository = repository,
        metadataClient = metadataClient,
        fabricMetadataClient = fabricMetadataClient,
        neoForgeMetadataClient = neoForgeMetadataClient,
        forgeMetadataClient = forgeMetadataClient,
        quiltMetadataClient = quiltMetadataClient,
        minecraftInstaller = installer,
        downloadPipeline = resourceDownloadPipeline,
        fileSystem = FileSystem.SYSTEM,
        directories = directories,
        archiveExtractor = archiveExtractor,
        systemProfile = systemProfile,
    )

    init {
        configurePreferences(preferences.read())
    }

    fun configurePreferences(preferences: LauncherPreferences) {
        val network = preferences.network
        coreDownloadPipeline.configure(network.concurrentDownloads, network.retryLimit)
        resourceDownloadPipeline.configure(network.concurrentDownloads, network.retryLimit)
        logger.configure(preferences.console.historyLimit, preferences.console.stopLoggingOnOverflow)
        resourceInstaller.configure(
            scanSubfolders = preferences.content.scanSubfolders,
            installDependencies = preferences.content.installDependencies,
            detectIncompatibilities = preferences.content.detectIncompatibilities,
            trackMetadata = preferences.content.trackMetadata,
        )
    }

    suspend fun downloadImport(url: String): Pair<String, ByteArray> {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw net.blockhost.trestle.domain.LauncherException.Network(
                "Import download failed with HTTP ${response.status.value}.",
            )
        }
        val bytes = response.body<ByteArray>()
        require(bytes.size <= MAX_REMOTE_IMPORT_BYTES) { "Remote imports must be smaller than 512 MiB." }
        val fileName = io.ktor.http.Url(url).encodedPath.substringAfterLast('/').substringBefore('?')
            .ifBlank { "download.zip" }
        return fileName to bytes
    }

    fun close() = httpClient.close()

    companion object {
        private const val MAX_REMOTE_IMPORT_BYTES = 512 * 1024 * 1024
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
            instanceExporter: InstanceExporter,
            gameDataManager: GameDataManager,
            runtimeFactory: (
                LauncherDirectories,
                MinecraftInstaller,
                SessionProvider,
                LauncherLogger,
                DownloadPipeline,
            ) -> MinecraftRuntime,
        ): LauncherServices {
            val fileSystem = FileSystem.SYSTEM
            val preferences = LauncherPreferencesStore(fileSystem, root / "preferences.json")
            val savedPreferences = preferences.read()
            val directories = LauncherDirectories(
                root = root,
                instances = savedPreferences.folders.instances.pathOr(root / "instances", root),
                runtimes = savedPreferences.folders.runtimes.pathOr(root / "runtimes", root),
                exports = savedPreferences.folders.downloads.pathOr(root / "exports", root),
            )
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
            val forgeMetadataClient = ForgeMetadataClient(httpClient, BuildInfo.USER_AGENT)
            val quiltMetadataClient = QuiltMetadataClient(httpClient)
            val downloadPipeline = DownloadPipeline(
                httpClient,
                fileSystem,
                maxConcurrency = savedPreferences.network.concurrentDownloads,
                maxAttempts = savedPreferences.network.retryLimit,
                logger = logger,
            )
            logger.configure(savedPreferences.console.historyLimit, savedPreferences.console.stopLoggingOnOverflow)
            val updateChecker = UpdateChecker(httpClient)
            val installer = MinecraftInstaller(
                repository,
                metadataClient,
                fabricMetadataClient,
                neoForgeMetadataClient,
                forgeMetadataClient,
                quiltMetadataClient,
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
                forgeMetadataClient,
                quiltMetadataClient,
                installer,
                runtimeFactory(directories, installer, accounts, logger, downloadPipeline),
                directories,
                environment,
                systemProfile,
                accounts,
                credentialStore,
                MinecraftProfileClient(httpClient, logger = logger),
                SkinLibrary(
                    fileSystem,
                    savedPreferences.folders.skins.pathOr(root / "skins", root),
                    clock::nowMillis,
                ),
                logger,
                clock,
                instanceExporter,
                gameDataManager,
                preferences,
                updateChecker,
                httpClient,
                downloadPipeline,
                curseForgeApiKey,
                savedPreferences.technicClientId,
                archiveExtractor,
            )
        }
    }
}

private fun String.pathOr(fallback: Path, root: Path): Path {
    if (isBlank()) return fallback
    val configured = toPath()
    return if (configured.isAbsolute) configured else root / configured
}
