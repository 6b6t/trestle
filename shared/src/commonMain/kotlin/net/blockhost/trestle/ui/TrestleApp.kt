@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.InstanceSortMode
import net.blockhost.trestle.app.LauncherProxyType
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.MinecraftNarratorMode
import net.blockhost.trestle.instance.MinecraftParticleSetting
import net.blockhost.trestle.instance.ServerStatus
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.DependencyKind
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.ReleaseChannel
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.ic_account
import net.blockhost.trestle.resources.ic_add
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_close
import net.blockhost.trestle.resources.ic_extension
import net.blockhost.trestle.resources.ic_library
import net.blockhost.trestle.resources.ic_more_vert
import net.blockhost.trestle.resources.ic_settings
import net.blockhost.trestle.resources.ic_visibility
import net.blockhost.trestle.resources.ic_visibility_off
import net.blockhost.trestle.resources.ui_accounts
import net.blockhost.trestle.resources.ui_active
import net.blockhost.trestle.resources.ui_add_account
import net.blockhost.trestle.resources.ui_add_file
import net.blockhost.trestle.resources.ui_add_server
import net.blockhost.trestle.resources.ui_additional_game_arguments
import net.blockhost.trestle.resources.ui_additional_jvm_arguments
import net.blockhost.trestle.resources.ui_address
import net.blockhost.trestle.resources.ui_android_runtime
import net.blockhost.trestle.resources.ui_any_category
import net.blockhost.trestle.resources.ui_any_version
import net.blockhost.trestle.resources.ui_appearance
import net.blockhost.trestle.resources.ui_apply
import net.blockhost.trestle.resources.ui_apply_filters
import net.blockhost.trestle.resources.ui_apply_these_defaults
import net.blockhost.trestle.resources.ui_atlauncher
import net.blockhost.trestle.resources.ui_audio
import net.blockhost.trestle.resources.ui_availability_is_controlled_by_the_trestle_build_api_key
import net.blockhost.trestle.resources.ui_available_versions_and_installation_details_will_appear_here
import net.blockhost.trestle.resources.ui_available_without_an_api_key
import net.blockhost.trestle.resources.ui_back
import net.blockhost.trestle.resources.ui_back_to_library
import net.blockhost.trestle.resources.ui_back_to_results
import net.blockhost.trestle.resources.ui_browse
import net.blockhost.trestle.resources.ui_browse_content
import net.blockhost.trestle.resources.ui_browse_modpacks
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_category
import net.blockhost.trestle.resources.ui_change
import net.blockhost.trestle.resources.ui_change_the_search_or_content_type
import net.blockhost.trestle.resources.ui_choose_a_skin_file
import net.blockhost.trestle.resources.ui_choose_how_trestle_should_use_this_zip_file
import net.blockhost.trestle.resources.ui_choose_one_from_the_library
import net.blockhost.trestle.resources.ui_clear
import net.blockhost.trestle.resources.ui_client_defaults
import net.blockhost.trestle.resources.ui_client_id
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_color_warnings_and_errors
import net.blockhost.trestle.resources.ui_console
import net.blockhost.trestle.resources.ui_content
import net.blockhost.trestle.resources.ui_controls_and_accessibility
import net.blockhost.trestle.resources.ui_copy_seed
import net.blockhost.trestle.resources.ui_crash_report_value
import net.blockhost.trestle.resources.ui_create_an_isolated_minecraft_instance
import net.blockhost.trestle.resources.ui_create_instance
import net.blockhost.trestle.resources.ui_creating
import net.blockhost.trestle.resources.ui_current
import net.blockhost.trestle.resources.ui_curseforge
import net.blockhost.trestle.resources.ui_curseforge_blocks_this_file_trestle_will_look_for_the_identical_file_on_
import net.blockhost.trestle.resources.ui_curseforge_requires_a_trestle_api_key_configured_by_the_application_buil
import net.blockhost.trestle.resources.ui_custom
import net.blockhost.trestle.resources.ui_custom_commands
import net.blockhost.trestle.resources.ui_custom_java_executable
import net.blockhost.trestle.resources.ui_delete
import net.blockhost.trestle.resources.ui_delete_named_world
import net.blockhost.trestle.resources.ui_delete_world
import net.blockhost.trestle.resources.ui_direct_download_or_curseforge_url
import net.blockhost.trestle.resources.ui_discover
import net.blockhost.trestle.resources.ui_drag_to_rotate
import net.blockhost.trestle.resources.ui_edit
import net.blockhost.trestle.resources.ui_edit_instance_settings
import net.blockhost.trestle.resources.ui_enter_an_instance_name
import net.blockhost.trestle.resources.ui_enter_one_name_value_pair_per_line_lines_starting_with_are_ignored
import net.blockhost.trestle.resources.ui_enter_this_code
import net.blockhost.trestle.resources.ui_environment_variables
import net.blockhost.trestle.resources.ui_events_from_this_session_right_click_an_entry_to_copy_diagnostics
import net.blockhost.trestle.resources.ui_existing_ftb_app_library
import net.blockhost.trestle.resources.ui_file_name
import net.blockhost.trestle.resources.ui_filter_named_field
import net.blockhost.trestle.resources.ui_filters
import net.blockhost.trestle.resources.ui_find_in_log
import net.blockhost.trestle.resources.ui_folder_changes_apply_after_trestle_restarts
import net.blockhost.trestle.resources.ui_folders
import net.blockhost.trestle.resources.ui_follow_launch
import net.blockhost.trestle.resources.ui_for_example_1_21_100
import net.blockhost.trestle.resources.ui_for_example_play_example_net_25565
import net.blockhost.trestle.resources.ui_forget
import net.blockhost.trestle.resources.ui_forget_account
import net.blockhost.trestle.resources.ui_forget_account_2
import net.blockhost.trestle.resources.ui_ftb_app_instances_folder
import net.blockhost.trestle.resources.ui_game_components
import net.blockhost.trestle.resources.ui_game_console
import net.blockhost.trestle.resources.ui_general
import net.blockhost.trestle.resources.ui_group
import net.blockhost.trestle.resources.ui_homepage
import net.blockhost.trestle.resources.ui_http_timeout_and_proxy_changes_apply_to_new_connections_after_restart
import net.blockhost.trestle.resources.ui_https_or_curseforge
import net.blockhost.trestle.resources.ui_icon_path_or_url
import net.blockhost.trestle.resources.ui_identity
import net.blockhost.trestle.resources.ui_import
import net.blockhost.trestle.resources.ui_import_a_64x64_or_legacy_64x32_png_to_start_your_local_library
import net.blockhost.trestle.resources.ui_import_ftb_app_instances
import net.blockhost.trestle.resources.ui_import_local_file
import net.blockhost.trestle.resources.ui_import_world
import net.blockhost.trestle.resources.ui_inspect_launch_plan
import net.blockhost.trestle.resources.ui_install
import net.blockhost.trestle.resources.ui_installed_bedrock_version
import net.blockhost.trestle.resources.ui_installed_content
import net.blockhost.trestle.resources.ui_instance
import net.blockhost.trestle.resources.ui_instance_logs
import net.blockhost.trestle.resources.ui_instance_settings
import net.blockhost.trestle.resources.ui_instance_sorting
import net.blockhost.trestle.resources.ui_instance_version_loader
import net.blockhost.trestle.resources.ui_instances
import net.blockhost.trestle.resources.ui_join
import net.blockhost.trestle.resources.ui_keep_files
import net.blockhost.trestle.resources.ui_keyboard_shortcuts
import net.blockhost.trestle.resources.ui_language
import net.blockhost.trestle.resources.ui_launch
import net.blockhost.trestle.resources.ui_launch_plan
import net.blockhost.trestle.resources.ui_launcher_log
import net.blockhost.trestle.resources.ui_leave_blank_to_group_this_instance_by_loader
import net.blockhost.trestle.resources.ui_leave_blank_to_use_trestles_managed_mojang_runtime
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_loader
import net.blockhost.trestle.resources.ui_loading_client_settings
import net.blockhost.trestle.resources.ui_logs
import net.blockhost.trestle.resources.ui_manage
import net.blockhost.trestle.resources.ui_manage_content
import net.blockhost.trestle.resources.ui_manage_skins
import net.blockhost.trestle.resources.ui_memory_classpath_native_path_and_architecture_options_are_managed_by_tre
import net.blockhost.trestle.resources.ui_microsoft_account_email
import net.blockhost.trestle.resources.ui_microsoft_account_password
import net.blockhost.trestle.resources.ui_minecraft_client
import net.blockhost.trestle.resources.ui_minecraft_version
import net.blockhost.trestle.resources.ui_modrinth
import net.blockhost.trestle.resources.ui_mods_and_modpacks
import net.blockhost.trestle.resources.ui_more
import net.blockhost.trestle.resources.ui_must_exit_successfully_before_minecraft_starts
import net.blockhost.trestle.resources.ui_name
import net.blockhost.trestle.resources.ui_network
import net.blockhost.trestle.resources.ui_new
import net.blockhost.trestle.resources.ui_new_instance
import net.blockhost.trestle.resources.ui_new_skin
import net.blockhost.trestle.resources.ui_no_accounts
import net.blockhost.trestle.resources.ui_no_instance_selected
import net.blockhost.trestle.resources.ui_no_instances_yet
import net.blockhost.trestle.resources.ui_no_launcher_events_in_this_session
import net.blockhost.trestle.resources.ui_no_matching_instances
import net.blockhost.trestle.resources.ui_no_results
import net.blockhost.trestle.resources.ui_no_saved_skins
import net.blockhost.trestle.resources.ui_notes
import net.blockhost.trestle.resources.ui_offline_username
import net.blockhost.trestle.resources.ui_only_vanilla_is_supported_on_android
import net.blockhost.trestle.resources.ui_open
import net.blockhost.trestle.resources.ui_open_manual_download
import net.blockhost.trestle.resources.ui_open_operation_details
import net.blockhost.trestle.resources.ui_open_microsoft_sign_in
import net.blockhost.trestle.resources.ui_open_version_release
import net.blockhost.trestle.resources.ui_optional
import net.blockhost.trestle.resources.ui_optional_dependencies
import net.blockhost.trestle.resources.ui_optional_some_private_or_rate_limited_technic_packs_require_it_applies_a
import net.blockhost.trestle.resources.ui_overview
import net.blockhost.trestle.resources.ui_password
import net.blockhost.trestle.resources.ui_pause
import net.blockhost.trestle.resources.ui_play
import net.blockhost.trestle.resources.ui_player_model
import net.blockhost.trestle.resources.ui_port
import net.blockhost.trestle.resources.ui_post_exit_command
import net.blockhost.trestle.resources.ui_pre_launch_command
import net.blockhost.trestle.resources.ui_proxy
import net.blockhost.trestle.resources.ui_proxy_credentials_are_stored_in_the_launcher_preferences_file
import net.blockhost.trestle.resources.ui_proxy_settings_apply_to_trestle_minecraft_does_not_use_them
import net.blockhost.trestle.resources.ui_quoted_values_and_escaped_characters_are_preserved
import net.blockhost.trestle.resources.ui_reading_installed_content
import net.blockhost.trestle.resources.ui_reading_the_game_directory
import net.blockhost.trestle.resources.ui_refresh
import net.blockhost.trestle.resources.ui_reload
import net.blockhost.trestle.resources.ui_remove
import net.blockhost.trestle.resources.ui_remove_named_skin
import net.blockhost.trestle.resources.ui_remove_skin
import net.blockhost.trestle.resources.ui_rename
import net.blockhost.trestle.resources.ui_rename_screenshot
import net.blockhost.trestle.resources.ui_rename_world
import net.blockhost.trestle.resources.ui_reset_to_default
import net.blockhost.trestle.resources.ui_restore_copy
import net.blockhost.trestle.resources.ui_resume_install
import net.blockhost.trestle.resources.ui_retry
import net.blockhost.trestle.resources.ui_retry_install
import net.blockhost.trestle.resources.ui_retry_launch
import net.blockhost.trestle.resources.ui_return_to_library
import net.blockhost.trestle.resources.ui_runs_after_minecraft_exits
import net.blockhost.trestle.resources.ui_runs_before_the_java_executable_for_example_gamescope
import net.blockhost.trestle.resources.ui_runtime
import net.blockhost.trestle.resources.ui_save
import net.blockhost.trestle.resources.ui_save_changes
import net.blockhost.trestle.resources.ui_save_notes
import net.blockhost.trestle.resources.ui_save_to_library
import net.blockhost.trestle.resources.ui_search_instances
import net.blockhost.trestle.resources.ui_search_mods_packs_and_shaders
import net.blockhost.trestle.resources.ui_seed_value
import net.blockhost.trestle.resources.ui_select_a_result
import net.blockhost.trestle.resources.ui_select_an_instance
import net.blockhost.trestle.resources.ui_server_status
import net.blockhost.trestle.resources.ui_servers
import net.blockhost.trestle.resources.ui_services
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_sign_out
import net.blockhost.trestle.resources.ui_skin_file
import net.blockhost.trestle.resources.ui_skins
import net.blockhost.trestle.resources.ui_stop
import net.blockhost.trestle.resources.ui_supports_modrinth_curseforge_prism_and_multimc_pack_archives
import net.blockhost.trestle.resources.ui_tasks_and_downloads
import net.blockhost.trestle.resources.ui_technic
import net.blockhost.trestle.resources.ui_the_author_blocks_downloads_from_third_party_launchers
import net.blockhost.trestle.resources.ui_this_content_type_cannot_be_installed_into_an_instance_yet
import net.blockhost.trestle.resources.ui_this_deletes_the_local_skin_profile_and_its_png_your_active_minecraft_sk
import net.blockhost.trestle.resources.ui_this_permanently_deletes_the_world_directory_existing_zip_backups_remain
import net.blockhost.trestle.resources.ui_this_removes_the_local_profile_and_saved_credentials_it_does_not_change_
import net.blockhost.trestle.resources.ui_tools
import net.blockhost.trestle.resources.ui_trestle
import net.blockhost.trestle.resources.ui_try_another_name_version_or_loader
import net.blockhost.trestle.resources.ui_unavailable
import net.blockhost.trestle.resources.ui_undo
import net.blockhost.trestle.resources.ui_update
import net.blockhost.trestle.resources.ui_use
import net.blockhost.trestle.resources.ui_use_a_64x64_png_or_a_legacy_64x32_skin
import net.blockhost.trestle.resources.ui_use_recommended
import net.blockhost.trestle.resources.ui_use_skin
import net.blockhost.trestle.resources.ui_used_when_importing_existing_ftb_app_instances
import net.blockhost.trestle.resources.ui_username
import net.blockhost.trestle.resources.ui_version_components
import net.blockhost.trestle.resources.ui_versions
import net.blockhost.trestle.resources.ui_video
import net.blockhost.trestle.resources.ui_view_on_provider
import net.blockhost.trestle.resources.ui_world_name
import net.blockhost.trestle.resources.ui_worlds
import net.blockhost.trestle.resources.ui_screenshots
import net.blockhost.trestle.resources.ui_wrap_lines
import net.blockhost.trestle.resources.ui_wrapper_command
import net.blockhost.trestle.resources.ui_write_notes_for_instance
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class LauncherDestination(val label: StringResource) {
    LIBRARY(Res.string.ui_library),
    INSTANCE(Res.string.ui_instance),
    DISCOVER(Res.string.ui_discover),
    ACCOUNTS(Res.string.ui_accounts),
    SETTINGS(Res.string.ui_settings),
}

private val browsableResourceTypes = listOf(
    ResourceType.MOD,
    ResourceType.MODPACK,
    ResourceType.RESOURCE_PACK,
    ResourceType.SHADER_PACK,
)

private val installableResourceTypes = browsableResourceTypes.toSet()

private val WideContentWidth = 1200.dp

internal object LauncherTestTags {
    const val ROOT = "launcher-root"
    const val TOP_NAVIGATION = "top-navigation"
    const val LIBRARY = "library"
    const val INSTANCE_SEARCH = "instance-search"
    const val RESOURCE_SEARCH = "resource-search"
    const val NEW_INSTANCE = "new-instance"
    const val SELECTED_INSTANCE = "selected-instance"
    const val PRIMARY_INSTANCE_ACTION = "primary-instance-action"
    const val INSTANCE_WORKSPACE = "instance-workspace"
    const val INSTANCE_BACK = "instance-back"
    const val DISCOVER = "discover"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
    const val SETTINGS_CATEGORIES = "settings-categories"
    const val SETTINGS_DETAIL = "settings-detail"
    const val RESOURCE_FILTERS = "resource-filters"
    const val RESOURCE_RESULTS = "resource-results"
    const val RESOURCE_DETAIL = "resource-detail"
    const val CREATE_DIALOG = "create-dialog"
    const val INSTANCE_SETTINGS_DIALOG = "instance-settings-dialog"
    const val INSTANCE_ICON_EDIT = "instance-icon-edit"
    const val INSTANCE_ICON_DIALOG = "instance-icon-dialog"
    const val INSTANCE_ICON_UPLOAD = "instance-icon-upload"
    const val RESOURCE_BROWSER_DIALOG = "resource-browser-dialog"
    const val ACCOUNT_LOGIN_DIALOG = "account-login-dialog"
    const val SKIN_STUDIO = "skin-studio"
    const val SKIN_EDITOR_DIALOG = "skin-editor-dialog"

    fun instance(id: InstanceId): String = "instance-${id.value}"
    fun account(profileId: String): String = "account-$profileId"
    fun instanceIconOption(id: String): String = "instance-icon-option-$id"
    fun instanceSection(section: String): String = "instance-section-${section.lowercase()}"
    fun navigation(destination: LauncherDestination): String = "navigation-${destination.name.lowercase()}"
}

