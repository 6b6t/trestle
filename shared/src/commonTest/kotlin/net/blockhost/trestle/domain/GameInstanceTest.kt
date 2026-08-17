package net.blockhost.trestle.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameInstanceTest {
    @Test
    fun readyInstanceCanLaunch() {
        assertTrue(instance(InstanceState.Ready).canLaunch())
    }

    @Test
    fun installingInstanceCannotLaunch() {
        assertFalse(instance(InstanceState.Installing(progress = 0.5f)).canLaunch())
    }

    private fun instance(state: InstanceState) = GameInstance(
        id = InstanceId("test"),
        name = "Test instance",
        gameVersion = "1.21.8",
        modLoader = ModLoader.FABRIC,
        javaVersion = 21,
        state = state,
    )
}
