package net.blockhost.trestle.instance

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_the_instance_could_not_be_exported
import net.blockhost.trestle.resources.msg_the_instance_directory_does_not_exist
import net.blockhost.trestle.resources.msg_vanilla_does_not_have_a_loader_component
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text
import net.blockhost.trestle.text.userError
import okio.Path
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class JvmInstanceExporter : InstanceExporter {
    override fun export(instance: GameInstance, destination: Path): Path {
        val source = Paths.get(instance.instanceDirectory).toAbsolutePath().normalize()
        val target = Paths.get(destination.toString()).toAbsolutePath().normalize()
        val temporary = target.resolveSibling(".${target.fileName}.tmp")
        try {
            requireUser(Files.isDirectory(source)) { text(Res.string.msg_the_instance_directory_does_not_exist) }
            Files.createDirectories(requireNotNull(target.parent))
            ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                zip.writeTextEntry("mmc-pack.json", prismManifest(instance))
                zip.writeTextEntry("instance.cfg", "name=${instance.displayName.replace('\n', ' ')}\n")
                zip.writeTextEntry("trestle-instance.json", exportJson.encodeToString(instance))
                listOf("modpack.json", "resources.json").forEach { name ->
                    val metadata = source.resolve(".trestle").resolve(name)
                    if (Files.isRegularFile(metadata) && !Files.isSymbolicLink(metadata)) {
                        zip.putNextEntry(ZipEntry("trestle-metadata/$name"))
                        Files.copy(metadata, zip)
                        zip.closeEntry()
                    }
                }
                val gameDirectory = source.resolve("game")
                Files.walk(gameDirectory).use { paths ->
                    paths.filter { Files.isRegularFile(it) }.forEach { file ->
                        val relative = gameDirectory.relativize(file).joinToString("/") { it.toString() }
                        zip.putNextEntry(ZipEntry(".minecraft/$relative"))
                        Files.copy(file, zip)
                        zip.closeEntry()
                    }
                }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return destination
        } catch (error: Exception) {
            runCatching { Files.deleteIfExists(temporary) }
            throw LauncherException.FileSystem(text(Res.string.msg_the_instance_could_not_be_exported), error)
        }
    }

    private fun prismManifest(instance: GameInstance): String = buildJsonObject {
        put("formatVersion", 1)
        put(
            "components",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("uid", "net.minecraft")
                        put("version", instance.minecraftVersionId)
                    },
                )
                instance.loaderVersion?.let { loaderVersion ->
                    add(
                        buildJsonObject {
                            put("uid", instance.modLoader.prismUid())
                            put("version", loaderVersion)
                        },
                    )
                }
            },
        )
    }.toString()

    private fun ZipOutputStream.writeTextEntry(name: String, value: String) {
        putNextEntry(ZipEntry(name))
        write(value.encodeToByteArray())
        closeEntry()
    }

    private fun ModLoader.prismUid(): String = when (this) {
        ModLoader.FABRIC -> "net.fabricmc.fabric-loader"
        ModLoader.NEOFORGE -> "net.neoforged"
        ModLoader.FORGE -> "net.minecraftforge"
        ModLoader.QUILT -> "org.quiltmc.quilt-loader"
        ModLoader.VANILLA -> userError(text(Res.string.msg_vanilla_does_not_have_a_loader_component))
    }

    private companion object {
        val exportJson = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
