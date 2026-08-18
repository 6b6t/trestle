package net.blockhost.trestle.instance

import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JvmInstanceExporterTest {
    @Test
    fun exportsPortablePrismCompatibleArchive() {
        val root = createTempDirectory("trestle-export")
        try {
            root.resolve("instance/game/mods").createDirectories().resolve("example.jar").writeText("mod")
            val instance = GameInstance(
                id = InstanceId("export-test"),
                displayName = "Export Test",
                minecraftVersionId = "1.21.1",
                modLoader = ModLoader.FABRIC,
                loaderVersion = "0.16.14",
                instanceDirectory = root.resolve("instance").toString(),
            )
            val archive = root.resolve("export.zip")

            JvmInstanceExporter().export(instance, archive.toString().toPath())

            ZipFile(archive.toFile()).use { zip ->
                assertNotNull(zip.getEntry("mmc-pack.json"))
                assertNotNull(zip.getEntry("trestle-instance.json"))
                assertNotNull(zip.getEntry(".minecraft/mods/example.jar"))
                val manifest = zip.getInputStream(zip.getEntry("mmc-pack.json")).bufferedReader().readText()
                assertContains(manifest, "net.fabricmc.fabric-loader")
                val settings = zip.getInputStream(zip.getEntry("instance.cfg")).bufferedReader().readText()
                assertEquals("name=Export Test\n", settings)
            }
        } finally {
            Files.walk(root).sorted(Comparator.reverseOrder()).use { paths ->
                paths.forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
