package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.RestrictedDownload
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_download_files_from_publishers
import net.blockhost.trestle.resources.ui_download_from_the_publisher
import net.blockhost.trestle.resources.ui_open_download
import net.blockhost.trestle.resources.ui_restricted_download_explanation
import net.blockhost.trestle.resources.ui_select_downloaded_file
import net.blockhost.trestle.resources.ui_verified
import net.blockhost.trestle.resources.ui_waiting_for_file
import net.blockhost.trestle.text.formattedString
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RestrictedDownloadDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val downloads = state.restrictedDownloads
    if (downloads.isEmpty()) return
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file != null) {
            scope.launch {
                if (runCatching { file.size() }.getOrDefault(-1L) > MAX_LOCAL_IMPORT_BYTES) {
                    actions.reportLocalFileTooLarge(file.name)
                } else {
                    runCatching { file.readBytes() }
                        .onSuccess(actions::acceptRestrictedDownload)
                        .onFailure { actions.reportLocalFileReadFailure(file.name) }
                }
            }
        }
    }
    val remaining = downloads.count { it.sha1 !in state.verifiedRestrictedDownloads }
    AlertDialog(
        onDismissRequest = actions::dismissRestrictedDownload,
        title = {
            Text(
                if (downloads.size == 1) {
                    stringResource(Res.string.ui_download_from_the_publisher)
                } else {
                    formattedString(Res.string.ui_download_files_from_publishers, downloads.size)
                },
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.ui_restricted_download_explanation))
                downloads.forEach { download ->
                    RestrictedDownloadRow(download, download.sha1 in state.verifiedRestrictedDownloads) {
                        uri.openUri(download.websiteUrl)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { picker.launch() }, enabled = remaining > 0) {
                Text(stringResource(Res.string.ui_select_downloaded_file))
            }
        },
        dismissButton = {
            TextButton(onClick = actions::dismissRestrictedDownload) { Text(stringResource(Res.string.ui_cancel)) }
        },
    )
}

@Composable
private fun RestrictedDownloadRow(download: RestrictedDownload, verified: Boolean, onOpen: () -> Unit) {
    val color = if (verified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (verified) "✓" else "•", color = color)
        Column(Modifier.weight(1f)) {
            Text(download.fileName, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (verified) stringResource(Res.string.ui_verified) else stringResource(Res.string.ui_waiting_for_file),
                color = color,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (!verified) {
            TextButton(onClick = onOpen) { Text(stringResource(Res.string.ui_open_download)) }
        }
    }
}
