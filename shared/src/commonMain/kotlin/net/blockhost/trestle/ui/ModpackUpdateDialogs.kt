package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_apply_pack_change
import net.blockhost.trestle.resources.ui_back_up_and_update
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_check_pack_updates
import net.blockhost.trestle.resources.ui_install_separate_copy
import net.blockhost.trestle.resources.ui_minecraft_changes_to_s_back_up_your
import net.blockhost.trestle.resources.ui_pack_already_installed
import net.blockhost.trestle.resources.ui_pack_already_installed_explanation
import net.blockhost.trestle.resources.ui_pack_backup_explanation
import net.blockhost.trestle.resources.ui_pack_page
import net.blockhost.trestle.resources.ui_restore_previous_pack_version
import net.blockhost.trestle.resources.ui_roll_back
import net.blockhost.trestle.resources.ui_roll_back_pack_update
import net.blockhost.trestle.resources.ui_rollback_explanation
import net.blockhost.trestle.resources.ui_s_pack_file_changes_worlds_personal_settings
import net.blockhost.trestle.resources.ui_update_named
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModpackOriginPanel(instance: GameInstance, state: LauncherUiState, actions: LauncherUiActions) {
    val origin = instance.modpackOrigin ?: return
    val uri = LocalUriHandler.current
    var confirmRollback by remember(instance.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${origin.name} ${origin.versionName}", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            origin.websiteUrl?.takeIf { it.startsWith("https://") }?.let { url ->
                TextButton(onClick = { uri.openUri(url) }) { Text(stringResource(Res.string.ui_pack_page)) }
            }
            OutlinedButton(onClick = actions::checkModpackUpdate, enabled = state.operation == null) {
                Text(stringResource(Res.string.ui_check_pack_updates))
            }
            if (state.canRollbackModpack) {
                TextButton(onClick = {
                    confirmRollback = true
                }, enabled = state.operation == null) { Text(stringResource(Res.string.ui_roll_back_pack_update)) }
            }
        }
    }
    if (confirmRollback) {
        AlertDialog(
            onDismissRequest = { confirmRollback = false },
            title = { Text(stringResource(Res.string.ui_restore_previous_pack_version)) },
            text = {
                Text(stringResource(Res.string.ui_rollback_explanation))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRollback = false
                    actions.rollbackModpackUpdate()
                }) { Text(stringResource(Res.string.ui_roll_back)) }
            },
            dismissButton = { TextButton(onClick = { confirmRollback = false }) { Text(stringResource(Res.string.ui_cancel)) } },
        )
    }
}

@Composable
internal fun ModpackUpdateDialogs(state: LauncherUiState, actions: LauncherUiActions) {
    if (state.suggestedPackInstances.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = actions::cancelModpackUpdate,
            title = { Text(stringResource(Res.string.ui_pack_already_installed)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.ui_pack_already_installed_explanation))
                    state.suggestedPackInstances.forEach { instance ->
                        OutlinedButton(onClick = { actions.previewSelectedModpackUpdate(instance.id) }) {
                            Text("${instance.displayName} (${instance.modpackOrigin?.versionName})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = actions::installSelectedModpackAsNew) { Text(stringResource(Res.string.ui_install_separate_copy)) }
            },
            dismissButton = { TextButton(onClick = actions::cancelModpackUpdate) { Text(stringResource(Res.string.ui_cancel)) } },
        )
    }
    val preview = state.modpackUpdatePreview ?: return
    var replace by remember(preview) { mutableStateOf(emptySet<String>()) }
    AlertDialog(
        onDismissRequest = actions::cancelModpackUpdate,
        title = { Text(stringResource(Res.string.ui_update_named, preview.original.displayName)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${preview.original.modpackOrigin?.versionName} → ${preview.candidate.modpackOrigin?.versionName}")
                Text(
                    stringResource(Res.string.ui_s_pack_file_changes_worlds_personal_settings, preview.changes.size),
                )
                if (preview.original.minecraftVersionId != preview.candidate.minecraftVersionId) {
                    Text(
                        stringResource(Res.string.ui_minecraft_changes_to_s_back_up_your, preview.candidate.minecraftVersionId),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                preview.changes.forEach { change ->
                    PackChangeRow(change.action, change.path, change.conflict, change.path in replace) { checked ->
                        replace = if (checked) replace + change.path else replace - change.path
                    }
                }
                Text(stringResource(Res.string.ui_pack_backup_explanation))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                actions.applyModpackUpdate(replace)
            }, enabled = state.operation == null) { Text(stringResource(Res.string.ui_back_up_and_update)) }
        },
        dismissButton = { TextButton(onClick = actions::cancelModpackUpdate) { Text(stringResource(Res.string.ui_cancel)) } },
    )
}

@Composable
private fun PackChangeRow(action: String, path: String, conflict: Boolean, replace: Boolean, onReplaceChange: (Boolean) -> Unit) {
    Column {
        Text("$action: $path", style = MaterialTheme.typography.bodyMedium)
        if (conflict) {
            Row(
                Modifier.toggleable(value = replace, role = Role.Checkbox, onValueChange = onReplaceChange)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = replace, onCheckedChange = null)
                Text(stringResource(Res.string.ui_apply_pack_change))
            }
        }
    }
}
