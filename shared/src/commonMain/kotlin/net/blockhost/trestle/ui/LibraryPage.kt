@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.InstanceSortMode
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ic_add
import net.blockhost.trestle.resources.ui_add_an_account_to_launch
import net.blockhost.trestle.resources.ui_choose_one_from_the_library
import net.blockhost.trestle.resources.ui_create_an_isolated_minecraft_instance
import net.blockhost.trestle.resources.ui_create_instance
import net.blockhost.trestle.resources.ui_import
import net.blockhost.trestle.resources.ui_install
import net.blockhost.trestle.resources.ui_instance_settings
import net.blockhost.trestle.resources.ui_launch
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_manage_content
import net.blockhost.trestle.resources.ui_new_instance
import net.blockhost.trestle.resources.ui_no_instances_yet
import net.blockhost.trestle.resources.ui_no_matching_instances
import net.blockhost.trestle.resources.ui_open_operation_details
import net.blockhost.trestle.resources.ui_pause
import net.blockhost.trestle.resources.ui_resume_install
import net.blockhost.trestle.resources.ui_retry
import net.blockhost.trestle.resources.ui_retry_install
import net.blockhost.trestle.resources.ui_retry_launch
import net.blockhost.trestle.resources.ui_search_instances
import net.blockhost.trestle.resources.ui_select_an_instance
import net.blockhost.trestle.resources.ui_stop
import net.blockhost.trestle.resources.ui_try_another_name_version_or_loader
import net.blockhost.trestle.resources.ui_unavailable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
internal fun LibraryPage(
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
internal fun BoxScope.DropOverlay(message: String) {
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
private fun InstanceShelfToolbar(query: String, onQueryChange: (String) -> Unit, searchFocusRequester: FocusRequester) {
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
internal fun InlineMessage(message: String, error: Boolean, onRetry: (() -> Unit)?) {
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
internal fun OperationBar(status: OperationStatus, onCancel: () -> Unit, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
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
            PrimaryInstanceAction(instance, state, actions, Modifier.fillMaxWidth())
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
        is LaunchStatus.Blocked -> if (requiresImportedAccount(state, instance)) {
            null
        } else {
            "Required before launch: ${status.missingRequirements.joinToString()}"
        }

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
internal fun PrimaryInstanceAction(instance: GameInstance, state: LauncherUiState, actions: LauncherUiActions, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PrimaryInstanceButton(instance, state, actions, Modifier.fillMaxWidth())
        if (requiresImportedAccount(state, instance)) {
            Text(
                stringResource(Res.string.ui_add_an_account_to_launch),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LauncherTestTags.LAUNCH_ACCOUNT_REQUIREMENT),
            )
        }
    }
}

private fun requiresImportedAccount(state: LauncherUiState, instance: GameInstance): Boolean {
    if (state.accounts.isNotEmpty()) return false
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status
    return status is LaunchStatus.Blocked && "Java account" in status.missingRequirements
}

@Composable
private fun PrimaryInstanceButton(instance: GameInstance, state: LauncherUiState, actions: LauncherUiActions, modifier: Modifier) {
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

internal fun Modifier.dismissOnEscape(enabled: Boolean = true, onDismiss: () -> Unit): Modifier = onPreviewKeyEvent { event ->
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
internal fun InstanceArtwork(instance: GameInstance, size: androidx.compose.ui.unit.Dp) {
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
        add(
            ContextAction(if (instance.pinned) "Unpin from top" else "Pin to top") {
                selectedAction(actions::toggleSelectedInstancePinned)
            },
        )
        if (supportsOpenPath) {
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
private fun LaunchButton(state: LauncherUiState, instance: GameInstance, actions: LauncherUiActions, modifier: Modifier = Modifier) {
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
internal fun LoadingRows(modifier: Modifier) {
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
