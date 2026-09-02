package net.blockhost.trestle.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidGraphicsCompatibilityTest {
    @Test
    fun classifiesSupportedMobileGpuFamilies() {
        assertEquals(
            AndroidGpuFamily.ADRENO,
            AndroidGraphicsCompatibility.gpuFamily("Adreno (TM) 740", "Qualcomm"),
        )
        assertEquals(
            AndroidGpuFamily.MALI,
            AndroidGraphicsCompatibility.gpuFamily("Mali-G715", "ARM"),
        )
        assertEquals(
            AndroidGpuFamily.POWER_VR,
            AndroidGraphicsCompatibility.gpuFamily("PowerVR BXM-8-256", "Imagination Technologies"),
        )
    }

    @Test
    fun enforcesMinecraft26VulkanBaseline() {
        val vulkan11 = (1 shl 22) or (1 shl 12)
        val vulkan12 = (1 shl 22) or (2 shl 12)

        assertFalse(
            AndroidGraphicsCompatibility(AndroidGpuFamily.ADRENO, "Adreno 630", "Qualcomm", vulkan11)
                .isSupported,
        )
        assertTrue(
            AndroidGraphicsCompatibility(AndroidGpuFamily.MALI, "Mali-G715", "ARM", vulkan12)
                .isSupported,
        )
        assertEquals("Vulkan 1.2.0", AndroidGraphicsCompatibility.formatVulkanVersion(vulkan12))
    }

    @Test
    fun selectsMobileGluesForAdreno8xxDevices() {
        val vulkan13 = (1 shl 22) or (3 shl 12)

        assertTrue(
            AndroidGraphicsCompatibility(AndroidGpuFamily.ADRENO, "Adreno (TM) 830", "Qualcomm", vulkan13)
                .prefersMobileGlues,
        )
        assertFalse(
            AndroidGraphicsCompatibility(AndroidGpuFamily.ADRENO, "Adreno (TM) 740", "Qualcomm", vulkan13)
                .prefersMobileGlues,
        )
        assertFalse(
            AndroidGraphicsCompatibility(AndroidGpuFamily.MALI, "Mali-G925", "ARM", vulkan13)
                .prefersMobileGlues,
        )
    }
}
