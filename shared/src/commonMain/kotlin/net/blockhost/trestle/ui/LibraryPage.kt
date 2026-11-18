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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
import net.blockhost.trestle.resources.ic_download
import net.blockhost.trestle.resources.ic_play
import net.blockhost.trestle.resources.trestle_mark
import net.blockhost.trestle.resources.ui_accounts
import net.blockhost.trestle.resources.ui_add_an_account_to_launch
import net.blockhost.trestle.resources.ui_add_content
import net.blockhost.trestle.resources.ui_add_mods
import net.blockhost.trestle.resources.ui_all_instances
import net.blockhost.trestle.resources.ui_checking
import net.blockhost.trestle.resources.ui_checking_launch_requirements
import net.blockhost.trestle.resources.ui_choose_one_from_the_library
import net.blockhost.trestle.resources.ui_copy_directory
import net.blockhost.trestle.resources.ui_copy_instance_details
import net.blockhost.trestle.resources.ui_create_an_isolated_minecraft_instance
import net.blockhost.trestle.resources.ui_create_instance
import net.blockhost.trestle.resources.ui_drop_to_import
import net.blockhost.trestle.resources.ui_duplicate_instance
import net.blockhost.trestle.resources.ui_export_instance
import net.blockhost.trestle.resources.ui_import
import net.blockhost.trestle.resources.ui_import_hint
import net.blockhost.trestle.resources.ui_import_hint_touch
import net.blockhost.trestle.resources.ui_inspect_launch_plan
import net.blockhost.trestle.resources.ui_install
import net.blockhost.trestle.resources.ui_instance_settings
import net.blockhost.trestle.resources.ui_java_account
import net.blockhost.trestle.resources.ui_last_played_on
import net.blockhost.trestle.resources.ui_launch
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_manage
import net.blockhost.trestle.resources.ui_manage_content
import net.blockhost.trestle.resources.ui_minecraft_is_running
import net.blockhost.trestle.resources.ui_minecraft_is_running_process_s
import net.blockhost.trestle.resources.ui_minecraft_s_s
import net.blockhost.trestle.resources.ui_move_to_trash
import net.blockhost.trestle.resources.ui_named_is_still_running
import net.blockhost.trestle.resources.ui_never_played
import net.blockhost.trestle.resources.ui_new_instance
import net.blockhost.trestle.resources.ui_no_account_to_play
import net.blockhost.trestle.resources.ui_no_instances_yet
import net.blockhost.trestle.resources.ui_no_matching_instances
import net.blockhost.trestle.resources.ui_open_game_folder
import net.blockhost.trestle.resources.ui_open_instance_folder
import net.blockhost.trestle.resources.ui_open_mods
import net.blockhost.trestle.resources.ui_open_named
import net.blockhost.trestle.resources.ui_open_operation_details
import net.blockhost.trestle.resources.ui_open_screenshots
import net.blockhost.trestle.resources.ui_pause
import net.blockhost.trestle.resources.ui_pause_installation
import net.blockhost.trestle.resources.ui_pin_to_top
import net.blockhost.trestle.resources.ui_pinned
import net.blockhost.trestle.resources.ui_play
import net.blockhost.trestle.resources.ui_play_named
import net.blockhost.trestle.resources.ui_playing_as
import net.blockhost.trestle.resources.ui_remove_from_library
import net.blockhost.trestle.resources.ui_required_before_launch_s
import net.blockhost.trestle.resources.ui_resume_install
import net.blockhost.trestle.resources.ui_resume_installation
import net.blockhost.trestle.resources.ui_retry
import net.blockhost.trestle.resources.ui_retry_install
import net.blockhost.trestle.resources.ui_retry_installation
import net.blockhost.trestle.resources.ui_retry_launch
import net.blockhost.trestle.resources.ui_running
import net.blockhost.trestle.resources.ui_s_of_s
import net.blockhost.trestle.resources.ui_s_of_s_files
import net.blockhost.trestle.resources.ui_s_of_s_files_saved_ready_to
import net.blockhost.trestle.resources.ui_search_instances
import net.blockhost.trestle.resources.ui_select_an_instance
import net.blockhost.trestle.resources.ui_sign_in_required_for
import net.blockhost.trestle.resources.ui_starting
import net.blockhost.trestle.resources.ui_starting_minecraft
import net.blockhost.trestle.resources.ui_stop
import net.blockhost.trestle.resources.ui_try_another_name_version_or_loader
import net.blockhost.trestle.resources.ui_unavailable
import net.blockhost.trestle.resources.ui_unpin_from_top
import net.blockhost.trestle.runtime.LaunchRequirement
import net.blockhost.trestle.text.formattedString
import net.blockhost.trestle.text.resolve
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
    onOpenAccounts: () -> Unit,
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
    val gridState = rememberLazyGridState()
    val filteredInstances = state.instances.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) ||
            it.minecraftVersionId.contains(query, ignoreCase = true) || it.modLoader.label.contains(query, ignoreCase = true)
    }
    // Requests are counters owned by the app shell. Remember which ones this page handled,
    // so returning to the library does not reopen the picker or steal focus again.
    var handledSearchFocusRequest by rememberSaveable { mutableIntStateOf(0) }
    var handledImportRequest by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > handledSearchFocusRequest) {
            handledSearchFocusRequest = searchFocusRequest
            searchFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(importRequest) {
        if (importRequest > handledImportRequest) {
            handledImportRequest = importRequest
            importPicker.launch()
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LibraryHeader(
                compact = compact,
                hasInstances = state.instances.isNotEmpty(),
                query = query,
                onQueryChange = { query = it },
                searchFocusRequester = searchFocusRequester,
                onImport = { importPicker.launch() },
                onCreate = actions::openCreate,
            )
        },
        floatingActionButton = {
            if (compact && state.instances.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = actions::openCreate,
                    icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(Res.string.ui_new_instance)) },
                    modifier = Modifier.testTag(LauncherTestTags.NEW_INSTANCE),
                )
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
                    onFiles = { files -> files.forEach { actions.queueLocalFileImport(it.name, it.bytes) } },
                    onFailure = actions::reportLocalFileReadFailure,
                ),
        ) {
            when {
                state.isInitializing -> LoadingRows(Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY))

                state.instances.isEmpty() -> EmptyLibrary(
                    onNew = actions::openCreate,
                    onImport = { importPicker.launch() },
                    modifier = Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY),
                )

                else -> InstanceCollection(
                    instances = filteredInstances,
                    showFeatured = query.isBlank(),
                    state = state,
                    actions = actions,
                    compact = compact,
                    gridState = gridState,
                    onOpenInstance = onManage,
                    onDiscover = onDiscover,
                    onOpenAccounts = onOpenAccounts,
                    modifier = Modifier.fillMaxSize().testTag(LauncherTestTags.LIBRARY),
                )
            }
            if (dropActive) {
                DropOverlay(stringResource(Res.string.ui_drop_to_import))
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    compact: Boolean,
    hasInstances: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    onImport: () -> Unit,
    onCreate: () -> Unit,
) {
    val search: @Composable (Modifier) -> Unit = { searchModifier ->
        TrestleSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(Res.string.ui_search_instances)) },
            modifier = searchModifier.focusRequester(searchFocusRequester).testTag(LauncherTestTags.INSTANCE_SEARCH),
        )
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.ui_library),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (!compact && hasInstances) search(Modifier.width(320.dp))
            // The empty library shows its own create and import actions.
            if (hasInstances && compact) {
                TextButton(onClick = onImport) { Text(stringResource(Res.string.ui_import)) }
            } else if (hasInstances) {
                OutlinedButton(onClick = onImport) { Text(stringResource(Res.string.ui_import)) }
                Button(onClick = onCreate, modifier = Modifier.testTag(LauncherTestTags.NEW_INSTANCE)) {
                    Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.ui_new_instance))
                }
            }
        }
        if (compact && hasInstances) search(Modifier.fillMaxWidth())
    }
}

