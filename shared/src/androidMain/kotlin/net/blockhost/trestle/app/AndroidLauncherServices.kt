package net.blockhost.trestle.app

import android.content.Context
import android.os.Build
import android.app.ActivityManager
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.auth.JvmMinecraftAuthenticator
import net.blockhost.trestle.auth.KSafeAccountCredentialStore
import net.blockhost.trestle.auth.OfficialMinecraftApplications
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.logging.AndroidLogSink
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.runtime.AndroidMinecraftRuntime
import net.blockhost.trestle.runtime.SystemProfile
import okio.Path.Companion.toPath
import java.util.UUID

fun createAndroidLauncherServices(context: Context): LauncherServices {
    val architectureName = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase().orEmpty()
    val architecture = Architecture.entries.firstOrNull { architectureName in it.aliases } ?: Architecture.UNKNOWN
    val environment = PlatformEnvironment(
        operatingSystem = OperatingSystem.LINUX,
        architecture = architecture,
        osVersion = Build.VERSION.RELEASE,
    )
    val memoryInfo = ActivityManager.MemoryInfo().also { info ->
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
    }
    val credentialStore = KSafeAccountCredentialStore(
        KSafe(
            context = context,
            fileName = "credentials",
            memoryPolicy = KSafeMemoryPolicy.ENCRYPTED,
            config = KSafeConfig(appNamespace = "net.blockhost.trestle"),
            baseDir = context.noBackupFilesDir.resolve("trestle-credential-vault"),
        ),
    )
    return LauncherServices.create(
        root = context.filesDir.resolve("trestle").absolutePath.toPath(),
        httpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
            }
        },
        environment = environment,
        idFactory = InstanceIdFactory { InstanceId(UUID.randomUUID().toString()) },
        clock = EpochClock(System::currentTimeMillis),
        systemProfile = SystemProfile(
            totalMemoryMiB = (memoryInfo.totalMem / (1024L * 1024L)).toInt().coerceAtLeast(512),
            availableProcessors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            isMobile = true,
        ),
        logSink = AndroidLogSink(),
        credentialStore = credentialStore,
        authenticator = JvmMinecraftAuthenticator(
            bedrockConfiguration = OfficialMinecraftApplications.bedrockAndroid,
            nowMillis = System::currentTimeMillis,
        ),
    ) { _, _, _, _, _ -> AndroidMinecraftRuntime() }
}
