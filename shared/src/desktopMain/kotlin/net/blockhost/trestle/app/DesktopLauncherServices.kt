package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import net.blockhost.trestle.auth.JvmMinecraftAuthenticator
import net.blockhost.trestle.auth.KSafeAccountCredentialStore
import net.blockhost.trestle.auth.OfficialMinecraftApplications
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.logging.Slf4jLogSink
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.resources.JvmArchiveExtractor
import net.blockhost.trestle.runtime.DesktopMinecraftRuntime
import net.blockhost.trestle.runtime.MojangJavaResolver
import net.blockhost.trestle.runtime.SystemProfile
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Path
import java.lang.management.ManagementFactory
import java.util.UUID

fun createDesktopLauncherServices(): LauncherServices {
    val environment = desktopEnvironment()
    val root = desktopDataDirectory(environment.operatingSystem)
    val loggerSink = Slf4jLogSink()
    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
    }
    val credentialStore = KSafeAccountCredentialStore(
        KSafe(
            fileName = "credentials",
            memoryPolicy = KSafeMemoryPolicy.ENCRYPTED,
            config = KSafeConfig(appNamespace = "net.blockhost.trestle"),
            baseDir = File(root, "credential-vault"),
        ),
    )
    return LauncherServices.create(
        root = root.toPath(),
        httpClient = httpClient,
        environment = environment,
        idFactory = InstanceIdFactory { InstanceId(UUID.randomUUID().toString()) },
        clock = EpochClock(System::currentTimeMillis),
        systemProfile = desktopSystemProfile(),
        logSink = loggerSink,
        credentialStore = credentialStore,
        authenticator = JvmMinecraftAuthenticator(
            bedrockConfiguration = OfficialMinecraftApplications.bedrockDesktop,
            nowMillis = System::currentTimeMillis,
        ),
        curseForgeApiKey = System.getenv("TRESTLE_CURSEFORGE_API_KEY")
            ?: System.getProperty("trestle.curseforge.apiKey").orEmpty(),
        archiveExtractor = JvmArchiveExtractor(),
    ) { directories, installer, sessionProvider, logger, downloadPipeline ->
        DesktopMinecraftRuntime(
            environment = environment,
            directories = directories,
            sessionProvider = sessionProvider,
            installedVersionReader = installer::readInstalledVersion,
            javaResolver = MojangJavaResolver(
                environment = environment,
                directories = directories,
                httpClient = httpClient,
                downloadPipeline = downloadPipeline,
                logger = logger,
            ),
            logger = logger,
        )
    }
}

private fun desktopSystemProfile(): SystemProfile {
    val operatingSystem = ManagementFactory.getOperatingSystemMXBean()
    val totalMemory = (operatingSystem as? com.sun.management.OperatingSystemMXBean)?.totalMemorySize
        ?: Runtime.getRuntime().maxMemory()
    return SystemProfile(
        totalMemoryMiB = (totalMemory / (1024L * 1024L)).toInt().coerceAtLeast(512),
        availableProcessors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
        isMobile = false,
    )
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
