package net.blockhost.trestle.desktop

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Taskbar
import java.awt.Window
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal class DesktopIntegration {
    private val appIcon = checkNotNull(javaClass.getResource("/trestle.png")) {
        "The desktop app icon is missing. Run scripts/generate-app-icons.sh."
    }.let(ImageIO::read)
    private val taskbar = runCatching {
        if (Taskbar.isTaskbarSupported()) Taskbar.getTaskbar() else null
    }.getOrNull()
    private val busyBadge = createBadge(Color(0xBE, 0x8F, 0x45), null)
    private val errorBadge = createBadge(Color(0xF0, 0xAA, 0x94), "!")
    private var lastErrorIdentity: String? = null

    fun prepare(window: Window, darkTheme: Boolean) {
        window.minimumSize = Dimension(MINIMUM_WINDOW_WIDTH, MINIMUM_WINDOW_HEIGHT)
        window.iconImages = listOf(appIcon)
        window.background = windowBackground(darkTheme)
        WindowsIntegration.prepareWindow(window, darkTheme)
        taskbar?.runIfSupported(Taskbar.Feature.ICON_IMAGE) { setIconImage(appIcon) }
    }

    fun updateAppearance(window: Window, darkTheme: Boolean) {
        window.background = windowBackground(darkTheme)
        WindowsIntegration.prepareWindow(window, darkTheme)
    }

    fun update(window: Window, indicator: DesktopIndicator) {
        updateProgress(window, indicator)
        updateBadge(window, indicator.badge)

        val errorIdentity = indicator.errorIdentity
        val currentTaskbar = taskbar
        if (errorIdentity != null && errorIdentity != lastErrorIdentity && !window.isActive) {
            when {
                currentTaskbar?.isSupported(Taskbar.Feature.USER_ATTENTION_WINDOW) == true ->
                    runCatching { currentTaskbar.requestWindowUserAttention(window) }
                currentTaskbar?.isSupported(Taskbar.Feature.USER_ATTENTION) == true ->
                    runCatching { currentTaskbar.requestUserAttention(true, true) }
            }
        }
        lastErrorIdentity = errorIdentity
    }

    fun clear(window: Window) {
        updateProgress(
            window,
            DesktopIndicator(DesktopProgressState.IDLE, null, DesktopBadge.NONE, null),
        )
        updateBadge(window, DesktopBadge.NONE)
        lastErrorIdentity = null
    }

    private fun updateProgress(window: Window, indicator: DesktopIndicator) {
        val currentTaskbar = taskbar ?: return
        val taskbarState = when (indicator.progressState) {
            DesktopProgressState.IDLE -> Taskbar.State.OFF
            DesktopProgressState.INDETERMINATE -> Taskbar.State.INDETERMINATE
            DesktopProgressState.NORMAL -> Taskbar.State.NORMAL
            DesktopProgressState.ERROR -> Taskbar.State.ERROR
        }

        currentTaskbar.runIfSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW) {
            setWindowProgressState(window, taskbarState)
        }
        when {
            currentTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW) -> runCatching {
                currentTaskbar.setWindowProgressValue(window, indicator.progressPercent ?: -1)
                if (indicator.progressState != DesktopProgressState.IDLE) {
                    currentTaskbar.setWindowProgressState(window, taskbarState)
                }
            }
            currentTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE) -> runCatching {
                currentTaskbar.setProgressValue(indicator.progressPercent ?: -1)
            }
        }
    }

    private fun updateBadge(window: Window, badge: DesktopBadge) {
        val currentTaskbar = taskbar ?: return
        when {
            currentTaskbar.isSupported(Taskbar.Feature.ICON_BADGE_IMAGE_WINDOW) -> runCatching {
                val image = when (badge) {
                    DesktopBadge.NONE -> null
                    DesktopBadge.BUSY -> busyBadge
                    DesktopBadge.ERROR -> errorBadge
                }
                currentTaskbar.setWindowIconBadge(window, image)
            }
            currentTaskbar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT) -> runCatching {
                currentTaskbar.setIconBadge(
                    when (badge) {
                        DesktopBadge.NONE -> null
                        DesktopBadge.BUSY -> "•"
                        DesktopBadge.ERROR -> "!"
                    },
                )
            }
            currentTaskbar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER) -> runCatching {
                currentTaskbar.setIconBadge(if (badge == DesktopBadge.NONE) null else "1")
            }
        }
    }

    private fun Taskbar.runIfSupported(feature: Taskbar.Feature, action: Taskbar.() -> Unit) {
        if (isSupported(feature)) runCatching { action() }
    }

    private fun windowBackground(darkTheme: Boolean): Color =
        if (darkTheme) Color(0x17, 0x17, 0x15) else Color(0xF8, 0xF5, 0xED)

    private fun createBadge(color: Color, glyph: String?): BufferedImage =
        BufferedImage(BADGE_SIZE, BADGE_SIZE, BufferedImage.TYPE_INT_ARGB).also { image ->
            val graphics = image.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.color = Color(0x17, 0x17, 0x15)
                graphics.fillOval(0, 0, BADGE_SIZE, BADGE_SIZE)
                graphics.color = color
                graphics.fillOval(BADGE_BORDER, BADGE_BORDER, BADGE_SIZE - BADGE_BORDER * 2, BADGE_SIZE - BADGE_BORDER * 2)
                if (glyph != null) drawBadgeGlyph(graphics, glyph)
            } finally {
                graphics.dispose()
            }
        }

    private fun drawBadgeGlyph(graphics: Graphics2D, glyph: String) {
        graphics.color = Color(0x17, 0x17, 0x15)
        graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 38)
        val metrics = graphics.fontMetrics
        graphics.drawString(
            glyph,
            (BADGE_SIZE - metrics.stringWidth(glyph)) / 2,
            (BADGE_SIZE - metrics.height) / 2 + metrics.ascent - 1,
        )
    }

    private companion object {
        const val MINIMUM_WINDOW_WIDTH = 760
        const val MINIMUM_WINDOW_HEIGHT = 560
        const val BADGE_SIZE = 64
        const val BADGE_BORDER = 6
    }
}
