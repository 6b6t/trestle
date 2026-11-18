package net.blockhost.trestle.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.blockhost.trestle.app.FolderPreferences
import net.blockhost.trestle.app.LauncherPreferences
import net.blockhost.trestle.app.LauncherServices
import net.blockhost.trestle.app.LauncherUpdate
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.AccountLoginRequest
import net.blockhost.trestle.auth.CredentialProtection
import net.blockhost.trestle.auth.DeviceAuthorization
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SecretValue
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.auth.inspectMinecraftSkin
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.MemorySettings
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.instance.GameDataInventory
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.SavedServer
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.metadata.VersionReference
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.ModpackUpdatePreview
import net.blockhost.trestle.resources.ReleaseChannel
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceSearchRequest
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.RestrictedDownload
import net.blockhost.trestle.resources.RestrictedDownloadRequired
import net.blockhost.trestle.resources.msg_account_added_and_set_as_active
import net.blockhost.trestle.resources.msg_account_profile_refreshed
import net.blockhost.trestle.resources.msg_adding_local_file
import net.blockhost.trestle.resources.msg_adding_offline_account
import net.blockhost.trestle.resources.msg_all_files_verified_continuing_the_installation
import net.blockhost.trestle.resources.msg_all_files_verified_retry_the_installation_to
import net.blockhost.trestle.resources.msg_applying_pack_update
import net.blockhost.trestle.resources.msg_applying_s
import net.blockhost.trestle.resources.msg_authenticating_account
import net.blockhost.trestle.resources.msg_backing_up_world
import net.blockhost.trestle.resources.msg_bedrock_account_added_it_can_t_launch
import net.blockhost.trestle.resources.msg_checking_modpack_updates
import net.blockhost.trestle.resources.msg_choose_a_jar_zip_or_mrpack_file
import net.blockhost.trestle.resources.msg_client_settings_are_unavailable_for_minecraft_s
import net.blockhost.trestle.resources.msg_client_settings_could_not_be_loaded
import net.blockhost.trestle.resources.msg_cloning_instance
import net.blockhost.trestle.resources.msg_content_update_cancelled
import net.blockhost.trestle.resources.msg_copying_world
import net.blockhost.trestle.resources.msg_created_s
import net.blockhost.trestle.resources.msg_deleted_s
import net.blockhost.trestle.resources.msg_deleted_s_2
import net.blockhost.trestle.resources.msg_deleted_s_existing_backups_were_kept
import net.blockhost.trestle.resources.msg_deleting_instance
import net.blockhost.trestle.resources.msg_deleting_log
import net.blockhost.trestle.resources.msg_deleting_screenshot
import net.blockhost.trestle.resources.msg_deleting_world
import net.blockhost.trestle.resources.msg_disabling_data_pack
import net.blockhost.trestle.resources.msg_enabling_data_pack
import net.blockhost.trestle.resources.msg_enter_an_http_https_or_curseforge_url
import net.blockhost.trestle.resources.msg_environment_variable_line_s_must_use_name
import net.blockhost.trestle.resources.msg_environment_variable_name_s_is_invalid
import net.blockhost.trestle.resources.msg_exported_s_to_s
import net.blockhost.trestle.resources.msg_exporting_instance
import net.blockhost.trestle.resources.msg_finalizing_s
import net.blockhost.trestle.resources.msg_finish_installing_this_instance_first
import net.blockhost.trestle.resources.msg_ftb_app_import_cancelled
import net.blockhost.trestle.resources.msg_imported_ftb_app_instances
import net.blockhost.trestle.resources.msg_imported_s
import net.blockhost.trestle.resources.msg_importing_ftb_app_instances
import net.blockhost.trestle.resources.msg_importing_modpack
import net.blockhost.trestle.resources.msg_importing_world
import net.blockhost.trestle.resources.msg_install_the_selected_instance_before_adding_resources
import net.blockhost.trestle.resources.msg_install_the_target_instance_before_adding_local
import net.blockhost.trestle.resources.msg_installation_is_complete
import net.blockhost.trestle.resources.msg_installation_paused_resume_it_when_you_are
import net.blockhost.trestle.resources.msg_installed_content_is_up_to_date
import net.blockhost.trestle.resources.msg_installing_modpack
import net.blockhost.trestle.resources.msg_installing_s
import net.blockhost.trestle.resources.msg_instance_created_install_its_game_files_when
import net.blockhost.trestle.resources.msg_instance_removed_files_kept
import net.blockhost.trestle.resources.msg_instance_settings_saved
import net.blockhost.trestle.resources.msg_instance_settings_saved_ignored_jvm_options
import net.blockhost.trestle.resources.msg_instance_settings_saved_install_the_new_game
import net.blockhost.trestle.resources.msg_launch_arguments_contain_an_unmatched_quote
import net.blockhost.trestle.resources.msg_loading_launcher_data
import net.blockhost.trestle.resources.msg_local_file_import_cancelled
import net.blockhost.trestle.resources.msg_minecraft_closed
import net.blockhost.trestle.resources.msg_minecraft_could_not_start
import net.blockhost.trestle.resources.msg_minecraft_exited_with_code_s
import net.blockhost.trestle.resources.msg_minecraft_stopped
import net.blockhost.trestle.resources.msg_modpack_import_cancelled
import net.blockhost.trestle.resources.msg_modpack_installation_cancelled_no_instance_was_added
import net.blockhost.trestle.resources.msg_no_newer_stable_version_of_this_pack
import net.blockhost.trestle.resources.msg_no_newer_trestle_release_is_available
import net.blockhost.trestle.resources.msg_offline_account_added_and_set_as_active
import net.blockhost.trestle.resources.msg_one_content_update_is_available
import net.blockhost.trestle.resources.msg_pack_updated_the_previous_files_are_backed
import net.blockhost.trestle.resources.msg_player_offline
import net.blockhost.trestle.resources.msg_player_sign_in_expired
import net.blockhost.trestle.resources.msg_preparing_android_runtime
import net.blockhost.trestle.resources.msg_preparing_installation
import net.blockhost.trestle.resources.msg_preparing_installation_resume
import net.blockhost.trestle.resources.msg_preparing_modpack_update
import net.blockhost.trestle.resources.msg_recommended_maximum_s_mib
import net.blockhost.trestle.resources.msg_refreshing_account_profile
import net.blockhost.trestle.resources.msg_refreshing_minecraft_versions
import net.blockhost.trestle.resources.msg_removed_s
import net.blockhost.trestle.resources.msg_removed_server
import net.blockhost.trestle.resources.msg_removing_server
import net.blockhost.trestle.resources.msg_renamed_screenshot_to_s
import net.blockhost.trestle.resources.msg_renamed_world_to_s
import net.blockhost.trestle.resources.msg_renaming_screenshot
import net.blockhost.trestle.resources.msg_renaming_world
import net.blockhost.trestle.resources.msg_reordering_servers
import net.blockhost.trestle.resources.msg_reset_the_world_icon
import net.blockhost.trestle.resources.msg_resetting_active_skin
import net.blockhost.trestle.resources.msg_resetting_world_icon
import net.blockhost.trestle.resources.msg_resource_installation_cancelled
import net.blockhost.trestle.resources.msg_resources_could_not_be_loaded
import net.blockhost.trestle.resources.msg_restored_s
import net.blockhost.trestle.resources.msg_restoring_pack_backup
import net.blockhost.trestle.resources.msg_restoring_world
import net.blockhost.trestle.resources.msg_resuming_s
import net.blockhost.trestle.resources.msg_s_and_its_files_were_deleted
import net.blockhost.trestle.resources.msg_s_content_updates_are_available
import net.blockhost.trestle.resources.msg_s_could_not_be_read
import net.blockhost.trestle.resources.msg_s_does_not_provide_s
import net.blockhost.trestle.resources.msg_s_is_empty
import net.blockhost.trestle.resources.msg_s_is_larger_than_the_mib_import
import net.blockhost.trestle.resources.msg_s_is_not_available_for_third_party
import net.blockhost.trestle.resources.msg_s_is_now_your_active_skin
import net.blockhost.trestle.resources.msg_s_is_still_running_close_it_before
import net.blockhost.trestle.resources.msg_s_updates_found_s_items_could_not
import net.blockhost.trestle.resources.msg_s_was_added_to_s
import net.blockhost.trestle.resources.msg_s_was_added_to_the_library
import net.blockhost.trestle.resources.msg_s_was_created
import net.blockhost.trestle.resources.msg_s_was_disabled
import net.blockhost.trestle.resources.msg_s_was_enabled
import net.blockhost.trestle.resources.msg_s_was_imported
import net.blockhost.trestle.resources.msg_s_was_installed
import net.blockhost.trestle.resources.msg_s_was_installed_with_dependencies
import net.blockhost.trestle.resources.msg_s_was_moved_to_trash
import net.blockhost.trestle.resources.msg_s_was_pinned_to_the_top
import net.blockhost.trestle.resources.msg_s_was_removed
import net.blockhost.trestle.resources.msg_s_was_restored_to_the_library
import net.blockhost.trestle.resources.msg_s_was_unpinned
import net.blockhost.trestle.resources.msg_s_was_updated_to_s
import net.blockhost.trestle.resources.msg_saved_notes_for_s
import net.blockhost.trestle.resources.msg_saved_s_to_the_minecraft_server_list
import net.blockhost.trestle.resources.msg_saving_and_applying_skin
import net.blockhost.trestle.resources.msg_saving_skin
import net.blockhost.trestle.resources.msg_select_a_java_account
import net.blockhost.trestle.resources.msg_sign_in_failed_try_again
import net.blockhost.trestle.resources.msg_skin_could_not_be_saved
import net.blockhost.trestle.resources.msg_skin_removed_from_the_local_library
import net.blockhost.trestle.resources.msg_skin_saved
import net.blockhost.trestle.resources.msg_still_finishing_last_change
import net.blockhost.trestle.resources.msg_stop_minecraft_before_moving_this_instance_to
import net.blockhost.trestle.resources.msg_stop_minecraft_before_preparing_a_pack_update
import net.blockhost.trestle.resources.msg_stop_minecraft_before_removing_this_instance
import net.blockhost.trestle.resources.msg_stop_minecraft_before_rolling_back_the_pack
import net.blockhost.trestle.resources.msg_stop_minecraft_before_updating_this_pack
import net.blockhost.trestle.resources.msg_that_file_is_not_one_the_pack
import net.blockhost.trestle.resources.msg_that_instance_is_no_longer_in_the
import net.blockhost.trestle.resources.msg_the_active_skin_was_reset
import net.blockhost.trestle.resources.msg_the_curseforge_file_blocks_third_party_downloads
import net.blockhost.trestle.resources.msg_the_curseforge_link_has_no_file_id
import net.blockhost.trestle.resources.msg_the_curseforge_link_has_no_project_id
import net.blockhost.trestle.resources.msg_the_instance_changed_after_the_preview_prepare
import net.blockhost.trestle.resources.msg_the_instance_could_not_be_moved_to
import net.blockhost.trestle.resources.msg_the_launch_check_failed
import net.blockhost.trestle.resources.msg_the_operation_failed
import net.blockhost.trestle.resources.msg_the_previous_pack_version_was_restored
import net.blockhost.trestle.resources.msg_the_resource_could_not_be_installed
import net.blockhost.trestle.resources.msg_the_selected_account_is_not_available
import net.blockhost.trestle.resources.msg_the_selected_account_needs_to_sign_in
import net.blockhost.trestle.resources.msg_the_selected_skin_file_could_not_be
import net.blockhost.trestle.resources.msg_trestle_s_is_available
import net.blockhost.trestle.resources.msg_updated_the_server_order
import net.blockhost.trestle.resources.msg_updating_s
import net.blockhost.trestle.resources.msg_vanilla_cannot_load_mods
import net.blockhost.trestle.resources.msg_versions_could_not_be_loaded
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_instance_copy_name
import net.blockhost.trestle.resources.ui_pause
import net.blockhost.trestle.resources.ui_player_skin_name
import net.blockhost.trestle.resources.ui_set_the_ftb_app_instances_folder_in
import net.blockhost.trestle.resources.ui_sign_in_required
import net.blockhost.trestle.resources.ui_trestle_could_not_read_that_image
import net.blockhost.trestle.runtime.JvmArgumentPolicy
import net.blockhost.trestle.runtime.LaunchEvent
import net.blockhost.trestle.runtime.LaunchOptions
import net.blockhost.trestle.runtime.LaunchRequirement
import net.blockhost.trestle.runtime.LaunchTuningAdvisor
import net.blockhost.trestle.runtime.PreparedLaunch
import net.blockhost.trestle.runtime.RuntimePreparationProgress
import net.blockhost.trestle.text.UserFacingArgumentException
import net.blockhost.trestle.text.UserText
import net.blockhost.trestle.text.asUserText
import net.blockhost.trestle.text.checkUser
import net.blockhost.trestle.text.load
import net.blockhost.trestle.text.phraseResource
import net.blockhost.trestle.text.pluralText
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.requireUserNotNull
import net.blockhost.trestle.text.singularResource
import net.blockhost.trestle.text.text
import net.blockhost.trestle.text.userError
import net.blockhost.trestle.text.userText
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
    val authentication: UserText,
)

