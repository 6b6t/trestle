@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ui_add_new_skin
import net.blockhost.trestle.resources.ui_back
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_change_how_s_looks_in_game
import net.blockhost.trestle.resources.ui_choose_a_skin_file
import net.blockhost.trestle.resources.ui_choose_a_skin_png
import net.blockhost.trestle.resources.ui_choose_png
import net.blockhost.trestle.resources.ui_classic
import net.blockhost.trestle.resources.ui_current
import net.blockhost.trestle.resources.ui_delete
import net.blockhost.trestle.resources.ui_drag_to_rotate
import net.blockhost.trestle.resources.ui_drop_png_to_use_this_skin
import net.blockhost.trestle.resources.ui_edit
import net.blockhost.trestle.resources.ui_edit_skin
import net.blockhost.trestle.resources.ui_import_a_64x64_or_legacy_64x32_png_to_start_your_local_library
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_loading_skin_preview
import net.blockhost.trestle.resources.ui_name
import net.blockhost.trestle.resources.ui_new_skin
import net.blockhost.trestle.resources.ui_no_account
import net.blockhost.trestle.resources.ui_no_custom_skin
import net.blockhost.trestle.resources.ui_no_saved_skins
import net.blockhost.trestle.resources.ui_player_model
import net.blockhost.trestle.resources.ui_remove_named_skin
import net.blockhost.trestle.resources.ui_remove_skin
import net.blockhost.trestle.resources.ui_replace_png
import net.blockhost.trestle.resources.ui_reset_skin
import net.blockhost.trestle.resources.ui_reset_skin_explanation
import net.blockhost.trestle.resources.ui_reset_skin_question
import net.blockhost.trestle.resources.ui_reset_to_default
import net.blockhost.trestle.resources.ui_s_model_drag_to_rotate
import net.blockhost.trestle.resources.ui_save
import net.blockhost.trestle.resources.ui_save_and_use
import net.blockhost.trestle.resources.ui_save_to_library
import net.blockhost.trestle.resources.ui_saving_2
import net.blockhost.trestle.resources.ui_select_skin
import net.blockhost.trestle.resources.ui_skin_could_not_load
import net.blockhost.trestle.resources.ui_skin_file
import net.blockhost.trestle.resources.ui_skins
import net.blockhost.trestle.resources.ui_slim
import net.blockhost.trestle.resources.ui_this_deletes_the_local_skin_profile_and_its_png_your_active_minecraft_sk
import net.blockhost.trestle.resources.ui_use_a_64x64_png_or_a_legacy_64x32_skin
import net.blockhost.trestle.resources.ui_use_skin
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
internal fun SkinStudioPage(state: LauncherUiState, actions: LauncherUiActions, modifier: Modifier) {
    val account = state.accounts.firstOrNull { it.isActive }
    val selected = state.savedSkins.firstOrNull { it.profile.id == state.skinStudio.selectedProfileId }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    if (confirmReset) {
        ConfirmationDialog(
            ConfirmationRequest(
                title = stringResource(Res.string.ui_reset_skin_question),
                text = stringResource(Res.string.ui_reset_skin_explanation),
                confirmLabel = stringResource(Res.string.ui_reset_skin),
            ) { actions.resetActiveSkin() },
        ) { confirmReset = false }
    }
    Column(
        modifier
            .fillMaxSize()
            .dismissOnEscape(onDismiss = actions::closeSkinStudio)
            .testTag(LauncherTestTags.SKIN_STUDIO),
    ) {
        PageHero(
            title = stringResource(Res.string.ui_skins),
            subtitle = account?.let { stringResource(Res.string.ui_change_how_s_looks_in_game, it.profile.playerName) },
            accent = MaterialTheme.colorScheme.tertiary,
            navigationIcon = {
                TrestleTooltipIconButton(
                    label = stringResource(Res.string.ui_back),
                    onClick = actions::closeSkinStudio,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.ui_back),
                    )
                }
            },
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize()) {
                    CurrentSkinPanel(
                        playerName = account?.profile?.playerName.orEmpty(),
                        texture = account?.let { state.accountSkinTextures[it.profile.profileId] },
                        variant = account?.profile?.skin?.variant ?: SkinVariant.CLASSIC,
                        onSave = actions::saveCurrentSkinToLibrary,
                        onReset = { confirmReset = true },
                        emptyLabel = skinPreviewLabel(state, account),
                        modifier = Modifier.width(320.dp).fillMaxHeight(),
                    )
                    SkinLibraryPanel(
                        skins = state.savedSkins,
                        selected = selected,
                        onSelect = actions::selectSavedSkin,
                        onNew = actions::openNewSkin,
                        onUse = actions::useSelectedSkin,
                        onEdit = actions::editSelectedSkin,
                        onDelete = { confirmDelete = true },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    CurrentSkinPanel(
                        playerName = account?.profile?.playerName.orEmpty(),
                        texture = account?.let { state.accountSkinTextures[it.profile.profileId] },
                        variant = account?.profile?.skin?.variant ?: SkinVariant.CLASSIC,
                        onSave = actions::saveCurrentSkinToLibrary,
                        onReset = { confirmReset = true },
                        emptyLabel = skinPreviewLabel(state, account),
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                    )
                    SkinLibraryPanel(
                        skins = state.savedSkins,
                        selected = selected,
                        onSelect = actions::selectSavedSkin,
                        onNew = actions::openNewSkin,
                        onUse = actions::useSelectedSkin,
                        onEdit = actions::editSelectedSkin,
                        onDelete = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
    if (confirmDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(stringResource(Res.string.ui_remove_named_skin, selected.profile.name))
            },
            text = {
                Text(stringResource(Res.string.ui_this_deletes_the_local_skin_profile_and_its_png_your_active_minecraft_sk))
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.ui_cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.deleteSelectedSkin()
                        confirmDelete = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.ui_remove_skin)) }
            },
        )
    }
}

