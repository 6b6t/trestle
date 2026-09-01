package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import net.blockhost.trestle.auth.SavedSkin
import net.blockhost.trestle.auth.SkinProfile
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader

@OptIn(ExperimentalTestApi::class)
class TrestleAppUiTest {
    @Test
    fun wideLibraryKeepsPrimaryControlsVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.TOP_NAVIGATION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.INSTANCE_SEARCH).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed().assertHasClickAction()
        onNodeWithTag(LauncherTestTags.SELECTED_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun compactLibraryOpensAnInstanceFromTheList() = runComposeUiTest {
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.LIBRARY).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.instance(InstanceId("building"))).performClick()

        onNodeWithTag(LauncherTestTags.INSTANCE_WORKSPACE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
    }

    @Test
    fun disabledLaunchExplainsWhenAnAccountMustBeAdded() = runComposeUiTest {
        val state = LauncherPreviewFixtures.loaded.copy(
            accounts = emptyList(),
            launch = InstanceLaunchState(
                LauncherPreviewFixtures.installed.id,
                LaunchStatus.Blocked(listOf("Java account")),
            ),
        )
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state,
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.INSTANCE,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.LAUNCH_ACCOUNT_REQUIREMENT).assertIsDisplayed()
    }

    @Test
    fun compactBreakpointUsesCompactChromeAt599Dp() = runComposeUiTest {
        setContent {
            Box(Modifier.size(599.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.installing,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(599, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.TOP_NAVIGATION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.instance(InstanceId("building"))).assertIsDisplayed()
    }

    @Test
    fun mediumBreakpointUsesRailAt600Dp() = runComposeUiTest {
        setContent {
            Box(Modifier.size(600.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.installing,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(600, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.TOP_NAVIGATION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.instance(InstanceId("building"))).assertIsDisplayed()
    }

    @Test
    fun wideBreakpointUsesWideChromeAt840Dp() = runComposeUiTest {
        setContent {
            Box(Modifier.size(840.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.installing,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(840, 720),
                )
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
        onNodeWithTag(LauncherTestTags.RESOURCE_RESULTS).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.RESOURCE_DETAIL).assertIsDisplayed()
    }

    @Test
    fun resourceDialogUsesOnePaneEvenWhenTheWindowIsWide() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.resourceDialog,
                    actions = NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.RESOURCE_BROWSER_DIALOG).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.RESOURCE_RESULTS).assertIsDisplayed()
        onAllNodesWithTag(LauncherTestTags.RESOURCE_DETAIL).assertCountEquals(0)
    }

    @Test
    fun discoverSearchUpdatesTheResourceQuery() = runComposeUiTest {
        var query = ""
        val actions = object : LauncherUiActions {
            override fun setResourceQuery(value: String) {
                query = value
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.discover,
                    actions = actions,
                    initialDestination = LauncherDestination.DISCOVER,
                )
            }
        }

        onNodeWithTag(LauncherTestTags.RESOURCE_SEARCH).performTextInput("sodium")
        waitForIdle()

        assertEquals("sodium", query)
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
        onNodeWithContentDescription("Add account").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun compactSettingsUsesCategoryAndDetailNavigation() = runComposeUiTest {
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.SETTINGS_CATEGORIES).assertIsDisplayed()
        onNodeWithText("General").performClick()
        waitForIdle()

        onNodeWithTag(LauncherTestTags.SETTINGS_DETAIL).assertIsDisplayed()
        onNodeWithText("Instance sorting").assertIsDisplayed()
        onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        waitForIdle()

        onNodeWithTag(LauncherTestTags.SETTINGS_CATEGORIES).assertIsDisplayed()
    }

    @Test
    fun wideSettingsKeepsCategoriesAndSelectedDetailVisible() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }
        waitForIdle()

        onNodeWithTag(LauncherTestTags.SETTINGS_CATEGORIES).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.SETTINGS_DETAIL).assertIsDisplayed()
        onNodeWithText("Instance sorting").assertIsDisplayed()
    }

    @Test
    fun compactDiscoverOpensFiltersInAMaterialSheet() = runComposeUiTest {
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.discover,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.DISCOVER,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.RESOURCE_FILTERS).performClick()
        onNodeWithText("Apply filters").assertIsDisplayed().assertHasClickAction()
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
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    actions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
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
    fun rightClickingInstanceOpensMaterialContextActions() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.instance(InstanceId("building"))).performMouseInput {
            click(button = MouseButton.Secondary)
        }

        onNodeWithText("Inspect launch plan").assertIsDisplayed().assertHasClickAction()
        onNodeWithText("Remove from library").assertIsDisplayed().assertHasClickAction()
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
                TrestleApp(state, actions, initialDestination = LauncherDestination.ACCOUNTS)
            }
        }

        onNodeWithText("Copper adventurer").performMouseInput { doubleClick() }

        assertEquals(listOf("select:copper", "use"), events)
    }

    @Test
    fun inactiveAccountRequiresAnExplicitUseAction() = runComposeUiTest {
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

        onNodeWithTag(LauncherTestTags.account("preview-account")).performClick()

        assertEquals(null, selectedProfileId)
        onNodeWithText("Use").performClick()

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
    fun longSelectedNameDoesNotDisplaceCompactLibraryControls() = runComposeUiTest {
        val state = LauncherPreviewFixtures.loaded.copy(
            selectedId = InstanceId("long-name"),
            launch = InstanceLaunchState(InstanceId("long-name"), LaunchStatus.NotChecked),
        )
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.NEW_INSTANCE).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.instance(InstanceId("long-name"))).assertIsDisplayed()
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
        listOf("overview", "content", "worlds", "servers", "screenshots", "notes", "logs", "settings")
            .forEach { section ->
            onNodeWithTag(LauncherTestTags.instanceSection(section)).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun compactInstanceWorkspaceMovesBetweenDetailAndSectionList() = runComposeUiTest {
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = NoopLauncherUiActions,
                    initialDestination = LauncherDestination.INSTANCE,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.PRIMARY_INSTANCE_ACTION).assertIsDisplayed()
        onNodeWithTag(LauncherTestTags.INSTANCE_BACK).performClick()

        listOf("overview", "content", "worlds", "servers", "screenshots", "notes", "logs", "settings")
            .forEach { section ->
            onNodeWithTag(LauncherTestTags.instanceSection(section)).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun changingVersionComponentsOpensTheInstanceSettingsSection() = runComposeUiTest {
        var settingsRequests = 0
        val actions = object : LauncherUiActions {
            override fun openInstanceSettings() {
                settingsRequests += 1
            }
        }
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state = LauncherPreviewFixtures.loaded,
                    actions = actions,
                    initialDestination = LauncherDestination.INSTANCE,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.VERSION_COMPONENTS_CHANGE).performScrollTo().performClick()

        assertEquals(1, settingsRequests)
        onNodeWithTag(LauncherTestTags.INSTANCE_CONFIGURATION).assertIsDisplayed()
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
    fun restrictedRuntimePresentsFixedCreateOptionsWithoutDeadFilters() = runComposeUiTest {
        val state = LauncherPreviewFixtures.createDialog.copy(
            supportedMinecraftVersions = setOf(LauncherPreviewFixtures.release.id),
            supportedModLoaders = setOf(ModLoader.VANILLA),
        )
        setContent {
            Box(Modifier.size(480.dp, 720.dp)) {
                TrestleApp(
                    state,
                    NoopLauncherUiActions,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(480, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.FIXED_CREATE_VERSION)
            .assertIsDisplayed()
            .assertHasNoClickAction()
        onNodeWithTag(LauncherTestTags.FIXED_CREATE_LOADER)
            .assertIsDisplayed()
            .assertHasNoClickAction()
        onAllNodesWithTag(LauncherTestTags.CREATE_RELEASE_FILTERS).assertCountEquals(0)
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

private fun testWindowAdaptiveInfo(widthDp: Int, heightDp: Int): WindowAdaptiveInfo {
    val minimumWidth = when {
        widthDp >= WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND ->
            WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND
        widthDp >= WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND -> WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND
        widthDp >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND ->
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        widthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND -> WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        else -> 0
    }
    val minimumHeight = when {
        heightDp >= WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND ->
            WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
        heightDp >= WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND ->
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
        else -> 0
    }
    return WindowAdaptiveInfo(WindowSizeClass(minimumWidth, minimumHeight), Posture())
}
