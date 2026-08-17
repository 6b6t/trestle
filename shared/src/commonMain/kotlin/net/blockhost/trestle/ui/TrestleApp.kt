package net.blockhost.trestle.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_account
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_extension
import net.blockhost.trestle.resources.ic_library
import net.blockhost.trestle.resources.ic_settings
import net.blockhost.trestle.resources.ic_visibility
import net.blockhost.trestle.resources.ic_visibility_off
import net.blockhost.trestle.resources.trestle_mark
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import net.blockhost.trestle.domain.GameInstance
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
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.DependencyKind
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.MinecraftNarratorMode
import net.blockhost.trestle.instance.MinecraftParticleSetting
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Destination(val label: String) {
    LIBRARY("Library"),
    INSTANCE("Instance"),
    DISCOVER("Discover"),
    ACCOUNTS("Accounts"),
    SETTINGS("Settings"),
}

private val globalDestinations = listOf(
    Destination.LIBRARY,
    Destination.DISCOVER,
    Destination.ACCOUNTS,
    Destination.SETTINGS,
)

private val browsableResourceTypes = listOf(
    ResourceType.MOD,
    ResourceType.MODPACK,
    ResourceType.RESOURCE_PACK,
    ResourceType.SHADER_PACK,
)

private val installableResourceTypes = browsableResourceTypes.toSet()

@Composable
fun TrestleApp(state: LauncherUiState, viewModel: LauncherViewModel) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.LIBRARY.name) }
    val destination = Destination.entries.firstOrNull { it.name == destinationName } ?: Destination.LIBRARY
    val snackbarHostState = remember { SnackbarHostState() }
    val destinationStateHolder = rememberSaveableStateHolder()
    val changeDestination: (Destination) -> Unit = { target ->
        if (
            destination == Destination.DISCOVER &&
            target != Destination.DISCOVER &&
            state.resourceBrowser.presentation == ResourceBrowserPresentation.PAGE
        ) {
            viewModel.closeResourceBrowser()
        }
        destinationName = target.name
        if (
            target == Destination.DISCOVER &&
            (!state.resourceBrowser.visible || state.resourceBrowser.presentation != ResourceBrowserPresentation.PAGE)
        ) {
            viewModel.openResourceBrowser(presentation = ResourceBrowserPresentation.PAGE)
        }
    }

    TrestleTheme {
        LaunchedEffect(state.error, state.notice) {
            val message = state.error ?: state.notice ?: return@LaunchedEffect
            val actionLabel = "Retry".takeIf { state.error != null && state.errorRecovery != null }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = state.error != null,
                duration = if (state.error != null) SnackbarDuration.Indefinite else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && actionLabel != null) viewModel.retryError()
            else viewModel.clearMessage()
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                state.operation?.let { OperationBar(it, viewModel::cancelActiveOperation) }
            },
        ) { contentPadding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding)) {
                val compact = maxWidth < 840.dp
                val destinationContent: @Composable (Modifier, Boolean) -> Unit = { modifier, isCompact ->
                    destinationStateHolder.SaveableStateProvider(destination.name) {
                        when (destination) {
                            Destination.LIBRARY -> LibraryPage(
                                state,
                                modifier,
                                viewModel,
                                compact = isCompact,
                                onManage = { changeDestination(Destination.INSTANCE) },
                            )
                            Destination.INSTANCE -> InstanceWorkspace(
                                state,
                                modifier,
                                viewModel,
                                onBack = { changeDestination(Destination.LIBRARY) },
                                compact = isCompact,
                            )
                            Destination.DISCOVER -> ResourceCatalogPage(state, modifier, viewModel)
                            Destination.ACCOUNTS -> AccountsPage(state, modifier, viewModel)
                            Destination.SETTINGS -> SettingsPage(state, modifier, viewModel)
                        }
                    }
                }
                if (compact) {
                    CompactLayout(state, destination, changeDestination, destinationContent)
                } else {
                    WideLayout(state, destination, changeDestination, destinationContent)
                }
            }
            if (state.create.visible) CreateInstanceDialog(state, viewModel)
            if (
                state.resourceBrowser.visible &&
                state.resourceBrowser.presentation == ResourceBrowserPresentation.DIALOG
            ) {
                ResourceBrowserDialog(state, viewModel)
            }
            if (state.instanceSettings.visible) InstanceSettingsDialog(state, viewModel)
            if (state.accountLogin.visible) AccountLoginDialog(state, viewModel)
            if (state.skinStudio.visible && !state.skinStudio.editor.visible) SkinStudioDialog(state, viewModel)
            if (state.skinStudio.editor.visible) SkinEditorDialog(state, viewModel)
            state.pendingInstanceRemovalId?.let { pendingId ->
                val instance = state.instances.firstOrNull { it.id == pendingId }
                AlertDialog(
                    onDismissRequest = viewModel::cancelInstanceRemoval,
                    title = { Text("Remove ${instance?.displayName ?: "instance"}?") },
                    text = { Text("This removes the instance from the library. Its game directory and files stay on disk.") },
                    dismissButton = {
                        TextButton(onClick = viewModel::cancelInstanceRemoval) { Text("Cancel") }
                    },
                    confirmButton = {
                        Button(onClick = viewModel::confirmInstanceRemoval) { Text("Remove from library") }
                    },
                )
            }
        }
    }
}

