package net.blockhost.trestle.runtime

import kotlinx.coroutines.flow.Flow
import net.blockhost.trestle.auth.SecretValue
import net.blockhost.trestle.domain.GameInstance

data class RuntimeCapabilities(
    val canPrepareLaunch: Boolean,
    val canLaunch: Boolean,
    val supportsManagedJava: Boolean,
    val supportsNativeExtraction: Boolean,
    val unavailableReason: String? = null,
)

data class LaunchOptions(
    val demo: Boolean = false,
    val additionalJvmArguments: List<String> = emptyList(),
    val additionalGameArguments: List<String> = emptyList(),
)

sealed interface CommandArgument {
    data class Public(val value: String) : CommandArgument
    data class Secret(val value: SecretValue) : CommandArgument
    data class RequiredCredential(val name: String) : CommandArgument
}

data class PreparedLaunch(
    val instanceId: String,
    val executable: String,
    val arguments: List<CommandArgument>,
    val workingDirectory: String,
    val environment: Map<String, String> = emptyMap(),
    val mainClass: String,
    val classpathEntries: List<String>,
    val nativeDirectory: String,
    val missingRequirements: List<String> = emptyList(),
) {
    fun safeCommand(): List<String> = listOf(executable) + arguments.map {
        when (it) {
            is CommandArgument.Public -> it.value
            is CommandArgument.Secret -> "[REDACTED]"
            is CommandArgument.RequiredCredential -> "<required:${it.name}>"
        }
    }

    internal fun processArguments(): List<String> = arguments.map {
        when (it) {
            is CommandArgument.Public -> it.value
            is CommandArgument.Secret -> it.value.reveal()
            is CommandArgument.RequiredCredential -> throw net.blockhost.trestle.domain.LauncherException.AuthenticationRequired()
        }
    }
}

sealed interface LaunchEvent {
    data class Started(val processId: Long?) : LaunchEvent
    data class Log(val line: String) : LaunchEvent
    data class Exited(val exitCode: Int) : LaunchEvent
    data class Failed(val message: String) : LaunchEvent
    data object Cancelled : LaunchEvent
}

interface MinecraftRuntime {
    val capabilities: RuntimeCapabilities

    suspend fun prepare(instance: GameInstance, options: LaunchOptions = LaunchOptions()): PreparedLaunch
    fun launch(preparedLaunch: PreparedLaunch): Flow<LaunchEvent>
}
