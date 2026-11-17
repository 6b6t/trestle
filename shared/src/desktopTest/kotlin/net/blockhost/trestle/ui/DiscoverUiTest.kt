package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.text.load
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DiscoverUiTest {
    @Test
    fun installExplainsThatVanillaCannotLoadMods() = runComposeUiTest {
        val state = LauncherPreviewFixtures.discover.copy(
            selectedId = InstanceId("vanilla"),
            resourceBrowser = LauncherPreviewFixtures.discover.resourceBrowser.copy(selectedProjectId = "sodium"),
        )
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state,
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.DISCOVER,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.INSTALL_BLOCKED_REASON).assertTextEquals(VANILLA_CONTENT_MESSAGE.load())
        onNodeWithText("Install").assertIsNotEnabled()
    }

    @Test
    fun installExplainsWhenTheTargetIsNotInstalled() = runComposeUiTest {
        val state = LauncherPreviewFixtures.discover.copy(
            selectedId = InstanceId("long-name"),
            resourceBrowser = LauncherPreviewFixtures.discover.resourceBrowser.copy(selectedProjectId = "sodium"),
        )
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state,
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.DISCOVER,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithText("Install instance").assertIsDisplayed()
    }

    @Test
    fun createDialogExplainsAMissingName() = runComposeUiTest {
        val state = LauncherPreviewFixtures.createDialog.copy(
            create = LauncherPreviewFixtures.createDialog.create.copy(name = ""),
        )
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(state, NoopLauncherUiActions, windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720))
            }
        }

        onNodeWithText("Enter an instance name.").assertIsDisplayed()
    }
}