@Composable
private fun WideLayout(
    state: LauncherUiState,
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    destinationContent: @Composable (Modifier, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopNavigation(state, destination, onDestinationChange)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        destinationContent(Modifier.weight(1f), false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactLayout(
    state: LauncherUiState,
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
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
                        AccountIdentity(account, compact = true) { onDestinationChange(Destination.ACCOUNTS) }
                    } ?: Text(
                        currentPlatform,
                        modifier = Modifier.padding(end = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                globalDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item || item == Destination.LIBRARY && destination == Destination.INSTANCE,
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
private fun TopNavigation(
    state: LauncherUiState,
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BridgeMark()
            Text("TRESTLE", style = MaterialTheme.typography.titleLarge)
        }
        NavigationItem(Destination.LIBRARY, destination == Destination.LIBRARY || destination == Destination.INSTANCE) {
            onDestinationChange(Destination.LIBRARY)
        }
        NavigationItem(Destination.DISCOVER, destination == Destination.DISCOVER) {
            onDestinationChange(Destination.DISCOVER)
        }
        Spacer(Modifier.weight(1f))
        state.accounts.firstOrNull { it.isActive }?.let { account ->
            AccountIdentity(account) { onDestinationChange(Destination.ACCOUNTS) }
        } ?: TextButton(onClick = { onDestinationChange(Destination.ACCOUNTS) }) { Text("Add account") }
        NavigationItem(Destination.SETTINGS, destination == Destination.SETTINGS) {
            onDestinationChange(Destination.SETTINGS)
        }
        Text(
            currentPlatform,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun destinationIcon(destination: Destination): DrawableResource = when (destination) {
    Destination.LIBRARY -> Res.drawable.ic_library
    Destination.INSTANCE -> Res.drawable.ic_library
    Destination.DISCOVER -> Res.drawable.ic_extension
    Destination.ACCOUNTS -> Res.drawable.ic_account
    Destination.SETTINGS -> Res.drawable.ic_settings
}

@Composable
private fun NavigationItem(
    destination: Destination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.width(84.dp).height(52.dp).clickable(onClick = onClick).padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            destination.label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.height(3.dp).fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun AccountIdentity(account: ManagedAccount, compact: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = if (compact) 0.dp else 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(30.dp).background(Ochre, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(account.profile.playerName.take(1).uppercase(), color = Soot, style = MaterialTheme.typography.labelLarge)
        }
        if (!compact) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(account.profile.playerName, style = MaterialTheme.typography.labelLarge)
                Text(if (account.isReady) "Ready" else "Sign-in required", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LibraryPage(
    state: LauncherUiState,
    modifier: Modifier,
    viewModel: LauncherViewModel,
    compact: Boolean = false,
    onManage: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val compactListState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val filteredInstances = state.instances.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) ||
            it.minecraftVersionId.contains(query, ignoreCase = true) || it.modLoader.label.contains(query, ignoreCase = true)
    }
    when {
        state.isInitializing -> LoadingRows(modifier.fillMaxSize())
        state.instances.isEmpty() -> EmptyLibrary(viewModel::openCreate, modifier.fillMaxSize())
        compact -> Column(modifier.fillMaxSize()) {
            InstanceShelfToolbar(query, { query = it }, compact = true, viewModel::openCreate)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            state.selectedInstance?.let { instance ->
                CompactLaunchStrip(state, instance, viewModel, onManage)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            InstanceCollection(
                instances = filteredInstances,
                state = state,
                viewModel = viewModel,
                compact = true,
                compactListState = compactListState,
                gridState = gridState,
                modifier = Modifier.weight(1f),
            )
        }
        else -> Row(modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                InstanceShelfToolbar(query, { query = it }, compact = false, viewModel::openCreate)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InstanceCollection(
                    instances = filteredInstances,
                    state = state,
                    viewModel = viewModel,
                    compact = false,
                    compactListState = compactListState,
                    gridState = gridState,
                    modifier = Modifier.weight(1f),
                )
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SelectedInstancePanel(
                state = state,
                viewModel = viewModel,
                onManage = onManage,
                modifier = Modifier.width(300.dp).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun InstanceCollection(
    instances: List<GameInstance>,
    state: LauncherUiState,
    viewModel: LauncherViewModel,
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
            Text("Try another name, version, or loader.", color = Muted)
        }
    } else if (compact) {
        LazyColumn(
            state = compactListState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(instances, key = { it.id.value }) { instance ->
                InstanceTile(instance, instance.id == state.selectedInstance?.id, state, viewModel, compact = true)
            }
        }
    } else {
        InstanceGrid(instances, state, viewModel, gridState, modifier)
    }
}

@Composable
private fun InstanceShelfToolbar(query: String, onQueryChange: (String) -> Unit, compact: Boolean, onNew: () -> Unit) {
    if (compact) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Instances", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onNew,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                ) { Text("New") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search instances…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search instances…") },
                singleLine = true,
                modifier = Modifier.width(260.dp),
            )
            Button(
                onClick = onNew,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) { Text("New instance") }
        }
    }
}

@Composable
private fun InlineMessage(message: String, error: Boolean, onRetry: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().background(
            if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
        )
        onRetry?.let { TextButton(onClick = it) { Text("Retry") } }
    }
}

@Composable
private fun OperationBar(status: OperationStatus, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
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
                status.detail?.let {
                    Text(
                        it,
                        color = Muted,
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

@Composable
private fun CompactLaunchStrip(
    launcherState: LauncherUiState,
    instance: GameInstance,
    viewModel: LauncherViewModel,
    onManage: () -> Unit,
) {
    val activeAccount = launcherState.accounts.firstOrNull { it.isActive }
    val installationState = instance.installationState
    val progress = installationState.installationProgress()
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InstanceArtwork(instance, 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(instance.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted, maxLines = 1)
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
            OutlinedButton(
                onClick = { viewModel.openResourceBrowser() },
                enabled = installationState is InstallationState.Installed,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
            ) { Text("Content") }
            OutlinedButton(
                onClick = onManage,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
            ) { Text("Manage") }
        }
        PrimaryInstanceButton(instance, launcherState, viewModel, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SelectedInstancePanel(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    onManage: () -> Unit,
    modifier: Modifier,
) {
    val instance = state.selectedInstance
    if (instance == null) {
        Column(
            modifier.background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Select an instance", style = MaterialTheme.typography.titleLarge)
            Text("Choose one from the library.", color = Muted)
        }
        return
    }

    val installationState = instance.installationState
    val activeAccount = state.accounts.firstOrNull { it.isActive }
    Column(
        modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InstanceArtwork(instance, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(instance.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted)
            }
        }
        Text(stateLabel(installationState), color = stateColor(installationState), style = MaterialTheme.typography.labelLarge)
        InstallationProgress(installationState, installationState.installationProgress())
        LaunchContext(state = installationState, activeAccount = activeAccount)
        LaunchReadiness(state, instance)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PrimaryInstanceButton(instance, state, viewModel, Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = { viewModel.openResourceBrowser() },
            enabled = installationState is InstallationState.Installed,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
        ) { Text("Manage content") }
        OutlinedButton(
            onClick = onManage,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
        ) { Text("Instance settings") }
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
        LaunchStatus.Starting -> "Starting Minecraft"
        is LaunchStatus.Running -> status.processId?.let { "Minecraft is running · Process $it" } ?: "Minecraft is running"
        else -> null
    } ?: return
    val color = when (status) {
        is LaunchStatus.Blocked, is LaunchStatus.Failed -> ErrorText
        else -> Muted
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
    Text(accountText, color = if (activeAccount?.isReady == true && state is InstallationState.Installed) Chalk else Muted)
}

@Composable
private fun PrimaryInstanceButton(
    instance: GameInstance,
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier,
) {
    when (instance.installationState) {
        is InstallationState.Installing -> OutlinedButton(
            onClick = viewModel::cancelInstall,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
        ) { Text("Pause") }
        is InstallationState.Interrupted -> Button(
            onClick = viewModel::installSelected,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Resume install") }
        is InstallationState.Failed -> Button(
            onClick = viewModel::installSelected,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Retry install") }
        InstallationState.NotInstalled -> Button(
            onClick = viewModel::installSelected,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Install") }
        is InstallationState.Installed -> LaunchButton(state, instance, viewModel, modifier)
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
            color = Ochre,
            trackColor = Rule,
        )
    } else {
        LinearProgressIndicator(Modifier.fillMaxWidth().widthIn(max = 520.dp), color = Ochre, trackColor = Rule)
    }
    Text(
        if (state is InstallationState.Interrupted) {
            "${progress.completedFiles} of ${progress.totalFiles} files saved · Ready to resume"
        } else {
            "${progress.completedFiles} of ${progress.totalFiles} files"
        },
        color = Muted,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun InstanceGrid(
    instances: List<GameInstance>,
    state: LauncherUiState,
    viewModel: LauncherViewModel,
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
                    color = Muted,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            gridItems(groupInstances, key = { it.id.value }) { instance ->
                InstanceTile(instance, instance.id == state.selectedInstance?.id, state, viewModel)
            }
        }
    }
}

private fun instanceGroupLabel(instance: GameInstance): String = when {
    instance.iconReference != null -> "Modpacks"
    instance.modLoader == ModLoader.VANILLA -> "Vanilla"
    else -> "${instance.modLoader.label} instances"
}

@Composable
private fun InstanceTile(
    instance: GameInstance,
    selected: Boolean,
    launcherState: LauncherUiState,
    viewModel: LauncherViewModel,
    compact: Boolean = false,
) {
    val installationState = instance.installationState
    val running = launcherState.launch.instanceId == instance.id && launcherState.launch.status is LaunchStatus.Running
    val progress = installationState.installationProgress()
    val selectionModifier = Modifier
        .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent, RoundedCornerShape(6.dp))
        .then(if (selected) Modifier.border(1.dp, Ochre, RoundedCornerShape(6.dp)) else Modifier)
    ContextActionArea(instanceContextActions(instance, viewModel)) {
        if (compact) {
            Row(
                modifier = selectionModifier.fillMaxWidth().clickable { viewModel.selectInstance(instance.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InstanceArtwork(instance, 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(instance.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted)
                }
                Text(if (running) "Running" else stateLabel(installationState), color = if (running) Ochre else stateColor(installationState))
            }
        } else {
            Column(
                modifier = selectionModifier.fillMaxWidth().clickable { viewModel.selectInstance(instance.id) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InstanceArtwork(instance, 58.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(instance.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted, maxLines = 1)
                }
                Text(
                    if (running) "Running" else stateLabel(installationState),
                    color = if (running) Ochre else stateColor(installationState),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (progress != null) InstallationProgress(installationState, progress)
            }
        }
    }
}

@Composable
private fun InstanceArtwork(instance: GameInstance, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (instance.iconReference != null) {
            AsyncImage(
                model = instance.iconReference,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                instance.modLoader.label.take(2).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun instanceContextActions(instance: GameInstance, viewModel: LauncherViewModel): List<ContextAction> {
    val copyText = rememberCopyText()
    val state = instance.installationState
    val selectedAction: (() -> Unit) -> Unit = { action ->
        viewModel.selectInstance(instance.id)
        action()
    }
    val primaryAction = when (state) {
        is InstallationState.Installing -> ContextAction("Pause installation") {
            selectedAction(viewModel::cancelInstall)
        }
        is InstallationState.Interrupted -> ContextAction("Resume installation") {
            selectedAction(viewModel::installSelected)
        }
        is InstallationState.Installed -> ContextAction("Launch") {
            selectedAction(viewModel::launchSelected)
        }
        is InstallationState.Failed -> ContextAction("Retry installation") {
            selectedAction(viewModel::installSelected)
        }
        InstallationState.NotInstalled -> ContextAction("Install") {
            selectedAction(viewModel::installSelected)
        }
    }
    return buildList {
        add(primaryAction)
        if (state is InstallationState.Installed) {
            add(ContextAction("Inspect launch plan") { selectedAction(viewModel::inspectLaunchPlan) })
        }
        if (state is InstallationState.Installed) {
            add(ContextAction("Add content") { selectedAction { viewModel.openResourceBrowser() } })
        }
        add(ContextAction("Instance settings", separatorBefore = true) { selectedAction(viewModel::openInstanceSettings) })
        add(ContextAction("Copy directory") { copyText(instance.instanceDirectory) })
        add(ContextAction("Copy instance details") { copyText(formatInstanceForClipboard(instance)) })
        add(ContextAction("Remove from library", separatorBefore = true) { selectedAction(viewModel::deleteSelected) })
    }
}

@Composable
private fun LaunchButton(
    state: LauncherUiState,
    instance: GameInstance,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier,
) {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status ?: LaunchStatus.NotChecked
    when (status) {
        is LaunchStatus.Running -> OutlinedButton(
            onClick = viewModel::stopLaunch,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
        ) { Text("Stop") }
        LaunchStatus.Checking,
        LaunchStatus.Starting,
        -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
        ) { Text(if (status == LaunchStatus.Checking) "Checking…" else "Starting…") }
        is LaunchStatus.Blocked -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
        ) { Text("Launch") }
        is LaunchStatus.Unavailable -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
        ) { Text("Unavailable") }
        is LaunchStatus.Failed -> Button(
            onClick = viewModel::launchSelected,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Retry launch") }
        LaunchStatus.NotChecked,
        LaunchStatus.Ready,
        -> Button(
            onClick = viewModel::launchSelected,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Launch") }
    }
}

@Composable
private fun InstanceSettingsDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.instanceSettings
    val minimum = form.minimumMemoryMiB.toIntOrNull()
    val maximum = form.maximumMemoryMiB.toIntOrNull()
    val valid = minimum != null && maximum != null && minimum > 0 && maximum >= minimum
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
    Dialog(
        onDismissRequest = { if (!form.isSaving) viewModel.closeInstanceSettings() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 820.dp),
        ) {
            Column {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Instance settings", style = MaterialTheme.typography.headlineMedium)
                    Text("Launch", style = MaterialTheme.typography.titleMedium)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val memoryField: @Composable (Boolean, Modifier) -> Unit = { isMinimum, modifier ->
                            val error = if (isMinimum) minimumError else maximumError
                            TextField(
                                value = if (isMinimum) form.minimumMemoryMiB else form.maximumMemoryMiB,
                                onValueChange = if (isMinimum) viewModel::setMinimumMemory else viewModel::setMaximumMemory,
                                label = { Text(if (isMinimum) "Minimum memory (MiB)" else "Maximum memory (MiB)") },
                                isError = error != null,
                                supportingText = if (error == null) null else ({ Text(error) }),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = if (isMinimum) ImeAction.Next else ImeAction.Done,
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
                        Text(form.recommendation.orEmpty(), color = Muted, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::applyRecommendedMemory) { Text("Use recommended") }
                    }
                    form.warnings.forEach { warning -> Text(warning, color = ErrorText) }
                    TextField(
                        value = form.jvmArguments,
                        onValueChange = viewModel::setJvmArguments,
                        label = { Text("Additional JVM arguments") },
                        supportingText = {
                            Text("Memory, classpath, native path, and architecture options are managed by Trestle.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HorizontalDivider()
                    Text("Minecraft client", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Changes are written to this instance's options.txt. Other game and mod settings are kept.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    when {
                        form.isLoadingClientSettings -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Loading client settings", color = Muted)
                        }
                        form.clientSettingsError != null -> Text(form.clientSettingsError, color = ErrorText)
                        form.clientSettings != null -> ClientSettingsFields(
                            form.clientSettings,
                            viewModel::setInstanceClientSettings,
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = viewModel::closeInstanceSettings, enabled = !form.isSaving) { Text("Cancel") }
                    Button(
                        onClick = viewModel::saveInstanceSettings,
                        enabled = valid && !form.isLoadingClientSettings && !form.isSaving,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Save changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceBrowserDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val browser = state.resourceBrowser
    Dialog(
        onDismissRequest = viewModel::closeResourceBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f).widthIn(max = 1040.dp),
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
                            color = Muted,
                        )
                    }
                    TextButton(onClick = viewModel::closeResourceBrowser, enabled = !browser.isInstalling) { Text("Close") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ResourceBrowserContent(state, viewModel, Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ResourceBrowserContent(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier,
) {
    val browser = state.resourceBrowser
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>()
    val resultListState = rememberLazyListState()
    val detailScrollState = rememberScrollState()
    val listPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden

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
        viewModel.clearResourceSelection()
    }
    PlatformBackHandler(
        enabled = browser.selectedProject != null && listPaneHidden && navigator.canNavigateBack(),
        onBack = clearSelection,
    )
    Column(modifier) {
        ResourceBrowserToolbar(browser, viewModel)
        browser.error?.let { InlineMessage(it, true, null) }
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.fillMaxSize(),
            listPane = {
                AnimatedPane {
                    ResourceResultList(
                        browser = browser,
                        viewModel = viewModel,
                        listState = resultListState,
                        onProjectClick = viewModel::selectResource,
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
                            viewModel = viewModel,
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
private fun ResourceBrowserToolbar(browser: ResourceBrowserState, viewModel: LauncherViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = browser.query,
                onValueChange = viewModel::setResourceQuery,
                label = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchResources() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = viewModel::searchResources,
                enabled = !browser.isSearching,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) { Text("Search") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceProviderButtons(browser, viewModel)
                    Selector(
                        label = "Content type",
                        value = browser.type.label,
                        values = browsableResourceTypes.map { it.label },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { label -> viewModel.setResourceType(browsableResourceTypes.first { it.label == label }) },
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceProviderButtons(browser, viewModel)
                    Spacer(Modifier.weight(1f))
                    Selector(
                        label = "Content type",
                        value = browser.type.label,
                        values = browsableResourceTypes.map { it.label },
                        modifier = Modifier.width(190.dp),
                        onSelect = { label -> viewModel.setResourceType(browsableResourceTypes.first { it.label == label }) },
                    )
                }
            }
        }
        if (!browser.curseForgeAvailable) {
            Text("CurseForge requires a Trestle API key configured by the application build.", color = Muted)
        }
    }
}

@Composable
private fun ResourceProviderButtons(browser: ResourceBrowserState, viewModel: LauncherViewModel) {
    SingleChoiceSegmentedButtonRow {
        ResourceProvider.entries.forEachIndexed { index, provider ->
            val available = provider != ResourceProvider.CURSEFORGE || browser.curseForgeAvailable
            SegmentedButton(
                selected = browser.provider == provider,
                onClick = { viewModel.setResourceProvider(provider) },
                enabled = available,
                shape = SegmentedButtonDefaults.itemShape(index, ResourceProvider.entries.size),
            ) { Text(provider.label) }
        }
    }
}

@Composable
private fun ResourceResultList(
    browser: ResourceBrowserState,
    viewModel: LauncherViewModel,
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
            Text("Change the search or content type.", color = Muted)
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
                        onClick = viewModel::loadMoreResources,
                        enabled = !browser.isSearching,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (browser.isSearching) "Loading" else "Load more") }
                }
            }
        }
    }
}

@Composable
private fun ResourceProjectRow(project: ResourceProject, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ResourceProjectImage(
            project = project,
            modifier = Modifier.size(width = 96.dp, height = 64.dp),
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
                Text(formatDownloads(project.downloads), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "${project.provider.label} · ${project.author.ifBlank { "Unknown author" }}",
                color = Ochre,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(project.summary, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (project.categories.isNotEmpty()) {
                Text(
                    project.categories.take(3).joinToString(" · "),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ResourceProjectImage(project: ResourceProject, modifier: Modifier) {
    var useIconFallback by remember(project.provider, project.id, project.featuredImageUrl) { mutableStateOf(false) }
    val showingFeaturedImage = project.featuredImageUrl != null && !useIconFallback
    val imageUrl = if (showingFeaturedImage) project.featuredImageUrl else project.iconUrl
    Box(
        modifier.clip(RoundedCornerShape(6.dp)).background(RaisedSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            project.name.firstOrNull()?.uppercase() ?: "?",
            color = Muted,
            style = MaterialTheme.typography.titleLarge,
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = if (showingFeaturedImage) ContentScale.Crop else ContentScale.Fit,
                onError = {
                    if (showingFeaturedImage && project.iconUrl != null) useIconFallback = true
                },
                modifier = Modifier.fillMaxSize().padding(if (showingFeaturedImage) 0.dp else 8.dp),
            )
        }
    }
}

@Composable
private fun ResourceSelection(
    browser: ResourceBrowserState,
    instance: GameInstance?,
    viewModel: LauncherViewModel,
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
            Text("Available versions and installation details will appear here.", color = Muted)
            return@Column
        }
        if (project.featuredImageUrl != null) {
            ResourceProjectImage(project, Modifier.fillMaxWidth().height(156.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (project.featuredImageUrl == null && project.iconUrl != null) {
                ResourceProjectImage(project, Modifier.size(56.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "by ${project.author.ifBlank { "Unknown author" }} on ${project.provider.label}",
                    color = Ochre,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(project.summary, color = Muted)
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
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Ochre)
        } else if (browser.versions.isNotEmpty()) {
            ResourceVersionPicker(browser, viewModel)
        }
        val version = browser.selectedVersion
        version?.let {
            ResourceVersionDetails(it)
            val optionalDependencies = it.dependencies.filter { dependency -> dependency.kind == DependencyKind.OPTIONAL }
            if (optionalDependencies.isNotEmpty()) {
                Text("Optional dependencies", style = MaterialTheme.typography.titleMedium)
                optionalDependencies.forEach { dependency ->
                    Row(
                        Modifier.fillMaxWidth().toggleable(
                            value = dependency.selectionKey in browser.selectedOptionalDependencies,
                            enabled = dependency.selectionKey.isNotBlank(),
                            role = Role.Checkbox,
                            onValueChange = { viewModel.toggleOptionalDependency(dependency.selectionKey) },
                        ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = dependency.selectionKey in browser.selectedOptionalDependencies,
                            onCheckedChange = null,
                            enabled = dependency.selectionKey.isNotBlank(),
                        )
                        Text(
                            dependency.fileName ?: dependency.projectId ?: dependency.versionId ?: "External dependency",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val supportedType = project.type in installableResourceTypes
        val instanceReady = project.type == ResourceType.MODPACK || instance?.installationState is InstallationState.Installed
        val selectedFile = version?.primaryFile
        val downloadable = selectedFile?.url != null || selectedFile?.sha1 != null
        if (!supportedType) Text("This content type cannot be installed into an instance yet.", color = ErrorText)
        if (selectedFile?.url == null && selectedFile?.sha1 != null) {
            Text("CurseForge blocks this file. Trestle will look for the identical file on Modrinth.", color = Muted)
        } else if (version != null && !downloadable) {
            Text("The author blocks downloads from third-party launchers.", color = ErrorText)
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
                shape = RoundedCornerShape(8.dp),
            ) { Text("Open manual download") }
        }
        Button(
            onClick = viewModel::installSelectedResource,
            enabled = supportedType && instanceReady && downloadable && !browser.isInstalling,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) {
            if (browser.isInstalling) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (project.type == ResourceType.MODPACK) "Create instance" else "Install")
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
private fun ResourceVersionPicker(browser: ResourceBrowserState, viewModel: LauncherViewModel) {
    val selected = browser.selectedVersion
    val labels = browser.versions.map { "${it.versionNumber} · ${it.channel.label}" }
    Selector(
        label = "Version",
        value = selected?.let { "${it.versionNumber} · ${it.channel.label}" } ?: "Select version",
        values = labels,
        modifier = Modifier.fillMaxWidth(),
        onSelect = { label ->
            browser.versions.getOrNull(labels.indexOf(label))?.let { viewModel.selectResourceVersion(it.id) }
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
private fun CreateInstanceDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.create
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { if (!form.isSaving) viewModel.closeCreate() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 760.dp),
        ) {
            Column {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("New instance", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        Modifier.fillMaxWidth().background(RaisedSurface).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier.background(Surface, RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                        ) { Text("Custom", color = Chalk, style = MaterialTheme.typography.labelLarge) }
                        TextButton(
                            onClick = {
                                viewModel.closeCreate()
                                viewModel.openResourceBrowser(ResourceType.MODPACK)
                            },
                        ) { Text("Browse modpacks") }
                    }
                    TextField(
                        value = form.name,
                        onValueChange = viewModel::setCreateName,
                        label = { Text("Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Selector(
                        label = "Minecraft version",
                        value = form.versionId.ifBlank {
                            if (state.isLoadingVersions) "Loading versions" else "No versions available"
                        },
                        values = state.versions.take(200).map { it.id },
                        enabled = !state.isLoadingVersions,
                        onSelect = viewModel::setCreateVersion,
                    )
                    Selector(
                        label = "Loader",
                        value = form.modLoader.label,
                        values = listOf(ModLoader.VANILLA, ModLoader.FABRIC).map { it.label },
                        onSelect = { label -> viewModel.setCreateLoader(ModLoader.entries.first { it.label == label }) },
                    )
                    if (form.modLoader == ModLoader.FABRIC) {
                        Selector(
                            label = "Fabric Loader",
                            value = form.loaderVersion ?: if (form.isResolvingLoader) "Loading" else "No compatible loader",
                            values = form.loaderVersions,
                            enabled = !form.isResolvingLoader,
                            onSelect = viewModel::setCreateLoaderVersion,
                        )
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Client defaults", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (showAdvanced) "Configure first-launch accessibility, audio, and distance settings."
                                else "Trestle will use balanced defaults for the first launch.",
                                color = Muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Hide" else "Customize")
                        }
                    }
                    if (showAdvanced) ClientDefaultsFields(form, viewModel, showHeading = false)
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = viewModel::closeCreate, enabled = !form.isSaving) { Text("Cancel") }
                    Button(
                        onClick = viewModel::createInstance,
                        enabled = form.name.isNotBlank() && form.versionId.isNotBlank() &&
                            (form.modLoader != ModLoader.FABRIC || form.loaderVersion != null) && !form.isSaving,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (form.isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Creating...")
                        } else {
                            Text("Create instance")
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
    viewModel: LauncherViewModel,
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
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = form.preconfigureClientSettings,
                    onCheckedChange = viewModel::setCreateClientPreconfiguration,
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Apply these defaults", modifier = Modifier.weight(1f), color = Muted)
                Switch(
                    checked = form.preconfigureClientSettings,
                    onCheckedChange = viewModel::setCreateClientPreconfiguration,
                )
            }
        }
        if (!form.preconfigureClientSettings) return@Column

        ClientSettingsFields(settings, viewModel::setCreateClientSettings)
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
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
        slider()
    }
}

@Composable
private fun ClientSettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        Text("Create an isolated Vanilla or Fabric instance.", color = Muted)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew, shape = RoundedCornerShape(8.dp)) { Text("Create instance") }
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
                        RoundedCornerShape(8.dp),
                    ),
            )
        }
    }
}

private enum class InstanceSection(val label: String) {
    OVERVIEW("Overview"),
    CONTENT("Content"),
    SETTINGS("Settings"),
}

@Composable
private fun InstanceWorkspace(
    state: LauncherUiState,
    modifier: Modifier,
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    compact: Boolean = false,
) {
    val instance = state.selectedInstance
    var sectionName by rememberSaveable(instance?.id) { mutableStateOf(InstanceSection.OVERVIEW.name) }
    val section = InstanceSection.entries.firstOrNull { it.name == sectionName } ?: InstanceSection.OVERVIEW
    val overviewListState = rememberLazyListState()
    val contentListState = rememberLazyListState()
    val configurationScrollState = rememberScrollState()
    Column(modifier.fillMaxSize()) {
        PageHeader(instance?.displayName ?: "Instance") {
            TextButton(onClick = onBack) { Text("Back to library") }
        }
        HorizontalDivider(color = Rule)
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
        Row(
            Modifier.fillMaxWidth().background(Surface).padding(horizontal = if (compact) 8.dp else 24.dp),
        ) {
            InstanceSection.entries.forEach { item ->
                NavigationTab(
                    label = item.label,
                    selected = section == item,
                    modifier = if (compact) Modifier.weight(1f) else Modifier,
                ) { sectionName = item.name }
            }
        }
        HorizontalDivider(color = Rule)
        when (section) {
            InstanceSection.OVERVIEW -> InstanceOverview(
                state,
                instance,
                viewModel,
                overviewListState,
                Modifier.weight(1f),
                compact,
            )
            InstanceSection.CONTENT -> InstanceContent(instance, viewModel, contentListState, Modifier.weight(1f))
            InstanceSection.SETTINGS -> InstanceConfiguration(
                instance,
                viewModel,
                configurationScrollState,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavigationTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(50.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(label, color = if (selected) Chalk else Muted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) Ochre else Color.Transparent))
    }
}

@Composable
private fun InstanceOverview(
    state: LauncherUiState,
    instance: GameInstance,
    viewModel: LauncherViewModel,
    listState: LazyListState,
    modifier: Modifier,
    compact: Boolean,
) {
    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(24.dp)) {
        item("identity") {
            if (compact) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        InstanceArtwork(instance, 64.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(instance.displayName, style = MaterialTheme.typography.headlineMedium)
                            Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted)
                            Text(stateLabel(instance.installationState), color = stateColor(instance.installationState))
                        }
                    }
                    PrimaryInstanceButton(instance, state, viewModel, Modifier.fillMaxWidth())
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    InstanceArtwork(instance, 80.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(instance.displayName, style = MaterialTheme.typography.headlineMedium)
                        Text("Minecraft ${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted)
                        Text(stateLabel(instance.installationState), color = stateColor(instance.installationState))
                    }
                    PrimaryInstanceButton(instance, state, viewModel, Modifier.widthIn(min = 132.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        item("properties") {
            Text("Instance", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            PropertyRow("Java", instance.requiredJavaMajor.toString())
            PropertyRow("Memory", "${instance.memory.minimumMiB}–${instance.memory.maximumMiB} MiB")
            PropertyRow("Directory", instance.instanceDirectory)
            PropertyRow("Last launch", instance.lastLaunchAtEpochMillis?.toString() ?: "Never")
        }
        state.launchPlan?.let { plan ->
            item("launch-plan") {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Launch plan", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = viewModel::inspectLaunchPlan) { Text("Refresh") }
                }
                PropertyRow("Main class", plan.mainClass)
                PropertyRow("Classpath", "${plan.classpathEntries} entries")
                PropertyRow("Natives", "${plan.nativeLibraries} libraries")
                PropertyRow("Account", plan.authentication)
            }
        } ?: item("inspect") {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = viewModel::inspectLaunchPlan,
                enabled = instance.installationState is InstallationState.Installed,
                shape = RoundedCornerShape(6.dp),
            ) { Text("Inspect launch plan") }
        }
    }
}

@Composable
private fun InstanceContent(
    instance: GameInstance,
    viewModel: LauncherViewModel,
    listState: LazyListState,
    modifier: Modifier,
) {
    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
        item("intro") {
            Text(
                "Browse compatible content for ${instance.displayName}. Required dependencies are resolved during installation.",
                modifier = Modifier.widthIn(max = 720.dp).padding(vertical = 16.dp),
                color = Muted,
            )
        }
        items(browsableResourceTypes.filterNot { it == ResourceType.MODPACK }, key = { it.name }) { type ->
            ContentTypeRow(
                type = type,
                enabled = instance.installationState is InstallationState.Installed,
                onClick = { viewModel.openResourceBrowser(type) },
            )
            HorizontalDivider(color = Rule)
        }
    }
}

@Composable
private fun InstanceConfiguration(
    instance: GameInstance,
    viewModel: LauncherViewModel,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    Column(modifier.verticalScroll(scrollState).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Instance settings", style = MaterialTheme.typography.titleLarge)
        Text(
            "Launch and Minecraft client settings apply only to ${instance.displayName}.",
            color = Muted,
            modifier = Modifier.widthIn(max = 640.dp).padding(bottom = 8.dp),
        )
        PropertyRow("Java", "Java ${instance.requiredJavaMajor}")
        PropertyRow("Minimum memory", "${instance.memory.minimumMiB} MiB")
        PropertyRow("Maximum memory", "${instance.memory.maximumMiB} MiB")
        PropertyRow("JVM arguments", instance.jvmArguments.joinToString().ifBlank { "Automatic" })
        OutlinedButton(
            onClick = viewModel::openInstanceSettings,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(6.dp),
        ) { Text("Edit instance settings") }
    }
}

@Composable
private fun ResourceCatalogPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    LaunchedEffect(state.resourceBrowser.visible, state.resourceBrowser.presentation) {
        if (
            !state.resourceBrowser.visible ||
            state.resourceBrowser.presentation != ResourceBrowserPresentation.PAGE
        ) {
            viewModel.openResourceBrowser(presentation = ResourceBrowserPresentation.PAGE)
        }
    }
    Column(modifier.fillMaxSize()) {
        PageHeader("Discover") {}
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (
            state.resourceBrowser.visible &&
            state.resourceBrowser.presentation == ResourceBrowserPresentation.PAGE
        ) {
            ResourceBrowserContent(state, viewModel, Modifier.fillMaxSize())
        } else {
            LoadingRows(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ContentTypeRow(type: ResourceType, enabled: Boolean, onClick: () -> Unit) {
    val description = when (type) {
        ResourceType.MOD -> "Extend an installed game with compatible client mods and required dependencies."
        ResourceType.MODPACK -> "Create a complete, isolated instance from a curated pack."
        ResourceType.RESOURCE_PACK -> "Change textures, sounds, and presentation without changing game logic."
        ResourceType.SHADER_PACK -> "Add compatible lighting and rendering effects to an installed instance."
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(Modifier.size(56.dp).background(RaisedSurface, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Text(type.label.take(1), color = Ochre, style = MaterialTheme.typography.headlineMedium)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(type.label, style = MaterialTheme.typography.titleLarge)
            Text(description, color = Muted, modifier = Modifier.widthIn(max = 680.dp))
            if (!enabled) Text("Select and install an instance first.", color = ErrorText, style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(6.dp)) { Text("Browse") }
    }
}

@Composable
private fun AccountsPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize()) {
        PageHeader("Accounts") {
            Button(
                onClick = viewModel::openAccountLogin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) { Text("Add account") }
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
                    color = Muted,
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
                        color = Muted,
                    )
                }
                items(state.accounts, key = { it.profile.profileId }) { account ->
                    AccountRow(
                        account = account,
                        texture = state.accountSkinTextures[account.profile.profileId],
                        viewModel = viewModel,
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
                        viewModel.removeAccount(profileId)
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
    viewModel: LauncherViewModel,
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
    val actions = buildList {
        if (!account.isActive) {
            add(ContextAction("Use account") { viewModel.selectAccount(profileId) })
        }
        if (canManageOfficialProfile) {
            add(ContextAction("Manage skins") { viewModel.openSkinStudio() })
            add(ContextAction("Refresh profile") { viewModel.refreshActiveAccount() })
            add(ContextAction("Reset skin") { viewModel.resetActiveSkin() })
        }
        if (account.isAuthenticated) {
            add(ContextAction("Sign out", separatorBefore = isNotEmpty()) { viewModel.signOutAccount(profileId) })
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

    ContextActionArea(actions) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 700.dp
            Column(
                Modifier.fillMaxWidth().background(
                    if (account.isActive) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                ),
            ) {
                if (compact) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccountMark(account, texture, compact = true)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(account.profile.playerName, style = MaterialTheme.typography.titleMedium)
                                Text(account.profile.authenticationMethod.label, color = Muted, maxLines = 1)
                            }
                            if (account.isActive) Text("Active", color = Ochre, style = MaterialTheme.typography.labelMedium)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(statusText, modifier = Modifier.weight(1f), color = if (account.isReady) Ochre else ErrorText)
                            if (!account.isActive) {
                                OutlinedButton(
                                    onClick = { viewModel.selectAccount(profileId) },
                                    shape = RoundedCornerShape(6.dp),
                                ) { Text("Use") }
                            }
                            if (canManageOfficialProfile) {
                                TextButton(onClick = viewModel::openSkinStudio) { Text("Skins") }
                            }
                            if (account.isAuthenticated) {
                                TextButton(onClick = { viewModel.signOutAccount(profileId) }) { Text("Sign out") }
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
                                if (account.isActive) Text("Active", color = Ochre, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(account.profile.authenticationMethod.label, color = Muted, maxLines = 1)
                        }
                        Column(Modifier.widthIn(min = 150.dp), horizontalAlignment = Alignment.End) {
                            Text(statusText, color = if (account.isReady) Ochre else ErrorText)
                            account.profile.skin?.let { skin ->
                                Text(
                                    "${skin.variant.name.lowercase().replaceFirstChar(Char::uppercase)} model",
                                    color = Muted,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        if (!account.isActive) {
                            OutlinedButton(onClick = { viewModel.selectAccount(profileId) }, shape = RoundedCornerShape(6.dp)) {
                                Text("Use")
                            }
                        } else if (canManageOfficialProfile) {
                            OutlinedButton(onClick = viewModel::openSkinStudio, shape = RoundedCornerShape(6.dp)) {
                                Text("Manage skins")
                            }
                        }
                        if (account.isAuthenticated) {
                            TextButton(onClick = { viewModel.signOutAccount(profileId) }) { Text("Sign out") }
                        }
                        TextButton(onClick = onForget) { Text("Forget") }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SkinStudioDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val account = state.accounts.firstOrNull { it.isActive }
    val selected = state.savedSkins.firstOrNull { it.profile.id == state.skinStudio.selectedProfileId }
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = viewModel::closeSkinStudio,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Soot,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.9f).widthIn(max = 1040.dp).heightIn(min = 540.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Skins", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::closeSkinStudio) { Text("Close") }
                }
                HorizontalDivider(color = Rule)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth >= 720.dp) {
                        Row(Modifier.fillMaxSize()) {
                            CurrentSkinPanel(
                                playerName = account?.profile?.playerName.orEmpty(),
                                texture = account?.let { state.accountSkinTextures[it.profile.profileId] },
                                variant = account?.profile?.skin?.variant ?: SkinVariant.CLASSIC,
                                onSave = viewModel::saveCurrentSkinToLibrary,
                                onReset = viewModel::resetActiveSkin,
                                modifier = Modifier.width(310.dp).fillMaxHeight(),
                            )
                            VerticalDivider(color = Rule)
                            SkinLibraryPanel(
                                skins = state.savedSkins,
                                selected = selected,
                                onSelect = viewModel::selectSavedSkin,
                                onNew = viewModel::openNewSkin,
                                onUse = viewModel::useSelectedSkin,
                                onEdit = viewModel::editSelectedSkin,
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
                                onSave = viewModel::saveCurrentSkinToLibrary,
                                onReset = viewModel::resetActiveSkin,
                                modifier = Modifier.fillMaxWidth().height(250.dp),
                            )
                            HorizontalDivider(color = Rule)
                            SkinLibraryPanel(
                                skins = state.savedSkins,
                                selected = selected,
                                onSelect = viewModel::selectSavedSkin,
                                onNew = viewModel::openNewSkin,
                                onUse = viewModel::useSelectedSkin,
                                onEdit = viewModel::editSelectedSkin,
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
        Dialog(onDismissRequest = { confirmDelete = false }) {
            Surface(color = Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.widthIn(max = 420.dp)) {
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
                                viewModel.deleteSelectedSkin()
                                confirmDelete = false
                            },
                            shape = RoundedCornerShape(8.dp),
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
            emptyLabel = "Loading skin preview",
        )
        Text(playerName, style = MaterialTheme.typography.titleMedium)
        Text(
            "${variant.label} model · Drag to rotate",
            color = Muted,
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
        Modifier.size(48.dp).background(if (account.isActive) Ochre else Surface, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            account.profile.playerName.take(1).uppercase(),
            color = if (account.isActive) Soot else Chalk,
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
            Button(
                onClick = onNew,
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                shape = RoundedCornerShape(8.dp),
            ) { Text("New skin") }
        }
        Spacer(Modifier.height(16.dp))
        if (skins.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No saved skins", style = MaterialTheme.typography.titleMedium)
                Text("Import a 64×64 or legacy 64×32 PNG to start your local library.", color = Muted)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onNew, shape = RoundedCornerShape(8.dp)) { Text("Choose a skin file") }
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
                    )
                }
            }
            HorizontalDivider(color = Rule)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("Use skin") }
            }
        }
    }
}

@Composable
private fun SkinLibraryItem(skin: SavedSkin, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, if (selected) Ochre else Rule, shape)
            .background(if (selected) RaisedSurface else Surface, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
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
        Text(skin.profile.variant.label, color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SkinEditorDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val editor = state.skinStudio.editor
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("png")),
    ) { file ->
        if (file != null) {
            scope.launch {
                runCatching { file.readBytes() }
                    .onSuccess { viewModel.setSkinFile(file.name, it) }
                    .onFailure { viewModel.reportSkinFileReadFailure() }
            }
        }
    }
    Dialog(
        onDismissRequest = viewModel::closeSkinEditor,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 820.dp).heightIn(min = 500.dp, max = 650.dp),
        ) {
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
                    TextButton(onClick = viewModel::closeSkinEditor, enabled = !editor.isSaving) { Text("Close") }
                }
                HorizontalDivider(color = Rule)
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val compact = maxWidth < 650.dp
                    if (compact) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                            SkinEditorPreview(editor)
                            Spacer(Modifier.height(20.dp))
                            SkinEditorFields(editor, { picker.launch() }, viewModel)
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            SkinEditorPreview(editor, Modifier.width(300.dp).fillMaxHeight().padding(24.dp))
                            VerticalDivider(color = Rule)
                            SkinEditorFields(
                                editor = editor,
                                onBrowse = { picker.launch() },
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = Rule)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = viewModel::closeSkinEditor, enabled = !editor.isSaving) { Text("Cancel") }
                    OutlinedButton(
                        onClick = { viewModel.saveSkin(useAfterSave = false) },
                        enabled = editor.canSave,
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Save") }
                    Button(
                        onClick = { viewModel.saveSkin(useAfterSave = true) },
                        enabled = editor.canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(if (editor.isSaving) "Saving" else "Save and use") }
                }
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
        Text("Drag to rotate", color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SkinEditorFields(
    editor: SkinEditorState,
    onBrowse: () -> Unit,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextField(
            value = editor.name,
            onValueChange = viewModel::setSkinName,
            label = { Text("Name") },
            singleLine = true,
            enabled = !editor.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Player model", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SkinVariant.entries.forEach { variant ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = editor.variant == variant,
                            onClick = { viewModel.setSkinVariant(variant) },
                            enabled = !editor.isSaving,
                        )
                        Text(variant.label)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Skin file", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onBrowse, enabled = !editor.isSaving, shape = RoundedCornerShape(8.dp)) {
                Text(if (editor.texture == null) "Choose PNG" else "Replace PNG")
            }
            editor.sourceFileName?.let {
                Text(it, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Use a 64×64 PNG, or a legacy 64×32 skin.", color = Muted, style = MaterialTheme.typography.labelMedium)
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
private fun AccountLoginDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.accountLogin
    val uriHandler = LocalUriHandler.current
    var passwordVisible by remember(form.method) { mutableStateOf(false) }
    var importedSecretVisible by remember(form.method) { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { if (!form.isWaiting) viewModel.closeAccountLogin() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 720.dp),
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Add account", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Choose how Trestle should create or verify this account.",
                    color = Muted,
                )
                Selector(
                    label = "Login method",
                    value = form.method.label,
                    values = AccountAuthenticationMethod.entries.map(AccountAuthenticationMethod::label),
                    enabled = !form.isWaiting,
                ) { selected ->
                    AccountAuthenticationMethod.entries.firstOrNull { it.label == selected }
                        ?.let(viewModel::setAccountLoginMethod)
                }
                if (form.edition == MinecraftEdition.BEDROCK) {
                    TextField(
                        value = form.bedrockGameVersion,
                        onValueChange = viewModel::setBedrockGameVersion,
                        label = { Text("Installed Bedrock version") },
                        placeholder = { Text("For example: 1.21.100") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Bedrock authentication is stored for the future runtime adapter. Trestle cannot launch Bedrock yet.",
                        color = Muted,
                    )
                }
                if (
                    form.method == AccountAuthenticationMethod.MICROSOFT_CREDENTIALS ||
                    form.method == AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS
                ) {
                    TextField(
                        value = form.email,
                        onValueChange = viewModel::setAccountEmail,
                        label = { Text("Microsoft account email") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.password.reveal(),
                        onValueChange = viewModel::setAccountPassword,
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Microsoft discourages direct password login and it cannot complete MFA. " +
                            "Trestle discards the password after this attempt and stores only encrypted token state.",
                        color = Muted,
                    )
                }
                if (form.method.requiresImportedSecret) {
                    TextField(
                        value = form.importedSecret.reveal(),
                        onValueChange = viewModel::setImportedAccountSecret,
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
                    Text(form.method.importWarning, color = Muted)
                }
                if (form.method == AccountAuthenticationMethod.OFFLINE) {
                    TextField(
                        value = form.offlineUsername,
                        onValueChange = viewModel::setOfflineUsername,
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Offline accounts prove no ownership. They only work with single-player and servers that allow offline identities.",
                        color = Muted,
                    )
                }
                form.authorization?.let { authorization ->
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Enter this code", color = Muted)
                        Text(authorization.userCode, style = MaterialTheme.typography.headlineMedium, color = Ochre)
                        Text(authorization.verificationUri, color = Muted)
                        Button(
                            onClick = { uriHandler.openUri(authorization.directVerificationUri) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                        ) { Text("Open Microsoft sign-in") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::closeAccountLogin) { Text("Cancel") }
                    if (form.authorization == null) {
                        Button(
                            onClick = viewModel::signInAccount,
                            enabled = !form.isWaiting && form.canSubmit,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
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
private fun SettingsPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    var sectionName by rememberSaveable { mutableStateOf(SettingsSection.RUNTIME.name) }
    val section = SettingsSection.entries.firstOrNull { it.name == sectionName } ?: SettingsSection.RUNTIME
    val runtimeScrollState = rememberScrollState()
    val logListState = rememberLazyListState()
    Column(modifier.fillMaxSize()) {
        PageHeader("Settings") {}
        HorizontalDivider(color = Rule)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth < 640.dp) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().background(Surface).padding(horizontal = 12.dp)) {
                        SettingsSection.entries.forEach { item ->
                            SettingsSectionButton(
                                item,
                                selected = section == item,
                                modifier = Modifier.weight(1f),
                            ) { sectionName = item.name }
                        }
                    }
                    HorizontalDivider(color = Rule)
                    SettingsSectionContent(
                        section,
                        state,
                        viewModel,
                        runtimeScrollState,
                        logListState,
                        Modifier.weight(1f),
                    )
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.width(210.dp).fillMaxHeight().background(Surface).padding(12.dp)) {
                        SettingsSection.entries.forEach { item ->
                            SettingsSectionButton(item, selected = section == item, modifier = Modifier.fillMaxWidth()) {
                                sectionName = item.name
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$currentPlatform build ${BuildInfo.VERSION}",
                            modifier = Modifier.padding(12.dp),
                            color = Muted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSectionContent(
                        section,
                        state,
                        viewModel,
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
    RUNTIME("Runtime"),
    LOGS("Launcher log"),
}

@Composable
private fun SettingsSectionButton(
    section: SettingsSection,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier.clickable(onClick = onClick).background(if (selected) RaisedSurface else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(3.dp).height(22.dp).background(if (selected) Ochre else Color.Transparent))
        Text(section.label, color = if (selected) Chalk else Muted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    runtimeScrollState: ScrollState,
    logListState: LazyListState,
    modifier: Modifier,
) {
    when (section) {
        SettingsSection.RUNTIME -> RuntimeSettings(state, viewModel, runtimeScrollState, modifier)
        SettingsSection.LOGS -> LauncherLog(state, viewModel, logListState, modifier)
    }
}

@Composable
private fun RuntimeSettings(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Runtime", style = MaterialTheme.typography.titleLarge)
        Text(
            "Trestle resolves the platform runtime and compatible Minecraft metadata automatically.",
            color = Muted,
            modifier = Modifier.widthIn(max = 640.dp).padding(bottom = 8.dp),
        )
        PropertyRow("Platform", currentPlatform)
        PropertyRow("Instances", state.instances.size.toString())
        PropertyRow("Accounts", state.accounts.size.toString())
        state.credentialProtection?.let { protection ->
            PropertyRow(
                "Credential vault",
                if (protection.encryptionOperational) protection.effectiveLevel else "Unavailable",
            )
        }
        Text(
            if (currentPlatform == "Android") {
                "Android can manage and install instances. A native game runtime is not installed."
            } else {
                "Desktop launch preparation downloads and uses Mojang's compatible Java runtime automatically."
            },
            color = Muted,
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 640.dp),
        )
        OutlinedButton(
            onClick = viewModel::refreshVersions,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(if (state.isLoadingVersions) "Refreshing versions" else "Refresh versions") }
    }
}

@Composable
private fun LauncherLog(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    listState: LazyListState,
    modifier: Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        item("logs-heading") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Launcher log", style = MaterialTheme.typography.titleLarge)
                    Text("Events from this session. Right-click an entry to copy diagnostics.", color = Muted)
                }
                TextButton(onClick = viewModel::clearLogs, enabled = state.logs.isNotEmpty()) { Text("Clear") }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (state.logs.isEmpty()) {
            item("logs-empty") { Text("No launcher events in this session.", color = Muted) }
        } else {
            items(state.logs.takeLast(80).asReversed(), key = { it.id }) { entry ->
                LogRow(entry)
                HorizontalDivider(color = Rule)
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
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
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.width(68.dp)) {
                Text(formatUtcTime(entry.timestampEpochMillis), color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(entry.level.name, color = if (entry.level.name == "ERROR") ErrorText else Muted)
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
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
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
private fun PropertyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, modifier = Modifier.width(92.dp), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color = Rule)
}

@Composable
private fun PageHeader(title: String, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMedium,
        )
        action()
    }
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
