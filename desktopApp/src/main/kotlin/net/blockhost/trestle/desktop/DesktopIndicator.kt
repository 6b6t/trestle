package net.blockhost.trestle.desktop

import net.blockhost.trestle.ui.LauncherUiState

internal enum class DesktopProgressState {
    IDLE,
    INDETERMINATE,
    NORMAL,
    ERROR,
}

internal enum class DesktopBadge {
    NONE,
    BUSY,
    ERROR,
}

internal data class DesktopIndicator(
    val progressState: DesktopProgressState,
    val progressPercent: Int?,
    val badge: DesktopBadge,
    val errorIdentity: String?,
)

internal fun LauncherUiState.desktopIndicator(): DesktopIndicator {
    if (error != null) {
        return DesktopIndicator(
            progressState = DesktopProgressState.ERROR,
            progressPercent = 100,
            badge = DesktopBadge.ERROR,
            errorIdentity = error,
        )
    }

    val activeOperation = operation ?: return DesktopIndicator(
        progressState = DesktopProgressState.IDLE,
        progressPercent = null,
        badge = DesktopBadge.NONE,
        errorIdentity = null,
    )
    val completed = activeOperation.completed
    val total = activeOperation.total
    val percent = if (completed != null && total != null && total > 0L) {
        ((completed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    } else {
        null
    }

    return DesktopIndicator(
        progressState = if (percent == null) DesktopProgressState.INDETERMINATE else DesktopProgressState.NORMAL,
        progressPercent = percent,
        badge = DesktopBadge.BUSY,
        errorIdentity = null,
    )
}
