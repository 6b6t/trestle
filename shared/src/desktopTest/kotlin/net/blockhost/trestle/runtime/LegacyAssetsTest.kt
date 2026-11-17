package net.blockhost.trestle.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyAssetsTest {
    @Test
    fun virtualIndexesAreCopiedByName() {
        val root = createTempDirectory("trestle-legacy-assets")
        try {
            val assets = writeAssets(root, "legacy", """"virtual":true""")

            val folder = LegacyAssets.prepare(assets, "legacy", root.resolve("game"))

            assertEquals(assets.resolve("virtual").resolve("legacy"), folder)
            assertEquals("click", folder.resolve("sounds/random/click.ogg").readText())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun resourceMappedIndexesAreCopiedIntoTheGameFolder() {
        val root = createTempDirectory("trestle-legacy-resources")
        try {
            val assets = writeAssets(root, "pre-1.6", """"map_to_resources":true""")

            val folder = LegacyAssets.prepare(assets, "pre-1.6", root.resolve("game"))

            assertEquals(root.resolve("game").resolve("resources"), folder)
            assertEquals("click", folder.resolve("sounds/random/click.ogg").readText())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun modernIndexesUseTheAssetsRoot() {
        val root = createTempDirectory("trestle-modern-assets")
        try {
            val assets = writeAssets(root, "17", """"virtual":false""")

            assertEquals(assets, LegacyAssets.prepare(assets, "17", root.resolve("game")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun writeAssets(root: Path, indexId: String, flag: String): Path {
        val assets = root.resolve("assets")
        val hash = "0123456789abcdef0123456789abcdef01234567"
        assets.resolve("objects").resolve("01").createDirectories().resolve(hash).writeText("click")
        assets.resolve("indexes").createDirectories().resolve("$indexId.json").writeText(
            """{"objects":{"sounds/random/click.ogg":{"hash":"$hash","size":5}},$flag}""",
        )
        Files.createDirectories(root.resolve("game"))
        return assets
    }
}
