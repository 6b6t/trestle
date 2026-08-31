package net.blockhost.trestle.resources

import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okio.FileSystem
import okio.Path
import okio.buffer
import org.tomlj.Toml

internal actual fun readEmbeddedMetadata(fileSystem: FileSystem, path: Path): ContentMetadata {
    val documents = mutableMapOf<String, String>()
    val wanted = setOf("fabric.mod.json", "quilt.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "mcmod.info", "pack.mcmeta")
    ZipInputStream(fileSystem.source(path).buffer().inputStream()).use { zip ->
        var total = 0L
        val buffer = ByteArray(8192)
        repeat(10000) {
            val entry = zip.nextEntry ?: return@use
            val output = if (entry.name in wanted) ByteArrayOutputStream() else null
            while (true) {
                val count = zip.read(buffer)
                if (count < 0) break
                total += count
                if (total > 64L * 1024 * 1024 || (output?.size() ?: 0) + count > 1024 * 1024) return@use
                output?.write(buffer, 0, count)
            }
            output?.let { documents[entry.name] = it.toString(Charsets.UTF_8.name()) }
        }
    }
    fun json(name: String) = documents[name]?.let { Json.parseToJsonElement(it) }
    (json("fabric.mod.json") as? JsonObject)?.let { mod ->
        return ContentMetadata(mod.string("name") ?: mod.string("id"), mod.string("version"),
            mod["authors"].names(), (mod["depends"] as? JsonObject)?.keys?.toList().orEmpty(),
            (mod["contact"] as? JsonObject)?.string("homepage"))
    }
    (json("quilt.mod.json")?.jsonObject?.get("quilt_loader") as? JsonObject)?.let { mod ->
        val metadata = mod["metadata"] as? JsonObject
        return ContentMetadata(metadata?.string("name") ?: mod.string("id"), mod.string("version"),
            (metadata?.get("contributors") as? JsonObject)?.keys?.toList().orEmpty(),
            (mod["depends"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.string("id") }.orEmpty(),
            (metadata?.get("contact") as? JsonObject)?.string("homepage"))
    }
    (documents["META-INF/neoforge.mods.toml"] ?: documents["META-INF/mods.toml"])?.let { text ->
        val toml = Toml.parse(text)
        if (!toml.hasErrors()) {
            val mods = toml.getArray("mods")
            val mod = mods?.takeIf { it.size() > 0 }?.getTable(0)
            if (mod != null) {
                val id = mod.getString("modId")
                val deps = id?.let { toml.getTable("dependencies")?.getArray(listOf(it)) }
                return ContentMetadata(mod.getString("displayName") ?: id,
                    mod.getString("version")?.takeUnless { it.contains("\${") },
                    mod.getString("authors")?.split(',')?.map(String::trim).orEmpty(),
                    (0 until (deps?.size() ?: 0)).mapNotNull { deps?.getTable(it)?.getString("modId") },
                    mod.getString("displayURL"))
            }
        }
    }
    json("mcmod.info")?.let { value ->
        val mods = (value as? JsonArray) ?: (value as? JsonObject)?.get("modList") as? JsonArray
        val mod = mods?.firstOrNull() as? JsonObject
        if (mod != null) return ContentMetadata(mod.string("name") ?: mod.string("modid"), mod.string("version"), mod["authorList"].names(), websiteUrl = mod.string("url"))
    }
    return ContentMetadata()
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
private fun JsonElement?.names(): List<String> = (this as? JsonArray)?.mapNotNull {
    (it as? JsonPrimitive)?.contentOrNull ?: (it as? JsonObject)?.string("name")
}.orEmpty()

/** CurseForge uses MurmurHash2, seed 1, with ASCII whitespace removed. */
internal actual fun curseForgeFingerprint(fileSystem: FileSystem, path: Path): Long {
    fun isIgnored(byte: Int) = byte == 9 || byte == 10 || byte == 13 || byte == 32
    var length = 0
    fileSystem.source(path).buffer().use { source ->
        while (!source.exhausted()) if (!isIgnored(source.readByte().toInt() and 255)) length++
    }
    var hash = 1 xor length
    var word = 0
    var shift = 0
    val multiplier = 0x5bd1e995
    fileSystem.source(path).buffer().use { source ->
        while (!source.exhausted()) {
            val byte = source.readByte().toInt() and 255
            if (isIgnored(byte)) continue
            word = word or (byte shl shift)
            shift += 8
            if (shift == 32) {
                word *= multiplier
                word = word xor (word ushr 24)
                word *= multiplier
                hash = (hash * multiplier) xor word
                word = 0
                shift = 0
            }
        }
    }
    if (shift > 0) hash = (hash xor word) * multiplier
    hash = hash xor (hash ushr 13)
    hash *= multiplier
    hash = hash xor (hash ushr 15)
    return hash.toLong() and 0xffffffffL
}
