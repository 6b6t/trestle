package net.blockhost.trestle.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameInstanceTest {
    @Test
    fun installedInstanceCanPrepareLaunch() {
        assertTrue(instance(InstallationState.Installed(100)).canPrepareLaunch())
    }

    @Test
    fun installingInstanceCannotLaunch() {
        assertFalse(instance(InstallationState.Installing(5, 10, 1, 2)).canPrepareLaunch())
    }

    private fun instance(state: InstallationState) = GameInstance(
        id = InstanceId("test01"),
        displayName = "Test instance",
        minecraftVersionId = "1.21.8",
        modLoader = ModLoader.FABRIC,
        loaderVersion = "0.17.2",
        instanceDirectory = "/instances/test01",
        requiredJavaMajor = 21,
        installationState = state,
    )
}
