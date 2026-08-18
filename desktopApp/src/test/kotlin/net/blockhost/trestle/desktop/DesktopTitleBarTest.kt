package net.blockhost.trestle.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import kotlin.test.Test
import kotlin.test.assertEquals
import net.blockhost.trestle.ui.LauncherCommand
import net.blockhost.trestle.ui.LauncherUiState

@OptIn(ExperimentalTestApi::class)
class DesktopTitleBarTest {
    @Test
    fun `material menu and window controls dispatch their actions`() = runComposeUiTest {
        val commands = mutableListOf<LauncherCommand>()
        val windowActions = mutableListOf<String>()
        setContent {
            MaterialTheme {
                Box(Modifier.size(760.dp, 120.dp)) {
                    DesktopTitleBarContent(
                        title = "Trestle",
                        state = LauncherUiState(),
                        isMac = false,
                        placement = WindowPlacement.Floating,
                        onCommand = commands::add,
                        onStopInstance = { windowActions += "stop" },
                        onQuit = { windowActions += "quit" },
                        onMinimize = { windowActions += "minimize" },
                        onToggleMaximize = { windowActions += "maximize" },
                        onClose = { windowActions += "close" },
                        dragArea = { modifier, content -> Box(modifier) { content() } },
                    )
                }
            }
        }

        onNodeWithText("File").performClick()
        onNodeWithText("New instance").performClick()
        onNodeWithTag(DesktopTitleBarTestTags.MINIMIZE).performClick()
        onNodeWithTag(DesktopTitleBarTestTags.MAXIMIZE).performClick()
        onNodeWithTag(DesktopTitleBarTestTags.CLOSE).performClick()

        assertEquals(listOf(LauncherCommand.NEW_INSTANCE), commands)
        assertEquals(listOf("minimize", "maximize", "close"), windowActions)
    }
}