sealed interface LaunchStatus {
    data object NotChecked : LaunchStatus
    data object Checking : LaunchStatus
    data object Ready : LaunchStatus
    data class Blocked(val missingRequirements: List<LaunchRequirement>) : LaunchStatus
    data class Unavailable(val reason: UserText) : LaunchStatus
    data object Starting : LaunchStatus
    data class Running(val processId: Long?) : LaunchStatus
    data class Failed(val message: UserText) : LaunchStatus
}

data class InstanceLaunchState(val instanceId: InstanceId? = null, val status: LaunchStatus = LaunchStatus.NotChecked)

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
    val error: UserText? = null,
    val curseForgeAvailable: Boolean = false,
) {
    val selectedProject: ResourceProject?
        get() = projects.firstOrNull { it.id == selectedProjectId }

    val selectedVersion: ResourceVersion?
        get() = versions.firstOrNull { it.id == selectedVersionId }
}

data class OperationStatus(
    val title: UserText,
    val detail: UserText? = null,
    val completed: Long? = null,
    val total: Long? = null,
    val completedItems: Int? = null,
    val totalItems: Int? = null,
    val cancellable: Boolean = false,
    val cancelLabel: UserText = text(Res.string.ui_pause),
    val instanceId: InstanceId? = null,
)

private class QueuedLocalFile(val fileName: String, val bytes: ByteArray, val type: ResourceType?, val sourceOrigin: String?)

/** Marks a search as running; a new search (not a page append) also clears the previous results and selection. */
private fun ResourceBrowserState.startingSearch(append: Boolean): ResourceBrowserState = if (append) {
    copy(isSearching = true, error = null)
} else {
    copy(
        isSearching = true,
        projects = emptyList(),
        totalProjects = 0,
        selectedProjectId = null,
        versions = emptyList(),
        selectedVersionId = null,
        selectedOptionalDependencies = emptySet(),
        isLoadingVersions = false,
        error = null,
    )
}

/** Content types that need a mod loader; Vanilla instances cannot load them. */
internal val loaderOnlyTypes = setOf(ResourceType.MOD, ResourceType.SHADER_PACK)

internal val VANILLA_CONTENT_MESSAGE: UserText = text(Res.string.msg_vanilla_cannot_load_mods)

internal val BUSY_NOTICE: UserText = text(Res.string.msg_still_finishing_last_change)

