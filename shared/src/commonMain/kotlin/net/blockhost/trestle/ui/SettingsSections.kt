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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.app.InstanceSortMode
import net.blockhost.trestle.app.LauncherProxyType
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_address
import net.blockhost.trestle.resources.ui_appearance
import net.blockhost.trestle.resources.ui_atlauncher
import net.blockhost.trestle.resources.ui_availability_is_controlled_by_the_trestle_build_api_key
import net.blockhost.trestle.resources.ui_available_without_an_api_key
import net.blockhost.trestle.resources.ui_client_id
import net.blockhost.trestle.resources.ui_console
import net.blockhost.trestle.resources.ui_curseforge
import net.blockhost.trestle.resources.ui_folder_changes_apply_after_trestle_restarts
import net.blockhost.trestle.resources.ui_folders
import net.blockhost.trestle.resources.ui_ftb_app_instances_folder
import net.blockhost.trestle.resources.ui_general
import net.blockhost.trestle.resources.ui_http_timeout_and_proxy_changes_apply_to_new_connections_after_restart
import net.blockhost.trestle.resources.ui_instance_sorting
import net.blockhost.trestle.resources.ui_language
import net.blockhost.trestle.resources.ui_modrinth
import net.blockhost.trestle.resources.ui_mods_and_modpacks
import net.blockhost.trestle.resources.ui_open_version_release
import net.blockhost.trestle.resources.ui_optional_some_private_or_rate_limited_technic_packs_require_it_applies_a
import net.blockhost.trestle.resources.ui_password
import net.blockhost.trestle.resources.ui_port
import net.blockhost.trestle.resources.ui_proxy
import net.blockhost.trestle.resources.ui_proxy_credentials_are_stored_in_the_launcher_preferences_file
import net.blockhost.trestle.resources.ui_proxy_settings_apply_to_trestle_minecraft_does_not_use_them
import net.blockhost.trestle.resources.ui_runtime
import net.blockhost.trestle.resources.ui_services
import net.blockhost.trestle.resources.ui_tasks_and_downloads
import net.blockhost.trestle.resources.ui_technic
import net.blockhost.trestle.resources.ui_tools
import net.blockhost.trestle.resources.ui_used_when_importing_existing_ftb_app_instances
import net.blockhost.trestle.resources.ui_username
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun SettingsColumn(
    title: StringResource,
    scrollState: ScrollState,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 840.dp).verticalScroll(scrollState).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
internal fun GeneralSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_general, scrollState, modifier) {
        val preferences = state.launcherPreferences
        Text(stringResource(Res.string.ui_instance_sorting), style = MaterialTheme.typography.titleMedium)
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
internal fun LanguageSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_language, scrollState, modifier) {
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
internal fun FolderSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_folders, scrollState, modifier) {
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
        Text(stringResource(Res.string.ui_folder_changes_apply_after_trestle_restarts), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
internal fun ContentSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_mods_and_modpacks, scrollState, modifier) {
        val preferences = state.launcherPreferences
        val content = preferences.content
        SettingsSwitch("Scan subfolders for blocked mods", content.scanSubfolders) {
            actions.setLauncherPreferences(preferences.copy(content = content.copy(scanSubfolders = it)))
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
    TrestleSwitchItem(
        label = label,
        checked = checked,
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        onCheckedChange = onCheckedChange,
    )
}

@Composable
internal fun NetworkSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_tasks_and_downloads, scrollState, modifier) {
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
        Text(stringResource(Res.string.ui_console), style = MaterialTheme.typography.titleMedium)
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
        Text(
            stringResource(Res.string.ui_http_timeout_and_proxy_changes_apply_to_new_connections_after_restart),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

@Composable
internal fun ProxySettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_proxy, scrollState, modifier) {
        val preferences = state.launcherPreferences
        val proxy = preferences.proxy
        Text(
            stringResource(Res.string.ui_proxy_settings_apply_to_trestle_minecraft_does_not_use_them),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                label = { Text(stringResource(Res.string.ui_address)) },
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
                label = { Text(stringResource(Res.string.ui_port)) },
                enabled = editable,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp),
            )
        }
        TextField(
            value = proxy.username,
            onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(username = it))) },
            label = { Text(stringResource(Res.string.ui_username)) },
            enabled = editable,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        )
        TextField(
            value = proxy.password,
            onValueChange = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(password = it))) },
            label = { Text(stringResource(Res.string.ui_password)) },
            enabled = editable,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        )
        Text(
            stringResource(Res.string.ui_proxy_credentials_are_stored_in_the_launcher_preferences_file),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

@Composable
internal fun ServiceSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_services, scrollState, modifier) {
        val preferences = state.launcherPreferences
        Text(stringResource(Res.string.ui_modrinth), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.ui_available_without_an_api_key), color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text(stringResource(Res.string.ui_curseforge), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.ui_availability_is_controlled_by_the_trestle_build_api_key),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text(stringResource(Res.string.ui_atlauncher), style = MaterialTheme.typography.titleMedium)
        Text(
            "The public catalog is available without an API key.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text(stringResource(Res.string.ui_technic), style = MaterialTheme.typography.titleMedium)
        TextField(
            value = preferences.technicClientId,
            onValueChange = { actions.setLauncherPreferences(preferences.copy(technicClientId = it)) },
            label = { Text(stringResource(Res.string.ui_client_id)) },
            supportingText = {
                Text(stringResource(Res.string.ui_optional_some_private_or_rate_limited_technic_packs_require_it_applies_a))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        )
        TextField(
            value = preferences.ftbAppInstancesPath,
            onValueChange = { actions.setLauncherPreferences(preferences.copy(ftbAppInstancesPath = it)) },
            label = { Text(stringResource(Res.string.ui_ftb_app_instances_folder)) },
            supportingText = { Text(stringResource(Res.string.ui_used_when_importing_existing_ftb_app_instances)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        )
    }

@Composable
internal fun ToolSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_tools, scrollState, modifier) {
        OutlinedButton(onClick = actions::refreshVersions) {
            Text(if (state.isLoadingVersions) "Refreshing versions…" else "Refresh Minecraft metadata")
        }
        val preferences = state.launcherPreferences
        SettingsSwitch("Check for Trestle updates automatically", preferences.updates.automaticChecks) {
            actions.setLauncherPreferences(preferences.copy(updates = preferences.updates.copy(automaticChecks = it)))
        }
        SettingsSwitch("Include preview releases", preferences.updates.includePrereleases) {
            actions.setLauncherPreferences(
                preferences.copy(updates = preferences.updates.copy(includePrereleases = it, lastCheckedAtMillis = 0)),
            )
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
internal fun AppearanceSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) {
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.ui_appearance), style = MaterialTheme.typography.titleLarge)
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
internal fun RuntimeSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier.verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.ui_runtime), style = MaterialTheme.typography.titleLarge)
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
                    Text(stringResource(Res.string.ui_open_version_release, update.version))
                }
            }
        }
    }
}
