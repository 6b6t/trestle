package net.blockhost.trestle.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class InstanceId(val value: String)

@Serializable
enum class ModLoader(val label: String) {
    VANILLA("Vanilla"),
    FABRIC("Fabric"),
    NEOFORGE("NeoForge"),
    FORGE("Forge"),
    QUILT("Quilt"),
}

@Serializable
data class MemorySettings(
    val minimumMiB: Int = 512,
    val maximumMiB: Int = 2048,
) {
    init {
        require(minimumMiB > 0) { "Minimum memory must be positive." }
        require(maximumMiB >= minimumMiB) { "Maximum memory must not be less than minimum memory." }
    }
}

@Serializable
sealed interface InstallationState {
    @Serializable
    @SerialName("not_installed")
    data object NotInstalled : InstallationState

    @Serializable
    @SerialName("installing")
    data class Installing(
        val completedBytes: Long,
        val totalBytes: Long?,
        val completedFiles: Int,
        val totalFiles: Int,
    ) : InstallationState

    @Serializable
    @SerialName("interrupted")
    data class Interrupted(
        val completedBytes: Long,
        val totalBytes: Long?,
        val completedFiles: Int,
        val totalFiles: Int,
    ) : InstallationState

    @Serializable
    @SerialName("installed")
    data class Installed(val installedAtEpochMillis: Long) : InstallationState

    @Serializable
    @SerialName("failed")
    data class Failed(val message: String) : InstallationState
}

@Serializable
data class GameInstance(
    val id: InstanceId,
    val displayName: String,
    val minecraftVersionId: String,
    val modLoader: ModLoader = ModLoader.VANILLA,
    val loaderVersion: String? = null,
    val instanceDirectory: String,
    val requiredJavaMajor: Int = 8,
    val jvmArguments: List<String> = emptyList(),
    val memory: MemorySettings = MemorySettings(),
    val gameArguments: List<String> = emptyList(),
    val javaExecutable: String? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
    val installationState: InstallationState = InstallationState.NotInstalled,
    val lastLaunchAtEpochMillis: Long? = null,
    val iconReference: String? = null,
    val pinned: Boolean = false,
    val group: String? = null,
    val launchCount: Int = 0,
    val playTimeMillis: Long = 0,
) {
    init {
        require(displayName.isNotBlank()) { "Instance name must not be blank." }
        require(minecraftVersionId.isNotBlank()) { "Minecraft version must not be blank." }
        require(instanceDirectory.isNotBlank()) { "Instance directory must not be blank." }
        require(requiredJavaMajor > 0) { "Java major version must be positive." }
        require(launchCount >= 0) { "Launch count must not be negative." }
        require(playTimeMillis >= 0) { "Play time must not be negative." }
        require(javaExecutable?.isNotBlank() != false) { "The Java executable must not be blank." }
        require(environmentVariables.keys.none(String::isBlank)) { "Environment variable names must not be blank." }
        require(modLoader != ModLoader.VANILLA || loaderVersion == null) {
            "Vanilla instances cannot have a loader version."
        }
    }
}

fun GameInstance.canPrepareLaunch(): Boolean = installationState is InstallationState.Installed
