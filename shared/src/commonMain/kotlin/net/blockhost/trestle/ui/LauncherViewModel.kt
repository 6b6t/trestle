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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.LauncherServices
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.metadata.VersionReference

data class CreateInstanceState(
    val visible: Boolean = false,
    val name: String = "",
    val versionId: String = "",
    val modLoader: ModLoader = ModLoader.VANILLA,
    val loaderVersion: String? = null,
    val loaderVersions: List<String> = emptyList(),
    val isResolvingLoader: Boolean = false,
    val isSaving: Boolean = false,
)

data class LaunchPlanSummary(
    val mainClass: String,
    val javaMajor: Int,
    val classpathEntries: Int,
    val nativeLibraries: Int,
    val workingDirectory: String,
    val authentication: String,
)

enum class ModProvider(val label: String) {
    MODRINTH("Modrinth"),
    CURSEFORGE("CurseForge"),
}

data class ModInstallState(
    val visible: Boolean = false,
    val provider: ModProvider = ModProvider.MODRINTH,
    val projectId: String = "",
    val curseForgeApiKey: SensitiveText = SensitiveText(),
    val isInstalling: Boolean = false,
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
    val notice: String? = null,
    val create: CreateInstanceState = CreateInstanceState(),
    val launchPlan: LaunchPlanSummary? = null,
    val modInstall: ModInstallState = ModInstallState(),
) {
    val selectedInstance: GameInstance?
        get() = instances.firstOrNull { it.id == selectedId } ?: instances.firstOrNull()
}

