@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ui_android_build
import net.blockhost.trestle.resources.ui_app
import net.blockhost.trestle.resources.ui_appearance
import net.blockhost.trestle.resources.ui_back
import net.blockhost.trestle.resources.ui_choose_a_category_to_change_trestle_settings
import net.blockhost.trestle.resources.ui_content
import net.blockhost.trestle.resources.ui_dependencies_metadata_and_modpacks
import net.blockhost.trestle.resources.ui_desktop_build
import net.blockhost.trestle.resources.ui_diagnostics
import net.blockhost.trestle.resources.ui_downloads_section
import net.blockhost.trestle.resources.ui_downloads_timeouts_and_log_history
import net.blockhost.trestle.resources.ui_folders
import net.blockhost.trestle.resources.ui_general
import net.blockhost.trestle.resources.ui_ios_build
import net.blockhost.trestle.resources.ui_language
import net.blockhost.trestle.resources.ui_launcher_language
import net.blockhost.trestle.resources.ui_launcher_log
import net.blockhost.trestle.resources.ui_library_sorting
import net.blockhost.trestle.resources.ui_light_dark_or_system_theme
import net.blockhost.trestle.resources.ui_metadata_refresh_and_update_checks
import net.blockhost.trestle.resources.ui_modrinth_curseforge_and_other_catalogs
import net.blockhost.trestle.resources.ui_network
import net.blockhost.trestle.resources.ui_platform_usage_and_credential_storage
import net.blockhost.trestle.resources.ui_proxy
import net.blockhost.trestle.resources.ui_route_launcher_traffic_through_a_proxy
import net.blockhost.trestle.resources.ui_runtime
import net.blockhost.trestle.resources.ui_services
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_storage
import net.blockhost.trestle.resources.ui_tools
import net.blockhost.trestle.resources.ui_what_the_launcher_did
import net.blockhost.trestle.resources.ui_where_instances_and_files_live
import net.blockhost.trestle.text.formattedString
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsPage(state: LauncherUiState, modifier: Modifier, actions: LauncherUiActions) {
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }
    val section = SettingsSection.entries.firstOrNull { it.name == sectionName }
    // Each section keeps its own scroll position, so a newly opened section starts at the top.
    val runtimeScrollState = rememberSaveable(section?.name, saver = ScrollState.Saver) { ScrollState(0) }
    val logListState = rememberLazyListState()
    val adaptiveInfo = LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo),
    )
    val scope = rememberCoroutineScope()
    val listPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val detailPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden
    val selectSection: (SettingsSection) -> Unit = { selectedSection ->
        sectionName = selectedSection.name
        scope.launch {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedSection.name)
        }
    }
    val showCategories: () -> Unit = {
        sectionName = null
        scope.launch {
            if (navigator.canNavigateBack()) {
                navigator.navigateBack()
            } else {
                navigator.navigateTo(ListDetailPaneScaffoldRole.List)
            }
        }
    }

    LaunchedEffect(detailPaneHidden) {
        if (!detailPaneHidden && section == null) {
            sectionName = SettingsSection.GENERAL.name
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, SettingsSection.GENERAL.name)
        }
    }
    PlatformBackHandler(
        enabled = section != null && listPaneHidden && navigator.canNavigateBack(),
        onBack = showCategories,
    )

    Column(modifier.fillMaxSize().testTag(LauncherTestTags.SETTINGS)) {
        if (listPaneHidden && section != null) {
            PageHeader(
                title = stringResource(section.label),
                navigationIcon = {
                    TrestleTooltipIconButton(
                        label = stringResource(Res.string.ui_back),
                        onClick = showCategories,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.ui_back),
                        )
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        } else {
            PageHero(
                title = stringResource(Res.string.ui_settings),
                subtitle = formattedString(
                    when (currentPlatform) {
                        "Android" -> Res.string.ui_android_build
                        "iOS" -> Res.string.ui_ios_build
                        else -> Res.string.ui_desktop_build
                    },
                    BuildInfo.VERSION,
                ),
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.fillMaxSize(),
            listPane = {
                AnimatedPane(Modifier.preferredWidth(300.dp)) {
                    Box(Modifier.fillMaxSize().testTag(LauncherTestTags.SETTINGS_CATEGORIES)) {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            SettingsGroup.entries.forEach { group ->
                                item("group-${group.name}") {
                                    Text(
                                        stringResource(group.label),
                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                items(
                                    SettingsSection.entries.filter { it.group == group },
                                    key = SettingsSection::name,
                                ) { item ->
                                    SettingsSectionButton(
                                        section = item,
                                        selected = section == item,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { selectSection(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    if (section == null) {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp).testTag(LauncherTestTags.SETTINGS_DETAIL),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(Res.string.ui_settings), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(Res.string.ui_choose_a_category_to_change_trestle_settings),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize().testTag(LauncherTestTags.SETTINGS_DETAIL)) {
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
            },
        )
    }
}

private enum class SettingsGroup(val label: StringResource) {
    APP(Res.string.ui_app),
    STORAGE(Res.string.ui_storage),
    NETWORK(Res.string.ui_network),
    DIAGNOSTICS(Res.string.ui_diagnostics),
}

private enum class SettingsSection(val label: StringResource, val group: SettingsGroup, val summary: StringResource) {
    GENERAL(Res.string.ui_general, SettingsGroup.APP, Res.string.ui_library_sorting),
    APPEARANCE(Res.string.ui_appearance, SettingsGroup.APP, Res.string.ui_light_dark_or_system_theme),
    LANGUAGE(Res.string.ui_language, SettingsGroup.APP, Res.string.ui_launcher_language),
    FOLDERS(Res.string.ui_folders, SettingsGroup.STORAGE, Res.string.ui_where_instances_and_files_live),
    CONTENT(Res.string.ui_content, SettingsGroup.STORAGE, Res.string.ui_dependencies_metadata_and_modpacks),
    NETWORK(Res.string.ui_downloads_section, SettingsGroup.NETWORK, Res.string.ui_downloads_timeouts_and_log_history),
    PROXY(Res.string.ui_proxy, SettingsGroup.NETWORK, Res.string.ui_route_launcher_traffic_through_a_proxy),
    SERVICES(Res.string.ui_services, SettingsGroup.NETWORK, Res.string.ui_modrinth_curseforge_and_other_catalogs),
    RUNTIME(Res.string.ui_runtime, SettingsGroup.DIAGNOSTICS, Res.string.ui_platform_usage_and_credential_storage),
    LOGS(Res.string.ui_launcher_log, SettingsGroup.DIAGNOSTICS, Res.string.ui_what_the_launcher_did),
    TOOLS(Res.string.ui_tools, SettingsGroup.DIAGNOSTICS, Res.string.ui_metadata_refresh_and_update_checks),
}

@Composable
private fun SettingsSectionButton(section: SettingsSection, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(section.label), style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            Text(
                stringResource(section.summary),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
        ),
        modifier = modifier.clip(MaterialTheme.shapes.medium).selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
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
