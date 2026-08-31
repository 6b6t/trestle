package net.blockhost.trestle.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.blockhost.trestle.app.LauncherServices
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.app.LauncherPreferences
import net.blockhost.trestle.app.FolderPreferences
import net.blockhost.trestle.app.LauncherUpdate
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.auth.inspectMinecraftSkin
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.AccountLoginRequest
import net.blockhost.trestle.auth.DeviceAuthorization
import net.blockhost.trestle.auth.CredentialProtection
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.SecretValue
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.domain.MemorySettings
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.runtime.JvmArgumentPolicy
import net.blockhost.trestle.runtime.LaunchEvent
import net.blockhost.trestle.runtime.LaunchOptions
import net.blockhost.trestle.runtime.LaunchTuningAdvisor
import net.blockhost.trestle.runtime.PreparedLaunch
import net.blockhost.trestle.runtime.RuntimePreparationProgress
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.GameDataInventory
import net.blockhost.trestle.instance.SavedServer
import net.blockhost.trestle.metadata.VersionReference
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceSearchRequest
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.ReleaseChannel
import okio.FileSystem
import okio.Path.Companion.toPath

data class CreateInstanceState(
    val visible: Boolean = false,
    val name: String = "",
    val group: String = "",
    val iconReference: String = "",
    val versionId: String = "",
    val modLoader: ModLoader = ModLoader.VANILLA,
    val loaderVersion: String? = null,
    val loaderVersions: List<String> = emptyList(),
    val isResolvingLoader: Boolean = false,
    val isSaving: Boolean = false,
    val preconfigureClientSettings: Boolean = true,
    val clientSettings: MinecraftClientSettings = MinecraftClientSettings(),
)

data class LaunchPlanSummary(
    val mainClass: String,
    val javaMajor: Int,
    val classpathEntries: Int,
    val nativeLibraries: Int,
    val workingDirectory: String,
    val authentication: String,
)

sealed interface LaunchStatus {
    data object NotChecked : LaunchStatus
    data object Checking : LaunchStatus
    data object Ready : LaunchStatus
    data class Blocked(val missingRequirements: List<String>) : LaunchStatus
    data class Unavailable(val reason: String) : LaunchStatus
    data object Starting : LaunchStatus
    data class Running(val processId: Long?) : LaunchStatus
    data class Failed(val message: String) : LaunchStatus
}

data class InstanceLaunchState(
    val instanceId: InstanceId? = null,
    val status: LaunchStatus = LaunchStatus.NotChecked,
)

enum class ResourceBrowserPresentation {
    PAGE,
    DIALOG,
}

data class ResourceBrowserState(
    val visible: Boolean = false,
    val presentation: ResourceBrowserPresentation = ResourceBrowserPresentation.DIALOG,
    val provider: ResourceProvider = ResourceProvider.MODRINTH,
    val type: ResourceType = ResourceType.MOD,
    val query: String = "",
    val gameVersionFilter: String = "",
    val loaderFilter: ModLoader? = null,
    val categoryFilter: String = "",
    val sort: ResourceSearchSort = ResourceSearchSort.RELEVANCE,
    val releaseChannels: Set<ReleaseChannel> = setOf(
        ReleaseChannel.RELEASE,
        ReleaseChannel.BETA,
        ReleaseChannel.ALPHA,
        ReleaseChannel.UNKNOWN,
    ),
    val projects: List<ResourceProject> = emptyList(),
    val totalProjects: Int = 0,
    val selectedProjectId: String? = null,
    val versions: List<ResourceVersion> = emptyList(),
    val selectedVersionId: String? = null,
    val selectedOptionalDependencies: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val isLoadingVersions: Boolean = false,
    val isInstalling: Boolean = false,
    val error: String? = null,
    val curseForgeAvailable: Boolean = false,
) {
    val selectedProject: ResourceProject?
        get() = projects.firstOrNull { it.id == selectedProjectId }

    val selectedVersion: ResourceVersion?
        get() = versions.firstOrNull { it.id == selectedVersionId }
}

data class OperationStatus(
    val title: String,
    val detail: String? = null,
    val completed: Long? = null,
    val total: Long? = null,
    val completedItems: Int? = null,
    val totalItems: Int? = null,
    val cancellable: Boolean = false,
    val cancelLabel: String = "Pause",
    val instanceId: InstanceId? = null,
)

data class LocalFileImportState(
    val visible: Boolean = false,
    val fileName: String = "",
    val bytes: ByteArray = byteArrayOf(),
    val selectedType: ResourceType? = null,
    val targetInstanceId: InstanceId? = null,
    val sourceOrigin: String? = null,
) {
    val extension: String get() = fileName.substringAfterLast('.', "").lowercase()
    val allowedTypes: List<ResourceType>
        get() = when (extension) {
            "jar" -> listOf(ResourceType.MOD)
            "mrpack" -> listOf(ResourceType.MODPACK)
            "zip" -> listOf(ResourceType.RESOURCE_PACK, ResourceType.SHADER_PACK, ResourceType.MODPACK)
            else -> emptyList()
        }

    override fun equals(other: Any?): Boolean =
        other is LocalFileImportState &&
            visible == other.visible &&
            fileName == other.fileName &&
            bytes.contentEquals(other.bytes) &&
            selectedType == other.selectedType &&
            targetInstanceId == other.targetInstanceId &&
            sourceOrigin == other.sourceOrigin

    override fun hashCode(): Int {
        var result = visible.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + (selectedType?.hashCode() ?: 0)
        result = 31 * result + (targetInstanceId?.hashCode() ?: 0)
        result = 31 * result + (sourceOrigin?.hashCode() ?: 0)
        return result
    }
}

enum class ErrorRecoveryAction {
    INITIALIZE,
    REFRESH_VERSIONS,
    RETRY_INSTALLATION,
}

enum class InstanceRemovalMode {
    LIBRARY_ONLY,
    MOVE_TO_TRASH,
}

data class InstanceSettingsState(
    val visible: Boolean = false,
    val instanceId: InstanceId? = null,
    val name: String = "",
    val group: String = "",
    val iconReference: String = "",
    val pendingIcon: PendingInstanceIcon? = null,
    val minecraftVersionId: String = "",
    val modLoader: ModLoader = ModLoader.VANILLA,
    val minimumMemoryMiB: String = "",
    val maximumMemoryMiB: String = "",
    val jvmArguments: String = "",
    val gameArguments: String = "",
    val javaExecutable: String = "",
    val environmentVariables: String = "",
    val preLaunchCommand: String = "",
    val wrapperCommand: String = "",
    val postExitCommand: String = "",
    val accountProfileId: String? = null,
    val clientSettings: MinecraftClientSettings? = null,
    val isLoadingClientSettings: Boolean = false,
    val clientSettingsError: String? = null,
    val recommendation: String? = null,
    val warnings: List<String> = emptyList(),
    val isSaving: Boolean = false,
)

class PendingInstanceIcon(fileName: String, bytes: ByteArray) {
    val fileName: String = fileName
    val bytes: ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean =
        other is PendingInstanceIcon && fileName == other.fileName && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * fileName.hashCode() + bytes.contentHashCode()
}

data class ServerEditorState(
    val visible: Boolean = false,
    val key: String? = null,
    val name: String = "",
    val address: String = "",
    val acceptTextures: Boolean? = null,
    val isSaving: Boolean = false,
)

data class AccountLoginState(
    val visible: Boolean = false,
    val method: AccountAuthenticationMethod = AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
    val bedrockGameVersion: String = "",
    val email: String = "",
    val password: SensitiveText = SensitiveText(),
    val importedSecret: SensitiveText = SensitiveText(),
    val offlineUsername: String = "",
    val authorization: DeviceAuthorization? = null,
    val isWaiting: Boolean = false,
) {
    val edition: MinecraftEdition get() = method.edition
}

data class SkinEditorState(
    val visible: Boolean = false,
    val profileId: String? = null,
    val name: String = "",
    val variant: SkinVariant = SkinVariant.CLASSIC,
    val texture: ByteArray? = null,
    val sourceFileName: String? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
)

data class SkinStudioState(
    val visible: Boolean = false,
    val selectedProfileId: String? = null,
    val editor: SkinEditorState = SkinEditorState(),
)

class SensitiveText(private val raw: String = "") {
    fun reveal(): String = raw
    fun isBlank(): Boolean = raw.isBlank()

    override fun toString(): String = "[REDACTED]"
    override fun equals(other: Any?): Boolean = other is SensitiveText && raw == other.raw
    override fun hashCode(): Int = raw.hashCode()
}

data class LauncherUiState(
    val instances: List<GameInstance> = emptyList(),
    val versions: List<VersionReference> = emptyList(),
    val selectedId: InstanceId? = null,
    val isInitializing: Boolean = true,
    val isLoadingVersions: Boolean = false,
    val error: String? = null,
    val errorRecovery: ErrorRecoveryAction? = null,
    val notice: String? = null,
    val create: CreateInstanceState = CreateInstanceState(),
    val launchPlan: LaunchPlanSummary? = null,
    val resourceBrowser: ResourceBrowserState = ResourceBrowserState(),
    val launch: InstanceLaunchState = InstanceLaunchState(),
    val activeLaunch: InstanceLaunchState? = null,
    val operation: OperationStatus? = null,
    val instanceSettings: InstanceSettingsState = InstanceSettingsState(),
    val accounts: List<ManagedAccount> = emptyList(),
    val accountLogin: AccountLoginState = AccountLoginState(),
    val savedSkins: List<SavedSkin> = emptyList(),
    val accountSkinTextures: Map<String, ByteArray> = emptyMap(),
    val skinStudio: SkinStudioState = SkinStudioState(),
    val logs: List<LogEntry> = emptyList(),
    val credentialProtection: CredentialProtection? = null,
    val pendingInstanceRemovalId: InstanceId? = null,
    val instanceRemovalMode: InstanceRemovalMode = InstanceRemovalMode.LIBRARY_ONLY,
    val supportedMinecraftVersions: Set<String>? = null,
    val supportedModLoaders: Set<ModLoader>? = null,
    val removedInstanceUndo: GameInstance? = null,
    val localFileImport: LocalFileImportState = LocalFileImportState(),
    val installedContent: List<InstalledContent> = emptyList(),
    val installedContentUpdates: Map<String, ResourceVersion> = emptyMap(),
    val isLoadingInstalledContent: Boolean = false,
    val isCheckingInstalledContentUpdates: Boolean = false,
    val gameData: GameDataInventory = GameDataInventory(),
    val isLoadingGameData: Boolean = false,
    val serverEditor: ServerEditorState = ServerEditorState(),
    val supportsCustomJava: Boolean = false,
    val supportsLaunchCommands: Boolean = false,
    val gameLogLines: List<String> = emptyList(),
    val selectedInstanceLogKey: String? = null,
    val selectedInstanceLogText: String = "",
    val isLoadingInstanceLog: Boolean = false,
    val lastCrashReport: String? = null,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val launcherPreferences: LauncherPreferences = LauncherPreferences(),
    val defaultFolders: FolderPreferences = FolderPreferences(),
    val availableUpdate: LauncherUpdate? = null,
    val isCheckingForUpdate: Boolean = false,
    val pendingWorldDeletionKey: String? = null,
) {
    val selectedInstance: GameInstance?
        get() = instances.firstOrNull { it.id == selectedId } ?: instances.firstOrNull()

    val activeInstance: GameInstance?
        get() = activeLaunch?.instanceId?.let { id -> instances.firstOrNull { it.id == id } }
}

