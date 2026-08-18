package net.blockhost.trestle.instance

import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import java.io.DataOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmGameDataManagerTest {
    @Test
    fun readsMinecraftServerListAndWritesUncompressedNbt() = runTest {
        val root = createTempDirectory("trestle-server-list")
        try {
            val game = root.resolve("game").createDirectories()
            val serversFile = game.resolve("servers.dat")
            writeServersDat(serversFile, compressed = false)
            val instance = testInstance(root)
            val manager = JvmGameDataManager(serverStatusProvider = { it })

            assertEquals("play.example.net", manager.inventory(instance).servers.single().address)

            manager.upsertServer(instance, SavedServer("", "Backup", "backup.example.net"))

            Files.newInputStream(serversFile).use { input ->
                assertEquals(10, input.read())
            }
            assertEquals(
                listOf("play.example.net", "backup.example.net"),
                manager.inventory(instance).servers.map(SavedServer::address),
            )
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun readsGzipServerListWrittenByOlderTrestleVersions() = runTest {
        val root = createTempDirectory("trestle-legacy-server-list")
        try {
            val game = root.resolve("game").createDirectories()
            writeServersDat(game.resolve("servers.dat"), compressed = true)
            val manager = JvmGameDataManager(serverStatusProvider = { it })

            val servers = manager.inventory(testInstance(root)).servers

            assertEquals("play.example.net", servers.single().address)
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun managesWorldBackupsDataPacksScreenshotsAndServers() = runTest {
        val root = createTempDirectory("trestle-game-data")
        try {
            val game = root.resolve("game").createDirectories()
            val world = game.resolve("saves/My World").createDirectories()
            writeLevelDat(world.resolve("level.dat"))
            val dataPacks = world.resolve("datapacks").createDirectories()
            dataPacks.resolve("recipes.zip").writeText("pack")
            val screenshots = game.resolve("screenshots").createDirectories()
            screenshots.resolve("shot.png").createFile()
            val logs = game.resolve("logs").createDirectories()
            logs.resolve("latest.log").writeText("first line\nsecond line")
            val instance = GameInstance(
                id = InstanceId("test"),
                displayName = "Test",
                minecraftVersionId = "1.21.1",
                instanceDirectory = root.toString(),
            )
            val manager = JvmGameDataManager(serverStatusProvider = { it }) { 1234L }

            manager.upsertServer(instance, SavedServer("", "Example", "play.example.net"))
            manager.upsertServer(instance, SavedServer("", "Backup", "backup.example.net"))
            val backupServerKey = manager.inventory(instance).servers.last().key
            manager.moveServer(instance, backupServerKey, -1)
            manager.setDataPackEnabled(instance, "My World", "recipes.zip", false)
            val backup = manager.backupWorld(instance, "My World")
            val copy = manager.copyWorld(instance, "My World")
            val renamedWorld = manager.renameWorld(instance, copy.key, "Copied World")
            val imported = manager.importWorld(instance, "Imported.zip", Files.readAllBytes(java.nio.file.Path.of(backup.path)))
            manager.deleteWorld(instance, "My World")
            val restored = manager.restoreWorldBackup(instance, backup.key)
            val renamedScreenshot = manager.renameScreenshot(instance, "shot.png", "renamed")
            val logText = manager.readLog(instance, "logs/latest.log")
            manager.deleteLog(instance, "logs/latest.log")
            val inventory = manager.inventory(instance)

            assertEquals("My World", restored.name)
            assertEquals("My World Copy", copy.key)
            assertEquals("Copied World", renamedWorld.name)
            assertEquals("Imported", imported.key)
            assertEquals("backup.example.net", inventory.servers.first().address)
            assertEquals(1, inventory.backups.size)
            assertTrue(inventory.worlds.all { it.dataPacks.single().fileName == "recipes.zip" })
            assertTrue(inventory.worlds.all { !it.dataPacks.single().enabled })
            assertEquals(setOf("Test World", "Copied World"), inventory.worlds.map { it.name }.toSet())
            assertTrue(inventory.worlds.all { it.gameMode == "Creative" })
            assertTrue(inventory.worlds.all { it.seed == 987654321L })
            assertEquals("renamed.png", renamedScreenshot.fileName)
            assertEquals("renamed.png", inventory.screenshots.single().fileName)
            assertEquals("first line\nsecond line", logText)
            assertTrue(inventory.logs.isEmpty())
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).use { paths ->
                paths.forEach { Files.deleteIfExists(it) }
            }
        }
    }

    private fun writeLevelDat(path: java.nio.file.Path) {
        DataOutputStream(GZIPOutputStream(Files.newOutputStream(path))).use { output ->
            output.writeByte(10)
            output.writeUTF("")
            output.writeByte(10)
            output.writeUTF("Data")
            output.writeByte(8)
            output.writeUTF("LevelName")
            output.writeUTF("Test World")
            output.writeByte(3)
            output.writeUTF("GameType")
            output.writeInt(1)
            output.writeByte(4)
            output.writeUTF("LastPlayed")
            output.writeLong(1_234_567L)
            output.writeByte(10)
            output.writeUTF("WorldGenSettings")
            output.writeByte(4)
            output.writeUTF("seed")
            output.writeLong(987654321L)
            output.writeByte(0)
            output.writeByte(0)
            output.writeByte(0)
        }
    }

    private fun writeServersDat(path: java.nio.file.Path, compressed: Boolean) {
        val fileOutput = Files.newOutputStream(path)
        val output: OutputStream = if (compressed) GZIPOutputStream(fileOutput) else fileOutput
        DataOutputStream(output).use { data ->
            data.writeByte(10)
            data.writeUTF("")
            data.writeByte(9)
            data.writeUTF("servers")
            data.writeByte(10)
            data.writeInt(1)
            data.writeByte(8)
            data.writeUTF("name")
            data.writeUTF("Example")
            data.writeByte(8)
            data.writeUTF("ip")
            data.writeUTF("play.example.net")
            data.writeByte(0)
            data.writeByte(0)
        }
    }

    private fun testInstance(root: java.nio.file.Path) = GameInstance(
        id = InstanceId("test"),
        displayName = "Test",
        minecraftVersionId = "1.21.1",
        instanceDirectory = root.toString(),
    )

    private fun deleteTree(root: java.nio.file.Path) {
        Files.walk(root).sorted(Comparator.reverseOrder()).use { paths ->
            paths.forEach { Files.deleteIfExists(it) }
        }
    }
}
