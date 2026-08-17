package net.blockhost.trestle.instance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileInstanceRepository(
    private val fileSystem: FileSystem,
    private val registryPath: Path,
    private val instancesDirectory: Path,
    private val idFactory: InstanceIdFactory,
    private val logger: LauncherLogger = NoopLauncherLogger,
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
            val recoveredCount = registry.instances.count { it.installationState is InstallationState.Installing }
            val recoveredInstances = registry.instances.map { instance ->
                val installing = instance.installationState as? InstallationState.Installing
                    ?: return@map instance
                instance.copy(
                    installationState = InstallationState.Interrupted(
                        completedBytes = installing.completedBytes,
                        totalBytes = installing.totalBytes,
                        completedFiles = installing.completedFiles,
                        totalFiles = installing.totalFiles,
                    ),
                )
            }
            if (recoveredInstances != registry.instances) {
                writeRegistry(registry.copy(instances = recoveredInstances))
                logger.warn(
                    "instances",
                    "Recovered interrupted installations",
                    details = mapOf(
                        "count" to recoveredCount,
                    ),
                )
            }
            mutableInstances.value = recoveredInstances.sortedBy { it.displayName.lowercase() }
            logger.info("instances", "Loaded instance registry", mapOf("count" to recoveredInstances.size))
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
        val gameDirectory = (instancesDirectory / id.value) / "game"
        fileSystem.createDirectories(gameDirectory)
        request.clientSettings?.let { writeClientSettings(instance, it, preserveExisting = false) }
        persist(mutableInstances.value + instance)
        logger.info("instances", "Created instance", mapOf("id" to instance.id.value, "version" to instance.minecraftVersionId))
        instance
    }

    override suspend fun readClientSettings(id: InstanceId): MinecraftClientSettings? = mutex.withLock {
        val instance = mutableInstances.value.firstOrNull { it.id == id }
            ?: error("Instance ${id.value} does not exist.")
        val optionsPath = instance.optionsPath()
        val options = if (fileSystem.exists(optionsPath)) {
            fileSystem.read(optionsPath) { readUtf8() }
        } else {
            ""
        }
        MinecraftClientSettings.fromOptionsText(options, instance.minecraftVersionId)
    }

    override suspend fun updateClientSettings(id: InstanceId, settings: MinecraftClientSettings) = mutex.withLock {
        val instance = mutableInstances.value.firstOrNull { it.id == id }
            ?: error("Instance ${id.value} does not exist.")
        writeClientSettings(instance, settings, preserveExisting = true)
        logger.info("instances", "Updated client settings", mapOf("id" to id.value))
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
        logger.info("instances", "Removed instance from registry", mapOf("id" to id.value))
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

    private fun writeClientSettings(
        instance: GameInstance,
        settings: MinecraftClientSettings,
        preserveExisting: Boolean,
    ) {
        val optionsPath = instance.optionsPath()
        val existing = if (preserveExisting && fileSystem.exists(optionsPath)) {
            fileSystem.read(optionsPath) { readUtf8() }
        } else {
            ""
        }
        val options = if (preserveExisting) {
            settings.mergeIntoOptionsText(existing, instance.minecraftVersionId)
        } else {
            settings.toOptionsText(instance.minecraftVersionId)
        }
        if (options.isEmpty()) return

        fileSystem.createDirectories(requireNotNull(optionsPath.parent))
        val temporaryPath = optionsPath.parent!! / ".${optionsPath.name}.tmp"
        fileSystem.write(temporaryPath) {
            writeUtf8(options)
            flush()
        }
        fileSystem.atomicMove(temporaryPath, optionsPath)
    }

    private fun GameInstance.optionsPath(): Path = instanceDirectory.toPath() / "game" / "options.txt"
}
