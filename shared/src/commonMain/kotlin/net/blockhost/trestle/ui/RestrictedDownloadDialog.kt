package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch

@Composable
internal fun RestrictedDownloadDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val download = state.restrictedDownload ?: return
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file != null) scope.launch {
            runCatching {
                require(file.size() in 1..512L * 1024 * 1024) { "File too large" }
                file.readBytes()
            }.onSuccess(actions::acceptRestrictedDownload)
                .onFailure { actions.reportLocalFileReadFailure(download.fileName) }
        }
    }
    AlertDialog(
        onDismissRequest = actions::dismissRestrictedDownload,
        title = { Text("Download from the publisher") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The publisher requires a manual download of ${download.fileName}. Download this exact version, then select the file. Trestle verifies its checksum and keeps a copy for installation.")
                TextButton(onClick = { uri.openUri(download.websiteUrl) }) { Text("Open publisher download") }
            }
        },
        confirmButton = { TextButton(onClick = { picker.launch() }) { Text("Select downloaded file") } },
        dismissButton = { TextButton(onClick = actions::dismissRestrictedDownload) { Text("Cancel") } },
    )
}
