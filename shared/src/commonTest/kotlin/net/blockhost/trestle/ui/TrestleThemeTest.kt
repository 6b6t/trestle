package net.blockhost.trestle.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrestleThemeTest {
    @Test
    fun `theme keeps its brand accent without a system color`() {
        assertEquals(Color(0xFFBE8F45), trestleColorScheme().primary)
    }

    @Test
    fun `dark system accent remains visible on the theme surface`() {
        val scheme = trestleColorScheme(Color(0xFF050505))

        assertTrue(contrastRatio(scheme.primary, scheme.surface) >= 3f)
    }

    @Test
    fun `primary content remains legible for a system accent`() {
        val scheme = trestleColorScheme(Color(0xFF777777))

        assertTrue(contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
    }

    @Test
    fun `system accent remains visible in light appearance`() {
        val scheme = trestleColorScheme(Color(0xFFE8D62F), darkTheme = false)

        assertTrue(contrastRatio(scheme.primary, scheme.surface) >= 3f)
        assertTrue(contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
    }
}
