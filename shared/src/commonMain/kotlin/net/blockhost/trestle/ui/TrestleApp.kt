@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ui_accounts
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_delete_files
import net.blockhost.trestle.resources.ui_delete_named_world
import net.blockhost.trestle.resources.ui_delete_world
import net.blockhost.trestle.resources.ui_discover
import net.blockhost.trestle.resources.ui_instance
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_move_instance_to_trash_explanation
import net.blockhost.trestle.resources.ui_move_named_to_trash
import net.blockhost.trestle.resources.ui_move_to_trash
import net.blockhost.trestle.resources.ui_remove
import net.blockhost.trestle.resources.ui_remove_instance_explanation
import net.blockhost.trestle.resources.ui_remove_named_instance
import net.blockhost.trestle.resources.ui_retry
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_this_permanently_deletes_the_world_directory_existing_zip_backups_remain
import net.blockhost.trestle.resources.ui_undo
import net.blockhost.trestle.text.resolve
import net.blockhost.trestle.text.text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class LauncherDestination(val label: StringResource) {
    LIBRARY(Res.string.ui_library),
    INSTANCE(Res.string.ui_instance),
    DISCOVER(Res.string.ui_discover),
    ACCOUNTS(Res.string.ui_accounts),
    SETTINGS(Res.string.ui_settings),
}

internal val browsableResourceTypes = listOf(
    ResourceType.MOD,
    ResourceType.MODPACK,
    ResourceType.RESOURCE_PACK,
    ResourceType.SHADER_PACK,
)

internal val installableResourceTypes = browsableResourceTypes.toSet()

internal val WideContentWidth = 1200.dp

internal object LauncherTestTags {
    const val ROOT = "launcher-root"
    const val TOP_NAVIGATION = "top-navigation"
    const val LIBRARY = "library"
    const val INSTANCE_SEARCH = "instance-search"
    const val RESOURCE_SEARCH = "resource-search"
    const val NEW_INSTANCE = "new-instance"
    const val SELECTED_INSTANCE = "selected-instance"
    const val PRIMARY_INSTANCE_ACTION = "primary-instance-action"
    const val LAUNCH_ACCOUNT_REQUIREMENT = "launch-account-requirement"
    const val INSTANCE_WORKSPACE = "instance-workspace"
    const val INSTANCE_BACK = "instance-back"
    const val INSTANCE_HEADER_EXPANDED = "instance-header-expanded"
    const val VERSION_COMPONENTS_CHANGE = "version-components-change"
    const val INSTANCE_CONFIGURATION = "instance-configuration"
    const val DISCOVER = "discover"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
    const val SETTINGS_CATEGORIES = "settings-categories"
    const val SETTINGS_DETAIL = "settings-detail"
    const val RESOURCE_FILTERS = "resource-filters"
    const val RESOURCE_RESULTS = "resource-results"
    const val RESOURCE_DETAIL = "resource-detail"
    const val INSTALL_BLOCKED_REASON = "install-blocked-reason"
    const val CREATE_DIALOG = "create-dialog"
    const val FIXED_CREATE_VERSION = "fixed-create-version"
    const val FIXED_CREATE_LOADER = "fixed-create-loader"
    const val CREATE_RELEASE_FILTERS = "create-release-filters"
    const val INSTANCE_SETTINGS_DIALOG = "instance-settings-dialog"
    const val INSTANCE_ICON_EDIT = "instance-icon-edit"
    const val INSTANCE_ICON_DIALOG = "instance-icon-dialog"
    const val INSTANCE_ICON_UPLOAD = "instance-icon-upload"
    const val RESOURCE_BROWSER_DIALOG = "resource-browser-dialog"
    const val ACCOUNT_LOGIN_DIALOG = "account-login-dialog"
    const val ACCOUNT_LOGIN_ERROR = "account-login-error"
    const val SKIN_STUDIO = "skin-studio"
    const val SKIN_EDITOR_DIALOG = "skin-editor-dialog"

    fun instance(id: InstanceId): String = "instance-${id.value}"
    fun instancePlay(id: InstanceId): String = "instance-play-${id.value}"
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