class LauncherViewModel(
    private val services: LauncherServices,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutableState = MutableStateFlow(LauncherUiState())
    private var installJob: Job? = null
    val state: StateFlow<LauncherUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            services.repository.instances.collectLatest { instances ->
                val selected = mutableState.value.selectedId?.takeIf { id -> instances.any { it.id == id } }
                    ?: instances.firstOrNull()?.id
                mutableState.value = mutableState.value.copy(instances = instances, selectedId = selected)
            }
        }
        initialize()
    }

    fun initialize() {
        scope.launch {
            mutableState.value = mutableState.value.copy(isInitializing = true, error = null)
            runCatching { services.repository.initialize() }
                .onFailure { showError(it) }
            mutableState.value = mutableState.value.copy(isInitializing = false)
            refreshVersions()
        }
    }

    fun refreshVersions() {
        scope.launch {
            mutableState.value = mutableState.value.copy(isLoadingVersions = true, error = null)
            try {
                val manifest = services.metadataClient.fetchVersionManifest()
                val versions = manifest.versions.filter { it.type == "release" || it.type == "snapshot" }
                val defaultVersion = versions.firstOrNull { it.id == manifest.latest.release }?.id
                    ?: versions.firstOrNull()?.id.orEmpty()
                mutableState.value = mutableState.value.copy(
                    versions = versions,
                    isLoadingVersions = false,
                    create = mutableState.value.create.copy(
                        versionId = mutableState.value.create.versionId.ifBlank { defaultVersion },
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(isLoadingVersions = false)
                showError(error)
            }
        }
    }

    fun selectInstance(id: InstanceId) {
        mutableState.value = mutableState.value.copy(selectedId = id, notice = null, launchPlan = null)
    }

    fun openCreate() {
        val defaultVersion = mutableState.value.create.versionId.ifBlank {
            mutableState.value.versions.firstOrNull()?.id.orEmpty()
        }
        mutableState.value = mutableState.value.copy(
            create = CreateInstanceState(visible = true, versionId = defaultVersion),
            error = null,
        )
    }

    fun closeCreate() {
        mutableState.value = mutableState.value.copy(create = CreateInstanceState())
    }

    fun setCreateName(value: String) {
        mutableState.value = mutableState.value.copy(create = mutableState.value.create.copy(name = value))
    }

    fun setCreateVersion(value: String) {
        mutableState.value = mutableState.value.copy(
            create = mutableState.value.create.copy(versionId = value, loaderVersion = null),
        )
        if (mutableState.value.create.modLoader == ModLoader.FABRIC) loadFabricVersions()
    }

    fun setCreateLoader(value: ModLoader) {
        mutableState.value = mutableState.value.copy(
            create = mutableState.value.create.copy(
                modLoader = value,
                loaderVersion = null,
                loaderVersions = emptyList(),
            ),
        )
        if (value == ModLoader.FABRIC) loadFabricVersions()
    }

    fun setCreateLoaderVersion(value: String) {
        mutableState.value = mutableState.value.copy(
            create = mutableState.value.create.copy(loaderVersion = value),
        )
    }

    fun createInstance() {
        val form = mutableState.value.create
        if (form.name.isBlank() || form.versionId.isBlank()) return
        scope.launch {
            mutableState.value = mutableState.value.copy(create = form.copy(isSaving = true), error = null)
            try {
                val metadata = services.metadataClient.resolveVersion(form.versionId)
                val instance = services.repository.create(
                    CreateInstanceRequest(
                        displayName = form.name,
                        minecraftVersionId = form.versionId,
                        modLoader = form.modLoader,
                        loaderVersion = form.loaderVersion,
                        requiredJavaMajor = metadata.javaVersion?.majorVersion ?: 8,
                    ),
                )
                mutableState.value = mutableState.value.copy(
                    selectedId = instance.id,
                    create = CreateInstanceState(),
                    notice = "Instance created. Install its game files when you are ready.",
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(create = form.copy(isSaving = false))
                showError(error)
            }
        }
    }

    fun installSelected() {
        val instance = mutableState.value.selectedInstance ?: return
        installJob?.cancel()
        installJob = scope.launch {
            mutableState.value = mutableState.value.copy(error = null, notice = null, launchPlan = null)
            try {
                services.installer.install(instance)
                mutableState.value = mutableState.value.copy(notice = "Installation is complete.")
            } catch (_: CancellationException) {
                mutableState.value = mutableState.value.copy(notice = "Installation cancelled.")
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    fun cancelInstall() {
        installJob?.cancel()
    }

    fun inspectLaunchPlan() {
        val instance = mutableState.value.selectedInstance ?: return
        if (instance.installationState !is InstallationState.Installed) return
        try {
            val installed = services.installer.readInstalledVersion(instance)
            mutableState.value = mutableState.value.copy(
                launchPlan = LaunchPlanSummary(
                    mainClass = installed.metadata.mainClass,
                    javaMajor = installed.requiredJavaMajor,
                    classpathEntries = installed.libraries.count { !it.native } + 1,
                    nativeLibraries = installed.libraries.count { it.native },
                    workingDirectory = "${instance.instanceDirectory}/game",
                    authentication = "Microsoft account required",
                ),
                notice = null,
                error = null,
            )
        } catch (error: Exception) {
            showError(error)
        }
    }

    fun validateLaunch() {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            try {
                val prepared = services.runtime.prepare(instance)
                mutableState.value = mutableState.value.copy(
                    notice = if (prepared.missingRequirements.isEmpty()) {
                        "The instance is ready to launch."
                    } else {
                        "The launch plan is valid. Sign in with a Microsoft account before launch."
                    },
                )
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    fun openModInstall() {
        mutableState.value = mutableState.value.copy(modInstall = ModInstallState(visible = true), error = null)
    }

    fun closeModInstall() {
        mutableState.value = mutableState.value.copy(modInstall = ModInstallState())
    }

    fun setModProvider(provider: ModProvider) {
        mutableState.value = mutableState.value.copy(
            modInstall = mutableState.value.modInstall.copy(provider = provider),
        )
    }

    fun setModProjectId(value: String) {
        mutableState.value = mutableState.value.copy(
            modInstall = mutableState.value.modInstall.copy(projectId = value),
        )
    }

    fun setCurseForgeApiKey(value: String) {
        mutableState.value = mutableState.value.copy(
            modInstall = mutableState.value.modInstall.copy(curseForgeApiKey = SensitiveText(value)),
        )
    }

    fun installMod() {
        val instance = mutableState.value.selectedInstance ?: return
        val form = mutableState.value.modInstall
        if (form.projectId.isBlank()) return
        scope.launch {
            mutableState.value = mutableState.value.copy(modInstall = form.copy(isInstalling = true), error = null)
            try {
                val provider = when (form.provider) {
                    ModProvider.MODRINTH -> services.modrinthDownloads
                    ModProvider.CURSEFORGE -> services.curseForgeDownloads(form.curseForgeApiKey.reveal())
                }
                val download = provider.resolve(
                    projectId = form.projectId.trim(),
                    gameVersion = instance.minecraftVersionId,
                    loader = instance.modLoader,
                )
                services.modInstaller.install(instance, download)
                mutableState.value = mutableState.value.copy(
                    modInstall = ModInstallState(),
                    notice = "${download.fileName} was added to ${instance.displayName}.",
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(modInstall = form.copy(isInstalling = false))
                showError(error)
            }
        }
    }

    fun deleteSelected() {
        val id = mutableState.value.selectedInstance?.id ?: return
        scope.launch {
            services.repository.delete(id)
            mutableState.value = mutableState.value.copy(
                selectedId = null,
                notice = "Instance removed from the library. Its game directory was kept.",
                launchPlan = null,
            )
        }
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(error = null, notice = null)
    }

    fun close() {
        scope.cancel()
        services.close()
    }

    private fun loadFabricVersions() {
        val gameVersion = mutableState.value.create.versionId
        if (gameVersion.isBlank()) return
        scope.launch {
            mutableState.value = mutableState.value.copy(
                create = mutableState.value.create.copy(isResolvingLoader = true),
            )
            try {
                val versions = services.fabricMetadataClient.loaderVersions(gameVersion)
                    .sortedWith(compareByDescending<net.blockhost.trestle.metadata.FabricLoaderVersion> { it.stable }.thenByDescending { it.build })
                    .map { it.version }
                mutableState.value = mutableState.value.copy(
                    create = mutableState.value.create.copy(
                        loaderVersions = versions,
                        loaderVersion = versions.firstOrNull(),
                        isResolvingLoader = false,
                    ),
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    create = mutableState.value.create.copy(isResolvingLoader = false),
                )
                showError(error)
            }
        }
    }

    private fun showError(error: Throwable) {
        mutableState.value = mutableState.value.copy(
            error = error.message ?: "The operation failed.",
            notice = null,
        )
    }
}
