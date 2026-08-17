package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import net.blockhost.trestle.domain.InstanceId

@OptIn(ExperimentalTestApi::class)
class TrestleAppUiTest {
    @Test
    fun wideLibraryKeepsPrimaryControlsVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.loaded, NoopLauncherUiActions)
            }
        }

        onNodeWithTag(LauncherTestTags.TOP_NAVIGATION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.INSTANCE_SEARCH).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed().assertHasClickAction()
        onNodeWithTag(LauncherTestTags.SELECTED_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun compactLibraryKeepsLaunchAndManagementActionsVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(600.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.loaded, NoopLauncherUiActions)
            }
        }

        onNodeWithTag(LauncherTestTags.LIBRARY).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed()
        onNodeWithText("Content").assertIsDisplayed().assertHasClickAction()
        onNodeWithText("Manage").assertIsDisplayed().assertHasClickAction()
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
        onNodeWithText("Settings").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun compactBreakpointUsesCompactChromeAt839Dp() = runComposeUiTest {
        setContent {
            Box(Modifier.size(839.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.installing, NoopLauncherUiActions)
            }
        }

        onAllNodesWithTag(LauncherTestTags.TOP_NAVIGATION).assertCountEquals(0)
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
        onNodeWithText("Content").assertIsDisplayed()
    }

    @Test
    fun wideBreakpointUsesWideChromeAt840Dp() = runComposeUiTest {
        setContent {
            Box(Modifier.size(840.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.installing, NoopLauncherUiActions)
            }
        }

        onNodeWithTag(LauncherTestTags.TOP_NAVIGATION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.SELECTED_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
    }

    @Test
    fun librarySearchFiltersInstancesWithoutLeavingStaleTiles() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.loaded, NoopLauncherUiActions)
            }
        }

        onNodeWithTag(LauncherTestTags.INSTANCE_SEARCH).performTextInput("Legacy redstone")
        waitForIdle()

        onNodeWithTag(LauncherTestTags.instance(InstanceId("archive"))).assertIsDisplayed()
        onAllNodesWithTag(LauncherTestTags.instance(InstanceId("building"))).assertCountEquals(0)
    }

    @Test
    fun wideNavigationChangesDestinationInsideTheStateOnlyUi() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.discover, NoopLauncherUiActions)
            }
        }

        onNodeWithTag(LauncherTestTags.navigation(LauncherDestination.DISCOVER)).performClick()
        onNodeWithTag(LauncherTestTags.DISCOVER).assertIsDisplayed()

        onNodeWithTag(LauncherTestTags.navigation(LauncherDestination.SETTINGS)).performClick()
        onNodeWithTag(LauncherTestTags.SETTINGS).assertIsDisplayed()
    }

    @Test
    fun discoverKeepsResourceSearchVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.discover,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.DISCOVER,
                )
            }
        }

        onNodeWithTag(LauncherTestTags.RESOURCE_SEARCH).assertIsDisplayed()
    }

    @Test
    fun initialDestinationCanRenderWithoutNavigationSideEffects() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.ACCOUNTS,
                )
            }
        }

        onNodeWithTag(LauncherTestTags.ACCOUNTS).assertIsDisplayed()
        onNodeWithText("Add account").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun uiEventsLeaveTheComposableThroughTheActionContract() = runComposeUiTest {
        var createRequests = 0
        val actions = object : LauncherUiActions {
            override fun openCreate() {
                createRequests += 1
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.loaded, actions)
            }
        }

        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).performClick()

        assertEquals(1, createRequests)
    }

    @Test
    fun longSelectedNameDoesNotDisplaceCompactActions() = runComposeUiTest {
        val state = LauncherPreviewFixtures.loaded.copy(
            selectedId = InstanceId("long-name"),
            launch = InstanceLaunchState(InstanceId("long-name"), LaunchStatus.NotChecked),
        )
        setContent {
            Box(Modifier.size(600.dp, 720.dp)) {
                TrestleApp(state, NoopLauncherUiActions)
            }
        }

        onNodeWithText("Content").assertIsDisplayed()
        onNodeWithText("Manage").assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
    }

    @Test
    fun wideInstanceWorkspaceKeepsAllSectionsVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.INSTANCE,
                )
            }
        }

        onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).assertIsDisplayed()
        listOf("overview", "content", "settings").forEach { section ->
            onNodeWithTag(LauncherTestTags.instanceSection(section)).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun compactInstanceWorkspaceKeepsAllSectionsVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(600.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.INSTANCE,
                )
            }
        }

        listOf("overview", "content", "settings").forEach { section ->
            onNodeWithTag(LauncherTestTags.instanceSection(section)).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun createDialogRendersItsCriticalAction() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.createDialog, NoopLauncherUiActions)
            }
        }
        onNodeWithTag(LauncherTestTags.CREATE_DIALOG).assertIsDisplayed()
        onNodeWithText("Create instance").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun instanceSettingsDialogRendersItsCriticalAction() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.settingsDialog, NoopLauncherUiActions)
            }
        }
        onNodeWithTag(LauncherTestTags.INSTANCE_SETTINGS_DIALOG).assertIsDisplayed()
        onNodeWithText("Save changes").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun accountDialogRendersItsCriticalAction() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.accountDialog, NoopLauncherUiActions)
            }
        }
        onNodeWithTag(LauncherTestTags.ACCOUNT_LOGIN_DIALOG).assertIsDisplayed()
        onNodeWithText("Get sign-in code").assertIsDisplayed().assertHasClickAction()
    }
}
