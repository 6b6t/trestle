package net.blockhost.trestle.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import java.awt.Dimension
import java.awt.image.MultiResolutionImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class DesktopTrayTest {
    @Test
    fun rendersPlainImageAtExactTraySize() {
        val image = renderTrayIcon(ColorPainter(Color.White), Dimension(24, 20))

        assertEquals(24, image.width)
        assertEquals(20, image.height)
        assertFalse(image is MultiResolutionImage)
    }

    @Test
    fun leavesTransparentPaddingAroundIcon() {
        val image = renderTrayIcon(ColorPainter(Color.White), Dimension(24, 24))

        assertEquals(0, image.getRGB(0, 0))
        assertNotEquals(0, image.getRGB(image.width / 2, image.height / 2))
    }
}
