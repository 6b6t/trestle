package net.blockhost.trestle.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.blockhost.trestle.ui.LauncherUiState
import net.blockhost.trestle.ui.OperationStatus

class DesktopIndicatorTest {
    @Test
    fun `idle state clears desktop indicators`() {
        val indicator = LauncherUiState(isInitializing = false).desktopIndicator()

        assertEquals(DesktopProgressState.IDLE, indicator.progressState)
        assertEquals(DesktopBadge.NONE, indicator.badge)
        assertNull(indicator.progressPercent)
    }

    @Test
    fun `known operation progress is converted to a bounded percentage`() {
        val indicator = LauncherUiState(
            operation = OperationStatus("Installing", completed = 250, total = 200),
        ).desktopIndicator()

        assertEquals(DesktopProgressState.NORMAL, indicator.progressState)
        assertEquals(DesktopBadge.BUSY, indicator.badge)
        assertEquals(100, indicator.progressPercent)
    }

    @Test
    fun `operation without a total uses indeterminate progress`() {
        val indicator = LauncherUiState(
            operation = OperationStatus("Resolving metadata", completed = 8),
        ).desktopIndicator()

        assertEquals(DesktopProgressState.INDETERMINATE, indicator.progressState)
        assertEquals(DesktopBadge.BUSY, indicator.badge)
        assertNull(indicator.progressPercent)
    }

    @Test
    fun `error takes priority over an active operation`() {
        val indicator = LauncherUiState(
            error = "Download failed",
            operation = OperationStatus("Installing", completed = 50, total = 100),
        ).desktopIndicator()

        assertEquals(DesktopProgressState.ERROR, indicator.progressState)
        assertEquals(DesktopBadge.ERROR, indicator.badge)
        assertEquals(100, indicator.progressPercent)
        assertEquals("Download failed", indicator.errorIdentity)
    }
}