/** The notice shown after an instance leaves the library; its snackbar is the only one that offers Undo. */
internal val INSTANCE_REMOVED_NOTICE: UserText = text(Res.string.msg_instance_removed_files_kept)

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

    override fun equals(other: Any?): Boolean = other is LocalFileImportState &&
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
    val clientSettingsError: UserText? = null,
    val recommendation: UserText? = null,
    val warnings: List<UserText> = emptyList(),
    val isSaving: Boolean = false,
    /** True when the form is shown inside the instance page instead of as a dialog. */
    val inline: Boolean = false,
    /** The values the form opened with, used to detect unsaved changes. */
    val initial: InstanceSettingsState? = null,
) {
    val hasChanges: Boolean
        get() = initial != null && editableValues() != initial.editableValues()

    private fun editableValues() = copy(
        visible = false,
        isLoadingClientSettings = false,
        clientSettingsError = null,
        recommendation = null,
        warnings = emptyList(),
        isSaving = false,
        inline = false,
        initial = null,
    )
}

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
    /** Why the last attempt failed, shown inside the dialog where the player is looking. */
    val error: UserText? = null,
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
    val error: UserText? = null,
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
    val error: UserText? = null,
    val errorRecovery: ErrorRecoveryAction? = null,
    val notice: UserText? = null,
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
    /** Accounts whose skin texture could not be downloaded, so previews can say so instead of loading forever. */
    val failedSkinTextures: Set<String> = emptySet(),
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
    /** Files the current install still needs from their publishers, and the checksums already verified. */
    val restrictedDownloads: List<RestrictedDownload> = emptyList(),
    val verifiedRestrictedDownloads: Set<String> = emptySet(),
    val suggestedPackInstances: List<GameInstance> = emptyList(),
    val modpackUpdatePreview: ModpackUpdatePreview? = null,
    val canRollbackModpack: Boolean = false,
    val availableUpdate: LauncherUpdate? = null,
    val isCheckingForUpdate: Boolean = false,
    val pendingWorldDeletionKey: String? = null,
) {
    val selectedInstance: GameInstance?
        get() = instances.firstOrNull { it.id == selectedId }
            ?: instances.maxByOrNull { it.lastLaunchAtEpochMillis ?: Long.MIN_VALUE }

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
    private var updateJob: Job? = null
    private var installJob: Job? = null
    private var resourceJob: Job? = null
    private var resourceSearchJob: Job? = null
    private var resourceVersionsJob: Job? = null
    private var installedContentJob: Job? = null
    private var contentWriteJob: Job? = null
    private var contentUpdateCheckJob: Job? = null
    private var runtimePreparationOperation: OperationStatus? = null
    private var gameDataJob: Job? = null
    private var gameDataWriteJob: Job? = null
    private var accountLoginJob: Job? = null
    private var launchCheckJob: Job? = null

    /** The instance whose worlds, content and launch check are loaded. */
    private var loadedSelectionId: InstanceId? = null
    private var launchJob: Job? = null
    private val initialInitialization = CompletableDeferred<Unit>()
    private var cachedLaunch: Pair<GameInstance, PreparedLaunch>? = null
    private val queuedLocalFiles = ArrayDeque<QueuedLocalFile>()
    private var restrictedDownloadRetry: (() -> Unit)? = null
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
        // Removing, creating, cloning or restoring an instance changes the selection without selectInstance.
        // Reload the per-instance data then, so actions never run against the previous instance's lists.
        scope.launch {
            mutableState.map { it.selectedInstance?.id }.distinctUntilChanged().collect { id ->
                if (initialInitialization.isCompleted && id != loadedSelectionId) reloadSelection(id)
            }
        }
        initialize()
        scope.launch {
            while (true) {
                checkLauncherUpdate(automatic = true)
                kotlinx.coroutines.delay(60 * 60 * 1000L)
            }
        }
    }

    fun initialize() {
        scope.launch {
            mutableState.update {
                it.copy(
                    isInitializing = true,
                    error = null,
                    operation = OperationStatus(text(Res.string.msg_loading_launcher_data)),
                )
            }
            val initialized = runCatching {
                services.repository.initialize()
                services.accounts.initialize()
                services.skinLibrary.initialize()
            }
                .onFailure { failure ->
                    // Hide only cancellation of this job. An interrupted worker can also throw CancellationException.
                    if (failure !is CancellationException || isActive) {
                        showError(failure, ErrorRecoveryAction.INITIALIZE)
                    }
                }
            val instances = services.repository.instances.value
            mutableState.update { state ->
                val selected = state.selectedId?.takeIf { id -> instances.any { it.id == id } }
                    ?: instances.firstOrNull()?.id
                state.copy(instances = instances, selectedId = selected)
            }
            mutableState.update { it.copy(isInitializing = false, operation = null) }
            loadedSelectionId = mutableState.value.selectedInstance?.id
            initialInitialization.complete(Unit)
            if (initialized.isSuccess) {
                checkLaunchReadiness(mutableState.value.selectedInstance)
                refreshInstalledContent()
                refreshGameData()
                // refreshVersions() clears the error, so run it only when there is no initialization error to keep.
                refreshVersions()
            }
        }
    }

    override fun refreshVersions() {
        scope.launch {
            mutableState.update {
                it.copy(
                    isLoadingVersions = true,
                    error = null,
                    operation = OperationStatus(text(Res.string.msg_refreshing_minecraft_versions)),
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
        mutableState.update {
            it.copy(
                selectedId = id,
                notice = null,
                removedInstanceUndo = null,
                instanceSettings = InstanceSettingsState(),
            )
        }
        reloadSelection(mutableState.value.selectedInstance?.id)
    }

    private fun reloadSelection(id: InstanceId?) {
        loadedSelectionId = id
        launchCheckJob?.cancel()
        contentUpdateCheckJob?.cancel()
        cachedLaunch = null
        mutableState.update {
            it.copy(
                launchPlan = null,
                launch = id?.let { selected ->
                    it.activeLaunch?.takeIf { active -> active.instanceId == selected } ?: InstanceLaunchState(selected)
                } ?: it.launch,
                installedContent = emptyList(),
                installedContentUpdates = emptyMap(),
                isCheckingInstalledContentUpdates = false,
                gameData = GameDataInventory(),
                pendingWorldDeletionKey = null,
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
                                text(Res.string.msg_s_was_pinned_to_the_top, updated.displayName)
                            } else {
                                text(Res.string.msg_s_was_unpinned, updated.displayName)
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
                it.copy(operation = OperationStatus(text(Res.string.msg_cloning_instance), instance.displayName.asUserText()))
            }
            runCatching {
                services.repository.clone(
                    instance.id,
                    text(Res.string.ui_instance_copy_name, instance.displayName).load(isolateArguments = false),
                )
            }.onSuccess { clone ->
                mutableState.update {
                    it.copy(
                        selectedId = clone.id,
                        operation = null,
                        notice = text(Res.string.msg_s_was_created, clone.displayName),
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
                it.copy(operation = OperationStatus(text(Res.string.msg_exporting_instance), instance.displayName.asUserText()))
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
                        notice = text(Res.string.msg_exported_s_to_s, instance.displayName, path),
                    )
                }
            }.onFailure { error ->
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun openCreate() {
        val versions = mutableState.value.versions
        val defaultVersion = mutableState.value.create.versionId.ifBlank {
            (versions.firstOrNull { it.type == "release" } ?: versions.firstOrNull())?.id.orEmpty()
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
                val configured = if (form.group.isBlank()) {
                    instance
                } else {
                    services.repository.update(
                        instance.copy(group = form.group.trim()),
                    )
                }
                mutableState.update {
                    it.copy(
                        selectedId = configured.id,
                        create = CreateInstanceState(),
                        notice = text(Res.string.msg_instance_created_install_its_game_files_when),
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

    override fun importRemoteModpack(url: String) = importRemoteModpack(url, mutableState.value.create)

    private fun importRemoteModpack(url: String, requestedIdentity: CreateInstanceState) {
        val source = url.trim()
        if (source.isBlank() || resourceJob?.isActive == true) return
        mutableState.update {
            it.copy(
                create = CreateInstanceState(),
                operation = OperationStatus(
                    text(Res.string.msg_importing_modpack),
                    source.asUserText(),
                    cancellable = true,
                    cancelLabel = text(Res.string.ui_cancel),
                ),
                error = null,
            )
        }
        resourceJob = scope.launch {
            try {
                val downloadUrl = resolveModpackImportUrl(source)
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
                        notice = text(Res.string.msg_s_was_imported, configured.displayName),
                    )
                }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = text(Res.string.msg_modpack_import_cancelled)) }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error, resume = { importRemoteModpack(source, requestedIdentity) })
            } finally {
                resourceJob = null
            }
        }
    }

    /** Turns a pasted modpack link into a direct download URL; curseforge:// links are resolved through the API. */
    private suspend fun resolveModpackImportUrl(source: String): String = if (source.startsWith("curseforge://", ignoreCase = true)) {
        val parameters = source.substringAfter('?', "").split('&').mapNotNull { part ->
            val key = part.substringBefore('=', "").lowercase()
            val value = part.substringAfter('=', "")
            if (key.isBlank() || value.isBlank()) null else key to value
        }.toMap()
        val projectId =
            requireUserNotNull(parameters["addonid"] ?: parameters["projectid"]) {
                text(Res.string.msg_the_curseforge_link_has_no_project_id)
            }
        val fileId = requireUserNotNull(parameters["fileid"]) { text(Res.string.msg_the_curseforge_link_has_no_file_id) }
        requireUserNotNull(
            services.resourcePlatforms.platform(ResourceProvider.CURSEFORGE).version(projectId, fileId).primaryFile?.url,
        ) { text(Res.string.msg_the_curseforge_file_blocks_third_party_downloads) }
    } else {
        requireUser(source.startsWith("https://") || source.startsWith("http://")) {
            text(Res.string.msg_enter_an_http_https_or_curseforge_url)
        }
        source
    }

    override fun importFtbAppInstances() = importFtbAppInstances(mutableState.value.create.group.trim())

    private fun importFtbAppInstances(requestedGroup: String) {
        if (resourceJob?.isActive == true) return
        val path = mutableState.value.launcherPreferences.ftbAppInstancesPath.trim()
        if (path.isBlank()) {
            showError(UserFacingArgumentException(text(Res.string.ui_set_the_ftb_app_instances_folder_in)))
            return
        }
        mutableState.update {
            it.copy(
                create = CreateInstanceState(),
                operation = OperationStatus(
                    text(Res.string.msg_importing_ftb_app_instances),
                    path.asUserText(),
                    cancellable = true,
                    cancelLabel = text(Res.string.ui_cancel),
                ),
                error = null,
            )
        }
        resourceJob = scope.launch {
            try {
                val imported = services.modpackInstaller.importFtbAppInstances(path)
                val configured = if (requestedGroup.isBlank()) {
                    imported
                } else {
                    imported.map { instance ->
                        services.repository.update(instance.copy(group = requestedGroup))
                    }
                }
                mutableState.update {
                    it.copy(
                        selectedId = configured.lastOrNull()?.id ?: it.selectedId,
                        operation = null,
                        notice = pluralText(Res.plurals.msg_imported_ftb_app_instances, configured.size, configured.size),
                    )
                }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = text(Res.string.msg_ftb_app_import_cancelled)) }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error, resume = { importFtbAppInstances(requestedGroup) })
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
                        if (resuming) text(Res.string.msg_preparing_installation_resume) else text(Res.string.msg_preparing_installation),
                        instance.displayName.asUserText(),
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
                                    text(Res.string.msg_finalizing_s, instance.displayName)
                                } else if (resuming) {
                                    text(Res.string.msg_resuming_s, instance.displayName)
                                } else {
                                    text(Res.string.msg_installing_s, instance.displayName)
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
                mutableState.update { it.copy(notice = text(Res.string.msg_installation_is_complete), operation = null) }
                checkLaunchReadiness(installed)
            } catch (_: CancellationException) {
                mutableState.update {
                    it.copy(
                        notice = text(Res.string.msg_installation_paused_resume_it_when_you_are),
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
                    notice = text(Res.string.msg_installation_paused_resume_it_when_you_are),
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
                            activeAccount == null -> text(Res.string.ui_sign_in_required)

                            activeAccount.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE ->
                                text(Res.string.msg_player_offline, activeAccount.profile.playerName)

                            activeAccount.isAuthenticated && activeAccount.profile.edition == MinecraftEdition.JAVA ->
                                activeAccount.profile.playerName.asUserText()

                            activeAccount.profile.edition == MinecraftEdition.BEDROCK ->
                                text(Res.string.msg_select_a_java_account)

                            else -> text(Res.string.msg_player_sign_in_expired, activeAccount.profile.playerName)
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
        if (launchJob?.isActive == true) {
            val running = mutableState.value.activeInstance
            if (running != null && running.id != mutableState.value.selectedInstance?.id) {
                mutableState.update {
                    it.copy(notice = text(Res.string.msg_s_is_still_running_close_it_before, running.displayName))
                }
            }
            return
        }
        if (resourceJob?.isActive == true || mutableState.value.modpackUpdatePreview != null) return
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
                                    text(Res.string.msg_minecraft_closed),
                                    activeEvent = true,
                                )
                            } else {
                                mutableState.update {
                                    it.copy(lastCrashReport = findLatestCrashReport(instance))
                                }
                                updateLaunch(
                                    instance.id,
                                    LaunchStatus.Failed(text(Res.string.msg_minecraft_exited_with_code_s, event.exitCode)),
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
                                text(Res.string.msg_minecraft_stopped),
                                activeEvent = true,
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                updateLaunch(instance.id, LaunchStatus.Ready, text(Res.string.msg_minecraft_stopped), activeEvent = true)
            } catch (error: Exception) {
                startedAtEpochMillis?.let { recordPlayTime(instance.id, it) }
                updateLaunch(
                    instance.id,
                    LaunchStatus.Failed(error.userText(text(Res.string.msg_minecraft_could_not_start))),
                    activeEvent = true,
                )
            } finally {
                clearRuntimePreparation()
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
                mutableState.update { it.copy(error = text(Res.string.msg_that_instance_is_no_longer_in_the)) }
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

    override fun openResourceBrowser(type: ResourceType, presentation: ResourceBrowserPresentation) {
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

    override fun ensureResourceBrowserPage() {
        val browser = mutableState.value.resourceBrowser
        if (!browser.visible || browser.presentation != ResourceBrowserPresentation.PAGE) {
            openResourceBrowser(ResourceType.MOD, ResourceBrowserPresentation.PAGE)
        }
    }

    override fun closeResourceBrowser() {
        if (resourceJob?.isActive == true) return
        resourceSearchJob?.cancel()
        mutableState.value = mutableState.value.copy(resourceBrowser = ResourceBrowserState())
    }

    override fun setResourceProvider(provider: ResourceProvider) {
        resourceSearchJob?.cancel()
        resourceVersionsJob?.cancel()
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
                isSearching = false,
                isLoadingVersions = false,
                error = when {
                    !platform.isAvailable -> text(Res.string.msg_s_is_not_available_for_third_party, provider.label)

                    !platform.supports(
                        current.type,
                    ) -> text(Res.string.msg_s_does_not_provide_s, provider.label, text(current.type.phraseResource))

                    else -> null
                },
            ),
        )
        if (platform.isAvailable && platform.supports(current.type)) searchResources()
    }

    override fun setResourceType(type: ResourceType) {
        resourceSearchJob?.cancel()
        resourceVersionsJob?.cancel()
        val current = mutableState.value.resourceBrowser
        val requestedPlatform = services.resourcePlatforms.platform(current.provider)
        val provider = if (requestedPlatform.supports(type)) current.provider else ResourceProvider.MODRINTH
        mutableState.value = mutableState.value.copy(
            resourceBrowser = current.copy(
                provider = provider,
                type = type,
                // Each content type has its own categories, so a category from the previous type no longer applies.
                categoryFilter = "",
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
        val cleared = value.isBlank() && mutableState.value.resourceBrowser.query.isNotBlank()
        mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(query = value)) }
        if (cleared) searchResources()
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
        if (channels.isEmpty()) return
        mutableState.update { state ->
            val current = state.resourceBrowser
            val visible = current.versions.filter { it.channel in channels }
            val keepSelection = visible.any { it.id == current.selectedVersionId }
            state.copy(
                resourceBrowser = current.copy(
                    releaseChannels = channels,
                    selectedVersionId = if (keepSelection) current.selectedVersionId else visible.firstOrNull()?.id,
                ),
            )
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
        resourceVersionsJob?.cancel()
        resourceVersionsJob = scope.launch {
            mutableState.update {
                it.copy(
                    resourceBrowser = it.resourceBrowser.copy(
                        selectedProjectId = projectId,
                        versions = emptyList(),
                        selectedVersionId = null,
                        selectedOptionalDependencies = emptySet(),
                        isLoadingVersions = true,
                        error = null,
                    ),
                )
            }
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
                        error = null,
                    ),
                )
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        isLoadingVersions = false,
                        error = error.userText(text(Res.string.msg_versions_could_not_be_loaded)),
                    ),
                )
            }
        }
    }

    override fun clearResourceSelection() {
        resourceVersionsJob?.cancel()
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

    override fun installSelectedResource() = installSelectedResource(suggestUpdate = true)

    override fun installSelectedModpackAsNew() {
        mutableState.update { it.copy(suggestedPackInstances = emptyList()) }
        installSelectedResource(suggestUpdate = false)
    }

    private fun installSelectedResource(suggestUpdate: Boolean) {
        if (resourceJob?.isActive == true) return
        val browser = mutableState.value.resourceBrowser
        val project = browser.selectedProject ?: return
        val version = browser.selectedVersion ?: return
        val instance = mutableState.value.selectedInstance
        if (suggestUpdate && project.type == ResourceType.MODPACK && mutableState.value.launcherPreferences.content.suggestModpackUpdates) {
            val existing = mutableState.value.instances.filter {
                it.modpackOrigin?.provider == project.provider.name && it.modpackOrigin.projectId == project.id
            }
            if (existing.isNotEmpty()) {
                mutableState.update { it.copy(suggestedPackInstances = existing) }
                return
            }
        }
        val blocked = when {
            project.type == ResourceType.MODPACK -> null

            instance?.installationState !is InstallationState.Installed -> text(
                Res.string.msg_install_the_selected_instance_before_adding_resources,
            )

            instance.modLoader == ModLoader.VANILLA && project.type in loaderOnlyTypes -> VANILLA_CONTENT_MESSAGE

            else -> null
        }
        if (blocked != null) {
            mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.copy(error = blocked)) }
            return
        }
        resourceJob = scope.launch {
            mutableState.value = mutableState.value.copy(
                resourceBrowser = mutableState.value.resourceBrowser.copy(isInstalling = true, error = null),
                operation = OperationStatus(
                    title = if (project.type == ResourceType.MODPACK) {
                        text(Res.string.msg_installing_modpack)
                    } else {
                        text(Res.string.msg_installing_s, project.name)
                    },
                    detail = if (project.type == ResourceType.MODPACK) {
                        project.name.asUserText()
                    } else {
                        text(project.type.singularResource)
                    },
                    cancellable = true,
                    cancelLabel = text(Res.string.ui_cancel),
                    instanceId = instance?.id,
                ),
            )
            try {
                val notice = if (project.type == ResourceType.MODPACK) {
                    val created = services.modpackInstaller.install(project, version, ::updateResourceProgress)
                    mutableState.value = mutableState.value.copy(selectedId = created.id)
                    text(Res.string.msg_s_was_added_to_the_library, created.displayName)
                } else {
                    val selectedInstance = requireNotNull(instance)
                    val summary = services.resourceInstaller.install(
                        selectedInstance,
                        project,
                        version,
                        browser.selectedOptionalDependencies,
                        ::updateResourceProgress,
                    )
                    if (summary.dependencyCount == 0) {
                        text(Res.string.msg_s_was_installed, project.name)
                    } else {
                        pluralText(
                            Res.plurals.msg_s_was_installed_with_dependencies,
                            summary.dependencyCount,
                            project.name,
                            summary.dependencyCount,
                        )
                    }
                }
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = if (browser.presentation == ResourceBrowserPresentation.PAGE) {
                        mutableState.value.resourceBrowser.copy(isInstalling = false, error = null)
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
                        text(Res.string.msg_modpack_installation_cancelled_no_instance_was_added)
                    } else {
                        text(Res.string.msg_resource_installation_cancelled)
                    },
                )
            } catch (error: Exception) {
                if (error is RestrictedDownloadRequired) {
                    restrictedDownloadRetry = { installSelectedResource(suggestUpdate = false) }
                    mutableState.update {
                        it.copy(restrictedDownloads = error.downloads, verifiedRestrictedDownloads = emptySet())
                    }
                }
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        isInstalling = false,
                        error = error.userText(text(Res.string.msg_the_resource_could_not_be_installed)),
                    ),
                    operation = null,
                )
            } finally {
                resourceJob = null
            }
        }
    }

    override fun dismissRestrictedDownload() {
        restrictedDownloadRetry = null
        mutableState.update { it.copy(restrictedDownloads = emptyList(), verifiedRestrictedDownloads = emptySet()) }
    }

    override fun acceptRestrictedDownload(bytes: ByteArray) {
        val pending = mutableState.value.restrictedDownloads
        if (pending.isEmpty()) return
        scope.launch {
            val download = services.restrictedDownloads.match(pending, bytes)
            if (download == null) {
                mutableState.update { it.copy(error = text(Res.string.msg_that_file_is_not_one_the_pack)) }
                return@launch
            }
            runCatching { services.restrictedDownloads.accept(download, bytes) }
                .onSuccess { onRestrictedDownloadVerified(download) }
                .onFailure(::showError)
        }
    }

    private fun onRestrictedDownloadVerified(download: RestrictedDownload) {
        mutableState.update { it.copy(verifiedRestrictedDownloads = it.verifiedRestrictedDownloads + download.sha1, error = null) }
        val state = mutableState.value
        if (state.restrictedDownloads.any { it.sha1 !in state.verifiedRestrictedDownloads }) return
        val retry = restrictedDownloadRetry
        restrictedDownloadRetry = null
        mutableState.update {
            it.copy(
                restrictedDownloads = emptyList(),
                verifiedRestrictedDownloads = emptySet(),
                notice = if (retry ==
                    null
                ) {
                    text(Res.string.msg_all_files_verified_retry_the_installation_to)
                } else {
                    text(Res.string.msg_all_files_verified_continuing_the_installation)
                },
            )
        }
        retry?.invoke()
    }

    override fun previewSelectedModpackUpdate(id: InstanceId) {
        val state = mutableState.value
        val instance = state.instances.firstOrNull { it.id == id } ?: return
        val project = state.resourceBrowser.selectedProject ?: return
        val version = state.resourceBrowser.selectedVersion ?: return
        mutableState.update { it.copy(suggestedPackInstances = emptyList()) }
        preparePackUpdate(instance, project, version)
    }

    override fun checkModpackUpdate() {
        if (resourceJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val origin = instance.modpackOrigin ?: return
        resourceJob = scope.launch {
            mutableState.update {
                it.copy(operation = OperationStatus(text(Res.string.msg_checking_modpack_updates), instanceId = instance.id))
            }
            try {
                val provider = ResourceProvider.valueOf(origin.provider)
                val platform = services.resourcePlatforms.platform(provider)
                val project = ResourceProject(
                    provider, origin.projectId, origin.projectId, origin.name, "", "",
                    ResourceType.MODPACK, 0, instance.iconReference, origin.websiteUrl, emptyList(),
                )
                val versions = platform.versions(project, null, null)
                val current = versions.firstOrNull { it.id == origin.versionId }
                    ?: platform.version(origin.projectId, origin.versionId)
                val latest = versions.filter {
                    it.channel == ReleaseChannel.RELEASE && it.id != origin.versionId &&
                        it.publishedAt > current.publishedAt
                }
                    .maxByOrNull { it.publishedAt }
                if (latest == null) {
                    mutableState.update { it.copy(notice = text(Res.string.msg_no_newer_stable_version_of_this_pack)) }
                } else {
                    preparePackUpdateNow(instance, project, latest)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error, resume = ::checkModpackUpdate)
            } finally {
                mutableState.update { it.copy(operation = null) }
            }
        }
    }

    private fun preparePackUpdate(instance: GameInstance, project: ResourceProject, version: ResourceVersion) {
        if (resourceJob?.isActive == true) return
        resourceJob = scope.launch {
            try {
                preparePackUpdateNow(instance, project, version)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error, resume = { preparePackUpdate(instance, project, version) })
            } finally {
                mutableState.update { it.copy(operation = null) }
            }
        }
    }

    private suspend fun preparePackUpdateNow(instance: GameInstance, project: ResourceProject, version: ResourceVersion) {
        requireUser(mutableState.value.activeLaunch?.instanceId != instance.id) {
            text(Res.string.msg_stop_minecraft_before_preparing_a_pack_update)
        }
        requireUser(instance.installationState is InstallationState.Installed) {
            text(Res.string.msg_finish_installing_this_instance_first)
        }
        mutableState.update {
            it.copy(
                operation = OperationStatus(
                    text(Res.string.msg_preparing_modpack_update),
                    project.name.asUserText(),
                    cancellable = true,
                    instanceId = instance.id,
                ),
            )
        }
        val preview = services.modpackInstaller.prepareUpdate(instance, project, version, ::updateResourceProgress)
        mutableState.update { it.copy(modpackUpdatePreview = preview) }
    }

    override fun applyModpackUpdate(replaceConflicts: Set<String>) {
        if (resourceJob?.isActive == true) return
        val preview = mutableState.value.modpackUpdatePreview ?: return
        resourceJob = scope.launch {
            mutableState.update {
                it.copy(operation = OperationStatus(text(Res.string.msg_applying_pack_update), instanceId = preview.original.id))
            }
            try {
                requireUser(mutableState.value.activeLaunch?.instanceId != preview.original.id) {
                    text(Res.string.msg_stop_minecraft_before_updating_this_pack)
                }
                val current = services.repository.get(preview.original.id)
                requireUser(current == preview.original) { text(Res.string.msg_the_instance_changed_after_the_preview_prepare) }
                withContext(NonCancellable) {
                    val updated = services.modpackUpdates.apply(preview, replaceConflicts)
                    services.repository.update(updated)
                    services.modpackUpdates.discardPreview(updated)
                    cachedLaunch = null
                    mutableState.update {
                        it.copy(
                            modpackUpdatePreview = null,
                            selectedId = updated.id,
                            canRollbackModpack = true,
                            notice = text(Res.string.msg_pack_updated_the_previous_files_are_backed),
                        )
                    }
                }
                refreshInstalledContent()
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error)
            } finally {
                mutableState.update { it.copy(operation = null) }
            }
        }
    }

    override fun cancelModpackUpdate() {
        if (resourceJob?.isActive == true) return
        val preview = mutableState.value.modpackUpdatePreview
        mutableState.update { it.copy(modpackUpdatePreview = null, suggestedPackInstances = emptyList()) }
        preview?.let { runCatching { services.modpackUpdates.discardPreview(it.original) }.onFailure(::showError) }
    }

    override fun rollbackModpackUpdate() {
        if (resourceJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        resourceJob = scope.launch {
            try {
                mutableState.update {
                    it.copy(operation = OperationStatus(text(Res.string.msg_restoring_pack_backup), instanceId = instance.id))
                }
                requireUser(mutableState.value.activeLaunch?.instanceId != instance.id) {
                    text(Res.string.msg_stop_minecraft_before_rolling_back_the_pack)
                }
                withContext(NonCancellable) {
                    services.repository.update(services.modpackUpdates.rollback(instance))
                    cachedLaunch = null
                    mutableState.update {
                        it.copy(canRollbackModpack = false, notice = text(Res.string.msg_the_previous_pack_version_was_restored))
                    }
                }
                refreshInstalledContent()
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error)
            } finally {
                mutableState.update { it.copy(operation = null) }
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
                val canRollback = services.modpackUpdates.canRollback(instance)
                mutableState.update { state ->
                    if (state.selectedId != instance.id) {
                        state
                    } else {
                        state.copy(
                            installedContent = content,
                            canRollbackModpack = canRollback,
                            installedContentUpdates = state.installedContentUpdates.filterKeys { key ->
                                content.any { it.key == key }
                            },
                            isLoadingInstalledContent = false,
                        )
                    }
                }
                val identified = services.contentIdentifier.identify(
                    instance,
                    content,
                    online = mutableState.value.launcherPreferences.content.trackMetadata,
                )
                currentCoroutineContext().ensureActive()
                mutableState.update { state ->
                    if (state.selectedId == instance.id) state.copy(installedContent = identified) else state
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
        if (contentUpdateCheckJob?.isActive == true) return
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.filter { it.direct && it.isTracked }
        contentUpdateCheckJob = scope.launch {
            mutableState.update { it.copy(isCheckingInstalledContentUpdates = true) }
            try {
                var failed = 0
                val updates = buildMap {
                    content.forEach { installed ->
                        try {
                            services.resourceInstaller.latestCompatibleVersion(instance, installed)?.let { version ->
                                put(installed.key, version)
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            failed++
                        }
                    }
                }
                mutableState.update { state ->
                    if (state.selectedId != instance.id) {
                        state
                    } else {
                        state.copy(
                            installedContentUpdates = updates,
                            isCheckingInstalledContentUpdates = false,
                            notice = when {
                                failed > 0 -> text(Res.string.msg_s_updates_found_s_items_could_not, updates.size, failed)
                                updates.isEmpty() -> text(Res.string.msg_installed_content_is_up_to_date)
                                updates.size == 1 -> text(Res.string.msg_one_content_update_is_available)
                                else -> text(Res.string.msg_s_content_updates_are_available, updates.size)
                            },
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error)
            } finally {
                // Also clears the flag when a refresh, a content change or a new selection cancels the check.
                mutableState.update { it.copy(isCheckingInstalledContentUpdates = false) }
            }
        }
    }

    override fun toggleInstalledContent(key: String) {
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.firstOrNull { it.key == key }?.takeIf { it.canManage } ?: return
        if (!startContentWrite()) return
        contentWriteJob = scope.launch {
            try {
                val enabled = !content.enabled
                if (services.resourceInstaller.setEnabled(instance, content, enabled)) {
                    mutableState.update {
                        it.copy(notice = text(if (enabled) Res.string.msg_s_was_enabled else Res.string.msg_s_was_disabled, content.name))
                    }
                    reloadInstalledContentIfSelected(instance)
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
                        title = text(Res.string.msg_updating_s, content.name),
                        detail = version.versionNumber.asUserText(),
                        cancellable = true,
                        cancelLabel = text(Res.string.ui_cancel),
                        instanceId = instance.id,
                    ),
                )
            }
            try {
                services.resourceInstaller.update(instance, content, version, ::updateResourceProgress)
                mutableState.update {
                    it.copy(
                        installedContentUpdates = if (it.selectedId ==
                            instance.id
                        ) {
                            it.installedContentUpdates - key
                        } else {
                            it.installedContentUpdates
                        },
                        operation = null,
                        notice = text(Res.string.msg_s_was_updated_to_s, content.name, version.versionNumber),
                    )
                }
                reloadInstalledContentIfSelected(instance)
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = text(Res.string.msg_content_update_cancelled)) }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                resourceJob = null
            }
        }
    }

    override fun removeInstalledContent(key: String) {
        val instance = mutableState.value.selectedInstance ?: return
        val content = mutableState.value.installedContent.firstOrNull { it.key == key }?.takeIf { it.canManage } ?: return
        if (!startContentWrite()) return
        contentWriteJob = scope.launch {
            try {
                if (services.resourceInstaller.uninstall(instance, content)) {
                    mutableState.update { it.copy(notice = text(Res.string.msg_s_was_removed, content.name)) }
                    reloadInstalledContentIfSelected(instance)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    /**
     * Content changes wait for each other but never for a background refresh: the refresh is cancelled
     * and the change reloads the list itself. Returns false when another change is still running.
     */
    private fun startContentWrite(): Boolean {
        if (contentWriteJob?.isActive == true) {
            mutableState.update { it.copy(notice = BUSY_NOTICE) }
            return false
        }
        installedContentJob?.cancel()
        contentUpdateCheckJob?.cancel()
        return true
    }

    /**
     * Reloads the content list after a change, with project names and tracking, unless the player has since
     * selected another instance. That instance's list must not be replaced with this one.
     */
    private fun reloadInstalledContentIfSelected(instance: GameInstance) {
        if (mutableState.value.selectedId == instance.id) refreshInstalledContent()
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
                    if (state.selectedId != instance.id) {
                        state
                    } else {
                        state.copy(gameData = inventory, isLoadingGameData = false)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isLoadingGameData = false) }
                showError(error)
            }
        }
    }

    override fun backupWorld(worldKey: String) = runGameDataOperation(text(Res.string.msg_backing_up_world)) { instance ->
        val backup = services.gameDataManager.backupWorld(instance, worldKey)
        text(Res.string.msg_created_s, backup.fileName)
    }

    override fun restoreWorldBackup(backupKey: String) = runGameDataOperation(text(Res.string.msg_restoring_world)) { instance ->
        val world = services.gameDataManager.restoreWorldBackup(instance, backupKey)
        text(Res.string.msg_restored_s, world.name)
    }

    override fun importWorld(fileName: String, bytes: ByteArray) = runGameDataOperation(text(Res.string.msg_importing_world)) { instance ->
        val world = services.gameDataManager.importWorld(instance, fileName, bytes)
        text(Res.string.msg_imported_s, world.name)
    }

    override fun copyWorld(worldKey: String) = runGameDataOperation(text(Res.string.msg_copying_world)) { instance ->
        val world = services.gameDataManager.copyWorld(instance, worldKey)
        text(Res.string.msg_created_s, world.name)
    }

    override fun renameWorld(worldKey: String, newName: String) {
        if (newName.isBlank()) return
        runGameDataOperation(text(Res.string.msg_renaming_world)) { instance ->
            val world = services.gameDataManager.renameWorld(instance, worldKey, newName)
            text(Res.string.msg_renamed_world_to_s, world.name)
        }
    }

    override fun resetWorldIcon(worldKey: String) = runGameDataOperation(text(Res.string.msg_resetting_world_icon)) { instance ->
        services.gameDataManager.resetWorldIcon(instance, worldKey)
        text(Res.string.msg_reset_the_world_icon)
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
        if (mutableState.value.gameData.worlds.none { it.key == worldKey }) return
        runGameDataOperation(text(Res.string.msg_deleting_world)) { instance ->
            services.gameDataManager.deleteWorld(instance, worldKey)
            text(Res.string.msg_deleted_s_existing_backups_were_kept, worldKey)
        }
    }

    override fun deleteScreenshot(screenshotKey: String) = runGameDataOperation(text(Res.string.msg_deleting_screenshot)) { instance ->
        services.gameDataManager.deleteScreenshot(instance, screenshotKey)
        text(Res.string.msg_deleted_s, screenshotKey)
    }

    override fun renameScreenshot(screenshotKey: String, newName: String) {
        if (newName.isBlank()) return
        runGameDataOperation(text(Res.string.msg_renaming_screenshot)) { instance ->
            val screenshot = services.gameDataManager.renameScreenshot(instance, screenshotKey, newName.trim())
            text(Res.string.msg_renamed_screenshot_to_s, screenshot.fileName)
        }
    }

    override fun toggleDataPack(worldKey: String, dataPackKey: String) {
        val pack = mutableState.value.gameData.worlds.firstOrNull { it.key == worldKey }
            ?.dataPacks?.firstOrNull { it.key == dataPackKey } ?: return
        runGameDataOperation(
            if (pack.enabled) text(Res.string.msg_disabling_data_pack) else text(Res.string.msg_enabling_data_pack),
        ) { instance ->
            services.gameDataManager.setDataPackEnabled(instance, worldKey, dataPackKey, !pack.enabled)
            text(if (pack.enabled) Res.string.msg_s_was_disabled else Res.string.msg_s_was_enabled, pack.fileName)
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

    override fun setServerResourcePacks(acceptTextures: Boolean?) {
        mutableState.update { it.copy(serverEditor = it.serverEditor.copy(acceptTextures = acceptTextures)) }
    }

    override fun saveServer() {
        val editor = mutableState.value.serverEditor
        val instance = mutableState.value.selectedInstance
        val ready = editor.visible && editor.name.isNotBlank() && editor.address.isNotBlank()
        if (!ready || instance == null || gameDataWriteJob?.isActive == true) return
        gameDataJob?.cancel()
        gameDataWriteJob = scope.launch {
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
                    val current = if (it.selectedId == instance.id) it.copy(gameData = inventory, isLoadingGameData = false) else it
                    current.copy(
                        serverEditor = ServerEditorState(),
                        notice = text(Res.string.msg_saved_s_to_the_minecraft_server_list, editor.name.trim()),
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

    override fun removeServer(serverKey: String) = runGameDataOperation(text(Res.string.msg_removing_server)) { instance ->
        val server = mutableState.value.gameData.servers.firstOrNull { it.key == serverKey }
        services.gameDataManager.removeServer(instance, serverKey)
        server?.name?.let { text(Res.string.msg_removed_s, it) } ?: text(Res.string.msg_removed_server)
    }

    override fun moveServer(serverKey: String, offset: Int) = runGameDataOperation(text(Res.string.msg_reordering_servers)) { instance ->
        services.gameDataManager.moveServer(instance, serverKey, offset)
        text(Res.string.msg_updated_the_server_order)
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
                    if (state.selectedId != instance.id || state.selectedInstanceLogKey != logKey) {
                        state
                    } else {
                        state.copy(selectedInstanceLogText = text, isLoadingInstanceLog = false)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isLoadingInstanceLog = false) }
                showError(error)
            }
        }
    }

    override fun deleteInstanceLog(logKey: String) = runGameDataOperation(text(Res.string.msg_deleting_log)) { instance ->
        services.gameDataManager.deleteLog(instance, logKey)
        mutableState.update { state ->
            if (state.selectedInstanceLogKey == logKey) {
                state.copy(selectedInstanceLogKey = null, selectedInstanceLogText = "")
            } else {
                state
            }
        }
        text(Res.string.msg_deleted_s_2, logKey.substringAfterLast('/'))
    }

    override fun saveInstanceNotes(value: String) {
        val instance = mutableState.value.selectedInstance ?: return
        scope.launch {
            runCatching { services.repository.update(instance.copy(notes = value.trimEnd())) }
                .onSuccess { mutableState.update { it.copy(notice = text(Res.string.msg_saved_notes_for_s, instance.displayName)) } }
                .onFailure(::showError)
        }
    }

    private fun runGameDataOperation(title: UserText, operation: suspend (GameInstance) -> UserText) {
        val instance = mutableState.value.selectedInstance ?: return
        if (gameDataWriteJob?.isActive == true) {
            mutableState.update { it.copy(notice = BUSY_NOTICE) }
            return
        }
        // A refresh only reads, so it is safe to stop; this operation reloads the inventory itself.
        gameDataJob?.cancel()
        gameDataWriteJob = scope.launch {
            mutableState.update { it.copy(operation = OperationStatus(title, instance.displayName.asUserText())) }
            try {
                val notice = operation(instance)
                val inventory = services.gameDataManager.inventory(instance)
                mutableState.update {
                    val current = if (it.selectedId == instance.id) it.copy(gameData = inventory, isLoadingGameData = false) else it
                    current.copy(operation = null, notice = notice)
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(operation = null) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            } finally {
                gameDataWriteJob = null
            }
        }
    }

    override fun deleteSelected() {
        val id = mutableState.value.selectedInstance?.id ?: return
        if (mutableState.value.activeLaunch?.instanceId == id) {
            mutableState.update { it.copy(error = text(Res.string.msg_stop_minecraft_before_removing_this_instance)) }
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
            mutableState.update { it.copy(error = text(Res.string.msg_stop_minecraft_before_moving_this_instance_to)) }
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
                    checkUser(movePathToTrash(removed.instanceDirectory)) { text(Res.string.msg_the_instance_could_not_be_moved_to) }
                }
                services.repository.delete(id)
                mutableState.update {
                    it.copy(
                        selectedId = null,
                        pendingInstanceRemovalId = null,
                        notice = if (removalMode == InstanceRemovalMode.MOVE_TO_TRASH) {
                            text(Res.string.msg_s_was_moved_to_trash, removed.displayName)
                        } else {
                            INSTANCE_REMOVED_NOTICE
                        },
                        launchPlan = null,
                        removedInstanceUndo = removed.takeIf {
                            removalMode == InstanceRemovalMode.LIBRARY_ONLY
                        },
                    )
                }
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (error: CancellationException) {
                throw error
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
                it.copy(operation = OperationStatus(text(Res.string.msg_deleting_instance), removed.displayName.asUserText()))
            }
            try {
                services.repository.deleteWithFiles(id)
                mutableState.update {
                    it.copy(
                        selectedId = null,
                        pendingInstanceRemovalId = null,
                        operation = null,
                        notice = text(Res.string.msg_s_and_its_files_were_deleted, removed.displayName),
                        launchPlan = null,
                        removedInstanceUndo = null,
                        installedContent = emptyList(),
                        installedContentUpdates = emptyMap(),
                    )
                }
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (error: CancellationException) {
                throw error
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
                            notice = text(Res.string.msg_s_was_restored_to_the_library, instance.displayName),
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update { it.copy(removedInstanceUndo = null) }
                    showError(error)
                }
        }
    }

    override fun queueLocalFileImport(fileName: String, bytes: ByteArray, type: ResourceType?, sourceOrigin: String?) {
        if (mutableState.value.localFileImport.visible || resourceJob?.isActive == true) {
            queuedLocalFiles.addLast(QueuedLocalFile(fileName, bytes, type, sourceOrigin))
        } else {
            openLocalFileImport(fileName, bytes, type, sourceOrigin)
        }
    }

    private fun openLocalFileImport(fileName: String, bytes: ByteArray, type: ResourceType?, sourceOrigin: String?) {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val allowedTypes = when (extension) {
            "jar" -> listOf(ResourceType.MOD)
            "mrpack" -> listOf(ResourceType.MODPACK)
            "zip" -> listOf(ResourceType.RESOURCE_PACK, ResourceType.SHADER_PACK, ResourceType.MODPACK)
            else -> emptyList()
        }
        if (allowedTypes.isEmpty()) {
            mutableState.update { it.copy(error = text(Res.string.msg_choose_a_jar_zip_or_mrpack_file)) }
            return
        }
        if (bytes.isEmpty()) {
            mutableState.update { it.copy(error = text(Res.string.msg_s_is_empty, fileName)) }
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
        mutableState.update { it.copy(error = text(Res.string.msg_s_could_not_be_read, fileName)) }
    }

    override fun reportLocalFileTooLarge(fileName: String) {
        mutableState.update { it.copy(error = text(Res.string.msg_s_is_larger_than_the_mib_import, fileName)) }
    }

    override fun setLocalFileImportType(type: ResourceType) {
        mutableState.update { state ->
            val pending = state.localFileImport
            if (type !in pending.allowedTypes) {
                state
            } else {
                state.copy(localFileImport = pending.copy(selectedType = type))
            }
        }
    }

    override fun cancelLocalFileImport() {
        mutableState.update { it.copy(localFileImport = LocalFileImportState()) }
        showNextQueuedLocalFile()
    }

    private fun showNextQueuedLocalFile() {
        val next = queuedLocalFiles.removeFirstOrNull() ?: return
        queueLocalFileImport(next.fileName, next.bytes, next.type, next.sourceOrigin)
    }

    override fun confirmLocalFileImport() = runLocalFileImport(mutableState.value.localFileImport)

    private fun runLocalFileImport(pending: LocalFileImportState) {
        val type = pending.selectedType
        if (resourceJob?.isActive == true || type == null) return
        val target = pending.targetInstanceId?.let { id -> mutableState.value.instances.firstOrNull { it.id == id } }
        if (type != ResourceType.MODPACK && target?.installationState !is InstallationState.Installed) {
            mutableState.update {
                it.copy(error = text(Res.string.msg_install_the_target_instance_before_adding_local))
            }
            return
        }
        resourceJob = scope.launch {
            mutableState.update {
                it.copy(
                    localFileImport = LocalFileImportState(),
                    operation = OperationStatus(
                        title = if (type ==
                            ResourceType.MODPACK
                        ) {
                            text(Res.string.msg_importing_modpack)
                        } else {
                            text(Res.string.msg_adding_local_file)
                        },
                        detail = pending.fileName.asUserText(),
                        cancellable = true,
                        cancelLabel = text(Res.string.ui_cancel),
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
                    text(Res.string.msg_s_was_imported, created.displayName)
                } else {
                    val instance = requireNotNull(target)
                    services.resourceInstaller.installLocal(instance, pending.fileName, pending.bytes, type)
                    text(Res.string.msg_s_was_added_to_s, pending.fileName, instance.displayName)
                }
                mutableState.update { it.copy(operation = null, notice = notice) }
                refreshInstalledContent()
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null, notice = text(Res.string.msg_local_file_import_cancelled)) }
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error, resume = { runLocalFileImport(pending) })
            } finally {
                resourceJob = null
                showNextQueuedLocalFile()
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

    override fun openInstanceSettings(inline: Boolean) {
        val instance = mutableState.value.selectedInstance ?: return
        val recommendation = LaunchTuningAdvisor.recommend(instance, services.systemProfile)
        mutableState.update {
            it.copy(
                instanceSettings = InstanceSettingsState(
                    inline = inline,
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
                    recommendation = text(Res.string.msg_recommended_maximum_s_mib, recommendation.memory.maximumMiB),
                    warnings = recommendation.warnings,
                ).let { form -> form.copy(initial = form) },
            )
        }
        scope.launch {
            try {
                val clientSettings = services.repository.readClientSettings(instance.id)
                mutableState.update { state ->
                    val form = state.instanceSettings
                    if (!form.visible || form.instanceId != instance.id) {
                        state
                    } else {
                        state.copy(
                            instanceSettings = form.copy(
                                clientSettings = clientSettings,
                                initial = form.initial?.copy(clientSettings = clientSettings),
                                isLoadingClientSettings = false,
                                clientSettingsError = if (clientSettings == null) {
                                    text(Res.string.msg_client_settings_are_unavailable_for_minecraft_s, instance.minecraftVersionId)
                                } else {
                                    null
                                },
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { state ->
                    val form = state.instanceSettings
                    if (!form.visible || form.instanceId != instance.id) {
                        state
                    } else {
                        state.copy(
                            instanceSettings = form.copy(
                                isLoadingClientSettings = false,
                                clientSettingsError = error.userText(text(Res.string.msg_client_settings_could_not_be_loaded)),
                            ),
                        )
                    }
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
        ) {
            return
        }
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
                            text(Res.string.msg_instance_settings_saved_install_the_new_game)
                        } else if (review.ignored.isNotEmpty()) {
                            text(Res.string.msg_instance_settings_saved_ignored_jvm_options, review.ignored.joinToString(" "))
                        } else {
                            text(Res.string.msg_instance_settings_saved)
                        },
                    )
                }
                checkLaunchReadiness(updated)
                refreshInstalledContent()
            } catch (error: CancellationException) {
                throw error
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
        // Only a running sign-in owns the operation bar. Otherwise it belongs to another task, such as an install.
        val signInRunning = accountLoginJob?.isActive == true
        accountLoginJob?.cancel()
        accountLoginJob = null
        mutableState.update {
            it.copy(accountLogin = AccountLoginState(), operation = if (signInRunning) null else it.operation)
        }
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
                    accountLogin = form.copy(isWaiting = true, error = null),
                    operation = OperationStatus(
                        if (form.method == AccountAuthenticationMethod.OFFLINE) {
                            text(Res.string.msg_adding_offline_account)
                        } else {
                            text(Res.string.msg_authenticating_account)
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
                        notice = when {
                            form.method == AccountAuthenticationMethod.OFFLINE ->
                                text(Res.string.msg_offline_account_added_and_set_as_active)

                            form.edition == MinecraftEdition.BEDROCK -> text(Res.string.msg_bedrock_account_added_it_can_t_launch)

                            else -> text(Res.string.msg_account_added_and_set_as_active)
                        },
                    )
                }
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (_: CancellationException) {
                mutableState.update { it.copy(operation = null) }
            } catch (error: Exception) {
                // Keep the failure in the dialog and drop the stale code, so the player can try again.
                mutableState.update {
                    it.copy(
                        accountLogin = it.accountLogin.copy(
                            isWaiting = false,
                            authorization = null,
                            error = error.userText(text(Res.string.msg_sign_in_failed_try_again)),
                        ),
                        operation = null,
                    )
                }
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
            mutableState.update { it.copy(operation = OperationStatus(text(Res.string.msg_refreshing_account_profile))) }
            try {
                val session = services.accounts.currentSession()
                    ?: userError(text(Res.string.msg_the_selected_account_needs_to_sign_in))
                val existing = mutableState.value.accounts.firstOrNull { it.profile.profileId == session.profileId }?.profile
                val profile = services.profileClient.fetchProfile(session).copy(
                    authenticationMethod = existing?.authenticationMethod
                        ?: AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                    lastAuthenticatedAtEpochMillis = existing?.lastAuthenticatedAtEpochMillis,
                )
                services.accounts.updateProfile(profile)
                mutableState.update { it.copy(operation = null, notice = text(Res.string.msg_account_profile_refreshed)) }
                cachedLaunch = null
                checkLaunchReadiness(mutableState.value.selectedInstance)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(operation = null) }
                showError(error)
            }
        }
    }

    override fun resetActiveSkin() {
        scope.launch {
            mutableState.update { it.copy(operation = OperationStatus(text(Res.string.msg_resetting_active_skin))) }
            try {
                val session = services.accounts.currentSession()
                    ?: userError(text(Res.string.msg_the_selected_account_needs_to_sign_in))
                val profile = services.profileClient.resetActiveSkin(session)
                val existing = mutableState.value.accounts.firstOrNull { it.profile.profileId == session.profileId }?.profile
                services.accounts.updateProfile(
                    profile.copy(
                        authenticationMethod = existing?.authenticationMethod
                            ?: AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                        lastAuthenticatedAtEpochMillis = existing?.lastAuthenticatedAtEpochMillis,
                    ),
                )
                mutableState.update { it.copy(operation = null, notice = text(Res.string.msg_the_active_skin_was_reset)) }
            } catch (error: CancellationException) {
                throw error
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
        scope.launch {
            val name = text(Res.string.ui_player_skin_name, account.profile.playerName).load(isolateArguments = false)
            openSkinEditorForCurrentSkin(account, texture, name)
        }
    }

    private fun openSkinEditorForCurrentSkin(account: ManagedAccount, texture: ByteArray, name: String) {
        mutableState.update {
            it.copy(
                skinStudio = it.skinStudio.copy(
                    editor = SkinEditorState(
                        visible = true,
                        name = name,
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
                        error = error?.userText(text(Res.string.ui_trestle_could_not_read_that_image)),
                    ),
                ),
            )
        }
    }

    override fun reportSkinFileReadFailure() {
        mutableState.update {
            it.copy(
                skinStudio = it.skinStudio.copy(
                    editor = it.skinStudio.editor.copy(error = text(Res.string.msg_the_selected_skin_file_could_not_be)),
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
                    operation = OperationStatus(
                        if (useAfterSave) text(Res.string.msg_saving_and_applying_skin) else text(Res.string.msg_saving_skin),
                    ),
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
                        notice = if (useAfterSave) {
                            text(
                                Res.string.msg_s_is_now_your_active_skin,
                                saved.profile.name,
                            )
                        } else {
                            text(Res.string.msg_skin_saved)
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        skinStudio = it.skinStudio.copy(
                            editor = it.skinStudio.editor.copy(
                                isSaving = false,
                                error = error.userText(text(Res.string.msg_skin_could_not_be_saved)),
                            ),
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
            mutableState.update { it.copy(operation = OperationStatus(text(Res.string.msg_applying_s, saved.profile.name)), error = null) }
            try {
                applySkin(saved)
                mutableState.update {
                    it.copy(operation = null, notice = text(Res.string.msg_s_is_now_your_active_skin, saved.profile.name))
                }
            } catch (error: CancellationException) {
                throw error
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
                .onSuccess { mutableState.update { it.copy(notice = text(Res.string.msg_skin_removed_from_the_local_library)) } }
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
                    // Settings take effect where the player is looking, so a confirmation snackbar would only add noise.
                    state.copy(themePreference = value.theme, launcherPreferences = value)
                }
                services.configurePreferences(value)
            }
            .onFailure(::showError)
    }

    override fun checkForLauncherUpdate() = checkLauncherUpdate(automatic = false)

    override fun remindAboutLauncherUpdateLater() {
        val preferences = mutableState.value.launcherPreferences
        val next = preferences.copy(
            updates = preferences.updates.copy(
                remindAfterMillis = services.clock.nowMillis() + 24 * 60 * 60 * 1000L,
            ),
        )
        runCatching {
            services.preferences.write(next)
            mutableState.update { it.copy(launcherPreferences = next, availableUpdate = null) }
        }.onFailure(::showError)
    }

    private fun checkLauncherUpdate(automatic: Boolean) {
        if (updateJob?.isActive == true) return
        val preferences = mutableState.value.launcherPreferences.updates
        val now = services.clock.nowMillis()
        if (automatic && (
                !preferences.automaticChecks || now < preferences.remindAfterMillis ||
                    (now >= preferences.lastCheckedAtMillis && now - preferences.lastCheckedAtMillis < 24 * 60 * 60 * 1000L)
                )
        ) {
            return
        }
        updateJob = scope.launch {
            mutableState.update { it.copy(isCheckingForUpdate = true) }
            try {
                val update = services.updateChecker.availableUpdate(includePrereleases = preferences.includePrereleases)
                // Merge with current settings so a slow request cannot overwrite edits made during the check.
                val latest = mutableState.value.launcherPreferences
                val saved = latest.copy(updates = latest.updates.copy(lastCheckedAtMillis = now))
                services.preferences.write(saved)
                mutableState.update {
                    it.copy(
                        launcherPreferences = saved,
                        availableUpdate = update.takeIf { !automatic || saved.updates.automaticChecks },
                        notice = if (automatic) {
                            it.notice
                        } else if (update ==
                            null
                        ) {
                            text(Res.string.msg_no_newer_trestle_release_is_available)
                        } else {
                            text(Res.string.msg_trestle_s_is_available, update.version)
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!automatic) showError(error)
                // Offline startup must not interrupt navigation. Retry on the next hourly check.
            } finally {
                mutableState.update { it.copy(isCheckingForUpdate = false) }
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
            ?: userError(text(Res.string.msg_the_selected_account_needs_to_sign_in))
        val existing = mutableState.value.accounts.firstOrNull { it.profile.profileId == session.profileId }?.profile
            ?: userError(text(Res.string.msg_the_selected_account_is_not_available))
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

    override fun reloadAccountSkins() {
        mutableState.update { it.copy(failedSkinTextures = emptySet()) }
        loadAccountSkinTextures(mutableState.value.accounts)
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
                                state.copy(
                                    accountSkinTextures = state.accountSkinTextures + (profileId to texture),
                                    failedSkinTextures = state.failedSkinTextures - profileId,
                                )
                            }
                        }
                    }
                    .onFailure {
                        if (loadedSkinUrls[profileId] == skin.url) loadedSkinUrls.remove(profileId)
                        mutableState.update { state -> state.copy(failedSkinTextures = state.failedSkinTextures + profileId) }
                    }
            }
        }
    }

    private fun searchResources(append: Boolean) {
        val browser = mutableState.value.resourceBrowser
        val platform = services.resourcePlatforms.platform(browser.provider)
        if (!platform.isAvailable || !platform.supports(browser.type)) return
        resourceSearchJob?.cancel()
        if (!append) resourceVersionsJob?.cancel()
        resourceSearchJob = scope.launch {
            mutableState.update { it.copy(resourceBrowser = it.resourceBrowser.startingSearch(append)) }
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
                        // The category list only exists for Modrinth. Keep the choice for when the player switches back.
                        category = browser.categoryFilter.ifBlank { null }
                            ?.takeIf { resourceCategories(browser.provider, browser.type).isNotEmpty() },
                        sort = browser.sort,
                        offset = if (append) browser.projects.size else 0,
                    ),
                )
                mutableState.update { state ->
                    val current = state.resourceBrowser
                    state.copy(
                        resourceBrowser = current.copy(
                            projects = if (append) current.projects + result.projects else result.projects,
                            totalProjects = result.total,
                            isSearching = false,
                            error = null,
                        ),
                    )
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    resourceBrowser = mutableState.value.resourceBrowser.copy(
                        isSearching = false,
                        error = error.userText(text(Res.string.msg_resources_could_not_be_loaded)),
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
                    LaunchStatus.Failed(error.userText(text(Res.string.msg_the_launch_check_failed))),
                )
            } finally {
                clearRuntimePreparation()
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

    private fun supportedLoaders(): Set<ModLoader> = services.runtime.capabilities.supportedModLoaders ?: ModLoader.entries.toSet()

    /**
     * Shows runtime preparation in the operation bar only when the bar is free, so a background launch check never
     * hides another task's progress and Cancel button.
     */
    private fun updateRuntimePreparation(progress: RuntimePreparationProgress) {
        mutableState.update {
            val current = it.operation
            if (current != null && current !== runtimePreparationOperation) return@update it
            val next = OperationStatus(
                title = text(Res.string.msg_preparing_android_runtime),
                detail = progress.stage,
                completed = progress.completedBytes,
                total = progress.totalBytes,
                completedItems = progress.completedItems,
                totalItems = progress.totalItems,
            )
            runtimePreparationOperation = next
            it.copy(operation = next)
        }
    }

    private fun clearRuntimePreparation() {
        val owned = runtimePreparationOperation ?: return
        runtimePreparationOperation = null
        mutableState.update { if (it.operation === owned) it.copy(operation = null) else it }
    }

    private fun updateLaunch(id: InstanceId, status: LaunchStatus, notice: UserText? = null, activeEvent: Boolean = false) {
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

    /**
     * Shows [error]. When it lists files the player must download from their publishers, [resume] restarts
     * the same operation once every file is verified, so the player never has to retry by hand.
     */
    private fun showError(error: Throwable, recovery: ErrorRecoveryAction? = null, resume: (() -> Unit)? = null) {
        if (error is RestrictedDownloadRequired) {
            restrictedDownloadRetry = resume
            mutableState.update { it.copy(restrictedDownloads = error.downloads, verifiedRestrictedDownloads = emptySet()) }
        }
        mutableState.update {
            it.copy(
                error = error.userText(text(Res.string.msg_the_operation_failed)),
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
        requireUser(separator > 0) { text(Res.string.msg_environment_variable_line_s_must_use_name, index + 1) }
        val name = line.substring(0, separator).trim()
        requireUser(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { text(Res.string.msg_environment_variable_name_s_is_invalid, name) }
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
    requireUser(quote == null) { text(Res.string.msg_launch_arguments_contain_an_unmatched_quote) }
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
        if (edition == MinecraftEdition.BEDROCK && bedrockGameVersion.isBlank()) {
            null
        } else {
            AccountLoginRequest.DeviceCode(method, bedrockGameVersion.takeIf(String::isNotBlank))
        }
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
    -> if (importedSecret.isBlank()) {
        null
    } else {
        AccountLoginRequest.SecretImport(
            method,
            SecretValue(importedSecret.reveal()),
        )
    }

    AccountAuthenticationMethod.OFFLINE -> offlineUsername.trim().takeIf(String::isNotBlank)
        ?.let(AccountLoginRequest::Offline)
}
