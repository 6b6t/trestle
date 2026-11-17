package net.blockhost.trestle.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_environment_variable_names_must_not_be_blank
import net.blockhost.trestle.resources.msg_instance_directory_must_not_be_blank
import net.blockhost.trestle.resources.msg_instance_name_must_not_be_blank
import net.blockhost.trestle.resources.msg_java_major_version_must_be_positive
import net.blockhost.trestle.resources.msg_launch_count_must_not_be_negative
import net.blockhost.trestle.resources.msg_maximum_memory_must_not_be_less_than
import net.blockhost.trestle.resources.msg_minecraft_version_must_not_be_blank
import net.blockhost.trestle.resources.msg_minimum_memory_must_be_positive
import net.blockhost.trestle.resources.msg_play_time_must_not_be_negative
import net.blockhost.trestle.resources.msg_post_exit_command_arguments_must_not_be
import net.blockhost.trestle.resources.msg_pre_launch_command_arguments_must_not_be
import net.blockhost.trestle.resources.msg_the_account_profile_id_must_not_be
import net.blockhost.trestle.resources.msg_the_java_executable_must_not_be_blank
import net.blockhost.trestle.resources.msg_vanilla_instances_cannot_have_a_loader_version
import net.blockhost.trestle.resources.msg_wrapper_command_arguments_must_not_be_blank
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text
import kotlin.jvm.JvmInline

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
data class MemorySettings(val minimumMiB: Int = 512, val maximumMiB: Int = 2048) {
    init {
        requireUser(minimumMiB > 0) { text(Res.string.msg_minimum_memory_must_be_positive) }
        requireUser(maximumMiB >= minimumMiB) { text(Res.string.msg_maximum_memory_must_not_be_less_than) }
    }
}

@Serializable
sealed interface InstallationState {
    @Serializable
    @SerialName("not_installed")
    data object NotInstalled : InstallationState

    @Serializable
    @SerialName("installing")
    data class Installing(val completedBytes: Long, val totalBytes: Long?, val completedFiles: Int, val totalFiles: Int) :
        InstallationState

    @Serializable
    @SerialName("interrupted")
    data class Interrupted(val completedBytes: Long, val totalBytes: Long?, val completedFiles: Int, val totalFiles: Int) :
        InstallationState

    @Serializable
    @SerialName("installed")
    data class Installed(val installedAtEpochMillis: Long) : InstallationState

    @Serializable
    @SerialName("failed")
    data class Failed(val message: String) : InstallationState
}

@Serializable
data class ModpackOrigin(
    val provider: String,
    val projectId: String,
    val versionId: String,
    val versionName: String,
    val name: String,
    val websiteUrl: String? = null,
)

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
    val notes: String = "",
    val preLaunchCommand: List<String> = emptyList(),
    val wrapperCommand: List<String> = emptyList(),
    val postExitCommand: List<String> = emptyList(),
    val accountProfileId: String? = null,
    val installationState: InstallationState = InstallationState.NotInstalled,
    val lastLaunchAtEpochMillis: Long? = null,
    val iconReference: String? = null,
    val pinned: Boolean = false,
    val group: String? = null,
    val launchCount: Int = 0,
    val playTimeMillis: Long = 0,
    val modpackOrigin: ModpackOrigin? = null,
) {
    init {
        requireUser(displayName.isNotBlank()) { text(Res.string.msg_instance_name_must_not_be_blank) }
        requireUser(minecraftVersionId.isNotBlank()) { text(Res.string.msg_minecraft_version_must_not_be_blank) }
        requireUser(instanceDirectory.isNotBlank()) { text(Res.string.msg_instance_directory_must_not_be_blank) }
        requireUser(requiredJavaMajor > 0) { text(Res.string.msg_java_major_version_must_be_positive) }
        requireUser(launchCount >= 0) { text(Res.string.msg_launch_count_must_not_be_negative) }
        requireUser(playTimeMillis >= 0) { text(Res.string.msg_play_time_must_not_be_negative) }
        requireUser(javaExecutable?.isNotBlank() != false) { text(Res.string.msg_the_java_executable_must_not_be_blank) }
        requireUser(environmentVariables.keys.none(String::isBlank)) { text(Res.string.msg_environment_variable_names_must_not_be_blank) }
        requireUser(preLaunchCommand.none(String::isBlank)) { text(Res.string.msg_pre_launch_command_arguments_must_not_be) }
        requireUser(wrapperCommand.none(String::isBlank)) { text(Res.string.msg_wrapper_command_arguments_must_not_be_blank) }
        requireUser(postExitCommand.none(String::isBlank)) { text(Res.string.msg_post_exit_command_arguments_must_not_be) }
        requireUser(accountProfileId?.isNotBlank() != false) { text(Res.string.msg_the_account_profile_id_must_not_be) }
        requireUser(modLoader != ModLoader.VANILLA || loaderVersion == null) {
            text(Res.string.msg_vanilla_instances_cannot_have_a_loader_version)
        }
    }
}

fun GameInstance.canPrepareLaunch(): Boolean = installationState is InstallationState.Installed