    LauncherLanguageEnvironment(state.launcherPreferences.launcherLanguage) {
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
                (state.instanceSettings.visible && !state.instanceSettings.inline) ||
                state.accountLogin.visible ||
                state.skinStudio.editor.visible ||
                state.localFileImport.visible ||
                state.serverEditor.visible ||
                state.pendingInstanceRemovalId != null ||
                state.pendingWorldDeletionKey != null ||
                showShortcuts || state.restrictedDownloads.isNotEmpty() || state.modpackUpdatePreview != null ||
                state.suggestedPackInstances.isNotEmpty() ||
                (
                    state.resourceBrowser.visible &&
                        state.resourceBrowser.presentation == ResourceBrowserPresentation.DIALOG
                    )
            val retryActionLabel = stringResource(Res.string.ui_retry)
            val undoActionLabel = stringResource(Res.string.ui_undo)
            // Messages are resolved here, in composition, so the snackbar shows them in the interface language.
            val errorText = state.error?.resolve()
            val noticeText = state.notice?.resolve()
            LaunchedEffect(errorText, noticeText, retryActionLabel, undoActionLabel) {
                val message = errorText ?: noticeText ?: return@LaunchedEffect
                val action = when {
                    state.error != null && state.errorRecovery != null -> LauncherSnackbarAction.RETRY
                    state.removedInstanceUndo != null && state.notice == INSTANCE_REMOVED_NOTICE -> LauncherSnackbarAction.UNDO
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
                        action == LauncherSnackbarAction.UNDO -> SnackbarDuration.Long
                        else -> SnackbarDuration.Short
                    },
                )
                if (result == SnackbarResult.ActionPerformed) {
                    when (action) {
                        LauncherSnackbarAction.RETRY -> actions.retryError()
                        LauncherSnackbarAction.UNDO -> actions.undoInstanceRemoval()
                        null -> Unit
                    }
                } else {
                    actions.clearMessage()
                }
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
                            else -> null
                        }
                        command?.let(handleCommand) ?: false
                    }
                    // Escape is handled after focused children, so a text field or pane can use it first.
                    .onKeyEvent { event ->
                        val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape && !modalVisible
                        when {
                            !escape -> false
                            EscapeBackRegistry.handle() -> true
                            destination == LauncherDestination.INSTANCE -> handleCommand(LauncherCommand.SHOW_LIBRARY)
                            else -> false
                        }
                    }
                    .testTag(LauncherTestTags.ROOT),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        topBar()
                        state.availableUpdate?.let { LauncherUpdateBanner(it, actions) }
                    }
                },
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
                                onOpenAccounts = { changeDestination(LauncherDestination.ACCOUNTS) },
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
                    snackbarHost = { SnackbarHost(snackbarHostState) },
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
                if (state.instanceSettings.visible && !state.instanceSettings.inline) {
                    InstanceSettingsDialog(state, actions)
                }
                if (state.accountLogin.visible) AccountLoginDialog(state, actions)
                if (state.skinStudio.editor.visible) SkinEditorDialog(state, actions)
                if (state.localFileImport.visible) LocalFileImportDialog(state, actions)
                if (state.serverEditor.visible) ServerEditorDialog(state, actions)
                if (showShortcuts) ShortcutsDialog { showShortcuts = false }
                state.pendingInstanceRemovalId?.let { pendingId ->
                    val instance = state.instances.firstOrNull { it.id == pendingId }
                    InstanceRemovalDialog(
                        instanceName = instance?.displayName ?: stringResource(Res.string.ui_instance),
                        moveToTrash = state.instanceRemovalMode == InstanceRemovalMode.MOVE_TO_TRASH,
                        actions = actions,
                    )
                }
                RestrictedDownloadDialog(state, actions)
                ModpackUpdateDialogs(state, actions)
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
}

private enum class LauncherSnackbarAction {
    RETRY,
    UNDO,
}

/**
 * Confirms removing an instance. Removing keeps the files and is the main action;
 * deleting the files is a separate, clearly destructive choice.
 */
@Composable
private fun InstanceRemovalDialog(instanceName: String, moveToTrash: Boolean, actions: LauncherUiActions) {
    AlertDialog(
        onDismissRequest = actions::cancelInstanceRemoval,
        title = {
            Text(
                if (moveToTrash) {
                    stringResource(Res.string.ui_move_named_to_trash, instanceName)
                } else {
                    stringResource(Res.string.ui_remove_named_instance, instanceName)
                },
            )
        },
        text = {
            Text(
                if (moveToTrash) {
                    stringResource(Res.string.ui_move_instance_to_trash_explanation)
                } else {
                    stringResource(Res.string.ui_remove_instance_explanation)
                },
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!moveToTrash) {
                    TextButton(
                        onClick = actions::confirmInstanceDeletion,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(Res.string.ui_delete_files)) }
                }
                TextButton(onClick = actions::cancelInstanceRemoval) { Text(stringResource(Res.string.ui_cancel)) }
            }
        },
        confirmButton = {
            Button(onClick = actions::confirmInstanceRemoval) {
                Text(
                    if (moveToTrash) {
                        stringResource(Res.string.ui_move_to_trash)
                    } else {
                        stringResource(Res.string.ui_remove)
                    },
                )
            }
        },
    )
}
