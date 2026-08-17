package net.blockhost.trestle.instance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.LauncherException
import okio.FileSystem
import okio.Path

class FileInstanceRepository(
    private val fileSystem: FileSystem,
    private val registryPath: Path,
    private val instancesDirectory: Path,
    private val idFactory: InstanceIdFactory,
) : InstanceRepository {
    private val mutex = Mutex()
    private val mutableInstances = MutableStateFlow<List<GameInstance>>(emptyList())

    override val instances: StateFlow<List<GameInstance>> = mutableInstances.asStateFlow()

    override suspend fun initialize() = mutex.withLock {
        try {
            fileSystem.createDirectories(instancesDirectory)
            fileSystem.createDirectories(requireNotNull(registryPath.parent))
            if (!fileSystem.exists(registryPath)) {
                writeRegistry(InstanceRegistry())
                mutableInstances.value = emptyList()
                return@withLock
            }

            val registry = registryJson.decodeFromString<InstanceRegistry>(
                fileSystem.read(registryPath) { readUtf8() },
            )
            if (registry.schemaVersion != InstanceRegistry.CURRENT_SCHEMA_VERSION) {
                throw LauncherException.FileSystem(
                    "Instance registry schema ${registry.schemaVersion} is not supported.",
                )
            }
            mutableInstances.value = registry.instances.sortedBy { it.displayName.lowercase() }
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The instance registry could not be read.", error)
        }
    }

    override suspend fun get(id: InstanceId): GameInstance? =
        instances.value.firstOrNull { it.id == id }

    override suspend fun create(request: CreateInstanceRequest): GameInstance = mutex.withLock {
        val id = generateUniqueId()
        val instance = GameInstance(
            id = id,
            displayName = request.displayName.trim(),
            minecraftVersionId = request.minecraftVersionId,
            modLoader = request.modLoader,
            loaderVersion = request.loaderVersion,
            instanceDirectory = (instancesDirectory / id.value).toString(),
            requiredJavaMajor = request.requiredJavaMajor,
            jvmArguments = request.jvmArguments,
            memory = request.memory,
            gameArguments = request.gameArguments,
            iconReference = request.iconReference,
        )
        fileSystem.createDirectories((instancesDirectory / id.value) / "game")
        persist(mutableInstances.value + instance)
        instance
    }

    override suspend fun update(instance: GameInstance): GameInstance = mutex.withLock {
        val current = mutableInstances.value
        check(current.any { it.id == instance.id }) { "Instance ${instance.id.value} does not exist." }
        persist(current.map { if (it.id == instance.id) instance else it })
        instance
    }

    override suspend fun delete(id: InstanceId): Boolean = mutex.withLock {
        val current = mutableInstances.value
        if (current.none { it.id == id }) return@withLock false
        persist(current.filterNot { it.id == id })
        true
    }

    private fun generateUniqueId(): InstanceId {
        repeat(20) {
            val id = idFactory.create()
            require(id.value.matches(Regex("[a-zA-Z0-9_-]{6,80}"))) { "Instance ID contains unsafe characters." }
            if (mutableInstances.value.none { it.id == id }) return id
        }
        error("Could not generate a unique instance ID.")
    }

    private fun persist(instances: List<GameInstance>) {
        val sorted = instances.sortedBy { it.displayName.lowercase() }
        try {
            writeRegistry(InstanceRegistry(instances = sorted))
            mutableInstances.value = sorted
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The instance registry could not be saved.", error)
        }
    }

    private fun writeRegistry(registry: InstanceRegistry) {
        val temporaryPath = registryPath.parent!! / ".${registryPath.name}.tmp"
        fileSystem.write(temporaryPath) {
            writeUtf8(registryJson.encodeToString(InstanceRegistry.serializer(), registry))
            flush()
        }
        fileSystem.atomicMove(temporaryPath, registryPath)
    }
}
