package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SkinProfile
import net.blockhost.trestle.auth.SkinVariant
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
    fun instanceWorkspaceSavesNotesThroughTheActionContract() = runComposeUiTest {
        var savedNotes: String? = null
        val actions = object : LauncherUiActions {
            override fun saveInstanceNotes(value: String) {
                savedNotes = value
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = actions,
                    initialDestination = LauncherDestination.INSTANCE,
                )
            }
        }

        onNodeWithText("Notes").performClick()
        onNodeWithText("Write notes for Building world").performTextInput("Use the survival seed")
        onNodeWithText("Save notes").performClick()

        assertEquals("Use the survival seed", savedNotes)
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
    fun externalShortcutCommandOpensTheShortcutSheet() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    externalCommand = LauncherCommandRequest(1L, LauncherCommand.SHOW_SHORTCUTS),
                )
            }
        }

        onNodeWithText("Keyboard shortcuts").assertIsDisplayed()
        onNodeWithText("Launch focused instance").assertIsDisplayed()
    }

    @Test
    fun doubleClickingInstanceSelectsItBeforeLaunching() = runComposeUiTest {
        val events = mutableListOf<String>()
        val instanceId = InstanceId("vanilla")
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
                TrestleApp(LauncherPreviewFixtures.loaded, actions)
            }
        }

        onNodeWithTag(LauncherTestTags.instance(instanceId)).performMouseInput { doubleClick() }

        assertEquals(listOf("select:vanilla", "launch"), events)
    }

    @Test
    fun doubleClickingSavedSkinSelectsItBeforeUse() = runComposeUiTest {
        val events = mutableListOf<String>()
        val skin = SavedSkin(
            profile = SkinProfile(
                id = "copper",
                name = "Copper adventurer",
                variant = SkinVariant.CLASSIC,
                textureFile = "copper.png",
                createdAtEpochMillis = 1L,
            ),
            texture = byteArrayOf(),
        )
        val state = LauncherPreviewFixtures.loaded.copy(
            savedSkins = listOf(skin),
            skinStudio = SkinStudioState(visible = true),
        )
        val actions = object : LauncherUiActions {
            override fun selectSavedSkin(profileId: String) {
                events += "select:$profileId"
            }

            override fun useSelectedSkin() {
                events += "use"
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(state, actions)
            }
        }

        onNodeWithText("Copper adventurer").performMouseInput { doubleClick() }

        assertEquals(listOf("select:copper", "use"), events)
    }

    @Test
    fun doubleClickingInactiveAccountMakesItActive() = runComposeUiTest {
        var selectedProfileId: String? = null
        val state = LauncherPreviewFixtures.loaded.copy(
            accounts = listOf(LauncherPreviewFixtures.activeAccount.copy(isActive = false)),
        )
        val actions = object : LauncherUiActions {
            override fun selectAccount(profileId: String) {
                selectedProfileId = profileId
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(state, actions, initialDestination = LauncherDestination.ACCOUNTS)
            }
        }

        onNodeWithText("Pistonmaster").performMouseInput { doubleClick() }

        assertEquals("preview-account", selectedProfileId)
    }

    @Test
    fun clickingInstanceOperationOpensItsWorkspace() = runComposeUiTest {
        val instanceId = InstanceId("vanilla")
        var selectedId: InstanceId? = null
        val state = LauncherPreviewFixtures.loaded.copy(
            operation = OperationStatus("Installing Vanilla", instanceId = instanceId),
        )
        val actions = object : LauncherUiActions {
            override fun selectInstance(id: InstanceId) {
                selectedId = id
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(state, actions)
            }
        }

        onNodeWithText("Installing Vanilla").performClick()

        assertEquals(instanceId, selectedId)
        onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).assertIsDisplayed()
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
        listOf("overview", "content", "game_data", "settings").forEach { section ->
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

        listOf("overview", "content", "game_data", "settings").forEach { section ->
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
    fun instanceImageEditorSelectsBuiltInLogos() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(LauncherPreviewFixtures.settingsDialog, NoopLauncherUiActions)
            }
        }

        onNodeWithTag(LauncherTestTags.INSTANCE_ICON_EDIT).performClick()
        onNodeWithTag(LauncherTestTags.INSTANCE_ICON_DIALOG).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.INSTANCE_ICON_UPLOAD).assertIsDisplayed().assertHasClickAction()
        onNodeWithTag(LauncherTestTags.instanceIconOption("terrain")).performClick().assertIsSelected()
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
