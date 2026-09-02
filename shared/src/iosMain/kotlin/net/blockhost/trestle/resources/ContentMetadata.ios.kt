package net.blockhost.trestle.resources

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.blockhost.trestle.platform.useOkio
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip

internal actual fun readEmbeddedMetadata(fileSystem: FileSystem, path: Path): ContentMetadata =
    fileSystem.openZip(path).useOkio { archive ->
        fun document(name: String): JsonElement? = runCatching {
            archive.source("/$name".toPath()).buffer().useOkio { source ->
                Json.parseToJsonElement(source.readUtf8(1_048_576))
            }
        }.getOrNull()

        (document("fabric.mod.json") as? JsonObject)?.let { mod ->
            return ContentMetadata(
                name = mod.string("name") ?: mod.string("id"),
                version = mod.string("version"),
                authors = mod["authors"].names(),
                dependencies = (mod["depends"] as? JsonObject)?.keys?.toList().orEmpty(),
                websiteUrl = (mod["contact"] as? JsonObject)?.string("homepage"),
            )
        }
        ((document("quilt.mod.json") as? JsonObject)?.get("quilt_loader") as? JsonObject)?.let { mod ->
            val metadata = mod["metadata"] as? JsonObject
            return ContentMetadata(
                name = metadata?.string("name") ?: mod.string("id"),
                version = mod.string("version"),
                authors = (metadata?.get("contributors") as? JsonObject)?.keys?.toList().orEmpty(),
                dependencies = (mod["depends"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonObject)?.string("id") }
                    .orEmpty(),
                websiteUrl = (metadata?.get("contact") as? JsonObject)?.string("homepage"),
            )
        }
        document("mcmod.info")?.let { value ->
            val mods = (value as? JsonArray) ?: (value as? JsonObject)?.get("modList") as? JsonArray
            val mod = mods?.firstOrNull() as? JsonObject
            if (mod != null) {
                return ContentMetadata(
                    name = mod.string("name") ?: mod.string("modid"),
                    version = mod.string("version"),
                    authors = mod["authorList"].names(),
                    websiteUrl = mod.string("url"),
                )
            }
        }
        ContentMetadata()
    }

private fun JsonObject.string(key: String): String? =
    (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

private fun JsonElement?.names(): List<String> = (this as? JsonArray)?.mapNotNull {
    (it as? JsonPrimitive)?.contentOrNull ?: (it as? JsonObject)?.string("name")
}.orEmpty()

internal actual fun curseForgeFingerprint(fileSystem: FileSystem, path: Path): Long {
    fun isIgnored(byte: Int) = byte == 9 || byte == 10 || byte == 13 || byte == 32
    var length = 0
    fileSystem.source(path).buffer().useOkio { source ->
        while (!source.exhausted()) if (!isIgnored(source.readByte().toInt() and 255)) length++
    }
    var hash = 1 xor length
    var word = 0
    var shift = 0
    val multiplier = 0x5bd1e995
    fileSystem.source(path).buffer().useOkio { source ->
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
