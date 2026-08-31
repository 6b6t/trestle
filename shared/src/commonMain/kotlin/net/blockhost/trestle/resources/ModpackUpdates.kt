package net.blockhost.trestle.resources

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

@Serializable
internal data class ModpackManifest(val schemaVersion: Int = 1, val files: Map<String, String>)

data class PackFileChange(val path: String, val previousHash: String?, val currentHash: String?, val incomingHash: String?) {
    val conflict: Boolean get() = currentHash != previousHash && currentHash != incomingHash
    val action: String get() = when {
        incomingHash == null -> "Remove"
        previousHash == null -> "Add"
        else -> "Replace"
    }
}

data class ModpackUpdatePreview(
    val original: GameInstance,
    val candidate: GameInstance,
    val changes: List<PackFileChange>,
    internal val expected: Map<String, String?>,
) {
    val conflicts: List<PackFileChange> get() = changes.filter { it.conflict }
}

/** Three-way updates preserve unowned files and require explicit choices for modified pack files. */
class ModpackUpdates(private val fileSystem: FileSystem) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    internal fun record(instance: GameInstance) {
        val root = instance.instanceDirectory.toPath()
        val manifest = ModpackManifest(files = inventory(root / "game"))
        write(root / ".trestle" / "modpack.json", json.encodeToString(ModpackManifest.serializer(), manifest))
        write(root / ".trestle" / "pack-instance.json", json.encodeToString(GameInstance.serializer(), instance))
    }

    internal fun restoreOrigin(instance: GameInstance, manifestSource: Path): GameInstance {
        if (!fileSystem.exists(manifestSource)) return instance.copy(modpackOrigin = null)
        val manifest = json.decodeFromString<ModpackManifest>(fileSystem.read(manifestSource) { readUtf8() })
        require(manifest.schemaVersion == 1) { "Unsupported exported pack manifest." }
        val root = instance.instanceDirectory.toPath()
        manifest.files.forEach { (name, hash) ->
            checkedContentPath(fileSystem, root / "game", name)
            require(hash.matches(Regex("[a-f0-9]{64}"))) { "Invalid exported pack hash." }
        }
        write(root / ".trestle" / "modpack.json", json.encodeToString(ModpackManifest.serializer(), manifest))
        write(root / ".trestle" / "pack-instance.json", json.encodeToString(GameInstance.serializer(), instance))
        return instance
    }

    fun preview(original: GameInstance, candidate: GameInstance): ModpackUpdatePreview {
        require(original.id != candidate.id) { "Prepare pack updates in a separate instance." }
        val root = original.instanceDirectory.toPath()
        val old = readManifest(root)
        val incoming = readManifest(candidate.instanceDirectory.toPath())
        val current = (old.files.keys + incoming.files.keys).associateWith { name -> hash(root / "game", name) }
        val changes = planPackChanges(old.files, current, incoming.files)
        val expected = changes.associate { "game/${it.path}" to it.currentHash }.toMutableMap()
        metadataPaths.forEach { expected[it] = hash(root, it) }
        return ModpackUpdatePreview(original, candidate, changes, expected)
    }

    fun apply(preview: ModpackUpdatePreview, replaceConflicts: Set<String>): GameInstance {
        require(replaceConflicts.all { name -> preview.conflicts.any { it.path == name } }) { "Unknown conflict choice." }
        val root = preview.original.instanceDirectory.toPath()
        val candidate = preview.candidate.instanceDirectory.toPath()
        val changes = preview.changes.filter { !it.conflict || it.path in replaceConflicts }
        val replacements = changes.associate { change ->
            "game/${change.path}" to change.incomingHash?.let { expected ->
                val source = checkedContentPath(fileSystem, candidate / "game", change.path)
                require(fileSystem.sha256(source) == expected) { "The staged pack changed. Prepare the update again." }
                source
            }
        }.toMutableMap()
        val updated = preview.original.withPackRuntime(preview.candidate)
        write(candidate / ".trestle" / "pack-instance.json", json.encodeToString(GameInstance.serializer(), updated))
        metadataPaths.forEach { replacements[it] = checkedContentPath(fileSystem, candidate, it) }
        ContentTransaction(fileSystem, root, root / ".trestle" / "pack-backup").apply(replacements, preview.expected)
        return updated
    }

    fun canRollback(instance: GameInstance): Boolean = transaction(instance).canRollback()

    fun rollback(instance: GameInstance): GameInstance {
        transaction(instance).rollback()
        return readRuntime(instance)
    }

    fun recover(instance: GameInstance): GameInstance {
        val root = instance.instanceDirectory.toPath()
        ContentTransaction(fileSystem, root, root / ".trestle" / "resource-backup").recover()
        transaction(instance).recover()
        return readRuntime(instance)
    }

    fun discardPreview(instance: GameInstance) {
        val directory = instance.instanceDirectory.toPath() / ".trestle" / "pack-preview"
        if (fileSystem.exists(directory)) fileSystem.deleteRecursively(directory)
    }

    private fun readRuntime(instance: GameInstance): GameInstance {
        val path = instance.instanceDirectory.toPath() / ".trestle" / "pack-instance.json"
        if (!fileSystem.exists(path)) return instance
        val stored = json.decodeFromString<GameInstance>(fileSystem.read(path) { readUtf8() })
        return if (stored.modpackOrigin != instance.modpackOrigin) instance.withPackRuntime(stored) else instance
    }

    private fun transaction(instance: GameInstance): ContentTransaction {
        val root = instance.instanceDirectory.toPath()
        return ContentTransaction(fileSystem, root, root / ".trestle" / "pack-backup")
    }

    private fun readManifest(root: Path): ModpackManifest {
        val path = root / ".trestle" / "modpack.json"
        val manifest = json.decodeFromString<ModpackManifest>(fileSystem.read(path) { readUtf8() })
        require(manifest.schemaVersion == 1) { "Unsupported modpack manifest." }
        manifest.files.forEach { (name, hash) ->
            checkedContentPath(fileSystem, root / "game", name)
            require(hash.matches(Regex("[0-9a-f]{64}"))) { "Invalid pack file hash." }
        }
        return manifest
    }

    private fun inventory(root: Path): Map<String, String> {
        if (!fileSystem.exists(root)) return emptyMap()
        return fileSystem.listRecursively(root).filter { fileSystem.metadata(it).isRegularFile }
            .map { path -> path.relativeTo(root).toString().replace('\\', '/') }
            .filterNot(::protectedPackPath)
            .associateWith { fileSystem.sha256(checkedContentPath(fileSystem, root, it)) }
    }

    private fun hash(root: Path, relative: String): String? {
        val path = checkedContentPath(fileSystem, root, relative)
        return if (fileSystem.exists(path)) fileSystem.sha256(path) else null
    }

    private fun write(path: Path, body: String) {
        fileSystem.createDirectories(requireNotNull(path.parent))
        val temporary = path.parent!! / ".${path.name}.tmp"
        fileSystem.write(temporary) { writeUtf8(body) }
        fileSystem.atomicMove(temporary, path)
    }

    private companion object {
        val metadataPaths = listOf(".trestle/modpack.json", ".trestle/installed-version.json", ".trestle/pack-instance.json")
    }
}

internal fun planPackChanges(original: Map<String, String>, current: Map<String, String?>, incoming: Map<String, String>): List<PackFileChange> =
    (original.keys + incoming.keys).filterNot(::protectedPackPath).sorted().mapNotNull { name ->
        val before = original[name]
        val after = incoming[name]
        if (before == after || current[name] == after) null else PackFileChange(name, before, current[name], after)
    }

private fun protectedPackPath(path: String): Boolean = path.substringBefore('/') in setOf(
    "saves", "screenshots", "logs", "crash-reports", "backups", "options.txt", "optionsof.txt", "servers.dat", "servers.dat_old",
)

private fun GameInstance.withPackRuntime(other: GameInstance): GameInstance = copy(
    minecraftVersionId = other.minecraftVersionId,
    modLoader = other.modLoader,
    loaderVersion = other.loaderVersion,
    requiredJavaMajor = other.requiredJavaMajor,
    installationState = other.installationState,
    modpackOrigin = other.modpackOrigin,
)
