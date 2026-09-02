package net.blockhost.trestle.app

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.cinterop.ExperimentalForeignApi
import net.blockhost.trestle.auth.IosMinecraftAuthenticator
import net.blockhost.trestle.auth.KSafeAccountCredentialStore
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.instance.IosGameDataManager
import net.blockhost.trestle.instance.IosInstanceExporter
import net.blockhost.trestle.logging.IosLogSink
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.resources.IosArchiveExtractor
import net.blockhost.trestle.runtime.IosMinecraftRuntime
import net.blockhost.trestle.runtime.IosRuntimeBridge
import net.blockhost.trestle.runtime.SystemProfile
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
fun createIosLauncherServices(runtimeBridge: IosRuntimeBridge): LauncherServices {
    val nowMillis = { (NSDate().timeIntervalSince1970 * 1_000.0).toLong() }
    val root = requireNotNull(
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path,
    ) { "iOS did not provide an Application Support directory." }.toPath() / "Trestle"
    FileSystem.SYSTEM.createDirectories(root)
    val httpClient = HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15 * 60_000L
            connectTimeoutMillis = 15_000L
        }
    }
    val authenticator = IosMinecraftAuthenticator(httpClient, nowMillis)
    val credentialStore = KSafeAccountCredentialStore(
        KSafe(
            fileName = "credentials",
            memoryPolicy = KSafeMemoryPolicy.ENCRYPTED,
            config = KSafeConfig(appNamespace = "net.blockhost.trestle"),
            directory = (root / "credential-vault").toString(),
        ),
    )
    return LauncherServices.create(
        root = root,
        httpClient = httpClient,
        environment = PlatformEnvironment(
            operatingSystem = OperatingSystem.IOS,
            architecture = Architecture.ARM64,
            osVersion = UIDevice.currentDevice.systemVersion,
        ),
        idFactory = InstanceIdFactory { InstanceId(NSUUID().UUIDString.lowercase()) },
        clock = EpochClock(nowMillis),
        systemProfile = SystemProfile(
            totalMemoryMiB = (NSProcessInfo.processInfo.physicalMemory / (1024uL * 1024uL))
                .toInt()
                .coerceAtLeast(512),
            availableProcessors = NSProcessInfo.processInfo.activeProcessorCount.toInt().coerceAtLeast(1),
            isMobile = true,
        ),
        logSink = IosLogSink(),
        credentialStore = credentialStore,
        authenticator = authenticator,
        curseForgeApiKey = "",
        archiveExtractor = IosArchiveExtractor(),
        instanceExporter = IosInstanceExporter(),
        gameDataManager = IosGameDataManager(),
    ) { directories, installer, sessionProvider, logger, _ ->
        IosMinecraftRuntime(
            bridge = runtimeBridge,
            directories = directories,
            sessionProvider = sessionProvider,
            installedVersionReader = installer::readInstalledVersion,
            logger = logger,
        )
    }
}
