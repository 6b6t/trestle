package net.blockhost.trestle.app

import android.content.Context
import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.install.EpochClock
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.runtime.AndroidMinecraftRuntime
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
    ) { _, _ -> AndroidMinecraftRuntime() }
}
