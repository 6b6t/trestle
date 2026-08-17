package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.MemorySettings
import net.blockhost.trestle.domain.ModLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LaunchTuningTest {
    @Test
    fun keepsEnoughMemoryAvailableOnMobileDevices() {
        val instance = GameInstance(
            id = InstanceId("instance-a"),
            displayName = "Fabric",
            minecraftVersionId = "1.21.8",
            modLoader = ModLoader.FABRIC,
            loaderVersion = "0.16.0",
            instanceDirectory = "/instances/a",
            memory = MemorySettings(512, 4_096),
        )

        val recommendation = LaunchTuningAdvisor.recommend(instance, SystemProfile(4_096, 8, isMobile = true))

        assertEquals(3_072, recommendation.memory.maximumMiB)
        assertTrue(recommendation.warnings.isNotEmpty())
    }

    @Test
    fun removesJvmArgumentsManagedByTheLauncher() {
        val review = JvmArgumentPolicy.review(
            listOf("-Xmx8G", "-cp", "custom.jar", "-Dfeature=true", "-Djava.library.path=/tmp"),
        )

        assertEquals(listOf("-Dfeature=true"), review.accepted)
        assertEquals(listOf("-Xmx8G", "-cp", "custom.jar", "-Djava.library.path=/tmp"), review.ignored)
    }
}
