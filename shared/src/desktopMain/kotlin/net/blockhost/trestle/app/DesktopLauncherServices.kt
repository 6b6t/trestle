package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.Url
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
import net.blockhost.trestle.instance.JvmInstanceExporter
import net.blockhost.trestle.instance.JvmGameDataManager
import net.blockhost.trestle.runtime.DesktopMinecraftRuntime
import net.blockhost.trestle.runtime.MojangJavaResolver
import net.blockhost.trestle.runtime.SystemProfile
import net.harawata.appdirs.AppDirsFactory
import okio.Path.Companion.toPath
import okio.FileSystem
import java.io.File
import java.lang.management.ManagementFactory
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.UUID

fun createDesktopLauncherServices(): LauncherServices {
    val environment = desktopEnvironment()
    val root = desktopDataDirectory(environment.operatingSystem)
    val rootPath = root.toPath()
    val preferences = LauncherPreferencesStore(FileSystem.SYSTEM, rootPath / "preferences.json").read()
    configureProxyAuthentication(preferences.proxy)
    val loggerSink = Slf4jLogSink()
    val httpClient = HttpClient(CIO) {
        engine {
            proxy = when (preferences.proxy.type) {
                LauncherProxyType.SYSTEM -> null
                LauncherProxyType.NONE -> Proxy.NO_PROXY
                LauncherProxyType.HTTP -> ProxyBuilder.http(
                    Url("http://${preferences.proxy.host}:${preferences.proxy.port}"),
                )
                LauncherProxyType.SOCKS5 -> ProxyBuilder.socks(
                    preferences.proxy.host,
                    preferences.proxy.port,
                )
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = preferences.network.httpTimeoutSeconds * 1_000L
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
        root = rootPath,
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
        instanceExporter = JvmInstanceExporter(),
        gameDataManager = JvmGameDataManager(),
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

private fun configureProxyAuthentication(proxy: ProxyPreferences) {
    if (proxy.type !in setOf(LauncherProxyType.HTTP, LauncherProxyType.SOCKS5) || proxy.username.isBlank()) return
    Authenticator.setDefault(
        object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                if (
                    requestingHost != proxy.host || requestingPort != proxy.port ||
                    requestorType != RequestorType.PROXY
                ) return null
                return PasswordAuthentication(proxy.username, proxy.password.toCharArray())
            }
        },
    )
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
    val appName = when (os) {
        OperatingSystem.WINDOWS, OperatingSystem.MACOS -> "Trestle"
        else -> "trestle"
    }
    // Keep existing data locations while AppDirs resolves the platform-specific base directory.
    return AppDirsFactory.getInstance().getUserDataDir(appName, null, null, os == OperatingSystem.WINDOWS)
}
