package net.blockhost.trestle.instance

import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.platform.useOkio
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

internal class IosInstanceExporter : InstanceExporter {
    override fun export(instance: GameInstance, destination: Path): Path {
        throw LauncherException.RuntimeUnavailable(
            "Portable ZIP export is not available in the iOS host yet.",
        )
    }
}

internal class IosGameDataManager(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : GameDataManager {
    override suspend fun inventory(instance: GameInstance): GameDataInventory {
        val game = gameDirectory(instance)
        return GameDataInventory(
            worlds = directories(game / "saves").map { world ->
                ManagedWorld(
                    key = world.name,
                    name = world.name,
                    sizeBytes = treeSize(world),
                    lastModifiedEpochMillis = fileSystem.metadataOrNull(world / "level.dat")?.lastModifiedAtMillis,
                    dataPacks = emptyList(),
                    iconPath = (world / "icon.png").takeIf(fileSystem::exists)?.toString(),
                )
            },
            screenshots = files(game / "screenshots").map { screenshot ->
                val metadata = fileSystem.metadata(screenshot)
                ManagedScreenshot(
                    key = screenshot.name,
                    fileName = screenshot.name,
                    path = screenshot.toString(),
                    sizeBytes = metadata.size ?: 0L,
                    createdAtEpochMillis = metadata.lastModifiedAtMillis,
                )
            }.sortedByDescending(ManagedScreenshot::createdAtEpochMillis),
            backups = files(game / "backups").map { backup ->
                val metadata = fileSystem.metadata(backup)
                WorldBackup(
                    key = backup.name,
                    fileName = backup.name,
                    path = backup.toString(),
                    sizeBytes = metadata.size ?: 0L,
                    createdAtEpochMillis = metadata.lastModifiedAtMillis,
                )
            }.sortedByDescending(WorldBackup::createdAtEpochMillis),
            logs = files(game / "logs").map { log ->
                val metadata = fileSystem.metadata(log)
                ManagedLogFile(
                    key = log.name,
                    fileName = log.name,
                    path = log.toString(),
                    sizeBytes = metadata.size ?: 0L,
                    lastModifiedEpochMillis = metadata.lastModifiedAtMillis,
                )
            }.sortedByDescending(ManagedLogFile::lastModifiedEpochMillis),
        )
    }

    override suspend fun deleteWorld(instance: GameInstance, worldKey: String) =
        deleteRecursively(safeChild(gameDirectory(instance) / "saves", worldKey))

    override suspend fun deleteScreenshot(instance: GameInstance, screenshotKey: String) {
        fileSystem.delete(safeChild(gameDirectory(instance) / "screenshots", screenshotKey), mustExist = false)
    }

    override suspend fun renameScreenshot(
        instance: GameInstance,
        screenshotKey: String,
        newName: String,
    ): ManagedScreenshot {
        val screenshots = gameDirectory(instance) / "screenshots"
        val source = safeChild(screenshots, screenshotKey)
        val extension = source.name.substringAfterLast('.', "")
        val base = safeName(newName.substringBeforeLast('.', newName))
        val destination = safeChild(screenshots, if (extension.isBlank()) base else "$base.$extension")
        require(!fileSystem.exists(destination)) { "A screenshot with that name already exists." }
        fileSystem.atomicMove(source, destination)
        val metadata = fileSystem.metadata(destination)
        return ManagedScreenshot(
            key = destination.name,
            fileName = destination.name,
            path = destination.toString(),
            sizeBytes = metadata.size ?: 0L,
            createdAtEpochMillis = metadata.lastModifiedAtMillis,
        )
    }

    override suspend fun readLog(instance: GameInstance, logKey: String): String {
        val path = safeChild(gameDirectory(instance) / "logs", logKey)
        val source = if (path.name.endsWith(".gz", ignoreCase = true)) {
            GzipSource(fileSystem.source(path)).buffer()
        } else {
            fileSystem.source(path).buffer()
        }
        return source.useOkio { it.readUtf8(MAX_LOG_BYTES) }
    }

    override suspend fun deleteLog(instance: GameInstance, logKey: String) {
        fileSystem.delete(safeChild(gameDirectory(instance) / "logs", logKey), mustExist = false)
    }

    override suspend fun backupWorld(instance: GameInstance, worldKey: String): WorldBackup = unsupported()
    override suspend fun restoreWorldBackup(instance: GameInstance, backupKey: String): ManagedWorld = unsupported()
    override suspend fun importWorld(instance: GameInstance, fileName: String, bytes: ByteArray): ManagedWorld = unsupported()
    override suspend fun copyWorld(instance: GameInstance, worldKey: String): ManagedWorld = unsupported()
    override suspend fun renameWorld(instance: GameInstance, worldKey: String, newName: String): ManagedWorld = unsupported()
    override suspend fun resetWorldIcon(instance: GameInstance, worldKey: String) = unsupported<Unit>()
    override suspend fun setDataPackEnabled(
        instance: GameInstance,
        worldKey: String,
        dataPackKey: String,
        enabled: Boolean,
    ) = unsupported<Unit>()
    override suspend fun upsertServer(instance: GameInstance, server: SavedServer) = unsupported<Unit>()
    override suspend fun removeServer(instance: GameInstance, serverKey: String) = unsupported<Unit>()
    override suspend fun moveServer(instance: GameInstance, serverKey: String, offset: Int) = unsupported<Unit>()

    private fun gameDirectory(instance: GameInstance) = instance.instanceDirectory.toPath() / "game"

    private fun files(directory: Path): List<Path> = fileSystem.listOrNull(directory).orEmpty()
        .filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }

    private fun directories(directory: Path): List<Path> = fileSystem.listOrNull(directory).orEmpty()
        .filter { fileSystem.metadataOrNull(it)?.isDirectory == true }

    private fun treeSize(directory: Path): Long = fileSystem.listRecursively(directory)
        .sumOf { fileSystem.metadataOrNull(it)?.size ?: 0L }

    private fun safeChild(parent: Path, key: String): Path {
        val name = safeName(key)
        require(name == key) { "The selected file name is not safe." }
        return parent / name
    }

    private fun safeName(value: String): String {
        val trimmed = value.trim()
        require(trimmed.isNotBlank() && trimmed !in setOf(".", "..") && '/' !in trimmed && '\\' !in trimmed) {
            "The file name is not valid."
        }
        return trimmed
    }

    private fun deleteRecursively(path: Path) {
        if (!fileSystem.exists(path)) return
        fileSystem.listRecursively(path).toList().asReversed().forEach { fileSystem.delete(it, mustExist = false) }
        fileSystem.delete(path, mustExist = false)
    }

    private fun <T> unsupported(): T = throw LauncherException.RuntimeUnavailable(
        "This game-data operation is not available in the iOS host yet.",
    )

    private companion object {
        const val MAX_LOG_BYTES = 4L * 1024L * 1024L
    }
}
