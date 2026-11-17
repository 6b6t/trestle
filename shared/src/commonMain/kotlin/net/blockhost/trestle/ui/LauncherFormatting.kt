@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import io.github.vinceglb.filekit.name
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ui_count_downloads
import net.blockhost.trestle.resources.ui_download_count
import net.blockhost.trestle.resources.ui_hours_minutes
import net.blockhost.trestle.resources.ui_install_failed
import net.blockhost.trestle.resources.ui_installed
import net.blockhost.trestle.resources.ui_installing
import net.blockhost.trestle.resources.ui_less_than_a_minute
import net.blockhost.trestle.resources.ui_minutes_short
import net.blockhost.trestle.resources.ui_not_installed
import net.blockhost.trestle.resources.ui_not_played
import net.blockhost.trestle.resources.ui_ready_to_resume
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal fun formatUtcTime(epochMillis: Long): String {
    val secondsPerDay = 24L * 60L * 60L
    val seconds = ((epochMillis / 1_000L) % secondsPerDay + secondsPerDay) % secondsPerDay
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remainingSeconds = seconds % 60L
    return "${hours.toString().padStart(2, '0')}:" +
        "${minutes.toString().padStart(2, '0')}:" +
        "${remainingSeconds.toString().padStart(2, '0')}Z"
}

@Composable
internal fun formatDownloads(downloads: Long): String = when {
    downloads >= 1_000L -> stringResource(Res.string.ui_count_downloads, formatCount(downloads))
    else -> pluralStringResource(Res.plurals.ui_download_count, downloads.toInt(), downloads)
}

internal fun formatCount(count: Long): String = when {
    count >= 1_000_000_000L -> "${count / 100_000_000L / 10.0}B"
    count >= 1_000_000L -> "${count / 100_000L / 10.0}M"
    count >= 1_000L -> "${count / 100L / 10.0}K"
    else -> count.toString()
}

internal fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${bytes / 107_374_182L / 10.0} GiB"
    bytes >= 1_048_576L -> "${bytes / 104_857L / 10.0} MiB"
    bytes >= 1_024L -> "${bytes / 102L / 10.0} KiB"
    else -> "$bytes B"
}

internal fun formatInstanceForClipboard(instance: GameInstance): String = buildString {
    appendLine(instance.displayName)
    appendLine("Minecraft ${instance.minecraftVersionId}")
    appendLine("${instance.modLoader.label} · Java ${instance.requiredJavaMajor}")
    append(instance.instanceDirectory)
}

@Composable
internal fun formatPlayTime(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> stringResource(Res.string.ui_hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(Res.string.ui_minutes_short, minutes)
        milliseconds > 0 -> stringResource(Res.string.ui_less_than_a_minute)
        else -> stringResource(Res.string.ui_not_played)
    }
}

internal fun formatLogDetails(entry: LogEntry): String = entry.details.entries.joinToString { (key, value) -> "$key=$value" }

internal fun formatLogEntryForClipboard(entry: LogEntry): String = buildString {
    append(formatUtcTime(entry.timestampEpochMillis))
    append(" · ")
    append(entry.level.name)
    append(" · ")
    append(entry.category)
    appendLine()
    append(entry.message)
    if (entry.details.isNotEmpty()) {
        appendLine()
        append(formatLogDetails(entry))
    }
}

@Composable
internal fun PropertyRow(
    label: String,
    value: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
    divider: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(value) },
        overlineContent = { Text(label) },
        trailingContent = if (actionLabel == null || onClick == null || !actionEnabled) {
            null
        } else {
            { TextButton(onClick = onClick) { Text(actionLabel) } }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun stateLabel(state: InstallationState): String = when (state) {
    InstallationState.NotInstalled -> stringResource(Res.string.ui_not_installed)
    is InstallationState.Installing -> stringResource(Res.string.ui_installing)
    is InstallationState.Interrupted -> stringResource(Res.string.ui_ready_to_resume)
    is InstallationState.Installed -> stringResource(Res.string.ui_installed)
    is InstallationState.Failed -> stringResource(Res.string.ui_install_failed)
}

@Composable
internal fun stateColor(state: InstallationState) = when (state) {
    is InstallationState.Installed -> MaterialTheme.colorScheme.tertiary
    is InstallationState.Installing, is InstallationState.Interrupted -> MaterialTheme.colorScheme.primary
    is InstallationState.Failed -> MaterialTheme.colorScheme.error
    InstallationState.NotInstalled -> MaterialTheme.colorScheme.onSurfaceVariant
}

internal data class InstallationProgressSnapshot(
    val completedBytes: Long,
    val totalBytes: Long?,
    val completedFiles: Int,
    val totalFiles: Int,
)

internal fun progressFraction(completedBytes: Long?, totalBytes: Long?, completedFiles: Int?, totalFiles: Int?): Float? = when {
    completedBytes != null && totalBytes != null && totalBytes > 0L ->
        (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)

    completedFiles != null && totalFiles != null && totalFiles > 0 ->
        (completedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)

    else -> null
}

internal fun InstallationState.installationProgress(): InstallationProgressSnapshot? = when (this) {
    is InstallationState.Installing -> InstallationProgressSnapshot(
        completedBytes,
        totalBytes,
        completedFiles,
        totalFiles,
    )

    is InstallationState.Interrupted -> InstallationProgressSnapshot(
        completedBytes,
        totalBytes,
        completedFiles,
        totalFiles,
    )

    else -> null
}
