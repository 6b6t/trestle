@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_account
import net.blockhost.trestle.resources.ui_additional_game_arguments
import net.blockhost.trestle.resources.ui_additional_jvm_arguments
import net.blockhost.trestle.resources.ui_browse
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_changes_are_written_to_this_instance_s
import net.blockhost.trestle.resources.ui_changing_the_game_version_or_loader_requires
import net.blockhost.trestle.resources.ui_commands_run_directly_in_the_game_directory
import net.blockhost.trestle.resources.ui_custom_commands
import net.blockhost.trestle.resources.ui_custom_java_executable
import net.blockhost.trestle.resources.ui_enter_a_maximum_memory_value
import net.blockhost.trestle.resources.ui_enter_a_minimum_memory_value
import net.blockhost.trestle.resources.ui_enter_a_valid_maximum_memory_value
import net.blockhost.trestle.resources.ui_enter_an_instance_name
import net.blockhost.trestle.resources.ui_enter_one_name_value_pair_per_line_lines_starting_with_are_ignored
import net.blockhost.trestle.resources.ui_environment_variables
import net.blockhost.trestle.resources.ui_game_components
import net.blockhost.trestle.resources.ui_group
import net.blockhost.trestle.resources.ui_identity
import net.blockhost.trestle.resources.ui_instance_settings
import net.blockhost.trestle.resources.ui_launch
import net.blockhost.trestle.resources.ui_leave_blank_to_list_under_all_instances
import net.blockhost.trestle.resources.ui_leave_blank_to_use_trestles_managed_mojang_runtime
import net.blockhost.trestle.resources.ui_loader
import net.blockhost.trestle.resources.ui_loading_client_settings
import net.blockhost.trestle.resources.ui_maximum_memory_mib
import net.blockhost.trestle.resources.ui_maximum_memory_must_be_at_least_minimum
import net.blockhost.trestle.resources.ui_memory_classpath_native_path_and_architecture_options_are_managed_by_tre
import net.blockhost.trestle.resources.ui_minecraft_client
import net.blockhost.trestle.resources.ui_minecraft_version
import net.blockhost.trestle.resources.ui_minimum_memory_mib
import net.blockhost.trestle.resources.ui_minimum_memory_must_be_greater_than_zero
import net.blockhost.trestle.resources.ui_mod_loader
import net.blockhost.trestle.resources.ui_must_exit_successfully_before_minecraft_starts
import net.blockhost.trestle.resources.ui_name
import net.blockhost.trestle.resources.ui_only_minecraft_version_is_supported_on_android
import net.blockhost.trestle.resources.ui_only_vanilla_is_supported_on_android
import net.blockhost.trestle.resources.ui_post_exit_command
import net.blockhost.trestle.resources.ui_pre_launch_command
import net.blockhost.trestle.resources.ui_quoted_values_and_escaped_characters_are_preserved
import net.blockhost.trestle.resources.ui_runs_after_minecraft_exits
import net.blockhost.trestle.resources.ui_runs_before_the_java_executable_for_example_gamescope
import net.blockhost.trestle.resources.ui_save_changes
import net.blockhost.trestle.resources.ui_use_active_account
import net.blockhost.trestle.resources.ui_use_recommended
import net.blockhost.trestle.resources.ui_wrapper_command
import net.blockhost.trestle.text.formattedString
import net.blockhost.trestle.text.resolve
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InstanceSettingsDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val form = state.instanceSettings
    TrestleDialog(
        onDismissRequest = { if (!form.isSaving) actions.closeInstanceSettings() },
        maxWidth = 620.dp,
        maxHeight = 820.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !form.isSaving, onDismiss = actions::closeInstanceSettings)
            .testTag(LauncherTestTags.INSTANCE_SETTINGS_DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        InstanceSettingsContent(
            state = state,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
            showHeader = true,
        )
    }
}

