package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.text.asUserText
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LibraryUiTest {
    @Test
    fun clickingInstanceTileSelectsItWithoutLaunching() = runComposeUiTest {
        val events = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun selectInstance(id: InstanceId) {
                events += "select:${id.value}"
            }

            override fun launchSelected() {
                events += "launch"
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.loaded, actions, windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720))
            }
        }

        onNodeWithTag(LauncherTestTags.LIBRARY)
            .performScrollToNode(hasTestTag(LauncherTestTags.instance(InstanceId("vanilla"))))
        onNodeWithTag(LauncherTestTags.instance(InstanceId("vanilla"))).performMouseInput { doubleClick() }

        assertEquals(listOf("select:vanilla"), events.distinct())
        onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).assertIsDisplayed()
    }

    @Test
    fun playIsUnavailableWhileAnotherInstanceIsRunning() = runComposeUiTest {
        val state = LauncherPreviewFixtures.loaded.copy(
            activeLaunch = InstanceLaunchState(InstanceId("vanilla"), LaunchStatus.Running(processId = 42)),
        )
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(state, NoopLauncherUiActions, windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720))
            }
        }

        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsNotEnabled()
        onNodeWithText("${isolated("Latest release")} is still running. Close it to play another instance.").assertIsDisplayed()
    }

    @Test
    fun undoAppearsOnlyOnTheInstanceRemovalNotice() = runComposeUiTest {
        val removed = LauncherPreviewFixtures.instances.last()
        var state by mutableStateOf(
            LauncherPreviewFixtures.loaded.copy(removedInstanceUndo = removed, notice = INSTANCE_REMOVED_NOTICE),
        )
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(state, NoopLauncherUiActions, windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720))
            }
        }
        onNodeWithText("Undo").assertIsDisplayed()

        state = state.copy(notice = "Minecraft closed.".asUserText())
        waitForIdle()

        onNodeWithText("Minecraft closed.").assertIsDisplayed()
        onAllNodesWithText("Undo").assertCountEquals(0)
    }

    @Test
    fun groupNamedPinnedDoesNotCollideWithPinnedSection() = runComposeUiTest {
        val instances = LauncherPreviewFixtures.instances.mapIndexed { index, instance ->
            when (index) {
                0 -> instance.copy(pinned = true)
                1 -> instance.copy(group = "Pinned")
                else -> instance
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(instances = instances),
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.LIBRARY)
            .performScrollToNode(hasTestTag(LauncherTestTags.instance(instances[1].id)))
        onNodeWithTag(LauncherTestTags.instance(instances[1].id)).assertIsDisplayed()
    }

    @Test
    fun removingAnInstanceKeepsFilesUnlessDeleteIsChosen() = runComposeUiTest {
        val events = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun confirmInstanceRemoval() {
                events += "remove"
            }

            override fun confirmInstanceDeletion() {
                events += "delete"
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(pendingInstanceRemovalId = LauncherPreviewFixtures.installed.id),
                    actions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithText("Remove").performClick()
        onNodeWithText("Delete files").assertHasClickAction()

        assertEquals(listOf("remove"), events)
    }

    @Test
    fun instanceTilePlayButtonLaunchesThatInstance() = runComposeUiTest {
        val events = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun selectInstance(id: InstanceId) {
                events += "select:${id.value}"
            }

            override fun launchSelected() {
                events += "launch"
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    actions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.LIBRARY)
            .performScrollToNode(hasTestTag(LauncherTestTags.instancePlay(InstanceId("vanilla"))))
        onNodeWithTag(LauncherTestTags.instancePlay(InstanceId("vanilla"))).performClick()

        assertEquals(listOf("select:vanilla", "launch"), events)
    }

    @Test
    fun libraryShowsWhichAccountWillPlay() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithText("Playing as ${isolated("Pistonmaster")}").assertIsDisplayed().assertHasClickAction()
    }
}
