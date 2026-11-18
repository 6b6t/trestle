@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.MinecraftNarratorMode
import net.blockhost.trestle.instance.MinecraftParticleSetting
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ui_alphas
import net.blockhost.trestle.resources.ui_android_runtime
import net.blockhost.trestle.resources.ui_apply_these_defaults
import net.blockhost.trestle.resources.ui_audio
import net.blockhost.trestle.resources.ui_auto
import net.blockhost.trestle.resources.ui_auto_jump
import net.blockhost.trestle.resources.ui_betas
import net.blockhost.trestle.resources.ui_brightness
import net.blockhost.trestle.resources.ui_browse_modpacks
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_client_defaults
import net.blockhost.trestle.resources.ui_configure_first_launch_accessibility_audio_and_distance
import net.blockhost.trestle.resources.ui_controls_and_accessibility
import net.blockhost.trestle.resources.ui_create_instance
import net.blockhost.trestle.resources.ui_creating
import net.blockhost.trestle.resources.ui_custom
import net.blockhost.trestle.resources.ui_customize
import net.blockhost.trestle.resources.ui_direct_download_or_curseforge_url
import net.blockhost.trestle.resources.ui_enter_an_instance_name
import net.blockhost.trestle.resources.ui_entity_shadows
import net.blockhost.trestle.resources.ui_existing_ftb_app_library
import net.blockhost.trestle.resources.ui_field_of_view
import net.blockhost.trestle.resources.ui_filter_named_field
import net.blockhost.trestle.resources.ui_frame_rate_limit
import net.blockhost.trestle.resources.ui_fullscreen
import net.blockhost.trestle.resources.ui_group
import net.blockhost.trestle.resources.ui_gui_scale
import net.blockhost.trestle.resources.ui_hide
import net.blockhost.trestle.resources.ui_https_or_curseforge
import net.blockhost.trestle.resources.ui_icon_path_or_url
import net.blockhost.trestle.resources.ui_import
import net.blockhost.trestle.resources.ui_import_ftb_app_instances
import net.blockhost.trestle.resources.ui_import_modpack
import net.blockhost.trestle.resources.ui_invert_mouse
import net.blockhost.trestle.resources.ui_loader
import net.blockhost.trestle.resources.ui_loading
import net.blockhost.trestle.resources.ui_loading_versions
import net.blockhost.trestle.resources.ui_master_volume
import net.blockhost.trestle.resources.ui_minecraft_version
import net.blockhost.trestle.resources.ui_mouse_sensitivity
import net.blockhost.trestle.resources.ui_music_volume
import net.blockhost.trestle.resources.ui_name
import net.blockhost.trestle.resources.ui_narrator
import net.blockhost.trestle.resources.ui_new_instance
import net.blockhost.trestle.resources.ui_no_compatible_loader
import net.blockhost.trestle.resources.ui_no_matching_options
import net.blockhost.trestle.resources.ui_no_versions_available
import net.blockhost.trestle.resources.ui_only_minecraft_version_is_supported_on_android
import net.blockhost.trestle.resources.ui_only_vanilla_is_supported_on_android
import net.blockhost.trestle.resources.ui_optional
import net.blockhost.trestle.resources.ui_particles
import net.blockhost.trestle.resources.ui_render_distance
import net.blockhost.trestle.resources.ui_s_chunks
import net.blockhost.trestle.resources.ui_s_version
import net.blockhost.trestle.resources.ui_set_the_ftb_app_instances_folder_in
import net.blockhost.trestle.resources.ui_simulation_distance
import net.blockhost.trestle.resources.ui_snapshots
import net.blockhost.trestle.resources.ui_subtitles
import net.blockhost.trestle.resources.ui_supports_modrinth_curseforge_prism_and_multimc_pack_archives
import net.blockhost.trestle.resources.ui_trestle_will_use_balanced_defaults_for_the
import net.blockhost.trestle.resources.ui_unlimited
import net.blockhost.trestle.resources.ui_video
import net.blockhost.trestle.resources.ui_view_bobbing
import net.blockhost.trestle.resources.ui_vsync
import net.blockhost.trestle.resources.ui_write_these_settings_before_the_first_launch
import net.blockhost.trestle.text.labelResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun CreateInstanceDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val form = state.create
    val restrictedRuntime = state.supportedMinecraftVersions != null || state.supportedModLoaders != null
    val loaderChoices = ModLoader.entries
        .filter { state.supportedModLoaders == null || it in state.supportedModLoaders }
    val focusManager = LocalFocusManager.current
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var showSnapshots by rememberSaveable { mutableStateOf(false) }
    var showBetas by rememberSaveable { mutableStateOf(false) }
    var showAlphas by rememberSaveable { mutableStateOf(false) }
    var source by rememberSaveable { mutableStateOf("custom") }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    TrestleDialog(
        onDismissRequest = { if (!form.isSaving) actions.closeCreate() },
        maxWidth = 760.dp,
        maxHeight = 820.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !form.isSaving, onDismiss = actions::closeCreate)
            .testTag(LauncherTestTags.CREATE_DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_new_instance),
                onClose = actions::closeCreate,
                closeEnabled = !form.isSaving,
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!restrictedRuntime) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = source == "custom",
                            onClick = { source = "custom" },
                            shape = SegmentedButtonDefaults.itemShape(0, 3),
                            modifier = Modifier.weight(1f),
                        ) { ButtonLabel(stringResource(Res.string.ui_custom)) }
                        SegmentedButton(
                            selected = source == "import",
                            onClick = { source = "import" },
                            shape = SegmentedButtonDefaults.itemShape(1, 3),
                            modifier = Modifier.weight(1f),
                        ) { ButtonLabel(stringResource(Res.string.ui_import)) }
                        SegmentedButton(
                            selected = false,
                            onClick = {
                                actions.closeCreate()
                                // On the Discover page, switch its type instead of opening a dialog on top of it.
                                if (state.resourceBrowser.visible &&
                                    state.resourceBrowser.presentation == ResourceBrowserPresentation.PAGE
                                ) {
                                    actions.setResourceType(ResourceType.MODPACK)
                                } else {
                                    actions.openResourceBrowser(ResourceType.MODPACK)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(2, 3),
                            modifier = Modifier.weight(1f),
                        ) { ButtonLabel(stringResource(Res.string.ui_browse_modpacks)) }
                    }
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = actions::setCreateName,
                    label = { Text(stringResource(Res.string.ui_name)) },
                    isError = form.name.isBlank(),
                    supportingText = if (form.name.isBlank()) ({ Text(stringResource(Res.string.ui_enter_an_instance_name)) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = form.group,
                        onValueChange = actions::setCreateGroup,
                        label = { Text(stringResource(Res.string.ui_group)) },
                        supportingText = { Text(stringResource(Res.string.ui_optional)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.iconReference,
                        onValueChange = actions::setCreateIconReference,
                        label = { Text(stringResource(Res.string.ui_icon_path_or_url)) },
                        supportingText = { Text(stringResource(Res.string.ui_optional)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (source == "import") {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text(stringResource(Res.string.ui_direct_download_or_curseforge_url)) },
                        placeholder = { Text(stringResource(Res.string.ui_https_or_curseforge)) },
                        supportingText = {
                            Text(stringResource(Res.string.ui_supports_modrinth_curseforge_prism_and_multimc_pack_archives))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider()
                    Text(stringResource(Res.string.ui_existing_ftb_app_library), style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.launcherPreferences.ftbAppInstancesPath.ifBlank {
                            stringResource(Res.string.ui_set_the_ftb_app_instances_folder_in)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OutlinedButton(
                        onClick = actions::importFtbAppInstances,
                        enabled = state.launcherPreferences.ftbAppInstancesPath.isNotBlank() && !form.isSaving,
                    ) { Text(stringResource(Res.string.ui_import_ftb_app_instances)) }
                } else {
                    val visibleVersions = state.versions.filter { version ->
                        when (version.type) {
                            "release" -> true
                            "snapshot" -> showSnapshots
                            "old_beta" -> showBetas
                            "old_alpha" -> showAlphas
                            else -> false
                        }
                    }.take(500)
                    val versionLabels = visibleVersions.map { version ->
                        val type = when (version.type) {
                            "old_beta" -> "beta"
                            "old_alpha" -> "alpha"
                            else -> version.type
                        }
                        listOfNotNull(version.id, type, version.releaseTime?.substringBefore('T')).joinToString(" · ")
                    }
                    val selectedVersionLabel = visibleVersions.indexOfFirst { it.id == form.versionId }
                        .takeIf { it >= 0 }
                        ?.let(versionLabels::get)
                        ?: form.versionId
                    val versionChoices = visibleVersions.map { it.id }
                    if (restrictedRuntime && versionChoices.size == 1) {
                        FixedOption(
                            label = stringResource(Res.string.ui_minecraft_version),
                            value = versionChoices.single(),
                            supportingText = stringResource(
                                Res.string.ui_only_minecraft_version_is_supported_on_android,
                                versionChoices.single(),
                            ),
                            modifier = Modifier.testTag(LauncherTestTags.FIXED_CREATE_VERSION),
                        )
                    } else if (versionChoices.size == 1) {
                        OutlinedTextField(
                            value = versionChoices.single(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.ui_minecraft_version)) },
                            supportingText = { Text(stringResource(Res.string.ui_android_runtime)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Selector(
                            label = stringResource(Res.string.ui_minecraft_version),
                            value = selectedVersionLabel.ifBlank {
                                if (state.isLoadingVersions) {
                                    stringResource(
                                        Res.string.ui_loading_versions,
                                    )
                                } else {
                                    stringResource(Res.string.ui_no_versions_available)
                                }
                            },
                            values = versionLabels,
                            enabled = !state.isLoadingVersions,
                            onSelect = { label ->
                                visibleVersions.getOrNull(versionLabels.indexOf(label))?.id?.let(actions::setCreateVersion)
                            },
                        )
                    }
                    if (!restrictedRuntime) {
                        Row(
                            Modifier.fillMaxWidth().testTag(LauncherTestTags.CREATE_RELEASE_FILTERS),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = showSnapshots,
                                onClick = { showSnapshots = !showSnapshots },
                                label = { Text(stringResource(Res.string.ui_snapshots)) },
                            )
                            FilterChip(
                                selected = showBetas,
                                onClick = { showBetas = !showBetas },
                                label = { Text(stringResource(Res.string.ui_betas)) },
                            )
                            FilterChip(
                                selected = showAlphas,
                                onClick = { showAlphas = !showAlphas },
                                label = { Text(stringResource(Res.string.ui_alphas)) },
                            )
                        }
                    }
                    if (restrictedRuntime && loaderChoices.size == 1) {
                        FixedOption(
                            label = stringResource(Res.string.ui_loader),
                            value = loaderChoices.single().label,
                            supportingText = stringResource(Res.string.ui_only_vanilla_is_supported_on_android),
                            modifier = Modifier.testTag(LauncherTestTags.FIXED_CREATE_LOADER),
                        )
                    } else if (loaderChoices.size == 1) {
                        OutlinedTextField(
                            value = loaderChoices.single().label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.ui_loader)) },
                            supportingText = { Text(stringResource(Res.string.ui_only_vanilla_is_supported_on_android)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LoaderChoice(selected = form.modLoader, choices = loaderChoices, onSelect = actions::setCreateLoader)
                    }
                    if (form.modLoader != ModLoader.VANILLA) {
                        Selector(
                            label = stringResource(Res.string.ui_s_version, form.modLoader.label),
                            value =
                            form.loaderVersion
                                ?: if (form.isResolvingLoader) {
                                    stringResource(
                                        Res.string.ui_loading,
                                    )
                                } else {
                                    stringResource(Res.string.ui_no_compatible_loader)
                                },
                            values = form.loaderVersions,
                            enabled = !form.isResolvingLoader,
                            onSelect = actions::setCreateLoaderVersion,
                        )
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(Res.string.ui_client_defaults), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (showAdvanced) {
                                    stringResource(Res.string.ui_configure_first_launch_accessibility_audio_and_distance)
                                } else {
                                    stringResource(Res.string.ui_trestle_will_use_balanced_defaults_for_the)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) stringResource(Res.string.ui_hide) else stringResource(Res.string.ui_customize))
                        }
                    }
                    if (showAdvanced) ClientDefaultsFields(form, actions, showHeading = false)
                }
            }
            TrestleDialogActions {
                TextButton(onClick = actions::closeCreate, enabled = !form.isSaving) { Text(stringResource(Res.string.ui_cancel)) }
                Button(
                    onClick = {
                        if (source == "import") actions.importRemoteModpack(remoteUrl) else actions.createInstance()
                    },
                    enabled = !form.isSaving && if (source == "import") {
                        remoteUrl.isNotBlank()
                    } else {
                        form.name.isNotBlank() && form.versionId.isNotBlank() &&
                            (form.modLoader == ModLoader.VANILLA || form.loaderVersion != null)
                    },
                ) {
                    if (form.isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.ui_creating))
                    } else {
                        Text(
                            if (source ==
                                "import"
                            ) {
                                stringResource(Res.string.ui_import_modpack)
                            } else {
                                stringResource(Res.string.ui_create_instance)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDefaultsFields(form: CreateInstanceState, actions: LauncherUiActions, showHeading: Boolean = true) {
    val settings = form.clientSettings
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeading) {
            TrestleSwitchItem(
                label = stringResource(Res.string.ui_client_defaults),
                checked = form.preconfigureClientSettings,
                supportingText = stringResource(Res.string.ui_write_these_settings_before_the_first_launch),
                onCheckedChange = actions::setCreateClientPreconfiguration,
            )
        } else {
            TrestleSwitchItem(
                label = stringResource(Res.string.ui_apply_these_defaults),
                checked = form.preconfigureClientSettings,
                onCheckedChange = actions::setCreateClientPreconfiguration,
            )
        }
        if (!form.preconfigureClientSettings) return@Column

        ClientSettingsFields(settings, actions::setCreateClientSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientSettingsFields(settings: MinecraftClientSettings, onSettingsChange: (MinecraftClientSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.ui_audio), style = MaterialTheme.typography.titleSmall)
        PercentageSlider(
            label = stringResource(Res.string.ui_master_volume),
            value = settings.masterVolumePercent,
            onValueChange = { onSettingsChange(settings.copy(masterVolumePercent = it)) },
        )
        PercentageSlider(
            label = stringResource(Res.string.ui_music_volume),
            value = settings.musicVolumePercent,
            onValueChange = { onSettingsChange(settings.copy(musicVolumePercent = it)) },
        )

        HorizontalDivider()
        Text(stringResource(Res.string.ui_video), style = MaterialTheme.typography.titleSmall)
        IntegerSlider(
            label = stringResource(Res.string.ui_field_of_view),
            valueLabel = "${settings.fieldOfViewDegrees}°",
            value = settings.fieldOfViewDegrees,
            range = 30..110,
            onValueChange = { onSettingsChange(settings.copy(fieldOfViewDegrees = it)) },
        )
        PercentageSlider(
            label = stringResource(Res.string.ui_brightness),
            value = settings.brightnessPercent,
            onValueChange = { onSettingsChange(settings.copy(brightnessPercent = it)) },
        )
        IntegerSlider(
            label = stringResource(Res.string.ui_frame_rate_limit),
            valueLabel = if (settings.maximumFrameRate ==
                260
            ) {
                stringResource(Res.string.ui_unlimited)
            } else {
                "${settings.maximumFrameRate} FPS"
            },
            value = settings.maximumFrameRate,
            range = 10..260,
            steps = 24,
            onValueChange = { onSettingsChange(settings.copy(maximumFrameRate = it)) },
        )
        IntegerSlider(
            label = stringResource(Res.string.ui_gui_scale),
            valueLabel = if (settings.guiScale == 0) stringResource(Res.string.ui_auto) else "${settings.guiScale}x",
            value = settings.guiScale,
            range = 0..8,
            onValueChange = { onSettingsChange(settings.copy(guiScale = it)) },
        )
        ChunkDistanceSlider(
            label = stringResource(Res.string.ui_render_distance),
            value = settings.renderDistanceChunks,
            range = 2..32,
            onValueChange = { onSettingsChange(settings.copy(renderDistanceChunks = it)) },
        )
        ChunkDistanceSlider(
            label = stringResource(Res.string.ui_simulation_distance),
            value = settings.simulationDistanceChunks,
            range = 5..32,
            onValueChange = { onSettingsChange(settings.copy(simulationDistanceChunks = it)) },
        )
        val particleLabels = MinecraftParticleSetting.entries.associateWith { stringResource(it.labelResource) }
        Selector(
            label = stringResource(Res.string.ui_particles),
            value = particleLabels.getValue(settings.particles),
            values = particleLabels.values.toList(),
            onSelect = { label ->
                onSettingsChange(
                    settings.copy(particles = particleLabels.entries.first { it.value == label }.key),
                )
            },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_fullscreen),
            checked = settings.fullscreen,
            onCheckedChange = { onSettingsChange(settings.copy(fullscreen = it)) },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_vsync),
            checked = settings.enableVsync,
            onCheckedChange = { onSettingsChange(settings.copy(enableVsync = it)) },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_view_bobbing),
            checked = settings.viewBobbing,
            onCheckedChange = { onSettingsChange(settings.copy(viewBobbing = it)) },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_entity_shadows),
            checked = settings.entityShadows,
            onCheckedChange = { onSettingsChange(settings.copy(entityShadows = it)) },
        )

        HorizontalDivider()
        Text(stringResource(Res.string.ui_controls_and_accessibility), style = MaterialTheme.typography.titleSmall)
        PercentageSlider(
            label = stringResource(Res.string.ui_mouse_sensitivity),
            value = settings.mouseSensitivityPercent,
            onValueChange = { onSettingsChange(settings.copy(mouseSensitivityPercent = it)) },
        )
        val narratorLabels = MinecraftNarratorMode.entries.associateWith { stringResource(it.labelResource) }
        Selector(
            label = stringResource(Res.string.ui_narrator),
            value = narratorLabels.getValue(settings.narratorMode),
            values = narratorLabels.values.toList(),
            onSelect = { label ->
                onSettingsChange(
                    settings.copy(narratorMode = narratorLabels.entries.first { it.value == label }.key),
                )
            },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_invert_mouse),
            checked = settings.invertMouse,
            onCheckedChange = { onSettingsChange(settings.copy(invertMouse = it)) },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_auto_jump),
            checked = settings.autoJump,
            onCheckedChange = { onSettingsChange(settings.copy(autoJump = it)) },
        )
        ClientSettingSwitch(
            label = stringResource(Res.string.ui_subtitles),
            checked = settings.showSubtitles,
            onCheckedChange = { onSettingsChange(settings.copy(showSubtitles = it)) },
        )
    }
}

@Composable
private fun PercentageSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    SliderSetting(label, "$value%") {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}

@Composable
private fun ChunkDistanceSlider(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    IntegerSlider(label, stringResource(Res.string.ui_s_chunks, value), value, range, onValueChange = onValueChange)
}

@Composable
internal fun IntegerSlider(
    label: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    steps: Int = range.last - range.first - 1,
    onValueChange: (Int) -> Unit,
) {
    SliderSetting(label, valueLabel) {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
        )
    }
}

@Composable
private fun SliderSetting(label: String, value: String, slider: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
        slider()
    }
}

