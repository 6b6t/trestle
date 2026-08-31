package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.app.LauncherUpdate

@Composable
internal fun LauncherUpdateBanner(update: LauncherUpdate, actions: LauncherUiActions) {
    val uriHandler = LocalUriHandler.current
    var showNotes by remember(update.version) { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Trestle ${update.version} is available", modifier = Modifier.padding(vertical = 12.dp))
            TextButton(onClick = { showNotes = true }) { Text("Release notes") }
            update.downloads.firstOrNull()?.let { download ->
                TextButton(onClick = { uriHandler.openUri(download.url) }) { Text("Download .${download.format}") }
            } ?: TextButton(onClick = { uriHandler.openUri(update.releaseUrl) }) { Text("View release") }
            TextButton(onClick = actions::remindAboutLauncherUpdateLater) { Text("Remind me tomorrow") }
        }
    }
    if (showNotes) AlertDialog(
        onDismissRequest = { showNotes = false },
        title = { Text("Trestle ${update.version}") },
        text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(update.releaseNotes.ifBlank { "See the release page for changes and installation instructions." })
                    update.downloads.forEach { download ->
                        Text(".${download.format}: ${download.minimumOS}\nSHA-256: ${download.sha256}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { uriHandler.openUri(update.releaseUrl) }) { Text("Open release page") } },
        dismissButton = { TextButton(onClick = { showNotes = false }) { Text("Close") } },
    )
}