@Composable
private fun InstanceCollection(
    instances: List<GameInstance>,
    showFeatured: Boolean,
    state: LauncherUiState,
    actions: LauncherUiActions,
    compact: Boolean,
    gridState: LazyGridState,
    onOpenInstance: () -> Unit,
    onDiscover: (ResourceType) -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier,
) {
    val featured = state.selectedInstance?.takeIf { showFeatured }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(280.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, top = 4.dp, end = 24.dp, bottom = if (compact) 96.dp else 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (featured != null) {
            item(key = "featured", span = { GridItemSpan(maxLineSpan) }) {
                FeaturedInstance(
                    instance = featured,
                    state = state,
                    actions = actions,
                    compact = compact,
                    onManage = onOpenInstance,
                    onAddMods = { onDiscover(ResourceType.MOD) },
                    onOpenAccounts = onOpenAccounts,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
        if (instances.isEmpty()) {
            item(key = "no-match", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(Res.string.ui_no_matching_instances), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.ui_try_another_name_version_or_loader),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        instanceSections(instances, state).forEach { section ->
            item(key = section.key, span = { GridItemSpan(maxLineSpan) }) { LibrarySectionHeader(section) }
            gridItems(section.instances, key = { it.id.value }) { instance ->
                InstanceTile(
                    instance = instance,
                    selected = instance.id == state.selectedInstance?.id,
                    launcherState = state,
                    actions = actions,
                    onOpen = onOpenInstance,
                    onDiscover = onDiscover,
                )
            }
        }
    }
}

private const val PINNED_SECTION_KEY = "pinned"

@Composable
private fun LibrarySectionHeader(section: LibrarySection) {
    Text(
        section.title ?: if (section.key == PINNED_SECTION_KEY) {
            stringResource(Res.string.ui_pinned)
        } else {
            stringResource(Res.string.ui_all_instances)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall,
    )
}

private class LibrarySection(val key: String, val title: String?, val instances: List<GameInstance>)

/**
 * Splits instances into the sections shown in the library.
 * Only pinned instances and groups the player named get their own section. Pinned and ungrouped sections have a
 * null title and are labeled when drawn.
 * Keys carry the section kind, so a group named "Pinned" cannot collide with the pinned section.
 */
private fun instanceSections(instances: List<GameInstance>, state: LauncherUiState): List<LibrarySection> {
    val sorted = sortInstances(instances, state)
    val pinned = sorted.filter { it.pinned }
    val rest = sorted.filterNot { it.pinned }
    val named = rest.filter { !it.group.isNullOrBlank() }.groupBy { it.group.orEmpty().trim() }
    val ungrouped = rest.filter { it.group.isNullOrBlank() }
    return buildList {
        if (pinned.isNotEmpty()) add(LibrarySection(PINNED_SECTION_KEY, null, pinned))
        named.entries.sortedBy { it.key.lowercase() }.forEach { (group, members) ->
            add(LibrarySection("group:$group", group, members))
        }
        if (ungrouped.isNotEmpty()) add(LibrarySection("rest", null, ungrouped))
    }
}

@Composable
private fun FeaturedInstance(
    instance: GameInstance,
    state: LauncherUiState,
    actions: LauncherUiActions,
    compact: Boolean,
    onManage: () -> Unit,
    onAddMods: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().testTag(LauncherTestTags.SELECTED_INSTANCE),
    ) {
        val accent = instanceAccent(instance)
        // The accent glows from the start edge, which is the right edge in right-to-left languages.
        val glow = listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.04f), Color.Transparent)
        val backdrop = Modifier.background(
            Brush.horizontalGradient(if (LocalLayoutDirection.current == LayoutDirection.Rtl) glow.reversed() else glow),
        )
        if (compact) {
            Column(backdrop.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InstanceArtwork(instance, 72.dp)
                    FeaturedInstanceDetails(instance, state, Modifier.weight(1f))
                }
                FeaturedInstanceControls(instance, state, actions, onManage, onAddMods, onOpenAccounts, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                backdrop.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InstanceArtwork(instance, 112.dp)
                FeaturedInstanceDetails(instance, state, Modifier.weight(1f))
                FeaturedInstanceControls(instance, state, actions, onManage, onAddMods, onOpenAccounts, Modifier.width(280.dp))
            }
        }
    }
}

@Composable
private fun FeaturedInstanceDetails(instance: GameInstance, state: LauncherUiState, modifier: Modifier) {
    val installationState = instance.installationState
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            instance.displayName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            instanceSubtitle(instance),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (installationState is InstallationState.Installed) {
            Text(
                instance.lastLaunchAtEpochMillis?.let {
                    formattedString(Res.string.ui_last_played_on, formatLocalDateTime(it))
                } ?: stringResource(Res.string.ui_never_played),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            InstallationStatusLine(instance, state)
        }
        InstallationProgress(installationState, installationState.installationProgress())
        LaunchReadiness(state, instance)
    }
}

@Composable
private fun FeaturedInstanceControls(
    instance: GameInstance,
    state: LauncherUiState,
    actions: LauncherUiActions,
    onManage: () -> Unit,
    onAddMods: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryInstanceAction(
            instance,
            state,
            actions,
            Modifier.fillMaxWidth(),
            buttonModifier = Modifier.heightIn(min = 52.dp),
            showAccountRequirement = false,
        )
        LaunchAccount(state, instance, onOpenAccounts)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onManage, modifier = Modifier.weight(1f)) {
                ButtonLabel(stringResource(Res.string.ui_manage))
            }
            if (instance.modLoader != ModLoader.VANILLA) {
                FilledTonalButton(
                    onClick = onAddMods,
                    enabled = instance.installationState is InstallationState.Installed,
                    modifier = Modifier.weight(1f),
                ) {
                    ButtonLabel(stringResource(Res.string.ui_add_mods))
                }
            }
        }
    }
}

