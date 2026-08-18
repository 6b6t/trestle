package net.blockhost.trestle.instance

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.MemorySettings
import net.blockhost.trestle.domain.ModLoader

@Serializable
data class InstanceRegistry(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val instances: List<GameInstance> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class CreateInstanceRequest(
    val displayName: String,
    val minecraftVersionId: String,
    val modLoader: ModLoader = ModLoader.VANILLA,
    val loaderVersion: String? = null,
    val requiredJavaMajor: Int = 8,
    val jvmArguments: List<String> = emptyList(),
    val memory: MemorySettings = MemorySettings(),
    val gameArguments: List<String> = emptyList(),
    val iconReference: String? = null,
    val group: String? = null,
    val clientSettings: MinecraftClientSettings? = MinecraftClientSettings(),
)

fun interface InstanceIdFactory {
    fun create(): InstanceId
}

interface InstanceRepository {
    val instances: StateFlow<List<GameInstance>>

    suspend fun initialize()
    suspend fun get(id: InstanceId): GameInstance?
    suspend fun create(request: CreateInstanceRequest): GameInstance
    suspend fun update(instance: GameInstance): GameInstance
    suspend fun updateWithIcon(instance: GameInstance, fileName: String, bytes: ByteArray): GameInstance
    suspend fun readClientSettings(id: InstanceId): MinecraftClientSettings?
    suspend fun updateClientSettings(id: InstanceId, settings: MinecraftClientSettings)
    suspend fun delete(id: InstanceId): Boolean
    suspend fun deleteWithFiles(id: InstanceId): Boolean
    suspend fun clone(id: InstanceId, displayName: String): GameInstance
    suspend fun restore(instance: GameInstance): GameInstance
}

internal val registryJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    ignoreUnknownKeys = true
    classDiscriminator = "state"
}

fun GameInstance.withInstallationState(state: InstallationState): GameInstance = copy(installationState = state)
