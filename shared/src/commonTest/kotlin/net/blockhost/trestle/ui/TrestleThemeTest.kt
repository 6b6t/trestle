package net.blockhost.trestle.ui

import androidx.compose.material3.darkColorScheme
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

    @Test
    fun `high contrast preserves a supplied color scheme while strengthening outlines`() {
        val supplied = darkColorScheme(
            primary = Color(0xFF7BA4FF),
            onSurface = Color.White,
            surface = Color(0xFF181818),
            outline = Color(0xFF454545),
        )

        val contrasted = supplied.withHighContrast(true)

        assertEquals(supplied.primary, contrasted.primary)
        assertEquals(supplied.onSurface, contrasted.outline)
        assertTrue(contrastRatio(contrasted.outlineVariant, contrasted.surface) >= 3f)
    }

    @Test
    fun `brand schemes define the complete fixed and surface color roles`() {
        val dark = trestleColorScheme(darkTheme = true)
        val light = trestleColorScheme(darkTheme = false)

        assertEquals(dark.primary, dark.surfaceTint)
        assertEquals(light.primary, light.surfaceTint)
        assertEquals(light.primaryFixed, dark.primaryFixed)
        assertEquals(light.primaryFixedDim, dark.primaryFixedDim)
        assertEquals(light.secondaryFixed, dark.secondaryFixed)
        assertEquals(light.tertiaryFixed, dark.tertiaryFixed)
        assertTrue(contrastRatio(dark.onPrimaryFixed, dark.primaryFixed) >= 4.5f)
        assertTrue(contrastRatio(dark.onSecondaryFixed, dark.secondaryFixed) >= 4.5f)
    }

    @Test
    fun `system accent updates fixed roles without reducing content contrast`() {
        val base = trestleColorScheme()
        val accented = trestleColorScheme(Color(0xFF2B6E45))

        assertTrue(accented.primaryFixed != base.primaryFixed)
        assertEquals(accented.primary, accented.surfaceTint)
        assertTrue(contrastRatio(accented.onPrimaryFixed, accented.primaryFixed) >= 4.5f)
        assertTrue(contrastRatio(accented.onPrimaryFixedVariant, accented.primaryFixedDim) >= 4.5f)
    }
}