@Composable
private fun CurrentSkinPanel(
    playerName: String,
    texture: ByteArray?,
    variant: SkinVariant,
    onSave: () -> Unit,
    onReset: () -> Unit,
    emptyLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceContainer),
                ),
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.ui_current),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start),
        )
        MinecraftSkinPreview(
            texture = texture,
            variant = variant,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
            emptyLabel = emptyLabel,
            background = Color.Transparent,
        )
        Text(playerName, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.ui_s_model_drag_to_rotate, variant.label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(onClick = onSave, enabled = texture != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.ui_save_to_library))
            }
            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.ui_reset_to_default)) }
        }
    }
}

@Composable
internal fun AccountMark(account: ManagedAccount, texture: ByteArray?, compact: Boolean = false) {
    if (account.profile.edition == MinecraftEdition.JAVA && texture != null) {
        MinecraftSkinPreview(
            texture = texture,
            variant = account.profile.skin?.variant ?: SkinVariant.CLASSIC,
            modifier = if (compact) Modifier.size(width = 44.dp, height = 64.dp) else Modifier.size(width = 56.dp, height = 80.dp),
            interactive = false,
            animate = false,
        )
        return
    }
    Box(
        Modifier.size(48.dp).background(
            if (account.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.shapes.small,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            account.profile.playerName.take(1).uppercase(),
            color = if (account.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun SkinLibraryPanel(
    skins: List<SavedSkin>,
    selected: SavedSkin?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.ui_library), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Button(onClick = onNew) { Text(stringResource(Res.string.ui_new_skin)) }
        }
        Spacer(Modifier.height(16.dp))
        if (skins.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.ui_no_saved_skins), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(Res.string.ui_import_a_64x64_or_legacy_64x32_png_to_start_your_local_library),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onNew) { Text(stringResource(Res.string.ui_choose_a_skin_file)) }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItems(skins, key = { it.profile.id }) { skin ->
                    SkinLibraryItem(
                        skin = skin,
                        selected = skin.profile.id == selected?.profile?.id,
                        onClick = { onSelect(skin.profile.id) },
                        onDoubleClick = {
                            onSelect(skin.profile.id)
                            onUse()
                        },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDelete, enabled = selected != null) { Text(stringResource(Res.string.ui_delete)) }
                TextButton(onClick = onEdit, enabled = selected != null) { Text(stringResource(Res.string.ui_edit)) }
                Button(
                    onClick = onUse,
                    enabled = selected != null,
                ) { Text(stringResource(Res.string.ui_use_skin)) }
            }
        }
    }
}