@Composable
private fun LaunchAccount(state: LauncherUiState, instance: GameInstance, onOpenAccounts: () -> Unit) {
    val account = instance.accountProfileId
        ?.let { profileId -> state.accounts.firstOrNull { it.profile.profileId == profileId } }
        ?: state.accounts.firstOrNull { it.isActive }
    val text = when {
        account == null -> stringResource(Res.string.ui_no_account_to_play)
        !account.isReady -> formattedString(Res.string.ui_sign_in_required_for, account.profile.playerName)
        else -> formattedString(Res.string.ui_playing_as, account.profile.playerName)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = stringResource(Res.string.ui_accounts), role = Role.Button, onClick = onOpenAccounts)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(22.dp).background(
                if (account?.isReady == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                account?.profile?.playerName?.take(1)?.uppercase() ?: "!",
                style = MaterialTheme.typography.labelSmall,
                color = if (account?.isReady == true) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun instanceSubtitle(instance: GameInstance): String =
    formattedString(Res.string.ui_minecraft_s_s, instance.minecraftVersionId, instance.modLoader.label)

@Composable
private fun InstallationStatusLine(instance: GameInstance, state: LauncherUiState) {
    val installationState = instance.installationState
    val running = state.activeLaunch?.instanceId == instance.id && state.activeLaunch.status is LaunchStatus.Running
    val label = if (running) stringResource(Res.string.ui_running) else stateLabel(installationState)
    val color = if (running) MaterialTheme.colorScheme.primary else stateColor(installationState)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun InstanceTile(
    instance: GameInstance,
    selected: Boolean,
    launcherState: LauncherUiState,
    actions: LauncherUiActions,
    onOpen: () -> Unit,
    onDiscover: (ResourceType) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val installationState = instance.installationState
    val progress = installationState.installationProgress()
    val interactionModifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(
            onClickLabel = formattedString(Res.string.ui_open_named, instance.displayName),
            role = Role.Button,
            onClick = {
                actions.selectInstance(instance.id)
                onOpen()
            },
        )
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Enter -> {
                    actions.selectInstance(instance.id)
                    onOpen()
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
    ContextActionArea(instanceContextActions(instance, actions, onDiscover)) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = interactionModifier,
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InstanceArtwork(instance, 52.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            instance.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${instance.minecraftVersionId} · ${instance.modLoader.label}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        InstallationStatusLine(instance, launcherState)
                    }
                    TileQuickAction(instance, launcherState, actions)
                }
                if (progress != null) InstallationProgress(installationState, progress)
            }
        }
    }
}

@Composable
private fun TileQuickAction(instance: GameInstance, state: LauncherUiState, actions: LauncherUiActions) {
    if (!showsTileQuickAction(instance, state)) return
    val installationState = instance.installationState
    val installed = installationState is InstallationState.Installed
    val label = when (installationState) {
        is InstallationState.Installed -> formattedString(Res.string.ui_play_named, instance.displayName)
        is InstallationState.Interrupted -> stringResource(Res.string.ui_resume_install)
        is InstallationState.Failed -> stringResource(Res.string.ui_retry_install)
        else -> stringResource(Res.string.ui_install)
    }
    TrestleTooltip(label) {
        FilledIconButton(
            onClick = {
                actions.selectInstance(instance.id)
                if (installed) actions.launchSelected() else actions.installSelected()
            },
            colors = if (installed) {
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.filledTonalIconButtonColors()
            },
            modifier = Modifier.size(44.dp).testTag(LauncherTestTags.instancePlay(instance.id)),
        ) {
            Icon(
                painterResource(if (installed) Res.drawable.ic_play else Res.drawable.ic_download),
                contentDescription = label,
            )
        }
    }
}

@Composable
private fun EmptyLibrary(onNew: () -> Unit, onImport: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(Res.drawable.trestle_mark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.ui_no_instances_yet), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.ui_create_an_isolated_minecraft_instance),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNew, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.ui_create_instance))
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.ui_import))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (currentPlatform == "Desktop") Res.string.ui_import_hint else Res.string.ui_import_hint_touch),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
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
                    Text(status.title.resolve(), style = MaterialTheme.typography.labelLarge)
                    val progressDetail = buildList {
                        status.detail?.let { add(it.resolve()) }
                        if (status.completed != null && status.total != null) {
                            add(formattedString(Res.string.ui_s_of_s, formatFileSize(status.completed), formatFileSize(status.total)))
                        }
                        if (status.completedItems != null && status.totalItems != null) {
                            add(formattedString(Res.string.ui_s_of_s_files, status.completedItems, status.totalItems))
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
                if (status.cancellable) TextButton(onClick = onCancel) { Text(status.cancelLabel.resolve()) }
            }
        }
    }
}

