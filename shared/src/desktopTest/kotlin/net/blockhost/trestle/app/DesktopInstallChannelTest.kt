package net.blockhost.trestle.app

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopInstallChannelTest {
    private fun channel(
        environment: Map<String, String> = emptyMap(),
        files: Set<String> = emptySet(),
        pacmanPackages: List<String> = emptyList(),
        launcherPath: String? = "/opt/trestle/bin/Trestle",
        rpm: Boolean = false,
    ) = linuxInstallChannel(
        environment = environment,
        exists = { it in files },
        list = { if (it == "/var/lib/pacman/local") pacmanPackages else emptyList() },
        launcherPath = launcherPath,
        rpmOwnsTrestle = { rpm },
    )

    @Test
    fun sandboxesAndPackageManagersWin() {
        assertEquals(
            InstallChannel.PackageManager(ManagedInstall.FLATPAK),
            channel(environment = mapOf("FLATPAK_ID" to "net.blockhost.trestle")),
        )
        assertEquals(InstallChannel.PackageManager(ManagedInstall.FLATPAK), channel(files = setOf("/.flatpak-info")))
        assertEquals(
            InstallChannel.PackageManager(ManagedInstall.AUR),
            channel(pacmanPackages = listOf("glibc-2.42-1", "trestle-bin-1.2.3-1")),
        )
    }

    @Test
    fun appImagesAndArchivesAreRecognizedWhereverTheyRun() {
        assertEquals(
            InstallChannel.Download("AppImage"),
            channel(environment = mapOf("APPIMAGE" to "/home/a/Trestle.AppImage"), launcherPath = "/tmp/.mount_x/trestle/bin/Trestle"),
        )
        assertEquals(InstallChannel.Download("tar.gz"), channel(launcherPath = "/home/a/trestle/bin/Trestle"))
        assertEquals(InstallChannel.Download("tar.gz"), channel(launcherPath = null))
    }

    @Test
    fun systemPackagesAreToldApartByTheirDatabase() {
        assertEquals(InstallChannel.Download("deb"), channel(files = setOf("/var/lib/dpkg/info/trestle.list")))
        assertEquals(InstallChannel.Download("rpm"), channel(rpm = true))
        assertEquals(InstallChannel.Download("tar.gz"), channel())
    }
}
