package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCheckerTest {
    @Test
    fun returnsOnlyNewerSemanticRelease() = runTest {
        val newer = UpdateChecker(
            HttpClient(MockEngine { respond("""{"tag_name":"v1.2.0","html_url":"https://example.test/1.2.0"}""") }),
            "https://example.test/latest",
        )
        val current = UpdateChecker(
            HttpClient(MockEngine { respond("""{"tag_name":"v1.1.0","html_url":"https://example.test/1.1.0"}""") }),
            "https://example.test/latest",
        )

        assertEquals("1.2.0", newer.availableUpdate("1.1.9")?.version)
        assertNull(current.availableUpdate("1.1.0"))
    }
}
