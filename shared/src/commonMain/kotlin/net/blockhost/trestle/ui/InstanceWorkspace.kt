@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
import net.blockhost.trestle.instance.ManagedScreenshot
import net.blockhost.trestle.instance.ManagedWorld
import net.blockhost.trestle.instance.SavedServer
import net.blockhost.trestle.instance.ServerStatus
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.ic_add
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_more_vert
import net.blockhost.trestle.resources.ic_play
import net.blockhost.trestle.resources.ui_add_mods
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
    val compact = layoutMode == TrestleLayoutMode.COMPACT
    val selectSection: (InstanceSection) -> Unit = { selectedSection ->
        sectionName = selectedSection.name
        if (selectedSection == InstanceSection.SETTINGS && !state.instanceSettings.visible) {
            actions.openInstanceSettings()
        }
    }

    Column(modifier.fillMaxSize().testTag(LauncherTestTags.INSTANCE_WORKSPACE)) {
        if (instance == null) {
            PageHeader(
                title = stringResource(Res.string.ui_instance),
                navigationIcon = { InstanceBackButton(onBack) },
            )
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
        InstanceWorkspaceHeader(state, instance, actions, onBack, compact)
        InstanceSectionTabs(section, compact, selectSection)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val contentModifier = Modifier.widthIn(max = WideContentWidth).fillMaxSize()
            when (section) {
                InstanceSection.OVERVIEW -> InstanceOverview(
                    state,
                    instance,
                    actions,
                    overviewListState,
                    onChangeComponents = { selectSection(InstanceSection.SETTINGS) },
                    onOpenSection = selectSection,
                    onDiscover = onDiscover,
                    modifier = contentModifier,
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
}

@Composable
private fun InstanceSectionTabs(section: InstanceSection, compact: Boolean, onSelect: (InstanceSection) -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        PrimaryScrollableTabRow(
            selectedTabIndex = section.ordinal,
            edgePadding = if (compact) 8.dp else 12.dp,
            containerColor = Color.Transparent,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            modifier = Modifier.widthIn(max = WideContentWidth).fillMaxWidth(),
        ) {
            InstanceSection.entries.forEach { item ->
                Tab(
                    selected = section == item,
                    onClick = { onSelect(item) },
                    text = { Text(stringResource(item.label), style = MaterialTheme.typography.titleSmall) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(LauncherTestTags.instanceSection(item.name)),
                )
            }
        }
    }
}

@Composable
private fun InstanceBackButton(onBack: () -> Unit) {
    TrestleTooltipIconButton(
        label = stringResource(Res.string.ui_back_to_library),
        onClick = onBack,
        modifier = Modifier.testTag(LauncherTestTags.INSTANCE_BACK),
    ) {
        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.ui_back_to_library))
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
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("game-data-heading") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    when (view) {
                        GameDataView.WORLDS -> "Local worlds, data packs, and backups for ${instance.displayName}."
                        GameDataView.SERVERS -> "Multiplayer servers saved in ${instance.displayName}."
                        GameDataView.SCREENSHOTS -> "Screenshots taken in ${instance.displayName}."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
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
                val worlds = state.gameData.worlds.sortedByDescending { it.lastModifiedEpochMillis ?: Long.MIN_VALUE }
                GameDataSection(
                    title = if (worlds.isEmpty()) "Worlds" else "Worlds (${worlds.size})",
                    emptyMessage = if (worlds.isEmpty()) "No worlds yet. Start the game to create one, or import a world." else null,
                    action = { WorldImportButton(actions) },
                ) {
                    worlds.forEach { world ->
                        WorldCard(
                            world = world,
                            instance = instance,
                            actions = actions,
                            onRename = {
                                worldToRename = world.key
                                worldName = world.name
                            },
                            onCopySeed = { seed -> copyText(seed.toString()) },
                        )
                    }
                }
            }
            item("backups") {
                GameDataSection(
                    title = "Backups",
                    emptyMessage = "Back up a world from its menu to keep a restorable copy."
                        .takeIf { state.gameData.backups.isEmpty() },
                ) {
                    state.gameData.backups.forEach { backup ->
                        GameDataRow(
                            title = backup.fileName,
                            details = listOfNotNull(
                                formatFileSize(backup.sizeBytes),
                                backup.createdAtEpochMillis?.let(::formatLocalDateTime),
                            ).joinToString(" · "),
                        ) {
                            FilledTonalButton(onClick = { actions.restoreWorldBackup(backup.key) }) {
                                Text(stringResource(Res.string.ui_restore_copy))
                            }
                        }
                    }
                }
            }
        }
        if (view == GameDataView.SERVERS) {
            item("servers") {
                GameDataSection(
                    title = stringResource(Res.string.ui_servers),
                    emptyMessage = if (state.gameData.servers.isEmpty()) "Add a server here or in the game's multiplayer menu." else null,
                    action = {
                        TextButton(onClick = { actions.openServerEditor() }) {
                            Icon(painterResource(Res.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add server")
                        }
                    },
                ) {
                    state.gameData.servers.forEach { server -> ServerCard(server, actions) }
                }
            }
        }
        if (view == GameDataView.SCREENSHOTS) {
            item("screenshots") {
                val screenshots = state.gameData.screenshots.sortedByDescending { it.createdAtEpochMillis ?: Long.MIN_VALUE }
                GameDataSection(
                    title = if (screenshots.isEmpty()) "Screenshots" else "Screenshots (${screenshots.size})",
                    emptyMessage = if (screenshots.isEmpty()) "Press F2 in game to take a screenshot. They appear here." else null,
                ) {
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        val columns = (maxWidth / 240.dp).toInt().coerceIn(1, 5)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            screenshots.chunked(columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { screenshot ->
                                        ScreenshotTile(
                                            screenshot = screenshot,
                                            onOpen = { openPath(screenshot.path) },
                                            menu = listOf(
                                                "Copy file path" to { copyText(screenshot.path) },
                                                "Rename" to {
                                                    screenshotToRename = screenshot.key
                                                    screenshotName = screenshot.fileName.substringBeforeLast('.')
                                                },
                                                "Delete" to { actions.deleteScreenshot(screenshot.key) },
                                            ),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
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
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.gameData.logs.isNotEmpty()) {
                Selector(
                    label = "Log file",
                    value = selectedKey.orEmpty(),
                    values = state.gameData.logs.map { it.key },
                    enabled = !state.isLoadingInstanceLog,
                    onSelect = actions::selectInstanceLog,
                    modifier = Modifier.weight(1f),
                )
            }
            TrestleSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(Res.string.ui_find_in_log)) },
                modifier = Modifier.weight(1f),
            )
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
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = followLaunch,
                onClick = { followLaunch = !followLaunch },
                label = { Text(stringResource(Res.string.ui_follow_launch)) },
            )
            FilterChip(
                selected = wrapLines,
                onClick = { wrapLines = !wrapLines },
                label = { Text(stringResource(Res.string.ui_wrap_lines)) },
            )
            FilterChip(
                selected = colorLines,
                onClick = { colorLines = !colorLines },
                label = { Text(stringResource(Res.string.ui_color_warnings_and_errors)) },
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            when {
                state.isLoadingInstanceLog -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.gameData.logs.isEmpty() && rawText.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No logs yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Minecraft output, archived logs, and crash reports appear here after you play.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> SelectionContainer {
                    Text(
                        if (renderedText.isEmpty()) AnnotatedString("No matching log lines.") else renderedText,
                        modifier = Modifier.fillMaxSize().verticalScroll(outputScroll).padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = wrapLines,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameDataSection(
    title: String,
    emptyMessage: String?,
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard(title = title, action = action) {
        if (emptyMessage != null) {
            Text(
                emptyMessage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun GameDataRow(
    title: String,
    details: String,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            Column(
                Modifier.weight(1f).padding(
                    start = if (leading ==
                        null
                    ) {
                        6.dp
                    } else {
                        0.dp
                    },
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (details.isNotBlank()) {
                    Text(
                        details,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = trailing)
        }
    }
}

@Composable
private fun WorldCard(
    world: ManagedWorld,
    instance: GameInstance,
    actions: LauncherUiActions,
    onRename: () -> Unit,
    onCopySeed: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GameDataRow(
            title = world.name,
            details = listOfNotNull(
                world.gameMode?.replaceFirstChar { it.uppercase() },
                formatFileSize(world.sizeBytes),
                world.dataPacks.size.takeIf { it > 0 }?.let { if (it == 1) "1 data pack" else "$it data packs" },
                world.lastModifiedEpochMillis?.let { "Played ${formatLocalDateTime(it)}" },
            ).joinToString(" · "),
            leading = { WorldThumbnail(world.iconPath, world.name, instance) },
        ) {
            FilledTonalButton(
                onClick = { actions.launchWorld(world.key) },
                enabled = instance.installationState is InstallationState.Installed,
            ) {
                Icon(painterResource(Res.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.ui_play))
            }
            InstanceItemActions(
                buildList {
                    add("Rename" to onRename)
                    add("Copy" to { actions.copyWorld(world.key) })
                    add("Back up" to { actions.backupWorld(world.key) })
                    world.seed?.let { seed -> add("Copy seed" to { onCopySeed(seed) }) }
                    if (world.iconPath != null) add("Reset icon" to { actions.resetWorldIcon(world.key) })
                    add("Delete" to { actions.deleteWorld(world.key) })
                },
            )
        }
        world.dataPacks.forEach { pack ->
            Row(
                Modifier.fillMaxWidth().padding(start = 72.dp, end = 12.dp).clip(MaterialTheme.shapes.small).toggleable(
                    value = pack.enabled,
                    role = Role.Switch,
                    onValueChange = { actions.toggleDataPack(world.key, pack.key) },
                ).padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(pack.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Data pack · ${formatFileSize(pack.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = pack.enabled, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun ServerCard(server: SavedServer, actions: LauncherUiActions) {
    val statusColor = when (server.status) {
        ServerStatus.ONLINE -> MaterialTheme.colorScheme.tertiary
        ServerStatus.OFFLINE -> MaterialTheme.colorScheme.error
        ServerStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    val status = when (server.status) {
        ServerStatus.ONLINE -> listOfNotNull(
            server.onlinePlayers?.let { online ->
                server.maximumPlayers?.let { maximum -> "$online/$maximum online" } ?: "$online online"
            },
            server.pingMillis?.let { "$it ms" },
        ).joinToString(" · ").ifBlank { "Online" }

        ServerStatus.OFFLINE -> "Offline"

        ServerStatus.UNKNOWN -> "Not checked"
    }
    GameDataRow(
        title = server.name,
        details = "${server.address} · $status",
        leading = {
            Box(
                Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(statusColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (server.iconDataUrl != null) {
                    AsyncImage(
                        model = server.iconDataUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(server.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = statusColor)
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(3.dp).size(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(2.dp)
                        .background(statusColor, CircleShape),
                )
            }
        },
    ) {
        FilledTonalButton(onClick = { actions.joinServer(server.key) }, enabled = server.status != ServerStatus.OFFLINE) {
            Text(stringResource(Res.string.ui_join))
        }
        InstanceItemActions(
            listOf(
                "Move up" to { actions.moveServer(server.key, -1) },
                "Move down" to { actions.moveServer(server.key, 1) },
                "Edit" to { actions.openServerEditor(server.key) },
                "Remove" to { actions.removeServer(server.key) },
            ),
        )
    }
}

@Composable
private fun ScreenshotTile(
    screenshot: ManagedScreenshot,
    onOpen: () -> Unit,
    menu: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(
            model = screenshot.path,
            contentDescription = screenshot.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(enabled = supportsOpenPath, onClickLabel = stringResource(Res.string.ui_open), onClick = onOpen),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(screenshot.fileName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        screenshot.createdAtEpochMillis?.let(::formatLocalDateTime),
                        formatFileSize(screenshot.sizeBytes),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            InstanceItemActions(menu)
        }
    }
}

@Composable
private fun InstanceItemActions(actions: List<Pair<String, () -> Unit>>) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        TrestleTooltipIconButton(label = stringResource(Res.string.ui_more), onClick = { expanded = true }) {
            Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = stringResource(Res.string.ui_more))
        }
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
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = actions::setServerName,
                    label = { Text(stringResource(Res.string.ui_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
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
    compact: Boolean,
) {
    val accent = instanceAccent(instance)
    Box(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.2f), Color.Transparent)),
        ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = WideContentWidth).fillMaxWidth()
                .padding(start = 12.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InstanceBackButton(onBack)
                InstanceArtwork(instance, if (compact) 64.dp else 88.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        instance.displayName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        "Minecraft ${instance.minecraftVersionId} · ${instance.modLoader.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!compact) InstanceHeaderStats(state, instance)
                }
                if (!compact) {
                    PrimaryInstanceAction(
                        instance,
                        state,
                        actions,
                        Modifier.width(220.dp),
                        buttonModifier = Modifier.heightIn(min = 52.dp),
                    )
                }
            }
            if (compact) {
                InstanceHeaderStats(state, instance, Modifier.padding(start = 12.dp))
                PrimaryInstanceAction(
                    instance,
                    state,
                    actions,
                    Modifier.fillMaxWidth().padding(start = 12.dp),
                    buttonModifier = Modifier.heightIn(min = 52.dp),
                )
            }
        }
    }
}

@Composable
private fun InstanceHeaderStats(state: LauncherUiState, instance: GameInstance, modifier: Modifier = Modifier) {
    val running = state.activeLaunch?.instanceId == instance.id && state.activeLaunch.status is LaunchStatus.Running
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (running) MaterialTheme.colorScheme.primary else stateColor(instance.installationState)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(
                if (running) "Running" else stateLabel(instance.installationState),
                color = color,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (instance.playTimeMillis > 0) {
            Text(
                "${formatPlayTime(instance.playTimeMillis)} played",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        instance.lastLaunchAtEpochMillis?.let {
            Text(
                "Last played ${formatLocalDateTime(it)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun InstanceOverview(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    listState: LazyListState,
    onChangeComponents: () -> Unit,
    onOpenSection: (InstanceSection) -> Unit,
    onDiscover: (ResourceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openPath = rememberOpenPath()
    BoxWithConstraints(modifier) {
        val twoColumns = maxWidth >= 900.dp
        val stackedStats = maxWidth < 600.dp
        val main: @Composable ColumnScope.() -> Unit = {
            OverviewStats(instance, state, stacked = stackedStats)
            OverviewWorlds(state, instance, actions, onOpenSection)
            OverviewServers(state, actions, onOpenSection)
            OverviewScreenshots(state, openPath, onOpenSection)
            if (state.gameLogLines.isNotEmpty() || state.lastCrashReport != null) {
                SectionCard(
                    title = stringResource(Res.string.ui_game_console),
                    action = {
                        TextButton(onClick = actions::clearGameLog, enabled = state.gameLogLines.isNotEmpty()) {
                            Text(stringResource(Res.string.ui_clear))
                        }
                    },
                ) { GameConsole(state, openPath) }
            }
        }
        val side: @Composable ColumnScope.() -> Unit = {
            OverviewContentSummary(state, instance, onOpenSection, onDiscover)
            OverviewSetup(state, instance, actions, openPath, onChangeComponents)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item("dashboard") {
                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1.65f), verticalArrangement = Arrangement.spacedBy(20.dp), content = main)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = side)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        main()
                        side()
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStats(instance: GameInstance, state: LauncherUiState, stacked: Boolean) {
    val stats = listOf(
        Triple(
            "Play time",
            if (instance.playTimeMillis > 0) formatPlayTime(instance.playTimeMillis) else "0 min",
            MaterialTheme.colorScheme.primary,
        ),
        Triple("Sessions", instance.launchCount.toString(), MaterialTheme.colorScheme.tertiary),
        Triple("Worlds", state.gameData.worlds.size.toString(), instanceAccent(instance)),
        Triple(
            "Mods",
            state.installedContent.count { it.type == ResourceType.MOD && it.direct }.toString(),
            MaterialTheme.colorScheme.secondary,
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(if (stacked) 2 else stats.size).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value, accent) ->
                    StatTile(label, value, accent = accent, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun OverviewWorlds(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    onOpenSection: (InstanceSection) -> Unit,
) {
    val worlds = state.gameData.worlds.sortedByDescending { it.lastModifiedEpochMillis ?: Long.MIN_VALUE }
    SectionCard(
        title = "Jump back in",
        supportingText = if (worlds.isEmpty()) null else "Open a world directly from here.",
        action = {
            if (worlds.isNotEmpty()) {
                TextButton(onClick = { onOpenSection(InstanceSection.WORLDS) }) { Text("All worlds") }
            }
        },
    ) {
        if (worlds.isEmpty()) {
            EmptyHint(
                "No worlds yet. Start the game to create one, or bring in a world you already have.",
            ) { WorldImportButton(actions) }
            return@SectionCard
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            worlds.take(3).forEach { world ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WorldThumbnail(world.iconPath, world.name, instance)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(world.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    world.gameMode?.replaceFirstChar { it.uppercase() },
                                    world.lastModifiedEpochMillis?.let { "Played ${formatLocalDateTime(it)}" },
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalButton(
                            onClick = { actions.launchWorld(world.key) },
                            enabled = instance.installationState is InstallationState.Installed,
                        ) {
                            Icon(painterResource(Res.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.ui_play))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldThumbnail(iconPath: String?, name: String, instance: GameInstance) {
    Box(
        Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
            .background(instanceAccent(instance).copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        if (iconPath != null) {
            AsyncImage(model = iconPath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun OverviewServers(state: LauncherUiState, actions: LauncherUiActions, onOpenSection: (InstanceSection) -> Unit) {
    val servers = state.gameData.servers
    if (servers.isEmpty()) return
    SectionCard(
        title = stringResource(Res.string.ui_servers),
        action = { TextButton(onClick = { onOpenSection(InstanceSection.SERVERS) }) { Text("Manage") } },
    ) {
        servers.take(3).forEach { server ->
            val online = server.status == ServerStatus.ONLINE
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(10.dp).background(
                        when (server.status) {
                            ServerStatus.ONLINE -> MaterialTheme.colorScheme.tertiary
                            ServerStatus.OFFLINE -> MaterialTheme.colorScheme.error
                            ServerStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
                        },
                        CircleShape,
                    ),
                )
                Column(Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(
                            server.address,
                            server.onlinePlayers?.let { online -> "$online/${server.maximumPlayers ?: "?"} online" },
                            server.pingMillis?.let { "$it ms" },
                        ).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { actions.joinServer(server.key) }, enabled = online || server.status == ServerStatus.UNKNOWN) {
                    Text(stringResource(Res.string.ui_join))
                }
            }
        }
    }
}

@Composable
private fun OverviewScreenshots(state: LauncherUiState, openPath: (String) -> Unit, onOpenSection: (InstanceSection) -> Unit) {
    val screenshots = state.gameData.screenshots.sortedByDescending { it.createdAtEpochMillis ?: Long.MIN_VALUE }
    if (screenshots.isEmpty()) return
    SectionCard(
        title = "Recent screenshots",
        action = { TextButton(onClick = { onOpenSection(InstanceSection.SCREENSHOTS) }) { Text("See all ${screenshots.size}") } },
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            screenshots.take(4).forEach { screenshot ->
                AsyncImage(
                    model = screenshot.path,
                    contentDescription = screenshot.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(enabled = supportsOpenPath, onClickLabel = stringResource(Res.string.ui_open)) {
                            openPath(screenshot.path)
                        },
                )
            }
            repeat((4 - screenshots.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun OverviewContentSummary(
    state: LauncherUiState,
    instance: GameInstance,
    onOpenSection: (InstanceSection) -> Unit,
    onDiscover: (ResourceType) -> Unit,
) {
    val direct = state.installedContent.filter { it.direct }
    val updates = state.installedContentUpdates.size
    SectionCard(
        title = stringResource(Res.string.ui_content),
        action = { TextButton(onClick = { onOpenSection(InstanceSection.CONTENT) }) { Text("Manage") } },
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            listOf(ResourceType.MOD, ResourceType.RESOURCE_PACK, ResourceType.SHADER_PACK).forEach { type ->
                Column(Modifier.weight(1f)) {
                    Text(direct.count { it.type == type }.toString(), style = MaterialTheme.typography.headlineMedium)
                    Text(type.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (updates > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { onOpenSection(InstanceSection.CONTENT) },
            ) {
                Text(
                    if (updates == 1) "1 update available" else "$updates updates available",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
        if (instance.modLoader != ModLoader.VANILLA) {
            Button(
                onClick = { onDiscover(ResourceType.MOD) },
                enabled = instance.installationState is InstallationState.Installed,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.ui_add_mods))
            }
        }
    }
}

@Composable
private fun OverviewSetup(
    state: LauncherUiState,
    instance: GameInstance,
    actions: LauncherUiActions,
    openPath: (String) -> Unit,
    onChangeComponents: () -> Unit,
) {
    SectionCard(
        title = "Setup",
        action = {
            TextButton(
                onClick = onChangeComponents,
                modifier = Modifier.testTag(LauncherTestTags.VERSION_COMPONENTS_CHANGE),
            ) { Text(stringResource(Res.string.ui_change)) }
        },
    ) {
        CompactProperty("Minecraft", instance.minecraftVersionId)
        CompactProperty(
            if (instance.modLoader == ModLoader.VANILLA) "Loader" else instance.modLoader.label,
            if (instance.modLoader == ModLoader.VANILLA) "Vanilla" else instance.loaderVersion ?: "Chosen at install",
        )
        CompactProperty("Java", "Java ${instance.requiredJavaMajor}")
        CompactProperty("Memory", "${instance.memory.minimumMiB}–${instance.memory.maximumMiB} MiB")
        instance.group?.let { CompactProperty("Group", it) }
        state.launchPlan?.let { plan ->
            CompactProperty("Classpath", "${plan.classpathEntries} entries")
            CompactProperty("Account", plan.authentication)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (supportsOpenPath) {
                TextButton(
                    onClick = {
                        openPath(if (currentPlatform == "Android") "${instance.instanceDirectory}/game" else instance.instanceDirectory)
                    },
                ) { Text("Open folder") }
            }
            TextButton(
                onClick = actions::inspectLaunchPlan,
                enabled = instance.installationState is InstallationState.Installed,
            ) { Text(stringResource(Res.string.ui_inspect_launch_plan)) }
        }
    }
}

@Composable
private fun CompactProperty(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EmptyHint(message: String, action: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun GameConsole(state: LauncherUiState, openPath: (String) -> Unit) {
    Column {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(max = 280.dp),
        ) {
            SelectionContainer {
                Text(
                    state.gameLogLines.takeLast(120).joinToString("\n").ifBlank { "No process output." },
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        state.lastCrashReport?.let { report ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("modpack-origin") {
                ModpackOriginPanel(instance, state, actions)
            }
            item("add-content") {
                val installed = instance.installationState is InstallationState.Installed
                SectionCard(
                    title = "Add content",
                    supportingText = "Browse compatible content or add a file. Trestle installs required dependencies for you.",
                ) {
                    browsableResourceTypes.filterNot { it == ResourceType.MODPACK }.forEach { type ->
                        ContentTypeRow(
                            type = type,
                            enabled = installed,
                            onClick = { onDiscover(type) },
                            localFileAction = { LocalFileButton(type = type, enabled = installed, actions = actions) },
                        )
                    }
                }
            }
            item("installed-content") {
                InstalledContentPanel(state, instance, actions)
            }
        }
        if (dropActive) DropOverlay("Drop to add content to ${instance.displayName}")
    }
}

@Composable
private fun InstalledContentPanel(state: LauncherUiState, instance: GameInstance, actions: LauncherUiActions) {
    val openPath = rememberOpenPath()
    val copyText = rememberCopyText()
    SectionCard(
        title = stringResource(Res.string.ui_installed_content),
        supportingText = "Turn content on or off, update it, or remove it.",
        action = {
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
        },
    ) {
        when {
            state.isLoadingInstalledContent -> Row(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.ui_reading_installed_content), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            state.installedContent.isEmpty() -> Text(
                "No mods, resource packs, or shaders are installed yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