@Composable
private fun SkinLibraryItem(skin: SavedSkin, selected: Boolean, onClick: () -> Unit, onDoubleClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .trestleSelectable(
                selected = selected,
                onClickLabel = stringResource(Res.string.ui_select_skin),
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MinecraftSkinPreview(
                texture = skin.texture,
                variant = skin.profile.variant,
                modifier = Modifier.fillMaxWidth().height(142.dp),
                interactive = false,
                animate = false,
            )
            Text(
                skin.profile.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                skin.profile.variant.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun SkinEditorDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val editor = state.skinStudio.editor
    val scope = rememberCoroutineScope()
    var dropActive by remember { mutableStateOf(false) }
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("png")),
    ) { file ->
        if (file != null) {
            scope.launch {
                runCatching { file.readBytes() }
                    .onSuccess { actions.setSkinFile(file.name, it) }
                    .onFailure { actions.reportSkinFileReadFailure() }
            }
        }
    }
    TrestleDialog(
        onDismissRequest = actions::closeSkinEditor,
        maxWidth = 820.dp,
        widthFraction = 0.88f,
        minHeight = 500.dp,
        maxHeight = 650.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !editor.isSaving, onDismiss = actions::closeSkinEditor)
            .localFileDropTarget(
                enabled = !editor.isSaving && currentPlatform == "Desktop",
                extensions = setOf("png"),
                onActiveChange = { dropActive = it },
                onFiles = { files ->
                    files.firstOrNull()?.let { actions.setSkinFile(it.name, it.bytes) }
                },
                onFailure = { actions.reportSkinFileReadFailure() },
            )
            .testTag(LauncherTestTags.SKIN_EDITOR_DIALOG),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TrestleDialogHeader(
                    title = if (editor.profileId ==
                        null
                    ) {
                        stringResource(Res.string.ui_add_new_skin)
                    } else {
                        stringResource(Res.string.ui_edit_skin)
                    },
                    onClose = actions::closeSkinEditor,
                    closeEnabled = !editor.isSaving,
                )
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val compact = maxWidth < 650.dp
                    if (compact) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                            SkinEditorPreview(editor)
                            Spacer(Modifier.height(20.dp))
                            SkinEditorFields(editor, { picker.launch() }, actions)
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            SkinEditorPreview(editor, Modifier.width(300.dp).fillMaxHeight().padding(24.dp))
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SkinEditorFields(
                                editor = editor,
                                onBrowse = { picker.launch() },
                                actions = actions,
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                            )
                        }
                    }
                }
                TrestleDialogActions {
                    TextButton(onClick = actions::closeSkinEditor, enabled = !editor.isSaving) {
                        Text(stringResource(Res.string.ui_cancel))
                    }
                    OutlinedButton(
                        onClick = { actions.saveSkin(useAfterSave = false) },
                        enabled = editor.canSave,
                    ) { Text(stringResource(Res.string.ui_save)) }
                    Button(
                        onClick = { actions.saveSkin(useAfterSave = true) },
                        enabled = editor.canSave,
                    ) { Text(if (editor.isSaving) stringResource(Res.string.ui_saving_2) else stringResource(Res.string.ui_save_and_use)) }
                }
            }
            if (dropActive) DropOverlay(stringResource(Res.string.ui_drop_png_to_use_this_skin))
        }
    }
}

@Composable
private fun SkinEditorPreview(editor: SkinEditorState, modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MinecraftSkinPreview(
            texture = editor.texture,
            variant = editor.variant,
            modifier = Modifier.fillMaxWidth().weight(1f),
            emptyLabel = stringResource(Res.string.ui_choose_a_skin_png),
        )
        Text(
            stringResource(Res.string.ui_drag_to_rotate),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SkinEditorFields(editor: SkinEditorState, onBrowse: () -> Unit, actions: LauncherUiActions, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = editor.name,
            onValueChange = actions::setSkinName,
            label = { Text(stringResource(Res.string.ui_name)) },
            singleLine = true,
            enabled = !editor.isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (editor.canSave) actions.saveSkin(useAfterSave = true) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.ui_player_model), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SkinVariant.entries.forEach { variant ->
                    Row(
                        Modifier.selectable(
                            selected = editor.variant == variant,
                            enabled = !editor.isSaving,
                            role = Role.RadioButton,
                            onClick = { actions.setSkinVariant(variant) },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = editor.variant == variant,
                            onClick = null,
                            enabled = !editor.isSaving,
                        )
                        Text(variant.label)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.ui_skin_file), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onBrowse, enabled = !editor.isSaving) {
                Text(if (editor.texture == null) stringResource(Res.string.ui_choose_png) else stringResource(Res.string.ui_replace_png))
            }
            editor.sourceFileName?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                stringResource(Res.string.ui_use_a_64x64_png_or_a_legacy_64x32_skin),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        editor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

internal val SkinVariant.label: String
    @Composable get() = when (this) {
        SkinVariant.CLASSIC -> stringResource(Res.string.ui_classic)
        SkinVariant.SLIM -> stringResource(Res.string.ui_slim)
    }

private val SkinEditorState.canSave: Boolean
    get() = name.isNotBlank() && texture != null && !isSaving

/** What a skin preview says while it has no texture: loading, no custom skin, or a failed download. */
@Composable
internal fun skinPreviewLabel(state: LauncherUiState, account: ManagedAccount?): String = when {
    account == null -> stringResource(Res.string.ui_no_account)
    account.profile.skin == null -> stringResource(Res.string.ui_no_custom_skin)
    account.profile.profileId in state.failedSkinTextures -> stringResource(Res.string.ui_skin_could_not_load)
    else -> stringResource(Res.string.ui_loading_skin_preview)
}
