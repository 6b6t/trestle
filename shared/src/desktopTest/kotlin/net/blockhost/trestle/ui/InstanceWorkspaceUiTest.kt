package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class InstanceWorkspaceUiTest {
    private fun instancePage(state: LauncherUiState, actions: LauncherUiActions): @Composable () -> Unit = {
        Box(Modifier.size(1000.dp, 720.dp)) {
            TrestleApp(
                state,
                actions,
                initialDestination = LauncherDestination.INSTANCE,
                windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
            )
        }
    }

    @Test
    fun notesDraftSurvivesSwitchingTabs() = runComposeUiTest {
        setContent(instancePage(LauncherPreviewFixtures.played, NoopLauncherUiActions))

        onNodeWithTag(LauncherTestTags.instanceSection("notes")).performClick()
        onNodeWithText("Write notes for Building world").performTextInput("Seed is 42")
        onNodeWithTag(LauncherTestTags.instanceSection("overview")).performClick()
        onNodeWithTag(LauncherTestTags.instanceSection("notes")).performClick()

        onNodeWithText("Seed is 42").assertIsDisplayed()
    }

    @Test
    fun escapeWhileTypingLeavesTheFieldInsteadOfThePage() = runComposeUiTest {
        setContent(instancePage(LauncherPreviewFixtures.played, NoopLauncherUiActions))

        onNodeWithTag(LauncherTestTags.instanceSection("notes")).performClick()
        val notes = onNodeWithText("Write notes for Building world")
        notes.performTextInput("Seed is 42")
        onNodeWithText("Seed is 42").performKeyInput { pressKey(Key.Escape) }

        onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).assertIsDisplayed()
        onNodeWithText("Seed is 42").assertIsNotFocused()
    }

    @Test
    fun phoneHeaderCollapsesWhileScrollingAndExpandsAtTheTop() = runComposeUiTest {
        setContent {
            Box(Modifier.size(420.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.played,
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.INSTANCE,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(420, 720),
                )
            }
        }
        onNodeWithTag(LauncherTestTags.INSTANCE_HEADER_EXPANDED).assertIsDisplayed()

        onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).performTouchInput { swipeUp() }
        onAllNodesWithTag(LauncherTestTags.INSTANCE_HEADER_EXPANDED).assertCountEquals(0)
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()

        repeat(3) {
            onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).performTouchInput {
                swipeDown(startY = centerY, endY = bottom)
            }
        }
        onNodeWithTag(LauncherTestTags.INSTANCE_HEADER_EXPANDED).assertIsDisplayed()
    }

    @Test
    fun leavingSettingsWithUnsavedChangesAsksFirst() = runComposeUiTest {
        val events = mutableListOf<String>()
        val opened = InstanceSettingsState(
            visible = true,
            inline = true,
            instanceId = LauncherPreviewFixtures.installed.id,
            name = "Building world",
            minecraftVersionId = "1.21.11",
            minimumMemoryMiB = "512",
            maximumMemoryMiB = "2048",
        )
        val edited = opened.copy(initial = opened, name = "Renamed world")
        val actions = object : LauncherUiActions {
            override fun closeInstanceSettings() {
                events += "close"
            }
        }
        setContent(instancePage(LauncherPreviewFixtures.played.copy(instanceSettings = edited), actions))

        onNodeWithTag(LauncherTestTags.instanceSection("settings")).performClick()
        events.clear()
        onNodeWithTag(LauncherTestTags.instanceSection("worlds")).performClick()

        onNodeWithText("Discard unsaved changes?").assertIsDisplayed()
        assertEquals(emptyList(), events)

        onNodeWithText("Discard").performClick()
        assertEquals(listOf("close"), events)
    }

    @Test
    fun removingContentAsksForConfirmation() = runComposeUiTest {
        val removed = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun removeInstalledContent(key: String) {
                removed += key
            }
        }
        setContent(instancePage(LauncherPreviewFixtures.played, actions))

        onNodeWithTag(LauncherTestTags.instanceSection("content")).performClick()
        onNodeWithText("Sodium").assertIsDisplayed()
        onAllNodesWithText("Remove")[0].performScrollTo().performClick()
        waitForIdle()

        onNodeWithText("Remove Sodium?").assertIsDisplayed()
        assertEquals(emptyList(), removed)
        val removeButtons = onAllNodesWithText("Remove")
        removeButtons[removeButtons.fetchSemanticsNodes().size - 1].performClick()
        assertEquals(listOf("sodium"), removed)
    }

    @Test
    fun tappingContentNameDoesNotToggleIt() = runComposeUiTest {
        val toggled = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun toggleInstalledContent(key: String) {
                toggled += key
            }
        }
        setContent(instancePage(LauncherPreviewFixtures.played, actions))

        onNodeWithTag(LauncherTestTags.instanceSection("content")).performClick()
        onNodeWithText("Sodium").performClick()
        assertEquals(emptyList(), toggled)

        onNodeWithContentDescription("Enable Sodium").performClick()
        assertEquals(listOf("sodium"), toggled)
    }

    @Test
    fun deletingScreenshotAsksForConfirmation() = runComposeUiTest {
        val deleted = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun deleteScreenshot(screenshotKey: String) {
                deleted += screenshotKey
            }
        }
        setContent(instancePage(LauncherPreviewFixtures.played, actions))

        onNodeWithTag(LauncherTestTags.instanceSection("screenshots")).performClick()
        onAllNodesWithContentDescription("More")[0].performClick()
        onNodeWithText("Delete").performClick()

        onNodeWithText("Delete 2025-12-16_20.14.02.png?").assertIsDisplayed()
        assertEquals(emptyList(), deleted)
        onNodeWithText("Delete").performClick()
        assertEquals(listOf("shot-1"), deleted)
    }
}
