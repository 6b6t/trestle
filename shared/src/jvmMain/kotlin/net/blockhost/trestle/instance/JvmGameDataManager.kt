package net.blockhost.trestle.instance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.Paths
import java.net.InetSocketAddress
import java.net.Socket
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class JvmGameDataManager(
    private val serverStatusProvider: suspend (SavedServer) -> SavedServer = ::queryServerStatus,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : GameDataManager {
    override suspend fun inventory(instance: GameInstance): GameDataInventory = withContext(Dispatchers.IO) {
        val game = gameDirectory(instance)
        GameDataInventory(
            worlds = directories(game.resolve("saves")).map { world ->
                val metadata = readWorldMetadata(world.resolve("level.dat"))
                ManagedWorld(
                    key = world.fileName.toString(),
                    name = metadata.name ?: world.fileName.toString(),
                    sizeBytes = treeSize(world),
                    lastModifiedEpochMillis = metadata.lastPlayedEpochMillis
                        ?: modifiedAt(world.resolve("level.dat"))
                        ?: modifiedAt(world),
                    dataPacks = files(world.resolve("datapacks")).map { pack ->
                        val fileName = pack.fileName.toString()
                        ManagedDataPack(
                            key = fileName,
                            fileName = fileName.removeSuffix(DISABLED_SUFFIX),
                            enabled = !fileName.endsWith(DISABLED_SUFFIX),
                            sizeBytes = treeSize(pack),
                        )
                    },
                    gameMode = metadata.gameMode,
                    seed = metadata.seed,
                    iconPath = world.resolve("icon.png").takeIf(Files::isRegularFile)?.toString(),
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
            servers = coroutineScope {
                readServers(game.resolve("servers.dat")).map { server ->
                    async(Dispatchers.IO) { serverStatusProvider(server) }
                }.awaitAll()
            },
            logs = LOG_DIRECTORIES.flatMap { directory ->
                files(game.resolve(directory)).filter { file ->
                    file.fileName.toString().substringAfterLast('.', "").lowercase() in LOG_EXTENSIONS
                }.map { file ->
                    ManagedLogFile(
                        key = game.relativize(file).toString().replace('\\', '/'),
                        fileName = file.fileName.toString(),
                        path = file.toString(),
                        sizeBytes = runCatching { Files.size(file) }.getOrDefault(0),
                        lastModifiedEpochMillis = modifiedAt(file),
                    )
                }
            }.sortedByDescending { it.lastModifiedEpochMillis ?: 0L },
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

    override suspend fun importWorld(
        instance: GameInstance,
        fileName: String,
        bytes: ByteArray,
    ): ManagedWorld = withContext(Dispatchers.IO) {
        require(bytes.isNotEmpty() && bytes.size <= MAX_WORLD_ARCHIVE_BYTES) {
            "World archives must be between 1 byte and 512 MiB."
        }
        val saves = gameDirectory(instance).resolve("saves")
        Files.createDirectories(saves)
        val preferred = safeFileName(fileName.substringBeforeLast('.'))
        val worldKey = availableWorldKey(saves, preferred)
        val temporary = safeChild(saves, ".${worldKey}-${nowMillis()}.import")
        val destination = safeChild(saves, worldKey)
        try {
            val entryNames = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                buildList {
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory) add(entry.name.replace('\\', '/'))
                        zip.closeEntry()
                    }
                }
            }
            val rootPrefix = entryNames.mapNotNull { it.substringBefore('/', "").takeIf(String::isNotEmpty) }
                .distinct()
                .singleOrNull()
                ?.takeIf { prefix -> entryNames.any { it == "$prefix/level.dat" } }
            var extractedBytes = 0L
            var extractedFiles = 0
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val rawName = entry.name.replace('\\', '/')
                    val relativeName = rootPrefix?.let { rawName.removePrefix("$it/") } ?: rawName
                    if (relativeName.isBlank()) {
                        zip.closeEntry()
                        continue
                    }
                    val relative = Paths.get(relativeName).normalize()
                    if (relative.isAbsolute || relative.startsWith("..")) {
                        throw LauncherException.InvalidMetadata("The world archive contains an unsafe path.")
                    }
                    val target = temporary.resolve(relative).normalize()
                    if (!target.startsWith(temporary)) {
                        throw LauncherException.InvalidMetadata("The world archive escapes its destination.")
                    }
                    if (entry.isDirectory) {
                        Files.createDirectories(target)
                    } else {
                        extractedFiles += 1
                        if (extractedFiles > MAX_WORLD_ARCHIVE_FILES) {
                            throw LauncherException.InvalidMetadata("The world archive contains too many files.")
                        }
                        Files.createDirectories(target.parent)
                        Files.newOutputStream(target).use { output ->
                            val buffer = ByteArray(8_192)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                extractedBytes += read
                                if (extractedBytes > MAX_EXTRACTED_WORLD_BYTES) {
                                    throw LauncherException.InvalidMetadata("The extracted world is larger than 2 GiB.")
                                }
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            if (!Files.isRegularFile(temporary.resolve("level.dat"))) {
                throw LauncherException.InvalidMetadata("The archive does not contain a Minecraft level.dat file.")
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE)
        } catch (error: Exception) {
            runCatching { deleteTree(temporary) }
            throw if (error is LauncherException) error else LauncherException.FileSystem("The world could not be imported.", error)
        }
        val metadata = readWorldMetadata(destination.resolve("level.dat"))
        ManagedWorld(
            key = worldKey,
            name = metadata.name ?: worldKey,
            sizeBytes = treeSize(destination),
            lastModifiedEpochMillis = metadata.lastPlayedEpochMillis ?: modifiedAt(destination.resolve("level.dat")),
            dataPacks = emptyList(),
            gameMode = metadata.gameMode,
            seed = metadata.seed,
            iconPath = destination.resolve("icon.png").takeIf(Files::isRegularFile)?.toString(),
        )
    }

    override suspend fun deleteWorld(instance: GameInstance, worldKey: String) = withContext(Dispatchers.IO) {
        deleteTree(safeChild(gameDirectory(instance).resolve("saves"), worldKey))
    }

    override suspend fun copyWorld(instance: GameInstance, worldKey: String): ManagedWorld = withContext(Dispatchers.IO) {
        val saves = gameDirectory(instance).resolve("saves")
        val source = safeChild(saves, worldKey)
        if (!Files.isDirectory(source)) throw LauncherException.FileSystem("The selected world does not exist.")
        val copyKey = availableWorldKey(saves, "$worldKey Copy")
        val destination = safeChild(saves, copyKey)
        try {
            copyTree(source, destination)
            val sourceMetadata = readWorldMetadata(source.resolve("level.dat"))
            rewriteWorldName(destination.resolve("level.dat"), "${sourceMetadata.name ?: worldKey} Copy")
        } catch (error: Exception) {
            runCatching { deleteTree(destination) }
            throw LauncherException.FileSystem("The selected world could not be copied.", error)
        }
        val metadata = readWorldMetadata(destination.resolve("level.dat"))
        ManagedWorld(
            key = copyKey,
            name = metadata.name?.let { "$it Copy" } ?: copyKey,
            sizeBytes = treeSize(destination),
            lastModifiedEpochMillis = metadata.lastPlayedEpochMillis ?: modifiedAt(destination.resolve("level.dat")),
            dataPacks = emptyList(),
            gameMode = metadata.gameMode,
            seed = metadata.seed,
            iconPath = destination.resolve("icon.png").takeIf(Files::isRegularFile)?.toString(),
        )
    }

    override suspend fun renameWorld(
        instance: GameInstance,
        worldKey: String,
        newName: String,
    ): ManagedWorld = withContext(Dispatchers.IO) {
        require(newName.isNotBlank()) { "The world name must not be blank." }
        val world = safeChild(gameDirectory(instance).resolve("saves"), worldKey)
        if (!Files.isDirectory(world)) throw LauncherException.FileSystem("The selected world does not exist.")
        rewriteWorldName(world.resolve("level.dat"), newName.trim())
        val metadata = readWorldMetadata(world.resolve("level.dat"))
        ManagedWorld(
            key = worldKey,
            name = metadata.name ?: worldKey,
            sizeBytes = treeSize(world),
            lastModifiedEpochMillis = metadata.lastPlayedEpochMillis ?: modifiedAt(world.resolve("level.dat")),
            dataPacks = emptyList(),
            gameMode = metadata.gameMode,
            seed = metadata.seed,
            iconPath = world.resolve("icon.png").takeIf(Files::isRegularFile)?.toString(),
        )
    }

    override suspend fun resetWorldIcon(instance: GameInstance, worldKey: String) = withContext(Dispatchers.IO) {
        val world = safeChild(gameDirectory(instance).resolve("saves"), worldKey)
        Files.deleteIfExists(world.resolve("icon.png"))
        Unit
    }

    override suspend fun deleteScreenshot(instance: GameInstance, screenshotKey: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(safeChild(gameDirectory(instance).resolve("screenshots"), screenshotKey))
        Unit
    }

    override suspend fun renameScreenshot(
        instance: GameInstance,
        screenshotKey: String,
        newName: String,
    ): ManagedScreenshot = withContext(Dispatchers.IO) {
        val screenshots = gameDirectory(instance).resolve("screenshots")
        val source = safeChild(screenshots, screenshotKey)
        if (!Files.isRegularFile(source)) throw LauncherException.FileSystem("The selected screenshot does not exist.")
        val sourceExtension = source.fileName.toString().substringAfterLast('.', "")
        val requested = safeFileName(newName)
        val destinationName = if (requested.substringAfterLast('.', "").equals(sourceExtension, ignoreCase = true)) {
            requested
        } else {
            "${requested.substringBeforeLast('.', requested)}.$sourceExtension"
        }
        val destination = safeChild(screenshots, destinationName)
        if (Files.exists(destination)) throw LauncherException.FileSystem("A screenshot named $destinationName already exists.")
        Files.move(source, destination)
        ManagedScreenshot(
            key = destinationName,
            fileName = destinationName,
            path = destination.toString(),
            sizeBytes = Files.size(destination),
            createdAtEpochMillis = modifiedAt(destination),
        )
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

    override suspend fun moveServer(instance: GameInstance, serverKey: String, offset: Int) = withContext(Dispatchers.IO) {
        if (offset == 0) return@withContext
        val path = gameDirectory(instance).resolve("servers.dat")
        val servers = readServers(path).toMutableList()
        val sourceIndex = servers.indexOfFirst { it.key == serverKey }
        if (sourceIndex < 0) return@withContext
        val targetIndex = (sourceIndex + offset).coerceIn(0, servers.lastIndex)
        if (sourceIndex == targetIndex) return@withContext
        val server = servers.removeAt(sourceIndex)
        servers.add(targetIndex, server)
        writeServers(path, servers)
    }

    override suspend fun readLog(instance: GameInstance, logKey: String): String = withContext(Dispatchers.IO) {
        val path = safeLogPath(instance, logKey)
        if (!Files.isRegularFile(path)) throw LauncherException.FileSystem("The selected log does not exist.")
        val input = Files.newInputStream(path)
        val stream = if (path.fileName.toString().endsWith(".gz", ignoreCase = true)) GZIPInputStream(input) else input
        stream.bufferedReader().use { reader ->
            val buffer = CharArray(8_192)
            val text = StringBuilder()
            while (text.length < MAX_LOG_CHARACTERS) {
                val read = reader.read(buffer, 0, minOf(buffer.size, MAX_LOG_CHARACTERS - text.length))
                if (read < 0) break
                text.append(buffer, 0, read)
            }
            text.toString()
        }
    }

    override suspend fun deleteLog(instance: GameInstance, logKey: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(safeLogPath(instance, logKey))
        Unit
    }

    private fun readServers(path: Path): List<SavedServer> {
        if (!Files.isRegularFile(path)) return emptyList()
        return runCatching {
            BufferedInputStream(Files.newInputStream(path)).use { source ->
                source.mark(GZIP_HEADER_SIZE)
                val isGzip = source.read() == GZIP_MAGIC_FIRST && source.read() == GZIP_MAGIC_SECOND
                source.reset()
                val decoded = if (isGzip) GZIPInputStream(source) else source
                DataInputStream(decoded).use { input ->
                    if (input.readUnsignedByte() != TAG_COMPOUND) return emptyList()
                    input.readUTF()
                    readRootCompound(input)
                }
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
        DataOutputStream(Files.newOutputStream(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)).use { output ->
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
    private fun safeLogPath(instance: GameInstance, logKey: String): Path {
        require(logKey.isNotBlank() && !Paths.get(logKey).isAbsolute) { "The selected log has an invalid path." }
        val game = gameDirectory(instance).normalize()
        val path = game.resolve(logKey).normalize()
        require(path.startsWith(game) && LOG_DIRECTORIES.any { path.startsWith(game.resolve(it)) }) {
            "The selected log is outside the instance log directories."
        }
        return path
    }

    private fun readWorldMetadata(path: Path): WorldMetadata {
        if (!Files.isRegularFile(path)) return WorldMetadata()
        return runCatching {
            DataInputStream(GZIPInputStream(Files.newInputStream(path))).use { input ->
                if (input.readUnsignedByte() != TAG_COMPOUND) return@use WorldMetadata()
                input.readUTF()
                readWorldRoot(input)
            }
        }.getOrDefault(WorldMetadata())
    }

    private fun rewriteWorldName(path: Path, newName: String) {
        if (!Files.isRegularFile(path)) throw LauncherException.FileSystem("The world does not contain level.dat.")
        val temporary = path.resolveSibling(".${path.fileName}.tmp")
        try {
            DataInputStream(GZIPInputStream(Files.newInputStream(path))).use { input ->
                DataOutputStream(GZIPOutputStream(Files.newOutputStream(temporary))).use { output ->
                    val rootType = input.readUnsignedByte()
                    require(rootType == TAG_COMPOUND) { "level.dat does not contain a root compound." }
                    output.writeByte(rootType)
                    output.writeUTF(input.readUTF())
                    copyNbtPayload(input, output, rootType, emptyList(), newName)
                }
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (error: Exception) {
            runCatching { Files.deleteIfExists(temporary) }
            throw LauncherException.FileSystem("The world name could not be updated.", error)
        }
    }

    private fun copyNbtPayload(
        input: DataInputStream,
        output: DataOutputStream,
        type: Int,
        path: List<String>,
        worldName: String,
    ) {
        when (type) {
            TAG_BYTE -> output.writeByte(input.readByte().toInt())
            TAG_SHORT -> output.writeShort(input.readShort().toInt())
            TAG_INT -> output.writeInt(input.readInt())
            TAG_LONG -> output.writeLong(input.readLong())
            TAG_FLOAT -> output.writeFloat(input.readFloat())
            TAG_DOUBLE -> output.writeDouble(input.readDouble())
            TAG_BYTE_ARRAY -> {
                val count = input.readInt().coerceAtLeast(0)
                output.writeInt(count)
                copyBytes(input, output, count.toLong())
            }
            TAG_STRING -> {
                val value = input.readUTF()
                output.writeUTF(if (path == WORLD_NAME_PATH) worldName else value)
            }
            TAG_LIST -> {
                val itemType = input.readUnsignedByte()
                val count = input.readInt().coerceAtLeast(0)
                output.writeByte(itemType)
                output.writeInt(count)
                repeat(count) { copyNbtPayload(input, output, itemType, path, worldName) }
            }
            TAG_COMPOUND -> while (true) {
                val itemType = input.readUnsignedByte()
                output.writeByte(itemType)
                if (itemType == TAG_END) break
                val name = input.readUTF()
                output.writeUTF(name)
                copyNbtPayload(input, output, itemType, path + name, worldName)
            }
            TAG_INT_ARRAY -> {
                val count = input.readInt().coerceAtLeast(0)
                output.writeInt(count)
                repeat(count) { output.writeInt(input.readInt()) }
            }
            TAG_LONG_ARRAY -> {
                val count = input.readInt().coerceAtLeast(0)
                output.writeInt(count)
                repeat(count) { output.writeLong(input.readLong()) }
            }
            else -> throw LauncherException.InvalidMetadata("level.dat contains an unknown NBT tag.")
        }
    }

    private fun copyBytes(input: DataInputStream, output: DataOutputStream, count: Long) {
        val buffer = ByteArray(8_192)
        var remaining = count
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw LauncherException.InvalidMetadata("level.dat ended unexpectedly.")
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun readWorldRoot(input: DataInputStream): WorldMetadata {
        while (true) {
            val type = input.readUnsignedByte()
            if (type == TAG_END) return WorldMetadata()
            val name = input.readUTF()
            if (type == TAG_COMPOUND && name == "Data") return readWorldData(input)
            skipPayload(input, type)
        }
    }

    private fun readWorldData(input: DataInputStream): WorldMetadata {
        var name: String? = null
        var gameType: Int? = null
        var hardcore = false
        var lastPlayed: Long? = null
        var seed: Long? = null
        while (true) {
            val type = input.readUnsignedByte()
            if (type == TAG_END) break
            when (val key = input.readUTF()) {
                "LevelName" -> if (type == TAG_STRING) name = input.readUTF() else skipPayload(input, type)
                "GameType" -> if (type == TAG_INT) gameType = input.readInt() else skipPayload(input, type)
                "hardcore" -> if (type == TAG_BYTE) hardcore = input.readByte().toInt() != 0 else skipPayload(input, type)
                "LastPlayed" -> if (type == TAG_LONG) lastPlayed = input.readLong() else skipPayload(input, type)
                "RandomSeed" -> if (type == TAG_LONG) seed = input.readLong() else skipPayload(input, type)
                "WorldGenSettings" -> if (type == TAG_COMPOUND) {
                    seed = readWorldGenSettingsSeed(input) ?: seed
                } else {
                    skipPayload(input, type)
                }
                else -> skipPayload(input, type)
            }
        }
        val mode = if (hardcore) {
            "Hardcore"
        } else {
            when (gameType) {
                0 -> "Survival"
                1 -> "Creative"
                2 -> "Adventure"
                3 -> "Spectator"
                else -> null
            }
        }
        return WorldMetadata(name, mode, lastPlayed, seed)
    }

    private fun readWorldGenSettingsSeed(input: DataInputStream): Long? {
        var seed: Long? = null
        while (true) {
            val type = input.readUnsignedByte()
            if (type == TAG_END) return seed
            val name = input.readUTF()
            if (type == TAG_LONG && name == "seed") seed = input.readLong() else skipPayload(input, type)
        }
    }
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
    private fun copyTree(source: Path, destination: Path) {
        Files.walk(source).use { paths ->
            paths.forEach { path ->
                val target = destination.resolve(source.relativize(path).toString())
                if (Files.isDirectory(path)) Files.createDirectories(target)
                else {
                    Files.createDirectories(target.parent)
                    Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
        }
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
        const val GZIP_HEADER_SIZE = 2
        const val GZIP_MAGIC_FIRST = 0x1f
        const val GZIP_MAGIC_SECOND = 0x8b
        val LOG_DIRECTORIES = listOf("logs", "crash-reports", ".trestle/logs")
        val LOG_EXTENSIONS = setOf("log", "txt", "gz")
        val WORLD_NAME_PATH = listOf("Data", "LevelName")
        const val MAX_LOG_CHARACTERS = 2_000_000
        const val MAX_WORLD_ARCHIVE_BYTES = 512 * 1024 * 1024
        const val MAX_EXTRACTED_WORLD_BYTES = 2L * 1024 * 1024 * 1024
        const val MAX_WORLD_ARCHIVE_FILES = 100_000
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

private data class WorldMetadata(
    val name: String? = null,
    val gameMode: String? = null,
    val lastPlayedEpochMillis: Long? = null,
    val seed: Long? = null,
)

private suspend fun queryServerStatus(server: SavedServer): SavedServer = withContext(Dispatchers.IO) {
    runCatching {
        val endpoint = server.address.toServerEndpoint()
        val startedAt = System.nanoTime()
        Socket().use { socket ->
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), SERVER_STATUS_TIMEOUT_MILLIS)
            socket.soTimeout = SERVER_STATUS_TIMEOUT_MILLIS
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            val handshakeBuffer = ByteArrayOutputStream()
            DataOutputStream(handshakeBuffer).use { handshake ->
                handshake.writeVarInt(0)
                handshake.writeVarInt(-1)
                handshake.writeProtocolString(endpoint.host)
                handshake.writeShort(endpoint.port)
                handshake.writeVarInt(1)
            }
            output.writePacket(handshakeBuffer.toByteArray())
            output.writePacket(byteArrayOf(0))
            output.flush()

            val packetLength = input.readVarInt()
            require(packetLength in 1..MAX_SERVER_STATUS_PACKET_BYTES) { "The status response is too large." }
            require(input.readVarInt() == 0) { "The server returned an unexpected status packet." }
            val jsonLength = input.readVarInt()
            require(jsonLength in 0..packetLength && jsonLength <= MAX_SERVER_STATUS_PACKET_BYTES) {
                "The server returned an invalid status message."
            }
            val jsonBytes = input.readNBytes(jsonLength)
            require(jsonBytes.size == jsonLength) { "The server status response ended unexpectedly." }
            val json = jsonBytes.decodeToString()
            val players = PLAYERS_BLOCK.find(json)?.groupValues?.get(1).orEmpty()
            val online = ONLINE_PLAYERS.find(players)?.groupValues?.get(1)?.toIntOrNull()
            val maximum = MAXIMUM_PLAYERS.find(players)?.groupValues?.get(1)?.toIntOrNull()
            val favicon = FAVICON.find(json)?.groupValues?.get(1)?.replace("\\/", "/")
            server.copy(
                status = ServerStatus.ONLINE,
                onlinePlayers = online,
                maximumPlayers = maximum,
                pingMillis = ((System.nanoTime() - startedAt) / 1_000_000).coerceAtLeast(0),
                iconDataUrl = favicon,
            )
        }
    }.getOrElse { server.copy(status = ServerStatus.OFFLINE) }
}

private data class ServerEndpoint(val host: String, val port: Int)

private fun String.toServerEndpoint(): ServerEndpoint {
    val value = trim()
    require(value.isNotBlank()) { "The server address is blank." }
    if (value.startsWith('[')) {
        val closing = value.indexOf(']')
        require(closing > 1) { "The IPv6 server address is invalid." }
        val host = value.substring(1, closing)
        val port = value.substring(closing + 1).removePrefix(":").toIntOrNull() ?: DEFAULT_SERVER_PORT
        return ServerEndpoint(host, port.requireServerPort())
    }
    if (value.count { it == ':' } == 1) {
        val host = value.substringBefore(':')
        val port = value.substringAfter(':').toIntOrNull() ?: DEFAULT_SERVER_PORT
        return ServerEndpoint(host, port.requireServerPort())
    }
    return ServerEndpoint(value, DEFAULT_SERVER_PORT)
}

private fun Int.requireServerPort(): Int = also { require(it in 1..65_535) { "The server port is invalid." } }

private fun DataOutputStream.writePacket(payload: ByteArray) {
    writeVarInt(payload.size)
    write(payload)
}

private fun DataOutputStream.writeProtocolString(value: String) {
    val bytes = value.encodeToByteArray()
    writeVarInt(bytes.size)
    write(bytes)
}

private fun DataOutputStream.writeVarInt(value: Int) {
    var remaining = value
    do {
        var current = remaining and 0x7F
        remaining = remaining ushr 7
        if (remaining != 0) current = current or 0x80
        writeByte(current)
    } while (remaining != 0)
}

private fun DataInputStream.readVarInt(): Int {
    var result = 0
    var shift = 0
    while (shift < 35) {
        val current = readUnsignedByte()
        result = result or ((current and 0x7F) shl shift)
        if (current and 0x80 == 0) return result
        shift += 7
    }
    throw LauncherException.InvalidMetadata("The server returned an invalid variable-length integer.")
}

private const val DEFAULT_SERVER_PORT = 25_565
private const val SERVER_STATUS_TIMEOUT_MILLIS = 1_200
private const val MAX_SERVER_STATUS_PACKET_BYTES = 1_048_576
private val PLAYERS_BLOCK = Regex("\\\"players\\\"\\s*:\\s*\\{(.*?)\\}")
private val ONLINE_PLAYERS = Regex("\\\"online\\\"\\s*:\\s*(\\d+)")
private val MAXIMUM_PLAYERS = Regex("\\\"max\\\"\\s*:\\s*(\\d+)")
private val FAVICON = Regex("\\\"favicon\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
