@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.name
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_clear
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_events_from_this_session_right_click_an_entry_to_copy_diagnostics
import net.blockhost.trestle.resources.ui_launcher_log
import net.blockhost.trestle.resources.ui_no_launcher_events_in_this_session
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LauncherLog(state: LauncherUiState, actions: LauncherUiActions, listState: LazyListState, modifier: Modifier) {
    var selectedEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        item("logs-heading") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(Res.string.ui_launcher_log), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.ui_events_from_this_session_right_click_an_entry_to_copy_diagnostics),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = actions::clearLogs, enabled = state.logs.isNotEmpty()) { Text(stringResource(Res.string.ui_clear)) }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (state.logs.isEmpty()) {
            item("logs-empty") {
                Text(stringResource(Res.string.ui_no_launcher_events_in_this_session), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(state.logs.takeLast(80).asReversed(), key = { it.id }) { entry ->
                LogRow(entry, onOpen = { selectedEntryId = entry.id })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    selectedEntryId?.let { entryId ->
        state.logs.firstOrNull { it.id == entryId }?.let { entry ->
            LogDetailsDialog(entry) { selectedEntryId = null }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry, onOpen: () -> Unit) {
    val copyText = rememberCopyText()
    val actions = buildList {
        add(ContextAction("Copy message") { copyText(entry.message) })
        if (entry.details.isNotEmpty()) {
            add(ContextAction("Copy details") { copyText(formatLogDetails(entry)) })
        }
        add(
            ContextAction("Copy event", separatorBefore = true) {
                copyText(formatLogEntryForClipboard(entry))
            },
        )
    }
    ContextActionArea(actions) {
        ListItem(
            leadingContent = {
                Column(Modifier.width(68.dp)) {
                    Text(
                        formatUtcTime(entry.timestampEpochMillis),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        entry.level.name,
                        color = if (entry.level.name == "ERROR") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
            headlineContent = { Text(entry.message, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = {
                Text(
                    buildString {
                        append(entry.category)
                        if (entry.details.isNotEmpty()) {
                            append(" · ")
                            append(formatLogDetails(entry))
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth().clickable(
                onClickLabel = "Open log details",
                role = Role.Button,
                onClick = onOpen,
            ),
        )
    }
}

@Composable
private fun LogDetailsDialog(entry: LogEntry, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.message) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyRow("Time", formatUtcTime(entry.timestampEpochMillis))
                PropertyRow("Level", entry.level.name)
                PropertyRow("Category", entry.category)
                if (entry.details.isNotEmpty()) PropertyRow("Details", formatLogDetails(entry))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_close)) } },
    )
}