@Composable
fun TrestleApp(
    state: LauncherUiState,
    actions: LauncherUiActions,
    initialDestination: LauncherDestination = LauncherDestination.LIBRARY,
    accentColor: Color? = null,
    colorScheme: ColorScheme? = null,
    darkTheme: Boolean? = null,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    externalCommand: LauncherCommandRequest? = null,
    onExternalCommandHandled: (Long) -> Unit = {},
    onDestinationChanged: (LauncherDestination) -> Unit = {},
    topBar: @Composable () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
) {
    var destinationName by rememberSaveable { mutableStateOf(initialDestination.name) }
    val destination = LauncherDestination.entries.firstOrNull { it.name == destinationName }
        ?: LauncherDestination.LIBRARY
    val snackbarHostState = remember { SnackbarHostState() }
    val destinationStateHolder = rememberSaveableStateHolder()
    var showShortcuts by rememberSaveable { mutableStateOf(false) }
    var librarySearchFocusRequest by rememberSaveable { mutableStateOf(0) }
    var libraryImportRequest by rememberSaveable { mutableStateOf(0) }
    var resourceSearchFocusRequest by rememberSaveable { mutableStateOf(0) }
    val changeDestination: (LauncherDestination) -> Unit = { target ->
        if (
            destination == LauncherDestination.DISCOVER &&
            target != LauncherDestination.DISCOVER &&
            state.resourceBrowser.presentation == ResourceBrowserPresentation.PAGE
        ) {
            actions.closeResourceBrowser()
        }
        destinationName = target.name
        onDestinationChanged(target)
        if (
            target == LauncherDestination.DISCOVER &&
            (!state.resourceBrowser.visible || state.resourceBrowser.presentation != ResourceBrowserPresentation.PAGE)
        ) {
            actions.openResourceBrowser(presentation = ResourceBrowserPresentation.PAGE)
        }
    }
    val handleCommand: (LauncherCommand) -> Boolean = { command ->
        when (command) {
            LauncherCommand.NEW_INSTANCE -> actions.openCreate()
            LauncherCommand.IMPORT_LOCAL_FILE -> {
                changeDestination(LauncherDestination.LIBRARY)
                libraryImportRequest++
            }
            LauncherCommand.FOCUS_SEARCH -> {
                if (destination == LauncherDestination.DISCOVER) {
                    resourceSearchFocusRequest++
                } else {
                    changeDestination(LauncherDestination.LIBRARY)
                    librarySearchFocusRequest++
                }
            }
            LauncherCommand.LAUNCH_SELECTED -> actions.launchSelected()
            LauncherCommand.REMOVE_SELECTED -> actions.deleteSelected()
            LauncherCommand.TOGGLE_SELECTED_PIN -> actions.toggleSelectedInstancePinned()
            LauncherCommand.SHOW_LIBRARY -> changeDestination(LauncherDestination.LIBRARY)
            LauncherCommand.SHOW_DISCOVER -> changeDestination(LauncherDestination.DISCOVER)
            LauncherCommand.SHOW_ACCOUNTS -> changeDestination(LauncherDestination.ACCOUNTS)
            LauncherCommand.SHOW_SETTINGS -> changeDestination(LauncherDestination.SETTINGS)
            LauncherCommand.SHOW_SHORTCUTS -> showShortcuts = true
        }
        true
    }

    LaunchedEffect(externalCommand?.sequence) {
        val request = externalCommand ?: return@LaunchedEffect
        handleCommand(request.command)
        onExternalCommandHandled(request.sequence)
    }

    TrestleTheme(
        accentColor = accentColor,
        colorSchemeOverride = colorScheme,
        preference = state.themePreference,
        systemDarkTheme = darkTheme,
        highContrast = highContrast,
        reducedMotion = reducedMotion,
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        val modalVisible = state.create.visible ||
            state.instanceSettings.visible ||
            state.accountLogin.visible ||
            state.skinStudio.editor.visible ||
            state.localFileImport.visible ||
            state.serverEditor.visible ||
            state.pendingInstanceRemovalId != null ||
            state.pendingWorldDeletionKey != null ||
            showShortcuts ||
            (
                state.resourceBrowser.visible &&
                    state.resourceBrowser.presentation == ResourceBrowserPresentation.DIALOG
            )
        val retryActionLabel = stringResource(Res.string.ui_retry)
        val undoActionLabel = stringResource(Res.string.ui_undo)
        LaunchedEffect(state.error, state.notice, retryActionLabel, undoActionLabel) {
            val message = state.error ?: state.notice ?: return@LaunchedEffect
            val action = when {
                state.error != null && state.errorRecovery != null -> LauncherSnackbarAction.RETRY
                state.removedInstanceUndo != null -> LauncherSnackbarAction.UNDO
                else -> null
            }
            val actionLabel = when (action) {
                LauncherSnackbarAction.RETRY -> retryActionLabel
                LauncherSnackbarAction.UNDO -> undoActionLabel
                null -> null
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = state.error != null,
                duration = when {
                    state.error != null -> SnackbarDuration.Indefinite
                    state.removedInstanceUndo != null -> SnackbarDuration.Long
                    else -> SnackbarDuration.Short
                },
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (action) {
                    LauncherSnackbarAction.RETRY -> actions.retryError()
                    LauncherSnackbarAction.UNDO -> actions.undoInstanceRemoval()
                    null -> Unit
                }
            } else actions.clearMessage()
        }
        Scaffold(
            modifier = Modifier
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (modalVisible) return@onPreviewKeyEvent false
                    val primary = event.isCtrlPressed || event.isMetaPressed
                    val command = when {
                        primary && event.key == Key.N -> LauncherCommand.NEW_INSTANCE
                        primary && event.key == Key.O -> LauncherCommand.IMPORT_LOCAL_FILE
                        primary && event.key == Key.F -> LauncherCommand.FOCUS_SEARCH
                        primary && event.key == Key.Comma -> LauncherCommand.SHOW_SETTINGS
                        primary && event.key == Key.One -> LauncherCommand.SHOW_LIBRARY
                        primary && event.key == Key.Two -> LauncherCommand.SHOW_DISCOVER
                        primary && event.key == Key.Three -> LauncherCommand.SHOW_ACCOUNTS
                        primary && event.key == Key.Four -> LauncherCommand.SHOW_SETTINGS
                        event.key == Key.F1 -> LauncherCommand.SHOW_SHORTCUTS
                        event.key == Key.Escape && destination == LauncherDestination.INSTANCE ->
                            LauncherCommand.SHOW_LIBRARY
                        else -> null
                    }
                    command?.let(handleCommand) ?: false
                }
                .testTag(LauncherTestTags.ROOT),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
        ) { contentPadding ->
            val destinationContent: @Composable (Modifier, TrestleLayoutMode) -> Unit = { modifier, layoutMode ->
                destinationStateHolder.SaveableStateProvider(destination.name) {
                    when (destination) {
                        LauncherDestination.LIBRARY -> LibraryPage(
                            state,
                            modifier,
                            actions,
                            compact = layoutMode != TrestleLayoutMode.EXPANDED,
                            onManage = { changeDestination(LauncherDestination.INSTANCE) },
                            onDiscover = { type ->
                                actions.openResourceBrowser(type, ResourceBrowserPresentation.PAGE)
                                changeDestination(LauncherDestination.DISCOVER)
                            },
                            searchFocusRequest = librarySearchFocusRequest,
                            importRequest = libraryImportRequest,
                        )
                        LauncherDestination.INSTANCE -> InstanceWorkspace(
                            state,
                            modifier,
                            actions,
                            onBack = { changeDestination(LauncherDestination.LIBRARY) },
                            onDiscover = { type ->
                                actions.openResourceBrowser(type, ResourceBrowserPresentation.PAGE)
                                changeDestination(LauncherDestination.DISCOVER)
                            },
                            layoutMode = layoutMode,
                        )
                        LauncherDestination.DISCOVER -> ResourceCatalogPage(
                            state,
                            modifier,
                            actions,
                            searchFocusRequest = resourceSearchFocusRequest,
                        )
                        LauncherDestination.ACCOUNTS -> AccountsPage(state, modifier, actions, layoutMode)
                        LauncherDestination.SETTINGS -> SettingsPage(state, modifier, actions)
                    }
                }
            }
            LauncherNavigationLayout(
                destination = destination,
                onDestinationChange = changeDestination,
                adaptiveInfo = windowAdaptiveInfo,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                operation = state.operation,
                onCancelOperation = actions::cancelActiveOperation,
                onOpenOperation = { instanceId ->
                    actions.selectInstance(instanceId)
                    changeDestination(LauncherDestination.INSTANCE)
                },
                destinationContent = destinationContent,
            )
            if (state.create.visible) CreateInstanceDialog(state, actions)
            if (
                state.resourceBrowser.visible &&
                state.resourceBrowser.presentation == ResourceBrowserPresentation.DIALOG
            ) {
                ResourceBrowserDialog(state, actions)
            }
            if (state.instanceSettings.visible && destination != LauncherDestination.INSTANCE) {
                InstanceSettingsDialog(state, actions)
            }
            if (state.accountLogin.visible) AccountLoginDialog(state, actions)
            if (state.skinStudio.editor.visible) SkinEditorDialog(state, actions)
            if (state.localFileImport.visible) LocalFileImportDialog(state, actions)
            if (state.serverEditor.visible) ServerEditorDialog(state, actions)
            if (showShortcuts) ShortcutsDialog { showShortcuts = false }
            state.pendingInstanceRemovalId?.let { pendingId ->
                val instance = state.instances.firstOrNull { it.id == pendingId }
                val moveToTrash = state.instanceRemovalMode == InstanceRemovalMode.MOVE_TO_TRASH
                AlertDialog(
                    onDismissRequest = actions::cancelInstanceRemoval,
                    title = {
                        Text(
                            if (moveToTrash) {
                                "Move ${instance?.displayName ?: "instance"} to Trash?"
                            } else {
                                "Remove ${instance?.displayName ?: "instance"}?"
                            },
                        )
                    },
                    text = {
                        Text(
                            if (moveToTrash) {
                                "This removes the instance from Trestle and moves its complete directory to the system Trash."
                            } else {
                                "Remove it from the library and keep its files, or permanently delete the complete instance directory."
                            },
                        )
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = actions::cancelInstanceRemoval) { Text(stringResource(Res.string.ui_cancel)) }
                            if (!moveToTrash) {
                                TextButton(onClick = actions::confirmInstanceRemoval) { Text(stringResource(Res.string.ui_keep_files)) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = if (moveToTrash) {
                                actions::confirmInstanceRemoval
                            } else {
                                actions::confirmInstanceDeletion
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (moveToTrash) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            ),
                        ) {
                            Text(if (moveToTrash) "Move to Trash" else "Delete files")
                        }
                    },
                )
            }
            state.pendingWorldDeletionKey?.let { worldKey ->
                AlertDialog(
                    onDismissRequest = actions::cancelWorldDeletion,
                    title = { Text(stringResource(Res.string.ui_delete_named_world, worldKey)) },
                    text = {
                        Text(stringResource(Res.string.ui_this_permanently_deletes_the_world_directory_existing_zip_backups_remain))
                    },
                    dismissButton = {
                        TextButton(onClick = actions::cancelWorldDeletion) { Text(stringResource(Res.string.ui_cancel)) }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = actions::confirmWorldDeletion,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text(stringResource(Res.string.ui_delete_world)) }
                    },
                )
            }
        }
    }
}

private enum class LauncherSnackbarAction {
    RETRY,
    UNDO,
}

@Composable
private fun LibraryPage(
    state: LauncherUiState,
    modifier: Modifier,
    actions: LauncherUiActions,
    compact: Boolean = false,
    onManage: () -> Unit,
    onDiscover: (ResourceType) -> Unit,
    searchFocusRequest: Int,
    importRequest: Int,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var dropActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val importPicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("jar", "zip", "mrpack")),
    ) { file ->
        if (file != null) {
            scope.launch {
                if (runCatching { file.size() }.getOrDefault(-1L) > MAX_LOCAL_IMPORT_BYTES) {
                    actions.reportLocalFileTooLarge(file.name)
                } else {
                    runCatching { file.readBytes() }
                        .onSuccess { actions.queueLocalFileImport(file.name, it) }
                        .onFailure { actions.reportLocalFileReadFailure(file.name) }
                }
            }
        }
    }
    val searchFocusRequester = remember { FocusRequester() }
    val compactListState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val adaptiveInfo = LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val supportingPaneNavigator = rememberSupportingPaneScaffoldNavigator<InstanceId?>(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo),
    )
    val filteredInstances = state.instances.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) ||
            it.minecraftVersionId.contains(query, ignoreCase = true) || it.modLoader.label.contains(query, ignoreCase = true)
    }
    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > 0) searchFocusRequester.requestFocus()
    }
    LaunchedEffect(importRequest) {
        if (importRequest > 0) importPicker.launch()
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                PageHeader(
                    title = stringResource(Res.string.ui_library),
                    actions = {
                        if (compact) {
                            TextButton(onClick = { importPicker.launch() }) {
                                Text(stringResource(Res.string.ui_import))
                            }
                        } else {
                            OutlinedButton(onClick = { importPicker.launch() }) {
                                Text(stringResource(Res.string.ui_import))
                            }
                            Button(
                                onClick = actions::openCreate,
                                modifier = Modifier.testTag(LauncherTestTags.NEW_INSTANCE),
                            ) {
                                Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.ui_new_instance))
                            }
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            if (compact && state.instances.isNotEmpty()) {
                FloatingActionButton(
                    onClick = actions::openCreate,
                    modifier = Modifier.testTag(LauncherTestTags.NEW_INSTANCE),
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.ui_new_instance),
                    )
                }
            }
        },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .localFileDropTarget(
                    enabled = currentPlatform == "Desktop",
                    extensions = setOf("jar", "zip", "mrpack"),
                    onActiveChange = { dropActive = it },
                    onFiles = { files ->
                        files.firstOrNull()?.let { actions.queueLocalFileImport(it.name, it.bytes) }
                    },
                    onFailure = actions::reportLocalFileReadFailure,
                ),
        ) {
            when {
                state.isInitializing -> LoadingRows(Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY))
                state.instances.isEmpty() -> EmptyLibrary(
                    actions::openCreate,
                    Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY),
                )
                compact -> Column(Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY)) {
                    InstanceShelfToolbar(
                        query,
                        { query = it },
                        searchFocusRequester = searchFocusRequester,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InstanceCollection(
                        instances = filteredInstances,
                        state = state,
                        actions = actions,
                        compact = true,
                        compactListState = compactListState,
                        gridState = gridState,
                        onOpenInstance = onManage,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> SupportingPaneScaffold(
                    directive = supportingPaneNavigator.scaffoldDirective,
                    scaffoldState = supportingPaneNavigator.scaffoldState,
                    modifier = Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY),
                    mainPane = {
                        AnimatedPane {
                            Column(Modifier.fillMaxSize()) {
                                InstanceShelfToolbar(
                                    query,
                                    { query = it },
                                    searchFocusRequester = searchFocusRequester,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InstanceCollection(
                                    instances = filteredInstances,
                                    state = state,
                                    actions = actions,
                                    compact = false,
                                    compactListState = compactListState,
                                    gridState = gridState,
                                    onOpenInstance = onManage,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    },
                    supportingPane = {
                        AnimatedPane(Modifier.preferredWidth(380.dp)) {
                            SelectedInstancePanel(
                                state = state,
                                actions = actions,
                                onManage = onManage,
                                onDiscover = { onDiscover(ResourceType.MOD) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )
            }
            if (dropActive) {
                DropOverlay("Drop to import local content")
            }
        }
    }
}

@Composable
private fun BoxScope.DropOverlay(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun InstanceCollection(
    instances: List<GameInstance>,
    state: LauncherUiState,
    actions: LauncherUiActions,
    compact: Boolean,
    compactListState: LazyListState,
    gridState: LazyGridState,
    onOpenInstance: () -> Unit,
    modifier: Modifier,
) {
    if (instances.isEmpty()) {
        Column(
            modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.ui_no_matching_instances), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.ui_try_another_name_version_or_loader), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else if (compact) {
        LazyColumn(
            state = compactListState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(sortInstances(instances, state), key = { it.id.value }) { instance ->
                InstanceTile(
                    instance,
                    instance.id == state.selectedInstance?.id,
                    state,
                    actions,
                    compact = true,
                    onOpen = onOpenInstance,
                )
            }
        }
    } else {
        InstanceGrid(instances, state, actions, gridState, modifier)
    }
}

@Composable
private fun InstanceShelfToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        TrestleSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(Res.string.ui_search_instances)) },
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).focusRequester(searchFocusRequester)
                .testTag(LauncherTestTags.INSTANCE_SEARCH),
        )
    }
}

@Composable
private fun InlineMessage(message: String, error: Boolean, onRetry: (() -> Unit)?) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, modifier = Modifier.weight(1f))
            onRetry?.let { TextButton(onClick = it) { Text(stringResource(Res.string.ui_retry)) } }
        }
    }
}

@Composable
internal fun OperationBar(
    status: OperationStatus,
    onCancel: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        onClickLabel = stringResource(Res.string.ui_open_operation_details),
                        role = Role.Button,
                        onClick = onClick,
                    )
                },
            ),
    ) {
        Column {
            val progress = progressFraction(
                completedBytes = status.completed,
                totalBytes = status.total,
                completedFiles = status.completedItems,
                totalFiles = status.totalItems,
            )
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            } else {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(status.title, style = MaterialTheme.typography.labelLarge)
                    val progressDetail = buildList {
                        status.detail?.let(::add)
                        if (status.completed != null && status.total != null) {
                            add("${formatFileSize(status.completed)} of ${formatFileSize(status.total)}")
                        }
                        if (status.completedItems != null && status.totalItems != null) {
                            add("${status.completedItems} of ${status.totalItems} files")
                        }
                    }.joinToString(" · ")
                    progressDetail.takeIf(String::isNotBlank)?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (status.cancellable) TextButton(onClick = onCancel) { Text(status.cancelLabel) }
            }
        }
    }
}

@Composable
private fun SelectedInstancePanel(
    state: LauncherUiState,
    actions: LauncherUiActions,
    onManage: () -> Unit,
    onDiscover: () -> Unit,
    modifier: Modifier,
) {
    val instance = state.selectedInstance
    if (instance == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.ui_select_an_instance), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(Res.string.ui_choose_one_from_the_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val installationState = instance.installationState
    val activeAccount = state.accounts.firstOrNull { it.isActive }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.testTag(LauncherTestTags.SELECTED_INSTANCE),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InstanceArtwork(instance, 64.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        instance.displayName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "${instance.minecraftVersionId} · ${instance.modLoader.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(stateLabel(installationState), color = stateColor(installationState), style = MaterialTheme.typography.labelLarge)
            InstallationProgress(installationState, installationState.installationProgress())
            LaunchContext(state = installationState, activeAccount = activeAccount)
            LaunchReadiness(state, instance)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PrimaryInstanceButton(instance, state, actions, Modifier.fillMaxWidth())
            FilledTonalButton(
                onClick = onDiscover,
                enabled = installationState is InstallationState.Installed,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.ui_manage_content)) }
            OutlinedButton(
                onClick = onManage,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.ui_instance_settings)) }
        }
    }
}

