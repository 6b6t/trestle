package net.blockhost.trestle.desktop

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemAccentTest {
    @Test
    fun `component colors convert to opaque argb`() {
        assertEquals(0xFF1A334D.toInt(), colorFromComponents(0.1, 0.2, 0.3))
    }

    @Test
    fun `invalid component colors are rejected`() {
        assertNull(colorFromComponents(Double.NaN, 0.2, 0.3))
        assertNull(colorFromComponents(-0.1, 0.2, 0.3))
        assertNull(colorFromComponents(0.1, 1.1, 0.3))
    }

    @Test
    fun `system accent publishes the source color`() {
        val sourceRead = CountDownLatch(1)
        val expected = 0xFFE62D42.toInt()
        var sourceClosed = false
        val source = object : SystemAccentSource {
            override fun read(): Int {
                sourceRead.countDown()
                return expected
            }

            override fun close() {
                sourceClosed = true
            }
        }
        SystemAccent(
            source = source,
            refreshIntervalMillis = 60_000,
        ).use { accent ->
            assertTrue(sourceRead.await(2, TimeUnit.SECONDS))
            await { accent.color.value == expected }
        }
        assertTrue(sourceClosed)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (!condition()) {
            assertTrue(System.nanoTime() < deadline, "Condition was not met before the timeout")
            Thread.sleep(10)
        }
    }
}
