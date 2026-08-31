package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.domain.GameInstance

@Composable
internal fun ModpackOriginPanel(instance: GameInstance, state: LauncherUiState, actions: LauncherUiActions) {
    val origin = instance.modpackOrigin ?: return
    val uri = LocalUriHandler.current
    var confirmRollback by remember(instance.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${origin.name} ${origin.versionName}", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            origin.websiteUrl?.takeIf { it.startsWith("https://") }?.let { url ->
                TextButton(onClick = { uri.openUri(url) }) { Text("Pack page") }
            }
            OutlinedButton(onClick = actions::checkModpackUpdate, enabled = state.operation == null) { Text("Check pack updates") }
            if (state.canRollbackModpack) TextButton(onClick = { confirmRollback = true }, enabled = state.operation == null) { Text("Roll back pack update") }
        }
    }
    if (confirmRollback) AlertDialog(
        onDismissRequest = { confirmRollback = false },
        title = { Text("Restore the previous pack version?") },
        text = { Text("Worlds and personal settings stay in place. Rollback will stop if an updated file has changed since installation.") },
        confirmButton = { TextButton(onClick = { confirmRollback = false; actions.rollbackModpackUpdate() }) { Text("Roll back") } },
        dismissButton = { TextButton(onClick = { confirmRollback = false }) { Text("Cancel") } },
    )
}

@Composable
internal fun ModpackUpdateDialogs(state: LauncherUiState, actions: LauncherUiActions) {
    if (state.suggestedPackInstances.isNotEmpty()) AlertDialog(
        onDismissRequest = actions::cancelModpackUpdate,
        title = { Text("This pack is already installed") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preview an update to an existing instance, or install a separate copy.")
                state.suggestedPackInstances.forEach { instance ->
                    OutlinedButton(onClick = { actions.previewSelectedModpackUpdate(instance.id) }) {
                        Text("${instance.displayName} (${instance.modpackOrigin?.versionName})")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = actions::installSelectedModpackAsNew) { Text("Install separate copy") } },
        dismissButton = { TextButton(onClick = actions::cancelModpackUpdate) { Text("Cancel") } },
    )
    val preview = state.modpackUpdatePreview ?: return
    var replace by remember(preview) { mutableStateOf(emptySet<String>()) }
    AlertDialog(
        onDismissRequest = actions::cancelModpackUpdate,
        title = { Text("Update ${preview.original.displayName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${preview.original.modpackOrigin?.versionName} → ${preview.candidate.modpackOrigin?.versionName}")
                Text("${preview.changes.size} pack file changes. Worlds, personal settings, and files you added are preserved unless they conflict with a pack file below. Modified pack files stay unchanged by default.")
                if (preview.original.minecraftVersionId != preview.candidate.minecraftVersionId) {
                    Text("Minecraft changes to ${preview.candidate.minecraftVersionId}. Back up your worlds before playing; opening a world in a newer game version can prevent downgrading.", color = MaterialTheme.colorScheme.error)
                }
                preview.changes.forEach { change ->
                    Column {
                        Text("${change.action}: ${change.path}", style = MaterialTheme.typography.bodyMedium)
                        if (change.conflict) Row {
                            Checkbox(checked = change.path in replace, onCheckedChange = { checked ->
                                replace = if (checked) replace + change.path else replace - change.path
                            })
                            Text("Apply pack change to my modified file", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                Text("Trestle keeps one backup of changed files. Applying another update replaces that rollback backup.")
            }
        },
        confirmButton = { TextButton(onClick = { actions.applyModpackUpdate(replace) }, enabled = state.operation == null) { Text("Back up and update") } },
        dismissButton = { TextButton(onClick = actions::cancelModpackUpdate) { Text("Cancel") } },
    )
}
