@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.resources.DependencyKind
import net.blockhost.trestle.resources.ReleaseChannel
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_close
import net.blockhost.trestle.resources.ic_expand_more
import net.blockhost.trestle.resources.ic_extension
import net.blockhost.trestle.resources.ui_any_category
import net.blockhost.trestle.resources.ui_any_loader
import net.blockhost.trestle.resources.ui_any_version
import net.blockhost.trestle.resources.ui_apply
import net.blockhost.trestle.resources.ui_apply_filters
import net.blockhost.trestle.resources.ui_available_versions_and_installation_details_will_appear_here
import net.blockhost.trestle.resources.ui_back_to_results
import net.blockhost.trestle.resources.ui_browse_content
import net.blockhost.trestle.resources.ui_by_author_on_provider
import net.blockhost.trestle.resources.ui_categories
import net.blockhost.trestle.resources.ui_category
import net.blockhost.trestle.resources.ui_change_the_search_or_content_type
import net.blockhost.trestle.resources.ui_channel
import net.blockhost.trestle.resources.ui_choose_a_version
import net.blockhost.trestle.resources.ui_choose_an_instance_to_install_into
import net.blockhost.trestle.resources.ui_choose_instance_to_install_into
import net.blockhost.trestle.resources.ui_client
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_compatible_with_s_s
import net.blockhost.trestle.resources.ui_create_instance
import net.blockhost.trestle.resources.ui_curseforge_blocks_this_file_trestle_will_look_for_the_identical_file_on_
import net.blockhost.trestle.resources.ui_curseforge_requires_a_trestle_api_key_configured_by_the_application_buil
import net.blockhost.trestle.resources.ui_dependencies
import net.blockhost.trestle.resources.ui_downloads
import net.blockhost.trestle.resources.ui_external_dependency
import net.blockhost.trestle.resources.ui_file
import net.blockhost.trestle.resources.ui_filters
import net.blockhost.trestle.resources.ui_filters_with_count
import net.blockhost.trestle.resources.ui_followers
import net.blockhost.trestle.resources.ui_install
import net.blockhost.trestle.resources.ui_install_instance
import net.blockhost.trestle.resources.ui_install_named_first
import net.blockhost.trestle.resources.ui_installing_into
import net.blockhost.trestle.resources.ui_instance_loader_named
import net.blockhost.trestle.resources.ui_instance_version_named
import net.blockhost.trestle.resources.ui_issues
import net.blockhost.trestle.resources.ui_license
import net.blockhost.trestle.resources.ui_load_more
import net.blockhost.trestle.resources.ui_loaders
import net.blockhost.trestle.resources.ui_loading
import net.blockhost.trestle.resources.ui_loading_versions
import net.blockhost.trestle.resources.ui_minecraft
import net.blockhost.trestle.resources.ui_minecraft_version
import net.blockhost.trestle.resources.ui_mod_loader
import net.blockhost.trestle.resources.ui_modpacks_create_a_new_instance
import net.blockhost.trestle.resources.ui_modpacks_install_as_a_new_instance
import net.blockhost.trestle.resources.ui_no_compatible_version
import net.blockhost.trestle.resources.ui_no_results
import net.blockhost.trestle.resources.ui_no_version_for_named
import net.blockhost.trestle.resources.ui_open_manual_download
import net.blockhost.trestle.resources.ui_optional_dependencies
import net.blockhost.trestle.resources.ui_pack_manifest
import net.blockhost.trestle.resources.ui_published
import net.blockhost.trestle.resources.ui_s_required
import net.blockhost.trestle.resources.ui_search_mods_packs_and_shaders
import net.blockhost.trestle.resources.ui_select_a_result
import net.blockhost.trestle.resources.ui_select_an_installed_instance_to_add_content
import net.blockhost.trestle.resources.ui_select_version
import net.blockhost.trestle.resources.ui_server
import net.blockhost.trestle.resources.ui_server_status
import net.blockhost.trestle.resources.ui_size
import net.blockhost.trestle.resources.ui_sort_by
import net.blockhost.trestle.resources.ui_source
import net.blockhost.trestle.resources.ui_the_author_blocks_downloads_from_third_party_launchers
import net.blockhost.trestle.resources.ui_unknown_author
import net.blockhost.trestle.resources.ui_updated
import net.blockhost.trestle.resources.ui_version
import net.blockhost.trestle.resources.ui_versions
import net.blockhost.trestle.resources.ui_view_on_provider
import net.blockhost.trestle.resources.ui_wiki
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ResourceBrowserDialog(state: LauncherUiState, actions: LauncherUiActions) {
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
                                stringResource(Res.string.ui_modpacks_create_a_new_instance)
                            } else if (instance == null) {
                                stringResource(Res.string.ui_select_an_installed_instance_to_add_content)
                            } else {
                                stringResource(Res.string.ui_compatible_with_s_s, instance.minecraftVersionId, instance.modLoader.label)
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
internal fun ResourceBrowserContent(
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
                if (navigator.canNavigateBack()) {
                    navigator.navigateBack()
                } else {
                    navigator.navigateTo(ListDetailPaneScaffoldRole.List)
                }
            }
        }
    }

    SelectFirstResultWhenSideBySide(
        browser = browser,
        sideBySide = navigator.scaffoldValue.bothPanesVisible(),
        onSelect = actions::selectResource,
    )
    val clearSelection = {
        actions.clearResourceSelection()
    }
    PlatformBackHandler(
        enabled = browser.selectedProject != null && listPaneHidden && navigator.canNavigateBack(),
        onBack = clearSelection,
    )
    Column(modifier) {
        // In a single pane, the details get the whole height; the toolbar returns with the results.
        if (!detailFillsScreen(listPaneHidden, browser)) {
            ResourceBrowserToolbar(browser, state, actions, searchFocusRequester)
        }
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
    state: LauncherUiState,
    actions: LauncherUiActions,
    searchFocusRequester: FocusRequester,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        val wide = maxWidth >= 840.dp
        val activeFilters = listOf(
            browser.gameVersionFilter.isNotBlank(),
            browser.loaderFilter != null,
            browser.categoryFilter.isNotBlank(),
        ).count { it }
        val filtersButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            OutlinedButton(
                onClick = {
                    // Closing the inline panel applies what was typed, so filters are never silently ignored.
                    if (showFilters && wide) actions.searchResources()
                    showFilters = !showFilters
                },
                modifier = buttonModifier.testTag(LauncherTestTags.RESOURCE_FILTERS),
            ) {
                Text(
                    if (activeFilters == 0) {
                        stringResource(Res.string.ui_filters)
                    } else {
                        stringResource(Res.string.ui_filters_with_count, activeFilters)
                    },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TrestleSearchBar(
                    value = browser.query,
                    onValueChange = actions::setResourceQuery,
                    searching = browser.isSearching,
                    onSearch = { actions.searchResources() },
                    placeholder = { Text(stringResource(Res.string.ui_search_mods_packs_and_shaders)) },
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier.focusRequester(searchFocusRequester)
                        .testTag(LauncherTestTags.RESOURCE_SEARCH),
                )
                if (wide) InstallTarget(browser, state, actions, Modifier.widthIn(max = 360.dp))
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ResourceTypeChips(browser, actions) }
                    ResourceProviderButtons(browser, actions)
                    filtersButton(Modifier)
                }
                if (showFilters) {
                    ResourceFilterFields(
                        browser = browser,
                        instance = state.selectedInstance,
                        actions = actions,
                        compact = false,
                        onApply = actions::searchResources,
                    )
                }
            } else {
                ResourceTypeChips(browser, actions)
                InstallTarget(browser, state, actions, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ResourceProviderButtons(browser, actions, Modifier.weight(1f))
                    filtersButton(Modifier)
                }
            }
        }
        if (showFilters && !wide) {
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                sheetState = filterSheetState,
            ) {
                ResourceFilterFields(
                    browser = browser,
                    instance = state.selectedInstance,
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
}

@Composable
private fun ResourceFilterFields(
    browser: ResourceBrowserState,
    instance: GameInstance?,
    actions: LauncherUiActions,
    compact: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameVersionField: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = browser.gameVersionFilter,
            onValueChange = actions::setResourceGameVersionFilter,
            label = { Text(stringResource(Res.string.ui_minecraft_version)) },
            placeholder = {
                Text(
                    instance?.takeIf { browser.type != ResourceType.MODPACK }
                        ?.let { stringResource(Res.string.ui_instance_version_named, it.minecraftVersionId) }
                        ?: stringResource(Res.string.ui_any_version),
                )
            },
            singleLine = true,
            modifier = fieldModifier,
        )
    }
    val defaultLoaderLabel = instance?.takeIf { browser.type != ResourceType.MODPACK }
        ?.let { stringResource(Res.string.ui_instance_loader_named, it.modLoader.label) }
        ?: stringResource(Res.string.ui_any_loader)
    val loaderField: @Composable (Modifier) -> Unit = { fieldModifier ->
        Selector(
            label = stringResource(Res.string.ui_mod_loader),
            value = browser.loaderFilter?.label ?: defaultLoaderLabel,
            values = listOf(defaultLoaderLabel) + ModLoader.entries.filterNot { it == ModLoader.VANILLA }.map { it.label },
            modifier = fieldModifier,
            onSelect = { label ->
                actions.setResourceLoaderFilter(ModLoader.entries.firstOrNull { it.label == label })
            },
        )
    }
    val categoryField: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
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
            label = stringResource(Res.string.ui_sort_by),
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
        if (compact) {
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.ui_apply_filters))
            }
        }
    }
}

