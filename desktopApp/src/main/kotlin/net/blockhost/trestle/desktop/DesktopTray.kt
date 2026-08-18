package net.blockhost.trestle.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import com.sun.jna.Platform
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Menu
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect

internal sealed interface DesktopTrayMenuEntry {
    data class Item(
        val label: String,
        val enabled: Boolean = true,
        val onClick: () -> Unit,
    ) : DesktopTrayMenuEntry

    data class Submenu(
        val label: String,
        val entries: List<DesktopTrayMenuEntry>,
    ) : DesktopTrayMenuEntry

    data object Separator : DesktopTrayMenuEntry
}

@Composable
internal fun ApplicationScope.DesktopTray(
    icon: Painter,
    state: TrayState,
    tooltip: String,
    onAction: () -> Unit,
    menu: List<DesktopTrayMenuEntry>,
) {
    if (Platform.isLinux()) {
        FixedSizeAwtTray(
            icon = icon,
            state = state,
            tooltip = tooltip,
            onAction = onAction,
            menu = menu,
        )
    } else {
        Tray(
            icon = icon,
            state = state,
            tooltip = tooltip,
            onAction = onAction,
        ) {
            ComposeTrayMenu(menu)
        }
    }
}

@Composable
private fun FixedSizeAwtTray(
    icon: Painter,
    state: TrayState,
    tooltip: String,
    onAction: () -> Unit,
    menu: List<DesktopTrayMenuEntry>,
) {
    val currentOnAction by rememberUpdatedState(onAction)
    val traySize = remember { SystemTray.getSystemTray().trayIconSize }
    // GNOME's legacy tray bridge can crop Compose's requested HiDPI image variant into a 1x slot.
    // Give AWT one concrete image at the tray's reported size so the complete mark is embedded.
    val awtIcon = remember(icon, traySize) { renderTrayIcon(icon, traySize) }
    val trayIcon = remember(awtIcon) {
        TrayIcon(awtIcon).apply {
            isImageAutoSize = false
            addActionListener { currentOnAction() }
        }
    }

    SideEffect {
        trayIcon.toolTip = tooltip
        trayIcon.popupMenu = createPopupMenu(menu)
    }

    DisposableEffect(trayIcon) {
        val systemTray = SystemTray.getSystemTray()
        systemTray.add(trayIcon)
        onDispose {
            systemTray.remove(trayIcon)
        }
    }

    LaunchedEffect(trayIcon, state) {
        state.notificationFlow.collect { notification ->
            trayIcon.displayMessage(
                notification.title,
                notification.message,
                notification.toAwtMessageType(),
            )
        }
    }
}

internal fun renderTrayIcon(icon: Painter, traySize: Dimension): BufferedImage {
    val width = max(1, traySize.width)
    val height = max(1, traySize.height)
    val padding = max(1, minOf(width, height) / 12)
    val availableWidth = max(1, width - padding * 2)
    val availableHeight = max(1, height - padding * 2)
    val intrinsicSize = icon.intrinsicSize
    val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
        intrinsicSize.width / intrinsicSize.height
    } else {
        1f
    }
    val drawWidth: Int
    val drawHeight: Int
    if (availableWidth / availableHeight.toFloat() > aspectRatio) {
        drawHeight = availableHeight
        drawWidth = max(1, (drawHeight * aspectRatio).roundToInt())
    } else {
        drawWidth = availableWidth
        drawHeight = max(1, (drawWidth / aspectRatio).roundToInt())
    }

    val deferredImage = icon.toAwtImage(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        size = Size(drawWidth.toFloat(), drawHeight.toFloat()),
    )
    val renderedImage = (deferredImage as MultiResolutionImage).getResolutionVariant(
        drawWidth.toDouble(),
        drawHeight.toDouble(),
    )
    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        createGraphics().use { graphics ->
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(
                renderedImage,
                (width - drawWidth) / 2,
                (height - drawHeight) / 2,
                drawWidth,
                drawHeight,
                null,
            )
        }
    }
}

private inline fun <T : Graphics2D, R> T.use(block: (T) -> R): R = try {
    block(this)
} finally {
    dispose()
}

private fun createPopupMenu(entries: List<DesktopTrayMenuEntry>) = PopupMenu().apply {
    entries.forEach { entry ->
        when (entry) {
            is DesktopTrayMenuEntry.Item -> add(entry.toAwtItem())
            is DesktopTrayMenuEntry.Submenu -> add(entry.toAwtMenu())
            DesktopTrayMenuEntry.Separator -> addSeparator()
        }
    }
}

private fun DesktopTrayMenuEntry.Item.toAwtItem() = MenuItem(label).apply {
    isEnabled = enabled
    addActionListener { onClick() }
}

private fun DesktopTrayMenuEntry.Submenu.toAwtMenu(): Menu = Menu(label).apply {
    entries.forEach { entry ->
        when (entry) {
            is DesktopTrayMenuEntry.Item -> add(entry.toAwtItem())
            is DesktopTrayMenuEntry.Submenu -> add(entry.toAwtMenu())
            DesktopTrayMenuEntry.Separator -> addSeparator()
        }
    }
}

@Composable
private fun MenuScope.ComposeTrayMenu(entries: List<DesktopTrayMenuEntry>) {
    entries.forEach { entry ->
        when (entry) {
            is DesktopTrayMenuEntry.Item -> Item(
                text = entry.label,
                enabled = entry.enabled,
                onClick = entry.onClick,
            )

            is DesktopTrayMenuEntry.Submenu -> Menu(entry.label) {
                ComposeTrayMenu(entry.entries)
            }

            DesktopTrayMenuEntry.Separator -> Separator()
        }
    }
}

private fun Notification.toAwtMessageType(): TrayIcon.MessageType = when (type) {
    Notification.Type.None -> TrayIcon.MessageType.NONE
    Notification.Type.Info -> TrayIcon.MessageType.INFO
    Notification.Type.Warning -> TrayIcon.MessageType.WARNING
    Notification.Type.Error -> TrayIcon.MessageType.ERROR
}
