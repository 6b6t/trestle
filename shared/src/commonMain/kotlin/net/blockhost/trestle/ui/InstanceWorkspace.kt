@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.ServerStatus
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ui_add_server
import net.blockhost.trestle.resources.ui_address
import net.blockhost.trestle.resources.ui_back
import net.blockhost.trestle.resources.ui_back_to_library
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_change
import net.blockhost.trestle.resources.ui_clear
import net.blockhost.trestle.resources.ui_color_warnings_and_errors
import net.blockhost.trestle.resources.ui_content
import net.blockhost.trestle.resources.ui_copy_seed
import net.blockhost.trestle.resources.ui_crash_report_value
import net.blockhost.trestle.resources.ui_edit_instance_settings
import net.blockhost.trestle.resources.ui_file_name
import net.blockhost.trestle.resources.ui_find_in_log
import net.blockhost.trestle.resources.ui_follow_launch
import net.blockhost.trestle.resources.ui_for_example_play_example_net_25565
import net.blockhost.trestle.resources.ui_game_console
import net.blockhost.trestle.resources.ui_game_files
import net.blockhost.trestle.resources.ui_homepage
import net.blockhost.trestle.resources.ui_import_world
import net.blockhost.trestle.resources.ui_inspect_launch_plan
import net.blockhost.trestle.resources.ui_installed_content
import net.blockhost.trestle.resources.ui_instance
import net.blockhost.trestle.resources.ui_instance_logs
import net.blockhost.trestle.resources.ui_instance_settings
import net.blockhost.trestle.resources.ui_instance_version_loader
import net.blockhost.trestle.resources.ui_join
import net.blockhost.trestle.resources.ui_launch_plan
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_logs
import net.blockhost.trestle.resources.ui_logs_crash_reports_screenshots_and_more
import net.blockhost.trestle.resources.ui_more
import net.blockhost.trestle.resources.ui_name
import net.blockhost.trestle.resources.ui_no_instance_selected
import net.blockhost.trestle.resources.ui_notes
import net.blockhost.trestle.resources.ui_open
import net.blockhost.trestle.resources.ui_open_log_file
import net.blockhost.trestle.resources.ui_overview
import net.blockhost.trestle.resources.ui_play
import net.blockhost.trestle.resources.ui_reading_installed_content
import net.blockhost.trestle.resources.ui_reading_the_game_directory
import net.blockhost.trestle.resources.ui_refresh
import net.blockhost.trestle.resources.ui_reload
import net.blockhost.trestle.resources.ui_remove
import net.blockhost.trestle.resources.ui_rename
import net.blockhost.trestle.resources.ui_rename_screenshot
import net.blockhost.trestle.resources.ui_rename_world
import net.blockhost.trestle.resources.ui_restore_copy
import net.blockhost.trestle.resources.ui_return_to_library
import net.blockhost.trestle.resources.ui_save_notes
import net.blockhost.trestle.resources.ui_screenshots
import net.blockhost.trestle.resources.ui_seed_value
import net.blockhost.trestle.resources.ui_server_status
import net.blockhost.trestle.resources.ui_servers
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_update
import net.blockhost.trestle.resources.ui_version_components
import net.blockhost.trestle.resources.ui_world_name
import net.blockhost.trestle.resources.ui_worlds
import net.blockhost.trestle.resources.ui_wrap_lines
import net.blockhost.trestle.resources.ui_write_notes_for_instance
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
internal fun InstanceWorkspace(
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
            if (navigator.canNavigateBack()) {
                navigator.navigateBack()
            } else {
                navigator.navigateTo(ListDetailPaneScaffoldRole.List)
            }
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
                                onChangeComponents = { selectSection(InstanceSection.SETTINGS) },
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
        if (view == GameDataView.WORLDS) {
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
        }
        if (view == GameDataView.SERVERS) {
            item("servers") {
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
                                        server.pingMillis?.let { "$it ms" },
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
        }
        if (view == GameDataView.WORLDS) {
            item("backups") {
                GameDataSection("Backups", if (state.gameData.backups.isEmpty()) "No world backups yet." else null) {
                    state.gameData.backups.forEach { backup ->
                        ListItem(
                            headlineContent = { Text(backup.fileName) },
                            supportingContent = { Text(formatFileSize(backup.sizeBytes)) },
                            trailingContent = {
                                TextButton(onClick = {
                                    actions.restoreWorldBackup(backup.key)
                                }) { Text(stringResource(Res.string.ui_restore_copy)) }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
        if (view == GameDataView.SCREENSHOTS) {
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
                                        enabled = supportsOpenPath,
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
private fun InstanceNotes(instance: GameInstance, actions: LauncherUiActions, modifier: Modifier) {
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
private fun InstanceLogs(state: LauncherUiState, instance: GameInstance, actions: LauncherUiActions, modifier: Modifier) {
    var query by rememberSaveable(instance.id) { mutableStateOf("") }
    var followLaunch by rememberSaveable(instance.id) { mutableStateOf(true) }
    var wrapLines by rememberSaveable(instance.id) { mutableStateOf(true) }
    var colorLines by rememberSaveable(instance.id) { mutableStateOf(true) }
    val copyText = rememberCopyText()
    val openPath = rememberOpenPath()
    val outputScroll = rememberScrollState()
    val selectedKey = state.selectedInstanceLogKey
    val selectedLogPath = state.gameData.logs.firstOrNull { it.key == selectedKey }?.path
    val openLogFileLabel = stringResource(Res.string.ui_open_log_file)
    val launchActive = state.activeLaunch?.status.let { it == LaunchStatus.Starting || it is LaunchStatus.Running }
    LaunchedEffect(state.gameData.logs, selectedKey) {
        if (selectedKey == null) state.gameData.logs.firstOrNull()?.let { actions.selectInstanceLog(it.key) }
    }
    val streamedLog = selectedKey?.endsWith(".trestle/logs/latest.log") == true && state.gameLogLines.isNotEmpty()
    val rawText = if (followLaunch && streamedLog) state.gameLogLines.joinToString("\n") else state.selectedInstanceLogText
    val visibleText = if (query.isBlank()) {
        rawText
    } else {
        rawText.lineSequence()
            .filter { it.contains(query, ignoreCase = true) }
            .joinToString("\n")
    }
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
                        if (supportsOpenPath && selectedLogPath != null) {
                            add(openLogFileLabel to { openPath(selectedLogPath) })
                        }
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
internal fun ServerEditorDialog(state: LauncherUiState, actions: LauncherUiActions) {
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
private fun InstanceWorkspaceHeader(state: LauncherUiState, instance: GameInstance, actions: LauncherUiActions, onBack: () -> Unit) {
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
            PrimaryInstanceAction(instance, state, actions, Modifier.widthIn(min = 132.dp, max = 196.dp))
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
    onChangeComponents: () -> Unit,
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
                    PrimaryInstanceAction(instance, state, actions, Modifier.fillMaxWidth())
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
                if (currentPlatform == "Android") {
                    PropertyRow(
                        stringResource(Res.string.ui_game_files),
                        stringResource(Res.string.ui_logs_crash_reports_screenshots_and_more),
                        actionLabel = stringResource(Res.string.ui_open),
                        onClick = { openPath("${instance.instanceDirectory}/game") },
                        actionEnabled = supportsOpenPath,
                    )
                } else {
                    PropertyRow(
                        "Directory",
                        instance.instanceDirectory,
                        actionLabel = "Open",
                        onClick = { openPath(instance.instanceDirectory) },
                        actionEnabled = supportsOpenPath,
                    )
                }
                PropertyRow(
                    "Last launch",
                    instance.lastLaunchAtEpochMillis?.let(::formatLocalDateTime) ?: "Never",
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.ui_version_components),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onChangeComponents,
                        modifier = Modifier.testTag(LauncherTestTags.VERSION_COMPONENTS_CHANGE),
                    ) { Text(stringResource(Res.string.ui_change)) }
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
                        Text(
                            stringResource(Res.string.ui_launch_plan),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
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
                        Text(
                            stringResource(Res.string.ui_game_console),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
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
                                enabled = supportsOpenPath,
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
            item("modpack-origin") {
                ModpackOriginPanel(instance, state, actions)
            }
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
private fun InstalledContentPanel(state: LauncherUiState, instance: GameInstance, actions: LauncherUiActions) {
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
                    add(
                        "Copy installed list" to {
                            copyText(
                                state.installedContent.filter { it.direct }
                                    .joinToString("\n") { content ->
                                        listOfNotNull(content.name, content.versionNumber).joinToString(" ")
                                    },
                            )
                        },
                    )
                    if (supportsOpenPath) {
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
private fun InstalledContentRow(content: InstalledContent, update: ResourceVersion?, actions: LauncherUiActions) {
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
                val role = if (content.direct) {
                    source
                } else {
                    "Dependency${if (content.requiredByCount > 1) " for ${content.requiredByCount} items" else ""}"
                }
                val details = buildList {
                    add(content.type.label.removeSuffix("s"))
                    content.versionNumber?.let(::add)
                    if (content.authors.isNotEmpty()) add(content.authors.joinToString())
                    if (content.dependencies.isNotEmpty()) add("Requires ${content.dependencies.joinToString()}")
                    if (content.gameVersions.isNotEmpty()) add("Minecraft ${content.gameVersions.joinToString()}")
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
                        if (content.iconUrl != null) {
                            coil3.compose.AsyncImage(model = content.iconUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(content.type.label.take(1), style = MaterialTheme.typography.titleMedium)
                        }
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
private fun InstanceConfiguration(instance: GameInstance, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) {
    Column(
        modifier
            .verticalScroll(scrollState)
            .padding(24.dp)
            .testTag(LauncherTestTags.INSTANCE_CONFIGURATION),
    ) {
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
            PropertyRow(
                "Environment",
                if (instance.environmentVariables.isEmpty()) "Inherited" else "${instance.environmentVariables.size} custom",
            )
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
