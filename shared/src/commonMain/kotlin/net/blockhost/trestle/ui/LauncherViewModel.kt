package net.blockhost.trestle.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.LauncherServices
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
import net.blockhost.trestle.runtime.LaunchTuningAdvisor
import net.blockhost.trestle.runtime.PreparedLaunch
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.metadata.VersionReference
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceSearchRequest
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.ReleaseChannel

data class CreateInstanceState(
    val visible: Boolean = false,
    val name: String = "",
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

data class ResourceBrowserState(
    val visible: Boolean = false,
    val provider: ResourceProvider = ResourceProvider.MODRINTH,
    val type: ResourceType = ResourceType.MOD,
    val query: String = "",
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
)

enum class ErrorRecoveryAction {
    INITIALIZE,
    REFRESH_VERSIONS,
    RETRY_INSTALLATION,
}

data class InstanceSettingsState(
    val visible: Boolean = false,
    val minimumMemoryMiB: String = "",
    val maximumMemoryMiB: String = "",
    val jvmArguments: String = "",
    val recommendation: String? = null,
    val warnings: List<String> = emptyList(),
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
    val operation: OperationStatus? = null,
    val instanceSettings: InstanceSettingsState = InstanceSettingsState(),
    val accounts: List<ManagedAccount> = emptyList(),
    val accountLogin: AccountLoginState = AccountLoginState(),
    val savedSkins: List<SavedSkin> = emptyList(),
    val accountSkinTextures: Map<String, ByteArray> = emptyMap(),
    val skinStudio: SkinStudioState = SkinStudioState(),
    val logs: List<LogEntry> = emptyList(),
    val credentialProtection: CredentialProtection? = null,
) {
    val selectedInstance: GameInstance?
        get() = instances.firstOrNull { it.id == selectedId } ?: instances.firstOrNull()
}

class LauncherViewModel(
    private val services: LauncherServices,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutableState = MutableStateFlow(
        LauncherUiState(credentialProtection = services.credentialStore.protection),
    )
    private var installJob: Job? = null
    private var resourceJob: Job? = null
    private var resourceSearchJob: Job? = null
    private var accountLoginJob: Job? = null
    private var launchCheckJob: Job? = null
    private var launchJob: Job? = null
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
            if (initialized.isSuccess) {
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }
            refreshVersions()
        }
    }

    fun refreshVersions() {
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
                val versions = manifest.versions.filter { it.type == "release" || it.type == "snapshot" }
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

    fun selectInstance(id: InstanceId) {
        launchCheckJob?.cancel()
        cachedLaunch = null
        mutableState.update {
            it.copy(
                selectedId = id,
                notice = null,
                launchPlan = null,
                launch = InstanceLaunchState(id),
            )
        }
        checkLaunchReadiness(mutableState.value.selectedInstance)
    }

    fun openCreate() {
        val defaultVersion = mutableState.value.create.versionId.ifBlank {
            mutableState.value.versions.firstOrNull()?.id.orEmpty()
        }
        mutableState.update {
            it.copy(
                create = CreateInstanceState(visible = true, versionId = defaultVersion),
                error = null,
            )
        }
    }

    fun closeCreate() {
        mutableState.update { it.copy(create = CreateInstanceState()) }
    }

    fun setCreateName(value: String) {
        mutableState.update { it.copy(create = it.create.copy(name = value)) }
    }

    fun setCreateVersion(value: String) {
        mutableState.update { it.copy(create = it.create.copy(versionId = value, loaderVersion = null)) }
        if (mutableState.value.create.modLoader == ModLoader.FABRIC) loadFabricVersions()
    }

    fun setCreateLoader(value: ModLoader) {
        mutableState.update {
            it.copy(
                create = it.create.copy(
                    modLoader = value,
                    loaderVersion = null,
                    loaderVersions = emptyList(),
                ),
            )
        }
        if (value == ModLoader.FABRIC) loadFabricVersions()
    }

    fun setCreateLoaderVersion(value: String) {
        mutableState.update { it.copy(create = it.create.copy(loaderVersion = value)) }
    }

    fun setCreateClientPreconfiguration(value: Boolean) {
        mutableState.update { it.copy(create = it.create.copy(preconfigureClientSettings = value)) }
    }

    fun setCreateClientSettings(value: MinecraftClientSettings) {
        mutableState.update { it.copy(create = it.create.copy(clientSettings = value)) }
    }

    fun createInstance() {
        val form = mutableState.value.create
        if (form.name.isBlank() || form.versionId.isBlank()) return
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
                        clientSettings = form.clientSettings.takeIf { form.preconfigureClientSettings },
                    ),
                )
                mutableState.update {
                    it.copy(
                        selectedId = instance.id,
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

    fun installSelected() {
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

    fun cancelInstall() {
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

    fun cancelActiveOperation() {
        if (resourceJob?.isActive == true) {
            resourceJob?.cancel()
        } else {
            cancelInstall()
        }
    }

    fun inspectLaunchPlan() {
        val instance = mutableState.value.selectedInstance ?: return
        if (instance.installationState !is InstallationState.Installed) return
        try {
            val installed = services.installer.readInstalledVersion(instance)
            val activeAccount = mutableState.value.accounts.firstOrNull { it.isActive }
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

    fun launchSelected() {
        if (launchJob?.isActive == true) return
        if (!services.runtime.capabilities.canLaunch) return
        val instance = mutableState.value.selectedInstance ?: return
        if (instance.installationState !is InstallationState.Installed) return
        launchCheckJob?.cancel()
        launchCheckJob = null
        launchJob = scope.launch {
            mutableState.update {
                it.copy(error = null, errorRecovery = null, notice = null)
            }
            updateLaunch(instance.id, LaunchStatus.Starting)
            try {
                val prepared = cachedLaunch
                    ?.takeIf { (cachedInstance) -> cachedInstance == instance }
                    ?.second
                    ?: services.runtime.prepare(instance)
                if (prepared.missingRequirements.isNotEmpty()) {
                    cachedLaunch = null
                    updateLaunch(instance.id, LaunchStatus.Blocked(prepared.missingRequirements))
                    return@launch
                }
                cachedLaunch = instance to prepared
                services.runtime.launch(prepared).collect { event ->
                    when (event) {
                        is LaunchEvent.Started -> {
                            try {
                                services.repository.get(instance.id)?.let { current ->
                                    services.repository.update(
                                        current.copy(lastLaunchAtEpochMillis = services.clock.nowMillis()),
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
                            updateLaunch(instance.id, LaunchStatus.Running(event.processId))
                        }
                        is LaunchEvent.Log -> Unit
                        is LaunchEvent.Exited -> {
                            if (event.exitCode == 0) {
                                updateLaunch(instance.id, LaunchStatus.Ready, "Minecraft closed.")
                            } else {
                                updateLaunch(
                                    instance.id,
                                    LaunchStatus.Failed("Minecraft exited with code ${event.exitCode}."),
                                )
                            }
                        }
                        is LaunchEvent.Failed -> updateLaunch(
                            instance.id,
                            LaunchStatus.Failed(event.message),
                        )
                        LaunchEvent.Cancelled -> updateLaunch(
                            instance.id,
                            LaunchStatus.Ready,
                            "Minecraft stopped.",
                        )
                    }
                }
            } catch (_: CancellationException) {
                updateLaunch(instance.id, LaunchStatus.Ready, "Minecraft stopped.")
            } catch (error: Exception) {
                updateLaunch(
                    instance.id,
                    LaunchStatus.Failed(error.message ?: "Minecraft could not start."),
                )
            } finally {
                launchJob = null
            }
        }
    }

    fun stopLaunch() {
        launchJob?.cancel()
    }

    fun openResourceBrowser(type: ResourceType = ResourceType.MOD) {
        val curseForgeAvailable = services.resourcePlatforms.platform(ResourceProvider.CURSEFORGE).isAvailable
        mutableState.value = mutableState.value.copy(
            resourceBrowser = ResourceBrowserState(
                visible = true,
                type = type,
                curseForgeAvailable = curseForgeAvailable,
            ),
            error = null,
        )
        searchResources()
    }

    fun closeResourceBrowser() {
        if (resourceJob?.isActive == true) return
        resourceSearchJob?.cancel()
        mutableState.value = mutableState.value.copy(resourceBrowser = ResourceBrowserState())
    }

    fun setResourceProvider(provider: ResourceProvider) {
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
                    !platform.isAvailable -> "CurseForge is not configured for this build."
                    !platform.supports(current.type) -> "${provider.label} does not provide ${current.type.label.lowercase()}."
                    else -> null
                },
            ),
        )
        if (platform.isAvailable && platform.supports(current.type)) searchResources()
    }

    fun setResourceType(type: ResourceType) {
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

    fun setResourceQuery(value: String) {
        mutableState.value = mutableState.value.copy(
            resourceBrowser = mutableState.value.resourceBrowser.copy(query = value),
        )
    }

    fun searchResources() {
        searchResources(append = false)
    }

    fun loadMoreResources() {
        val browser = mutableState.value.resourceBrowser
        if (!browser.isSearching && browser.projects.size < browser.totalProjects) searchResources(append = true)
    }

    fun selectResource(projectId: String) {
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
                val versions = services.resourcePlatforms.platform(project.provider).versions(
                    project = project,
                    gameVersion = if (project.type == ResourceType.MODPACK) null else instance?.minecraftVersionId,
                    loader = if (project.type == ResourceType.MODPACK) null else instance?.modLoader,
                )
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
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

    fun selectResourceVersion(versionId: String) {
        mutableState.value = mutableState.value.copy(
            resourceBrowser = mutableState.value.resourceBrowser.copy(
                selectedVersionId = versionId,
                selectedOptionalDependencies = emptySet(),
            ),
        )
    }

    fun toggleOptionalDependency(key: String) {
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

    fun installSelectedResource() {
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
                    resourceBrowser = ResourceBrowserState(),
                    notice = notice,
                    operation = null,
                )
            } catch (_: CancellationException) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(isInstalling = false),
                    operation = null,
                    notice = if (project.type == ResourceType.MODPACK) {
                        "Modpack installation cancelled. No instance was added."
                    } else {
                        "Resource installation paused."
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

    fun deleteSelected() {
        val id = mutableState.value.selectedInstance?.id ?: return
        scope.launch {
            services.repository.delete(id)
            mutableState.update {
                it.copy(
                    selectedId = null,
                    notice = "Instance removed from the library. Its game directory was kept.",
                    launchPlan = null,
                )
            }
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(error = null, errorRecovery = null, notice = null) }
    }

    fun retryError() {
        val recovery = mutableState.value.errorRecovery
        mutableState.update { it.copy(error = null, errorRecovery = null) }
        when (recovery) {
            ErrorRecoveryAction.INITIALIZE -> initialize()
            ErrorRecoveryAction.REFRESH_VERSIONS -> refreshVersions()
            ErrorRecoveryAction.RETRY_INSTALLATION -> installSelected()
            null -> Unit
        }
    }

    fun openInstanceSettings() {
        val instance = mutableState.value.selectedInstance ?: return
        val recommendation = LaunchTuningAdvisor.recommend(instance, services.systemProfile)
        mutableState.update {
            it.copy(
                instanceSettings = InstanceSettingsState(
                    visible = true,
                    minimumMemoryMiB = instance.memory.minimumMiB.toString(),
                    maximumMemoryMiB = instance.memory.maximumMiB.toString(),
                    jvmArguments = instance.jvmArguments.joinToString(" "),
                    recommendation = "Recommended maximum: ${recommendation.memory.maximumMiB} MiB",
                    warnings = recommendation.warnings,
                ),
            )
        }
    }

    fun closeInstanceSettings() {
        mutableState.update { it.copy(instanceSettings = InstanceSettingsState()) }
    }

    fun setMinimumMemory(value: String) {
        if (value.all(Char::isDigit)) {
            mutableState.update {
                it.copy(instanceSettings = it.instanceSettings.copy(minimumMemoryMiB = value))
            }
        }
    }

    fun setMaximumMemory(value: String) {
        if (value.all(Char::isDigit)) {
            mutableState.update {
                it.copy(instanceSettings = it.instanceSettings.copy(maximumMemoryMiB = value))
            }
        }
    }

    fun setJvmArguments(value: String) {
        mutableState.update { it.copy(instanceSettings = it.instanceSettings.copy(jvmArguments = value)) }
    }

    fun applyRecommendedMemory() {
        val instance = mutableState.value.selectedInstance ?: return
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

    fun saveInstanceSettings() {
        val instance = mutableState.value.selectedInstance ?: return
        val form = mutableState.value.instanceSettings
        val minimum = form.minimumMemoryMiB.toIntOrNull() ?: return
        val maximum = form.maximumMemoryMiB.toIntOrNull() ?: return
        if (minimum <= 0 || maximum < minimum) return
        scope.launch {
            mutableState.update { it.copy(instanceSettings = form.copy(isSaving = true)) }
            try {
                val arguments = form.jvmArguments.split(Regex("\\s+")).filter(String::isNotBlank)
                val review = JvmArgumentPolicy.review(arguments)
                services.repository.update(
                    instance.copy(
                        memory = MemorySettings(minimum, maximum),
                        jvmArguments = review.accepted,
                    ),
                )
                cachedLaunch = null
                mutableState.update {
                    it.copy(
                        instanceSettings = InstanceSettingsState(),
                        notice = if (review.ignored.isEmpty()) {
                            "Launch settings saved."
                        } else {
                            "Launch settings saved. Trestle ignored managed JVM options: ${review.ignored.joinToString(" ")}"
                        },
                    )
                }
                checkLaunchReadiness(services.repository.get(instance.id))
            } catch (error: Exception) {
                mutableState.update { it.copy(instanceSettings = form.copy(isSaving = false)) }
                showError(error)
            }
        }
    }

    fun selectAccount(profileId: String) {
        scope.launch {
            runCatching {
                services.accounts.select(profileId)
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }.onFailure(::showError)
        }
    }

    fun openAccountLogin() {
        mutableState.update { it.copy(accountLogin = AccountLoginState(visible = true), error = null) }
    }

    fun closeAccountLogin() {
        accountLoginJob?.cancel()
        accountLoginJob = null
        mutableState.update { it.copy(accountLogin = AccountLoginState(), operation = null) }
    }

    fun setAccountLoginMethod(method: AccountAuthenticationMethod) {
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

    fun setBedrockGameVersion(value: String) {
        mutableState.update { it.copy(accountLogin = it.accountLogin.copy(bedrockGameVersion = value)) }
    }

    fun setAccountEmail(value: String) {
        mutableState.update { it.copy(accountLogin = it.accountLogin.copy(email = value)) }
    }

    fun setAccountPassword(value: String) {
        mutableState.update {
            it.copy(accountLogin = it.accountLogin.copy(password = SensitiveText(value)))
        }
    }

    fun setImportedAccountSecret(value: String) {
        mutableState.update {
            it.copy(accountLogin = it.accountLogin.copy(importedSecret = SensitiveText(value)))
        }
    }

    fun setOfflineUsername(value: String) {
        mutableState.update { it.copy(accountLogin = it.accountLogin.copy(offlineUsername = value)) }
    }

    fun signInAccount() {
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

    fun signOutAccount(profileId: String) {
        scope.launch {
            runCatching {
                services.accounts.signOut(profileId)
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }.onFailure(::showError)
        }
    }

    fun removeAccount(profileId: String) {
        scope.launch {
            runCatching {
                services.accounts.remove(profileId)
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            }.onFailure(::showError)
        }
    }

    fun refreshActiveAccount() {
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

    fun resetActiveSkin() {
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

    fun openSkinStudio() {
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

    fun closeSkinStudio() {
        mutableState.update { it.copy(skinStudio = SkinStudioState()) }
    }

    fun selectSavedSkin(profileId: String) {
        mutableState.update { it.copy(skinStudio = it.skinStudio.copy(selectedProfileId = profileId)) }
    }

    fun openNewSkin() {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = SkinEditorState(visible = true)))
        }
    }

    fun saveCurrentSkinToLibrary() {
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

    fun editSelectedSkin() {
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

    fun closeSkinEditor() {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = SkinEditorState()))
        }
    }

    fun setSkinName(value: String) {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = it.skinStudio.editor.copy(name = value, error = null)))
        }
    }

    fun setSkinVariant(value: SkinVariant) {
        mutableState.update {
            it.copy(skinStudio = it.skinStudio.copy(editor = it.skinStudio.editor.copy(variant = value, error = null)))
        }
    }

    fun setSkinFile(fileName: String, bytes: ByteArray) {
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

    fun reportSkinFileReadFailure() {
        mutableState.update {
            it.copy(
                skinStudio = it.skinStudio.copy(
                    editor = it.skinStudio.editor.copy(error = "The selected skin file could not be read."),
                ),
            )
        }
    }

    fun saveSkin(useAfterSave: Boolean) {
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

    fun useSelectedSkin() {
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

    fun deleteSelectedSkin() {
        val profileId = mutableState.value.skinStudio.selectedProfileId ?: return
        scope.launch {
            runCatching { services.skinLibrary.delete(profileId) }
                .onSuccess { mutableState.update { it.copy(notice = "Skin removed from the local library.") } }
                .onFailure(::showError)
        }
    }

    fun clearLogs() = services.logger.clear()

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
                        gameVersion = if (browser.type == ResourceType.MODPACK) null else instance?.minecraftVersionId,
                        loader = if (browser.type == ResourceType.MODPACK) null else instance?.modLoader,
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

    private fun loadFabricVersions() {
        val gameVersion = mutableState.value.create.versionId
        if (gameVersion.isBlank()) return
        scope.launch {
            mutableState.update { it.copy(create = it.create.copy(isResolvingLoader = true)) }
            try {
                val versions = services.fabricMetadataClient.loaderVersions(gameVersion)
                    .sortedWith(compareByDescending<net.blockhost.trestle.metadata.FabricLoaderVersion> { it.stable }.thenByDescending { it.build })
                    .map { it.version }
                mutableState.update {
                    it.copy(
                        create = it.create.copy(
                            loaderVersions = versions,
                            loaderVersion = versions.firstOrNull(),
                            isResolvingLoader = false,
                        ),
                    )
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(create = it.create.copy(isResolvingLoader = false)) }
                showError(error)
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
                val prepared = services.runtime.prepare(instance)
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
                launchCheckJob = null
            }
        }
    }

    private fun updateLaunch(id: InstanceId, status: LaunchStatus, notice: String? = null) {
        mutableState.update { state ->
            if (state.selectedInstance?.id != id) state
            else state.copy(
                launch = InstanceLaunchState(id, status),
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
        this != AccountAuthenticationMethod.OFFLINE &&
        this != AccountAuthenticationMethod.THE_ALTENING

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
    AccountAuthenticationMethod.THE_ALTENING,
    -> if (importedSecret.isBlank()) null else AccountLoginRequest.SecretImport(
        method,
        SecretValue(importedSecret.reveal()),
    )
    AccountAuthenticationMethod.OFFLINE -> offlineUsername.trim().takeIf(String::isNotBlank)
        ?.let(AccountLoginRequest::Offline)
}
