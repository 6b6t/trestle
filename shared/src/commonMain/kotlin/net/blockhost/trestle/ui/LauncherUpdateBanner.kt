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
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_download_format
import net.blockhost.trestle.resources.ui_open_release_page
import net.blockhost.trestle.resources.ui_release_notes
import net.blockhost.trestle.resources.ui_remind_me_tomorrow
import net.blockhost.trestle.resources.ui_s_s_sha_s
import net.blockhost.trestle.resources.ui_see_the_release_page_for_changes_and
import net.blockhost.trestle.resources.ui_trestle_version
import net.blockhost.trestle.resources.ui_trestle_version_available
import net.blockhost.trestle.resources.ui_view_release
import net.blockhost.trestle.text.formattedString
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LauncherUpdateBanner(update: LauncherUpdate, actions: LauncherUiActions) {
    val uriHandler = LocalUriHandler.current
    var showNotes by remember(update.version) { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(formattedString(Res.string.ui_trestle_version_available, update.version), modifier = Modifier.padding(vertical = 12.dp))
            TextButton(onClick = { showNotes = true }) { Text(stringResource(Res.string.ui_release_notes)) }
            update.downloads.firstOrNull()?.let { download ->
                TextButton(onClick = {
                    uriHandler.openUri(download.url)
                }) { Text(formattedString(Res.string.ui_download_format, download.format)) }
            } ?: TextButton(onClick = { uriHandler.openUri(update.releaseUrl) }) { Text(stringResource(Res.string.ui_view_release)) }
            TextButton(onClick = actions::remindAboutLauncherUpdateLater) { Text(stringResource(Res.string.ui_remind_me_tomorrow)) }
        }
    }
    if (showNotes) {
        AlertDialog(
            onDismissRequest = { showNotes = false },
            title = { Text(formattedString(Res.string.ui_trestle_version, update.version)) },
            text = {
                SelectionContainer {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(update.releaseNotes.ifBlank { stringResource(Res.string.ui_see_the_release_page_for_changes_and) })
                        update.downloads.forEach { download ->
                            Text(
                                formattedString(Res.string.ui_s_s_sha_s, download.format, download.minimumOS, download.sha256),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { uriHandler.openUri(update.releaseUrl) }) { Text(stringResource(Res.string.ui_open_release_page)) }
            },
            dismissButton = { TextButton(onClick = { showNotes = false }) { Text(stringResource(Res.string.ui_close)) } },
        )
    }
}
