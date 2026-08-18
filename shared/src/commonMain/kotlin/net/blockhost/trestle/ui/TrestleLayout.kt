package net.blockhost.trestle.ui

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.Posture
import androidx.window.core.layout.WindowSizeClass

internal enum class TrestleLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

internal val compactTrestleWindowAdaptiveInfo = WindowAdaptiveInfo(
    windowSizeClass = WindowSizeClass(0, 0),
    windowPosture = Posture(),
)

internal fun WindowAdaptiveInfo.trestleLayoutMode(): TrestleLayoutMode = when {
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
        TrestleLayoutMode.EXPANDED
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
        TrestleLayoutMode.MEDIUM
    else -> TrestleLayoutMode.COMPACT
}
