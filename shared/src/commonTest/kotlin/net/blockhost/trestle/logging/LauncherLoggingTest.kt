package net.blockhost.trestle.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LauncherLoggingTest {
    @Test
    fun redactsCredentialsBeforeBufferingOrWriting() {
        val written = mutableListOf<LogEntry>()
        val logger = BufferedLauncherLogger(
            nowMillis = { 1234L },
            sink = LogSink { entry, _ -> written += entry },
        )

        logger.info(
            "auth",
            "Authorization: Bearer token-value",
            mapOf("access_token" to "secret-value"),
        )

        val entry = logger.entries.value.single()
        assertEquals(entry, written.single())
        assertFalse(entry.message.contains("token-value"))
        assertFalse(entry.details.values.any { it.contains("secret-value") })
    }
}
