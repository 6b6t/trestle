package net.blockhost.trestle.instance

import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmGameDataManagerTest {
    @Test
    fun managesWorldBackupsDataPacksScreenshotsAndServers() = runTest {
        val root = createTempDirectory("trestle-game-data")
        try {
            val game = root.resolve("game").createDirectories()
            val world = game.resolve("saves/My World").createDirectories()
            world.resolve("level.dat").writeText("level")
            val dataPacks = world.resolve("datapacks").createDirectories()
            dataPacks.resolve("recipes.zip").writeText("pack")
            val screenshots = game.resolve("screenshots").createDirectories()
            screenshots.resolve("shot.png").createFile()
            val instance = GameInstance(
                id = InstanceId("test"),
                displayName = "Test",
                minecraftVersionId = "1.21.1",
                instanceDirectory = root.toString(),
            )
            val manager = JvmGameDataManager { 1234L }

            manager.upsertServer(instance, SavedServer("", "Example", "play.example.net"))
            manager.setDataPackEnabled(instance, "My World", "recipes.zip", false)
            val backup = manager.backupWorld(instance, "My World")
            manager.deleteWorld(instance, "My World")
            val restored = manager.restoreWorldBackup(instance, backup.key)
            manager.deleteScreenshot(instance, "shot.png")
            val inventory = manager.inventory(instance)

            assertEquals("My World", restored.name)
            assertEquals("play.example.net", inventory.servers.single().address)
            assertEquals(1, inventory.backups.size)
            assertTrue(inventory.worlds.single().dataPacks.single().fileName == "recipes.zip")
            assertFalse(inventory.worlds.single().dataPacks.single().enabled)
            assertTrue(inventory.screenshots.isEmpty())
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).use { paths ->
                paths.forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
