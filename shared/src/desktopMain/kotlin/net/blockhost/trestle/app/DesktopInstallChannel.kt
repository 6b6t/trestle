package net.blockhost.trestle.app

import net.blockhost.trestle.metadata.OperatingSystem
import java.io.File
import java.util.concurrent.TimeUnit

/** Detects how this desktop copy of Trestle was installed. */
internal fun desktopInstallChannel(operatingSystem: OperatingSystem): InstallChannel = if (operatingSystem == OperatingSystem.LINUX) {
    linuxInstallChannel(
        environment = System.getenv(),
        exists = { File(it).exists() },
        list = { File(it).list()?.toList().orEmpty() },
        // jpackage launchers set this property to the launcher's own path.
        launcherPath = System.getProperty("jpackage.app-path"),
        rpmOwnsTrestle = ::rpmOwnsTrestle,
    )
} else {
    // Windows and macOS installers and WinGet use the same packages that the launcher offers.
    InstallChannel.Download()
}

/**
 * Decides the Linux install channel from the environment and the package databases. The checks are passed in so tests
 * can describe a system without touching the real one.
 */
internal fun linuxInstallChannel(
    environment: Map<String, String>,
    exists: (String) -> Boolean,
    list: (String) -> List<String>,
    launcherPath: String?,
    rpmOwnsTrestle: () -> Boolean,
): InstallChannel {
    val packaged = launcherPath?.startsWith("$PACKAGE_ROOT/") == true
    return when {
        environment.containsKey("FLATPAK_ID") || exists("/.flatpak-info") -> InstallChannel.PackageManager(ManagedInstall.FLATPAK)

        environment.containsKey("APPIMAGE") -> InstallChannel.Download("AppImage")

        !packaged -> InstallChannel.Download("tar.gz")

        list("/var/lib/pacman/local").any { it.startsWith("trestle-bin-") } -> InstallChannel.PackageManager(ManagedInstall.AUR)

        exists("/var/lib/dpkg/info/trestle.list") -> InstallChannel.Download("deb")

        rpmOwnsTrestle() -> InstallChannel.Download("rpm")

        // Someone unpacked the portable archive into the same folder that the packages use.
        else -> InstallChannel.Download("tar.gz")
    }
}

private const val PACKAGE_ROOT = "/opt/trestle"
private const val RPM_TIMEOUT_SECONDS = 5L

private fun rpmOwnsTrestle(): Boolean = runCatching {
    val process = ProcessBuilder("rpm", "-q", "trestle")
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    val finished = process.waitFor(RPM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    if (!finished) process.destroyForcibly()
    finished && process.exitValue() == 0
}.getOrDefault(false)
