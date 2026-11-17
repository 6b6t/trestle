@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.vinceglb.filekit.size
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ui_add_file
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_choose_how_trestle_should_use_this_zip_file
import net.blockhost.trestle.resources.ui_import_local_file
import net.blockhost.trestle.resources.ui_import_modpack
import net.blockhost.trestle.resources.ui_select_an_instance_before_adding_this_file
import net.blockhost.trestle.resources.ui_target_s
import net.blockhost.trestle.resources.ui_the_modpack_creates_a_new_instance
import net.blockhost.trestle.text.labelResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LocalFileImportDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val pending = state.localFileImport
    val selectedType = pending.selectedType
    val target = pending.targetInstanceId?.let { id -> state.instances.firstOrNull { it.id == id } }
    TrestleDialog(
        onDismissRequest = actions::cancelLocalFileImport,
        maxWidth = 480.dp,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Escape -> {
                    actions.cancelLocalFileImport()
                    true
                }

                Key.Enter -> {
                    if (selectedType != null) actions.confirmLocalFileImport()
                    selectedType != null
                }

                else -> false
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_import_local_file),
                onClose = actions::cancelLocalFileImport,
            )
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    pending.fileName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                pending.sourceOrigin?.let { origin ->
                    Text(
                        origin,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (pending.allowedTypes.size > 1) {
                    Text(stringResource(Res.string.ui_choose_how_trestle_should_use_this_zip_file))
                    Column {
                        pending.allowedTypes.forEach { type ->
                            Row(
                                Modifier.fillMaxWidth().selectable(
                                    selected = selectedType == type,
                                    role = Role.RadioButton,
                                    onClick = { actions.setLocalFileImportType(type) },
                                ).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = selectedType == type, onClick = null)
                                Text(stringResource(type.labelResource))
                            }
                        }
                    }
                }
                if (selectedType != ResourceType.MODPACK) {
                    Text(
                        target?.let { stringResource(Res.string.ui_target_s, it.displayName) }
                            ?: stringResource(Res.string.ui_select_an_instance_before_adding_this_file),
                        color = if (target == null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                } else {
                    Text(
                        stringResource(Res.string.ui_the_modpack_creates_a_new_instance),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TrestleDialogActions {
                TextButton(onClick = actions::cancelLocalFileImport) { Text(stringResource(Res.string.ui_cancel)) }
                Button(
                    onClick = actions::confirmLocalFileImport,
                    enabled = selectedType != null &&
                        (selectedType == ResourceType.MODPACK || target?.installationState is InstallationState.Installed),
                ) {
                    Text(
                        if (selectedType ==
                            ResourceType.MODPACK
                        ) {
                            stringResource(Res.string.ui_import_modpack)
                        } else {
                            stringResource(Res.string.ui_add_file)
                        },
                    )
                }
            }
        }
    }
}
