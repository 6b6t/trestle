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
import net.blockhost.trestle.auth.ManagedAccount
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
import net.blockhost.trestle.runtime.LaunchTuningAdvisor
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

data class OperationStatus(
    val title: String,
    val detail: String? = null,
    val completed: Long? = null,
    val total: Long? = null,
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
    val modInstall: ModInstallState = ModInstallState(),
    val operation: OperationStatus? = null,
    val instanceSettings: InstanceSettingsState = InstanceSettingsState(),
    val accounts: List<ManagedAccount> = emptyList(),
    val accountLogin: AccountLoginState = AccountLoginState(),
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
    private var accountLoginJob: Job? = null
    val state: StateFlow<LauncherUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            services.repository.instances.collectLatest { instances ->
                val selected = mutableState.value.selectedId?.takeIf { id -> instances.any { it.id == id } }
                    ?: instances.firstOrNull()?.id
                mutableState.value = mutableState.value.copy(instances = instances, selectedId = selected)
            }
        }
        scope.launch {
            services.accounts.accounts.collectLatest { accounts ->
                mutableState.value = mutableState.value.copy(accounts = accounts)
            }
        }
        scope.launch {
            services.logger.entries.collectLatest { logs ->
                mutableState.value = mutableState.value.copy(logs = logs)
            }
        }
        initialize()
    }

    fun initialize() {
        scope.launch {
            mutableState.value = mutableState.value.copy(
                isInitializing = true,
                error = null,
                operation = OperationStatus("Loading launcher data"),
            )
            runCatching {
                services.repository.initialize()
                services.accounts.initialize()
            }
                .onFailure { showError(it, ErrorRecoveryAction.INITIALIZE) }
            mutableState.value = mutableState.value.copy(isInitializing = false, operation = null)
            refreshVersions()
        }
    }

    fun refreshVersions() {
        scope.launch {
            mutableState.value = mutableState.value.copy(
                isLoadingVersions = true,
                error = null,
                operation = OperationStatus("Refreshing Minecraft versions"),
            )
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
                    operation = null,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(isLoadingVersions = false, operation = null)
                showError(error, ErrorRecoveryAction.REFRESH_VERSIONS)
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
                        memory = LaunchTuningAdvisor.recommendMemory(form.modLoader, services.systemProfile),
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
        if (installJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val resuming = instance.installationState is InstallationState.Interrupted
        installJob = scope.launch {
            mutableState.value = mutableState.value.copy(
                error = null,
                notice = null,
                launchPlan = null,
                operation = OperationStatus(
                    if (resuming) "Preparing installation resume" else "Preparing installation",
                    instance.displayName,
                    cancellable = true,
                ),
            )
            try {
                services.installer.install(instance) { progress ->
                    mutableState.value = mutableState.value.copy(
                        operation = OperationStatus(
                            title = if (resuming) {
                                "Resuming ${instance.displayName}"
                            } else {
                                "Installing ${instance.displayName}"
                            },
                            detail = progress.activeFile,
                            completed = progress.completedBytes,
                            total = progress.totalBytes,
                            cancellable = true,
                        ),
                    )
                }
                mutableState.value = mutableState.value.copy(notice = "Installation is complete.", operation = null)
            } catch (_: CancellationException) {
                mutableState.value = mutableState.value.copy(
                    notice = "Installation paused. Resume it when you are ready.",
                    operation = null,
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(operation = null)
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
            mutableState.value = mutableState.value.copy(
                notice = "Installation paused. Resume it when you are ready.",
                operation = null,
            )
        }
    }

    fun inspectLaunchPlan() {
        val instance = mutableState.value.selectedInstance ?: return
        if (instance.installationState !is InstallationState.Installed) return
        try {
            val installed = services.installer.readInstalledVersion(instance)
            val activeAccount = mutableState.value.accounts.firstOrNull { it.isActive }
            mutableState.value = mutableState.value.copy(
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
        } catch (error: Exception) {
            showError(error)
        }
    }

    fun validateLaunch() {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            mutableState.value = mutableState.value.copy(operation = OperationStatus("Checking launch requirements"))
            try {
                val prepared = services.runtime.prepare(instance)
                mutableState.value = mutableState.value.copy(
                    notice = if (prepared.missingRequirements.isEmpty()) {
                        "The instance is ready to launch."
                    } else {
                        "The launch plan is valid. Select a ready Java account before launch."
                    },
                    operation = null,
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(operation = null)
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
            mutableState.value = mutableState.value.copy(operation = OperationStatus("Resolving mod download", form.projectId))
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
                    operation = null,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(modInstall = form.copy(isInstalling = false))
                mutableState.value = mutableState.value.copy(operation = null)
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
        mutableState.value = mutableState.value.copy(error = null, errorRecovery = null, notice = null)
    }

    fun retryError() {
        val recovery = mutableState.value.errorRecovery
        mutableState.value = mutableState.value.copy(error = null, errorRecovery = null)
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
        mutableState.value = mutableState.value.copy(
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

    fun closeInstanceSettings() {
        mutableState.value = mutableState.value.copy(instanceSettings = InstanceSettingsState())
    }

    fun setMinimumMemory(value: String) {
        if (value.all(Char::isDigit)) {
            mutableState.value = mutableState.value.copy(
                instanceSettings = mutableState.value.instanceSettings.copy(minimumMemoryMiB = value),
            )
        }
    }

    fun setMaximumMemory(value: String) {
        if (value.all(Char::isDigit)) {
            mutableState.value = mutableState.value.copy(
                instanceSettings = mutableState.value.instanceSettings.copy(maximumMemoryMiB = value),
            )
        }
    }

    fun setJvmArguments(value: String) {
        mutableState.value = mutableState.value.copy(
            instanceSettings = mutableState.value.instanceSettings.copy(jvmArguments = value),
        )
    }

    fun applyRecommendedMemory() {
        val instance = mutableState.value.selectedInstance ?: return
        val recommendation = LaunchTuningAdvisor.recommend(instance, services.systemProfile)
        mutableState.value = mutableState.value.copy(
            instanceSettings = mutableState.value.instanceSettings.copy(
                minimumMemoryMiB = recommendation.memory.minimumMiB.toString(),
                maximumMemoryMiB = recommendation.memory.maximumMiB.toString(),
                warnings = emptyList(),
            ),
        )
    }

    fun saveInstanceSettings() {
        val instance = mutableState.value.selectedInstance ?: return
        val form = mutableState.value.instanceSettings
        val minimum = form.minimumMemoryMiB.toIntOrNull() ?: return
        val maximum = form.maximumMemoryMiB.toIntOrNull() ?: return
        if (minimum <= 0 || maximum < minimum) return
        scope.launch {
            mutableState.value = mutableState.value.copy(instanceSettings = form.copy(isSaving = true))
            try {
                val arguments = form.jvmArguments.split(Regex("\\s+")).filter(String::isNotBlank)
                val review = JvmArgumentPolicy.review(arguments)
                services.repository.update(
                    instance.copy(
                        memory = MemorySettings(minimum, maximum),
                        jvmArguments = review.accepted,
                    ),
                )
                mutableState.value = mutableState.value.copy(
                    instanceSettings = InstanceSettingsState(),
                    notice = if (review.ignored.isEmpty()) {
                        "Launch settings saved."
                    } else {
                        "Launch settings saved. Trestle ignored managed JVM options: ${review.ignored.joinToString(" ")}"
                    },
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(instanceSettings = form.copy(isSaving = false))
                showError(error)
            }
        }
    }

    fun selectAccount(profileId: String) {
        scope.launch {
            runCatching { services.accounts.select(profileId) }.onFailure(::showError)
        }
    }

    fun openAccountLogin() {
        mutableState.value = mutableState.value.copy(accountLogin = AccountLoginState(visible = true), error = null)
    }

    fun closeAccountLogin() {
        accountLoginJob?.cancel()
        accountLoginJob = null
        mutableState.value = mutableState.value.copy(accountLogin = AccountLoginState(), operation = null)
    }

    fun setAccountLoginMethod(method: AccountAuthenticationMethod) {
        mutableState.value = mutableState.value.copy(
            accountLogin = mutableState.value.accountLogin.copy(
                method = method,
                authorization = null,
                isWaiting = false,
            ),
        )
    }

    fun setBedrockGameVersion(value: String) {
        mutableState.value = mutableState.value.copy(
            accountLogin = mutableState.value.accountLogin.copy(bedrockGameVersion = value),
        )
    }

    fun setAccountEmail(value: String) {
        mutableState.value = mutableState.value.copy(
            accountLogin = mutableState.value.accountLogin.copy(email = value),
        )
    }

    fun setAccountPassword(value: String) {
        mutableState.value = mutableState.value.copy(
            accountLogin = mutableState.value.accountLogin.copy(password = SensitiveText(value)),
        )
    }

    fun setImportedAccountSecret(value: String) {
        mutableState.value = mutableState.value.copy(
            accountLogin = mutableState.value.accountLogin.copy(importedSecret = SensitiveText(value)),
        )
    }

    fun setOfflineUsername(value: String) {
        mutableState.value = mutableState.value.copy(
            accountLogin = mutableState.value.accountLogin.copy(offlineUsername = value),
        )
    }

    fun signInAccount() {
        if (accountLoginJob?.isActive == true) return
        val form = mutableState.value.accountLogin
        val request = form.toLoginRequest() ?: return
        accountLoginJob = scope.launch {
            mutableState.value = mutableState.value.copy(
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
            try {
                services.accounts.addAccount(request) { authorization ->
                    mutableState.value = mutableState.value.copy(
                        accountLogin = mutableState.value.accountLogin.copy(
                            authorization = authorization,
                            isWaiting = true,
                        ),
                    )
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
                mutableState.value = mutableState.value.copy(
                    accountLogin = AccountLoginState(),
                    operation = null,
                    notice = if (form.method == AccountAuthenticationMethod.OFFLINE) {
                        "Offline account added. It can only join servers that allow offline identities."
                    } else {
                        "Account added."
                    },
                )
            } catch (_: CancellationException) {
                mutableState.value = mutableState.value.copy(operation = null)
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    accountLogin = mutableState.value.accountLogin.copy(isWaiting = false),
                    operation = null,
                )
                showError(error)
            } finally {
                accountLoginJob = null
            }
        }
    }

    fun signOutAccount(profileId: String) {
        scope.launch {
            runCatching { services.accounts.signOut(profileId) }.onFailure(::showError)
        }
    }

    fun removeAccount(profileId: String) {
        scope.launch {
            runCatching { services.accounts.remove(profileId) }.onFailure(::showError)
        }
    }

    fun refreshActiveAccount() {
        scope.launch {
            mutableState.value = mutableState.value.copy(operation = OperationStatus("Refreshing account profile"))
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
                mutableState.value = mutableState.value.copy(operation = null, notice = "Account profile refreshed.")
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(operation = null)
                showError(error)
            }
        }
    }

    fun resetActiveSkin() {
        scope.launch {
            mutableState.value = mutableState.value.copy(operation = OperationStatus("Resetting active skin"))
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
                mutableState.value = mutableState.value.copy(operation = null, notice = "The active skin was reset.")
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(operation = null)
                showError(error)
            }
        }
    }

    fun clearLogs() = services.logger.clear()

    fun close() {
        accountLoginJob?.cancel()
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

    private fun showError(error: Throwable, recovery: ErrorRecoveryAction? = null) {
        mutableState.value = mutableState.value.copy(
            error = error.message ?: "The operation failed.",
            errorRecovery = recovery,
            notice = null,
        )
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
