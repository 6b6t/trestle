@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_add
import net.blockhost.trestle.resources.ic_account
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_extension
import net.blockhost.trestle.resources.ic_library
import net.blockhost.trestle.resources.ic_close
import net.blockhost.trestle.resources.ic_search
import net.blockhost.trestle.resources.ic_settings
import net.blockhost.trestle.resources.ic_visibility
import net.blockhost.trestle.resources.ic_visibility_off
import net.blockhost.trestle.resources.trestle_mark
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.app.InstanceSortMode
import net.blockhost.trestle.app.LauncherProxyType
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ReleaseChannel
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.DependencyKind
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.MinecraftNarratorMode
import net.blockhost.trestle.instance.MinecraftParticleSetting
import net.blockhost.trestle.instance.ServerStatus
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class LauncherDestination(val label: String) {
    LIBRARY("Library"),
    INSTANCE("Instance"),
    DISCOVER("Discover"),
    ACCOUNTS("Accounts"),
    SETTINGS("Settings"),
}

private val globalDestinations = listOf(
    LauncherDestination.LIBRARY,
    LauncherDestination.DISCOVER,
    LauncherDestination.ACCOUNTS,
    LauncherDestination.SETTINGS,
)

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
    const val DISCOVER = "discover"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
    const val CREATE_DIALOG = "create-dialog"
    const val INSTANCE_SETTINGS_DIALOG = "instance-settings-dialog"
    const val INSTANCE_ICON_EDIT = "instance-icon-edit"
    const val INSTANCE_ICON_DIALOG = "instance-icon-dialog"
    const val INSTANCE_ICON_UPLOAD = "instance-icon-upload"
    const val RESOURCE_BROWSER_DIALOG = "resource-browser-dialog"
    const val ACCOUNT_LOGIN_DIALOG = "account-login-dialog"
    const val SKIN_STUDIO_DIALOG = "skin-studio-dialog"
    const val SKIN_EDITOR_DIALOG = "skin-editor-dialog"

    fun instance(id: InstanceId): String = "instance-${id.value}"
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
    darkTheme: Boolean? = null,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    externalCommand: LauncherCommandRequest? = null,
    onExternalCommandHandled: (Long) -> Unit = {},
    onDestinationChanged: (LauncherDestination) -> Unit = {},
    topBar: @Composable () -> Unit = {},
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
        preference = state.themePreference,
        systemDarkTheme = darkTheme,
        highContrast = highContrast,
        reducedMotion = reducedMotion,
    ) {
        val modalVisible = state.create.visible ||
            state.instanceSettings.visible ||
            state.accountLogin.visible ||
            state.skinStudio.visible ||
            state.localFileImport.visible ||
            state.serverEditor.visible ||
            state.pendingInstanceRemovalId != null ||
            state.pendingWorldDeletionKey != null ||
            showShortcuts ||
            (
                state.resourceBrowser.visible &&
                    state.resourceBrowser.presentation == ResourceBrowserPresentation.DIALOG
            )
        LaunchedEffect(state.error, state.notice) {
            val message = state.error ?: state.notice ?: return@LaunchedEffect
            val actionLabel = when {
                state.error != null && state.errorRecovery != null -> "Retry"
                state.removedInstanceUndo != null -> "Undo"
                else -> null
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
                when (actionLabel) {
                    "Retry" -> actions.retryError()
                    "Undo" -> actions.undoInstanceRemoval()
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
            bottomBar = {
                state.operation?.let { operation ->
                    OperationBar(
                        status = operation,
                        onCancel = actions::cancelActiveOperation,
                        onClick = operation.instanceId?.let { instanceId ->
                            {
                                actions.selectInstance(instanceId)
                                changeDestination(LauncherDestination.INSTANCE)
                            }
                        },
                    )
                }
            },
        ) { contentPadding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding)) {
                val compact = maxWidth < 840.dp
                val destinationContent: @Composable (Modifier, Boolean) -> Unit = { modifier, isCompact ->
                    destinationStateHolder.SaveableStateProvider(destination.name) {
                        when (destination) {
                            LauncherDestination.LIBRARY -> LibraryPage(
                                state,
                                modifier,
                                actions,
                                compact = isCompact,
                                onManage = { changeDestination(LauncherDestination.INSTANCE) },
                                searchFocusRequest = librarySearchFocusRequest,
                                importRequest = libraryImportRequest,
                            )
                            LauncherDestination.INSTANCE -> InstanceWorkspace(
                                state,
                                modifier,
                                actions,
                                onBack = { changeDestination(LauncherDestination.LIBRARY) },
                                compact = isCompact,
                            )
                            LauncherDestination.DISCOVER -> ResourceCatalogPage(
                                state,
                                modifier,
                                actions,
                                searchFocusRequest = resourceSearchFocusRequest,
                            )
                            LauncherDestination.ACCOUNTS -> AccountsPage(state, modifier, actions)
                            LauncherDestination.SETTINGS -> SettingsPage(state, modifier, actions)
                        }
                    }
                }
                if (compact) {
                    CompactLayout(state, destination, changeDestination, destinationContent)
                } else {
                    WideLayout(state, destination, changeDestination, destinationContent)
                }
            }
            if (state.create.visible) CreateInstanceDialog(state, actions)
            if (
                state.resourceBrowser.visible &&
                state.resourceBrowser.presentation == ResourceBrowserPresentation.DIALOG
            ) {
                ResourceBrowserDialog(state, actions)
            }
            if (state.instanceSettings.visible) InstanceSettingsDialog(state, actions)
            if (state.accountLogin.visible) AccountLoginDialog(state, actions)
            if (state.skinStudio.visible && !state.skinStudio.editor.visible) SkinStudioDialog(state, actions)
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
                            TextButton(onClick = actions::cancelInstanceRemoval) { Text("Cancel") }
                            if (!moveToTrash) {
                                TextButton(onClick = actions::confirmInstanceRemoval) { Text("Keep files") }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = if (moveToTrash) {
                                actions::confirmInstanceRemoval
                            } else {
                                actions::confirmInstanceDeletion
                            },
                        ) {
                            Text(if (moveToTrash) "Move to Trash" else "Delete files")
                        }
                    },
                )
            }
            state.pendingWorldDeletionKey?.let { worldKey ->
                AlertDialog(
                    onDismissRequest = actions::cancelWorldDeletion,
                    title = { Text("Delete $worldKey?") },
                    text = {
                        Text("This permanently deletes the world directory. Existing ZIP backups remain available.")
                    },
                    dismissButton = {
                        TextButton(onClick = actions::cancelWorldDeletion) { Text("Cancel") }
                    },
                    confirmButton = {
                        Button(onClick = actions::confirmWorldDeletion) { Text("Delete world") }
                    },
                )
            }
        }
    }
}

