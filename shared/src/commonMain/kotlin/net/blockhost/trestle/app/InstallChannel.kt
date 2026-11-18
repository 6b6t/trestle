package net.blockhost.trestle.app

/** How this copy of Trestle was installed. It decides which update the launcher offers. */
sealed interface InstallChannel {
    /**
     * Trestle updates by downloading a new package. [format] is the release format to offer, such as `AppImage` or
     * `deb`, or null to offer the first package for this system.
     */
    data class Download(val format: String? = null) : InstallChannel

    /** A package manager owns this installation, so the launcher tells the player to update there. */
    data class PackageManager(val manager: ManagedInstall) : InstallChannel
}

/** Package managers that update Trestle themselves. */
enum class ManagedInstall {
    FLATPAK,
}
