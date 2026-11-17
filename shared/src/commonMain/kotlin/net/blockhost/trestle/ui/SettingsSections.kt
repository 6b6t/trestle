@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import net.blockhost.trestle.resources.ui_proxy_changes_apply_after_restart
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
import kotlin.math.roundToInt

@Composable
private fun SettingsColumn(
    title: StringResource,
    scrollState: ScrollState,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 840.dp).verticalScroll(scrollState)
                .padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        ) {
            SectionCard(title = stringResource(title)) {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
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
        CommittedTextField(
            value = preferences.ftbAppInstancesPath,
            onCommit = { actions.setLauncherPreferences(preferences.copy(ftbAppInstancesPath = it)) },
            label = stringResource(Res.string.ui_ftb_app_instances_folder),
            supportingText = stringResource(Res.string.ui_used_when_importing_existing_ftb_app_instances),
            validate = ::folderPathError,
        )
        Text(stringResource(Res.string.ui_folder_changes_apply_after_trestle_restarts), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

@Composable
private fun FolderPreferenceField(label: String, value: String, defaultValue: String, onValueChange: (String) -> Unit) {
    CommittedTextField(
        value = value,
        onCommit = onValueChange,
        label = label,
        placeholder = defaultValue,
        supportingText = if (value.isBlank()) "Default: $defaultValue" else "Custom location",
        validate = ::folderPathError,
    )
}

/** Folder fields accept a blank value (the default) or a full path, never a half-typed relative one. */
private fun folderPathError(value: String): String? {
    val trimmed = value.trim()
    val absolute = trimmed.startsWith("/") || Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) ||
        trimmed.startsWith("\\\\")
    return if (trimmed.isEmpty() || absolute) null else "Enter a full folder path, or leave it blank for the default."
}

/**
 * A text field that keeps its own draft and saves only when the player presses Enter or leaves the field,
 * and only when [validate] accepts the value. Saving on every keystroke would store half-typed values.
 */
@Composable
private fun CommittedTextField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
    placeholder: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    validate: (String) -> String? = { null },
) {
    var draft by remember(value) { mutableStateOf(value) }
    val error = validate(draft)
    val commit = {
        if (error == null && draft != value) onCommit(draft.trim())
    }
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier.onFocusChanged { focus -> if (!focus.isFocused) commit() },
    )
}

/** A slider that saves once the player releases it, instead of on every frame of the drag. */
@Composable
private fun PreferenceSlider(
    label: String,
    valueLabel: (Int) -> String,
    value: Int,
    range: IntRange,
    steps: Int = range.last - range.first - 1,
    onCommit: (Int) -> Unit,
) {
    var draft by remember(value) { mutableIntStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.widthIn(max = 760.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(valueLabel(draft), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = draft.toFloat(),
            onValueChange = { draft = it.roundToInt() },
            onValueChangeFinished = { if (draft != value) onCommit(draft) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
        )
    }
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
        PreferenceSlider("Concurrent task limit", { it.toString() }, network.concurrentTasks, 1..64) {
            actions.setLauncherPreferences(preferences.copy(network = network.copy(concurrentTasks = it)))
        }
        PreferenceSlider("Concurrent download limit", { it.toString() }, network.concurrentDownloads, 1..32) {
            actions.setLauncherPreferences(preferences.copy(network = network.copy(concurrentDownloads = it)))
        }
        PreferenceSlider("Retry limit", { it.toString() }, network.retryLimit, 1..10) {
            actions.setLauncherPreferences(preferences.copy(network = network.copy(retryLimit = it)))
        }
        PreferenceSlider("HTTP timeout", { "${it}s" }, network.httpTimeoutSeconds, 5..300, steps = 58) {
            actions.setLauncherPreferences(preferences.copy(network = network.copy(httpTimeoutSeconds = it)))
        }
        HorizontalDivider()
        Text(stringResource(Res.string.ui_console), style = MaterialTheme.typography.titleMedium)
        PreferenceSlider(
            "Log history limit",
            { "$it lines" },
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
            CommittedTextField(
                value = proxy.host,
                onCommit = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(host = it))) },
                label = stringResource(Res.string.ui_address),
                enabled = editable,
                modifier = Modifier.weight(1f),
            )
            CommittedTextField(
                value = proxy.port.toString(),
                onCommit = { value ->
                    value.toIntOrNull()?.let { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(port = it))) }
                },
                label = stringResource(Res.string.ui_port),
                enabled = editable,
                keyboardType = KeyboardType.Number,
                validate = { value -> if (value.toIntOrNull() in 1..65535) null else "1–65535" },
                modifier = Modifier.width(140.dp),
            )
        }
        CommittedTextField(
            value = proxy.username,
            onCommit = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(username = it))) },
            label = stringResource(Res.string.ui_username),
            enabled = editable,
        )
        CommittedTextField(
            value = proxy.password,
            onCommit = { actions.setLauncherPreferences(preferences.copy(proxy = proxy.copy(password = it))) },
            label = stringResource(Res.string.ui_password),
            enabled = editable,
            password = true,
        )
        Text(
            stringResource(Res.string.ui_proxy_credentials_are_stored_in_the_launcher_preferences_file),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(Res.string.ui_proxy_changes_apply_after_restart), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        CommittedTextField(
            value = preferences.technicClientId,
            onCommit = { actions.setLauncherPreferences(preferences.copy(technicClientId = it)) },
            label = stringResource(Res.string.ui_client_id),
            supportingText = stringResource(Res.string.ui_optional_some_private_or_rate_limited_technic_packs_require_it_applies_a),
        )
    }

@Composable
internal fun ToolSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_tools, scrollState, modifier) {
        OutlinedButton(onClick = actions::refreshVersions, enabled = !state.isLoadingVersions) {
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
internal fun AppearanceSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) =
    SettingsColumn(Res.string.ui_appearance, scrollState, modifier) {
        Text(
            "Choose how Trestle looks. Changes apply right away.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemePreference.entries.forEach { preference ->
                ThemeOption(
                    preference = preference,
                    selected = state.themePreference == preference,
                    onClick = { actions.setThemePreference(preference) },
                )
            }
        }
        Text(
            "Desktop system accent colors remain available in every mode.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

@Composable
private fun ThemeOption(preference: ThemePreference, selected: Boolean, onClick: () -> Unit) {
    val dark = trestleColorScheme(darkTheme = true)
    val light = trestleColorScheme(darkTheme = false)
    Column(
        Modifier.width(168.dp).clip(MaterialTheme.shapes.large)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.large,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(84.dp).clip(MaterialTheme.shapes.medium)) {
            val schemes = when (preference) {
                ThemePreference.SYSTEM -> listOf(light, dark)
                ThemePreference.LIGHT -> listOf(light)
                ThemePreference.DARK -> listOf(dark)
            }
            schemes.forEach { scheme -> ThemeSwatch(scheme, Modifier.weight(1f).fillMaxHeight()) }
        }
        Text(preference.label, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ThemeSwatch(scheme: ColorScheme, modifier: Modifier) {
    Column(modifier.background(scheme.background).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth(0.7f).height(8.dp).background(scheme.onBackground.copy(alpha = 0.8f), CircleShape))
        Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.surfaceContainerHigh, MaterialTheme.shapes.small))
        Box(Modifier.fillMaxWidth(0.55f).height(14.dp).background(scheme.primary, CircleShape))
    }
}

@Composable
internal fun RuntimeSettings(state: LauncherUiState, actions: LauncherUiActions, scrollState: ScrollState, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    SettingsColumn(Res.string.ui_runtime, scrollState, modifier) {
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
            enabled = !state.isLoadingVersions,
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