@Composable
internal fun InstanceSettingsContent(state: LauncherUiState, actions: LauncherUiActions, modifier: Modifier, showHeader: Boolean) {
    val form = state.instanceSettings
    val instance = form.instanceId?.let { id -> state.instances.firstOrNull { it.id == id } }
    var showIconEditor by rememberSaveable(form.instanceId?.value) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val javaPicker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        file?.let { actions.setJavaExecutable(it.path) }
    }
    val minimum = form.minimumMemoryMiB.toIntOrNull()
    val maximum = form.maximumMemoryMiB.toIntOrNull()
    val valid = form.name.isNotBlank() && form.minecraftVersionId.isNotBlank() &&
        minimum != null && maximum != null && minimum > 0 && maximum >= minimum
    val availableVersions = state.versions.map { it.id }
    val availableLoaders = ModLoader.entries.filter { loader ->
        state.supportedModLoaders == null || loader in state.supportedModLoaders
    }
    val accountChoices = listOf(stringResource(Res.string.ui_use_active_account) to null) + state.accounts
        .filter { it.profile.edition == MinecraftEdition.JAVA }
        .map { account ->
            "${account.profile.playerName} (${account.profile.profileId.takeLast(6)})" to account.profile.profileId
        }
    val componentsChanged = instance != null && (
        instance.minecraftVersionId != form.minecraftVersionId || instance.modLoader != form.modLoader
        )
    val minimumError = when {
        form.minimumMemoryMiB.isBlank() -> stringResource(Res.string.ui_enter_a_minimum_memory_value)
        minimum == null || minimum <= 0 -> stringResource(Res.string.ui_minimum_memory_must_be_greater_than_zero)
        else -> null
    }
    val maximumError = when {
        form.maximumMemoryMiB.isBlank() -> stringResource(Res.string.ui_enter_a_maximum_memory_value)
        maximum == null -> stringResource(Res.string.ui_enter_a_valid_maximum_memory_value)
        minimum != null && maximum < minimum -> stringResource(Res.string.ui_maximum_memory_must_be_at_least_minimum)
        else -> null
    }
    Column(modifier) {
        if (showHeader) {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_instance_settings),
                onClose = actions::closeInstanceSettings,
                closeEnabled = !form.isSaving,
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormSectionTitle(stringResource(Res.string.ui_identity))
            OutlinedTextField(
                value = form.name,
                onValueChange = actions::setInstanceName,
                label = { Text(stringResource(Res.string.ui_name)) },
                isError = form.name.isBlank(),
                supportingText = if (form.name.isBlank()) ({ Text(stringResource(Res.string.ui_enter_an_instance_name)) }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.group,
                onValueChange = actions::setInstanceGroup,
                label = { Text(stringResource(Res.string.ui_group)) },
                supportingText = { Text(stringResource(Res.string.ui_leave_blank_to_list_under_all_instances)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            instance?.let {
                InstanceIconSetting(
                    instance = it,
                    form = form,
                    onEdit = { showIconEditor = true },
                )
            }

            Spacer(Modifier.height(8.dp))
            FormSectionTitle(stringResource(Res.string.ui_game_components))
            if (state.supportedMinecraftVersions != null && availableVersions.size == 1) {
                FixedOption(
                    label = stringResource(Res.string.ui_minecraft_version),
                    value = availableVersions.single(),
                    supportingText = formattedString(
                        Res.string.ui_only_minecraft_version_is_supported_on_android,
                        availableVersions.single(),
                    ),
                )
            } else {
                Selector(
                    label = stringResource(Res.string.ui_minecraft_version),
                    value = form.minecraftVersionId,
                    values = availableVersions,
                    enabled = !state.isLoadingVersions,
                    onSelect = actions::setInstanceVersion,
                )
            }
            if (state.supportedModLoaders != null && availableLoaders.size == 1) {
                FixedOption(
                    label = stringResource(Res.string.ui_loader),
                    value = availableLoaders.single().label,
                    supportingText = stringResource(Res.string.ui_only_vanilla_is_supported_on_android),
                )
            } else {
                Selector(
                    label = stringResource(Res.string.ui_mod_loader),
                    value = form.modLoader.label,
                    values = availableLoaders.map { it.label },
                    onSelect = { selected ->
                        availableLoaders.firstOrNull { it.label == selected }?.let(actions::setInstanceLoader)
                    },
                )
            }
            if (componentsChanged) {
                Text(
                    stringResource(Res.string.ui_changing_the_game_version_or_loader_requires),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(8.dp))
            FormSectionTitle(stringResource(Res.string.ui_launch))
            Selector(
                label = stringResource(Res.string.ui_account),
                value = accountChoices.firstOrNull { it.second == form.accountProfileId }?.first
                    ?: stringResource(Res.string.ui_use_active_account),
                values = accountChoices.map { it.first },
                onSelect = { selected ->
                    actions.setInstanceAccount(accountChoices.firstOrNull { it.first == selected }?.second)
                },
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val memoryField: @Composable (Boolean, Modifier) -> Unit = { isMinimum, modifier ->
                    val error = if (isMinimum) minimumError else maximumError
                    OutlinedTextField(
                        value = if (isMinimum) form.minimumMemoryMiB else form.maximumMemoryMiB,
                        onValueChange = if (isMinimum) actions::setMinimumMemory else actions::setMaximumMemory,
                        label = {
                            Text(
                                if (isMinimum) {
                                    stringResource(
                                        Res.string.ui_minimum_memory_mib,
                                    )
                                } else {
                                    stringResource(Res.string.ui_maximum_memory_mib)
                                },
                            )
                        },
                        isError = error != null,
                        supportingText = if (error == null) null else ({ Text(error) }),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (isMinimum) ImeAction.Next else ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                            onDone = {
                                if (valid && !form.isLoadingClientSettings && !form.isSaving) {
                                    actions.saveInstanceSettings()
                                }
                            },
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
                Text(
                    form.recommendation?.resolve().orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions::applyRecommendedMemory) { Text(stringResource(Res.string.ui_use_recommended)) }
            }
            form.warnings.forEach { warning -> Text(warning.resolve(), color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(
                value = form.jvmArguments,
                onValueChange = actions::setJvmArguments,
                label = { Text(stringResource(Res.string.ui_additional_jvm_arguments)) },
                supportingText = {
                    Text(stringResource(Res.string.ui_memory_classpath_native_path_and_architecture_options_are_managed_by_tre))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.gameArguments,
                onValueChange = actions::setGameArguments,
                label = { Text(stringResource(Res.string.ui_additional_game_arguments)) },
                supportingText = { Text(stringResource(Res.string.ui_quoted_values_and_escaped_characters_are_preserved)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.supportsCustomJava) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val javaField: @Composable (Modifier) -> Unit = { fieldModifier ->
                        OutlinedTextField(
                            value = form.javaExecutable,
                            onValueChange = actions::setJavaExecutable,
                            label = { Text(stringResource(Res.string.ui_custom_java_executable)) },
                            supportingText = { Text(stringResource(Res.string.ui_leave_blank_to_use_trestles_managed_mojang_runtime)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    }
                    if (maxWidth < 440.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            javaField(Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { javaPicker.launch() }) { Text(stringResource(Res.string.ui_browse)) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            javaField(Modifier.weight(1f))
                            OutlinedButton(onClick = { javaPicker.launch() }) { Text(stringResource(Res.string.ui_browse)) }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = form.environmentVariables,
                onValueChange = actions::setEnvironmentVariables,
                label = { Text(stringResource(Res.string.ui_environment_variables)) },
                supportingText = {
                    Text(stringResource(Res.string.ui_enter_one_name_value_pair_per_line_lines_starting_with_are_ignored))
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.supportsLaunchCommands) {
                Spacer(Modifier.height(8.dp))
                FormSectionTitle(stringResource(Res.string.ui_custom_commands))
                Text(
                    stringResource(Res.string.ui_commands_run_directly_in_the_game_directory),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = form.preLaunchCommand,
                    onValueChange = actions::setPreLaunchCommand,
                    label = { Text(stringResource(Res.string.ui_pre_launch_command)) },
                    supportingText = { Text(stringResource(Res.string.ui_must_exit_successfully_before_minecraft_starts)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.wrapperCommand,
                    onValueChange = actions::setWrapperCommand,
                    label = { Text(stringResource(Res.string.ui_wrapper_command)) },
                    supportingText = { Text(stringResource(Res.string.ui_runs_before_the_java_executable_for_example_gamescope)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.postExitCommand,
                    onValueChange = actions::setPostExitCommand,
                    label = { Text(stringResource(Res.string.ui_post_exit_command)) },
                    supportingText = { Text(stringResource(Res.string.ui_runs_after_minecraft_exits)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
            FormSectionTitle(stringResource(Res.string.ui_minecraft_client))
            Text(
                stringResource(Res.string.ui_changes_are_written_to_this_instance_s),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            when {
                form.isLoadingClientSettings -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.ui_loading_client_settings), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                form.clientSettingsError != null -> Text(form.clientSettingsError.resolve(), color = MaterialTheme.colorScheme.error)

                form.clientSettings != null -> ClientSettingsFields(
                    form.clientSettings,
                    actions::setInstanceClientSettings,
                )
            }
        }
        TrestleDialogActions {
            TextButton(onClick = actions::closeInstanceSettings, enabled = !form.isSaving) {
                Text(stringResource(Res.string.ui_cancel))
            }
            Button(
                onClick = actions::saveInstanceSettings,
                enabled = valid && !form.isLoadingClientSettings && !form.isSaving,
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.ui_save_changes))
                }
            }
        }
    }
    if (showIconEditor && instance != null) {
        InstanceIconEditorDialog(
            instance = instance,
            reference = form.iconReference,
            pendingIcon = form.pendingIcon,
            onDismiss = { showIconEditor = false },
            onSave = { reference, pendingIcon ->
                if (pendingIcon == null) {
                    actions.setInstanceIconReference(reference)
                } else {
                    actions.setCustomInstanceIcon(pendingIcon.fileName, pendingIcon.bytes)
                }
                showIconEditor = false
            },
        )
    }
}

/** A form section title with a short accent bar, so long forms are easy to scan. */
@Composable
internal fun FormSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
