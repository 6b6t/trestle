package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.app.LauncherPreferences
import net.blockhost.trestle.app.LauncherProxyType
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AccountsAndSettingsUiTest {
    @Test
    fun signInErrorsShowInsideTheDialog() = runComposeUiTest {
        val state = LauncherPreviewFixtures.accountDialog.copy(
            accountLogin = LauncherPreviewFixtures.accountDialog.accountLogin.copy(error = "The sign-in code expired."),
        )
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    state,
                    NoopLauncherUiActions,
                    initialDestination = LauncherDestination.ACCOUNTS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithTag(LauncherTestTags.ACCOUNT_LOGIN_ERROR).assertTextEquals("The sign-in code expired.")
        onNodeWithText("Get sign-in code").assertIsDisplayed()
    }

    @Test
    fun signingOutAsksForConfirmation() = runComposeUiTest {
        val events = mutableListOf<String>()
        val actions = object : LauncherUiActions {
            override fun signOutAccount(profileId: String) {
                events += "sign-out:$profileId"
            }
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded,
                    actions,
                    initialDestination = LauncherDestination.ACCOUNTS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }

        onNodeWithText("Sign out").performClick()
        onNodeWithText("Sign out?").assertIsDisplayed()
        assertEquals(emptyList(), events)
    }

    @Test
    fun proxyPortSavesOnlyValidCommittedValues() = runComposeUiTest {
        val saved = mutableListOf<LauncherPreferences>()
        val actions = object : LauncherUiActions {
            override fun setLauncherPreferences(value: LauncherPreferences) {
                saved += value
            }
        }
        val preferences = LauncherPreferences().let {
            it.copy(proxy = it.proxy.copy(type = LauncherProxyType.HTTP, port = 8080))
        }
        setContent {
            Box(Modifier.size(1000.dp, 720.dp)) {
                TrestleApp(
                    LauncherPreviewFixtures.loaded.copy(launcherPreferences = preferences),
                    actions,
                    initialDestination = LauncherDestination.SETTINGS,
                    windowAdaptiveInfo = testWindowAdaptiveInfo(1000, 720),
                )
            }
        }
        onNodeWithText("Proxy").performClick()

        onNodeWithText("8080").performTextClearance()
        onNodeWithText("1–65535").assertIsDisplayed()
        assertEquals(emptyList(), saved)
    }
}
