package net.blockhost.trestle.instance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class JvmGameDataManager(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : GameDataManager {
    override suspend fun inventory(instance: GameInstance): GameDataInventory = withContext(Dispatchers.IO) {
        val game = gameDirectory(instance)
        GameDataInventory(
            worlds = directories(game.resolve("saves")).map { world ->
                ManagedWorld(
                    key = world.fileName.toString(),
                    name = world.fileName.toString(),
                    sizeBytes = treeSize(world),
                    lastModifiedEpochMillis = modifiedAt(world.resolve("level.dat")) ?: modifiedAt(world),
                    dataPacks = files(world.resolve("datapacks")).map { pack ->
                        val fileName = pack.fileName.toString()
                        ManagedDataPack(
                            key = fileName,
                            fileName = fileName.removeSuffix(DISABLED_SUFFIX),
                            enabled = !fileName.endsWith(DISABLED_SUFFIX),
                            sizeBytes = treeSize(pack),
                        )
                    },
                )
            }.sortedByDescending { it.lastModifiedEpochMillis ?: 0L },
            screenshots = files(game.resolve("screenshots")).map { screenshot ->
                ManagedScreenshot(
                    key = screenshot.fileName.toString(),
                    fileName = screenshot.fileName.toString(),
                    path = screenshot.toString(),
                    sizeBytes = runCatching { Files.size(screenshot) }.getOrDefault(0),
                    createdAtEpochMillis = modifiedAt(screenshot),
                )
            }.sortedByDescending { it.createdAtEpochMillis ?: 0L },
            backups = files(game.resolve("backups")).filter { it.fileName.toString().endsWith(".zip") }.map { backup ->
                WorldBackup(
                    key = backup.fileName.toString(),
                    fileName = backup.fileName.toString(),
                    path = backup.toString(),
                    sizeBytes = runCatching { Files.size(backup) }.getOrDefault(0),
                    createdAtEpochMillis = modifiedAt(backup),
                )
            }.sortedByDescending { it.createdAtEpochMillis ?: 0L },
            servers = readServers(game.resolve("servers.dat")),
        )
    }

    override suspend fun backupWorld(instance: GameInstance, worldKey: String): WorldBackup =
        withContext(Dispatchers.IO) {
            val game = gameDirectory(instance)
            val world = safeChild(game.resolve("saves"), worldKey)
            if (!Files.isDirectory(world)) throw LauncherException.FileSystem("The selected world does not exist.")
            val backups = game.resolve("backups")
            Files.createDirectories(backups)
            val destination = backups.resolve("${safeFileName(worldKey)}-${nowMillis()}.zip")
            val temporary = destination.resolveSibling(".${destination.fileName}.tmp")
            ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                Files.walk(world).use { paths ->
                    paths.filter(Files::isRegularFile).forEach { file ->
                        val relative = world.relativize(file).toString().replace('\\', '/')
                        zip.putNextEntry(ZipEntry("$worldKey/$relative"))
                        Files.copy(file, zip)
                        zip.closeEntry()
                    }
                }
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            WorldBackup(
                key = destination.fileName.toString(),
                fileName = destination.fileName.toString(),
                path = destination.toString(),
                sizeBytes = Files.size(destination),
                createdAtEpochMillis = modifiedAt(destination),
            )
        }

    override suspend fun restoreWorldBackup(instance: GameInstance, backupKey: String): ManagedWorld =
        withContext(Dispatchers.IO) {
            val game = gameDirectory(instance)
            val archive = safeChild(game.resolve("backups"), backupKey)
            if (!Files.isRegularFile(archive)) throw LauncherException.FileSystem("The selected backup does not exist.")
            val saves = game.resolve("saves")
            Files.createDirectories(saves)
            var restoredKey: String? = null
            ZipInputStream(Files.newInputStream(archive)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val entryPath = Paths.get(entry.name).normalize()
                    if (entryPath.isAbsolute || entryPath.startsWith("..") || entryPath.nameCount < 2) {
                        throw LauncherException.InvalidMetadata("The world backup contains an unsafe path.")
                    }
                    val worldKey = entryPath.getName(0).toString()
                    restoredKey = restoredKey ?: availableWorldKey(saves, worldKey)
                    val relative = entryPath.subpath(1, entryPath.nameCount)
                    val target = saves.resolve(requireNotNull(restoredKey)).resolve(relative).normalize()
                    if (!target.startsWith(saves)) throw LauncherException.InvalidMetadata("The backup escapes the saves directory.")
                    if (entry.isDirectory) Files.createDirectories(target)
                    else {
                        Files.createDirectories(target.parent)
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                    zip.closeEntry()
                }
            }
            val key = restoredKey ?: throw LauncherException.InvalidMetadata("The selected backup is empty.")
            val restored = saves.resolve(key)
            ManagedWorld(key, key, treeSize(restored), modifiedAt(restored.resolve("level.dat")), emptyList())
        }

    override suspend fun deleteWorld(instance: GameInstance, worldKey: String) = withContext(Dispatchers.IO) {
        deleteTree(safeChild(gameDirectory(instance).resolve("saves"), worldKey))
    }

    override suspend fun deleteScreenshot(instance: GameInstance, screenshotKey: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(safeChild(gameDirectory(instance).resolve("screenshots"), screenshotKey))
        Unit
    }

    override suspend fun setDataPackEnabled(
        instance: GameInstance,
        worldKey: String,
        dataPackKey: String,
        enabled: Boolean,
    ) = withContext(Dispatchers.IO) {
        val packs = safeChild(gameDirectory(instance).resolve("saves"), worldKey).resolve("datapacks")
        val source = safeChild(packs, dataPackKey)
        val sourceName = source.fileName.toString()
        val destinationName = if (enabled) sourceName.removeSuffix(DISABLED_SUFFIX) else "$sourceName$DISABLED_SUFFIX"
        if (sourceName != destinationName) {
            val destination = safeChild(packs, destinationName)
            if (Files.exists(destination)) throw LauncherException.FileSystem("A data pack with that name already exists.")
            Files.move(source, destination)
        }
    }

    override suspend fun upsertServer(instance: GameInstance, server: SavedServer) = withContext(Dispatchers.IO) {
        require(server.name.isNotBlank()) { "Server name must not be blank." }
        require(server.address.isNotBlank()) { "Server address must not be blank." }
        val path = gameDirectory(instance).resolve("servers.dat")
        val servers = readServers(path).toMutableList()
        val index = servers.indexOfFirst { it.key == server.key }
        val normalized = server.copy(key = server.key.ifBlank { serverKey(server.name, server.address) })
        if (index >= 0) servers[index] = normalized else servers += normalized
        writeServers(path, servers)
    }

    override suspend fun removeServer(instance: GameInstance, serverKey: String) = withContext(Dispatchers.IO) {
        val path = gameDirectory(instance).resolve("servers.dat")
        writeServers(path, readServers(path).filterNot { it.key == serverKey })
    }

    private fun readServers(path: Path): List<SavedServer> {
        if (!Files.isRegularFile(path)) return emptyList()
        return runCatching {
            DataInputStream(GZIPInputStream(Files.newInputStream(path))).use { input ->
                if (input.readUnsignedByte() != TAG_COMPOUND) return emptyList()
                input.readUTF()
                readRootCompound(input)
            }
        }.getOrElse { throw LauncherException.FileSystem("servers.dat could not be read.", it) }
    }

    private fun readRootCompound(input: DataInputStream): List<SavedServer> {
        while (true) {
            val type = input.readUnsignedByte()
            if (type == TAG_END) return emptyList()
            val name = input.readUTF()
            if (type == TAG_LIST && name == "servers") {
                val elementType = input.readUnsignedByte()
                val count = input.readInt().coerceAtLeast(0)
                if (elementType != TAG_COMPOUND) {
                    repeat(count) { skipPayload(input, elementType) }
                    return emptyList()
                }
                return List(count) { readServerCompound(input) }
            }
            skipPayload(input, type)
        }
    }

    private fun readServerCompound(input: DataInputStream): SavedServer {
        var name = ""
        var address = ""
        var textures: Boolean? = null
        while (true) {
            val type = input.readUnsignedByte()
            if (type == TAG_END) break
            when (val key = input.readUTF()) {
                "name" -> if (type == TAG_STRING) name = input.readUTF() else skipPayload(input, type)
                "ip" -> if (type == TAG_STRING) address = input.readUTF() else skipPayload(input, type)
                "acceptTextures" -> if (type == TAG_BYTE) textures = input.readByte().toInt() != 0 else skipPayload(input, type)
                else -> skipPayload(input, type)
            }
        }
        return SavedServer(serverKey(name, address), name, address, textures)
    }

    private fun writeServers(path: Path, servers: List<SavedServer>) {
        Files.createDirectories(path.parent)
        val temporary = path.resolveSibling(".${path.fileName}.tmp")
        DataOutputStream(GZIPOutputStream(Files.newOutputStream(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))).use { output ->
            output.writeByte(TAG_COMPOUND)
            output.writeUTF("")
            output.writeByte(TAG_LIST)
            output.writeUTF("servers")
            output.writeByte(TAG_COMPOUND)
            output.writeInt(servers.size)
            servers.forEach { server ->
                writeString(output, "name", server.name)
                writeString(output, "ip", server.address)
                server.acceptTextures?.let {
                    output.writeByte(TAG_BYTE)
                    output.writeUTF("acceptTextures")
                    output.writeByte(if (it) 1 else 0)
                }
                output.writeByte(TAG_END)
            }
            output.writeByte(TAG_END)
        }
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun writeString(output: DataOutputStream, name: String, value: String) {
        output.writeByte(TAG_STRING)
        output.writeUTF(name)
        output.writeUTF(value)
    }

    private fun skipPayload(input: DataInputStream, type: Int) {
        when (type) {
            TAG_BYTE -> input.readByte()
            TAG_SHORT -> input.readShort()
            TAG_INT -> input.readInt()
            TAG_LONG -> input.readLong()
            TAG_FLOAT -> input.readFloat()
            TAG_DOUBLE -> input.readDouble()
            TAG_BYTE_ARRAY -> skipBytes(input, input.readInt().coerceAtLeast(0).toLong())
            TAG_STRING -> input.readUTF()
            TAG_LIST -> {
                val itemType = input.readUnsignedByte()
                repeat(input.readInt().coerceAtLeast(0)) { skipPayload(input, itemType) }
            }
            TAG_COMPOUND -> while (true) {
                val itemType = input.readUnsignedByte()
                if (itemType == TAG_END) break
                input.readUTF()
                skipPayload(input, itemType)
            }
            TAG_INT_ARRAY -> skipBytes(input, input.readInt().coerceAtLeast(0) * 4L)
            TAG_LONG_ARRAY -> skipBytes(input, input.readInt().coerceAtLeast(0) * 8L)
            else -> throw LauncherException.InvalidMetadata("servers.dat contains an unknown NBT tag.")
        }
    }

    private fun skipBytes(input: DataInputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) remaining -= skipped
            else if (input.read() >= 0) remaining--
            else throw LauncherException.InvalidMetadata("servers.dat ended unexpectedly.")
        }
    }

    private fun gameDirectory(instance: GameInstance): Path = Paths.get(instance.instanceDirectory).resolve("game")
    private fun directories(path: Path): List<Path> = if (Files.isDirectory(path)) Files.newDirectoryStream(path).use { stream ->
        stream.filter(Files::isDirectory)
    } else emptyList()
    private fun files(path: Path): List<Path> = if (Files.isDirectory(path)) Files.newDirectoryStream(path).use { stream ->
        stream.filter { !Files.isDirectory(it) }
    } else emptyList()
    private fun modifiedAt(path: Path): Long? = runCatching { Files.getLastModifiedTime(path).toMillis() }.getOrNull()
    private fun treeSize(path: Path): Long = if (!Files.exists(path)) 0 else Files.walk(path).use { stream ->
        stream.filter(Files::isRegularFile).mapToLong { runCatching { Files.size(it) }.getOrDefault(0) }.sum()
    }
    private fun safeChild(parent: Path, child: String): Path {
        require(child.isNotBlank() && '/' !in child && '\\' !in child && child !in setOf(".", "..")) {
            "The selected item has an invalid path."
        }
        return parent.resolve(child).normalize().also { require(it.parent == parent.normalize()) }
    }
    private fun deleteTree(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).sorted(Comparator.reverseOrder()).use { paths -> paths.forEach(Files::deleteIfExists) }
    }
    private fun safeFileName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "world" }
    private fun availableWorldKey(saves: Path, preferred: String): String {
        if (!Files.exists(saves.resolve(preferred))) return preferred
        var number = 2
        while (Files.exists(saves.resolve("$preferred ($number)"))) number += 1
        return "$preferred ($number)"
    }
    private fun serverKey(name: String, address: String): String = "$name\u0000$address"

    private companion object {
        const val DISABLED_SUFFIX = ".disabled"
        const val TAG_END = 0
        const val TAG_BYTE = 1
        const val TAG_SHORT = 2
        const val TAG_INT = 3
        const val TAG_LONG = 4
        const val TAG_FLOAT = 5
        const val TAG_DOUBLE = 6
        const val TAG_BYTE_ARRAY = 7
        const val TAG_STRING = 8
        const val TAG_LIST = 9
        const val TAG_COMPOUND = 10
        const val TAG_INT_ARRAY = 11
        const val TAG_LONG_ARRAY = 12
    }
}