@Composable
private fun WideLayout(
    state: LauncherUiState,
    destination: LauncherDestination,
    onDestinationChange: (LauncherDestination) -> Unit,
    destinationContent: @Composable (Modifier, Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        WideNavigationRail(state, destination, onDestinationChange)
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        destinationContent(Modifier.weight(1f), false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactLayout(
    state: LauncherUiState,
    destination: LauncherDestination,
    onDestinationChange: (LauncherDestination) -> Unit,
    destinationContent: @Composable (Modifier, Boolean) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BridgeMark()
                        Text("Trestle")
                    }
                },
                actions = {
                    state.accounts.firstOrNull { it.isActive }?.let { account ->
                        AccountIdentity(
                            account = account,
                            texture = state.accountSkinTextures[account.profile.profileId],
                            compact = true,
                            onClick = { onDestinationChange(LauncherDestination.ACCOUNTS) },
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                globalDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item || item == LauncherDestination.LIBRARY && destination == LauncherDestination.INSTANCE,
                        onClick = { onDestinationChange(item) },
                        icon = { Icon(painterResource(destinationIcon(item)), contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        destinationContent(Modifier.padding(padding), true)
    }
}

@Composable
private fun WideNavigationRail(
    state: LauncherUiState,
    destination: LauncherDestination,
    onDestinationChange: (LauncherDestination) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight().testTag(LauncherTestTags.TOP_NAVIGATION),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = {
            BridgeMark(Modifier.padding(vertical = 16.dp).size(width = 36.dp, height = 28.dp))
        },
    ) {
        globalDestinations.forEach { item ->
            NavigationRailItem(
                selected = destination == item || item == LauncherDestination.LIBRARY && destination == LauncherDestination.INSTANCE,
                onClick = { onDestinationChange(item) },
                icon = { Icon(painterResource(destinationIcon(item)), contentDescription = null) },
                label = { Text(item.label) },
                modifier = Modifier.testTag(LauncherTestTags.navigation(item)),
            )
        }
        Spacer(Modifier.weight(1f))
        state.accounts.firstOrNull { it.isActive }?.let { account ->
            AccountIdentity(
                account = account,
                texture = state.accountSkinTextures[account.profile.profileId],
                compact = true,
                modifier = Modifier.padding(bottom = 12.dp),
                onClick = { onDestinationChange(LauncherDestination.ACCOUNTS) },
            )
        }
    }
}

private fun destinationIcon(destination: LauncherDestination): DrawableResource = when (destination) {
    LauncherDestination.LIBRARY -> Res.drawable.ic_library
    LauncherDestination.INSTANCE -> Res.drawable.ic_library
    LauncherDestination.DISCOVER -> Res.drawable.ic_extension
    LauncherDestination.ACCOUNTS -> Res.drawable.ic_account
    LauncherDestination.SETTINGS -> Res.drawable.ic_settings
}

@Composable
private fun AccountIdentity(
    account: ManagedAccount,
    texture: ByteArray?,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (compact) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier) {
            MinecraftSkinHead(
                texture = texture.takeIf { account.profile.edition == MinecraftEdition.JAVA },
                contentDescription = account.profile.playerName,
                modifier = Modifier.size(32.dp),
            ) {
                Text(account.profile.playerName.take(1).uppercase(), style = MaterialTheme.typography.labelLarge)
            }
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(account.profile.playerName)
        }
    }
}

@Composable
private fun LibraryPage(
    state: LauncherUiState,
    modifier: Modifier,
    actions: LauncherUiActions,
    compact: Boolean = false,
    onManage: () -> Unit,
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
    Box(
        modifier.fillMaxSize().localFileDropTarget(
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
                    compact = true,
                    onNew = actions::openCreate,
                    onImport = { importPicker.launch() },
                    searchFocusRequester = searchFocusRequester,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                state.selectedInstance?.let { instance ->
                    CompactLaunchStrip(state, instance, actions, onManage)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                InstanceCollection(
                    instances = filteredInstances,
                    state = state,
                    actions = actions,
                    compact = true,
                    compactListState = compactListState,
                    gridState = gridState,
                    modifier = Modifier.weight(1f),
                )
            }
            else -> Row(Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    InstanceShelfToolbar(
                        query,
                        { query = it },
                        compact = false,
                        onNew = actions::openCreate,
                        onImport = { importPicker.launch() },
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
                        modifier = Modifier.weight(1f),
                    )
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SelectedInstancePanel(
                    state = state,
                    actions = actions,
                    onManage = onManage,
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                )
            }
        }
        if (dropActive) {
            DropOverlay("Drop to import local content")
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
    modifier: Modifier,
) {
    if (instances.isEmpty()) {
        Column(
            modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No matching instances", style = MaterialTheme.typography.titleLarge)
            Text("Try another name, version, or loader.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else if (compact) {
        LazyColumn(
            state = compactListState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(sortInstances(instances, state), key = { it.id.value }) { instance ->
                InstanceTile(instance, instance.id == state.selectedInstance?.id, state, actions, compact = true)
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
    compact: Boolean,
    onNew: () -> Unit,
    onImport: () -> Unit,
    searchFocusRequester: FocusRequester,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (compact || maxWidth < 620.dp) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Instances", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onImport) { Text("Import") }
                    Button(
                        onClick = onNew,
                        modifier = Modifier.testTag(LauncherTestTags.NEW_INSTANCE),
                    ) {
                        Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New")
                    }
                }
                TrestleSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search instances…") },
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester)
                        .testTag(LauncherTestTags.INSTANCE_SEARCH),
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Instances", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TrestleSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search instances…") },
                    modifier = Modifier.width(320.dp).focusRequester(searchFocusRequester)
                        .testTag(LauncherTestTags.INSTANCE_SEARCH),
                )
                OutlinedButton(onClick = onImport) { Text("Import") }
                Button(
                    onClick = onNew,
                    modifier = Modifier.testTag(LauncherTestTags.NEW_INSTANCE),
                ) {
                    Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New instance")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrestleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searching: Boolean = false,
    onSearch: (String) -> Unit = {},
    placeholder: @Composable () -> Unit,
) {
    SearchBarDefaults.InputField(
        query = value,
        onQueryChange = onValueChange,
        onSearch = onSearch,
        expanded = false,
        onExpandedChange = {},
        enabled = enabled,
        placeholder = placeholder,
        leadingIcon = {
            Icon(painterResource(Res.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = {
            when {
                searching -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                value.isNotEmpty() -> IconButton(onClick = { onValueChange("") }) {
                    Icon(painterResource(Res.drawable.ic_close), contentDescription = "Clear search")
                }
            }
        },
        modifier = modifier,
    )
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
            onRetry?.let { TextButton(onClick = it) { Text("Retry") } }
        }
    }
}

@Composable
private fun OperationBar(
    status: OperationStatus,
    onCancel: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .windowInsetsPadding(WindowInsets.navigationBars),
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
private fun CompactLaunchStrip(
    launcherState: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    onManage: () -> Unit,
) {
    val activeAccount = launcherState.accounts.firstOrNull { it.isActive }
    val installationState = instance.installationState
    val progress = installationState.installationProgress()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InstanceArtwork(instance, 52.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        instance.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "${instance.minecraftVersionId} · ${instance.modLoader.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    stateLabel(installationState),
                    color = stateColor(installationState),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            InstallationProgress(installationState, progress)
            LaunchContext(state = installationState, activeAccount = activeAccount)
            LaunchReadiness(launcherState, instance)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { actions.openResourceBrowser() },
                    enabled = installationState is InstallationState.Installed,
                    modifier = Modifier.weight(1f),
                ) { Text("Content") }
                OutlinedButton(
                    onClick = onManage,
                    modifier = Modifier.weight(1f),
                ) { Text("Manage") }
            }
            PrimaryInstanceButton(instance, launcherState, actions, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SelectedInstancePanel(
    state: LauncherUiState,
    actions: LauncherUiActions,
    onManage: () -> Unit,
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
                Text("Select an instance", style = MaterialTheme.typography.titleLarge)
                Text("Choose one from the library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                onClick = { actions.openResourceBrowser() },
                enabled = installationState is InstallationState.Installed,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Manage content") }
            OutlinedButton(
                onClick = onManage,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Instance settings") }
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
        ) { Text("Pause") }
        is InstallationState.Interrupted -> Button(
            onClick = actions::installSelected,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text("Resume install") }
        is InstallationState.Failed -> Button(
            onClick = actions::installSelected,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text("Retry install") }
        InstallationState.NotInstalled -> Button(
            onClick = actions::installSelected,
            modifier = modifier.testTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION),
        ) { Text("Install") }
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

private fun Modifier.primarySelectable(
    selected: Boolean,
    onClickLabel: String,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
): Modifier = combinedClickable(
    onClickLabel = onClickLabel,
    role = Role.RadioButton,
    onClick = onClick,
    onDoubleClick = onDoubleClick,
).semantics {
    this.selected = selected
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
        columns = GridCells.Adaptive(136.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
) {
    val focusManager = LocalFocusManager.current
    val installationState = instance.installationState
    val running = launcherState.activeLaunch?.instanceId == instance.id &&
        launcherState.activeLaunch.status is LaunchStatus.Running
    val progress = installationState.installationProgress()
    val versionLabel = "${instance.minecraftVersionId} · ${instance.modLoader.label}" +
        if (instance.pinned) " · Pinned" else ""
    ContextActionArea(instanceContextActions(instance, actions)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .primarySelectable(
                    selected = selected,
                    onClickLabel = "Select instance",
                    onClick = { actions.selectInstance(instance.id) },
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
                            actions.launchSelected()
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
                .testTag(LauncherTestTags.instance(instance.id)),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InstanceArtwork(instance, 48.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(instance.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            versionLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (running) "Running" else stateLabel(installationState),
                        color = if (running) MaterialTheme.colorScheme.primary else stateColor(installationState),
                    )
                }
            } else {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InstanceArtwork(instance, 58.dp)
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
        ) { Text("Stop") }
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
        ) { Text("Launch") }
        is LaunchStatus.Unavailable -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
        ) { Text("Unavailable") }
        is LaunchStatus.Failed -> Button(
            onClick = actions::launchSelected,
            modifier = modifier,
        ) { Text("Retry launch") }
        LaunchStatus.NotChecked,
        LaunchStatus.Ready,
        -> Button(
            onClick = actions::launchSelected,
            modifier = modifier,
        ) { Text("Launch") }
    }
}

@Composable
private fun InstanceSettingsDialog(state: LauncherUiState, actions: LauncherUiActions) {
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
    BasicAlertDialog(
        onDismissRequest = { if (!form.isSaving) actions.closeInstanceSettings() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 820.dp)
                .dismissOnEscape(enabled = !form.isSaving, onDismiss = actions::closeInstanceSettings)
                .testTag(LauncherTestTags.INSTANCE_SETTINGS_DIALOG),
        ) {
            Column {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Instance settings", style = MaterialTheme.typography.headlineMedium)
                    Text("Identity", style = MaterialTheme.typography.titleMedium)
                    TextField(
                        value = form.name,
                        onValueChange = actions::setInstanceName,
                        label = { Text("Name") },
                        isError = form.name.isBlank(),
                        supportingText = if (form.name.isBlank()) ({ Text("Enter an instance name.") }) else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.group,
                        onValueChange = actions::setInstanceGroup,
                        label = { Text("Group") },
                        supportingText = { Text("Leave blank to group this instance by loader.") },
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
                    Text("Game components", style = MaterialTheme.typography.titleMedium)
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
                    Text("Launch", style = MaterialTheme.typography.titleMedium)
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
                        TextButton(onClick = actions::applyRecommendedMemory) { Text("Use recommended") }
                    }
                    form.warnings.forEach { warning -> Text(warning, color = MaterialTheme.colorScheme.error) }
                    TextField(
                        value = form.jvmArguments,
                        onValueChange = actions::setJvmArguments,
                        label = { Text("Additional JVM arguments") },
                        supportingText = {
                            Text("Memory, classpath, native path, and architecture options are managed by Trestle.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.gameArguments,
                        onValueChange = actions::setGameArguments,
                        label = { Text("Additional game arguments") },
                        supportingText = { Text("Quoted values and escaped characters are preserved.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.supportsCustomJava) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val javaField: @Composable (Modifier) -> Unit = { fieldModifier ->
                                TextField(
                                    value = form.javaExecutable,
                                    onValueChange = actions::setJavaExecutable,
                                    label = { Text("Custom Java executable") },
                                    supportingText = { Text("Leave blank to use Trestle's managed Mojang runtime.") },
                                    singleLine = true,
                                    modifier = fieldModifier,
                                )
                            }
                            if (maxWidth < 440.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    javaField(Modifier.fillMaxWidth())
                                    OutlinedButton(onClick = { javaPicker.launch() }) { Text("Browse") }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                    javaField(Modifier.weight(1f))
                                    OutlinedButton(onClick = { javaPicker.launch() }) { Text("Browse") }
                                }
                            }
                        }
                    }
                    TextField(
                        value = form.environmentVariables,
                        onValueChange = actions::setEnvironmentVariables,
                        label = { Text("Environment variables") },
                        supportingText = { Text("Enter one NAME=value pair per line. Lines starting with # are ignored.") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.supportsLaunchCommands) {
                        HorizontalDivider()
                        Text("Custom commands", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Commands run directly in the game directory. Enter an executable followed by its arguments; shell operators are not expanded.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextField(
                            value = form.preLaunchCommand,
                            onValueChange = actions::setPreLaunchCommand,
                            label = { Text("Pre-launch command") },
                            supportingText = { Text("Must exit successfully before Minecraft starts.") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextField(
                            value = form.wrapperCommand,
                            onValueChange = actions::setWrapperCommand,
                            label = { Text("Wrapper command") },
                            supportingText = { Text("Runs before the Java executable, for example gamescope --.") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextField(
                            value = form.postExitCommand,
                            onValueChange = actions::setPostExitCommand,
                            label = { Text("Post-exit command") },
                            supportingText = { Text("Runs after Minecraft exits.") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    HorizontalDivider()
                    Text("Minecraft client", style = MaterialTheme.typography.titleMedium)
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
                            Text("Loading client settings…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        form.clientSettingsError != null -> Text(form.clientSettingsError, color = MaterialTheme.colorScheme.error)
                        form.clientSettings != null -> ClientSettingsFields(
                            form.clientSettings,
                            actions::setInstanceClientSettings,
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = actions::closeInstanceSettings, enabled = !form.isSaving) { Text("Cancel") }
                    Button(
                        onClick = actions::saveInstanceSettings,
                        enabled = valid && !form.isLoadingClientSettings && !form.isSaving,
                    ) {
                        if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Save changes")
                    }
                }
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
    BasicAlertDialog(
        onDismissRequest = actions::closeResourceBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f).widthIn(max = 1040.dp)
                .dismissOnEscape(enabled = !browser.isInstalling, onDismiss = actions::closeResourceBrowser)
                .testTag(LauncherTestTags.RESOURCE_BROWSER_DIALOG),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Browse content", style = MaterialTheme.typography.headlineMedium)
                        val instance = state.selectedInstance
                        Text(
                            if (browser.type == ResourceType.MODPACK) {
                                "Modpacks create a new instance."
                            } else if (instance == null) {
                                "Select an installed instance to add content."
                            } else {
                                "Compatible with ${instance.minecraftVersionId} · ${instance.modLoader.label}"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = actions::closeResourceBrowser, enabled = !browser.isInstalling) { Text("Close") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ResourceBrowserContent(state, actions, Modifier.fillMaxSize(), searchFocusRequest = 0)
            }
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
) {
    val browser = state.resourceBrowser
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>()
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
        ResourceBrowserToolbar(browser, actions, searchFocusRequester)
        browser.error?.let { InlineMessage(it, true, null) }
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.fillMaxSize(),
            listPane = {
                AnimatedPane {
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
                AnimatedPane {
                    Column(Modifier.fillMaxSize()) {
                        if (listPaneHidden) {
                            TextButton(
                                onClick = clearSelection,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
                                Text("Back to results")
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
    actions: LauncherUiActions,
    searchFocusRequester: FocusRequester,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrestleSearchField(
            value = browser.query,
            onValueChange = actions::setResourceQuery,
            searching = browser.isSearching,
            onSearch = { actions.searchResources() },
            placeholder = { Text("Search mods, packs, and shaders") },
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                .focusRequester(searchFocusRequester)
                .testTag(LauncherTestTags.RESOURCE_SEARCH),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceProviderButtons(browser, actions)
                    TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Hide filters" else "Filter options") }
                    Selector(
                        label = "Content type",
                        value = browser.type.label,
                        values = browsableResourceTypes.map { it.label },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { label -> actions.setResourceType(browsableResourceTypes.first { it.label == label }) },
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceProviderButtons(browser, actions)
                    TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Hide filters" else "Filter options") }
                    Spacer(Modifier.weight(1f))
                    Selector(
                        label = "Content type",
                        value = browser.type.label,
                        values = browsableResourceTypes.map { it.label },
                        modifier = Modifier.width(190.dp),
                        onSelect = { label -> actions.setResourceType(browsableResourceTypes.first { it.label == label }) },
                    )
                }
            }
        }
        if (showFilters) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val filterFields: @Composable ColumnScope.() -> Unit = {
                    TextField(
                        value = browser.gameVersionFilter,
                        onValueChange = actions::setResourceGameVersionFilter,
                        label = { Text("Minecraft version") },
                        placeholder = { Text("Any version") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Selector(
                        label = "Mod loader",
                        value = browser.loaderFilter?.label ?: "Any loader",
                        values = listOf("Any loader") + ModLoader.entries.filterNot { it == ModLoader.VANILLA }.map { it.label },
                        onSelect = { label ->
                            actions.setResourceLoaderFilter(ModLoader.entries.firstOrNull { it.label == label })
                        },
                    )
                    TextField(
                        value = browser.categoryFilter,
                        onValueChange = actions::setResourceCategoryFilter,
                        label = { Text("Category") },
                        placeholder = { Text("Any category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Selector(
                        label = "Sort by",
                        value = browser.sort.label,
                        values = ResourceSearchSort.entries.map { it.label },
                        onSelect = { label ->
                            ResourceSearchSort.entries.firstOrNull { it.label == label }?.let(actions::setResourceSort)
                        },
                    )
                    Button(onClick = { actions.searchResources() }, modifier = Modifier.fillMaxWidth()) { Text("Apply filters") }
                }
                if (maxWidth < 720.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = filterFields)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = browser.gameVersionFilter,
                            onValueChange = actions::setResourceGameVersionFilter,
                            label = { Text("Minecraft version") },
                            placeholder = { Text("Any version") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Selector(
                            label = "Mod loader",
                            value = browser.loaderFilter?.label ?: "Any loader",
                            values = listOf("Any loader") + ModLoader.entries.filterNot { it == ModLoader.VANILLA }.map { it.label },
                            modifier = Modifier.weight(1f),
                            onSelect = { label ->
                                actions.setResourceLoaderFilter(ModLoader.entries.firstOrNull { it.label == label })
                            },
                        )
                        TextField(
                            value = browser.categoryFilter,
                            onValueChange = actions::setResourceCategoryFilter,
                            label = { Text("Category") },
                            placeholder = { Text("Any category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Selector(
                            label = "Sort by",
                            value = browser.sort.label,
                            values = ResourceSearchSort.entries.map { it.label },
                            modifier = Modifier.weight(1f),
                            onSelect = { label ->
                                ResourceSearchSort.entries.firstOrNull { it.label == label }?.let(actions::setResourceSort)
                            },
                        )
                        Button(onClick = actions::searchResources) { Text("Apply") }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Versions", style = MaterialTheme.typography.labelLarge)
                ReleaseChannel.entries.filterNot { it == ReleaseChannel.UNKNOWN }.forEach { channel ->
                    CompactCheck(channel.label, channel in browser.releaseChannels) {
                        actions.toggleResourceReleaseChannel(channel)
                    }
                }
            }
        }
        if (!browser.curseForgeAvailable) {
            Text("CurseForge requires a Trestle API key configured by the application build.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (browser.type == ResourceType.MODPACK) {
            Text(
                "ATLauncher does not permit third-party launcher access to its CDN. Direct pack archives can still be imported.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResourceProviderButtons(browser: ResourceBrowserState, actions: LauncherUiActions) {
    SingleChoiceSegmentedButtonRow {
        ResourceProvider.entries.forEachIndexed { index, provider ->
            val available = provider != ResourceProvider.ATLAUNCHER &&
                (provider != ResourceProvider.CURSEFORGE || browser.curseForgeAvailable) &&
                (browser.type == ResourceType.MODPACK || provider in setOf(ResourceProvider.MODRINTH, ResourceProvider.CURSEFORGE))
            SegmentedButton(
                selected = browser.provider == provider,
                onClick = { actions.setResourceProvider(provider) },
                enabled = available,
                shape = SegmentedButtonDefaults.itemShape(index, ResourceProvider.entries.size),
            ) { Text(provider.label) }
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
            Text("No results", style = MaterialTheme.typography.titleLarge)
            Text("Change the search or content type.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ResourceProjectLogo(
                project = project,
                modifier = Modifier.size(64.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        project.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatDownloads(project.downloads),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    "${project.provider.label} · ${project.author.ifBlank { "Unknown author" }}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    project.summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.categories.isNotEmpty()) {
                    Text(
                        project.categories.take(3).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
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
    Column(
        modifier.fillMaxHeight().verticalScroll(scrollState).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (project == null) {
            Text("Select a result", style = MaterialTheme.typography.titleLarge)
            Text("Available versions and installation details will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
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
            Text(
                readableProjectDescription(description),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        ResourceProjectDetails(project)
        project.websiteUrl?.takeIf(String::isNotBlank)?.let { websiteUrl ->
            TextButton(onClick = { uriHandler.openUri(websiteUrl) }) { Text("View on ${project.provider.label}") }
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
        val version = browser.selectedVersion
        version?.let {
            ResourceVersionDetails(it)
            val optionalDependencies = it.dependencies.filter { dependency -> dependency.kind == DependencyKind.OPTIONAL }
            if (optionalDependencies.isNotEmpty()) {
                Text("Optional dependencies", style = MaterialTheme.typography.titleMedium)
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
        val supportedType = project.type in installableResourceTypes
        val instanceReady = project.type == ResourceType.MODPACK || instance?.installationState is InstallationState.Installed
        val selectedFile = version?.primaryFile
        val downloadable = version?.externalPack != null || selectedFile?.url != null || selectedFile?.sha1 != null
        if (!supportedType) Text("This content type cannot be installed into an instance yet.", color = MaterialTheme.colorScheme.error)
        if (selectedFile?.url == null && selectedFile?.sha1 != null) {
            Text("CurseForge blocks this file. Trestle will look for the identical file on Modrinth.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (version != null && version.externalPack == null && !downloadable) {
            Text("The author blocks downloads from third-party launchers.", color = MaterialTheme.colorScheme.error)
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
            ) { Text("Open manual download") }
        }
        Button(
            onClick = actions::installSelectedResource,
            enabled = supportedType && instanceReady && downloadable && !browser.isInstalling,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (browser.isInstalling) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (project.type == ResourceType.MODPACK) "Create instance" else "Install")
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
}

private fun readableProjectDescription(value: String): String = value
    .replace(Regex("(?i)<br\\s*/?>"), "\n")
    .replace(Regex("(?i)</p>|</div>|</h[1-6]>|</li>"), "\n")
    .replace(Regex("<[^>]+>"), "")
    .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
    .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1 ($2)")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

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
    BasicAlertDialog(onDismissRequest = actions::cancelLocalFileImport) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().onPreviewKeyEvent { event ->
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
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Import local file", style = MaterialTheme.typography.headlineMedium)
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
                    Text("Choose how Trestle should use this ZIP file.")
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = actions::cancelLocalFileImport) { Text("Cancel") }
                    Button(
                        onClick = actions::confirmLocalFileImport,
                        enabled = selectedType != null &&
                            (selectedType == ResourceType.MODPACK || target?.installationState is InstallationState.Installed),
                    ) { Text(if (selectedType == ResourceType.MODPACK) "Import modpack" else "Add file") }
                }
            }
        }
    }
}

@Composable
private fun ShortcutsDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Keyboard shortcuts", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
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
    BasicAlertDialog(
        onDismissRequest = { if (!form.isSaving) actions.closeCreate() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 820.dp)
                .dismissOnEscape(enabled = !form.isSaving, onDismiss = actions::closeCreate)
                .testTag(LauncherTestTags.CREATE_DIALOG),
        ) {
            Column {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("New instance", style = MaterialTheme.typography.headlineMedium)
                    if (!restrictedRuntime) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = source == "custom",
                            onClick = { source = "custom" },
                            shape = SegmentedButtonDefaults.itemShape(0, 3),
                            modifier = Modifier.weight(1f),
                        ) { Text("Custom") }
                        SegmentedButton(
                            selected = source == "import",
                            onClick = { source = "import" },
                            shape = SegmentedButtonDefaults.itemShape(1, 3),
                            modifier = Modifier.weight(1f),
                        ) { Text("Import") }
                        SegmentedButton(
                            selected = false,
                            onClick = {
                                actions.closeCreate()
                                actions.openResourceBrowser(ResourceType.MODPACK)
                            },
                            shape = SegmentedButtonDefaults.itemShape(2, 3),
                            modifier = Modifier.weight(1f),
                        ) { Text("Browse modpacks") }
                    }
                    TextField(
                        value = form.name,
                        onValueChange = actions::setCreateName,
                        label = { Text("Name") },
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
                            label = { Text("Group") },
                            supportingText = { Text("Optional") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextField(
                            value = form.iconReference,
                            onValueChange = actions::setCreateIconReference,
                            label = { Text("Icon path or URL") },
                            supportingText = { Text("Optional") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (source == "import") {
                        TextField(
                            value = remoteUrl,
                            onValueChange = { remoteUrl = it },
                            label = { Text("Direct download or CurseForge URL") },
                            placeholder = { Text("https://… or curseforge://…") },
                            supportingText = {
                                Text("Supports Modrinth, CurseForge, Prism, and MultiMC pack archives.")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        HorizontalDivider()
                        Text("Existing FTB App library", style = MaterialTheme.typography.titleMedium)
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
                        ) { Text("Import FTB App instances") }
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
                            label = { Text("Minecraft version") },
                            supportingText = { Text("Android runtime") },
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
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompactCheck("Snapshots", showSnapshots) { showSnapshots = it }
                        CompactCheck("Betas", showBetas) { showBetas = it }
                        CompactCheck("Alphas", showAlphas) { showAlphas = it }
                    }
                    if (loaderChoices.size == 1) {
                        OutlinedTextField(
                            value = loaderChoices.single().label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loader") },
                            supportingText = { Text("Only vanilla is supported on Android") },
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
                            Text("Client defaults", style = MaterialTheme.typography.titleMedium)
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
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = actions::closeCreate, enabled = !form.isSaving) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (source == "import") actions.importRemoteModpack(remoteUrl) else actions.createInstance()
                        },
                        enabled = if (source == "import") {
                            remoteUrl.isNotBlank()
                        } else {
                            form.name.isNotBlank() && form.versionId.isNotBlank() &&
                                (form.modLoader == ModLoader.VANILLA || form.loaderVersion != null) && !form.isSaving
                        },
                    ) {
                        if (form.isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Creating…")
                        } else {
                            Text(if (source == "import") "Import modpack" else "Create instance")
                        }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Client defaults", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Write these settings before the first launch. Settings unavailable in older versions are skipped.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = form.preconfigureClientSettings,
                    onCheckedChange = actions::setCreateClientPreconfiguration,
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Apply these defaults", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(
                    checked = form.preconfigureClientSettings,
                    onCheckedChange = actions::setCreateClientPreconfiguration,
                )
            }
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
        Text("Audio", style = MaterialTheme.typography.titleSmall)
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
        Text("Video", style = MaterialTheme.typography.titleSmall)
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
        Text("Controls and accessibility", style = MaterialTheme.typography.titleSmall)
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
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

@Composable
private fun CompactCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label)
    }
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
                    label = { Text("Filter $label") },
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
        Text("No instances yet", style = MaterialTheme.typography.titleLarge)
        Text("Create an isolated Minecraft instance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew) { Text("Create instance") }
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

private enum class InstanceSection(val label: String) {
    OVERVIEW("Overview"),
    CONTENT("Content"),
    GAME_DATA("Game data"),
    NOTES("Notes"),
    LOGS("Logs"),
    SETTINGS("Settings"),
}

@Composable
private fun InstanceWorkspace(
    state: LauncherUiState,
    modifier: Modifier,
    actions: LauncherUiActions,
    onBack: () -> Unit,
    compact: Boolean = false,
) {
    val instance = state.selectedInstance
    var sectionName by rememberSaveable(instance?.id) { mutableStateOf(InstanceSection.OVERVIEW.name) }
    val section = InstanceSection.entries.firstOrNull { it.name == sectionName } ?: InstanceSection.OVERVIEW
    val overviewListState = rememberLazyListState()
    val contentListState = rememberLazyListState()
    val gameDataListState = rememberLazyListState()
    val configurationScrollState = rememberScrollState()
    Column(modifier.fillMaxSize().testTag(LauncherTestTags.INSTANCE_WORKSPACE)) {
        if (compact || instance == null) {
            PageHeader(instance?.displayName ?: "Instance") {
                TextButton(onClick = onBack) { Text("Back to library") }
            }
        } else {
            InstanceWorkspaceHeader(state, instance, actions, onBack)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (instance == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No instance selected", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) { Text("Return to library") }
            }
            return@Column
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SecondaryScrollableTabRow(
                selectedTabIndex = section.ordinal,
                modifier = Modifier.widthIn(max = WideContentWidth).fillMaxWidth(),
                edgePadding = 0.dp,
            ) {
                InstanceSection.entries.forEach { item ->
                    Tab(
                        selected = section == item,
                        onClick = { sectionName = item.name },
                        modifier = Modifier.testTag(LauncherTestTags.instanceSection(item.name)),
                        text = { Text(item.label) },
                    )
                }
            }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val contentModifier = Modifier.widthIn(max = WideContentWidth).fillMaxSize()
            when (section) {
                InstanceSection.OVERVIEW -> InstanceOverview(
                    state,
                    instance,
                    actions,
                    overviewListState,
                    contentModifier,
                    compact,
                )
                InstanceSection.CONTENT -> InstanceContent(state, instance, actions, contentListState, contentModifier)
                InstanceSection.GAME_DATA -> InstanceGameData(
                    state,
                    instance,
                    actions,
                    gameDataListState,
                    contentModifier,
                )
                InstanceSection.NOTES -> InstanceNotes(instance, actions, contentModifier)
                InstanceSection.LOGS -> InstanceLogs(state, instance, actions, contentModifier)
                InstanceSection.SETTINGS -> InstanceConfiguration(
                    instance,
                    actions,
                    configurationScrollState,
                    contentModifier,
                )
            }
        }
    }
}

@Composable
private fun InstanceGameData(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
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
                    Text("Game data", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Manage worlds, data packs, server bookmarks, screenshots, and backups for ${instance.displayName}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = actions::refreshGameData, enabled = !state.isLoadingGameData) {
                    Text("Refresh")
                }
            }
        }
        if (state.isLoadingGameData) {
            item("game-data-loading") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Reading the game directory…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item("worlds") {
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
                                TextButton(onClick = { actions.launchWorld(world.key) }) { Text("Play") }
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
                            Text("Seed $seed", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            TextButton(onClick = { copyText(seed.toString()) }) { Text("Copy seed") }
                        }
                    }
                    world.dataPacks.forEach { pack ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(pack.fileName, style = MaterialTheme.typography.bodyMedium)
                                Text(formatFileSize(pack.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = pack.enabled,
                                onCheckedChange = { actions.toggleDataPack(world.key, pack.key) },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        item("servers") {
            GameDataSection("Servers", if (state.gameData.servers.isEmpty()) "No saved multiplayer servers." else null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { actions.openServerEditor() }) { Text("Add server") }
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
                            Text("${server.address} · $status")
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { actions.joinServer(server.key) }) { Text("Join") }
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
        item("backups") {
            GameDataSection("Backups", if (state.gameData.backups.isEmpty()) "No world backups yet." else null) {
                state.gameData.backups.forEach { backup ->
                    ListItem(
                        headlineContent = { Text(backup.fileName) },
                        supportingContent = { Text(formatFileSize(backup.sizeBytes)) },
                        trailingContent = {
                            TextButton(onClick = { actions.restoreWorldBackup(backup.key) }) { Text("Restore copy") }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
        item("screenshots") {
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
                                ) { Text("Open") }
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
            title = { Text("Rename world") },
            text = {
                OutlinedTextField(
                    value = worldName,
                    onValueChange = { worldName = it },
                    label = { Text("World name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = { TextButton(onClick = { worldToRename = null }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = {
                        actions.renameWorld(worldKey, worldName)
                        worldToRename = null
                    },
                    enabled = worldName.isNotBlank(),
                ) { Text("Rename") }
            },
        )
    }
    screenshotToRename?.let { screenshotKey ->
        AlertDialog(
            onDismissRequest = { screenshotToRename = null },
            title = { Text("Rename screenshot") },
            text = {
                OutlinedTextField(
                    value = screenshotName,
                    onValueChange = { screenshotName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { screenshotToRename = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.renameScreenshot(screenshotKey, screenshotName)
                        screenshotToRename = null
                    },
                    enabled = screenshotName.isNotBlank(),
                ) { Text("Rename") }
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
    TextButton(onClick = { picker.launch() }) { Text("Import world") }
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
                    Text("Notes", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Keep setup details, server information, or reminders with this instance.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { actions.saveInstanceNotes(draft) },
                    enabled = draft.trimEnd() != instance.notes,
                ) { Text("Save notes") }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Write notes for ${instance.displayName}") },
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
                    Text("Instance logs", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Review Minecraft output, archived logs, and crash reports.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { selectedKey?.let(actions::selectInstanceLog) },
                    enabled = selectedKey != null && !state.isLoadingInstanceLog,
                ) { Text("Reload") }
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
                    label = { Text("Find in log") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        Modifier.toggleable(followLaunch, role = Role.Checkbox) { followLaunch = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(followLaunch, onCheckedChange = null)
                        Text("Follow launch")
                    }
                    Row(
                        Modifier.toggleable(wrapLines, role = Role.Checkbox) { wrapLines = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(wrapLines, onCheckedChange = null)
                        Text("Wrap lines")
                    }
                }
                Row(
                    Modifier.toggleable(colorLines, role = Role.Checkbox) { colorLines = it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(colorLines, onCheckedChange = null)
                    Text("Color warnings and errors")
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
        TextButton(onClick = { expanded = true }) { Text("More") }
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
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = editor.address,
                    onValueChange = actions::setServerAddress,
                    label = { Text("Address") },
                    supportingText = { Text("For example, play.example.net:25565") },
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
            TextButton(onClick = actions::closeServerEditor, enabled = !editor.isSaving) { Text("Cancel") }
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
                Text("Library")
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
                            Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Instance", style = MaterialTheme.typography.titleLarge)
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
                    Text("Version components", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = actions::openInstanceSettings) { Text("Change") }
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
                        Text("Launch plan", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = actions::inspectLaunchPlan) { Text("Refresh") }
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
                ) { Text("Inspect launch plan") }
            }
        }
        if (state.gameLogLines.isNotEmpty() || state.lastCrashReport != null) {
            item("game-console") {
                Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(top = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Game console", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = actions::clearGameLog, enabled = state.gameLogLines.isNotEmpty()) {
                            Text("Clear")
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
                            Text("Crash report: $report", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { openPath(report) },
                                enabled = currentPlatform == "Desktop",
                            ) { Text("Open") }
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
                        onClick = { actions.openResourceBrowser(type) },
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
                Text("Installed content", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Manage Trestle installs and compatible files already in ${instance.displayName}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = actions::refreshInstalledContent,
                enabled = !state.isLoadingInstalledContent,
            ) { Text("Refresh") }
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
                Text("Reading installed content…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    onCheckedChange = { actions.toggleInstalledContent(content.key) },
                    enabled = content.canManage,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        if (content.canManage) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                content.websiteUrl?.let { websiteUrl ->
                    TextButton(onClick = { uriHandler.openUri(websiteUrl) }) { Text("Homepage") }
                }
                if (update != null) {
                    FilledTonalButton(onClick = { actions.updateInstalledContent(content.key) }) {
                        Text("Update")
                    }
                }
                TextButton(onClick = { actions.removeInstalledContent(content.key) }) { Text("Remove") }
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
            Text("Instance settings", style = MaterialTheme.typography.titleLarge)
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
            ) { Text("Edit instance settings") }
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
        PageHeader("Discover") {}
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
                OutlinedButton(onClick = onClick, enabled = enabled) { Text("Browse") }
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
    TextButton(onClick = { picker.launch() }, enabled = enabled) { Text("Add file") }
}

private const val MAX_LOCAL_IMPORT_BYTES = 512L * 1024L * 1024L

@Composable
private fun AccountsPage(state: LauncherUiState, modifier: Modifier, actions: LauncherUiActions) {
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().testTag(LauncherTestTags.ACCOUNTS)) {
        PageHeader("Accounts") {
            Button(onClick = actions::openAccountLogin) { Text("Add account") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (state.accounts.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No accounts", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Add a verified Java or Bedrock account, import an existing session, or create an offline profile. " +
                        "Trestle encrypts saved online authentication state in the platform credential vault.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 620.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                item("accounts-summary") {
                    Text(
                        "Choose the identity Trestle uses to launch. Online credential state stays in the platform vault.",
                        modifier = Modifier.widthIn(max = 720.dp).padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.accounts, key = { it.profile.profileId }) { account ->
                    AccountRow(
                        account = account,
                        texture = state.accountSkinTextures[account.profile.profileId],
                        actions = actions,
                    ) { pendingRemoval = account.profile.profileId }
                }
            }
        }
    }
    pendingRemoval?.let { profileId ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Forget account?") },
            text = {
                Text("This removes the local profile and saved credentials. It does not change the source account.")
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.removeAccount(profileId)
                        pendingRemoval = null
                    },
                ) { Text("Forget account") }
            },
        )
    }
}

@Composable
private fun AccountRow(
    account: ManagedAccount,
    texture: ByteArray?,
    actions: LauncherUiActions,
    onForget: () -> Unit,
) {
    val copyText = rememberCopyText()
    val profileId = account.profile.profileId
    val canManageOfficialProfile =
        account.isActive &&
            account.isAuthenticated &&
            account.profile.edition == MinecraftEdition.JAVA &&
            account.profile.authenticationMethod != AccountAuthenticationMethod.THE_ALTENING
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
        if (canManageOfficialProfile) {
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 700.dp
            Card(
                modifier = Modifier.fillMaxWidth().then(
                    if (account.isActive) {
                        Modifier
                    } else {
                        Modifier.pointerInput(profileId) {
                            detectTapGestures(onDoubleTap = { actions.selectAccount(profileId) })
                        }
                    },
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (account.isActive) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ),
            ) {
                if (compact) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccountMark(account, texture, compact = true)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(account.profile.playerName, style = MaterialTheme.typography.titleMedium)
                                Text(account.profile.authenticationMethod.label, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (account.isActive) Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                statusText,
                                modifier = Modifier.weight(1f),
                                color = if (account.isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            if (!account.isActive) {
                                OutlinedButton(
                                    onClick = { actions.selectAccount(profileId) },
                                ) { Text("Use") }
                            }
                            if (canManageOfficialProfile) {
                                TextButton(onClick = actions::openSkinStudio) { Text("Skins") }
                            }
                            if (account.isAuthenticated) {
                                TextButton(onClick = { actions.signOutAccount(profileId) }) { Text("Sign out") }
                            }
                            TextButton(onClick = onForget) { Text("Forget") }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AccountMark(account, texture)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(account.profile.playerName, style = MaterialTheme.typography.titleMedium)
                                if (account.isActive) Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(account.profile.authenticationMethod.label, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Column(Modifier.widthIn(min = 150.dp), horizontalAlignment = Alignment.End) {
                            Text(
                                statusText,
                                color = if (account.isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            account.profile.skin?.let { skin ->
                                Text(
                                    "${skin.variant.name.lowercase().replaceFirstChar(Char::uppercase)} model",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        if (!account.isActive) {
                            OutlinedButton(onClick = { actions.selectAccount(profileId) }) {
                                Text("Use")
                            }
                        } else if (canManageOfficialProfile) {
                            OutlinedButton(onClick = actions::openSkinStudio) {
                                Text("Manage skins")
                            }
                        }
                        if (account.isAuthenticated) {
                            TextButton(onClick = { actions.signOutAccount(profileId) }) { Text("Sign out") }
                        }
                        TextButton(onClick = onForget) { Text("Forget") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinStudioDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val account = state.accounts.firstOrNull { it.isActive }
    val selected = state.savedSkins.firstOrNull { it.profile.id == state.skinStudio.selectedProfileId }
    var confirmDelete by remember { mutableStateOf(false) }
    BasicAlertDialog(
        onDismissRequest = actions::closeSkinStudio,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.9f).widthIn(max = 1040.dp).heightIn(min = 540.dp)
                .dismissOnEscape(onDismiss = actions::closeSkinStudio)
                .testTag(LauncherTestTags.SKIN_STUDIO_DIALOG),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Skins", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = actions::closeSkinStudio) { Text("Close") }
                }
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
        }
    }
    if (confirmDelete && selected != null) {
        BasicAlertDialog(onDismissRequest = { confirmDelete = false }) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.widthIn(max = 420.dp),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Remove ${selected.profile.name}?", style = MaterialTheme.typography.headlineMedium)
                    Text("This deletes the local skin profile and its PNG. Your active Minecraft skin does not change.")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                actions.deleteSelectedSkin()
                                confirmDelete = false
                            },
                        ) { Text("Remove skin") }
                    }
                }
            }
        }
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
        Text("Current", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Start))
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
            TextButton(onClick = onSave, enabled = texture != null) { Text("Save to library") }
            TextButton(onClick = onReset) { Text("Reset to default") }
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
            Text("Library", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Button(onClick = onNew) { Text("New skin") }
        }
        Spacer(Modifier.height(16.dp))
        if (skins.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No saved skins", style = MaterialTheme.typography.titleMedium)
                Text("Import a 64×64 or legacy 64×32 PNG to start your local library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onNew) { Text("Choose a skin file") }
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
                TextButton(onClick = onDelete, enabled = selected != null) { Text("Delete") }
                TextButton(onClick = onEdit, enabled = selected != null) { Text("Edit") }
                Button(
                    onClick = onUse,
                    enabled = selected != null,
                ) { Text("Use skin") }
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
            .primarySelectable(
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
    BasicAlertDialog(
        onDismissRequest = actions::closeSkinEditor,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 820.dp).heightIn(min = 500.dp, max = 650.dp)
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
        ) {
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (editor.profileId == null) "Add new skin" else "Edit skin",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = actions::closeSkinEditor, enabled = !editor.isSaving) { Text("Close") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = actions::closeSkinEditor, enabled = !editor.isSaving) { Text("Cancel") }
                    OutlinedButton(
                        onClick = { actions.saveSkin(useAfterSave = false) },
                        enabled = editor.canSave,
                    ) { Text("Save") }
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
        Text("Drag to rotate", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
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
            label = { Text("Name") },
            singleLine = true,
            enabled = !editor.isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (editor.canSave) actions.saveSkin(useAfterSave = true) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Player model", style = MaterialTheme.typography.labelLarge)
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
            Text("Skin file", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onBrowse, enabled = !editor.isSaving) {
                Text(if (editor.texture == null) "Choose PNG" else "Replace PNG")
            }
            editor.sourceFileName?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Use a 64×64 PNG, or a legacy 64×32 skin.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
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
    BasicAlertDialog(
        onDismissRequest = { if (!form.isWaiting) actions.closeAccountLogin() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 720.dp)
                .dismissOnEscape(enabled = !form.isWaiting, onDismiss = actions::closeAccountLogin)
                .testTag(LauncherTestTags.ACCOUNT_LOGIN_DIALOG),
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Add account", style = MaterialTheme.typography.headlineMedium)
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
                        label = { Text("Installed Bedrock version") },
                        placeholder = { Text("For example: 1.21.100") },
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
                        label = { Text("Microsoft account email") },
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
                        label = { Text("Microsoft account password") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
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
                            IconButton(onClick = { importedSecretVisible = !importedSecretVisible }) {
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
                        label = { Text("Offline username") },
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
                            Text("Enter this code", color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                authorization.userCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(authorization.verificationUri, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Button(
                                onClick = { uriHandler.openUri(authorization.directVerificationUri) },
                            ) { Text("Open Microsoft sign-in") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = actions::closeAccountLogin) { Text("Cancel") }
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
private fun SettingsPage(state: LauncherUiState, modifier: Modifier, actions: LauncherUiActions) {
    var sectionName by rememberSaveable { mutableStateOf(SettingsSection.GENERAL.name) }
    val section = SettingsSection.entries.firstOrNull { it.name == sectionName } ?: SettingsSection.GENERAL
    val runtimeScrollState = rememberScrollState()
    val logListState = rememberLazyListState()
    Column(modifier.fillMaxSize().testTag(LauncherTestTags.SETTINGS)) {
        PageHeader("Settings") {}
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth < 640.dp) {
                Column(Modifier.fillMaxSize()) {
                    SecondaryScrollableTabRow(selectedTabIndex = section.ordinal, edgePadding = 0.dp) {
                        SettingsSection.entries.forEach { item ->
                            Tab(
                                selected = section == item,
                                onClick = { sectionName = item.name },
                                text = { Text(item.label) },
                            )
                        }
                    }
                    SettingsSectionContent(
                        section,
                        state,
                        actions,
                        runtimeScrollState,
                        logListState,
                        Modifier.weight(1f),
                    )
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.width(210.dp).fillMaxHeight(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            SettingsSection.entries.forEach { item ->
                                SettingsSectionButton(item, selected = section == item, modifier = Modifier.fillMaxWidth()) {
                                    sectionName = item.name
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "$currentPlatform build ${BuildInfo.VERSION}",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSectionContent(
                        section,
                        state,
                        actions,
                        runtimeScrollState,
                        logListState,
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class SettingsSection(val label: String) {
    GENERAL("General"),
    LANGUAGE("Language"),
    APPEARANCE("Appearance"),
    FOLDERS("Folders"),
    CONTENT("Content"),
    NETWORK("Network"),
    PROXY("Proxy"),
    RUNTIME("Runtime"),
    LOGS("Launcher log"),
    SERVICES("Services"),
    TOOLS("Tools"),
}

@Composable
private fun SettingsSectionButton(
    section: SettingsSection,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(section.label) },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    state: LauncherUiState,
    actions: LauncherUiActions,
    runtimeScrollState: ScrollState,
    logListState: LazyListState,
    modifier: Modifier,
) {
    when (section) {
        SettingsSection.GENERAL -> GeneralSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.LANGUAGE -> LanguageSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.APPEARANCE -> AppearanceSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.FOLDERS -> FolderSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.CONTENT -> ContentSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.NETWORK -> NetworkSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.PROXY -> ProxySettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.RUNTIME -> RuntimeSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.LOGS -> LauncherLog(state, actions, logListState, modifier)
        SettingsSection.SERVICES -> ServiceSettings(state, actions, runtimeScrollState, modifier)
        SettingsSection.TOOLS -> ToolSettings(state, actions, runtimeScrollState, modifier)
    }
}

@Composable
private fun SettingsColumn(
    title: String,
    scrollState: ScrollState,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun GeneralSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("General", scrollState, modifier) {
    val preferences = state.launcherPreferences
    Text("Instance sorting", style = MaterialTheme.typography.titleMedium)
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
private fun LanguageSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Language", scrollState, modifier) {
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
private fun FolderSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Folders", scrollState, modifier) {
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
    Text("Folder changes apply after Trestle restarts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ContentSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Mods and modpacks", scrollState, modifier) {
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
    Row(
        Modifier.fillMaxWidth().widthIn(max = 760.dp).toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NetworkSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Tasks and downloads", scrollState, modifier) {
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
    Text("Console", style = MaterialTheme.typography.titleMedium)
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
    Text("HTTP timeout and proxy changes apply to new connections after restart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ProxySettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Proxy", scrollState, modifier) {
    val preferences = state.launcherPreferences
    val proxy = preferences.proxy
    Text("Proxy settings apply to Trestle. Minecraft does not use them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            label = { Text("Address") },
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
            label = { Text("Port") },
            enabled = editable,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(140.dp),
        )
    }
    TextField(
        value = proxy.username,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(username = it))) },
        label = { Text("Username") },
        enabled = editable,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
    TextField(
        value = proxy.password,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(password = it))) },
        label = { Text("Password") },
        enabled = editable,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
    Text("Proxy credentials are stored in the launcher preferences file.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ServiceSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Services", scrollState, modifier) {
    val preferences = state.launcherPreferences
    Text("Modrinth", style = MaterialTheme.typography.titleMedium)
    Text("Available without an API key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider()
    Text("CurseForge", style = MaterialTheme.typography.titleMedium)
    Text("Availability is controlled by the Trestle build API key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider()
    Text("ATLauncher", style = MaterialTheme.typography.titleMedium)
    Text(
        "Direct archive import is available. Catalog access is disabled because ATLauncher does not permit third-party CDN use.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider()
    Text("Technic", style = MaterialTheme.typography.titleMedium)
    TextField(
        value = preferences.technicClientId,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(technicClientId = it)) },
        label = { Text("Client ID") },
        supportingText = { Text("Optional. Some private or rate-limited Technic packs require it. Applies after restart.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
    TextField(
        value = preferences.ftbAppInstancesPath,
        onValueChange = { actions.setLauncherPreferences(preferences.copy(ftbAppInstancesPath = it)) },
        label = { Text("FTB App instances folder") },
        supportingText = { Text("Used when importing existing FTB App instances.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    )
}

@Composable
private fun ToolSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) = SettingsColumn("Tools", scrollState, modifier) {
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
private fun AppearanceSettings(
    state: LauncherUiState,
    actions: LauncherUiActions,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
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
private fun RuntimeSettings(
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
        Text("Runtime", style = MaterialTheme.typography.titleLarge)
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
                    Text("Open ${update.version} release")
                }
            }
        }
    }
}

@Composable
private fun LauncherLog(
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
                    Text("Launcher log", style = MaterialTheme.typography.titleLarge)
                    Text("Events from this session. Right-click an entry to copy diagnostics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = actions::clearLogs, enabled = state.logs.isNotEmpty()) { Text("Clear") }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (state.logs.isEmpty()) {
            item("logs-empty") { Text("No launcher events in this session.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.logs.takeLast(80).asReversed(), key = { it.id }) { entry ->
                LogRow(entry, onDoubleClick = { selectedEntryId = entry.id })
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
private fun LogRow(entry: LogEntry, onDoubleClick: () -> Unit) {
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
        Row(
            Modifier.fillMaxWidth().pointerInput(entry.id) {
                detectTapGestures(onDoubleTap = { onDoubleClick() })
            }.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.width(68.dp)) {
                Text(formatUtcTime(entry.timestampEpochMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(
                    entry.level.name,
                    color = if (entry.level.name == "ERROR") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(Modifier.weight(1f)) {
                Text(entry.message, style = MaterialTheme.typography.bodyMedium)
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
            }
        }
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
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
private fun PageHeader(title: String, action: @Composable () -> Unit) {
    TopAppBar(
        title = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        actions = { action() },
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

@Composable
private fun BridgeMark(modifier: Modifier = Modifier.size(width = 32.dp, height = 24.dp)) {
    Image(
        painter = painterResource(Res.drawable.trestle_mark),
        contentDescription = null,
        modifier = modifier,
    )
}

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
