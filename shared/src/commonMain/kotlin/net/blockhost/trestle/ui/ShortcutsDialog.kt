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
import net.blockhost.trestle.resources.ui_keyboard_shortcuts
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
                ShortcutRow("Ctrl/Cmd + N", "New instance")
                ShortcutRow("Ctrl/Cmd + F", "Search")
                ShortcutRow("Ctrl/Cmd + O", "Import a file")
                ShortcutRow("Ctrl/Cmd + 1–4", "Switch section")
                ShortcutRow("Ctrl/Cmd + ,", "Settings")
                ShortcutRow("Enter", "Open focused instance")
                ShortcutRow("Arrow keys", "Move instance focus")
                ShortcutRow("Delete", "Remove focused instance")
                ShortcutRow("Escape", "Close or go back")
                ShortcutRow("F1", "Show shortcuts")
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