/** Side-by-side layouts show the first result instead of an empty detail pane. */
@Composable
private fun SelectFirstResultWhenSideBySide(browser: ResourceBrowserState, sideBySide: Boolean, onSelect: (String) -> Unit) {
    val firstProjectId = browser.projects.firstOrNull()?.id
    val nothingSelected = browser.selectedProjectId == null
    val currentOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(firstProjectId, sideBySide, nothingSelected) {
        if (sideBySide && nothingSelected && firstProjectId != null) currentOnSelect(firstProjectId)
    }
}

@Composable
private fun ResourceTypeChips(browser: ResourceBrowserState, actions: LauncherUiActions) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        browsableResourceTypes.forEach { type ->
            val selected = browser.type == type
            FilterChip(
                selected = selected,
                onClick = { if (!selected) actions.setResourceType(type) },
                label = { Text(type.label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }
    }
}

/** Shows which instance new content goes into, and lets the player switch it without leaving Discover. */
@Composable
private fun InstallTarget(
    browser: ResourceBrowserState,
    state: LauncherUiState,
    actions: LauncherUiActions,
    modifier: Modifier = Modifier,
) {
    val instance = state.selectedInstance
    var expanded by remember { mutableStateOf(false) }
    val targets = state.instances.filter { it.installationState is InstallationState.Installed }
    val creatingInstance = browser.type == ResourceType.MODPACK
    Box(modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            onClick = { expanded = true },
            enabled = !creatingInstance && targets.isNotEmpty(),
        ) {
            Row(
                Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InstallTargetLabel(instance.takeUnless { creatingInstance }, creatingInstance)
            }
        }
        InstallTargetMenu(
            expanded = expanded,
            targets = targets,
            onDismiss = { expanded = false },
            onPick = { target ->
                expanded = false
                if (target.id != instance?.id) {
                    actions.selectInstance(target.id)
                    actions.searchResources()
                }
            },
        )
    }
}