@Composable
private fun LaunchReadiness(state: LauncherUiState, instance: GameInstance) {
    val running = otherRunningInstance(state, instance)
    if (running != null) {
        Text(
            formattedString(Res.string.ui_named_is_still_running, running.displayName),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    } else {
        LaunchStatusMessage(state, instance)
    }
}

@Composable
private fun LaunchStatusMessage(state: LauncherUiState, instance: GameInstance) {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status ?: return
    val message = launchStatusMessage(state, instance, status) ?: return
    val color = when (status) {
        is LaunchStatus.Blocked, is LaunchStatus.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(message, color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun launchStatusMessage(state: LauncherUiState, instance: GameInstance, status: LaunchStatus): String? = when (status) {
    is LaunchStatus.Blocked -> if (requiresImportedAccount(state, instance)) {
        null
    } else {
        formattedString(Res.string.ui_required_before_launch_s, status.missingRequirements.map { it.label() }.joinToString())
    }

    is LaunchStatus.Failed -> status.message.resolve()

    is LaunchStatus.Unavailable -> status.reason.resolve()

    LaunchStatus.Checking -> stringResource(Res.string.ui_checking_launch_requirements)

    LaunchStatus.Starting -> stringResource(Res.string.ui_starting_minecraft)

    is LaunchStatus.Running -> status.processId?.let { formattedString(Res.string.ui_minecraft_is_running_process_s, it) }
        ?: stringResource(Res.string.ui_minecraft_is_running)

    else -> null
}

@Composable
internal fun PrimaryInstanceAction(
    instance: GameInstance,
    state: LauncherUiState,
    actions: LauncherUiActions,
    modifier: Modifier,
    buttonModifier: Modifier = Modifier,
    showAccountRequirement: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PrimaryInstanceButton(instance, state, actions, buttonModifier.fillMaxWidth())
        if (showAccountRequirement && requiresImportedAccount(state, instance)) {
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

/** Tiles hide their quick action while the instance installs, launches, or while any game is running. */
private fun showsTileQuickAction(instance: GameInstance, state: LauncherUiState): Boolean {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status
    val launching = status is LaunchStatus.Running || status == LaunchStatus.Starting || status == LaunchStatus.Checking
    val installed = instance.installationState is InstallationState.Installed
    val gameRunning = state.activeLaunch?.instanceId == instance.id || otherRunningInstance(state, instance) != null
    return instance.installationState !is InstallationState.Installing && !launching && !(installed && gameRunning)
}

/** Returns the instance that is already running when it is not [instance]; only one game runs at a time. */
internal fun otherRunningInstance(state: LauncherUiState, instance: GameInstance): GameInstance? = state.activeLaunch
    ?.takeIf { it.instanceId != instance.id }
    ?.takeIf { it.status is LaunchStatus.Running || it.status == LaunchStatus.Starting }
    ?.let { state.activeInstance }

private fun requiresImportedAccount(state: LauncherUiState, instance: GameInstance): Boolean {
    if (state.accounts.isNotEmpty()) return false
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status
    return status is LaunchStatus.Blocked && LaunchRequirement.JAVA_ACCOUNT in status.missingRequirements
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
            formattedString(Res.string.ui_s_of_s_files_saved_ready_to, progress.completedFiles, progress.totalFiles)
        } else {
            formattedString(Res.string.ui_s_of_s_files, progress.completedFiles, progress.totalFiles)
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

@Composable
internal fun InstanceArtwork(instance: GameInstance, size: androidx.compose.ui.unit.Dp) {
    InstanceIconArtwork(instance = instance, size = size)
}

@Composable
private fun instanceContextActions(
    instance: GameInstance,
    actions: LauncherUiActions,
    onDiscover: (ResourceType) -> Unit,
): List<ContextAction> {
    val copyText = rememberCopyText()
    val openPath = rememberOpenPath()
    val state = instance.installationState
    val selectedAction: (() -> Unit) -> Unit = { action ->
        actions.selectInstance(instance.id)
        action()
    }
    val primaryAction = when (state) {
        is InstallationState.Installing -> ContextAction(stringResource(Res.string.ui_pause_installation)) {
            selectedAction(actions::cancelInstall)
        }

        is InstallationState.Interrupted -> ContextAction(stringResource(Res.string.ui_resume_installation)) {
            selectedAction(actions::installSelected)
        }

        is InstallationState.Installed -> ContextAction(stringResource(Res.string.ui_play)) {
            selectedAction(actions::launchSelected)
        }

        is InstallationState.Failed -> ContextAction(stringResource(Res.string.ui_retry_installation)) {
            selectedAction(actions::installSelected)
        }

        InstallationState.NotInstalled -> ContextAction(stringResource(Res.string.ui_install)) {
            selectedAction(actions::installSelected)
        }
    }
    return buildList {
        add(primaryAction)
        if (state is InstallationState.Installed) {
            add(ContextAction(stringResource(Res.string.ui_inspect_launch_plan)) { selectedAction(actions::inspectLaunchPlan) })
        }
        if (state is InstallationState.Installed) {
            add(ContextAction(stringResource(Res.string.ui_add_content)) { selectedAction { onDiscover(ResourceType.MOD) } })
        }
        add(
            ContextAction(stringResource(Res.string.ui_instance_settings), separatorBefore = true) {
                selectedAction(actions::openInstanceSettings)
            },
        )
        add(ContextAction(stringResource(Res.string.ui_duplicate_instance)) { selectedAction(actions::cloneSelectedInstance) })
        add(ContextAction(stringResource(Res.string.ui_export_instance)) { selectedAction(actions::exportSelectedInstance) })
        add(
            ContextAction(if (instance.pinned) stringResource(Res.string.ui_unpin_from_top) else stringResource(Res.string.ui_pin_to_top)) {
                selectedAction(actions::toggleSelectedInstancePinned)
            },
        )
        if (supportsOpenPath) addAll(instanceFolderActions(instance, openPath))
        add(ContextAction(stringResource(Res.string.ui_copy_directory)) { copyText(instance.instanceDirectory) })
        add(ContextAction(stringResource(Res.string.ui_copy_instance_details)) { copyText(formatInstanceForClipboard(instance)) })
        add(
            ContextAction(stringResource(Res.string.ui_remove_from_library), separatorBefore = true) {
                selectedAction(actions::deleteSelected)
            },
        )
        if (supportsPathTrash) {
            add(ContextAction(stringResource(Res.string.ui_move_to_trash)) { selectedAction(actions::moveSelectedToTrash) })
        }
    }
}

@Composable
private fun instanceFolderActions(instance: GameInstance, openPath: (String) -> Unit): List<ContextAction> = buildList {
    val state = instance.installationState
    add(
        ContextAction(stringResource(Res.string.ui_open_instance_folder), separatorBefore = true) {
            openPath(instance.instanceDirectory)
        },
    )
    if (state is InstallationState.Installed) {
        add(ContextAction(stringResource(Res.string.ui_open_game_folder)) { openPath("${instance.instanceDirectory}/game") })
        add(
            ContextAction(stringResource(Res.string.ui_open_screenshots)) {
                openPath("${instance.instanceDirectory}/game/screenshots")
            },
        )
        if (instance.modLoader != ModLoader.VANILLA) {
            add(ContextAction(stringResource(Res.string.ui_open_mods)) { openPath("${instance.instanceDirectory}/game/mods") })
        }
    }
}

@Composable
private fun LaunchButton(state: LauncherUiState, instance: GameInstance, actions: LauncherUiActions, modifier: Modifier = Modifier) {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status ?: LaunchStatus.NotChecked
    if (otherRunningInstance(state, instance) != null) {
        Button(onClick = {}, enabled = false, modifier = modifier) { Text(stringResource(Res.string.ui_play)) }
        return
    }
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
        ) { Text(if (status == LaunchStatus.Checking) stringResource(Res.string.ui_checking) else stringResource(Res.string.ui_starting)) }

        is LaunchStatus.Blocked -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
        ) { Text(stringResource(Res.string.ui_play)) }

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
        ) {
            Icon(painterResource(Res.drawable.ic_play), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.ui_play))
        }
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

/** The name of something the player must provide before launch. */
@Composable
internal fun LaunchRequirement.label(): String = when (this) {
    LaunchRequirement.JAVA_ACCOUNT -> stringResource(Res.string.ui_java_account)
}
