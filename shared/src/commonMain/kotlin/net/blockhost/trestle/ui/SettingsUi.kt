@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ui_back
import net.blockhost.trestle.resources.ui_appearance
import net.blockhost.trestle.resources.ui_content
import net.blockhost.trestle.resources.ui_folders
import net.blockhost.trestle.resources.ui_general
import net.blockhost.trestle.resources.ui_language
import net.blockhost.trestle.resources.ui_launcher_log
import net.blockhost.trestle.resources.ui_network
import net.blockhost.trestle.resources.ui_proxy
import net.blockhost.trestle.resources.ui_runtime
import net.blockhost.trestle.resources.ui_services
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_tools
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsPage(state: LauncherUiState, modifier: Modifier, actions: LauncherUiActions) {
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }
    val section = SettingsSection.entries.firstOrNull { it.name == sectionName }
    val runtimeScrollState = rememberScrollState()
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
            if (navigator.canNavigateBack()) navigator.navigateBack()
            else navigator.navigateTo(ListDetailPaneScaffoldRole.List)
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
        PageHeader(
            title = if (listPaneHidden && section != null) {
                stringResource(section.label)
            } else {
                stringResource(Res.string.ui_settings)
            },
            navigationIcon = {
                if (listPaneHidden && section != null) {
                    TrestleTooltipIconButton(
                        label = stringResource(Res.string.ui_back),
                        onClick = showCategories,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.ui_back),
                        )
                    }
                }
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.fillMaxSize(),
            listPane = {
                AnimatedPane {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxSize().testTag(LauncherTestTags.SETTINGS_CATEGORIES),
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(SettingsSection.entries, key = SettingsSection::name) { item ->
                                SettingsSectionButton(
                                    section = item,
                                    selected = section == item,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { selectSection(item) },
                                )
                            }
                            item("build-info") {
                                Text(
                                    "$currentPlatform build ${BuildInfo.VERSION}",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                )
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
                                "Choose a category to change Trestle settings.",
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

private enum class SettingsSection(val label: StringResource) {
    GENERAL(Res.string.ui_general),
    LANGUAGE(Res.string.ui_language),
    APPEARANCE(Res.string.ui_appearance),
    FOLDERS(Res.string.ui_folders),
    CONTENT(Res.string.ui_content),
    NETWORK(Res.string.ui_network),
    PROXY(Res.string.ui_proxy),
    RUNTIME(Res.string.ui_runtime),
    LOGS(Res.string.ui_launcher_log),
    SERVICES(Res.string.ui_services),
    TOOLS(Res.string.ui_tools),
}

@Composable
private fun SettingsSectionButton(
    section: SettingsSection,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(section.label)) },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
        ),
        modifier = modifier.selectable(
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
