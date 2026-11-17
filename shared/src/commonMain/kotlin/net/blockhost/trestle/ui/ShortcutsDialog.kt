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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_arrow_keys
import net.blockhost.trestle.resources.ui_close_or_go_back
import net.blockhost.trestle.resources.ui_import_a_file
import net.blockhost.trestle.resources.ui_keyboard_shortcuts
import net.blockhost.trestle.resources.ui_move_instance_focus
import net.blockhost.trestle.resources.ui_new_instance
import net.blockhost.trestle.resources.ui_open_focused_instance
import net.blockhost.trestle.resources.ui_remove_focused_instance
import net.blockhost.trestle.resources.ui_search
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_show_shortcuts
import net.blockhost.trestle.resources.ui_switch_section
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ShortcutsDialog(onDismiss: () -> Unit) {
    TrestleDialog(
        onDismissRequest = onDismiss,
        maxWidth = 480.dp,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onDismiss()
                true
            } else {
                false
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_keyboard_shortcuts),
                onClose = onDismiss,
            )
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShortcutRow("Ctrl/Cmd + N", stringResource(Res.string.ui_new_instance))
                ShortcutRow("Ctrl/Cmd + F", stringResource(Res.string.ui_search))
                ShortcutRow("Ctrl/Cmd + O", stringResource(Res.string.ui_import_a_file))
                ShortcutRow("Ctrl/Cmd + 1–4", stringResource(Res.string.ui_switch_section))
                ShortcutRow("Ctrl/Cmd + ,", stringResource(Res.string.ui_settings))
                ShortcutRow("Enter", stringResource(Res.string.ui_open_focused_instance))
                ShortcutRow(stringResource(Res.string.ui_arrow_keys), stringResource(Res.string.ui_move_instance_focus))
                ShortcutRow("Delete", stringResource(Res.string.ui_remove_focused_instance))
                ShortcutRow("Escape", stringResource(Res.string.ui_close_or_go_back))
                ShortcutRow("F1", stringResource(Res.string.ui_show_shortcuts))
            }
        }
    }
}

@Composable
private fun ShortcutRow(keys: String, action: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(action, modifier = Modifier.weight(1f))
        Text(keys, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    }
}