@Composable
private fun LaunchReadiness(state: LauncherUiState, instance: GameInstance) {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status ?: return
    val message = when (status) {
        is LaunchStatus.Blocked -> "Required before launch: ${status.missingRequirements.joinToString()}"
        is LaunchStatus.Failed -> status.message
        is LaunchStatus.Unavailable -> status.reason
        LaunchStatus.Checking -> "Checking launch requirements"
        LaunchStatus.Starting -> "Starting Minecraft…"
        is LaunchStatus.Running -> status.processId?.let { "Minecraft is running · Process $it" } ?: "Minecraft is running"
        else -> null
    } ?: return
    val color = when (status) {
        is LaunchStatus.Blocked, is LaunchStatus.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(message, color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun LaunchContext(state: InstallationState, activeAccount: ManagedAccount?) {
    val accountText = when {
        activeAccount == null -> "No account selected"
        !activeAccount.isReady -> "${activeAccount.profile.playerName} · Sign-in required"
        else -> "${activeAccount.profile.playerName} · Ready to play"
    }
    Text(
        accountText,
        color = if (activeAccount?.isReady == true && state is InstallationState.Installed) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun PrimaryInstanceButton(
    instance: GameInstance,
    state: LauncherUiState,
    actions: LauncherUiActions,
    modifier: Modifier,
) {
    when (instance.installationState) {
        is InstallationState.Installing -> OutlinedButton(
            onClick = actions::cancelInstall,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text(stringResource(Res.string.ui_pause)) }
        is InstallationState.Interrupted -> Button(
            onClick = actions::installSelected,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text(stringResource(Res.string.ui_resume_install)) }
        is InstallationState.Failed -> Button(
            onClick = actions::installSelected,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text(stringResource(Res.string.ui_retry_install)) }
        InstallationState.NotInstalled -> Button(
            onClick = actions::installSelected,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text(stringResource(Res.string.ui_install)) }
        is InstallationState.Installed -> LaunchButton(
            state,
            instance,
            actions,
            modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        )
    }
}

@Composable
private fun InstallationProgress(state: InstallationState, progress: InstallationProgressSnapshot?) {
    if (progress == null) return
    val fraction = progressFraction(
        completedBytes = progress.completedBytes,
        totalBytes = progress.totalBytes,
        completedFiles = progress.completedFiles,
        totalFiles = progress.totalFiles,
    )
    if (fraction != null) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    } else {
        LinearProgressIndicator(
            Modifier.fillMaxWidth().widthIn(max = 520.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
    Text(
        if (state is InstallationState.Interrupted) {
            "${progress.completedFiles} of ${progress.totalFiles} files saved · Ready to resume"
        } else {
            "${progress.completedFiles} of ${progress.totalFiles} files"
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
}

private fun Modifier.dismissOnEscape(enabled: Boolean = true, onDismiss: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (enabled && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            onDismiss()
            true
        } else {
            false
        }
    }

@Composable
private fun InstanceGrid(
    instances: List<GameInstance>,
    state: LauncherUiState,
    actions: LauncherUiActions,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val grouped = instances.groupBy(::instanceGroupLabel)
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(184.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        grouped.forEach { (group, groupInstances) ->
            item(key = "group-$group", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    group,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            gridItems(sortInstances(groupInstances, state), key = { it.id.value }) { instance ->
                InstanceTile(instance, instance.id == state.selectedInstance?.id, state, actions)
            }
        }
    }
}

private fun sortInstances(instances: List<GameInstance>, state: LauncherUiState): List<GameInstance> =
    when (state.launcherPreferences.instanceSort) {
        net.blockhost.trestle.app.InstanceSortMode.NAME -> instances.sortedWith(
            compareByDescending<GameInstance> { it.pinned }.thenBy { it.displayName.lowercase() },
        )
        net.blockhost.trestle.app.InstanceSortMode.LAST_LAUNCHED -> instances.sortedWith(
            compareByDescending<GameInstance> { it.pinned }
                .thenByDescending { it.lastLaunchAtEpochMillis ?: Long.MIN_VALUE }
                .thenBy { it.displayName.lowercase() },
        )
    }

private fun instanceGroupLabel(instance: GameInstance): String = when {
    instance.pinned -> "Pinned"
    !instance.group.isNullOrBlank() -> instance.group
    instance.iconReference?.let { it.startsWith("https://") || it.startsWith("http://") } == true -> "Modpacks"
    instance.modLoader == ModLoader.VANILLA -> "Vanilla"
    else -> "${instance.modLoader.label} instances"
}

@Composable
private fun InstanceTile(
    instance: GameInstance,
    selected: Boolean,
    launcherState: LauncherUiState,
    actions: LauncherUiActions,
    compact: Boolean = false,
    onOpen: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val installationState = instance.installationState
    val running = launcherState.activeLaunch?.instanceId == instance.id &&
        launcherState.activeLaunch.status is LaunchStatus.Running
    val progress = installationState.installationProgress()
    val versionLabel = "${instance.minecraftVersionId} · ${instance.modLoader.label}" +
        if (instance.pinned) " · Pinned" else ""
    val interactionModifier = Modifier
        .fillMaxWidth()
        .trestleSelectable(
            selected = selected,
            onClickLabel = if (onOpen == null) "Select instance" else "Open instance",
            onClick = {
                actions.selectInstance(instance.id)
                onOpen?.invoke()
            },
            onDoubleClick = {
                actions.selectInstance(instance.id)
                actions.launchSelected()
            },
        )
        .onFocusChanged { focusState ->
            if (focusState.isFocused) actions.selectInstance(instance.id)
        }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Enter -> {
                    actions.selectInstance(instance.id)
                    if (onOpen == null) actions.launchSelected() else onOpen()
                    true
                }
                Key.Delete -> {
                    actions.selectInstance(instance.id)
                    actions.deleteSelected()
                    true
                }
                Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Left)
                Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Right)
                Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                else -> false
            }
        }
        .testTag(LauncherTestTags.instance(instance.id))
    ContextActionArea(instanceContextActions(instance, actions)) {
        if (compact) {
            ListItem(
                headlineContent = {
                    Text(instance.displayName, style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = { Text(versionLabel) },
                leadingContent = { InstanceArtwork(instance, 48.dp) },
                trailingContent = {
                    Text(
                        if (running) "Running" else stateLabel(installationState),
                        color = if (running) MaterialTheme.colorScheme.primary else stateColor(installationState),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                ),
                modifier = interactionModifier,
            )
            return@ContextActionArea
        }
        Card(
            modifier = interactionModifier,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InstanceArtwork(instance, 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        instance.displayName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        versionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    if (running) "Running" else stateLabel(installationState),
                    color = if (running) MaterialTheme.colorScheme.primary else stateColor(installationState),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (progress != null) InstallationProgress(installationState, progress)
            }
        }
    }
}

@Composable
private fun InstanceArtwork(instance: GameInstance, size: androidx.compose.ui.unit.Dp) {
    InstanceIconArtwork(instance = instance, size = size)
}

@Composable
private fun instanceContextActions(instance: GameInstance, actions: LauncherUiActions): List<ContextAction> {
    val copyText = rememberCopyText()
    val openPath = rememberOpenPath()
    val state = instance.installationState
    val selectedAction: (() -> Unit) -> Unit = { action ->
        actions.selectInstance(instance.id)
        action()
    }
    val primaryAction = when (state) {
        is InstallationState.Installing -> ContextAction("Pause installation") {
            selectedAction(actions::cancelInstall)
        }
        is InstallationState.Interrupted -> ContextAction("Resume installation") {
            selectedAction(actions::installSelected)
        }
        is InstallationState.Installed -> ContextAction("Launch") {
            selectedAction(actions::launchSelected)
        }
        is InstallationState.Failed -> ContextAction("Retry installation") {
            selectedAction(actions::installSelected)
        }
        InstallationState.NotInstalled -> ContextAction("Install") {
            selectedAction(actions::installSelected)
        }
    }
    return buildList {
        add(primaryAction)
        if (state is InstallationState.Installed) {
            add(ContextAction("Inspect launch plan") { selectedAction(actions::inspectLaunchPlan) })
        }
        if (state is InstallationState.Installed) {
            add(ContextAction("Add content") { selectedAction { actions.openResourceBrowser() } })
        }
        add(ContextAction("Instance settings", separatorBefore = true) { selectedAction(actions::openInstanceSettings) })
        add(ContextAction("Duplicate instance") { selectedAction(actions::cloneSelectedInstance) })
        add(ContextAction("Export instance") { selectedAction(actions::exportSelectedInstance) })
        add(ContextAction(if (instance.pinned) "Unpin from top" else "Pin to top") {
            selectedAction(actions::toggleSelectedInstancePinned)
        })
        if (currentPlatform == "Desktop") {
            add(ContextAction("Open instance folder", separatorBefore = true) { openPath(instance.instanceDirectory) })
            if (state is InstallationState.Installed) {
                add(ContextAction("Open game folder") { openPath("${instance.instanceDirectory}/game") })
                add(ContextAction("Open screenshots") { openPath("${instance.instanceDirectory}/game/screenshots") })
                if (instance.modLoader != ModLoader.VANILLA) {
                    add(ContextAction("Open mods") { openPath("${instance.instanceDirectory}/game/mods") })
                }
            }
        }
        add(ContextAction("Copy directory") { copyText(instance.instanceDirectory) })
        add(ContextAction("Copy instance details") { copyText(formatInstanceForClipboard(instance)) })
        add(ContextAction("Remove from library", separatorBefore = true) { selectedAction(actions::deleteSelected) })
        if (supportsPathTrash) {
            add(ContextAction("Move to Trash") { selectedAction(actions::moveSelectedToTrash) })
        }
    }
}

@Composable
private fun LaunchButton(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    modifier: Modifier = Modifier,
) {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status ?: LaunchStatus.NotChecked
    when (status) {
        is LaunchStatus.Running -> OutlinedButton(
            onClick = actions::stopLaunch,
            modifier = modifier,
        ) { Text(stringResource(Res.string.ui_stop)) }
        LaunchStatus.Checking,
        LaunchStatus.Starting,
        -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
        ) { Text(if (status == LaunchStatus.Checking) "Checking…" else "Starting…") }
        is LaunchStatus.Blocked -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
        ) { Text(stringResource(Res.string.ui_launch)) }
        is LaunchStatus.Unavailable -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
        ) { Text(stringResource(Res.string.ui_unavailable)) }
        is LaunchStatus.Failed -> Button(
            onClick = actions::launchSelected,
            modifier = modifier,
        ) { Text(stringResource(Res.string.ui_retry_launch)) }
        LaunchStatus.NotChecked,
        LaunchStatus.Ready,
        -> Button(
            onClick = actions::launchSelected,
            modifier = modifier,
        ) { Text(stringResource(Res.string.ui_launch)) }
    }
}

@Composable
private fun InstanceSettingsDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val form = state.instanceSettings
    TrestleDialog(
        onDismissRequest = { if (!form.isSaving) actions.closeInstanceSettings() },
        maxWidth = 620.dp,
        maxHeight = 820.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !form.isSaving, onDismiss = actions::closeInstanceSettings)
            .testTag(LauncherTestTags.INSTANCE_SETTINGS_DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        InstanceSettingsContent(
            state = state,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
            showHeader = true,
        )
    }
}

@Composable
private fun InstanceSettingsContent(
    state: LauncherUiState,
    actions: LauncherUiActions,
    modifier: Modifier,
    showHeader: Boolean,
) {
    val form = state.instanceSettings
    val instance = form.instanceId?.let { id -> state.instances.firstOrNull { it.id == id } }
    var showIconEditor by rememberSaveable(form.instanceId?.value) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val javaPicker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        file?.let { actions.setJavaExecutable(it.path) }
    }
    val minimum = form.minimumMemoryMiB.toIntOrNull()
    val maximum = form.maximumMemoryMiB.toIntOrNull()
    val valid = form.name.isNotBlank() && form.minecraftVersionId.isNotBlank() &&
        minimum != null && maximum != null && minimum > 0 && maximum >= minimum
    val availableVersions = state.versions.map { it.id }
    val availableLoaders = ModLoader.entries.filter { loader ->
        state.supportedModLoaders == null || loader in state.supportedModLoaders
    }
    val accountChoices = listOf("Use active account" to null) + state.accounts
        .filter { it.profile.edition == MinecraftEdition.JAVA }
        .map { account ->
            "${account.profile.playerName} (${account.profile.profileId.takeLast(6)})" to account.profile.profileId
        }
    val componentsChanged = instance != null && (
        instance.minecraftVersionId != form.minecraftVersionId || instance.modLoader != form.modLoader
    )
    val minimumError = when {
        form.minimumMemoryMiB.isBlank() -> "Enter a minimum memory value."
        minimum == null || minimum <= 0 -> "Minimum memory must be greater than 0."
        else -> null
    }
    val maximumError = when {
        form.maximumMemoryMiB.isBlank() -> "Enter a maximum memory value."
        maximum == null -> "Enter a valid maximum memory value."
        minimum != null && maximum < minimum -> "Maximum memory must be at least the minimum."
        else -> null
    }
    Column(modifier) {
        if (showHeader) {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_instance_settings),
                onClose = actions::closeInstanceSettings,
                closeEnabled = !form.isSaving,
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
                Text(stringResource(Res.string.ui_identity), style = MaterialTheme.typography.titleMedium)
                TextField(
                    value = form.name,
                    onValueChange = actions::setInstanceName,
                    label = { Text(stringResource(Res.string.ui_name)) },
                    isError = form.name.isBlank(),
                    supportingText = if (form.name.isBlank()) ({ Text(stringResource(Res.string.ui_enter_an_instance_name)) }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = form.group,
                    onValueChange = actions::setInstanceGroup,
                    label = { Text(stringResource(Res.string.ui_group)) },
                    supportingText = { Text(stringResource(Res.string.ui_leave_blank_to_group_this_instance_by_loader)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                instance?.let {
                    InstanceIconSetting(
                        instance = it,
                        form = form,
                        onEdit = { showIconEditor = true },
                    )
                }

                HorizontalDivider()
                Text(stringResource(Res.string.ui_game_components), style = MaterialTheme.typography.titleMedium)
                Selector(
                    label = "Minecraft version",
                    value = form.minecraftVersionId,
                    values = availableVersions,
                    enabled = !state.isLoadingVersions,
                    onSelect = actions::setInstanceVersion,
                )
                Selector(
                    label = "Mod loader",
                    value = form.modLoader.label,
                    values = availableLoaders.map { it.label },
                    onSelect = { selected ->
                        availableLoaders.firstOrNull { it.label == selected }?.let(actions::setInstanceLoader)
                    },
                )
                if (componentsChanged) {
                    Text(
                        "Changing the game version or loader requires another installation. Trestle keeps worlds and content files in place.",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                HorizontalDivider()
                Text(stringResource(Res.string.ui_launch), style = MaterialTheme.typography.titleMedium)
                Selector(
                    label = "Account",
                    value = accountChoices.firstOrNull { it.second == form.accountProfileId }?.first
                        ?: "Use active account",
                    values = accountChoices.map { it.first },
                    onSelect = { selected ->
                        actions.setInstanceAccount(accountChoices.firstOrNull { it.first == selected }?.second)
                    },
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val memoryField: @Composable (Boolean, Modifier) -> Unit = { isMinimum, modifier ->
                        val error = if (isMinimum) minimumError else maximumError
                        TextField(
                            value = if (isMinimum) form.minimumMemoryMiB else form.maximumMemoryMiB,
                            onValueChange = if (isMinimum) actions::setMinimumMemory else actions::setMaximumMemory,
                            label = { Text(if (isMinimum) "Minimum memory (MiB)" else "Maximum memory (MiB)") },
                            isError = error != null,
                            supportingText = if (error == null) null else ({ Text(error) }),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = if (isMinimum) ImeAction.Next else ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                onDone = {
                                    if (valid && !form.isLoadingClientSettings && !form.isSaving) {
                                        actions.saveInstanceSettings()
                                    }
                                },
                            ),
                            singleLine = true,
                            modifier = modifier,
                        )
                    }
                    if (maxWidth < 440.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            memoryField(true, Modifier.fillMaxWidth())
                            memoryField(false, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            memoryField(true, Modifier.weight(1f))
                            memoryField(false, Modifier.weight(1f))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(form.recommendation.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = actions::applyRecommendedMemory) { Text(stringResource(Res.string.ui_use_recommended)) }
                }
                form.warnings.forEach { warning -> Text(warning, color = MaterialTheme.colorScheme.error) }
                TextField(
                    value = form.jvmArguments,
                    onValueChange = actions::setJvmArguments,
                    label = { Text(stringResource(Res.string.ui_additional_jvm_arguments)) },
                    supportingText = {
                        Text(stringResource(Res.string.ui_memory_classpath_native_path_and_architecture_options_are_managed_by_tre))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = form.gameArguments,
                    onValueChange = actions::setGameArguments,
                    label = { Text(stringResource(Res.string.ui_additional_game_arguments)) },
                    supportingText = { Text(stringResource(Res.string.ui_quoted_values_and_escaped_characters_are_preserved)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.supportsCustomJava) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val javaField: @Composable (Modifier) -> Unit = { fieldModifier ->
                            TextField(
                                value = form.javaExecutable,
                                onValueChange = actions::setJavaExecutable,
                                label = { Text(stringResource(Res.string.ui_custom_java_executable)) },
                                supportingText = { Text(stringResource(Res.string.ui_leave_blank_to_use_trestles_managed_mojang_runtime)) },
                                singleLine = true,
                                modifier = fieldModifier,
                            )
                        }
                        if (maxWidth < 440.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                javaField(Modifier.fillMaxWidth())
                                OutlinedButton(onClick = { javaPicker.launch() }) { Text(stringResource(Res.string.ui_browse)) }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                javaField(Modifier.weight(1f))
                                OutlinedButton(onClick = { javaPicker.launch() }) { Text(stringResource(Res.string.ui_browse)) }
                            }
                        }
                    }
                }
                TextField(
                    value = form.environmentVariables,
                    onValueChange = actions::setEnvironmentVariables,
                    label = { Text(stringResource(Res.string.ui_environment_variables)) },
                    supportingText = { Text(stringResource(Res.string.ui_enter_one_name_value_pair_per_line_lines_starting_with_are_ignored)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.supportsLaunchCommands) {
                    HorizontalDivider()
                    Text(stringResource(Res.string.ui_custom_commands), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Commands run directly in the game directory. Enter an executable followed by its arguments; shell operators are not expanded.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextField(
                        value = form.preLaunchCommand,
                        onValueChange = actions::setPreLaunchCommand,
                        label = { Text(stringResource(Res.string.ui_pre_launch_command)) },
                        supportingText = { Text(stringResource(Res.string.ui_must_exit_successfully_before_minecraft_starts)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.wrapperCommand,
                        onValueChange = actions::setWrapperCommand,
                        label = { Text(stringResource(Res.string.ui_wrapper_command)) },
                        supportingText = { Text(stringResource(Res.string.ui_runs_before_the_java_executable_for_example_gamescope)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.postExitCommand,
                        onValueChange = actions::setPostExitCommand,
                        label = { Text(stringResource(Res.string.ui_post_exit_command)) },
                        supportingText = { Text(stringResource(Res.string.ui_runs_after_minecraft_exits)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider()
                Text(stringResource(Res.string.ui_minecraft_client), style = MaterialTheme.typography.titleMedium)
                Text(
                    "Changes are written to this instance's options.txt. Other game and mod settings are kept.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                when {
                    form.isLoadingClientSettings -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(Res.string.ui_loading_client_settings), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    form.clientSettingsError != null -> Text(form.clientSettingsError, color = MaterialTheme.colorScheme.error)
                    form.clientSettings != null -> ClientSettingsFields(
                        form.clientSettings,
                        actions::setInstanceClientSettings,
                    )
                }
        }
        TrestleDialogActions {
            TextButton(onClick = actions::closeInstanceSettings, enabled = !form.isSaving) {
                Text(stringResource(Res.string.ui_cancel))
            }
            Button(
                onClick = actions::saveInstanceSettings,
                enabled = valid && !form.isLoadingClientSettings && !form.isSaving,
            ) {
                if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.ui_save_changes))
            }
        }
    }
    if (showIconEditor && instance != null) {
        InstanceIconEditorDialog(
            instance = instance,
            reference = form.iconReference,
            pendingIcon = form.pendingIcon,
            onDismiss = { showIconEditor = false },
            onSave = { reference, pendingIcon ->
                if (pendingIcon == null) {
                    actions.setInstanceIconReference(reference)
                } else {
                    actions.setCustomInstanceIcon(pendingIcon.fileName, pendingIcon.bytes)
                }
                showIconEditor = false
            },
        )
    }
}

@Composable
private fun ResourceBrowserDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val browser = state.resourceBrowser
    TrestleDialog(
        onDismissRequest = actions::closeResourceBrowser,
        maxWidth = 1040.dp,
        widthFraction = 0.94f,
        heightFraction = 0.9f,
        modifier = Modifier
            .dismissOnEscape(enabled = !browser.isInstalling, onDismiss = actions::closeResourceBrowser)
            .testTag(LauncherTestTags.RESOURCE_BROWSER_DIALOG),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize()) {
            val instance = state.selectedInstance
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.ui_browse_content))
                        Text(
                            if (browser.type == ResourceType.MODPACK) {
                                "Modpacks create a new instance."
                            } else if (instance == null) {
                                "Select an installed instance to add content."
                            } else {
                                "Compatible with ${instance.minecraftVersionId} · ${instance.modLoader.label}"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    TrestleTooltipIconButton(
                        label = stringResource(Res.string.ui_close),
                        onClick = actions::closeResourceBrowser,
                        enabled = !browser.isInstalling,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.ui_close),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ResourceBrowserContent(
                state = state,
                actions = actions,
                modifier = Modifier.fillMaxSize(),
                searchFocusRequest = 0,
                adaptiveInfoOverride = compactTrestleWindowAdaptiveInfo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ResourceBrowserContent(
    state: LauncherUiState,
    actions: LauncherUiActions,
    modifier: Modifier,
    searchFocusRequest: Int,
    adaptiveInfoOverride: WindowAdaptiveInfo? = null,
) {
    val browser = state.resourceBrowser
    val adaptiveInfo = adaptiveInfoOverride ?: LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo),
    )
    val resultListState = rememberLazyListState()
    val detailScrollState = rememberScrollState()
    val searchFocusRequester = remember { FocusRequester() }
    val listPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden

    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > 0) searchFocusRequester.requestFocus()
    }

    LaunchedEffect(browser.selectedProjectId) {
        detailScrollState.scrollTo(0)
        when {
            browser.selectedProjectId != null &&
                navigator.currentDestination?.pane != ListDetailPaneScaffoldRole.Detail -> {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, browser.selectedProjectId)
            }
            browser.selectedProjectId == null &&
                navigator.currentDestination?.pane != ListDetailPaneScaffoldRole.List -> {
                if (navigator.canNavigateBack()) navigator.navigateBack()
                else navigator.navigateTo(ListDetailPaneScaffoldRole.List)
            }
        }
    }

    val clearSelection = {
        actions.clearResourceSelection()
    }
    PlatformBackHandler(
        enabled = browser.selectedProject != null && listPaneHidden && navigator.canNavigateBack(),
        onBack = clearSelection,
    )
    Column(modifier) {
        ResourceBrowserToolbar(browser, state.selectedInstance, actions, searchFocusRequester)
        browser.error?.let { InlineMessage(it, true, null) }
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.fillMaxSize(),
            listPane = {
                AnimatedPane(
                    Modifier.preferredWidth(440.dp).testTag(LauncherTestTags.RESOURCE_RESULTS),
                ) {
                    ResourceResultList(
                        browser = browser,
                        actions = actions,
                        listState = resultListState,
                        onProjectClick = actions::selectResource,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            detailPane = {
                AnimatedPane(Modifier.testTag(LauncherTestTags.RESOURCE_DETAIL)) {
                    Column(Modifier.fillMaxSize()) {
                        if (listPaneHidden) {
                            TextButton(
                                onClick = clearSelection,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
                                Text(stringResource(Res.string.ui_back_to_results))
                            }
                        }
                        ResourceSelection(
                            browser = browser,
                            instance = state.selectedInstance,
                            actions = actions,
                            scrollState = detailScrollState,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ResourceBrowserToolbar(
    browser: ResourceBrowserState,
    instance: GameInstance?,
    actions: LauncherUiActions,
    searchFocusRequester: FocusRequester,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrestleSearchBar(
            value = browser.query,
            onValueChange = actions::setResourceQuery,
            searching = browser.isSearching,
            onSearch = { actions.searchResources() },
            placeholder = { Text(stringResource(Res.string.ui_search_mods_packs_and_shaders)) },
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            inputModifier = Modifier.focusRequester(searchFocusRequester)
                .testTag(LauncherTestTags.RESOURCE_SEARCH),
        )
        if (browser.type != ResourceType.MODPACK && instance != null) {
            Text(
                "Installing into ${instance.displayName} · ${instance.minecraftVersionId} · ${instance.modLoader.label}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val toolbarWidth = maxWidth
            val compactFilterSheet = toolbarWidth < 840.dp
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (toolbarWidth < 840.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResourceProviderButtons(browser, actions, Modifier.fillMaxWidth())
                        if (toolbarWidth < 520.dp) {
                            OutlinedButton(
                                onClick = { showFilters = !showFilters },
                                modifier = Modifier.fillMaxWidth().testTag(LauncherTestTags.RESOURCE_FILTERS),
                            ) {
                                Text(stringResource(Res.string.ui_filters))
                            }
                            Selector(
                                label = "Content type",
                                value = browser.type.label,
                                values = browsableResourceTypes.map { it.label },
                                modifier = Modifier.fillMaxWidth(),
                                onSelect = { label ->
                                    actions.setResourceType(browsableResourceTypes.first { it.label == label })
                                },
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = { showFilters = !showFilters },
                                    modifier = Modifier.testTag(LauncherTestTags.RESOURCE_FILTERS),
                                ) {
                                    Text(stringResource(Res.string.ui_filters))
                                }
                                Selector(
                                    label = "Content type",
                                    value = browser.type.label,
                                    values = browsableResourceTypes.map { it.label },
                                    modifier = Modifier.weight(1f),
                                    onSelect = { label ->
                                        actions.setResourceType(browsableResourceTypes.first { it.label == label })
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResourceProviderButtons(browser, actions, Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier.testTag(LauncherTestTags.RESOURCE_FILTERS),
                        ) {
                            Text(stringResource(Res.string.ui_filters))
                        }
                        Selector(
                            label = "Content type",
                            value = browser.type.label,
                            values = browsableResourceTypes.map { it.label },
                            modifier = Modifier.width(190.dp),
                            onSelect = { label ->
                                actions.setResourceType(browsableResourceTypes.first { it.label == label })
                            },
                        )
                    }
                }
                if (showFilters && !compactFilterSheet) {
                    ResourceFilterFields(
                        browser = browser,
                        actions = actions,
                        compact = false,
                        onApply = actions::searchResources,
                    )
                }
            }
            if (showFilters && compactFilterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showFilters = false },
                    sheetState = filterSheetState,
                ) {
                    ResourceFilterFields(
                        browser = browser,
                        actions = actions,
                        compact = true,
                        onApply = {
                            actions.searchResources()
                            showFilters = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    )
                }
            }
        }
        if (!browser.curseForgeAvailable) {
            Text(stringResource(Res.string.ui_curseforge_requires_a_trestle_api_key_configured_by_the_application_buil), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResourceFilterFields(
    browser: ResourceBrowserState,
    actions: LauncherUiActions,
    compact: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameVersionField: @Composable (Modifier) -> Unit = { fieldModifier ->
        TextField(
            value = browser.gameVersionFilter,
            onValueChange = actions::setResourceGameVersionFilter,
            label = { Text(stringResource(Res.string.ui_minecraft_version)) },
            placeholder = { Text(stringResource(Res.string.ui_any_version)) },
            singleLine = true,
            modifier = fieldModifier,
        )
    }
    val loaderField: @Composable (Modifier) -> Unit = { fieldModifier ->
        Selector(
            label = "Mod loader",
            value = browser.loaderFilter?.label ?: "Any loader",
            values = listOf("Any loader") + ModLoader.entries.filterNot { it == ModLoader.VANILLA }.map { it.label },
            modifier = fieldModifier,
            onSelect = { label ->
                actions.setResourceLoaderFilter(ModLoader.entries.firstOrNull { it.label == label })
            },
        )
    }
    val categoryField: @Composable (Modifier) -> Unit = { fieldModifier ->
        TextField(
            value = browser.categoryFilter,
            onValueChange = actions::setResourceCategoryFilter,
            label = { Text(stringResource(Res.string.ui_category)) },
            placeholder = { Text(stringResource(Res.string.ui_any_category)) },
            singleLine = true,
            modifier = fieldModifier,
        )
    }
    val sortField: @Composable (Modifier) -> Unit = { fieldModifier ->
        Selector(
            label = "Sort by",
            value = browser.sort.label,
            values = ResourceSearchSort.entries.map { it.label },
            modifier = fieldModifier,
            onSelect = { label ->
                ResourceSearchSort.entries.firstOrNull { it.label == label }?.let(actions::setResourceSort)
            },
        )
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (compact) {
            Text(stringResource(Res.string.ui_filters), style = MaterialTheme.typography.titleLarge)
            gameVersionField(Modifier.fillMaxWidth())
            loaderField(Modifier.fillMaxWidth())
            categoryField(Modifier.fillMaxWidth())
            sortField(Modifier.fillMaxWidth())
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                gameVersionField(Modifier.weight(1f))
                loaderField(Modifier.weight(1f))
                categoryField(Modifier.weight(1f))
                sortField(Modifier.weight(1f))
                Button(onClick = onApply) { Text(stringResource(Res.string.ui_apply)) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.ui_versions), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReleaseChannel.entries.filterNot { it == ReleaseChannel.UNKNOWN }.forEach { channel ->
                    FilterChip(
                        selected = channel in browser.releaseChannels,
                        onClick = { actions.toggleResourceReleaseChannel(channel) },
                        label = { Text(channel.label) },
                    )
                }
            }
        }
        if (compact) {
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.ui_apply_filters))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceProviderButtons(
    browser: ResourceBrowserState,
    actions: LauncherUiActions,
    modifier: Modifier = Modifier,
) {
    val providers = if (browser.type == ResourceType.MODPACK) {
        ResourceProvider.entries
    } else {
        listOf(ResourceProvider.MODRINTH, ResourceProvider.CURSEFORGE)
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        providers.forEach { provider ->
            FilterChip(
                selected = browser.provider == provider,
                onClick = { actions.setResourceProvider(provider) },
                enabled = provider != ResourceProvider.CURSEFORGE || browser.curseForgeAvailable,
                label = { Text(provider.label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun ResourceResultList(
    browser: ResourceBrowserState,
    actions: LauncherUiActions,
    listState: LazyListState,
    onProjectClick: (String) -> Unit,
    modifier: Modifier,
) {
    when {
        browser.isSearching && browser.projects.isEmpty() -> LoadingRows(modifier)
        browser.projects.isEmpty() -> Column(
            modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.ui_no_results), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.ui_change_the_search_or_content_type), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(browser.projects, key = { "${it.provider.name}:${it.id}" }) { project ->
                ResourceProjectRow(
                    project = project,
                    selected = browser.selectedProjectId == project.id,
                    onClick = { onProjectClick(project.id) },
                )
            }
            if (browser.projects.size < browser.totalProjects) {
                item("load-more") {
                    TextButton(
                        onClick = actions::loadMoreResources,
                        enabled = !browser.isSearching,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (browser.isSearching) "Loading…" else "Load more") }
                }
            }
        }
    }
}

@Composable
private fun ResourceProjectRow(project: ResourceProject, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                project.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        overlineContent = {
            Text(
                "${project.provider.label} · ${project.author.ifBlank { "Unknown author" }}",
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    project.summary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(formatDownloads(project.downloads))
                        if (project.categories.isNotEmpty()) {
                            append(" · ")
                            append(project.categories.take(3).joinToString(" · "))
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            ResourceProjectLogo(project = project, modifier = Modifier.size(52.dp))
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    )
}

@Composable
private fun ResourceProjectLogo(project: ResourceProject, modifier: Modifier) {
    Box(
        modifier.aspectRatio(1f).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            project.name.firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
        )
        project.iconUrl?.let { iconUrl ->
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        }
    }
}

@Composable
private fun ResourceProjectBanner(project: ResourceProject, modifier: Modifier) {
    val bannerUrl = project.featuredImageUrl ?: return
    AsyncImage(
        model = bannerUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(16f / 9f).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
private fun ResourceSelection(
    browser: ResourceBrowserState,
    instance: GameInstance?,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    val project = browser.selectedProject
    val uriHandler = LocalUriHandler.current
    if (project == null) {
        Column(
            modifier.fillMaxHeight().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.ui_select_a_result), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.ui_available_versions_and_installation_details_will_appear_here), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val version = browser.selectedVersion
    val supportedType = project.type in installableResourceTypes
    val instanceReady = project.type == ResourceType.MODPACK || instance?.installationState is InstallationState.Installed
    val selectedFile = version?.primaryFile
    val downloadable = version?.externalPack != null || selectedFile?.url != null || selectedFile?.sha1 != null
    Column(modifier.fillMaxHeight()) {
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = 960.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResourceProjectLogo(project, Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "by ${project.author.ifBlank { "Unknown author" }} on ${project.provider.label}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(project.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        project.description?.takeIf(String::isNotBlank)?.let { description ->
            HorizontalDivider()
            ResourceDescription(description)
        }
        ResourceProjectDetails(project)
        project.websiteUrl?.takeIf(String::isNotBlank)?.let { websiteUrl ->
            TextButton(onClick = { uriHandler.openUri(websiteUrl) }) {
                Text(stringResource(Res.string.ui_view_on_provider, project.provider.label))
            }
        }
        val platformLinks = listOfNotNull(
            project.sourceUrl?.let { "Source" to it },
            project.issuesUrl?.let { "Issues" to it },
            project.wikiUrl?.let { "Wiki" to it },
        )
        if (platformLinks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                platformLinks.forEach { (label, url) ->
                    TextButton(onClick = { uriHandler.openUri(url) }) { Text(label) }
                }
            }
        }
        if (browser.isLoadingVersions) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else if (browser.versions.isNotEmpty()) {
            ResourceVersionPicker(browser, actions)
        }
        version?.let {
            ResourceVersionDetails(it)
            val optionalDependencies = it.dependencies.filter { dependency -> dependency.kind == DependencyKind.OPTIONAL }
            if (optionalDependencies.isNotEmpty()) {
                Text(stringResource(Res.string.ui_optional_dependencies), style = MaterialTheme.typography.titleMedium)
                optionalDependencies.forEach { dependency ->
                    ListItem(
                        headlineContent = {
                            Text(dependency.fileName ?: dependency.projectId ?: dependency.versionId ?: "External dependency")
                        },
                        leadingContent = {
                            Checkbox(
                                checked = dependency.selectionKey in browser.selectedOptionalDependencies,
                                onCheckedChange = null,
                                enabled = dependency.selectionKey.isNotBlank(),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = dependency.selectionKey in browser.selectedOptionalDependencies,
                            enabled = dependency.selectionKey.isNotBlank(),
                            role = Role.Checkbox,
                            onValueChange = { actions.toggleOptionalDependency(dependency.selectionKey) },
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!supportedType) Text(stringResource(Res.string.ui_this_content_type_cannot_be_installed_into_an_instance_yet), color = MaterialTheme.colorScheme.error)
        if (selectedFile?.url == null && selectedFile?.sha1 != null) {
            Text(stringResource(Res.string.ui_curseforge_blocks_this_file_trestle_will_look_for_the_identical_file_on_), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (version != null && version.externalPack == null && !downloadable) {
            Text(stringResource(Res.string.ui_the_author_blocks_downloads_from_third_party_launchers), color = MaterialTheme.colorScheme.error)
        }
        if (
            project.provider == ResourceProvider.CURSEFORGE &&
            selectedFile?.url == null &&
            selectedFile?.id != null &&
            project.websiteUrl != null
        ) {
            val manualUrl = "${project.websiteUrl.trimEnd('/')}/download/${selectedFile.id}"
            OutlinedButton(
                onClick = { uriHandler.openUri(manualUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.ui_open_manual_download)) }
        }
        if (project.featuredImageUrl != null) {
            Spacer(Modifier.height(8.dp))
            ResourceProjectBanner(project, Modifier.fillMaxWidth())
        }
        project.galleryUrls.filterNot { it == project.featuredImageUrl }.take(4).forEach { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = actions::installSelectedResource,
                    enabled = supportedType && instanceReady && downloadable && !browser.isInstalling,
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    if (browser.isInstalling) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (project.type == ResourceType.MODPACK) "Create instance" else "Install")
                }
            }
        }
    }
}

@Composable
private fun ResourceProjectDetails(project: ResourceProject) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PropertyRow("Downloads", formatCount(project.downloads))
        project.followers?.let { PropertyRow("Followers", formatCount(it)) }
        project.updatedAt?.let { PropertyRow("Updated", it.substringBefore('T')) }
        project.license?.takeIf(String::isNotBlank)?.let { PropertyRow("License", it) }
        project.clientSupport?.let { PropertyRow("Client", it.label) }
        project.serverSupport?.let { PropertyRow("Server", it.label) }
        if (project.categories.isNotEmpty()) {
            PropertyRow("Categories", project.categories.take(4).joinToString())
        }
    }
}

@Composable
private fun ResourceVersionPicker(browser: ResourceBrowserState, actions: LauncherUiActions) {
    val selected = browser.selectedVersion
    val versions = browser.versions.filter { it.channel in browser.releaseChannels }
    val labels = versions.map { "${it.versionNumber} · ${it.channel.label}" }
    Selector(
        label = "Version",
        value = selected?.let { "${it.versionNumber} · ${it.channel.label}" } ?: "Select version",
        values = labels,
        modifier = Modifier.fillMaxWidth(),
        onSelect = { label ->
            versions.getOrNull(labels.indexOf(label))?.let { actions.selectResourceVersion(it.id) }
        },
    )
}

@Composable
private fun ResourceVersionDetails(version: ResourceVersion) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PropertyRow("Channel", version.channel.label)
        PropertyRow("Minecraft", version.gameVersions.take(4).joinToString().ifBlank { "Pack manifest" })
        if (version.loaders.isNotEmpty()) PropertyRow("Loaders", version.loaders.joinToString())
        version.publishedAt.takeIf(String::isNotBlank)?.let { PropertyRow("Published", it.substringBefore('T')) }
        version.primaryFile?.let { file ->
            PropertyRow("File", file.fileName)
            file.size?.let { PropertyRow("Size", formatFileSize(it)) }
        }
        val required = version.dependencies.count { it.kind == DependencyKind.REQUIRED }
        if (required > 0) PropertyRow("Dependencies", "$required required")
    }
}

@Composable
private fun LocalFileImportDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val pending = state.localFileImport
    val selectedType = pending.selectedType
    val target = pending.targetInstanceId?.let { id -> state.instances.firstOrNull { it.id == id } }
    TrestleDialog(
        onDismissRequest = actions::cancelLocalFileImport,
        maxWidth = 480.dp,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Escape -> {
                    actions.cancelLocalFileImport()
                    true
                }
                Key.Enter -> {
                    if (selectedType != null) actions.confirmLocalFileImport()
                    selectedType != null
                }
                else -> false
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_import_local_file),
                onClose = actions::cancelLocalFileImport,
            )
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    pending.fileName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                pending.sourceOrigin?.let { origin ->
                    Text(
                        origin,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (pending.allowedTypes.size > 1) {
                    Text(stringResource(Res.string.ui_choose_how_trestle_should_use_this_zip_file))
                    Column {
                        pending.allowedTypes.forEach { type ->
                            Row(
                                Modifier.fillMaxWidth().selectable(
                                    selected = selectedType == type,
                                    role = Role.RadioButton,
                                    onClick = { actions.setLocalFileImportType(type) },
                                ).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = selectedType == type, onClick = null)
                                Text(type.label)
                            }
                        }
                    }
                }
                if (selectedType != ResourceType.MODPACK) {
                    Text(
                        target?.let { "Target: ${it.displayName}" }
                            ?: "Select an instance before adding this file.",
                        color = if (target == null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "The modpack creates a new instance.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TrestleDialogActions {
                TextButton(onClick = actions::cancelLocalFileImport) { Text(stringResource(Res.string.ui_cancel)) }
                Button(
                    onClick = actions::confirmLocalFileImport,
                    enabled = selectedType != null &&
                        (selectedType == ResourceType.MODPACK || target?.installationState is InstallationState.Installed),
                ) { Text(if (selectedType == ResourceType.MODPACK) "Import modpack" else "Add file") }
            }
        }
    }
}

@Composable
private fun ShortcutsDialog(onDismiss: () -> Unit) {
    TrestleDialog(
        onDismissRequest = onDismiss,
        maxWidth = 480.dp,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onDismiss()
                true
            } else {
                false
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_keyboard_shortcuts),
                onClose = onDismiss,
            )
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShortcutRow("Ctrl/Cmd + N", "New instance")
                ShortcutRow("Ctrl/Cmd + F", "Search")
                ShortcutRow("Ctrl/Cmd + 1–4", "Switch section")
                ShortcutRow("Ctrl/Cmd + ,", "Settings")
                ShortcutRow("Enter", "Launch focused instance")
                ShortcutRow("Arrow keys", "Move instance focus")
                ShortcutRow("Delete", "Remove focused instance")
                ShortcutRow("Escape", "Close or go back")
                ShortcutRow("F1", "Show shortcuts")
            }
        }
    }
}

@Composable
private fun ShortcutRow(keys: String, action: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(action, modifier = Modifier.weight(1f))
        Text(keys, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CreateInstanceDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val form = state.create
    val restrictedRuntime = state.supportedMinecraftVersions != null || state.supportedModLoaders != null
    val loaderChoices = ModLoader.entries
        .filter { state.supportedModLoaders == null || it in state.supportedModLoaders }
    val focusManager = LocalFocusManager.current
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var showSnapshots by rememberSaveable { mutableStateOf(false) }
    var showBetas by rememberSaveable { mutableStateOf(false) }
    var showAlphas by rememberSaveable { mutableStateOf(false) }
    var source by rememberSaveable { mutableStateOf("custom") }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    TrestleDialog(
        onDismissRequest = { if (!form.isSaving) actions.closeCreate() },
        maxWidth = 760.dp,
        maxHeight = 820.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !form.isSaving, onDismiss = actions::closeCreate)
            .testTag(LauncherTestTags.CREATE_DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_new_instance),
                onClose = actions::closeCreate,
                closeEnabled = !form.isSaving,
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!restrictedRuntime) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = source == "custom",
                        onClick = { source = "custom" },
                        shape = SegmentedButtonDefaults.itemShape(0, 3),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(Res.string.ui_custom)) }
                    SegmentedButton(
                        selected = source == "import",
                        onClick = { source = "import" },
                        shape = SegmentedButtonDefaults.itemShape(1, 3),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(Res.string.ui_import)) }
                    SegmentedButton(
                        selected = false,
                        onClick = {
                            actions.closeCreate()
                            actions.openResourceBrowser(ResourceType.MODPACK)
                        },
                        shape = SegmentedButtonDefaults.itemShape(2, 3),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(Res.string.ui_browse_modpacks)) }
                }
                TextField(
                    value = form.name,
                    onValueChange = actions::setCreateName,
                    label = { Text(stringResource(Res.string.ui_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = form.group,
                        onValueChange = actions::setCreateGroup,
                        label = { Text(stringResource(Res.string.ui_group)) },
                        supportingText = { Text(stringResource(Res.string.ui_optional)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = form.iconReference,
                        onValueChange = actions::setCreateIconReference,
                        label = { Text(stringResource(Res.string.ui_icon_path_or_url)) },
                        supportingText = { Text(stringResource(Res.string.ui_optional)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (source == "import") {
                    TextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text(stringResource(Res.string.ui_direct_download_or_curseforge_url)) },
                        placeholder = { Text(stringResource(Res.string.ui_https_or_curseforge)) },
                        supportingText = {
                            Text(stringResource(Res.string.ui_supports_modrinth_curseforge_prism_and_multimc_pack_archives))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider()
                    Text(stringResource(Res.string.ui_existing_ftb_app_library), style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.launcherPreferences.ftbAppInstancesPath.ifBlank {
                            "Set the FTB App instances folder in Settings > Services."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OutlinedButton(
                        onClick = actions::importFtbAppInstances,
                        enabled = state.launcherPreferences.ftbAppInstancesPath.isNotBlank() && !form.isSaving,
                    ) { Text(stringResource(Res.string.ui_import_ftb_app_instances)) }
                } else {
                    val visibleVersions = state.versions.filter { version ->
                        when (version.type) {
                            "release" -> true
                            "snapshot" -> showSnapshots
                            "old_beta" -> showBetas
                            "old_alpha" -> showAlphas
                            else -> false
                        }
                    }.take(500)
                    val versionLabels = visibleVersions.map { version ->
                        val type = when (version.type) {
                            "old_beta" -> "beta"
                            "old_alpha" -> "alpha"
                            else -> version.type
                        }
                        listOfNotNull(version.id, type, version.releaseTime?.substringBefore('T')).joinToString(" · ")
                    }
                    val selectedVersionLabel = visibleVersions.indexOfFirst { it.id == form.versionId }
                        .takeIf { it >= 0 }
                        ?.let(versionLabels::get)
                        ?: form.versionId
                    val versionChoices = visibleVersions.map { it.id }
                    if (versionChoices.size == 1) {
                        OutlinedTextField(
                            value = versionChoices.single(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.ui_minecraft_version)) },
                            supportingText = { Text(stringResource(Res.string.ui_android_runtime)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Selector(
                            label = "Minecraft version",
                            value = selectedVersionLabel.ifBlank {
                                if (state.isLoadingVersions) "Loading versions…" else "No versions available"
                            },
                            values = versionLabels,
                            enabled = !state.isLoadingVersions,
                            onSelect = { label ->
                                visibleVersions.getOrNull(versionLabels.indexOf(label))?.id?.let(actions::setCreateVersion)
                            },
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = showSnapshots,
                            onClick = { showSnapshots = !showSnapshots },
                            label = { Text("Snapshots") },
                        )
                        FilterChip(
                            selected = showBetas,
                            onClick = { showBetas = !showBetas },
                            label = { Text("Betas") },
                        )
                        FilterChip(
                            selected = showAlphas,
                            onClick = { showAlphas = !showAlphas },
                            label = { Text("Alphas") },
                        )
                    }
                    if (loaderChoices.size == 1) {
                        OutlinedTextField(
                            value = loaderChoices.single().label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.ui_loader)) },
                            supportingText = { Text(stringResource(Res.string.ui_only_vanilla_is_supported_on_android)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Selector(
                            label = "Loader",
                            value = form.modLoader.label,
                            values = loaderChoices.map { it.label },
                            onSelect = { label ->
                                loaderChoices.firstOrNull { it.label == label }?.let(actions::setCreateLoader)
                            },
                        )
                    }
                    if (form.modLoader != ModLoader.VANILLA) {
                        Selector(
                            label = "${form.modLoader.label} version",
                            value = form.loaderVersion ?: if (form.isResolvingLoader) "Loading…" else "No compatible loader",
                            values = form.loaderVersions,
                            enabled = !form.isResolvingLoader,
                            onSelect = actions::setCreateLoaderVersion,
                        )
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(Res.string.ui_client_defaults), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (showAdvanced) "Configure first-launch accessibility, audio, and distance settings."
                                else "Trestle will use balanced defaults for the first launch.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Hide" else "Customize")
                        }
                    }
                    if (showAdvanced) ClientDefaultsFields(form, actions, showHeading = false)
                }
            }
            TrestleDialogActions {
                TextButton(onClick = actions::closeCreate, enabled = !form.isSaving) { Text(stringResource(Res.string.ui_cancel)) }
                Button(
                    onClick = {
                        if (source == "import") actions.importRemoteModpack(remoteUrl) else actions.createInstance()
                    },
                    enabled = !form.isSaving && if (source == "import") {
                        remoteUrl.isNotBlank()
                    } else {
                        form.name.isNotBlank() && form.versionId.isNotBlank() &&
                            (form.modLoader == ModLoader.VANILLA || form.loaderVersion != null)
                    },
                ) {
                    if (form.isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.ui_creating))
                    } else {
                        Text(if (source == "import") "Import modpack" else "Create instance")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDefaultsFields(
    form: CreateInstanceState,
    actions: LauncherUiActions,
    showHeading: Boolean = true,
) {
    val settings = form.clientSettings
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeading) {
            TrestleSwitchItem(
                label = stringResource(Res.string.ui_client_defaults),
                checked = form.preconfigureClientSettings,
                supportingText = "Write these settings before the first launch. Settings unavailable in older versions are skipped.",
                onCheckedChange = actions::setCreateClientPreconfiguration,
            )
        } else {
            TrestleSwitchItem(
                label = stringResource(Res.string.ui_apply_these_defaults),
                checked = form.preconfigureClientSettings,
                onCheckedChange = actions::setCreateClientPreconfiguration,
            )
        }
        if (!form.preconfigureClientSettings) return@Column

        ClientSettingsFields(settings, actions::setCreateClientSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientSettingsFields(
    settings: MinecraftClientSettings,
    onSettingsChange: (MinecraftClientSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.ui_audio), style = MaterialTheme.typography.titleSmall)
        PercentageSlider(
            label = "Master volume",
            value = settings.masterVolumePercent,
            onValueChange = { onSettingsChange(settings.copy(masterVolumePercent = it)) },
        )
        PercentageSlider(
            label = "Music volume",
            value = settings.musicVolumePercent,
            onValueChange = { onSettingsChange(settings.copy(musicVolumePercent = it)) },
        )

        HorizontalDivider()
        Text(stringResource(Res.string.ui_video), style = MaterialTheme.typography.titleSmall)
        IntegerSlider(
            label = "Field of view",
            valueLabel = "${settings.fieldOfViewDegrees}°",
            value = settings.fieldOfViewDegrees,
            range = 30..110,
            onValueChange = { onSettingsChange(settings.copy(fieldOfViewDegrees = it)) },
        )
        PercentageSlider(
            label = "Brightness",
            value = settings.brightnessPercent,
            onValueChange = { onSettingsChange(settings.copy(brightnessPercent = it)) },
        )
        IntegerSlider(
            label = "Frame rate limit",
            valueLabel = if (settings.maximumFrameRate == 260) "Unlimited" else "${settings.maximumFrameRate} FPS",
            value = settings.maximumFrameRate,
            range = 10..260,
            steps = 24,
            onValueChange = { onSettingsChange(settings.copy(maximumFrameRate = it)) },
        )
        IntegerSlider(
            label = "GUI scale",
            valueLabel = if (settings.guiScale == 0) "Auto" else "${settings.guiScale}x",
            value = settings.guiScale,
            range = 0..8,
            onValueChange = { onSettingsChange(settings.copy(guiScale = it)) },
        )
        ChunkDistanceSlider(
            label = "Render distance",
            value = settings.renderDistanceChunks,
            range = 2..32,
            onValueChange = { onSettingsChange(settings.copy(renderDistanceChunks = it)) },
        )
        ChunkDistanceSlider(
            label = "Simulation distance",
            value = settings.simulationDistanceChunks,
            range = 5..32,
            onValueChange = { onSettingsChange(settings.copy(simulationDistanceChunks = it)) },
        )
        Selector(
            label = "Particles",
            value = settings.particles.label,
            values = MinecraftParticleSetting.entries.map { it.label },
            onSelect = { label ->
                onSettingsChange(
                    settings.copy(particles = MinecraftParticleSetting.entries.first { it.label == label }),
                )
            },
        )
        ClientSettingSwitch(
            label = "Fullscreen",
            checked = settings.fullscreen,
            onCheckedChange = { onSettingsChange(settings.copy(fullscreen = it)) },
        )
        ClientSettingSwitch(
            label = "VSync",
            checked = settings.enableVsync,
            onCheckedChange = { onSettingsChange(settings.copy(enableVsync = it)) },
        )
        ClientSettingSwitch(
            label = "View bobbing",
            checked = settings.viewBobbing,
            onCheckedChange = { onSettingsChange(settings.copy(viewBobbing = it)) },
        )
        ClientSettingSwitch(
            label = "Entity shadows",
            checked = settings.entityShadows,
            onCheckedChange = { onSettingsChange(settings.copy(entityShadows = it)) },
        )

        HorizontalDivider()
        Text(stringResource(Res.string.ui_controls_and_accessibility), style = MaterialTheme.typography.titleSmall)
        PercentageSlider(
            label = "Mouse sensitivity",
            value = settings.mouseSensitivityPercent,
            onValueChange = { onSettingsChange(settings.copy(mouseSensitivityPercent = it)) },
        )
        Selector(
            label = "Narrator",
            value = settings.narratorMode.label,
            values = MinecraftNarratorMode.entries.map { it.label },
            onSelect = { label ->
                onSettingsChange(
                    settings.copy(narratorMode = MinecraftNarratorMode.entries.first { it.label == label }),
                )
            },
        )
        ClientSettingSwitch(
            label = "Invert mouse",
            checked = settings.invertMouse,
            onCheckedChange = { onSettingsChange(settings.copy(invertMouse = it)) },
        )
        ClientSettingSwitch(
            label = "Auto-jump",
            checked = settings.autoJump,
            onCheckedChange = { onSettingsChange(settings.copy(autoJump = it)) },
        )
        ClientSettingSwitch(
            label = "Subtitles",
            checked = settings.showSubtitles,
            onCheckedChange = { onSettingsChange(settings.copy(showSubtitles = it)) },
        )
    }
}

@Composable
private fun PercentageSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    SliderSetting(label, "$value%") {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}

@Composable
private fun ChunkDistanceSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    IntegerSlider(label, "$value chunks", value, range, onValueChange = onValueChange)
}

@Composable
private fun IntegerSlider(
    label: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    steps: Int = range.last - range.first - 1,
    onValueChange: (Int) -> Unit,
) {
    SliderSetting(label, valueLabel) {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
        )
    }
}

@Composable
private fun SliderSetting(label: String, value: String, slider: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
        slider()
    }
}

@Composable
private fun ClientSettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    TrestleSwitchItem(
        label = label,
        checked = checked,
        modifier = Modifier.fillMaxWidth(),
        onCheckedChange = onCheckedChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Selector(
    label: String,
    value: String,
    values: List<String>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember(label) { mutableStateOf(false) }
    var filter by remember(label) { mutableStateOf("") }
    val canOpen = enabled && values.isNotEmpty()
    val visibleValues = if (filter.isBlank()) values else values.filter { it.contains(filter, ignoreCase = true) }
    LaunchedEffect(canOpen, values) {
        if (!canOpen) expanded = false
        filter = ""
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (canOpen) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(
                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = canOpen,
            ).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                filter = ""
            },
        ) {
            if (values.size > 20) {
                TextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text(stringResource(Res.string.ui_filter_named_field, label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            visibleValues.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        filter = ""
                        onSelect(item)
                    },
                )
            }
            if (visibleValues.isEmpty()) {
                Text(
                    "No matching options",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onNew: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.ui_no_instances_yet), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.ui_create_an_isolated_minecraft_instance), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew) { Text(stringResource(Res.string.ui_create_instance)) }
    }
}

@Composable
private fun LoadingRows(modifier: Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        generateN(3).forEach { ordinal ->
            Box(
                Modifier.fillMaxWidth().height(72.dp)
                    .background(
                        if (ordinal == 1) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        MaterialTheme.shapes.medium,
                    ),
            )
        }
    }
}

private enum class InstanceSection(val label: StringResource) {
    OVERVIEW(Res.string.ui_overview),
    CONTENT(Res.string.ui_content),
    WORLDS(Res.string.ui_worlds),
    SERVERS(Res.string.ui_servers),
    SCREENSHOTS(Res.string.ui_screenshots),
    NOTES(Res.string.ui_notes),
    LOGS(Res.string.ui_logs),
    SETTINGS(Res.string.ui_settings),
}

@Composable
private fun InstanceWorkspace(
    state: LauncherUiState,
    modifier: Modifier,
    actions: LauncherUiActions,
    onBack: () -> Unit,
    onDiscover: (ResourceType) -> Unit,
    layoutMode: TrestleLayoutMode,
) {
    val instance = state.selectedInstance
    var sectionName by rememberSaveable(instance?.id) { mutableStateOf(InstanceSection.OVERVIEW.name) }
    val section = InstanceSection.entries.firstOrNull { it.name == sectionName } ?: InstanceSection.OVERVIEW
    val overviewListState = rememberLazyListState()
    val contentListState = rememberLazyListState()
    val worldsListState = rememberLazyListState()
    val serversListState = rememberLazyListState()
    val screenshotsListState = rememberLazyListState()
    val configurationScrollState = rememberScrollState()
    val adaptiveInfo = LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo),
    )
    val scope = rememberCoroutineScope()
    val listPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val selectSection: (InstanceSection) -> Unit = { selectedSection ->
        sectionName = selectedSection.name
        if (selectedSection == InstanceSection.SETTINGS && !state.instanceSettings.visible) {
            actions.openInstanceSettings()
        }
        scope.launch {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedSection.name)
        }
    }
    val showSections: () -> Unit = {
        scope.launch {
            if (navigator.canNavigateBack()) navigator.navigateBack()
            else navigator.navigateTo(ListDetailPaneScaffoldRole.List)
        }
    }

    LaunchedEffect(instance?.id) {
        if (instance != null) {
            sectionName = InstanceSection.OVERVIEW.name
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, InstanceSection.OVERVIEW.name)
        }
    }
    PlatformBackHandler(
        enabled = instance != null && listPaneHidden && navigator.canNavigateBack(),
        onBack = showSections,
    )

    Column(modifier.fillMaxSize().testTag(LauncherTestTags.INSTANCE_WORKSPACE)) {
        if (layoutMode == TrestleLayoutMode.EXPANDED && instance != null) {
            InstanceWorkspaceHeader(state, instance, actions, onBack)
        } else {
            PageHeader(
                title = if (listPaneHidden && instance != null) {
                    stringResource(section.label)
                } else {
                    instance?.displayName ?: stringResource(Res.string.ui_instance)
                },
                navigationIcon = {
                    TrestleTooltipIconButton(
                        label = if (listPaneHidden && instance != null) {
                            stringResource(Res.string.ui_back)
                        } else {
                            stringResource(Res.string.ui_back_to_library)
                        },
                        onClick = if (listPaneHidden && instance != null) showSections else onBack,
                        modifier = Modifier.testTag(LauncherTestTags.INSTANCE_BACK),
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (instance == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.ui_no_instance_selected), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) { Text(stringResource(Res.string.ui_return_to_library)) }
            }
            return@Column
        }
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = {
                AnimatedPane(Modifier.preferredWidth(232.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(InstanceSection.entries, key = InstanceSection::name) { item ->
                                ListItem(
                                    headlineContent = { Text(stringResource(item.label)) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (section == item) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = section == item,
                                            role = Role.RadioButton,
                                            onClick = { selectSection(item) },
                                        )
                                        .testTag(LauncherTestTags.instanceSection(item.name)),
                                )
                            }
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        val contentModifier = Modifier.widthIn(max = WideContentWidth).fillMaxSize()
                        when (section) {
                            InstanceSection.OVERVIEW -> InstanceOverview(
                                state,
                                instance,
                                actions,
                                overviewListState,
                                contentModifier,
                                layoutMode == TrestleLayoutMode.COMPACT,
                            )
                            InstanceSection.CONTENT -> InstanceContent(
                                state,
                                instance,
                                actions,
                                onDiscover,
                                contentListState,
                                contentModifier,
                            )
                            InstanceSection.WORLDS -> InstanceGameData(
                                state,
                                instance,
                                actions,
                                GameDataView.WORLDS,
                                worldsListState,
                                contentModifier,
                            )
                            InstanceSection.SERVERS -> InstanceGameData(
                                state,
                                instance,
                                actions,
                                GameDataView.SERVERS,
                                serversListState,
                                contentModifier,
                            )
                            InstanceSection.SCREENSHOTS -> InstanceGameData(
                                state,
                                instance,
                                actions,
                                GameDataView.SCREENSHOTS,
                                screenshotsListState,
                                contentModifier,
                            )
                            InstanceSection.NOTES -> InstanceNotes(instance, actions, contentModifier)
                            InstanceSection.LOGS -> InstanceLogs(state, instance, actions, contentModifier)
                            InstanceSection.SETTINGS -> if (state.instanceSettings.visible) {
                                InstanceSettingsContent(
                                    state = state,
                                    actions = actions,
                                    modifier = contentModifier,
                                    showHeader = false,
                                )
                            } else {
                                InstanceConfiguration(
                                    instance = instance,
                                    actions = actions,
                                    scrollState = configurationScrollState,
                                    modifier = contentModifier,
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

private enum class GameDataView {
    WORLDS,
    SERVERS,
    SCREENSHOTS,
}

@Composable
private fun InstanceGameData(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    view: GameDataView,
    listState: LazyListState,
    modifier: Modifier,
) {
    val openPath = rememberOpenPath()
    val copyText = rememberCopyText()
    var screenshotToRename by rememberSaveable(instance.id) { mutableStateOf<String?>(null) }
    var screenshotName by rememberSaveable(instance.id) { mutableStateOf("") }
    var worldToRename by rememberSaveable(instance.id) { mutableStateOf<String?>(null) }
    var worldName by rememberSaveable(instance.id) { mutableStateOf("") }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item("game-data-heading") {
            Row(
                Modifier.widthIn(max = 820.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when (view) {
                            GameDataView.WORLDS -> stringResource(Res.string.ui_worlds)
                            GameDataView.SERVERS -> stringResource(Res.string.ui_servers)
                            GameDataView.SCREENSHOTS -> stringResource(Res.string.ui_screenshots)
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        when (view) {
                            GameDataView.WORLDS -> "Manage local worlds, data packs, and backups for ${instance.displayName}."
                            GameDataView.SERVERS -> "Manage multiplayer servers saved in ${instance.displayName}."
                            GameDataView.SCREENSHOTS -> "Review and organize screenshots from ${instance.displayName}."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = actions::refreshGameData, enabled = !state.isLoadingGameData) {
                    Text(stringResource(Res.string.ui_refresh))
                }
            }
        }
        if (state.isLoadingGameData) {
            item("game-data-loading") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.ui_reading_the_game_directory), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (view == GameDataView.WORLDS) item("worlds") {
            GameDataSection("Worlds", if (state.gameData.worlds.isEmpty()) "No local worlds yet." else null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    WorldImportButton(actions)
                }
                state.gameData.worlds.forEach { world ->
                    ListItem(
                        leadingContent = world.iconPath?.let { iconPath ->
                            {
                                AsyncImage(
                                    model = iconPath,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        },
                        headlineContent = { Text(world.name) },
                        supportingContent = {
                            val details = listOfNotNull(
                                world.gameMode,
                                formatFileSize(world.sizeBytes),
                                "${world.dataPacks.size} data packs",
                            )
                            Text(details.joinToString(" · "))
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { actions.launchWorld(world.key) }) { Text(stringResource(Res.string.ui_play)) }
                                InstanceItemActions(
                                    buildList {
                                        add(
                                            "Rename" to {
                                                worldToRename = world.key
                                                worldName = world.name
                                            },
                                        )
                                        add("Copy" to { actions.copyWorld(world.key) })
                                        add("Back up" to { actions.backupWorld(world.key) })
                                        if (world.iconPath != null) add("Reset icon" to { actions.resetWorldIcon(world.key) })
                                        add("Delete" to { actions.deleteWorld(world.key) })
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    world.seed?.let { seed ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(Res.string.ui_seed_value, seed),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { copyText(seed.toString()) }) { Text(stringResource(Res.string.ui_copy_seed)) }
                        }
                    }
                    world.dataPacks.forEach { pack ->
                        ListItem(
                            headlineContent = { Text(pack.fileName) },
                            supportingContent = { Text(formatFileSize(pack.sizeBytes)) },
                            trailingContent = {
                                Switch(
                                    checked = pack.enabled,
                                    onCheckedChange = null,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp).toggleable(
                                value = pack.enabled,
                                role = Role.Switch,
                                onValueChange = { actions.toggleDataPack(world.key, pack.key) },
                            ),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        if (view == GameDataView.SERVERS) item("servers") {
            GameDataSection("Servers", if (state.gameData.servers.isEmpty()) "No saved multiplayer servers." else null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { actions.openServerEditor() }) { Text(stringResource(Res.string.ui_add_server)) }
                }
                state.gameData.servers.forEach { server ->
                    ListItem(
                        leadingContent = server.iconDataUrl?.let { iconDataUrl ->
                            {
                                AsyncImage(
                                    model = iconDataUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp).clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        },
                        headlineContent = { Text(server.name) },
                        supportingContent = {
                            val status = when (server.status) {
                                ServerStatus.ONLINE -> listOfNotNull(
                                    server.onlinePlayers?.let { online ->
                                        server.maximumPlayers?.let { maximum -> "$online/$maximum online" } ?: "$online online"
                                    },
                                    server.pingMillis?.let { "${it} ms" },
                                ).joinToString(" · ")
                                ServerStatus.OFFLINE -> "Offline"
                                ServerStatus.UNKNOWN -> "Not checked"
                            }
                            Text(stringResource(Res.string.ui_server_status, server.address, status))
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { actions.joinServer(server.key) }) { Text(stringResource(Res.string.ui_join)) }
                                InstanceItemActions(
                                    listOf(
                                        "Move up" to { actions.moveServer(server.key, -1) },
                                        "Move down" to { actions.moveServer(server.key, 1) },
                                        "Edit" to { actions.openServerEditor(server.key) },
                                        "Remove" to { actions.removeServer(server.key) },
                                    ),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
        if (view == GameDataView.WORLDS) item("backups") {
            GameDataSection("Backups", if (state.gameData.backups.isEmpty()) "No world backups yet." else null) {
                state.gameData.backups.forEach { backup ->
                    ListItem(
                        headlineContent = { Text(backup.fileName) },
                        supportingContent = { Text(formatFileSize(backup.sizeBytes)) },
                        trailingContent = {
                            TextButton(onClick = { actions.restoreWorldBackup(backup.key) }) { Text(stringResource(Res.string.ui_restore_copy)) }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
        if (view == GameDataView.SCREENSHOTS) item("screenshots") {
            GameDataSection("Screenshots", if (state.gameData.screenshots.isEmpty()) "No screenshots yet." else null) {
                state.gameData.screenshots.forEach { screenshot ->
                    ListItem(
                        leadingContent = {
                            AsyncImage(
                                model = screenshot.path,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop,
                            )
                        },
                        headlineContent = { Text(screenshot.fileName) },
                        supportingContent = { Text(formatFileSize(screenshot.sizeBytes)) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { openPath(screenshot.path) },
                                    enabled = currentPlatform == "Desktop",
                                ) { Text(stringResource(Res.string.ui_open)) }
                                InstanceItemActions(
                                    listOf(
                                        "Copy file path" to { copyText(screenshot.path) },
                                        "Rename" to {
                                            screenshotToRename = screenshot.key
                                            screenshotName = screenshot.fileName.substringBeforeLast('.')
                                        },
                                        "Delete" to { actions.deleteScreenshot(screenshot.key) },
                                    ),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
    worldToRename?.let { worldKey ->
        AlertDialog(
            onDismissRequest = { worldToRename = null },
            title = { Text(stringResource(Res.string.ui_rename_world)) },
            text = {
                OutlinedTextField(
                    value = worldName,
                    onValueChange = { worldName = it },
                    label = { Text(stringResource(Res.string.ui_world_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = { TextButton(onClick = { worldToRename = null }) { Text(stringResource(Res.string.ui_cancel)) } },
            confirmButton = {
                Button(
                    onClick = {
                        actions.renameWorld(worldKey, worldName)
                        worldToRename = null
                    },
                    enabled = worldName.isNotBlank(),
                ) { Text(stringResource(Res.string.ui_rename)) }
            },
        )
    }
    screenshotToRename?.let { screenshotKey ->
        AlertDialog(
            onDismissRequest = { screenshotToRename = null },
            title = { Text(stringResource(Res.string.ui_rename_screenshot)) },
            text = {
                OutlinedTextField(
                    value = screenshotName,
                    onValueChange = { screenshotName = it },
                    label = { Text(stringResource(Res.string.ui_file_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { screenshotToRename = null }) { Text(stringResource(Res.string.ui_cancel)) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.renameScreenshot(screenshotKey, screenshotName)
                        screenshotToRename = null
                    },
                    enabled = screenshotName.isNotBlank(),
                ) { Text(stringResource(Res.string.ui_rename)) }
            },
        )
    }
}

@Composable
private fun WorldImportButton(actions: LauncherUiActions) {
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File(extensions = listOf("zip"))) { file ->
        if (file != null) {
            scope.launch {
                if (runCatching { file.size() }.getOrDefault(-1L) > MAX_LOCAL_IMPORT_BYTES) {
                    actions.reportLocalFileTooLarge(file.name)
                } else {
                    runCatching { file.readBytes() }
                        .onSuccess { actions.importWorld(file.name, it) }
                        .onFailure { actions.reportLocalFileReadFailure(file.name) }
                }
            }
        }
    }
    TextButton(onClick = { picker.launch() }) { Text(stringResource(Res.string.ui_import_world)) }
}

@Composable
private fun InstanceNotes(
    instance: GameInstance,
    actions: LauncherUiActions,
    modifier: Modifier,
) {
    var draft by rememberSaveable(instance.id, instance.notes) { mutableStateOf(instance.notes) }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.widthIn(max = 820.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(Res.string.ui_notes), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Keep setup details, server information, or reminders with this instance.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { actions.saveInstanceNotes(draft) },
                    enabled = draft.trimEnd() != instance.notes,
                ) { Text(stringResource(Res.string.ui_save_notes)) }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(stringResource(Res.string.ui_write_notes_for_instance, instance.displayName))
                },
                minLines = 16,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InstanceLogs(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    modifier: Modifier,
) {
    var query by rememberSaveable(instance.id) { mutableStateOf("") }
    var followLaunch by rememberSaveable(instance.id) { mutableStateOf(true) }
    var wrapLines by rememberSaveable(instance.id) { mutableStateOf(true) }
    var colorLines by rememberSaveable(instance.id) { mutableStateOf(true) }
    val copyText = rememberCopyText()
    val outputScroll = rememberScrollState()
    val selectedKey = state.selectedInstanceLogKey
    val launchActive = state.activeLaunch?.status.let { it == LaunchStatus.Starting || it is LaunchStatus.Running }
    LaunchedEffect(state.gameData.logs, selectedKey) {
        if (selectedKey == null) state.gameData.logs.firstOrNull()?.let { actions.selectInstanceLog(it.key) }
    }
    val streamedLog = selectedKey?.endsWith(".trestle/logs/latest.log") == true && state.gameLogLines.isNotEmpty()
    val rawText = if (followLaunch && streamedLog) state.gameLogLines.joinToString("\n") else state.selectedInstanceLogText
    val visibleText = if (query.isBlank()) rawText else rawText.lineSequence()
        .filter { it.contains(query, ignoreCase = true) }
        .joinToString("\n")
    val renderedText = if (colorLines) {
        buildAnnotatedString {
            visibleText.lineSequence().forEachIndexed { index, line ->
                val color = when {
                    "error" in line.lowercase() || "fatal" in line.lowercase() -> MaterialTheme.colorScheme.error
                    "warn" in line.lowercase() -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                if (index > 0) append('\n')
                withStyle(SpanStyle(color = color)) { append(line) }
            }
        }
    } else {
        AnnotatedString(visibleText)
    }
    LaunchedEffect(visibleText, followLaunch) {
        if (followLaunch) outputScroll.scrollTo(outputScroll.maxValue)
    }
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.widthIn(max = 1000.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(Res.string.ui_instance_logs), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Review Minecraft output, archived logs, and crash reports.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { selectedKey?.let(actions::selectInstanceLog) },
                    enabled = selectedKey != null && !state.isLoadingInstanceLog,
                ) { Text(stringResource(Res.string.ui_reload)) }
                InstanceItemActions(
                    buildList {
                        if (visibleText.isNotEmpty()) add("Copy visible log" to { copyText(visibleText) })
                        if (state.gameLogLines.isNotEmpty()) add("Clear streamed log" to actions::clearGameLog)
                        if (selectedKey != null && !launchActive) {
                            add("Delete log file" to { actions.deleteInstanceLog(selectedKey) })
                        }
                    },
                )
            }
            if (state.gameData.logs.isEmpty()) {
                Text(
                    "Logs appear here after Minecraft starts.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Selector(
                    label = "Log file",
                    value = selectedKey.orEmpty(),
                    values = state.gameData.logs.map { it.key },
                    enabled = !state.isLoadingInstanceLog,
                    onSelect = actions::selectInstanceLog,
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.ui_find_in_log)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        Modifier.toggleable(followLaunch, role = Role.Checkbox) { followLaunch = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(followLaunch, onCheckedChange = null)
                        Text(stringResource(Res.string.ui_follow_launch))
                    }
                    Row(
                        Modifier.toggleable(wrapLines, role = Role.Checkbox) { wrapLines = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(wrapLines, onCheckedChange = null)
                        Text(stringResource(Res.string.ui_wrap_lines))
                    }
                }
                Row(
                    Modifier.toggleable(colorLines, role = Role.Checkbox) { colorLines = it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(colorLines, onCheckedChange = null)
                    Text(stringResource(Res.string.ui_color_warnings_and_errors))
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when {
                    state.isLoadingInstanceLog -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            if (renderedText.isEmpty()) AnnotatedString("No matching log lines.") else renderedText,
                            modifier = Modifier.fillMaxSize().verticalScroll(outputScroll).padding(12.dp),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = wrapLines,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameDataSection(title: String, emptyMessage: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (emptyMessage != null) {
            Text(
                emptyMessage,
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun InstanceItemActions(actions: List<Pair<String, () -> Unit>>) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(stringResource(Res.string.ui_more)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun ServerEditorDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val editor = state.serverEditor
    val valid = editor.name.isNotBlank() && editor.address.isNotBlank()
    AlertDialog(
        onDismissRequest = { if (!editor.isSaving) actions.closeServerEditor() },
        title = { Text(if (editor.key == null) "Add server" else "Edit server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = editor.name,
                    onValueChange = actions::setServerName,
                    label = { Text(stringResource(Res.string.ui_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = editor.address,
                    onValueChange = actions::setServerAddress,
                    label = { Text(stringResource(Res.string.ui_address)) },
                    supportingText = { Text(stringResource(Res.string.ui_for_example_play_example_net_25565)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Selector(
                    label = "Server resource packs",
                    value = when (editor.acceptTextures) {
                        true -> "Always"
                        false -> "Never"
                        null -> "Ask"
                    },
                    values = listOf("Ask", "Always", "Never"),
                    onSelect = actions::setServerResourcePacks,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = actions::closeServerEditor, enabled = !editor.isSaving) { Text(stringResource(Res.string.ui_cancel)) }
        },
        confirmButton = {
            Button(onClick = actions::saveServer, enabled = valid && !editor.isSaving) {
                Text(if (editor.isSaving) "Saving…" else "Save")
            }
        },
    )
}

@Composable
private fun InstanceWorkspaceHeader(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(88.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.widthIn(max = WideContentWidth).fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.ui_library))
            }
            InstanceArtwork(instance, 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    instance.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Minecraft ${instance.minecraftVersionId} · ${instance.modLoader.label} · ${stateLabel(instance.installationState)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PrimaryInstanceButton(instance, state, actions, Modifier.widthIn(min = 132.dp))
        }
    }
}

@Composable
private fun InstanceOverview(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    listState: LazyListState,
    modifier: Modifier,
    compact: Boolean,
) {
    val openPath = rememberOpenPath()
    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(24.dp)) {
        if (compact) {
            item("identity") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        InstanceArtwork(instance, 64.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(instance.displayName, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(
                                    Res.string.ui_instance_version_loader,
                                    instance.minecraftVersionId,
                                    instance.modLoader.label,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(stateLabel(instance.installationState), color = stateColor(instance.installationState))
                        }
                    }
                    PrimaryInstanceButton(instance, state, actions, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        item("properties") {
            Column(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                Text(stringResource(Res.string.ui_instance), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                PropertyRow("Java", instance.requiredJavaMajor.toString())
                PropertyRow("Memory", "${instance.memory.minimumMiB}–${instance.memory.maximumMiB} MiB")
                instance.group?.let { PropertyRow("Group", it) }
                PropertyRow("Launches", instance.launchCount.toString())
                PropertyRow("Play time", formatPlayTime(instance.playTimeMillis))
                PropertyRow(
                    "Directory",
                    instance.instanceDirectory,
                    actionLabel = "Open",
                    onClick = { openPath(instance.instanceDirectory) },
                    actionEnabled = currentPlatform == "Desktop",
                )
                PropertyRow(
                    "Last launch",
                    instance.lastLaunchAtEpochMillis?.let(::formatLocalDateTime) ?: "Never",
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.ui_version_components), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = actions::openInstanceSettings) { Text(stringResource(Res.string.ui_change)) }
                }
                PropertyRow("Minecraft", instance.minecraftVersionId)
                if (instance.modLoader == ModLoader.VANILLA) {
                    PropertyRow("Loader", "Vanilla")
                } else {
                    PropertyRow(instance.modLoader.label, instance.loaderVersion ?: "Select during installation")
                }
                PropertyRow("Java runtime", "Java ${instance.requiredJavaMajor}")
            }
        }
        state.launchPlan?.let { plan ->
            item("launch-plan") {
                Column(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.ui_launch_plan), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = actions::inspectLaunchPlan) { Text(stringResource(Res.string.ui_refresh)) }
                    }
                    PropertyRow("Main class", plan.mainClass)
                    PropertyRow("Classpath", "${plan.classpathEntries} entries")
                    PropertyRow("Natives", "${plan.nativeLibraries} libraries")
                    PropertyRow("Account", plan.authentication)
                }
            }
        } ?: item("inspect") {
            Column(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = actions::inspectLaunchPlan,
                    enabled = instance.installationState is InstallationState.Installed,
                ) { Text(stringResource(Res.string.ui_inspect_launch_plan)) }
            }
        }
        if (state.gameLogLines.isNotEmpty() || state.lastCrashReport != null) {
            item("game-console") {
                Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(top = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.ui_game_console), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = actions::clearGameLog, enabled = state.gameLogLines.isNotEmpty()) {
                            Text(stringResource(Res.string.ui_clear))
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                state.gameLogLines.takeLast(120).joinToString("\n").ifBlank { "No process output." },
                                modifier = Modifier.padding(12.dp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    state.lastCrashReport?.let { report ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(Res.string.ui_crash_report_value, report),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { openPath(report) },
                                enabled = currentPlatform == "Desktop",
                            ) { Text(stringResource(Res.string.ui_open)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstanceContent(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    onDiscover: (ResourceType) -> Unit,
    listState: LazyListState,
    modifier: Modifier,
) {
    var dropActive by remember { mutableStateOf(false) }
    Box(
        modifier.localFileDropTarget(
            enabled = instance.installationState is InstallationState.Installed && currentPlatform == "Desktop",
            extensions = setOf("jar", "zip"),
            onActiveChange = { dropActive = it },
            onFiles = { files ->
                files.firstOrNull()?.let { actions.queueLocalFileImport(it.name, it.bytes) }
            },
            onFailure = actions::reportLocalFileReadFailure,
        ),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            item("installed-content") {
                InstalledContentPanel(state, instance, actions)
            }
            item("intro") {
                Text(
                    "Add compatible content to ${instance.displayName}. Required dependencies are resolved during installation.",
                    modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(top = 28.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(browsableResourceTypes.filterNot { it == ResourceType.MODPACK }, key = { it.name }) { type ->
                Column(Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                    ContentTypeRow(
                        type = type,
                        enabled = instance.installationState is InstallationState.Installed,
                        onClick = { onDiscover(type) },
                        localFileAction = {
                            LocalFileButton(
                                type = type,
                                enabled = instance.installationState is InstallationState.Installed,
                                actions = actions,
                            )
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        if (dropActive) DropOverlay("Drop to add content to ${instance.displayName}")
    }
}

@Composable
private fun InstalledContentPanel(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
) {
    val openPath = rememberOpenPath()
    val copyText = rememberCopyText()
    Column(
        Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.ui_installed_content), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Manage Trestle installs and compatible files already in ${instance.displayName}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = actions::refreshInstalledContent,
                enabled = !state.isLoadingInstalledContent,
            ) { Text(stringResource(Res.string.ui_refresh)) }
            OutlinedButton(
                onClick = actions::checkInstalledContentUpdates,
                enabled = state.installedContent.any { it.direct && it.isTracked } &&
                    !state.isCheckingInstalledContentUpdates,
            ) {
                Text(if (state.isCheckingInstalledContentUpdates) "Checking…" else "Check updates")
            }
            InstanceItemActions(
                buildList {
                    add("Copy installed list" to {
                        copyText(
                            state.installedContent.filter { it.direct }
                                .joinToString("\n") { content ->
                                    listOfNotNull(content.name, content.versionNumber).joinToString(" ")
                                },
                        )
                    })
                    if (currentPlatform == "Desktop") {
                        add("Open game folder" to { openPath("${instance.instanceDirectory}/game") })
                        add("Open config folder" to { openPath("${instance.instanceDirectory}/game/config") })
                    }
                },
            )
        }
        when {
            state.isLoadingInstalledContent -> Row(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.ui_reading_installed_content), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.installedContent.isEmpty() -> Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "No mods, resource packs, or shaders are installed yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                browsableResourceTypes.filterNot { it == ResourceType.MODPACK }.forEach { type ->
                    val contentForType = state.installedContent.filter { it.type == type }
                    if (contentForType.isNotEmpty()) {
                        Column {
                            Text(
                                "${type.label} (${contentForType.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                            contentForType.forEach { content ->
                                InstalledContentRow(
                                    content = content,
                                    update = state.installedContentUpdates[content.key],
                                    actions = actions,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledContentRow(
    content: InstalledContent,
    update: ResourceVersion?,
    actions: LauncherUiActions,
) {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(content.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (update != null) {
                        Text(
                            "Update ${update.versionNumber}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            },
            supportingContent = {
                val source = content.provider?.label ?: "Local file"
                val role = if (content.direct) source else {
                    "Dependency${if (content.requiredByCount > 1) " for ${content.requiredByCount} items" else ""}"
                }
                val details = buildList {
                    add(content.type.label.removeSuffix("s"))
                    content.versionNumber?.let(::add)
                    add(role)
                    if (content.sizeBytes > 0) add(formatFileSize(content.sizeBytes))
                    content.lastModifiedEpochMillis?.let { add("Modified ${formatLocalDateTime(it)}") }
                }
                Text(details.joinToString(" · "))
            },
            leadingContent = {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(content.type.label.take(1), style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            trailingContent = {
                Switch(
                    checked = content.enabled,
                    onCheckedChange = null,
                    enabled = content.canManage,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth().toggleable(
                value = content.enabled,
                enabled = content.canManage,
                role = Role.Switch,
                onValueChange = { actions.toggleInstalledContent(content.key) },
            ),
        )
        if (content.canManage) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                content.websiteUrl?.let { websiteUrl ->
                    TextButton(onClick = { uriHandler.openUri(websiteUrl) }) { Text(stringResource(Res.string.ui_homepage)) }
                }
                if (update != null) {
                    FilledTonalButton(onClick = { actions.updateInstalledContent(content.key) }) {
                        Text(stringResource(Res.string.ui_update))
                    }
                }
                TextButton(onClick = { actions.removeInstalledContent(content.key) }) { Text(stringResource(Res.string.ui_remove)) }
            }
        }
    }
}

@Composable
private fun InstanceConfiguration(
    instance: GameInstance,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    Column(modifier.verticalScroll(scrollState).padding(24.dp)) {
        Column(
            Modifier.widthIn(max = 820.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.ui_instance_settings), style = MaterialTheme.typography.titleLarge)
            Text(
                "Launch and Minecraft client settings apply only to ${instance.displayName}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PropertyRow("Java", "Java ${instance.requiredJavaMajor}")
            PropertyRow("Minimum memory", "${instance.memory.minimumMiB} MiB")
            PropertyRow("Maximum memory", "${instance.memory.maximumMiB} MiB")
            PropertyRow("JVM arguments", instance.jvmArguments.joinToString().ifBlank { "Automatic" })
            PropertyRow("Game arguments", instance.gameArguments.joinToString().ifBlank { "None" })
            PropertyRow("Java runtime", instance.javaExecutable ?: "Managed Java ${instance.requiredJavaMajor}")
            PropertyRow("Environment", if (instance.environmentVariables.isEmpty()) "Inherited" else "${instance.environmentVariables.size} custom")
            if (instance.preLaunchCommand.isNotEmpty()) {
                PropertyRow("Pre-launch", instance.preLaunchCommand.joinToString(" "))
            }
            if (instance.wrapperCommand.isNotEmpty()) {
                PropertyRow("Wrapper", instance.wrapperCommand.joinToString(" "))
            }
            if (instance.postExitCommand.isNotEmpty()) {
                PropertyRow("Post-exit", instance.postExitCommand.joinToString(" "))
            }
            OutlinedButton(
                onClick = actions::openInstanceSettings,
                modifier = Modifier.padding(top = 12.dp),
            ) { Text(stringResource(Res.string.ui_edit_instance_settings)) }
        }
    }
}

@Composable
private fun ResourceCatalogPage(
    state: LauncherUiState,
    modifier: Modifier,
    actions: LauncherUiActions,
    searchFocusRequest: Int,
) {
    LaunchedEffect(state.resourceBrowser.visible, state.resourceBrowser.presentation) {
        if (
            !state.resourceBrowser.visible ||
            state.resourceBrowser.presentation != ResourceBrowserPresentation.PAGE
        ) {
            actions.openResourceBrowser(presentation = ResourceBrowserPresentation.PAGE)
        }
    }
    Column(modifier.fillMaxSize().testTag(LauncherTestTags.DISCOVER)) {
        PageHeader(stringResource(Res.string.ui_discover))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (
            state.resourceBrowser.visible &&
            state.resourceBrowser.presentation == ResourceBrowserPresentation.PAGE
        ) {
            ResourceBrowserContent(
                state,
                actions,
                Modifier.fillMaxSize(),
                searchFocusRequest = searchFocusRequest,
            )
        } else {
            LoadingRows(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ContentTypeRow(
    type: ResourceType,
    enabled: Boolean,
    onClick: () -> Unit,
    localFileAction: @Composable () -> Unit,
) {
    val description = when (type) {
        ResourceType.MOD -> "Extend an installed game with compatible client mods and required dependencies."
        ResourceType.MODPACK -> "Create a complete, isolated instance from a curated pack."
        ResourceType.RESOURCE_PACK -> "Change textures, sounds, and presentation without changing game logic."
        ResourceType.SHADER_PACK -> "Add compatible lighting and rendering effects to an installed instance."
    }
    ListItem(
        headlineContent = { Text(type.label) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(description)
                if (!enabled) {
                    Text(
                        "Select and install an instance first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(type.label.take(1), style = MaterialTheme.typography.headlineMedium)
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                localFileAction()
                OutlinedButton(onClick = onClick, enabled = enabled) { Text(stringResource(Res.string.ui_browse)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LocalFileButton(
    type: ResourceType,
    enabled: Boolean,
    actions: LauncherUiActions,
) {
    val scope = rememberCoroutineScope()
    val extensions = when (type) {
        ResourceType.MOD -> listOf("jar")
        ResourceType.RESOURCE_PACK,
        ResourceType.SHADER_PACK,
        -> listOf("zip")
        ResourceType.MODPACK -> listOf("mrpack", "zip")
    }
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = extensions),
    ) { file ->
        if (file != null) {
            scope.launch {
                if (runCatching { file.size() }.getOrDefault(-1L) > MAX_LOCAL_IMPORT_BYTES) {
                    actions.reportLocalFileTooLarge(file.name)
                } else {
                    runCatching { file.readBytes() }
                        .onSuccess { actions.queueLocalFileImport(file.name, it, type) }
                        .onFailure { actions.reportLocalFileReadFailure(file.name) }
                }
            }
        }
    }
    TextButton(onClick = { picker.launch() }, enabled = enabled) { Text(stringResource(Res.string.ui_add_file)) }
}

private const val MAX_LOCAL_IMPORT_BYTES = 512L * 1024L * 1024L

@Composable
private fun AccountsPage(
    state: LauncherUiState,
    modifier: Modifier,
    actions: LauncherUiActions,
    layoutMode: TrestleLayoutMode,
) {
    if (state.skinStudio.visible && !state.skinStudio.editor.visible) {
        SkinStudioPage(state, actions, modifier)
        return
    }
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProfileId by rememberSaveable {
        mutableStateOf(state.accounts.firstOrNull { it.isActive }?.profile?.profileId)
    }
    val selectedAccount = state.accounts.firstOrNull { it.profile.profileId == selectedProfileId }
    val adaptiveInfo = LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo),
    )
    val scope = rememberCoroutineScope()
    val listPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val detailPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden
    val openAccount: (ManagedAccount) -> Unit = { account ->
        selectedProfileId = account.profile.profileId
        scope.launch {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, account.profile.profileId)
        }
    }
    val showAccounts: () -> Unit = {
        scope.launch {
            if (navigator.canNavigateBack()) navigator.navigateBack()
            else navigator.navigateTo(ListDetailPaneScaffoldRole.List)
        }
    }

    LaunchedEffect(state.accounts, detailPaneHidden) {
        if (selectedProfileId !in state.accounts.map { it.profile.profileId }) {
            selectedProfileId = state.accounts.firstOrNull { it.isActive }?.profile?.profileId
                ?: state.accounts.firstOrNull()?.profile?.profileId
        }
        if (!detailPaneHidden && selectedProfileId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedProfileId)
        }
    }
    PlatformBackHandler(
        enabled = listPaneHidden && navigator.canNavigateBack(),
        onBack = showAccounts,
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(LauncherTestTags.ACCOUNTS),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                PageHeader(
                    title = if (listPaneHidden && selectedAccount != null) {
                        selectedAccount.profile.playerName
                    } else {
                        stringResource(Res.string.ui_accounts)
                    },
                    navigationIcon = {
                        if (listPaneHidden) {
                            TrestleTooltipIconButton(
                                label = stringResource(Res.string.ui_back),
                                onClick = showAccounts,
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_arrow_back),
                                    contentDescription = stringResource(Res.string.ui_back),
                                )
                            }
                        }
                    },
                    actions = {
                        if (layoutMode != TrestleLayoutMode.COMPACT) {
                            TrestleTooltipIconButton(
                                label = stringResource(Res.string.ui_add_account),
                                onClick = actions::openAccountLogin,
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_add),
                                    contentDescription = stringResource(Res.string.ui_add_account),
                                )
                            }
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            if (layoutMode == TrestleLayoutMode.COMPACT && !listPaneHidden) {
                FloatingActionButton(onClick = actions::openAccountLogin) {
                    Icon(
                        painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.ui_add_account),
                    )
                }
            }
        },
    ) { contentPadding ->
        if (state.accounts.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.ui_no_accounts), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Add an online account, import an existing session, or create an offline profile.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 520.dp),
                )
                Button(
                    onClick = actions::openAccountLogin,
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text(stringResource(Res.string.ui_add_account)) }
            }
        } else {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                listPane = {
                    AnimatedPane(Modifier.preferredWidth(340.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                items(state.accounts, key = { it.profile.profileId }) { account ->
                                    AccountRow(
                                        account = account,
                                        texture = state.accountSkinTextures[account.profile.profileId],
                                        selected = account.profile.profileId == selectedProfileId,
                                        actions = actions,
                                        onOpen = { openAccount(account) },
                                        onForget = { pendingRemoval = account.profile.profileId },
                                    )
                                }
                            }
                        }
                    }
                },
                detailPane = {
                    AnimatedPane {
                        if (selectedAccount == null) {
                            Column(
                                Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(stringResource(Res.string.ui_accounts), style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Choose an account to view its profile.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            AccountDetail(
                                account = selectedAccount,
                                texture = state.accountSkinTextures[selectedAccount.profile.profileId],
                                actions = actions,
                                onForget = { pendingRemoval = selectedAccount.profile.profileId },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
            )
        }
    }
    pendingRemoval?.let { profileId ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(Res.string.ui_forget_account)) },
            text = {
                Text(stringResource(Res.string.ui_this_removes_the_local_profile_and_saved_credentials_it_does_not_change_))
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(Res.string.ui_cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.removeAccount(profileId)
                        pendingRemoval = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.ui_forget_account_2)) }
            },
        )
    }
}

@Composable
private fun AccountRow(
    account: ManagedAccount,
    texture: ByteArray?,
    selected: Boolean,
    actions: LauncherUiActions,
    onOpen: () -> Unit,
    onForget: () -> Unit,
) {
    val copyText = rememberCopyText()
    val profileId = account.profile.profileId
    var menuExpanded by rememberSaveable(profileId) { mutableStateOf(false) }
    val statusText = when {
        account.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE -> "Offline profile"
        !account.isAuthenticated -> "Sign-in required"
        account.profile.edition == MinecraftEdition.JAVA -> "Ready to launch"
        else -> "Bedrock saved"
    }
    val contextActions = buildList {
        if (!account.isActive) {
            add(ContextAction("Use account") { actions.selectAccount(profileId) })
        }
        if (account.canManageOfficialProfile) {
            add(ContextAction("Manage skins") { actions.openSkinStudio() })
            add(ContextAction("Refresh profile") { actions.refreshActiveAccount() })
            add(ContextAction("Reset skin") { actions.resetActiveSkin() })
        }
        if (account.isAuthenticated) {
            add(ContextAction("Sign out", separatorBefore = isNotEmpty()) { actions.signOutAccount(profileId) })
        }
        add(
            ContextAction(
                "Copy player name",
                separatorBefore = true,
            ) { copyText(account.profile.playerName) },
        )
        add(ContextAction("Copy profile ID") { copyText(profileId) })
        add(ContextAction("Forget account", separatorBefore = true, onClick = onForget))
    }

    ContextActionArea(contextActions) {
        ListItem(
            headlineContent = { Text(account.profile.playerName, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Text("${account.profile.authenticationMethod.label} · $statusText")
            },
            leadingContent = { AccountMark(account, texture, compact = true) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (account.isActive) {
                        Text(
                            stringResource(Res.string.ui_active),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    AccountOverflowMenu(
                        account = account,
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        actions = actions,
                        onForget = onForget,
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .trestleSelectable(
                    selected = selected,
                    onClickLabel = "Open ${account.profile.playerName}",
                    onClick = onOpen,
                )
                .testTag(LauncherTestTags.account(profileId)),
        )
    }
}

@Composable
private fun AccountDetail(
    account: ManagedAccount,
    texture: ByteArray?,
    actions: LauncherUiActions,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileId = account.profile.profileId
    val statusText = when {
        account.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE -> "Offline profile"
        !account.isAuthenticated -> "Sign-in required"
        account.profile.edition == MinecraftEdition.JAVA -> "Ready to launch"
        else -> "Bedrock saved"
    }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (account.profile.edition == MinecraftEdition.JAVA) {
            MinecraftSkinPreview(
                texture = texture,
                variant = account.profile.skin?.variant ?: SkinVariant.CLASSIC,
                modifier = Modifier.fillMaxWidth().height(260.dp),
                interactive = texture != null,
                emptyLabel = "Loading skin preview…",
            )
        } else {
            AccountMark(account, texture)
        }
        Column(
            Modifier.fillMaxWidth().widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(account.profile.playerName, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${account.profile.authenticationMethod.label} · $statusText",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            PropertyRow("Edition", account.profile.edition.name.lowercase().replaceFirstChar(Char::uppercase))
            PropertyRow("Profile ID", profileId)
            account.profile.skin?.let { skin -> PropertyRow("Player model", skin.variant.label) }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!account.isActive) {
                    Button(onClick = { actions.selectAccount(profileId) }) {
                        Text(stringResource(Res.string.ui_use))
                    }
                }
                if (account.canManageOfficialProfile) {
                    Button(onClick = actions::openSkinStudio) {
                        Text(stringResource(Res.string.ui_manage_skins))
                    }
                    OutlinedButton(onClick = actions::refreshActiveAccount) {
                        Text(stringResource(Res.string.ui_refresh))
                    }
                }
                if (account.isAuthenticated) {
                    OutlinedButton(onClick = { actions.signOutAccount(profileId) }) {
                        Text(stringResource(Res.string.ui_sign_out))
                    }
                }
            }
            TextButton(onClick = onForget, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(Res.string.ui_forget), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private val ManagedAccount.canManageOfficialProfile: Boolean
    get() = isActive &&
        isAuthenticated &&
        profile.edition == MinecraftEdition.JAVA &&
        profile.authenticationMethod != AccountAuthenticationMethod.THE_ALTENING

@Composable
private fun AccountOverflowMenu(
    account: ManagedAccount,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: LauncherUiActions,
    onForget: () -> Unit,
) {
    val profileId = account.profile.profileId
    Box {
        TrestleTooltipIconButton(
            label = stringResource(Res.string.ui_more),
            onClick = { onExpandedChange(true) },
        ) {
            Icon(
                painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.ui_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            if (
                account.isActive &&
                account.isAuthenticated &&
                account.profile.edition == MinecraftEdition.JAVA &&
                account.profile.authenticationMethod != AccountAuthenticationMethod.THE_ALTENING
            ) {
                DropdownMenuItem(
                    text = { Text("Refresh profile") },
                    onClick = {
                        onExpandedChange(false)
                        actions.refreshActiveAccount()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Reset skin") },
                    onClick = {
                        onExpandedChange(false)
                        actions.resetActiveSkin()
                    },
                )
                HorizontalDivider()
            }
            if (account.isAuthenticated) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.ui_sign_out)) },
                    onClick = {
                        onExpandedChange(false)
                        actions.signOutAccount(profileId)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.ui_forget), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onExpandedChange(false)
                    onForget()
                },
            )
        }
    }
}

@Composable
private fun SkinStudioPage(
    state: LauncherUiState,
    actions: LauncherUiActions,
    modifier: Modifier,
) {
    val account = state.accounts.firstOrNull { it.isActive }
    val selected = state.savedSkins.firstOrNull { it.profile.id == state.skinStudio.selectedProfileId }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .dismissOnEscape(onDismiss = actions::closeSkinStudio)
            .testTag(LauncherTestTags.SKIN_STUDIO),
    ) {
        PageHeader(
            title = stringResource(Res.string.ui_skins),
            navigationIcon = {
                TrestleTooltipIconButton(
                    label = stringResource(Res.string.ui_back),
                    onClick = actions::closeSkinStudio,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.ui_back),
                    )
                }
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize()) {
                    CurrentSkinPanel(
                        playerName = account?.profile?.playerName.orEmpty(),
                        texture = account?.let { state.accountSkinTextures[it.profile.profileId] },
                        variant = account?.profile?.skin?.variant ?: SkinVariant.CLASSIC,
                        onSave = actions::saveCurrentSkinToLibrary,
                        onReset = actions::resetActiveSkin,
                        modifier = Modifier.width(310.dp).fillMaxHeight(),
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SkinLibraryPanel(
                        skins = state.savedSkins,
                        selected = selected,
                        onSelect = actions::selectSavedSkin,
                        onNew = actions::openNewSkin,
                        onUse = actions::useSelectedSkin,
                        onEdit = actions::editSelectedSkin,
                        onDelete = { confirmDelete = true },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    CurrentSkinPanel(
                        playerName = account?.profile?.playerName.orEmpty(),
                        texture = account?.let { state.accountSkinTextures[it.profile.profileId] },
                        variant = account?.profile?.skin?.variant ?: SkinVariant.CLASSIC,
                        onSave = actions::saveCurrentSkinToLibrary,
                        onReset = actions::resetActiveSkin,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SkinLibraryPanel(
                        skins = state.savedSkins,
                        selected = selected,
                        onSelect = actions::selectSavedSkin,
                        onNew = actions::openNewSkin,
                        onUse = actions::useSelectedSkin,
                        onEdit = actions::editSelectedSkin,
                        onDelete = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
    if (confirmDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(stringResource(Res.string.ui_remove_named_skin, selected.profile.name))
            },
            text = {
                Text(stringResource(Res.string.ui_this_deletes_the_local_skin_profile_and_its_png_your_active_minecraft_sk))
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.ui_cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.deleteSelectedSkin()
                        confirmDelete = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.ui_remove_skin)) }
            },
        )
    }
}

@Composable
private fun CurrentSkinPanel(
    playerName: String,
    texture: ByteArray?,
    variant: SkinVariant,
    onSave: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.ui_current), style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Start))
        MinecraftSkinPreview(
            texture = texture,
            variant = variant,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
            emptyLabel = "Loading skin preview…",
        )
        Text(playerName, style = MaterialTheme.typography.titleMedium)
        Text(
            "${variant.label} model · Drag to rotate",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onSave, enabled = texture != null) { Text(stringResource(Res.string.ui_save_to_library)) }
            TextButton(onClick = onReset) { Text(stringResource(Res.string.ui_reset_to_default)) }
        }
    }
}

@Composable
private fun AccountMark(account: ManagedAccount, texture: ByteArray?, compact: Boolean = false) {
    if (account.profile.edition == MinecraftEdition.JAVA && texture != null) {
        MinecraftSkinPreview(
            texture = texture,
            variant = account.profile.skin?.variant ?: SkinVariant.CLASSIC,
            modifier = if (compact) Modifier.size(width = 44.dp, height = 64.dp) else Modifier.size(width = 56.dp, height = 80.dp),
            interactive = false,
            animate = false,
        )
        return
    }
    Box(
        Modifier.size(48.dp).background(
            if (account.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.shapes.small,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            account.profile.playerName.take(1).uppercase(),
            color = if (account.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun SkinLibraryPanel(
    skins: List<SavedSkin>,
    selected: SavedSkin?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.ui_library), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Button(onClick = onNew) { Text(stringResource(Res.string.ui_new_skin)) }
        }
        Spacer(Modifier.height(16.dp))
        if (skins.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.ui_no_saved_skins), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.ui_import_a_64x64_or_legacy_64x32_png_to_start_your_local_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onNew) { Text(stringResource(Res.string.ui_choose_a_skin_file)) }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItems(skins, key = { it.profile.id }) { skin ->
                    SkinLibraryItem(
                        skin = skin,
                        selected = skin.profile.id == selected?.profile?.id,
                        onClick = { onSelect(skin.profile.id) },
                        onDoubleClick = {
                            onSelect(skin.profile.id)
                            onUse()
                        },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDelete, enabled = selected != null) { Text(stringResource(Res.string.ui_delete)) }
                TextButton(onClick = onEdit, enabled = selected != null) { Text(stringResource(Res.string.ui_edit)) }
                Button(
                    onClick = onUse,
                    enabled = selected != null,
                ) { Text(stringResource(Res.string.ui_use_skin)) }
            }
        }
    }
}

@Composable
private fun SkinLibraryItem(
    skin: SavedSkin,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .trestleSelectable(
                selected = selected,
                onClickLabel = "Select skin",
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MinecraftSkinPreview(
                texture = skin.texture,
                variant = skin.profile.variant,
                modifier = Modifier.fillMaxWidth().height(142.dp),
                interactive = false,
                animate = false,
            )
            Text(
                skin.profile.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                skin.profile.variant.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SkinEditorDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val editor = state.skinStudio.editor
    val scope = rememberCoroutineScope()
    var dropActive by remember { mutableStateOf(false) }
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("png")),
    ) { file ->
        if (file != null) {
            scope.launch {
                runCatching { file.readBytes() }
                    .onSuccess { actions.setSkinFile(file.name, it) }
                    .onFailure { actions.reportSkinFileReadFailure() }
            }
        }
    }
    TrestleDialog(
        onDismissRequest = actions::closeSkinEditor,
        maxWidth = 820.dp,
        widthFraction = 0.88f,
        minHeight = 500.dp,
        maxHeight = 650.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !editor.isSaving, onDismiss = actions::closeSkinEditor)
            .localFileDropTarget(
                enabled = !editor.isSaving && currentPlatform == "Desktop",
                extensions = setOf("png"),
                onActiveChange = { dropActive = it },
                onFiles = { files ->
                    files.firstOrNull()?.let { actions.setSkinFile(it.name, it.bytes) }
                },
                onFailure = { actions.reportSkinFileReadFailure() },
            )
            .testTag(LauncherTestTags.SKIN_EDITOR_DIALOG),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TrestleDialogHeader(
                    title = if (editor.profileId == null) "Add new skin" else "Edit skin",
                    onClose = actions::closeSkinEditor,
                    closeEnabled = !editor.isSaving,
                )
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val compact = maxWidth < 650.dp
                    if (compact) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                            SkinEditorPreview(editor)
                            Spacer(Modifier.height(20.dp))
                            SkinEditorFields(editor, { picker.launch() }, actions)
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            SkinEditorPreview(editor, Modifier.width(300.dp).fillMaxHeight().padding(24.dp))
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SkinEditorFields(
                                editor = editor,
                                onBrowse = { picker.launch() },
                                actions = actions,
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                            )
                        }
                    }
                }
                TrestleDialogActions {
                    TextButton(onClick = actions::closeSkinEditor, enabled = !editor.isSaving) { Text(stringResource(Res.string.ui_cancel)) }
                    OutlinedButton(
                        onClick = { actions.saveSkin(useAfterSave = false) },
                        enabled = editor.canSave,
                    ) { Text(stringResource(Res.string.ui_save)) }
                    Button(
                        onClick = { actions.saveSkin(useAfterSave = true) },
                        enabled = editor.canSave,
                    ) { Text(if (editor.isSaving) "Saving" else "Save and use") }
                }
            }
            if (dropActive) DropOverlay("Drop PNG to use this skin")
        }
    }
}

@Composable
private fun SkinEditorPreview(editor: SkinEditorState, modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MinecraftSkinPreview(
            texture = editor.texture,
            variant = editor.variant,
            modifier = Modifier.fillMaxWidth().weight(1f),
            emptyLabel = "Choose a skin PNG",
        )
        Text(stringResource(Res.string.ui_drag_to_rotate), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SkinEditorFields(
    editor: SkinEditorState,
    onBrowse: () -> Unit,
    actions: LauncherUiActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextField(
            value = editor.name,
            onValueChange = actions::setSkinName,
            label = { Text(stringResource(Res.string.ui_name)) },
            singleLine = true,
            enabled = !editor.isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (editor.canSave) actions.saveSkin(useAfterSave = true) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.ui_player_model), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SkinVariant.entries.forEach { variant ->
                    Row(
                        Modifier.selectable(
                            selected = editor.variant == variant,
                            enabled = !editor.isSaving,
                            role = Role.RadioButton,
                            onClick = { actions.setSkinVariant(variant) },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = editor.variant == variant,
                            onClick = null,
                            enabled = !editor.isSaving,
                        )
                        Text(variant.label)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.ui_skin_file), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onBrowse, enabled = !editor.isSaving) {
                Text(if (editor.texture == null) "Choose PNG" else "Replace PNG")
            }
            editor.sourceFileName?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(stringResource(Res.string.ui_use_a_64x64_png_or_a_legacy_64x32_skin), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        editor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private val SkinVariant.label: String
    get() = when (this) {
        SkinVariant.CLASSIC -> "Classic"
        SkinVariant.SLIM -> "Slim"
    }

private val SkinEditorState.canSave: Boolean
    get() = name.isNotBlank() && texture != null && !isSaving

@Composable
private fun AccountLoginDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val form = state.accountLogin
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    var passwordVisible by remember(form.method) { mutableStateOf(false) }
    var importedSecretVisible by remember(form.method) { mutableStateOf(false) }
    TrestleDialog(
        onDismissRequest = { if (!form.isWaiting) actions.closeAccountLogin() },
        maxWidth = 520.dp,
        maxHeight = 720.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !form.isWaiting, onDismiss = actions::closeAccountLogin)
            .testTag(LauncherTestTags.ACCOUNT_LOGIN_DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_add_account),
                onClose = actions::closeAccountLogin,
                closeEnabled = !form.isWaiting,
            )
            Column(
                Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Choose how Trestle should create or verify this account.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Selector(
                    label = "Login method",
                    value = form.method.label,
                    values = AccountAuthenticationMethod.entries.map(AccountAuthenticationMethod::label),
                    enabled = !form.isWaiting,
                ) { selected ->
                    AccountAuthenticationMethod.entries.firstOrNull { it.label == selected }
                        ?.let(actions::setAccountLoginMethod)
                }
                if (form.edition == MinecraftEdition.BEDROCK) {
                    TextField(
                        value = form.bedrockGameVersion,
                        onValueChange = actions::setBedrockGameVersion,
                        label = { Text(stringResource(Res.string.ui_installed_bedrock_version)) },
                        placeholder = { Text(stringResource(Res.string.ui_for_example_1_21_100)) },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Bedrock authentication is stored for the future runtime adapter. Trestle cannot launch Bedrock yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (
                    form.method == AccountAuthenticationMethod.MICROSOFT_CREDENTIALS ||
                    form.method == AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS
                ) {
                    TextField(
                        value = form.email,
                        onValueChange = actions::setAccountEmail,
                        label = { Text(stringResource(Res.string.ui_microsoft_account_email)) },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.password.reveal(),
                        onValueChange = actions::setAccountPassword,
                        label = { Text(stringResource(Res.string.ui_microsoft_account_password)) },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TrestleTooltipIconButton(
                                label = if (passwordVisible) "Hide password" else "Show password",
                                onClick = { passwordVisible = !passwordVisible },
                            ) {
                                Icon(
                                    painterResource(
                                        if (passwordVisible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility,
                                    ),
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (form.canSubmit && !form.isWaiting) actions.signInAccount() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Microsoft discourages direct password login and it cannot complete MFA. " +
                            "Trestle discards the password after this attempt and stores only encrypted token state.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (form.method.requiresImportedSecret) {
                    TextField(
                        value = form.importedSecret.reveal(),
                        onValueChange = actions::setImportedAccountSecret,
                        label = { Text(form.method.secretInputLabel) },
                        enabled = !form.isWaiting,
                        singleLine = form.method != AccountAuthenticationMethod.MICROSOFT_COOKIES,
                        minLines = if (form.method == AccountAuthenticationMethod.MICROSOFT_COOKIES) 3 else 1,
                        maxLines = if (form.method == AccountAuthenticationMethod.MICROSOFT_COOKIES) 5 else 1,
                        visualTransformation = if (importedSecretVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TrestleTooltipIconButton(
                                label = if (importedSecretVisible) "Hide secret" else "Show secret",
                                onClick = { importedSecretVisible = !importedSecretVisible },
                            ) {
                                Icon(
                                    painterResource(
                                        if (importedSecretVisible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility,
                                    ),
                                    contentDescription = if (importedSecretVisible) "Hide secret" else "Show secret",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(form.method.importWarning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (form.method == AccountAuthenticationMethod.OFFLINE) {
                    TextField(
                        value = form.offlineUsername,
                        onValueChange = actions::setOfflineUsername,
                        label = { Text(stringResource(Res.string.ui_offline_username)) },
                        isError = form.offlineUsername.isNotEmpty() &&
                            !form.offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$")),
                        supportingText = {
                            Text(
                                if (form.offlineUsername.isNotEmpty() &&
                                    !form.offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$"))
                                ) {
                                    "Use 1 to 16 letters, numbers, or underscores."
                                } else {
                                    "1 to 16 letters, numbers, or underscores"
                                },
                            )
                        },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (form.canSubmit && !form.isWaiting) actions.signInAccount() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Offline accounts prove no ownership. They only work with single-player and servers that allow offline identities.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                form.authorization?.let { authorization ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.ui_enter_this_code), color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                authorization.userCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(authorization.verificationUri, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Button(
                                onClick = { uriHandler.openUri(authorization.directVerificationUri) },
                            ) { Text(stringResource(Res.string.ui_open_microsoft_sign_in)) }
                        }
                    }
                }
            }
            TrestleDialogActions {
                TextButton(onClick = actions::closeAccountLogin) { Text(stringResource(Res.string.ui_cancel)) }
                if (form.authorization == null) {
                    Button(
                        onClick = actions::signInAccount,
                        enabled = !form.isWaiting && form.canSubmit,
                    ) {
                        Text(
                            if (form.isWaiting) {
                                "Waiting…"
                            } else {
                                when (form.method) {
                                    AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                                    AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE,
                                    -> "Get sign-in code"
                                    AccountAuthenticationMethod.MICROSOFT_CREDENTIALS,
                                    AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS,
                                    -> "Sign in"
                                    AccountAuthenticationMethod.OFFLINE -> "Add offline account"
                                    else -> "Import account"
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private val AccountAuthenticationMethod.requiresImportedSecret: Boolean
    get() = this == AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ||
        this == AccountAuthenticationMethod.MICROSOFT_COOKIES ||
        this == AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN ||
        this == AccountAuthenticationMethod.THE_ALTENING

private val AccountAuthenticationMethod.secretInputLabel: String
    get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN -> "Microsoft refresh token"
        AccountAuthenticationMethod.MICROSOFT_COOKIES -> "login.live.com cookies or cookie export"
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> "Minecraft access token"
        AccountAuthenticationMethod.THE_ALTENING -> "TheAltening account token"
        else -> "Imported secret"
    }

private val AccountAuthenticationMethod.importWarning: String
    get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ->
            "Use a refresh token issued for the same Minecraft title configuration. It is exchanged and stored encrypted."
        AccountAuthenticationMethod.MICROSOFT_COOKIES ->
            "Cookies grant access to your Microsoft session. Trestle exchanges them once, then stores only encrypted token state."
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN ->
            "Raw Minecraft access tokens cannot be renewed. Trestle validates the profile and stores the token encrypted until it expires."
        AccountAuthenticationMethod.THE_ALTENING ->
            "This third-party provider uses an unencrypted HTTP authentication and session endpoint. Do not reuse this token elsewhere."
        else -> ""
    }

private val AccountLoginState.canSubmit: Boolean
    get() = when (method) {
        AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE -> true
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE -> bedrockGameVersion.isNotBlank()
        AccountAuthenticationMethod.MICROSOFT_CREDENTIALS -> email.isNotBlank() && !password.isBlank()
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS ->
            bedrockGameVersion.isNotBlank() && email.isNotBlank() && !password.isBlank()
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN,
        AccountAuthenticationMethod.MICROSOFT_COOKIES,
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN,
        AccountAuthenticationMethod.THE_ALTENING,
        -> !importedSecret.isBlank()
        AccountAuthenticationMethod.OFFLINE -> offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$"))
    }

@Composable
private fun SettingsColumn(
    title: StringResource,
    scrollState: ScrollState,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 840.dp).verticalScroll(scrollState).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
internal fun GeneralSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_general, scrollState, modifier) {
    val preferences = state.launcherPreferences
    Text(stringResource(Res.string.ui_instance_sorting), style = MaterialTheme.typography.titleMedium)
    SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
        InstanceSortMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = preferences.instanceSort == mode,
                onClick = { actions.setLauncherPreferences(preferences.copy(instanceSort = mode)) },
                shape = SegmentedButtonDefaults.itemShape(index, InstanceSortMode.entries.size),
            ) { Text(mode.label) }
        }
    }
    Text(
        "Instance folders use stable IDs, so renaming an instance never invalidates its files.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun LanguageSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_language, scrollState, modifier) {
    val preferences = state.launcherPreferences
    Selector(
        label = "Interface language",
        value = preferences.language,
        values = listOf("System default", "English"),
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        onSelect = { actions.setLauncherPreferences(preferences.copy(language = it)) },
    )
    Text(
        "Trestle currently ships English text. The saved language preference is ready for additional translations.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun FolderSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_folders, scrollState, modifier) {
    val preferences = state.launcherPreferences
    val folders = preferences.folders
    FolderPreferenceField("Instances", folders.instances, state.defaultFolders.instances) {
        actions.setLauncherPreferences(preferences.copy(folders = folders.copy(instances = it)))
    }
    FolderPreferenceField("Java runtimes", folders.runtimes, state.defaultFolders.runtimes) {
        actions.setLauncherPreferences(preferences.copy(folders = folders.copy(runtimes = it)))
    }
    FolderPreferenceField("Skins", folders.skins, state.defaultFolders.skins) {
        actions.setLauncherPreferences(preferences.copy(folders = folders.copy(skins = it)))
    }
    FolderPreferenceField("Downloads and exports", folders.downloads, state.defaultFolders.downloads) {
        actions.setLauncherPreferences(preferences.copy(folders = folders.copy(downloads = it)))
    }
    Text(stringResource(Res.string.ui_folder_changes_apply_after_trestle_restarts), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FolderPreferenceField(label: String, value: String, defaultValue: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(defaultValue) },
        supportingText = { Text(if (value.isBlank()) "Default: $defaultValue" else "Custom location") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
}

@Composable
internal fun ContentSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_mods_and_modpacks, scrollState, modifier) {
    val preferences = state.launcherPreferences
    val content = preferences.content
    SettingsSwitch("Scan subfolders for blocked mods", content.scanSubfolders) {
        actions.setLauncherPreferences(preferences.copy(content = content.copy(scanSubfolders = it)))
    }
    SettingsSwitch("Move blocked mods instead of copying them", content.moveBlockedFiles) {
        actions.setLauncherPreferences(preferences.copy(content = content.copy(moveBlockedFiles = it)))
    }
    SettingsSwitch("Keep track of content metadata", content.trackMetadata) {
        actions.setLauncherPreferences(preferences.copy(content = content.copy(trackMetadata = it)))
    }
    SettingsSwitch("Install required dependencies automatically", content.installDependencies) {
        actions.setLauncherPreferences(preferences.copy(content = content.copy(installDependencies = it)))
    }
    SettingsSwitch("Detect incompatible content", content.detectIncompatibilities) {
        actions.setLauncherPreferences(preferences.copy(content = content.copy(detectIncompatibilities = it)))
    }
    SettingsSwitch("Suggest updating an existing instance during modpack installation", content.suggestModpackUpdates) {
        actions.setLauncherPreferences(preferences.copy(content = content.copy(suggestModpackUpdates = it)))
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    TrestleSwitchItem(
        label = label,
        checked = checked,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        onCheckedChange = onCheckedChange,
    )
}

@Composable
internal fun NetworkSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_tasks_and_downloads, scrollState, modifier) {
    val preferences = state.launcherPreferences
    val network = preferences.network
    IntegerSlider("Concurrent task limit", network.concurrentTasks.toString(), network.concurrentTasks, 1..64) {
        actions.setLauncherPreferences(preferences.copy(network = network.copy(concurrentTasks = it)))
    }
    IntegerSlider("Concurrent download limit", network.concurrentDownloads.toString(), network.concurrentDownloads, 1..32) {
        actions.setLauncherPreferences(preferences.copy(network = network.copy(concurrentDownloads = it)))
    }
    IntegerSlider("Retry limit", network.retryLimit.toString(), network.retryLimit, 1..10) {
        actions.setLauncherPreferences(preferences.copy(network = network.copy(retryLimit = it)))
    }
    IntegerSlider("HTTP timeout", "${network.httpTimeoutSeconds}s", network.httpTimeoutSeconds, 5..300, steps = 58) {
        actions.setLauncherPreferences(preferences.copy(network = network.copy(httpTimeoutSeconds = it)))
    }
    HorizontalDivider()
    Text(stringResource(Res.string.ui_console), style = MaterialTheme.typography.titleMedium)
    IntegerSlider(
        "Log history limit",
        "${preferences.console.historyLimit} lines",
        preferences.console.historyLimit,
        1_000..100_000,
        steps = 98,
    ) {
        actions.setLauncherPreferences(preferences.copy(console = preferences.console.copy(historyLimit = it)))
    }
    SettingsSwitch("Stop logging when the history limit is reached", preferences.console.stopLoggingOnOverflow) {
        actions.setLauncherPreferences(preferences.copy(console = preferences.console.copy(stopLoggingOnOverflow = it)))
    }
    Text(stringResource(Res.string.ui_http_timeout_and_proxy_changes_apply_to_new_connections_after_restart), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ProxySettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_proxy, scrollState, modifier) {
    val preferences = state.launcherPreferences
    val proxy = preferences.proxy
    Text(stringResource(Res.string.ui_proxy_settings_apply_to_trestle_minecraft_does_not_use_them), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Selector(
        label = "Type",
        value = proxy.type.label,
        values = LauncherProxyType.entries.map { it.label },
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        onSelect = { label ->
            LauncherProxyType.entries.firstOrNull { it.label == label }?.let {
                actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(type = it)))
            }
        },
    )
    val editable = proxy.type == LauncherProxyType.HTTP || proxy.type == LauncherProxyType.SOCKS5
    Row(Modifier.fillMaxWidth().widthIn(max = 760.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField(
            value = proxy.host,
            onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(host = it))) },
            label = { Text(stringResource(Res.string.ui_address)) },
            enabled = editable,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextField(
            value = proxy.port.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.takeIf { it in 1..65535 }?.let {
                    actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(port = it)))
                }
            },
            label = { Text(stringResource(Res.string.ui_port)) },
            enabled = editable,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(140.dp),
        )
    }
    TextField(
        value = proxy.username,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(username = it))) },
        label = { Text(stringResource(Res.string.ui_username)) },
        enabled = editable,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
    TextField(
        value = proxy.password,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(password = it))) },
        label = { Text(stringResource(Res.string.ui_password)) },
        enabled = editable,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
    Text(stringResource(Res.string.ui_proxy_credentials_are_stored_in_the_launcher_preferences_file), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ServiceSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_services, scrollState, modifier) {
    val preferences = state.launcherPreferences
    Text(stringResource(Res.string.ui_modrinth), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(Res.string.ui_available_without_an_api_key), color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider()
    Text(stringResource(Res.string.ui_curseforge), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(Res.string.ui_availability_is_controlled_by_the_trestle_build_api_key), color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider()
    Text(stringResource(Res.string.ui_atlauncher), style = MaterialTheme.typography.titleMedium)
    Text(
        "The public catalog is available without an API key.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider()
    Text(stringResource(Res.string.ui_technic), style = MaterialTheme.typography.titleMedium)
    TextField(
        value = preferences.technicClientId,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(technicClientId = it)) },
        label = { Text(stringResource(Res.string.ui_client_id)) },
        supportingText = { Text(stringResource(Res.string.ui_optional_some_private_or_rate_limited_technic_packs_require_it_applies_a)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
    TextField(
        value = preferences.ftbAppInstancesPath,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(ftbAppInstancesPath = it)) },
        label = { Text(stringResource(Res.string.ui_ftb_app_instances_folder)) },
        supportingText = { Text(stringResource(Res.string.ui_used_when_importing_existing_ftb_app_instances)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
}

@Composable
internal fun ToolSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn(Res.string.ui_tools, scrollState, modifier) {
    OutlinedButton(onClick = actions::refreshVersions) {
        Text(if (state.isLoadingVersions) "Refreshing versions…" else "Refresh Minecraft metadata")
    }
    OutlinedButton(onClick = actions::checkForLauncherUpdate, enabled = !state.isCheckingForUpdate) {
        Text(if (state.isCheckingForUpdate) "Checking…" else "Check for Trestle updates")
    }
    Text(
        "Instance export, launch-plan inspection, log diagnostics, and file-management tools remain available from each instance.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun AppearanceSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.ui_appearance), style = MaterialTheme.typography.titleLarge)
        Text(
            "Choose how Trestle follows your device appearance. The preference applies on the next screen immediately.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 640.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            ThemePreference.entries.forEachIndexed { index, preference ->
                SegmentedButton(
                    selected = state.themePreference == preference,
                    onClick = { actions.setThemePreference(preference) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemePreference.entries.size),
                ) { Text(preference.label) }
            }
        }
        Text(
            "Desktop system accent colors remain available in every mode.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun RuntimeSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.ui_runtime), style = MaterialTheme.typography.titleLarge)
        Text(
            "Trestle resolves the platform runtime and compatible Minecraft metadata automatically.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 640.dp).padding(bottom = 8.dp),
        )
        PropertyRow("Platform", currentPlatform)
        PropertyRow("Instances", state.instances.size.toString())
        PropertyRow("Accounts", state.accounts.size.toString())
        PropertyRow("Total launches", state.instances.sumOf(GameInstance::launchCount).toString())
        PropertyRow("Total play time", formatPlayTime(state.instances.sumOf(GameInstance::playTimeMillis)))
        state.credentialProtection?.let { protection ->
            PropertyRow(
                "Credential vault",
                if (protection.encryptionOperational) protection.effectiveLevel else "Unavailable",
            )
        }
        Text(
            if (currentPlatform == "Android") {
                "Android provisions its Java runtime and verified native game components when the selected version is launched."
            } else {
                "Desktop launch preparation downloads and uses Mojang's compatible Java runtime automatically."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 640.dp),
        )
        OutlinedButton(
            onClick = actions::refreshVersions,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(if (state.isLoadingVersions) "Refreshing versions…" else "Refresh versions") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = actions::checkForLauncherUpdate,
                enabled = !state.isCheckingForUpdate,
            ) { Text(if (state.isCheckingForUpdate) "Checking…" else "Check for Trestle updates") }
            state.availableUpdate?.let { update ->
                TextButton(onClick = { uriHandler.openUri(update.releaseUrl) }) {
                    Text(stringResource(Res.string.ui_open_version_release, update.version))
                }
            }
        }
    }
}

@Composable
internal fun LauncherLog(
    state: LauncherUiState,
    actions: LauncherUiActions,
    listState: LazyListState,
    modifier: Modifier,
) {
    var selectedEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        item("logs-heading") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(Res.string.ui_launcher_log), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(Res.string.ui_events_from_this_session_right_click_an_entry_to_copy_diagnostics), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = actions::clearLogs, enabled = state.logs.isNotEmpty()) { Text(stringResource(Res.string.ui_clear)) }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (state.logs.isEmpty()) {
            item("logs-empty") { Text(stringResource(Res.string.ui_no_launcher_events_in_this_session), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.logs.takeLast(80).asReversed(), key = { it.id }) { entry ->
                LogRow(entry, onOpen = { selectedEntryId = entry.id })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    selectedEntryId?.let { entryId ->
        state.logs.firstOrNull { it.id == entryId }?.let { entry ->
            LogDetailsDialog(entry) { selectedEntryId = null }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry, onOpen: () -> Unit) {
    val copyText = rememberCopyText()
    val actions = buildList {
        add(ContextAction("Copy message") { copyText(entry.message) })
        if (entry.details.isNotEmpty()) {
            add(ContextAction("Copy details") { copyText(formatLogDetails(entry)) })
        }
        add(ContextAction("Copy event", separatorBefore = true) {
            copyText(formatLogEntryForClipboard(entry))
        })
    }
    ContextActionArea(actions) {
        ListItem(
            leadingContent = {
                Column(Modifier.width(68.dp)) {
                    Text(
                        formatUtcTime(entry.timestampEpochMillis),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        entry.level.name,
                        color = if (entry.level.name == "ERROR") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
            headlineContent = { Text(entry.message, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = {
                Text(
                    buildString {
                        append(entry.category)
                        if (entry.details.isNotEmpty()) {
                            append(" · ")
                            append(formatLogDetails(entry))
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth().clickable(
                onClickLabel = "Open log details",
                role = Role.Button,
                onClick = onOpen,
            ),
        )
    }
}

@Composable
private fun LogDetailsDialog(entry: LogEntry, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.message) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyRow("Time", formatUtcTime(entry.timestampEpochMillis))
                PropertyRow("Level", entry.level.name)
                PropertyRow("Category", entry.category)
                if (entry.details.isNotEmpty()) PropertyRow("Details", formatLogDetails(entry))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_close)) } },
    )
}

private fun formatUtcTime(epochMillis: Long): String {
    val secondsPerDay = 24L * 60L * 60L
    val seconds = ((epochMillis / 1_000L) % secondsPerDay + secondsPerDay) % secondsPerDay
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remainingSeconds = seconds % 60L
    return "${hours.toString().padStart(2, '0')}:" +
        "${minutes.toString().padStart(2, '0')}:" +
        "${remainingSeconds.toString().padStart(2, '0')}Z"
}

private fun formatDownloads(downloads: Long): String = when {
    downloads >= 1_000_000_000L -> "${downloads / 100_000_000L / 10.0}B downloads"
    downloads >= 1_000_000L -> "${downloads / 100_000L / 10.0}M downloads"
    downloads >= 1_000L -> "${downloads / 100L / 10.0}K downloads"
    else -> "$downloads downloads"
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000_000L -> "${count / 100_000_000L / 10.0}B"
    count >= 1_000_000L -> "${count / 100_000L / 10.0}M"
    count >= 1_000L -> "${count / 100L / 10.0}K"
    else -> count.toString()
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${bytes / 107_374_182L / 10.0} GiB"
    bytes >= 1_048_576L -> "${bytes / 104_857L / 10.0} MiB"
    bytes >= 1_024L -> "${bytes / 102L / 10.0} KiB"
    else -> "$bytes B"
}

private fun formatInstanceForClipboard(instance: GameInstance): String = buildString {
    appendLine(instance.displayName)
    appendLine("Minecraft ${instance.minecraftVersionId}")
    appendLine("${instance.modLoader.label} · Java ${instance.requiredJavaMajor}")
    append(instance.instanceDirectory)
}

private fun formatPlayTime(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        milliseconds > 0 -> "Less than a minute"
        else -> "Not played"
    }
}

private fun formatLogDetails(entry: LogEntry): String =
    entry.details.entries.joinToString { (key, value) -> "$key=$value" }

private fun formatLogEntryForClipboard(entry: LogEntry): String = buildString {
    append(formatUtcTime(entry.timestampEpochMillis))
    append(" · ")
    append(entry.level.name)
    append(" · ")
    append(entry.category)
    appendLine()
    append(entry.message)
    if (entry.details.isNotEmpty()) {
        appendLine()
        append(formatLogDetails(entry))
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(value) },
        overlineContent = { Text(label) },
        trailingContent = if (actionLabel == null || onClick == null || !actionEnabled) {
            null
        } else {
            { TextButton(onClick = onClick) { Text(actionLabel) } }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun stateLabel(state: InstallationState): String = when (state) {
    InstallationState.NotInstalled -> "Not installed"
    is InstallationState.Installing -> "Installing"
    is InstallationState.Interrupted -> "Ready to resume"
    is InstallationState.Installed -> "Installed"
    is InstallationState.Failed -> "Install failed"
}

@Composable
private fun stateColor(state: InstallationState) = when (state) {
    is InstallationState.Installed -> MaterialTheme.colorScheme.onSurfaceVariant
    is InstallationState.Interrupted -> MaterialTheme.colorScheme.primary
    is InstallationState.Failed -> MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private data class InstallationProgressSnapshot(
    val completedBytes: Long,
    val totalBytes: Long?,
    val completedFiles: Int,
    val totalFiles: Int,
)

private fun progressFraction(
    completedBytes: Long?,
    totalBytes: Long?,
    completedFiles: Int?,
    totalFiles: Int?,
): Float? = when {
    completedBytes != null && totalBytes != null && totalBytes > 0L ->
        (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    completedFiles != null && totalFiles != null && totalFiles > 0 ->
        (completedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)
    else -> null
}

private fun InstallationState.installationProgress(): InstallationProgressSnapshot? = when (this) {
    is InstallationState.Installing -> InstallationProgressSnapshot(
        completedBytes,
        totalBytes,
        completedFiles,
        totalFiles,
    )
    is InstallationState.Interrupted -> InstallationProgressSnapshot(
        completedBytes,
        totalBytes,
        completedFiles,
        totalFiles,
    )
    else -> null
}
