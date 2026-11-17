package net.blockhost.trestle.runtime

import kotlinx.serialization.json.Json
import net.blockhost.trestle.metadata.AssetIndex
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Minecraft 1.7.2 and older read assets by file name instead of from the hashed object store. Their asset index is
 * marked `virtual` (1.6 to 1.7.2) or `map_to_resources` (1.5.2 and older). This copies the objects to the folder those
 * versions expect and returns it for the `game_assets` argument. Newer versions use the assets root unchanged.
 */
internal object LegacyAssets {
    private val json = Json { ignoreUnknownKeys = true }

    fun prepare(assetsRoot: Path, indexId: String, gameDirectory: Path): Path {
        val indexFile = assetsRoot.resolve("indexes").resolve("$indexId.json")
        val index = indexFile.takeIf { indexId.isNotBlank() && Files.isRegularFile(it) }
            ?.let { json.decodeFromString<AssetIndex>(Files.readString(it)) }
        val target = when {
            index == null -> null
            index.mapToResources -> gameDirectory.resolve("resources")
            index.virtual -> assetsRoot.resolve("virtual").resolve(indexId)
            else -> null
        }?.normalize()
        if (index == null || target == null) return assetsRoot
        copyObjects(assetsRoot, index, target)
        return target
    }

    private fun copyObjects(assetsRoot: Path, index: AssetIndex, target: Path) {
        index.objects.forEach { (name, asset) ->
            val destination = target.resolve(name).normalize()
            if (!destination.startsWith(target)) return@forEach
            val source = assetsRoot.resolve("objects").resolve(asset.hash.take(2)).resolve(asset.hash)
            if (!Files.isRegularFile(source)) return@forEach
            if (Files.isRegularFile(destination) && Files.size(destination) == asset.size) return@forEach
            Files.createDirectories(destination.parent)
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
