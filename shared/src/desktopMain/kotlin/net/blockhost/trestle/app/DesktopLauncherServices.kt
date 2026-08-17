package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import net.blockhost.trestle.auth.NoSessionProvider
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.runtime.DesktopMinecraftRuntime
import okio.Path.Companion.toPath
import java.nio.file.Path
import java.util.UUID

fun createDesktopLauncherServices(): LauncherServices {
    val environment = desktopEnvironment()
    return LauncherServices.create(
        root = desktopDataDirectory(environment.operatingSystem).toPath(),
        httpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
            }
        },
        environment = environment,
        idFactory = InstanceIdFactory { InstanceId(UUID.randomUUID().toString()) },
        clock = EpochClock(System::currentTimeMillis),
    ) { directories, installer ->
        DesktopMinecraftRuntime(
            environment = environment,
            directories = directories,
            sessionProvider = NoSessionProvider,
            installedVersionReader = installer::readInstalledVersion,
        )
    }
}

private fun desktopEnvironment(): PlatformEnvironment {
    val osName = System.getProperty("os.name").lowercase()
    val os = when {
        osName.contains("win") -> OperatingSystem.WINDOWS
        osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MACOS
        osName.contains("linux") || osName.contains("unix") -> OperatingSystem.LINUX
        else -> OperatingSystem.UNKNOWN
    }
    val architectureName = System.getProperty("os.arch").lowercase()
    val architecture = Architecture.entries.firstOrNull { architectureName in it.aliases } ?: Architecture.UNKNOWN
    return PlatformEnvironment(os, architecture, System.getProperty("os.version"))
}

private fun desktopDataDirectory(os: OperatingSystem): String {
    val home = System.getProperty("user.home")
    return when (os) {
        OperatingSystem.WINDOWS -> Path.of(System.getenv("APPDATA") ?: home, "Trestle").toString()
        OperatingSystem.MACOS -> Path.of(home, "Library", "Application Support", "Trestle").toString()
        else -> Path.of(System.getenv("XDG_DATA_HOME") ?: Path.of(home, ".local", "share").toString(), "trestle")
            .toString()
    }
}
