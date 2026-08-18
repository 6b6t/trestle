package net.blockhost.trestle.desktop

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.sun.jna.Platform
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class SystemDarkMode(
    private val readDarkMode: () -> Boolean = ::readSystemDarkMode,
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Trestle system appearance").apply { isDaemon = true }
    },
) : AutoCloseable {
    private val mutableDark = mutableStateOf(readDarkMode())
    private val mutableHighContrast = mutableStateOf(readSystemHighContrast())
    private val mutableReducedMotion = mutableStateOf(readSystemReducedMotion())
    val dark: State<Boolean> = mutableDark
    val highContrast: State<Boolean> = mutableHighContrast
    val reducedMotion: State<Boolean> = mutableReducedMotion

    init {
        executor.scheduleWithFixedDelay(::refresh, 5, 5, TimeUnit.SECONDS)
    }

    private fun refresh() {
        val value = runCatching(readDarkMode).getOrDefault(true)
        val highContrast = runCatching(::readSystemHighContrast).getOrDefault(false)
        val reducedMotion = runCatching(::readSystemReducedMotion).getOrDefault(false)
        if (
            value == mutableDark.value &&
            highContrast == mutableHighContrast.value &&
            reducedMotion == mutableReducedMotion.value
        ) {
            return
        }
        Snapshot.withMutableSnapshot {
            mutableDark.value = value
            mutableHighContrast.value = highContrast
            mutableReducedMotion.value = reducedMotion
        }
    }

    override fun close() {
        executor.shutdownNow()
    }
}

private fun readSystemHighContrast(): Boolean = when {
    Platform.isWindows() -> java.awt.Toolkit.getDefaultToolkit()
        .getDesktopProperty("win.highContrast.on") as? Boolean ?: false
    Platform.isMac() -> readDefaultsBoolean("com.apple.universalaccess", "increaseContrast")
    Platform.isLinux() -> System.getenv("GTK_THEME").orEmpty().contains("highcontrast", ignoreCase = true)
    else -> false
}

private fun readSystemReducedMotion(): Boolean = when {
    Platform.isWindows() -> runCatching {
        Advapi32Util.registryGetStringValue(
            WinReg.HKEY_CURRENT_USER,
            "Control Panel\\Accessibility",
            "Animation",
        ) == "0"
    }.getOrDefault(false)
    Platform.isMac() -> readDefaultsBoolean("com.apple.universalaccess", "reduceMotion")
    Platform.isLinux() -> runCatching {
        ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "enable-animations")
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().use { it.readText() }
            .trim() == "false"
    }.getOrDefault(false)
    else -> false
}

private fun readDefaultsBoolean(domain: String, key: String): Boolean = runCatching {
    ProcessBuilder("defaults", "read", domain, key)
        .redirectErrorStream(true)
        .start()
        .inputStream.bufferedReader().use { it.readText() }
        .trim() == "1"
}.getOrDefault(false)

private fun readSystemDarkMode(): Boolean = when {
    Platform.isWindows() -> runCatching {
        Advapi32Util.registryGetIntValue(
            WinReg.HKEY_CURRENT_USER,
            "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "AppsUseLightTheme",
        ) == 0
    }.getOrDefault(true)
    Platform.isMac() -> runCatching {
        ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().use { it.readText() }
            .contains("dark", ignoreCase = true)
    }.getOrDefault(false)
    Platform.isLinux() -> {
        val theme = System.getenv("GTK_THEME").orEmpty()
        theme.contains("dark", ignoreCase = true) || runCatching {
            ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().use { it.readText() }
                .contains("prefer-dark", ignoreCase = true)
        }.getOrDefault(false)
    }
    else -> true
}
