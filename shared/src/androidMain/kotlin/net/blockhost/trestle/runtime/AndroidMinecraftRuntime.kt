package net.blockhost.trestle.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException

class AndroidMinecraftRuntime : MinecraftRuntime {
    override val capabilities = RuntimeCapabilities(
        canPrepareLaunch = false,
        canLaunch = false,
        supportsManagedJava = false,
        supportsNativeExtraction = false,
        unavailableReason = MESSAGE,
    )

    override suspend fun prepare(instance: GameInstance, options: LaunchOptions): PreparedLaunch {
        throw LauncherException.RuntimeUnavailable(MESSAGE)
    }

    override fun launch(preparedLaunch: PreparedLaunch): Flow<LaunchEvent> = flow {
        emit(LaunchEvent.Failed(MESSAGE))
    }

    private companion object {
        const val MESSAGE = "The Android native Minecraft runtime is not installed."
    }
}
