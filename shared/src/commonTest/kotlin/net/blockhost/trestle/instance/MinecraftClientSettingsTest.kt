package net.blockhost.trestle.instance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftClientSettingsTest {
    private val settings = MinecraftClientSettings()

    @Test
    fun addsOptionsAtTheirSnapshotCompatibilityBoundaries() {
        val initialModernOptions = settings.toOptionsText("13w36a")
        val narratorOptions = settings.toOptionsText("17w06a")
        val beforeSimulationDistance = settings.toOptionsText("21w37a")
        val simulationDistanceOptions = settings.toOptionsText("21w38a")

        assertTrue("renderDistance:12" in initialModernOptions)
        assertFalse("showSubtitles:" in initialModernOptions)
        assertFalse("narrator:" in initialModernOptions)
        assertTrue("narrator:0" in narratorOptions)
        assertFalse("simulationDistance:" in beforeSimulationDistance)
        assertTrue("simulationDistance:8" in simulationDistanceOptions)
    }

    @Test
    fun recognizesModernPrereleaseAndCalendarVersionIdentifiers() {
        assertTrue("narrator:0" in settings.toOptionsText("1.21.9-rc1"))
        assertTrue("simulationDistance:8" in settings.toOptionsText("26.1.2"))
    }

    @Test
    fun readsSupportedValuesAndKeepsUnknownOptionsWhenMerging() {
        val existing = """
            fov:-0.5
            gamma:1.0
            mouseSensitivity:0.35
            maxFps:60
            particles:2
            customModOption:unchanged
        """.trimIndent() + "\n"

        val parsed = requireNotNull(MinecraftClientSettings.fromOptionsText(existing, "1.21.8"))

        assertEquals(50, parsed.fieldOfViewDegrees)
        assertEquals(100, parsed.brightnessPercent)
        assertEquals(35, parsed.mouseSensitivityPercent)
        assertEquals(60, parsed.maximumFrameRate)
        assertEquals(MinecraftParticleSetting.MINIMAL, parsed.particles)

        val merged = parsed.copy(maximumFrameRate = 144, fullscreen = true)
            .mergeIntoOptionsText(existing, "1.21.8")

        assertTrue("maxFps:144" in merged)
        assertTrue("fullscreen:true" in merged)
        assertTrue("customModOption:unchanged" in merged)
    }
}
