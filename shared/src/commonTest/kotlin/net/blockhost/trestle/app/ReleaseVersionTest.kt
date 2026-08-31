package net.blockhost.trestle.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseVersionTest {
    @Test fun ordersPrereleasesAccordingToSemver() {
        val ordered = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1")
            .map { requireNotNull(ReleaseVersion.parse(it)) }
        ordered.zipWithNext().forEach { (left, right) -> assertTrue(left < right); assertTrue(right > left) }
        assertEquals(0, ReleaseVersion.parse("v2.0.0+build.42")!!.compareTo(ReleaseVersion.parse("2.0.0+other")!!))
    }

    @Test fun ignoresMalformedTagsRatherThanOfferingSpuriousUpdates() {
        listOf("latest", "", "01.0.0", "1.2", "1.0.0-01", "1.0.0-", "1.0.0+", "-1.0.0").forEach {
            assertNull(ReleaseVersion.parse(it))
        }
    }
}
