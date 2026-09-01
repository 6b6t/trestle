package net.blockhost.trestle.download

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class ProgressUpdateThrottleTest {
    @Test
    fun limitsUpdatesWithoutDelayingForcedProgress() {
        val timeSource = TestTimeSource()
        val throttle = ProgressUpdateThrottle(250.milliseconds, timeSource)

        assertTrue(throttle.shouldUpdate())
        assertFalse(throttle.shouldUpdate())

        timeSource += 249.milliseconds
        assertFalse(throttle.shouldUpdate())

        timeSource += 1.milliseconds
        assertTrue(throttle.shouldUpdate())
        assertFalse(throttle.shouldUpdate())
        assertTrue(throttle.shouldUpdate(force = true))
    }
}