class LauncherViewModel(
    private val services: LauncherServices,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : LauncherUiActions {
    private val initialPreferences = services.preferences.read()
    private val mutableState = MutableStateFlow(
        LauncherUiState(
            credentialProtection = services.credentialStore.protection,
            supportedMinecraftVersions = services.runtime.capabilities.supportedMinecraftVersions,
            supportedModLoaders = services.runtime.capabilities.supportedModLoaders,
            supportsLaunchCommands = services.runtime.capabilities.supportsLaunchCommands,
            supportsCustomJava = services.runtime.capabilities.supportsCustomJava,
            themePreference = initialPreferences.theme,
            launcherPreferences = initialPreferences,
            defaultFolders = FolderPreferences(
                instances = services.directories.instances.toString(),
                runtimes = services.directories.runtimes.toString(),
                skins = services.directories.root.resolve("skins").toString(),
                downloads = services.directories.exports.toString(),
            ),
        ),
    )
    private var installJob: Job? = null
    private var resourceJob: Job? = null
    private var resourceSearchJob: Job? = null
    private var installedContentJob: Job? = null
    private var gameDataJob: Job? = null
    private var accountLoginJob: Job? = null
    private var launchCheckJob: Job? = null
    private var launchJob: Job? = null
    private val initialInitialization = CompletableDeferred<Unit>()
    private var cachedLaunch: Pair<GameInstance, PreparedLaunch>? = null
    private val loadedSkinUrls = mutableMapOf<String, String>()
    val state: StateFlow<LauncherUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            services.repository.instances.collectLatest { instances ->
                mutableState.update { state ->
                    val selected = state.selectedId?.takeIf { id -> instances.any { it.id == id } }
                        ?: instances.firstOrNull()?.id
                    state.copy(instances = instances, selectedId = selected)
                }
            }
        }
        scope.launch {
            services.accounts.accounts.collectLatest { accounts ->
                mutableState.update { it.copy(accounts = accounts) }
                loadAccountSkinTextures(accounts)
            }
        }
        scope.launch {
            services.skinLibrary.skins.collectLatest { skins ->
                mutableState.update { state ->
                    val selection = state.skinStudio.selectedProfileId?.takeIf { selected ->
                        skins.any { it.profile.id == selected }
                    } ?: skins.firstOrNull()?.profile?.id
                    state.copy(
                        savedSkins = skins,
                        skinStudio = state.skinStudio.copy(selectedProfileId = selection),
                    )
                }
            }
        }
        scope.launch {
            services.logger.entries.collectLatest { logs ->
                mutableState.update { it.copy(logs = logs) }
            }
        }
        initialize()
    }

    fun initialize() {
        scope.launch {
            mutableState.update {
                it.copy(
                    isInitializing = true,
                    error = null,
                    operation = OperationStatus("Loading launcher data"),
                )
            }
            val initialized = runCatching {
                services.repository.initialize()
                services.accounts.initialize()
                services.skinLibrary.initialize()
            }
                .onFailure { showError(it, ErrorRecoveryAction.INITIALIZE) }
            val instances = services.repository.instances.value
            mutableState.update { state ->
                val selected = state.selectedId?.takeIf { id -> instances.any { it.id == id } }
                    ?: instances.firstOrNull()?.id
                state.copy(instances = instances, selectedId = selected)
            }
            mutableState.update { it.copy(isInitializing = false, operation = null) }
            initialInitialization.complete(Unit)
            if (initialized.isSuccess) {
                checkLaunchReadiness(mutableState.value.selectedInstance)
                refreshInstalledContent()
                refreshGameData()
            }
            refreshVersions()
        }
    }

    override fun refreshVersions() {
        scope.launch {
            mutableState.update {
                it.copy(
                    isLoadingVersions = true,
                    error = null,
                    operation = OperationStatus("Refreshing Minecraft versions"),
                )
            }
            try {
                val manifest = services.metadataClient.fetchVersionManifest()
                val supported = services.runtime.capabilities.supportedMinecraftVersions
                val versions = manifest.versions.filter {
                    it.type in setOf("release", "snapshot", "old_beta", "old_alpha") &&
                        (supported == null || it.id in supported)
                }
                val defaultVersion = versions.firstOrNull { it.id == manifest.latest.release }?.id
                    ?: versions.firstOrNull()?.id.orEmpty()
                mutableState.update { state ->
                    state.copy(
                        versions = versions,
                        isLoadingVersions = false,
                        create = state.create.copy(
                            versionId = state.create.versionId.ifBlank { defaultVersion },
                        ),
                        operation = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isLoadingVersions = false, operation = null) }
                showError(error, ErrorRecoveryAction.REFRESH_VERSIONS)
            }
        }
    }

    override fun selectInstance(id: InstanceId) {
        launchCheckJob?.cancel()
        cachedLaunch = null
        mutableState.update {
            it.copy(
                selectedId = id,
                notice = null,
                launchPlan = null,
                launch = InstanceLaunchState(id),
                installedContent = emptyList(),
                installedContentUpdates = emptyMap(),
                gameData = GameDataInventory(),
                selectedInstanceLogKey = null,
                selectedInstanceLogText = "",
                isLoadingInstanceLog = false,
            )
        }
        checkLaunchReadiness(mutableState.value.selectedInstance)
        refreshInstalledContent()
        refreshGameData()
    }

    override fun toggleSelectedInstancePinned() {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            runCatching { services.repository.update(instance.copy(pinned = !instance.pinned)) }
                .onSuccess { updated ->
                    mutableState.update {
                        it.copy(
                            selectedId = updated.id,
                            notice = if (updated.pinned) {
                                "${updated.displayName} was pinned to the top."
                            } else {
                                "${updated.displayName} was unpinned."
                            },
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    override fun cloneSelectedInstance() {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            mutableState.update {
                it.copy(operation = OperationStatus("Cloning instance", instance.displayName))
            }
            runCatching {
                services.repository.clone(instance.id, "${instance.displayName} Copy")
            }.onSuccess { clone ->
                mutableState.update {
                    it.copy(
                        selectedId = clone.id,
                        operation = null,
                        notice = "${clone.displayName} was created.",
                    )
                }
                checkLaunchReadiness(clone)
                refreshInstalledContent()
            }.onFailure { error ->
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun exportSelectedInstance() {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            mutableState.update {
                it.copy(operation = OperationStatus("Exporting instance", instance.displayName))
            }
            val fileName = instance.displayName.lowercase()
                .replace(Regex("[^a-z0-9._-]+"), "-")
                .trim('-')
                .ifBlank { instance.id.value }
            runCatching {
                services.instanceExporter.export(
                    instance,
                    services.directories.exports / "$fileName.zip",
                )
            }.onSuccess { path ->
                mutableState.update {
                    it.copy(
                        operation = null,
                        notice = "Exported ${instance.displayName} to $path.",
                    )
                }
            }.onFailure { error ->
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun openCreate() {
        val defaultVersion = mutableState.value.create.versionId.ifBlank {
            mutableState.value.versions.firstOrNull()?.id.orEmpty()
        }
        mutableState.update {
            it.copy(
                create = CreateInstanceState(
                    visible = true,
                    versionId = defaultVersion,
                    modLoader = supportedLoaders().firstOrNull() ?: ModLoader.VANILLA,
                ),
                error = null,
            )
        }
    }

    override fun closeCreate() {
        mutableState.update { it.copy(create = CreateInstanceState()) }
    }

    override fun setCreateName(value: String) {
        mutableState.update { it.copy(create = it.create.copy(name = value)) }
    }

    override fun setCreateGroup(value: String) {
        mutableState.update { it.copy(create = it.create.copy(group = value)) }
    }

    override fun setCreateIconReference(value: String) {
        mutableState.update { it.copy(create = it.create.copy(iconReference = value)) }
    }

    override fun setCreateVersion(value: String) {
        val supported = services.runtime.capabilities.supportedMinecraftVersions
        if (supported != null && value !in supported) return
        mutableState.update { it.copy(create = it.create.copy(versionId = value, loaderVersion = null)) }
        loadCreateLoaderVersions(mutableState.value.create.modLoader)
    }

    override fun setCreateLoader(value: ModLoader) {
        if (value !in supportedLoaders()) return
        mutableState.update {
            it.copy(
                create = it.create.copy(
                    modLoader = value,
                    loaderVersion = null,
                    loaderVersions = emptyList(),
                    isResolvingLoader = false,
                ),
            )
        }
        loadCreateLoaderVersions(value)
    }

    override fun setCreateLoaderVersion(value: String) {
        mutableState.update { it.copy(create = it.create.copy(loaderVersion = value)) }
    }

    override fun setCreateClientPreconfiguration(value: Boolean) {
        mutableState.update { it.copy(create = it.create.copy(preconfigureClientSettings = value)) }
    }

    override fun setCreateClientSettings(value: MinecraftClientSettings) {
        mutableState.update { it.copy(create = it.create.copy(clientSettings = value)) }
    }

    override fun createInstance() {
        val form = mutableState.value.create
        if (form.name.isBlank() || form.versionId.isBlank()) return
        val supportedVersions = services.runtime.capabilities.supportedMinecraftVersions
        if (supportedVersions != null && form.versionId !in supportedVersions) return
        if (form.modLoader !in supportedLoaders()) return
        scope.launch {
            mutableState.update { it.copy(create = form.copy(isSaving = true), error = null) }
            try {
                val metadata = services.metadataClient.resolveVersion(form.versionId)
                val instance = services.repository.create(
                    CreateInstanceRequest(
                        displayName = form.name,
                        minecraftVersionId = form.versionId,
                        modLoader = form.modLoader,
                        loaderVersion = form.loaderVersion,
                        requiredJavaMajor = metadata.javaVersion?.majorVersion ?: 8,
                        memory = LaunchTuningAdvisor.recommendMemory(form.modLoader, services.systemProfile),
                        iconReference = form.iconReference.trim().ifBlank { null },
                        clientSettings = form.clientSettings.takeIf { form.preconfigureClientSettings },
                    ),
                )
                val configured = if (form.group.isBlank()) instance else services.repository.update(
                    instance.copy(group = form.group.trim()),
                )
                mutableState.update {
                    it.copy(
                        selectedId = configured.id,
                        create = CreateInstanceState(),
                        notice = "Instance created. Install its game files when you are ready.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(create = form.copy(isSaving = false)) }
                showError(error)
            }
        }
    }

    override fun importRemoteModpack(url: String) {
        val source = url.trim()
        if (source.isBlank() || resourceJob?.isActive == true) return
        val requestedIdentity = mutableState.value.create
        mutableState.update {
            it.copy(
                create = CreateInstanceState(),
                operation = OperationStatus("Importing modpack", source, cancellable = true, cancelLabel = "Cancel"),
                error = null,
            )
        }
        resourceJob = scope.launch {
            try {
                val downloadUrl = if (source.startsWith("curseforge://", ignoreCase = true)) {
                    val parameters = source.substringAfter('?', "").split('&').mapNotNull { part ->
                        val key = part.substringBefore('=', "").lowercase()
                        val value = part.substringAfter('=', "")
                        if (key.isBlank() || value.isBlank()) null else key to value
                    }.toMap()
                    val projectId = parameters["addonid"] ?: parameters["projectid"]
                        ?: throw IllegalArgumentException("The CurseForge link has no project ID.")
                    val fileId = parameters["fileid"]
                        ?: throw IllegalArgumentException("The CurseForge link has no file ID.")
                    services.resourcePlatforms.platform(ResourceProvider.CURSEFORGE)
                        .version(projectId, fileId).primaryFile?.url
                        ?: throw IllegalArgumentException("The CurseForge file blocks third-party downloads.")
                } else {
                    require(source.startsWith("https://") || source.startsWith("http://")) {
                        "Enter an HTTP, HTTPS, or curseforge:// URL."
                    }
                    source
                }
                val (fileName, bytes) = services.downloadImport(downloadUrl)
                val imported = services.modpackInstaller.installLocal(fileName, bytes, ::updateResourceProgress)
                val configured = services.repository.update(
                    imported.copy(
                        displayName = requestedIdentity.name.trim().ifBlank { imported.displayName },
                        group = requestedIdentity.group.trim().ifBlank { null },
                        iconReference = requestedIdentity.iconReference.trim().ifBlank { imported.iconReference },
                    ),
                )
                mutableState.update {
                    it.copy(
                        selectedId = configured.id,
                        operation = null,
                        notice = "${configured.displayName} was imported.",
                    )
                }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = "Modpack import cancelled.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                resourceJob = null
            }
        }
    }

    override fun importFtbAppInstances() {
        if (resourceJob?.isActive == true) return
        val path = mutableState.value.launcherPreferences.ftbAppInstancesPath.trim()
        val requestedGroup = mutableState.value.create.group.trim()
        if (path.isBlank()) {
            showError(IllegalArgumentException("Set the FTB App instances folder in Settings > Services first."))
            return
        }
        mutableState.update {
            it.copy(
                create = CreateInstanceState(),
                operation = OperationStatus("Importing FTB App instances", path, cancellable = true, cancelLabel = "Cancel"),
                error = null,
            )
        }
        resourceJob = scope.launch {
            try {
                val imported = services.modpackInstaller.importFtbAppInstances(path)
                val configured = if (requestedGroup.isBlank()) imported else imported.map { instance ->
                    services.repository.update(instance.copy(group = requestedGroup))
                }
                mutableState.update {
                    it.copy(
                        selectedId = configured.lastOrNull()?.id ?: it.selectedId,
                        operation = null,
                        notice = "Imported ${configured.size} FTB App ${if (configured.size == 1) "instance" else "instances"}.",
                    )
                }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = "FTB App import cancelled.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                resourceJob = null
            }
        }
    }

    override fun installSelected() {
        if (installJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val resuming = instance.installationState is InstallationState.Interrupted
        installJob = scope.launch {
            mutableState.update {
                it.copy(
                    error = null,
                    notice = null,
                    launchPlan = null,
                    operation = OperationStatus(
                        if (resuming) "Preparing installation resume" else "Preparing installation",
                        instance.displayName,
                        cancellable = true,
                        instanceId = instance.id,
                    ),
                )
            }
            try {
                val installed = services.installer.install(instance) { progress ->
                    mutableState.update {
                        it.copy(
                            operation = OperationStatus(
                                title = if (progress.isFinalizing) {
                                    "Finalizing ${instance.displayName}"
                                } else if (resuming) {
                                    "Resuming ${instance.displayName}"
                                } else {
                                    "Installing ${instance.displayName}"
                                },
                                detail = progress.activeLabel,
                                completed = progress.completedBytes,
                                total = progress.totalBytes,
                                completedItems = progress.completedFiles,
                                totalItems = progress.totalFiles,
                                cancellable = !progress.isFinalizing,
                                instanceId = instance.id,
                            ),
                        )
                    }
                }
                mutableState.update { it.copy(notice = "Installation is complete.", operation = null) }
                checkLaunchReadiness(installed)
            } catch (_: CancellationException) {
                mutableState.update {
                    it.copy(
                        notice = "Installation paused. Resume it when you are ready.",
                        operation = null,
                    )
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error, ErrorRecoveryAction.RETRY_INSTALLATION)
            } finally {
                installJob = null
            }
        }
    }

    override fun cancelInstall() {
        val activeJob = installJob
        if (activeJob?.isActive == true) {
            activeJob.cancel()
            return
        }
        val instance = mutableState.value.selectedInstance ?: return
        val progress = instance.installationState as? InstallationState.Installing ?: return
        scope.launch {
            services.repository.update(
                instance.copy(
                    installationState = InstallationState.Interrupted(
                        completedBytes = progress.completedBytes,
                        totalBytes = progress.totalBytes,
                        completedFiles = progress.completedFiles,
                        totalFiles = progress.totalFiles,
                    ),
                ),
            )
            mutableState.update {
                it.copy(
                    notice = "Installation paused. Resume it when you are ready.",
                    operation = null,
                )
            }
        }
    }

    override fun cancelActiveOperation() {
        if (resourceJob?.isActive == true) {
            resourceJob?.cancel()
        } else {
            cancelInstall()
        }
    }

    override fun inspectLaunchPlan() {
        val instance = mutableState.value.selectedInstance ?: return
        if (instance.installationState !is InstallationState.Installed) return
        try {
            val installed = services.installer.readInstalledVersion(instance)
            val activeAccount = instance.accountProfileId?.let { profileId ->
                mutableState.value.accounts.firstOrNull { it.profile.profileId == profileId }
            } ?: mutableState.value.accounts.firstOrNull { it.isActive }
            mutableState.update {
                it.copy(
                    launchPlan = LaunchPlanSummary(
                        mainClass = installed.metadata.mainClass,
                        javaMajor = installed.requiredJavaMajor,
                        classpathEntries = installed.libraries.count { !it.native } + 1,
                        nativeLibraries = installed.libraries.count { it.native },
                        workingDirectory = "${instance.instanceDirectory}/game",
                        authentication = when {
                            activeAccount == null -> "Sign-in required"
                            activeAccount.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE ->
                                "${activeAccount.profile.playerName} · Offline"
                            activeAccount.isAuthenticated && activeAccount.profile.edition == MinecraftEdition.JAVA ->
                                activeAccount.profile.playerName
                            activeAccount.profile.edition == MinecraftEdition.BEDROCK ->
                                "Select a Java account"
                            else -> "${activeAccount.profile.playerName} · Sign-in expired"
                        },
                    ),
                    notice = null,
                    error = null,
                )
            }
        } catch (error: Exception) {
            showError(error)
        }
    }

    override fun launchSelected() = launchSelected(LaunchOptions())

    private fun launchSelected(options: LaunchOptions) {
        if (launchJob?.isActive == true) return
        if (!services.runtime.capabilities.canLaunch) return
        val instance = mutableState.value.selectedInstance ?: return
        if (instance.installationState !is InstallationState.Installed) return
        launchCheckJob?.cancel()
        launchCheckJob = null
        launchJob = scope.launch {
            var startedAtEpochMillis: Long? = null
            mutableState.update {
                it.copy(
                    error = null,
                    errorRecovery = null,
                    notice = null,
                    gameLogLines = emptyList(),
                    lastCrashReport = null,
                )
            }
            updateLaunch(instance.id, LaunchStatus.Starting, activeEvent = true)
            try {
                val prepared = cachedLaunch
                    ?.takeIf { (cachedInstance) -> cachedInstance == instance && options == LaunchOptions() }
                    ?.second
                    ?: services.runtime.prepare(instance, options, ::updateRuntimePreparation)
                if (prepared.missingRequirements.isNotEmpty()) {
                    cachedLaunch = null
                    updateLaunch(instance.id, LaunchStatus.Blocked(prepared.missingRequirements), activeEvent = true)
                    return@launch
                }
                if (options == LaunchOptions()) cachedLaunch = instance to prepared
                services.runtime.launch(prepared).collect { event ->
                    when (event) {
                        is LaunchEvent.Started -> {
                            startedAtEpochMillis = services.clock.nowMillis()
                            try {
                                services.repository.get(instance.id)?.let { current ->
                                    services.repository.update(
                                        current.copy(
                                            lastLaunchAtEpochMillis = startedAtEpochMillis,
                                            launchCount = current.launchCount + 1,
                                        ),
                                    )
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                services.logger.warn(
                                    "instances",
                                    "Could not save the last launch time",
                                    error,
                                    mapOf("instanceId" to instance.id.value),
                                )
                            }
                            updateLaunch(instance.id, LaunchStatus.Running(event.processId), activeEvent = true)
                        }
                        is LaunchEvent.Log -> mutableState.update { state ->
                            state.copy(gameLogLines = (state.gameLogLines + event.line).takeLast(MAX_GAME_LOG_LINES))
                        }
                        is LaunchEvent.Exited -> {
                            startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                            startedAtEpochMillis = null
                            if (event.exitCode == 0) {
                                updateLaunch(
                                    instance.id,
                                    LaunchStatus.Ready,
                                    "Minecraft closed.",
                                    activeEvent = true,
                                )
                            } else {
                                mutableState.update {
                                    it.copy(lastCrashReport = findLatestCrashReport(instance))
                                }
                                updateLaunch(
                                    instance.id,
                                    LaunchStatus.Failed("Minecraft exited with code ${event.exitCode}."),
                                    activeEvent = true,
                                )
                            }
                        }
                        is LaunchEvent.Failed -> {
                            startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                            startedAtEpochMillis = null
                            updateLaunch(
                                instance.id,
                                LaunchStatus.Failed(event.message),
                                activeEvent = true,
                            )
                        }
                        LaunchEvent.Cancelled -> {
                            startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                            startedAtEpochMillis = null
                            updateLaunch(
                                instance.id,
                                LaunchStatus.Ready,
                                "Minecraft stopped.",
                                activeEvent = true,
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                updateLaunch(instance.id, LaunchStatus.Ready, "Minecraft stopped.", activeEvent = true)
            } catch (error: Exception) {
                startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                updateLaunch(
                    instance.id,
                    LaunchStatus.Failed(error.message ?: "Minecraft could not start."),
                    activeEvent = true,
                )
            } finally {
                mutableState.update { it.copy(operation = null) }
                refreshGameData()
                launchJob = null
            }
        }
    }

    override fun launchInstance(id: InstanceId) {
        scope.launch {
            initialInitialization.await()
            val instance = mutableState.value.instances.firstOrNull { it.id == id }
            if (instance == null) {
                mutableState.update { it.copy(error = "That instance is no longer in the library.") }
                return@launch
            }
            selectInstance(id)
            launchSelected()
        }
    }

    private suspend fun recordPlayTime(instanceId: InstanceId, startedAtEpochMillis: Long) {
        withContext(NonCancellable) {
            try {
                services.repository.get(instanceId)?.let { current ->
                    val elapsed = (services.clock.nowMillis() - startedAtEpochMillis).coerceAtLeast(0)
                    services.repository.update(
                        current.copy(playTimeMillis = current.playTimeMillis + elapsed),
                    )
                }
            } catch (error: Exception) {
                services.logger.warn(
                    "instances",
                    "Could not save instance play time",
                    error,
                    mapOf("instanceId" to instanceId.value),
                )
            }
        }
    }

    override fun stopLaunch() {
        launchJob?.cancel()
    }

    override fun clearGameLog() {
        mutableState.update { it.copy(gameLogLines = emptyList()) }
    }

    override fun openResourceBrowser(
        type: ResourceType,
        presentation: ResourceBrowserPresentation,
    ) {
        val curseForgeAvailable = services.resourcePlatforms.platform(ResourceProvider.CURSEFORGE).isAvailable
        mutableState.value = mutableState.value.copy(
            resourceBrowser = ResourceBrowserState(
                visible = true,
                presentation = presentation,
                type = type,
                curseForgeAvailable = curseForgeAvailable,
            ),
            error = null,
        )
        searchResources()
    }

    override fun closeResourceBrowser() {
        if (resourceJob?.isActive == true) return
        resourceSearchJob?.cancel()
        mutableState.value = mutableState.value.copy(resourceBrowser = ResourceBrowserState())
    }

    override fun setResourceProvider(provider: ResourceProvider) {
        resourceSearchJob?.cancel()
        val current = mutableState.value.resourceBrowser
        val platform = services.resourcePlatforms.platform(provider)
        mutableState.value = mutableState.value.copy(
            resourceBrowser = current.copy(
                provider = provider,
                projects = emptyList(),
                totalProjects = 0,
                selectedProjectId = null,
                versions = emptyList(),
                selectedVersionId = null,
                selectedOptionalDependencies = emptySet(),
                error = when {
                    !platform.isAvailable -> "${provider.label} is not available for third-party launcher access."
                    !platform.supports(current.type) -> "${provider.label} does not provide ${current.type.label.lowercase()}."
                    else -> null
                },
            ),
        )
        if (platform.isAvailable && platform.supports(current.type)) searchResources()
    }

    override fun setResourceType(type: ResourceType) {
        resourceSearchJob?.cancel()
        val current = mutableState.value.resourceBrowser
        val requestedPlatform = services.resourcePlatforms.platform(current.provider)
        val provider = if (requestedPlatform.supports(type)) current.provider else ResourceProvider.MODRINTH
        mutableState.value = mutableState.value.copy(
            resourceBrowser = current.copy(
                provider = provider,
                type = type,
                projects = emptyList(),
                totalProjects = 0,
                selectedProjectId = null,
                versions = emptyList(),
                selectedVersionId = null,
                selectedOptionalDependencies = emptySet(),
                error = null,
            ),
        )
        searchResources()
    }

    override fun setResourceQuery(value: String) {
        mutableState.value = mutableState.value.copy(
            resourceBrowser = mutableState.value.resourceBrowser.copy(query = value),
        )
    }

    override fun setResourceGameVersionFilter(value: String) {
        mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(gameVersionFilter = value)) }
    }

    override fun setResourceLoaderFilter(value: ModLoader?) {
        mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(loaderFilter = value)) }
    }

    override fun setResourceCategoryFilter(value: String) {
        mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(categoryFilter = value)) }
    }

    override fun setResourceSort(value: ResourceSearchSort) {
        mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(sort = value)) }
        searchResources()
    }

    override fun toggleResourceReleaseChannel(value: ReleaseChannel) {
        val browser = mutableState.value.resourceBrowser
        val channels = if (value in browser.releaseChannels) {
            browser.releaseChannels - value
        } else {
            browser.releaseChannels + value
        }
        if (channels.isNotEmpty()) {
            mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(releaseChannels = channels)) }
        }
    }

    override fun searchResources() {
        searchResources(append = false)
    }

    override fun loadMoreResources() {
        val browser = mutableState.value.resourceBrowser
        if (!browser.isSearching && browser.projects.size < browser.totalProjects) searchResources(append = true)
    }

    override fun selectResource(projectId: String) {
        val browser = mutableState.value.resourceBrowser
        val project = browser.projects.firstOrNull { it.id == projectId } ?: return
        resourceSearchJob?.cancel()
        resourceSearchJob = scope.launch {
            mutableState.value = mutableState.value.copy(
                resourceBrowser = browser.copy(
                    selectedProjectId = projectId,
                    versions = emptyList(),
                    selectedVersionId = null,
                    selectedOptionalDependencies = emptySet(),
                    isLoadingVersions = true,
                    error = null,
                ),
            )
            try {
                val instance = mutableState.value.selectedInstance
                val platform = services.resourcePlatforms.platform(project.provider)
                val detailedProject = platform.details(project)
                val versions = platform.versions(
                    project = detailedProject,
                    gameVersion = if (project.type == ResourceType.MODPACK) null else instance?.minecraftVersionId,
                    loader = if (project.type == ResourceType.MODPACK) null else instance?.modLoader,
                )
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        projects = mutableState.value.resourceBrowser.projects.map {
                            if (it.provider == detailedProject.provider && it.id == detailedProject.id) detailedProject else it
                        },
                        versions = versions,
                        selectedVersionId = versions.firstOrNull { it.channel == ReleaseChannel.RELEASE }?.id
                            ?: versions.firstOrNull()?.id,
                        isLoadingVersions = false,
                        error = if (versions.isEmpty()) "No compatible versions were found." else null,
                    ),
                )
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        isLoadingVersions = false,
                        error = error.message ?: "Versions could not be loaded.",
                    ),
                )
            }
        }
    }

    override fun clearResourceSelection() {
        resourceSearchJob?.cancel()
        val browser = mutableState.value.resourceBrowser
        mutableState.value = mutableState.value.copy(
            resourceBrowser = browser.copy(
                selectedProjectId = null,
                versions = emptyList(),
                selectedVersionId = null,
                selectedOptionalDependencies = emptySet(),
                isLoadingVersions = false,
                error = null,
            ),
        )
    }

    override fun selectResourceVersion(versionId: String) {
        mutableState.value = mutableState.value.copy(
            resourceBrowser = mutableState.value.resourceBrowser.copy(
                selectedVersionId = versionId,
                selectedOptionalDependencies = emptySet(),
            ),
        )
    }

    override fun toggleOptionalDependency(key: String) {
        val browser = mutableState.value.resourceBrowser
        val selected = if (key in browser.selectedOptionalDependencies) {
            browser.selectedOptionalDependencies - key
        } else {
            browser.selectedOptionalDependencies + key
        }
        mutableState.value = mutableState.value.copy(
            resourceBrowser = browser.copy(selectedOptionalDependencies = selected),
        )
    }

    override fun installSelectedResource() {
        if (resourceJob?.isActive == true) return
        val browser = mutableState.value.resourceBrowser
        val project = browser.selectedProject ?: return
        val version = browser.selectedVersion ?: return
        val instance = mutableState.value.selectedInstance
        if (project.type != ResourceType.MODPACK && instance?.installationState !is InstallationState.Installed) {
            mutableState.value = mutableState.value.copy(
                resourceBrowser = browser.copy(error = "Install the selected instance before adding resources."),
            )
            return
        }
        resourceJob = scope.launch {
            mutableState.value = mutableState.value.copy(
                resourceBrowser = browser.copy(isInstalling = true, error = null),
                operation = OperationStatus(
                    title = if (project.type == ResourceType.MODPACK) "Installing modpack" else "Installing ${project.type.label.lowercase()}",
                    detail = project.name,
                    cancellable = true,
                    cancelLabel = "Cancel",
                    instanceId = instance?.id,
                ),
            )
            try {
                val notice = if (project.type == ResourceType.MODPACK) {
                    val created = services.modpackInstaller.install(project, version, ::updateResourceProgress)
                    mutableState.value = mutableState.value.copy(selectedId = created.id)
                    "${created.displayName} was added to the library."
                } else {
                    val selectedInstance = requireNotNull(instance)
                    val summary = services.resourceInstaller.install(
                        selectedInstance,
                        project,
                        version,
                        browser.selectedOptionalDependencies,
                        ::updateResourceProgress,
                    )
                    val dependencyText = if (summary.dependencyCount == 0) "" else {
                        " with ${summary.dependencyCount} ${if (summary.dependencyCount == 1) "dependency" else "dependencies"}"
                    }
                    "${project.name} was installed$dependencyText."
                }
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = if (browser.presentation == ResourceBrowserPresentation.PAGE) {
                        browser.copy(isInstalling = false, error = null)
                    } else {
                        ResourceBrowserState()
                    },
                    notice = notice,
                    operation = null,
                )
                refreshInstalledContent()
            } catch (_: CancellationException) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(isInstalling = false),
                    operation = null,
                    notice = if (project.type == ResourceType.MODPACK) {
                        "Modpack installation cancelled. No instance was added."
                    } else {
                        "Resource installation cancelled."
                    },
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        isInstalling = false,
                        error = error.message ?: "The resource could not be installed.",
                    ),
                    operation = null,
                )
            } finally {
                resourceJob = null
            }
        }
    }

    override fun refreshInstalledContent() {
        installedContentJob?.cancel()
        val instance = mutableState.value.selectedInstance
        if (instance?.installationState !is InstallationState.Installed) {
            mutableState.update {
                it.copy(
                    installedContent = emptyList(),
                    installedContentUpdates = emptyMap(),
                    isLoadingInstalledContent = false,
                    isCheckingInstalledContentUpdates = false,
                )
            }
            return
        }
        installedContentJob = scope.launch {
            mutableState.update { it.copy(isLoadingInstalledContent = true) }
            try {
                val content = services.resourceInstaller.installedContent(instance)
                mutableState.update { state ->
                    if (state.selectedId != instance.id) state
                    else state.copy(
                        installedContent = content,
                        installedContentUpdates = state.installedContentUpdates.filterKeys { key ->
                            content.any { it.key == key }
                        },
                        isLoadingInstalledContent = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isLoadingInstalledContent = false) }
                showError(error)
            }
        }
    }

    override fun checkInstalledContentUpdates() {
        if (installedContentJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.filter { it.direct && it.isTracked }
        installedContentJob = scope.launch {
            mutableState.update { it.copy(isCheckingInstalledContentUpdates = true) }
            try {
                val updates = buildMap {
                    content.forEach { installed ->
                        services.resourceInstaller.latestCompatibleVersion(instance, installed)?.let { version ->
                            put(installed.key, version)
                        }
                    }
                }
                mutableState.update { state ->
                    if (state.selectedId != instance.id) state
                    else state.copy(
                        installedContentUpdates = updates,
                        isCheckingInstalledContentUpdates = false,
                        notice = when (updates.size) {
                            0 -> "Installed content is up to date."
                            1 -> "One content update is available."
                            else -> "${updates.size} content updates are available."
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isCheckingInstalledContentUpdates = false) }
                showError(error)
            }
        }
    }

    override fun toggleInstalledContent(key: String) {
        if (installedContentJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.firstOrNull { it.key == key } ?: return
        if (!content.canManage) return
        installedContentJob = scope.launch {
            try {
                val enabled = !content.enabled
                if (services.resourceInstaller.setEnabled(instance, content, enabled)) {
                    mutableState.update {
                        it.copy(notice = "${content.name} was ${if (enabled) "enabled" else "disabled"}.")
                    }
                    val refreshed = services.resourceInstaller.installedContent(instance)
                    mutableState.update { it.copy(installedContent = refreshed) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    override fun updateInstalledContent(key: String) {
        if (resourceJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.firstOrNull { it.key == key } ?: return
        val version = mutableState.value.installedContentUpdates[key] ?: return
        resourceJob = scope.launch {
            mutableState.update {
                it.copy(
                    operation = OperationStatus(
                        title = "Updating ${content.name}",
                        detail = version.versionNumber,
                        cancellable = true,
                        cancelLabel = "Cancel",
                        instanceId = instance.id,
                    ),
                )
            }
            try {
                services.resourceInstaller.update(instance, content, version, ::updateResourceProgress)
                val refreshed = services.resourceInstaller.installedContent(instance)
                mutableState.update {
                    it.copy(
                        installedContent = refreshed,
                        installedContentUpdates = it.installedContentUpdates - key,
                        operation = null,
                        notice = "${content.name} was updated to ${version.versionNumber}.",
                    )
                }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = "Content update cancelled.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                resourceJob = null
            }
        }
    }

    override fun removeInstalledContent(key: String) {
        if (installedContentJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.firstOrNull { it.key == key } ?: return
        if (!content.canManage) return
        installedContentJob = scope.launch {
            try {
                if (services.resourceInstaller.uninstall(instance, content)) {
                    val refreshed = services.resourceInstaller.installedContent(instance)
                    mutableState.update {
                        it.copy(
                            installedContent = refreshed,
                            installedContentUpdates = it.installedContentUpdates - key,
                            notice = "${content.name} was removed.",
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    override fun refreshGameData() {
        gameDataJob?.cancel()
        val instance = mutableState.value.selectedInstance ?: run {
            mutableState.update { it.copy(gameData = GameDataInventory(), isLoadingGameData = false) }
            return
        }
        gameDataJob = scope.launch {
            mutableState.update { it.copy(isLoadingGameData = true) }
            try {
                val inventory = services.gameDataManager.inventory(instance)
                mutableState.update { state ->
                    if (state.selectedId != instance.id) state
                    else state.copy(gameData = inventory, isLoadingGameData = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isLoadingGameData = false) }
                showError(error)
            }
        }
    }

    override fun backupWorld(worldKey: String) = runGameDataOperation("Backing up world") { instance ->
        val backup = services.gameDataManager.backupWorld(instance, worldKey)
        "Created ${backup.fileName}."
    }

    override fun restoreWorldBackup(backupKey: String) = runGameDataOperation("Restoring world") { instance ->
        val world = services.gameDataManager.restoreWorldBackup(instance, backupKey)
        "Restored ${world.name}."
    }

    override fun importWorld(fileName: String, bytes: ByteArray) = runGameDataOperation("Importing world") { instance ->
        val world = services.gameDataManager.importWorld(instance, fileName, bytes)
        "Imported ${world.name}."
    }

    override fun copyWorld(worldKey: String) = runGameDataOperation("Copying world") { instance ->
        val world = services.gameDataManager.copyWorld(instance, worldKey)
        "Created ${world.name}."
    }

    override fun renameWorld(worldKey: String, newName: String) {
        if (newName.isBlank()) return
        runGameDataOperation("Renaming world") { instance ->
            val world = services.gameDataManager.renameWorld(instance, worldKey, newName)
            "Renamed world to ${world.name}."
        }
    }

    override fun resetWorldIcon(worldKey: String) = runGameDataOperation("Resetting world icon") { instance ->
        services.gameDataManager.resetWorldIcon(instance, worldKey)
        "Reset the world icon."
    }

    override fun launchWorld(worldKey: String) {
        if (mutableState.value.gameData.worlds.none { it.key == worldKey }) return
        launchSelected(LaunchOptions(additionalGameArguments = listOf("--quickPlaySingleplayer", worldKey)))
    }

    override fun deleteWorld(worldKey: String) {
        if (mutableState.value.gameData.worlds.none { it.key == worldKey }) return
        mutableState.update { it.copy(pendingWorldDeletionKey = worldKey) }
    }

    override fun cancelWorldDeletion() {
        mutableState.update { it.copy(pendingWorldDeletionKey = null) }
    }

    override fun confirmWorldDeletion() {
        val worldKey = mutableState.value.pendingWorldDeletionKey ?: return
        mutableState.update { it.copy(pendingWorldDeletionKey = null) }
        runGameDataOperation("Deleting world") { instance ->
            services.gameDataManager.deleteWorld(instance, worldKey)
            "Deleted $worldKey. Existing backups were kept."
        }
    }

    override fun deleteScreenshot(screenshotKey: String) = runGameDataOperation("Deleting screenshot") { instance ->
        services.gameDataManager.deleteScreenshot(instance, screenshotKey)
        "Deleted $screenshotKey."
    }

    override fun renameScreenshot(screenshotKey: String, newName: String) {
        if (newName.isBlank()) return
        runGameDataOperation("Renaming screenshot") { instance ->
            val screenshot = services.gameDataManager.renameScreenshot(instance, screenshotKey, newName.trim())
            "Renamed screenshot to ${screenshot.fileName}."
        }
    }

    override fun toggleDataPack(worldKey: String, dataPackKey: String) {
        val pack = mutableState.value.gameData.worlds.firstOrNull { it.key == worldKey }
            ?.dataPacks?.firstOrNull { it.key == dataPackKey } ?: return
        runGameDataOperation(if (pack.enabled) "Disabling data pack" else "Enabling data pack") { instance ->
            services.gameDataManager.setDataPackEnabled(instance, worldKey, dataPackKey, !pack.enabled)
            "${pack.fileName} was ${if (pack.enabled) "disabled" else "enabled"}."
        }
    }

    override fun openServerEditor(serverKey: String?) {
        val server = mutableState.value.gameData.servers.firstOrNull { it.key == serverKey }
        mutableState.update {
            it.copy(
                serverEditor = ServerEditorState(
                    visible = true,
                    key = server?.key,
                    name = server?.name.orEmpty(),
                    address = server?.address.orEmpty(),
                    acceptTextures = server?.acceptTextures,
                ),
            )
        }
    }

    override fun closeServerEditor() {
        mutableState.update { it.copy(serverEditor = ServerEditorState()) }
    }

    override fun setServerName(value: String) {
        mutableState.update { it.copy(serverEditor = it.serverEditor.copy(name = value)) }
    }

    override fun setServerAddress(value: String) {
        mutableState.update { it.copy(serverEditor = it.serverEditor.copy(address = value)) }
    }

    override fun setServerResourcePacks(value: String) {
        val acceptTextures = when (value) {
            "Always" -> true
            "Never" -> false
            else -> null
        }
        mutableState.update { it.copy(serverEditor = it.serverEditor.copy(acceptTextures = acceptTextures)) }
    }

    override fun saveServer() {
        val editor = mutableState.value.serverEditor
        if (!editor.visible || editor.name.isBlank() || editor.address.isBlank()) return
        val instance = mutableState.value.selectedInstance ?: return
        gameDataJob = scope.launch {
            mutableState.update { it.copy(serverEditor = editor.copy(isSaving = true)) }
            try {
                services.gameDataManager.upsertServer(
                    instance,
                    SavedServer(
                        editor.key.orEmpty(),
                        editor.name.trim(),
                        editor.address.trim(),
                        editor.acceptTextures,
                    ),
                )
                val inventory = services.gameDataManager.inventory(instance)
                mutableState.update {
                    it.copy(
                        gameData = inventory,
                        serverEditor = ServerEditorState(),
                        notice = "Saved ${editor.name.trim()} to the Minecraft server list.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(serverEditor = editor.copy(isSaving = false)) }
                showError(error)
            }
        }
    }

    override fun removeServer(serverKey: String) = runGameDataOperation("Removing server") { instance ->
        val server = mutableState.value.gameData.servers.firstOrNull { it.key == serverKey }
        services.gameDataManager.removeServer(instance, serverKey)
        "Removed ${server?.name ?: "server"}."
    }

    override fun moveServer(serverKey: String, offset: Int) = runGameDataOperation("Reordering servers") { instance ->
        services.gameDataManager.moveServer(instance, serverKey, offset)
        "Updated the server order."
    }

    override fun joinServer(serverKey: String) {
        val server = mutableState.value.gameData.servers.firstOrNull { it.key == serverKey } ?: return
        launchSelected(LaunchOptions(additionalGameArguments = listOf("--server", server.address)))
    }

    override fun selectInstanceLog(logKey: String) {
        val instance = mutableState.value.selectedInstance ?: return
        if (mutableState.value.gameData.logs.none { it.key == logKey }) return
        scope.launch {
            mutableState.update {
                it.copy(
                    selectedInstanceLogKey = logKey,
                    selectedInstanceLogText = "",
                    isLoadingInstanceLog = true,
                )
            }
            try {
                val text = services.gameDataManager.readLog(instance, logKey)
                mutableState.update { state ->
                    if (state.selectedId != instance.id || state.selectedInstanceLogKey != logKey) state
                    else state.copy(selectedInstanceLogText = text, isLoadingInstanceLog = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isLoadingInstanceLog = false) }
                showError(error)
            }
        }
    }

    override fun deleteInstanceLog(logKey: String) = runGameDataOperation("Deleting log") { instance ->
        services.gameDataManager.deleteLog(instance, logKey)
        mutableState.update { state ->
            if (state.selectedInstanceLogKey == logKey) {
                state.copy(selectedInstanceLogKey = null, selectedInstanceLogText = "")
            } else {
                state
            }
        }
        "Deleted ${logKey.substringAfterLast('/')}"
    }

    override fun saveInstanceNotes(value: String) {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            runCatching { services.repository.update(instance.copy(notes = value.trimEnd())) }
                .onSuccess { mutableState.update { it.copy(notice = "Saved notes for ${instance.displayName}.") } }
                .onFailure(::showError)
        }
    }

    private fun runGameDataOperation(
        title: String,
        operation: suspend (GameInstance) -> String,
    ) {
        if (gameDataJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        gameDataJob = scope.launch {
            mutableState.update { it.copy(operation = OperationStatus(title, instance.displayName)) }
            try {
                val notice = operation(instance)
                val inventory = services.gameDataManager.inventory(instance)
                mutableState.update { it.copy(gameData = inventory, operation = null, notice = notice) }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(operation = null) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                gameDataJob = null
            }
        }
    }

    override fun deleteSelected() {
        val id = mutableState.value.selectedInstance?.id ?: return
        if (mutableState.value.activeLaunch?.instanceId == id) {
            mutableState.update { it.copy(error = "Stop Minecraft before removing this instance.") }
            return
        }
        mutableState.update {
            it.copy(
                pendingInstanceRemovalId = id,
                instanceRemovalMode = InstanceRemovalMode.LIBRARY_ONLY,
            )
        }
    }

    override fun moveSelectedToTrash() {
        if (!supportsPathTrash) return
        val id = mutableState.value.selectedInstance?.id ?: return
        if (mutableState.value.activeLaunch?.instanceId == id) {
            mutableState.update { it.copy(error = "Stop Minecraft before moving this instance to Trash.") }
            return
        }
        mutableState.update {
            it.copy(
                pendingInstanceRemovalId = id,
                instanceRemovalMode = InstanceRemovalMode.MOVE_TO_TRASH,
            )
        }
    }

    override fun cancelInstanceRemoval() {
        mutableState.update { it.copy(pendingInstanceRemovalId = null) }
    }

    override fun confirmInstanceRemoval() {
        val id = mutableState.value.pendingInstanceRemovalId ?: return
        val removed = mutableState.value.instances.firstOrNull { it.id == id } ?: return
        val removalMode = mutableState.value.instanceRemovalMode
        scope.launch {
            try {
                if (removalMode == InstanceRemovalMode.MOVE_TO_TRASH) {
                    check(movePathToTrash(removed.instanceDirectory)) {
                        "The instance could not be moved to Trash. Its files were not changed."
                    }
                }
                services.repository.delete(id)
                mutableState.update {
                    it.copy(
                        selectedId = null,
                        pendingInstanceRemovalId = null,
                        notice = if (removalMode == InstanceRemovalMode.MOVE_TO_TRASH) {
                            "${removed.displayName} was moved to Trash."
                        } else {
                            "Instance removed from the library. Its game directory was kept."
                        },
                        launchPlan = null,
                        removedInstanceUndo = removed.takeIf {
                            removalMode == InstanceRemovalMode.LIBRARY_ONLY
                        },
                    )
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(pendingInstanceRemovalId = null) }
                showError(error)
            }
        }
    }

    override fun confirmInstanceDeletion() {
        val id = mutableState.value.pendingInstanceRemovalId ?: return
        val removed = mutableState.value.instances.firstOrNull { it.id == id } ?: return
        scope.launch {
            mutableState.update {
                it.copy(operation = OperationStatus("Deleting instance", removed.displayName))
            }
            try {
                services.repository.deleteWithFiles(id)
                mutableState.update {
                    it.copy(
                        selectedId = null,
                        pendingInstanceRemovalId = null,
                        operation = null,
                        notice = "${removed.displayName} and its files were deleted.",
                        launchPlan = null,
                        removedInstanceUndo = null,
                        installedContent = emptyList(),
                        installedContentUpdates = emptyMap(),
                    )
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(pendingInstanceRemovalId = null, operation = null) }
                showError(error)
            }
        }
    }

    override fun undoInstanceRemoval() {
        val instance = mutableState.value.removedInstanceUndo ?: return
        scope.launch {
            runCatching { services.repository.restore(instance) }
                .onSuccess {
                    mutableState.update { state ->
                        state.copy(
                            selectedId = instance.id,
                            removedInstanceUndo = null,
                            notice = "${instance.displayName} was restored to the library.",
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update { it.copy(removedInstanceUndo = null) }
                    showError(error)
                }
        }
    }

    override fun queueLocalFileImport(
        fileName: String,
        bytes: ByteArray,
        type: ResourceType?,
        sourceOrigin: String?,
    ) {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val allowedTypes = when (extension) {
            "jar" -> listOf(ResourceType.MOD)
            "mrpack" -> listOf(ResourceType.MODPACK)
            "zip" -> listOf(ResourceType.RESOURCE_PACK, ResourceType.SHADER_PACK, ResourceType.MODPACK)
            else -> emptyList()
        }
        if (allowedTypes.isEmpty()) {
            mutableState.update { it.copy(error = "Choose a .jar, .zip, or .mrpack file.") }
            return
        }
        if (bytes.isEmpty()) {
            mutableState.update { it.copy(error = "$fileName is empty.") }
            return
        }
        val selectedType = type?.takeIf { it in allowedTypes }
            ?: allowedTypes.singleOrNull()
        mutableState.update {
            it.copy(
                localFileImport = LocalFileImportState(
                    visible = true,
                    fileName = fileName,
                    bytes = bytes,
                    selectedType = selectedType,
                    targetInstanceId = it.selectedInstance?.id,
                    sourceOrigin = sourceOrigin,
                ),
                error = null,
            )
        }
    }

    override fun reportLocalFileReadFailure(fileName: String) {
        mutableState.update { it.copy(error = "$fileName could not be read.") }
    }

    override fun reportLocalFileTooLarge(fileName: String) {
        mutableState.update { it.copy(error = "$fileName is larger than the 512 MiB import limit.") }
    }

    override fun setLocalFileImportType(type: ResourceType) {
        mutableState.update { state ->
            val pending = state.localFileImport
            if (type !in pending.allowedTypes) state
            else state.copy(localFileImport = pending.copy(selectedType = type))
        }
    }

    override fun cancelLocalFileImport() {
        mutableState.update { it.copy(localFileImport = LocalFileImportState()) }
    }

    override fun confirmLocalFileImport() {
        if (resourceJob?.isActive == true) return
        val pending = mutableState.value.localFileImport
        val type = pending.selectedType ?: return
        val target = pending.targetInstanceId?.let { id -> mutableState.value.instances.firstOrNull { it.id == id } }
        if (type != ResourceType.MODPACK && target?.installationState !is InstallationState.Installed) {
            mutableState.update {
                it.copy(error = "Install the target instance before adding local content.")
            }
            return
        }
        resourceJob = scope.launch {
            mutableState.update {
                it.copy(
                    localFileImport = LocalFileImportState(),
                    operation = OperationStatus(
                        title = if (type == ResourceType.MODPACK) "Importing modpack" else "Adding local ${type.label.lowercase()}",
                        detail = pending.fileName,
                        cancellable = true,
                        cancelLabel = "Cancel",
                        instanceId = target?.id,
                    ),
                )
            }
            try {
                val notice = if (type == ResourceType.MODPACK) {
                    val created = services.modpackInstaller.installLocal(
                        pending.fileName,
                        pending.bytes,
                        ::updateResourceProgress,
                    )
                    mutableState.update { it.copy(selectedId = created.id) }
                    "${created.displayName} was imported."
                } else {
                    val instance = requireNotNull(target)
                    services.resourceInstaller.installLocal(instance, pending.fileName, pending.bytes, type)
                    "${pending.fileName} was added to ${instance.displayName}."
                }
                mutableState.update { it.copy(operation = null, notice = notice) }
                refreshInstalledContent()
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = "Local file import cancelled.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                resourceJob = null
            }
        }
    }

    override fun clearMessage() {
        mutableState.update {
            it.copy(error = null, errorRecovery = null, notice = null, removedInstanceUndo = null)
        }
    }

    override fun retryError() {
        val recovery = mutableState.value.errorRecovery
        mutableState.update { it.copy(error = null, errorRecovery = null) }
        when (recovery) {
            ErrorRecoveryAction.INITIALIZE -> initialize()
            ErrorRecoveryAction.REFRESH_VERSIONS -> refreshVersions()
            ErrorRecoveryAction.RETRY_INSTALLATION -> installSelected()
            null -> Unit
        }
    }

    override fun openInstanceSettings() {
        val instance = mutableState.value.selectedInstance ?: return
        val recommendation = LaunchTuningAdvisor.recommend(instance, services.systemProfile)
        mutableState.update {
            it.copy(
                instanceSettings = InstanceSettingsState(
                    visible = true,
                    instanceId = instance.id,
                    name = instance.displayName,
                    group = instance.group.orEmpty(),
                    iconReference = instance.iconReference.orEmpty(),
                    minecraftVersionId = instance.minecraftVersionId,
                    modLoader = instance.modLoader,
                    minimumMemoryMiB = instance.memory.minimumMiB.toString(),
                    maximumMemoryMiB = instance.memory.maximumMiB.toString(),
                    jvmArguments = instance.jvmArguments.joinToString(" "),
                    gameArguments = instance.gameArguments.joinToString(" "),
                    javaExecutable = instance.javaExecutable.orEmpty(),
                    environmentVariables = instance.environmentVariables.entries.joinToString("\n") { (key, value) ->
                        "$key=$value"
                    },
                    preLaunchCommand = instance.preLaunchCommand.joinToCommandLine(),
                    wrapperCommand = instance.wrapperCommand.joinToCommandLine(),
                    postExitCommand = instance.postExitCommand.joinToCommandLine(),
                    accountProfileId = instance.accountProfileId?.takeIf { profileId ->
                        mutableState.value.accounts.any { it.profile.profileId == profileId }
                    },
                    isLoadingClientSettings = true,
                    recommendation = "Recommended maximum: ${recommendation.memory.maximumMiB} MiB",
                    warnings = recommendation.warnings,
                ),
            )
        }
        scope.launch {
            try {
                val clientSettings = services.repository.readClientSettings(instance.id)
                mutableState.update { state ->
                    val form = state.instanceSettings
                    if (!form.visible || form.instanceId != instance.id) state
                    else state.copy(
                        instanceSettings = form.copy(
                            clientSettings = clientSettings,
                            isLoadingClientSettings = false,
                            clientSettingsError = if (clientSettings == null) {
                                "Client settings are unavailable for Minecraft ${instance.minecraftVersionId}."
                            } else {
                                null
                            },
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { state ->
                    val form = state.instanceSettings
                    if (!form.visible || form.instanceId != instance.id) state
                    else state.copy(
                        instanceSettings = form.copy(
                            isLoadingClientSettings = false,
                            clientSettingsError = error.message ?: "Client settings could not be loaded.",
                        ),
                    )
                }
            }
        }
    }

    override fun closeInstanceSettings() {
        mutableState.update { it.copy(instanceSettings = InstanceSettingsState()) }
    }

    override fun setMinimumMemory(value: String) {
        if (value.all(Char::isDigit)) {
            mutableState.update {
                it.copy(instanceSettings = it.instanceSettings.copy(minimumMemoryMiB = value))
            }
        }
    }

    override fun setMaximumMemory(value: String) {
        if (value.all(Char::isDigit)) {
            mutableState.update {
                it.copy(instanceSettings = it.instanceSettings.copy(maximumMemoryMiB = value))
            }
        }
    }

    override fun setJvmArguments(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(jvmArguments = value)) }
    }

    override fun setGameArguments(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(gameArguments = value)) }
    }

    override fun setJavaExecutable(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(javaExecutable = value)) }
    }

    override fun setEnvironmentVariables(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(environmentVariables = value)) }
    }

    override fun setPreLaunchCommand(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(preLaunchCommand = value)) }
    }

    override fun setWrapperCommand(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(wrapperCommand = value)) }
    }

    override fun setPostExitCommand(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(postExitCommand = value)) }
    }

    override fun setInstanceAccount(profileId: String?) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(accountProfileId = profileId)) }
    }

    override fun setInstanceName(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(name = value)) }
    }

    override fun setInstanceGroup(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(group = value)) }
    }

    override fun setInstanceIconReference(value: String) {
        mutableState.update {
            it.copy(instanceSettings = it.instanceSettings.copy(iconReference = value, pendingIcon = null))
        }
    }

    override fun setCustomInstanceIcon(fileName: String, bytes: ByteArray) {
        mutableState.update {
            it.copy(
                instanceSettings = it.instanceSettings.copy(
                    pendingIcon = PendingInstanceIcon(fileName, bytes),
                ),
            )
        }
    }

    override fun setInstanceVersion(value: String) {
        val supported = services.runtime.capabilities.supportedMinecraftVersions
        if (supported != null && value !in supported) return
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(minecraftVersionId = value)) }
    }

    override fun setInstanceLoader(value: ModLoader) {
        if (value !in supportedLoaders()) return
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(modLoader = value)) }
    }

    override fun setInstanceClientSettings(value: MinecraftClientSettings) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(clientSettings = value)) }
    }

    override fun applyRecommendedMemory() {
        val instanceId = mutableState.value.instanceSettings.instanceId ?: return
        val instance = mutableState.value.instances.firstOrNull { it.id == instanceId } ?: return
        val recommendation = LaunchTuningAdvisor.recommend(instance, services.systemProfile)
        mutableState.update {
            it.copy(
                instanceSettings = it.instanceSettings.copy(
                    minimumMemoryMiB = recommendation.memory.minimumMiB.toString(),
                    maximumMemoryMiB = recommendation.memory.maximumMiB.toString(),
                    warnings = emptyList(),
                ),
            )
        }
    }

    override fun saveInstanceSettings() {
        val form = mutableState.value.instanceSettings
        val instanceId = form.instanceId ?: return
        val instance = mutableState.value.instances.firstOrNull { it.id == instanceId } ?: return
        val minimum = form.minimumMemoryMiB.toIntOrNull() ?: return
        val maximum = form.maximumMemoryMiB.toIntOrNull() ?: return
        if (
            form.name.isBlank() || form.minecraftVersionId.isBlank() ||
            minimum <= 0 || maximum < minimum || form.isLoadingClientSettings
        ) return
        scope.launch {
            mutableState.update { it.copy(instanceSettings = form.copy(isSaving = true)) }
            try {
                val arguments = parseCommandLine(form.jvmArguments)
                val review = JvmArgumentPolicy.review(arguments)
                val gameArguments = parseCommandLine(form.gameArguments)
                val environmentVariables = parseEnvironmentVariables(form.environmentVariables)
                val supportsLaunchCommands = services.runtime.capabilities.supportsLaunchCommands
                val preLaunchCommand = if (supportsLaunchCommands) {
                    parseCommandLine(form.preLaunchCommand)
                } else {
                    instance.preLaunchCommand
                }
                val wrapperCommand = if (supportsLaunchCommands) {
                    parseCommandLine(form.wrapperCommand)
                } else {
                    instance.wrapperCommand
                }
                val postExitCommand = if (supportsLaunchCommands) {
                    parseCommandLine(form.postExitCommand)
                } else {
                    instance.postExitCommand
                }
                val componentsChanged = form.minecraftVersionId != instance.minecraftVersionId ||
                    form.modLoader != instance.modLoader
                val requiredJavaMajor = if (form.minecraftVersionId != instance.minecraftVersionId) {
                    services.metadataClient.resolveVersion(form.minecraftVersionId).javaVersion?.majorVersion ?: 8
                } else {
                    instance.requiredJavaMajor
                }
                val pendingIcon = form.pendingIcon
                val updatedInstance = instance.copy(
                    displayName = form.name.trim(),
                    group = form.group.trim().ifBlank { null },
                    iconReference = form.iconReference.trim().ifBlank { null },
                    minecraftVersionId = form.minecraftVersionId,
                    modLoader = form.modLoader,
                    loaderVersion = if (componentsChanged) null else instance.loaderVersion,
                    requiredJavaMajor = requiredJavaMajor,
                    memory = MemorySettings(minimum, maximum),
                    jvmArguments = review.accepted,
                    gameArguments = gameArguments,
                    javaExecutable = form.javaExecutable.trim().ifBlank { null },
                    environmentVariables = environmentVariables,
                    preLaunchCommand = preLaunchCommand,
                    wrapperCommand = wrapperCommand,
                    postExitCommand = postExitCommand,
                    accountProfileId = form.accountProfileId,
                    installationState = if (componentsChanged) {
                        InstallationState.NotInstalled
                    } else {
                        instance.installationState
                    },
                )
                val updated = if (pendingIcon == null) {
                    services.repository.update(updatedInstance)
                } else {
                    services.repository.updateWithIcon(
                        instance = updatedInstance,
                        fileName = pendingIcon.fileName,
                        bytes = pendingIcon.bytes,
                    )
                }
                form.clientSettings?.let { services.repository.updateClientSettings(instance.id, it) }
                cachedLaunch = null
                mutableState.update {
                    it.copy(
                        instanceSettings = InstanceSettingsState(),
                        notice = if (componentsChanged) {
                            "Instance settings saved. Install the new game components before launching."
                        } else if (review.ignored.isNotEmpty()) {
                            "Instance settings saved. Trestle ignored managed JVM options: ${review.ignored.joinToString(" ")}"
                        } else {
                            "Instance settings saved."
                        },
                    )
                }
                checkLaunchReadiness(updated)
                refreshInstalledContent()
            } catch (error: Exception) {
                mutableState.update { it.copy(instanceSettings = form.copy(isSaving = false)) }
                showError(error)
            }
        }
    }

    override fun selectAccount(profileId: String) {
        scope.launch {
            runCatching {
                services.accounts.select(profileId)
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }.onFailure(::showError)
        }
    }

    override fun openAccountLogin() {
        mutableState.update { it.copy(accountLogin = AccountLoginState(visible = true), error = null) }
    }

    override fun closeAccountLogin() {
        accountLoginJob?.cancel()
        accountLoginJob = null
        mutableState.update { it.copy(accountLogin = AccountLoginState(), operation = null) }
    }

    override fun setAccountLoginMethod(method: AccountAuthenticationMethod) {
        mutableState.update {
            it.copy(
                accountLogin = it.accountLogin.copy(
                    method = method,
                    authorization = null,
                    isWaiting = false,
                ),
            )
        }
    }

    override fun setBedrockGameVersion(value: String) {
        mutableState.update { it.copy(accountLogin = it.accountLogin.copy(bedrockGameVersion = value)) }
    }

    override fun setAccountEmail(value: String) {
        mutableState.update { it.copy(accountLogin = it.accountLogin.copy(email = value)) }
    }

    override fun setAccountPassword(value: String) {
        mutableState.update {
            it.copy(accountLogin = it.accountLogin.copy(password = SensitiveText(value)))
        }
    }

    override fun setImportedAccountSecret(value: String) {
        mutableState.update {
            it.copy(accountLogin = it.accountLogin.copy(importedSecret = SensitiveText(value)))
        }
    }

    override fun setOfflineUsername(value: String) {
        mutableState.update { it.copy(accountLogin = it.accountLogin.copy(offlineUsername = value)) }
    }

    override fun signInAccount() {
        if (accountLoginJob?.isActive == true) return
        val form = mutableState.value.accountLogin
        val request = form.toLoginRequest() ?: return
        accountLoginJob = scope.launch {
            mutableState.update {
                it.copy(
                    accountLogin = form.copy(isWaiting = true),
                    operation = OperationStatus(
                        if (form.method == AccountAuthenticationMethod.OFFLINE) {
                            "Adding offline account"
                        } else {
                            "Authenticating account"
                        },
                    ),
                    error = null,
                )
            }
            try {
                services.accounts.addAccount(request) { authorization ->
                    mutableState.update {
                        it.copy(
                            accountLogin = it.accountLogin.copy(
                                authorization = authorization,
                                isWaiting = true,
                            ),
                        )
                    }
                }
                val activeAccount = services.accounts.accounts.value.firstOrNull { it.isActive }
                if (form.method.usesOfficialJavaProfile && activeAccount?.isAuthenticated == true) {
                    val session = services.accounts.currentSession()
                    if (session != null) {
                        val profile = services.profileClient.fetchProfile(session).copy(
                            edition = MinecraftEdition.JAVA,
                            authenticationMethod = activeAccount.profile.authenticationMethod,
                            lastAuthenticatedAtEpochMillis = activeAccount.profile.lastAuthenticatedAtEpochMillis,
                        )
                        services.accounts.updateProfile(profile)
                    }
                }
                mutableState.update {
                    it.copy(
                        accountLogin = AccountLoginState(),
                        operation = null,
                        notice = if (form.method == AccountAuthenticationMethod.OFFLINE) {
                            "Offline account added. It can only join servers that allow offline identities."
                        } else {
                            "Account added."
                        },
                    )
                }
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null) }
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        accountLogin = it.accountLogin.copy(isWaiting = false),
                        operation = null,
                    )
                }
                showError(error)
            } finally {
                accountLoginJob = null
            }
        }
    }

    override fun signOutAccount(profileId: String) {
        scope.launch {
            runCatching {
                services.accounts.signOut(profileId)
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }.onFailure(::showError)
        }
    }

    override fun removeAccount(profileId: String) {
        scope.launch {
            runCatching {
                services.accounts.remove(profileId)
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }.onFailure(::showError)
        }
    }

    override fun refreshActiveAccount() {
        scope.launch {
            mutableState.update { it.copy(operation = OperationStatus("Refreshing account profile")) }
            try {
                val session = services.accounts.currentSession()
                    ?: error("The selected account needs to sign in again.")
                val existing = mutableState.value.accounts.firstOrNull { it.profile.profileId == session.profileId }?.profile
                val profile = services.profileClient.fetchProfile(session).copy(
                    authenticationMethod = existing?.authenticationMethod
                        ?: AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                    lastAuthenticatedAtEpochMillis = existing?.lastAuthenticatedAtEpochMillis,
                )
                services.accounts.updateProfile(profile)
                mutableState.update { it.copy(operation = null, notice = "Account profile refreshed.") }
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun resetActiveSkin() {
        scope.launch {
            mutableState.update { it.copy(operation = OperationStatus("Resetting active skin")) }
            try {
                val session = services.accounts.currentSession()
                    ?: error("The selected account needs to sign in again.")
                val profile = services.profileClient.resetActiveSkin(session)
                val existing = mutableState.value.accounts.firstOrNull { it.profile.profileId == session.profileId }?.profile
                services.accounts.updateProfile(
                    profile.copy(
                        authenticationMethod = existing?.authenticationMethod
                            ?: AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                        lastAuthenticatedAtEpochMillis = existing?.lastAuthenticatedAtEpochMillis,
                    ),
                )
                mutableState.update { it.copy(operation = null, notice = "The active skin was reset.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun openSkinStudio() {
        loadAccountSkinTextures(mutableState.value.accounts)
        val selected = mutableState.value.skinStudio.selectedProfileId
            ?: mutableState.value.savedSkins.firstOrNull()?.profile?.id
        mutableState.update {
            it.copy(
                skinStudio = SkinStudioState(visible = true, selectedProfileId = selected),
                error = null,
            )
        }
    }

    override fun closeSkinStudio() {
        mutableState.update { it.copy(skinStudio = SkinStudioState()) }
    }

    override fun selectSavedSkin(profileId: String) {
        mutableState.update { it.copy(skinStudio = it.skinStudio.copy(selectedProfileId = profileId)) }
    }

    override fun openNewSkin() {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = SkinEditorState(visible = true)))
        }
    }

    override fun saveCurrentSkinToLibrary() {
        val account = mutableState.value.accounts.firstOrNull { it.isActive } ?: return
        val texture = mutableState.value.accountSkinTextures[account.profile.profileId] ?: return
        mutableState.update {
            it.copy(
                skinStudio = it.skinStudio.copy(
                    editor = SkinEditorState(
                        visible = true,
                        name = "${account.profile.playerName}'s skin",
                        variant = account.profile.skin?.variant ?: SkinVariant.CLASSIC,
                        texture = texture.copyOf(),
                        sourceFileName = "current-skin.png",
                    ),
                ),
            )
        }
    }

    override fun editSelectedSkin() {
        val saved = mutableState.value.savedSkins.firstOrNull {
            it.profile.id == mutableState.value.skinStudio.selectedProfileId
        } ?: return
        mutableState.update {
            it.copy(
                skinStudio = it.skinStudio.copy(
                    editor = SkinEditorState(
                        visible = true,
                        profileId = saved.profile.id,
                        name = saved.profile.name,
                        variant = saved.profile.variant,
                        texture = saved.texture.copyOf(),
                        sourceFileName = saved.profile.textureFile,
                    ),
                ),
            )
        }
    }

    override fun closeSkinEditor() {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = SkinEditorState()))
        }
    }

    override fun setSkinName(value: String) {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = it.skinStudio.editor.copy(name = value, error = null)))
        }
    }

    override fun setSkinVariant(value: SkinVariant) {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = it.skinStudio.editor.copy(variant = value, error = null)))
        }
    }

    override fun setSkinFile(fileName: String, bytes: ByteArray) {
        val error = runCatching { inspectMinecraftSkin(bytes) }.exceptionOrNull()
        mutableState.update { state ->
            val editor = state.skinStudio.editor
            state.copy(
                skinStudio = state.skinStudio.copy(
                    editor = editor.copy(
                        name = if (editor.name.isBlank() && error == null) {
                            fileName.substringBeforeLast('.').take(64)
                        } else {
                            editor.name
                        },
                        texture = bytes.copyOf().takeIf { error == null } ?: editor.texture,
                        sourceFileName = fileName.takeIf { error == null } ?: editor.sourceFileName,
                        error = error?.message,
                    ),
                ),
            )
        }
    }

    override fun reportSkinFileReadFailure() {
        mutableState.update {
            it.copy(
                skinStudio = it.skinStudio.copy(
                    editor = it.skinStudio.editor.copy(error = "The selected skin file could not be read."),
                ),
            )
        }
    }

    override fun saveSkin(useAfterSave: Boolean) {
        val editor = mutableState.value.skinStudio.editor
        val texture = editor.texture ?: return
        if (editor.name.isBlank() || editor.isSaving) return
        scope.launch {
            mutableState.update {
                it.copy(
                    skinStudio = it.skinStudio.copy(editor = editor.copy(isSaving = true, error = null)),
                    operation = OperationStatus(if (useAfterSave) "Saving and applying skin" else "Saving skin"),
                )
            }
            try {
                val saved = services.skinLibrary.save(editor.name, editor.variant, texture, editor.profileId)
                if (useAfterSave) applySkin(saved)
                mutableState.update {
                    it.copy(
                        skinStudio = it.skinStudio.copy(
                            selectedProfileId = saved.profile.id,
                            editor = SkinEditorState(),
                        ),
                        operation = null,
                        notice = if (useAfterSave) "${saved.profile.name} is now your active skin." else "Skin saved.",
                    )
                }
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        skinStudio = it.skinStudio.copy(
                            editor = it.skinStudio.editor.copy(isSaving = false, error = error.message),
                        ),
                        operation = null,
                    )
                }
            }
        }
    }

    override fun useSelectedSkin() {
        val saved = mutableState.value.savedSkins.firstOrNull {
            it.profile.id == mutableState.value.skinStudio.selectedProfileId
        } ?: return
        scope.launch {
            mutableState.update { it.copy(operation = OperationStatus("Applying ${saved.profile.name}"), error = null) }
            try {
                applySkin(saved)
                mutableState.update { it.copy(operation = null, notice = "${saved.profile.name} is now your active skin.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun deleteSelectedSkin() {
        val profileId = mutableState.value.skinStudio.selectedProfileId ?: return
        scope.launch {
            runCatching { services.skinLibrary.delete(profileId) }
                .onSuccess { mutableState.update { it.copy(notice = "Skin removed from the local library.") } }
                .onFailure(::showError)
        }
    }

    override fun clearLogs() = services.logger.clear()

    override fun setThemePreference(value: ThemePreference) {
        setLauncherPreferences(mutableState.value.launcherPreferences.copy(theme = value))
    }

    override fun setLauncherPreferences(value: LauncherPreferences) {
        runCatching { services.preferences.write(value) }
            .onSuccess {
                mutableState.update { state ->
                    state.copy(
                        themePreference = value.theme,
                        launcherPreferences = value,
                        notice = "Launcher settings saved.",
                    )
                }
                services.configurePreferences(value)
            }
            .onFailure(::showError)
    }

    override fun checkForLauncherUpdate() {
        if (mutableState.value.isCheckingForUpdate) return
        scope.launch {
            mutableState.update { it.copy(isCheckingForUpdate = true) }
            try {
                val update = services.updateChecker.availableUpdate()
                mutableState.update {
                    it.copy(
                        isCheckingForUpdate = false,
                        availableUpdate = update,
                        notice = if (update == null) "Trestle is up to date." else "Trestle ${update.version} is available.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isCheckingForUpdate = false) }
                showError(error)
            }
        }
    }

    fun close() {
        accountLoginJob?.cancel()
        resourceSearchJob?.cancel()
        resourceJob?.cancel()
        scope.cancel()
        services.close()
    }

    private suspend fun applySkin(saved: SavedSkin) {
        val session = services.accounts.currentSession()
            ?: error("The selected account needs to sign in again.")
        val existing = mutableState.value.accounts.firstOrNull { it.profile.profileId == session.profileId }?.profile
            ?: error("The selected account is not available.")
        val profile = services.profileClient.uploadSkin(session, saved.texture, saved.profile.variant).copy(
            authenticationMethod = existing.authenticationMethod,
            lastAuthenticatedAtEpochMillis = existing.lastAuthenticatedAtEpochMillis,
        )
        services.accounts.updateProfile(profile)
        profile.skin?.let { skin -> loadedSkinUrls[profile.profileId] = skin.url }
        mutableState.update { state ->
            state.copy(accountSkinTextures = state.accountSkinTextures + (profile.profileId to saved.texture.copyOf()))
        }
    }

    private fun loadAccountSkinTextures(accounts: List<ManagedAccount>) {
        val profileIds = accounts.mapTo(mutableSetOf()) { it.profile.profileId }
        loadedSkinUrls.keys.retainAll(profileIds)
        mutableState.update { state ->
            state.copy(accountSkinTextures = state.accountSkinTextures.filterKeys { it in profileIds })
        }
        accounts.forEach { account ->
            val profileId = account.profile.profileId
            val skin = account.profile.skin
            if (skin == null) {
                loadedSkinUrls.remove(profileId)
                mutableState.update { state ->
                    state.copy(accountSkinTextures = state.accountSkinTextures - profileId)
                }
                return@forEach
            }
            if (loadedSkinUrls[profileId] == skin.url) return@forEach
            loadedSkinUrls[profileId] = skin.url
            scope.launch {
                runCatching { services.profileClient.fetchSkinTexture(skin.url) }
                    .onSuccess { texture ->
                        val currentUrl = mutableState.value.accounts
                            .firstOrNull { it.profile.profileId == profileId }
                            ?.profile?.skin?.url
                        if (currentUrl == skin.url) {
                            mutableState.update { state ->
                                state.copy(accountSkinTextures = state.accountSkinTextures + (profileId to texture))
                            }
                        }
                    }
                    .onFailure {
                        if (loadedSkinUrls[profileId] == skin.url) loadedSkinUrls.remove(profileId)
                    }
            }
        }
    }

    private fun searchResources(append: Boolean) {
        val browser = mutableState.value.resourceBrowser
        val platform = services.resourcePlatforms.platform(browser.provider)
        if (!platform.isAvailable || !platform.supports(browser.type)) return
        resourceSearchJob?.cancel()
        resourceSearchJob = scope.launch {
            mutableState.value = mutableState.value.copy(
                resourceBrowser = browser.copy(
                    isSearching = true,
                    projects = if (append) browser.projects else emptyList(),
                    totalProjects = if (append) browser.totalProjects else 0,
                    selectedProjectId = if (append) browser.selectedProjectId else null,
                    versions = if (append) browser.versions else emptyList(),
                    selectedVersionId = if (append) browser.selectedVersionId else null,
                    selectedOptionalDependencies = if (append) browser.selectedOptionalDependencies else emptySet(),
                    error = null,
                ),
            )
            try {
                val instance = mutableState.value.selectedInstance
                val result = platform.search(
                    ResourceSearchRequest(
                        query = browser.query,
                        type = browser.type,
                        gameVersion = browser.gameVersionFilter.ifBlank {
                            if (browser.type == ResourceType.MODPACK) "" else instance?.minecraftVersionId.orEmpty()
                        }.ifBlank { null },
                        loader = browser.loaderFilter ?: if (browser.type == ResourceType.MODPACK) null else instance?.modLoader,
                        category = browser.categoryFilter.ifBlank { null },
                        sort = browser.sort,
                        offset = if (append) browser.projects.size else 0,
                    ),
                )
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        projects = if (append) browser.projects + result.projects else result.projects,
                        totalProjects = result.total,
                        isSearching = false,
                        error = if (result.projects.isEmpty() && !append) "No resources matched these filters." else null,
                    ),
                )
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        isSearching = false,
                        error = error.message ?: "Resources could not be loaded.",
                    ),
                )
            }
        }
    }

    private fun updateResourceProgress(progress: net.blockhost.trestle.download.DownloadProgress) {
        val current = mutableState.value.operation ?: return
        mutableState.value = mutableState.value.copy(
            operation = current.copy(
                detail = progress.activeLabel,
                completed = progress.completedBytes,
                total = progress.totalBytes,
                completedItems = progress.completedFiles,
                totalItems = progress.totalFiles,
                cancellable = !progress.isFinalizing,
            ),
        )
    }

    private fun loadCreateLoaderVersions(loader: ModLoader) {
        if (loader == ModLoader.VANILLA) return
        val gameVersion = mutableState.value.create.versionId
        if (gameVersion.isBlank()) return
        scope.launch {
            mutableState.update { it.copy(create = it.create.copy(isResolvingLoader = true)) }
            try {
                val versions = when (loader) {
                    ModLoader.FABRIC -> services.fabricMetadataClient.loaderVersions(gameVersion)
                        .sortedWith(
                            compareByDescending<net.blockhost.trestle.metadata.FabricLoaderVersion> { it.stable }
                                .thenByDescending { it.build },
                        )
                        .map { it.version }
                    ModLoader.NEOFORGE -> services.neoForgeMetadataClient.loaderVersions(gameVersion)
                        .sortedWith(
                            compareByDescending<net.blockhost.trestle.metadata.NeoForgeLoaderVersion> { it.stable }
                                .thenByDescending { it.recommended }
                                .thenByDescending { it.releaseTime },
                        )
                        .map { it.version }
                    ModLoader.FORGE -> services.forgeMetadataClient.loaderVersions(gameVersion)
                        .sortedWith(
                            compareByDescending<net.blockhost.trestle.metadata.ForgeLoaderVersion> { it.recommended }
                                .thenByDescending { it.stable }
                                .thenByDescending { it.releaseTime },
                        )
                        .map { it.version }
                    ModLoader.QUILT -> services.quiltMetadataClient.loaderVersions(gameVersion)
                        .sortedWith(
                            compareByDescending<net.blockhost.trestle.metadata.QuiltLoaderVersion> { it.stable }
                                .thenByDescending { it.build },
                        )
                        .map { it.version }
                    ModLoader.VANILLA -> emptyList()
                }
                if (
                    mutableState.value.create.versionId != gameVersion ||
                    mutableState.value.create.modLoader != loader
                ) {
                    return@launch
                }
                mutableState.update {
                    it.copy(
                        create = it.create.copy(
                            loaderVersions = versions,
                            loaderVersion = versions.firstOrNull(),
                            isResolvingLoader = false,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (
                    mutableState.value.create.versionId == gameVersion &&
                    mutableState.value.create.modLoader == loader
                ) {
                    mutableState.update { it.copy(create = it.create.copy(isResolvingLoader = false)) }
                    showError(error)
                }
            }
        }
    }

    private fun checkLaunchReadiness(instance: GameInstance?) {
        if (
            instance == null ||
            instance.installationState !is InstallationState.Installed ||
            !services.runtime.capabilities.canPrepareLaunch
        ) {
            cachedLaunch = null
            mutableState.update {
                it.copy(
                    launch = InstanceLaunchState(
                        instanceId = instance?.id,
                        status = services.runtime.capabilities.unavailableReason
                            ?.let(LaunchStatus::Unavailable)
                            ?: LaunchStatus.NotChecked,
                    ),
                )
            }
            return
        }
        if (launchJob?.isActive == true) return
        launchCheckJob?.cancel()
        launchCheckJob = scope.launch {
            updateLaunch(instance.id, LaunchStatus.Checking)
            try {
                val prepared = services.runtime.prepare(instance, onProgress = ::updateRuntimePreparation)
                if (mutableState.value.selectedInstance?.id != instance.id) return@launch
                if (prepared.missingRequirements.isEmpty()) {
                    cachedLaunch = instance to prepared
                    updateLaunch(instance.id, LaunchStatus.Ready)
                } else {
                    cachedLaunch = null
                    updateLaunch(instance.id, LaunchStatus.Blocked(prepared.missingRequirements))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                cachedLaunch = null
                updateLaunch(
                    instance.id,
                    LaunchStatus.Failed(error.message ?: "The launch check failed."),
                )
            } finally {
                mutableState.update { it.copy(operation = null) }
                launchCheckJob = null
            }
        }
    }

    private fun findLatestCrashReport(instance: GameInstance): String? {
        val directory = instance.instanceDirectory.toPath() / "game" / "crash-reports"
        return runCatching {
            FileSystem.SYSTEM.list(directory)
                .filter { it.name.endsWith(".txt", ignoreCase = true) }
                .maxByOrNull { FileSystem.SYSTEM.metadataOrNull(it)?.lastModifiedAtMillis ?: 0L }
                ?.toString()
        }.getOrNull()
    }

    private fun supportedLoaders(): Set<ModLoader> =
        services.runtime.capabilities.supportedModLoaders ?: ModLoader.entries.toSet()

    private fun updateRuntimePreparation(progress: RuntimePreparationProgress) {
        mutableState.update {
            it.copy(
                operation = OperationStatus(
                    title = "Preparing Android runtime",
                    detail = progress.stage,
                    completed = progress.completedBytes,
                    total = progress.totalBytes,
                    completedItems = progress.completedItems,
                    totalItems = progress.totalItems,
                ),
            )
        }
    }

    private fun updateLaunch(
        id: InstanceId,
        status: LaunchStatus,
        notice: String? = null,
        activeEvent: Boolean = false,
    ) {
        mutableState.update { state ->
            val activeLaunch = if (activeEvent) {
                when (status) {
                    LaunchStatus.Starting,
                    is LaunchStatus.Running,
                    -> InstanceLaunchState(id, status)
                    else -> state.activeLaunch?.takeUnless { it.instanceId == id }
                }
            } else {
                state.activeLaunch
            }
            state.copy(
                launch = if (state.selectedInstance?.id == id) {
                    InstanceLaunchState(id, status)
                } else {
                    state.launch
                },
                activeLaunch = activeLaunch,
                error = if (activeEvent && status is LaunchStatus.Failed) status.message else state.error,
                notice = notice ?: state.notice,
            )
        }
    }

    private fun showError(error: Throwable, recovery: ErrorRecoveryAction? = null) {
        mutableState.update {
            it.copy(
                error = error.message ?: "The operation failed.",
                errorRecovery = recovery,
                notice = null,
            )
        }
    }
}

private val AccountAuthenticationMethod.usesOfficialJavaProfile: Boolean
    get() = edition == MinecraftEdition.JAVA &&
        this != AccountAuthenticationMethod.OFFLINE

private const val MAX_GAME_LOG_LINES = 1_000

private fun parseEnvironmentVariables(value: String): Map<String, String> = buildMap {
    value.lineSequence().forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isBlank() || line.startsWith('#')) return@forEachIndexed
        val separator = line.indexOf('=')
        require(separator > 0) { "Environment variable line ${index + 1} must use NAME=value." }
        val name = line.substring(0, separator).trim()
        require(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
            "Environment variable name $name is invalid."
        }
        put(name, line.substring(separator + 1))
    }
}

private fun parseCommandLine(value: String): List<String> {
    val arguments = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    var escaping = false
    var started = false
    value.forEach { character ->
        when {
            escaping -> {
                current.append(character)
                escaping = false
                started = true
            }
            character == '\\' -> escaping = true
            quote != null && character == quote -> quote = null
            quote == null && character in setOf('\'', '"') -> {
                quote = character
                started = true
            }
            quote == null && character.isWhitespace() -> if (started) {
                arguments += current.toString()
                current.clear()
                started = false
            }
            else -> {
                current.append(character)
                started = true
            }
        }
    }
    require(quote == null) { "Launch arguments contain an unmatched quote." }
    if (escaping) current.append('\\')
    if (started || current.isNotEmpty()) arguments += current.toString()
    return arguments
}

private fun List<String>.joinToCommandLine(): String = joinToString(" ") { argument ->
    if (argument.isNotEmpty() && argument.none { it.isWhitespace() || it in setOf('\\', '\'', '"') }) {
        argument
    } else {
        "\"${argument.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}

private fun AccountLoginState.toLoginRequest(): AccountLoginRequest? = when (method) {
    AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
    AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE,
    -> {
        if (edition == MinecraftEdition.BEDROCK && bedrockGameVersion.isBlank()) null
        else AccountLoginRequest.DeviceCode(method, bedrockGameVersion.takeIf(String::isNotBlank))
    }
    AccountAuthenticationMethod.MICROSOFT_CREDENTIALS,
    AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS,
    -> {
        if (
            email.isBlank() || password.isBlank() ||
            (edition == MinecraftEdition.BEDROCK && bedrockGameVersion.isBlank())
        ) {
            null
        } else {
            AccountLoginRequest.Credentials(
                method = method,
                email = email.trim(),
                password = SecretValue(password.reveal()),
                bedrockGameVersion = bedrockGameVersion.takeIf(String::isNotBlank),
            )
        }
    }
    AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN,
    AccountAuthenticationMethod.MICROSOFT_COOKIES,
    AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN,
    -> if (importedSecret.isBlank()) null else AccountLoginRequest.SecretImport(
        method,
        SecretValue(importedSecret.reveal()),
    )
    AccountAuthenticationMethod.OFFLINE -> offlineUsername.trim().takeIf(String::isNotBlank)
        ?.let(AccountLoginRequest::Offline)
}