@Composable
private fun ClientSettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    TrestleSwitchItem(
        label = label,
        checked = checked,
        modifier = Modifier.fillMaxWidth(),
        onCheckedChange = onCheckedChange,
    )
}

@Composable
internal fun FixedOption(label: String, value: String, supportingText: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
        Text(
            supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Selector(
    label: String,
    value: String,
    values: List<String>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember(label) { mutableStateOf(false) }
    var filter by remember(label) { mutableStateOf("") }
    val canOpen = enabled && values.isNotEmpty()
    val visibleValues = if (filter.isBlank()) values else values.filter { it.contains(filter, ignoreCase = true) }
    LaunchedEffect(canOpen, values) {
        if (!canOpen) expanded = false
        filter = ""
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (canOpen) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(
                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = canOpen,
            ).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                filter = ""
            },
        ) {
            if (values.size > 20) {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text(stringResource(Res.string.ui_filter_named_field, label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            visibleValues.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        filter = ""
                        onSelect(item)
                    },
                )
            }
            if (visibleValues.isEmpty()) {
                Text(
                    stringResource(Res.string.ui_no_matching_options),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Shows every loader at once as a choice chip, so players can compare them without opening a menu. */
@Composable
private fun LoaderChoice(selected: ModLoader, choices: List<ModLoader>, onSelect: (ModLoader) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.ui_loader), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { loader ->
                FilterChip(
                    selected = loader == selected,
                    onClick = { onSelect(loader) },
                    label = { Text(loader.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}