@Composable
private fun RowScope.InstallTargetLabel(instance: GameInstance?, creatingInstance: Boolean) {
    if (instance != null) {
        InstanceArtwork(instance, 28.dp)
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                stringResource(Res.string.ui_installing_into),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${instance.displayName} · ${instance.minecraftVersionId} ${instance.modLoader.label}",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(painterResource(Res.drawable.ic_expand_more), contentDescription = null, modifier = Modifier.size(18.dp))
    } else {
        Text(
            if (creatingInstance) {
                stringResource(
                    Res.string.ui_modpacks_install_as_a_new_instance,
                )
            } else {
                stringResource(Res.string.ui_choose_an_instance_to_install_into)
            },
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun InstallTargetMenu(expanded: Boolean, targets: List<GameInstance>, onDismiss: () -> Unit, onPick: (GameInstance) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        targets.forEach { target ->
            DropdownMenuItem(
                leadingIcon = { InstanceArtwork(target, 28.dp) },
                text = {
                    Column {
                        Text(target.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${target.minecraftVersionId} · ${target.modLoader.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = { onPick(target) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceProviderButtons(browser: ResourceBrowserState, actions: LauncherUiActions, modifier: Modifier = Modifier) {
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
        val unavailableReason =
            stringResource(Res.string.ui_curseforge_requires_a_trestle_api_key_configured_by_the_application_buil)
        providers.forEach { provider ->
            val available = provider != ResourceProvider.CURSEFORGE || browser.curseForgeAvailable
            val chip: @Composable () -> Unit = {
                FilterChip(
                    selected = browser.provider == provider,
                    onClick = { actions.setResourceProvider(provider) },
                    enabled = available,
                    label = { Text(provider.label, maxLines = 1) },
                )
            }
            if (available) chip() else TrestleTooltip(unavailableReason) { chip() }
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
            contentPadding = PaddingValues(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    ) { Text(if (browser.isSearching) stringResource(Res.string.ui_loading) else stringResource(Res.string.ui_load_more)) }
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
                stringResource(
                    Res.string.ui_server_status,
                    project.provider.label,
                    project.author.ifBlank {
                        stringResource(Res.string.ui_unknown_author)
                    },
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = { ResourceProjectSummary(project) },
        leadingContent = {
            ResourceProjectLogo(project = project, modifier = Modifier.size(52.dp))
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectionOutline(selected)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    )
}

@Composable
private fun ResourceProjectSummary(project: ResourceProject) {
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
}

@Composable
private fun ResourceProjectLogo(project: ResourceProject, modifier: Modifier) {
    val iconUrl = project.iconUrl
    var iconLoaded by remember(iconUrl) { mutableStateOf(false) }
    Box(
        modifier.aspectRatio(1f).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (!iconLoaded) {
            Text(
                project.name.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        iconUrl?.let {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { iconLoaded = true },
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
            modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(Res.drawable.ic_extension),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(stringResource(Res.string.ui_select_a_result), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.ui_available_versions_and_installation_details_will_appear_here),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val version = browser.selectedVersion
    val selectedFile = version?.primaryFile
    val downloadable = version?.externalPack != null || selectedFile?.url != null || selectedFile?.sha1 != null
    val blockedReason = installBlockedReason(project, instance, browser, downloadable)
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
                        stringResource(
                            Res.string.ui_by_author_on_provider,
                            project.author.ifBlank {
                                stringResource(Res.string.ui_unknown_author)
                            },
                            project.provider.label,
                        ),
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
                project.sourceUrl?.let { stringResource(Res.string.ui_source) to it },
                project.issuesUrl?.let { stringResource(Res.string.ui_issues) to it },
                project.wikiUrl?.let { stringResource(Res.string.ui_wiki) to it },
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
                                Text(
                                    dependency.fileName ?: dependency.projectId ?: dependency.versionId
                                        ?: stringResource(Res.string.ui_external_dependency),
                                )
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
            if (selectedFile?.url == null && selectedFile?.sha1 != null) {
                Text(
                    stringResource(Res.string.ui_curseforge_blocks_this_file_trestle_will_look_for_the_identical_file_on_),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (version != null && version.externalPack == null && !downloadable) {
                Text(
                    stringResource(Res.string.ui_the_author_blocks_downloads_from_third_party_launchers),
                    color = MaterialTheme.colorScheme.error,
                )
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
        InstallBar(project, instance, browser, blockedReason, actions)
    }
}

@Composable
private fun ResourceProjectDetails(project: ResourceProject) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PropertyRow(stringResource(Res.string.ui_downloads), formatCount(project.downloads))
        project.followers?.let { PropertyRow(stringResource(Res.string.ui_followers), formatCount(it)) }
        project.updatedAt?.let { PropertyRow(stringResource(Res.string.ui_updated), it.substringBefore('T')) }
        project.license?.takeIf(String::isNotBlank)?.let { PropertyRow(stringResource(Res.string.ui_license), it) }
        project.clientSupport?.let { PropertyRow(stringResource(Res.string.ui_client), it.label) }
        project.serverSupport?.let { PropertyRow(stringResource(Res.string.ui_server), it.label) }
        if (project.categories.isNotEmpty()) {
            PropertyRow(stringResource(Res.string.ui_categories), project.categories.take(4).joinToString())
        }
    }
}

@Composable
private fun ResourceVersionPicker(browser: ResourceBrowserState, actions: LauncherUiActions) {
    val selected = browser.selectedVersion
    val versions = browser.versions.filter { it.channel in browser.releaseChannels }
    val labels = versions.map { "${it.versionNumber} · ${it.channel.label}" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Selector(
            label = stringResource(Res.string.ui_version),
            value = selected?.let { "${it.versionNumber} · ${it.channel.label}" } ?: stringResource(Res.string.ui_select_version),
            values = labels,
            modifier = Modifier.fillMaxWidth(),
            onSelect = { label ->
                versions.getOrNull(labels.indexOf(label))?.let { actions.selectResourceVersion(it.id) }
            },
        )
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
}

@Composable
private fun ResourceVersionDetails(version: ResourceVersion) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PropertyRow(stringResource(Res.string.ui_channel), version.channel.label)
        PropertyRow(
            stringResource(Res.string.ui_minecraft),
            version.gameVersions.take(4).joinToString().ifBlank {
                stringResource(Res.string.ui_pack_manifest)
            },
        )
        if (version.loaders.isNotEmpty()) PropertyRow(stringResource(Res.string.ui_loaders), version.loaders.joinToString())
        version.publishedAt.takeIf(String::isNotBlank)?.let {
            PropertyRow(stringResource(Res.string.ui_published), it.substringBefore('T'))
        }
        version.primaryFile?.let { file ->
            PropertyRow(stringResource(Res.string.ui_file), file.fileName)
            file.size?.let { PropertyRow(stringResource(Res.string.ui_size), formatFileSize(it)) }
        }
        val required = version.dependencies.count { it.kind == DependencyKind.REQUIRED }
        if (required > 0) PropertyRow(stringResource(Res.string.ui_dependencies), stringResource(Res.string.ui_s_required, required))
    }
}

/** Explains, in one line, why Install is not available yet; null when it is. */
@Composable
private fun installBlockedReason(
    project: ResourceProject,
    instance: GameInstance?,
    browser: ResourceBrowserState,
    downloadable: Boolean,
): String? = targetBlockedReason(project, instance) ?: versionBlockedReason(project, instance, browser, downloadable)

@Composable
private fun targetBlockedReason(project: ResourceProject, instance: GameInstance?): String? = when {
    project.type == ResourceType.MODPACK -> null
    instance == null -> stringResource(Res.string.ui_choose_instance_to_install_into)
    instance.installationState !is InstallationState.Installed -> stringResource(Res.string.ui_install_named_first, instance.displayName)
    instance.modLoader == ModLoader.VANILLA && project.type in loaderOnlyTypes -> VANILLA_CONTENT_MESSAGE
    else -> null
}

@Composable
private fun versionBlockedReason(
    project: ResourceProject,
    instance: GameInstance?,
    browser: ResourceBrowserState,
    downloadable: Boolean,
): String? = when {
    browser.isLoadingVersions -> stringResource(Res.string.ui_loading_versions)

    browser.selectedVersion == null && browser.versions.isEmpty() -> if (project.type != ResourceType.MODPACK && instance != null) {
        stringResource(Res.string.ui_no_version_for_named, "${instance.minecraftVersionId} ${instance.modLoader.label}")
    } else {
        stringResource(Res.string.ui_no_compatible_version)
    }

    browser.selectedVersion == null -> stringResource(Res.string.ui_choose_a_version)

    !downloadable -> stringResource(Res.string.ui_the_author_blocks_downloads_from_third_party_launchers)

    else -> null
}

/** In a single pane, an open project's details take the whole height. */
private fun detailFillsScreen(listPaneHidden: Boolean, browser: ResourceBrowserState): Boolean =
    listPaneHidden && browser.selectedProject != null

private fun ThreePaneScaffoldValue.bothPanesVisible(): Boolean = this[ListDetailPaneScaffoldRole.List] != PaneAdaptedValue.Hidden &&
    this[ListDetailPaneScaffoldRole.Detail] != PaneAdaptedValue.Hidden

@Composable
private fun InstallBar(
    project: ResourceProject,
    instance: GameInstance?,
    browser: ResourceBrowserState,
    blockedReason: String?,
    actions: LauncherUiActions,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (blockedReason != null) {
                Row(
                    Modifier.widthIn(max = 960.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        blockedReason,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).testTag(LauncherTestTags.INSTALL_BLOCKED_REASON),
                    )
                    val notInstalled = project.type != ResourceType.MODPACK && instance != null &&
                        instance.installationState !is InstallationState.Installed &&
                        instance.installationState !is InstallationState.Installing
                    if (notInstalled) {
                        FilledTonalButton(onClick = actions::installSelected) { Text(stringResource(Res.string.ui_install_instance)) }
                    }
                }
            }
            Button(
                onClick = actions::installSelectedResource,
                enabled = blockedReason == null && !browser.isInstalling,
                modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
            ) {
                if (browser.isInstalling) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (project.type ==
                            ResourceType.MODPACK
                        ) {
                            stringResource(Res.string.ui_create_instance)
                        } else {
                            stringResource(Res.string.ui_install)
                        },
                    )
                }
            }
        }
    }
}
