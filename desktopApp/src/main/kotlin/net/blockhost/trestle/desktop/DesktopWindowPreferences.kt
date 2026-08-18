package net.blockhost.trestle.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.GraphicsEnvironment
import java.util.prefs.Preferences
import net.blockhost.trestle.ui.LauncherDestination

internal data class RestoredWindow(
    val size: DpSize = DpSize(1180.dp, 760.dp),
    val position: WindowPosition = WindowPosition.PlatformDefault,
    val placement: WindowPlacement = WindowPlacement.Floating,
    val destination: LauncherDestination = LauncherDestination.LIBRARY,
)

internal class DesktopWindowPreferences(
    private val preferences: Preferences = Preferences.userRoot().node("net/blockhost/trestle/window"),
) {
    fun restore(): RestoredWindow {
        val width = preferences.getFloat("width", 1180f).coerceIn(760f, 4000f)
        val height = preferences.getFloat("height", 760f).coerceIn(560f, 3000f)
        val x = preferences.getInt("x", Int.MIN_VALUE)
        val y = preferences.getInt("y", Int.MIN_VALUE)
        val position = if (x != Int.MIN_VALUE && y != Int.MIN_VALUE && isVisible(x, y)) {
            WindowPosition(x.dp, y.dp)
        } else {
            WindowPosition.PlatformDefault
        }
        val placement = runCatching {
            WindowPlacement.valueOf(preferences.get("placement", WindowPlacement.Floating.name))
        }.getOrDefault(WindowPlacement.Floating).let {
            if (it == WindowPlacement.Fullscreen) WindowPlacement.Floating else it
        }
        val destination = runCatching {
            LauncherDestination.valueOf(
                preferences.get("destination", LauncherDestination.LIBRARY.name),
            )
        }.getOrDefault(LauncherDestination.LIBRARY)
        return RestoredWindow(DpSize(width.dp, height.dp), position, placement, destination)
    }

    fun saveWindow(state: WindowState) {
        if (state.placement == WindowPlacement.Floating) {
            preferences.putFloat("width", state.size.width.value)
            preferences.putFloat("height", state.size.height.value)
            (state.position as? WindowPosition.Absolute)?.let { position ->
                preferences.putInt("x", position.x.value.toInt())
                preferences.putInt("y", position.y.value.toInt())
            }
        }
        preferences.put("placement", state.placement.name)
    }

    fun saveDestination(destination: LauncherDestination) {
        preferences.put("destination", destination.name)
    }

    private fun isVisible(x: Int, y: Int): Boolean = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
            device.defaultConfiguration.bounds.run {
                x in (this.x - 80)..(this.x + width) && y in (this.y - 40)..(this.y + height)
            }
        }
    }.getOrDefault(false)
}
