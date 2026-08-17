package net.blockhost.trestle.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import okio.FileSystem
import okio.Path

@Serializable
data class SkinProfile(
    val id: String,
    val name: String,
    val variant: SkinVariant,
    val textureFile: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

data class SavedSkin(
    val profile: SkinProfile,
    val texture: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is SavedSkin && profile == other.profile && texture.contentEquals(other.texture)

    override fun hashCode(): Int = 31 * profile.hashCode() + texture.contentHashCode()
}

data class MinecraftSkinImage(
    val width: Int,
    val height: Int,
)

@Serializable
private data class SkinLibraryRegistry(
    val schemaVersion: Int = 1,
    val profiles: List<SkinProfile> = emptyList(),
)

class SkinLibrary(
    private val fileSystem: FileSystem,
    private val directory: Path,
    private val nowMillis: () -> Long,
) {
    private val mutex = Mutex()
    private val registryPath = directory / "library.json"
    private var savedSkins = emptyList<SavedSkin>()
    private val mutableSkins = MutableStateFlow<List<SavedSkin>>(emptyList())

    val skins: StateFlow<List<SavedSkin>> = mutableSkins.asStateFlow()

    suspend fun initialize() = mutex.withLock {
        try {
            fileSystem.createDirectories(directory)
            if (!fileSystem.exists(registryPath)) {
                persist()
                return@withLock
            }
            val registry = skinLibraryJson.decodeFromString<SkinLibraryRegistry>(
                fileSystem.read(registryPath) { readUtf8() },
            )
            if (registry.schemaVersion != 1) {
                throw LauncherException.FileSystem(
                    "Skin library schema ${registry.schemaVersion} is not supported.",
                )
            }
            savedSkins = registry.profiles.mapNotNull { profile ->
                val texturePath = directory / profile.textureFile
                if (!fileSystem.exists(texturePath)) return@mapNotNull null
                val texture = fileSystem.read(texturePath) { readByteArray() }
                runCatching { inspectMinecraftSkin(texture) }.getOrNull()?.let { SavedSkin(profile, texture) }
            }.sortedBy { it.profile.name.lowercase() }
            if (savedSkins.size != registry.profiles.size) persist() else publish()
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The skin library could not be read.", error)
        }
    }

    suspend fun save(
        name: String,
        variant: SkinVariant,
        texture: ByteArray,
        profileId: String? = null,
    ): SavedSkin = mutex.withLock {
        inspectMinecraftSkin(texture)
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Give this skin a name before saving it." }
        require(trimmedName.length <= 64) { "Skin names cannot be longer than 64 characters." }

        try {
            val existing = profileId?.let { id -> savedSkins.firstOrNull { it.profile.id == id } }
            val timestamp = nowMillis()
            val id = existing?.profile?.id ?: createId(timestamp, texture)
            val textureFile = existing?.profile?.textureFile ?: "$id.png"
            val profile = SkinProfile(
                id = id,
                name = trimmedName,
                variant = variant,
                textureFile = textureFile,
                createdAtEpochMillis = existing?.profile?.createdAtEpochMillis ?: timestamp,
                updatedAtEpochMillis = timestamp,
            )
            writeAtomically(directory / textureFile, texture)
            val saved = SavedSkin(profile, texture.copyOf())
            savedSkins = (savedSkins.filterNot { it.profile.id == id } + saved)
                .sortedBy { it.profile.name.lowercase() }
            persist()
            saved
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The skin could not be saved.", error)
        }
    }

    suspend fun delete(profileId: String) = mutex.withLock {
        val saved = savedSkins.firstOrNull { it.profile.id == profileId } ?: return@withLock
        try {
            val texturePath = directory / saved.profile.textureFile
            if (fileSystem.exists(texturePath)) fileSystem.delete(texturePath)
            savedSkins = savedSkins.filterNot { it.profile.id == profileId }
            persist()
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The skin could not be removed from the library.", error)
        }
    }

    private fun persist() {
        fileSystem.createDirectories(directory)
        val content = skinLibraryJson.encodeToString(
            SkinLibraryRegistry.serializer(),
            SkinLibraryRegistry(profiles = savedSkins.map(SavedSkin::profile)),
        ).encodeToByteArray()
        writeAtomically(registryPath, content)
        publish()
    }

    private fun writeAtomically(path: Path, content: ByteArray) {
        val temporaryPath = directory / ".${path.name}.tmp"
        fileSystem.write(temporaryPath) {
            write(content)
            flush()
        }
        fileSystem.atomicMove(temporaryPath, path)
    }

    private fun publish() {
        mutableSkins.value = savedSkins.toList()
    }

    private fun createId(timestamp: Long, texture: ByteArray): String {
        val base = "skin-$timestamp-${texture.contentHashCode().toUInt().toString(16)}"
        var candidate = base
        var suffix = 2
        while (savedSkins.any { it.profile.id == candidate }) {
            candidate = "$base-$suffix"
            suffix++
        }
        return candidate
    }
}

fun inspectMinecraftSkin(bytes: ByteArray): MinecraftSkinImage {
    if (bytes.size !in 24..MAX_SKIN_BYTES) {
        throw LauncherException.FileSystem("The selected skin must be a PNG smaller than 2 MiB.")
    }
    if (!bytes.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)) {
        throw LauncherException.FileSystem("The selected skin is not a PNG image.")
    }
    if (!bytes.copyOfRange(12, 16).contentEquals(IHDR_CHUNK)) {
        throw LauncherException.FileSystem("The selected PNG does not contain a valid image header.")
    }
    val width = bytes.readBigEndianInt(16)
    val height = bytes.readBigEndianInt(20)
    if (width != 64 || height !in setOf(32, 64)) {
        throw LauncherException.FileSystem("Minecraft Java skins must be 64×64 or legacy 64×32 PNG files.")
    }
    return MinecraftSkinImage(width, height)
}

private fun ByteArray.readBigEndianInt(offset: Int): Int =
    ((this[offset].toInt() and 0xff) shl 24) or
        ((this[offset + 1].toInt() and 0xff) shl 16) or
        ((this[offset + 2].toInt() and 0xff) shl 8) or
        (this[offset + 3].toInt() and 0xff)

private const val MAX_SKIN_BYTES = 2 * 1024 * 1024
private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
private val IHDR_CHUNK = byteArrayOf(0x49, 0x48, 0x44, 0x52)
private val skinLibraryJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    ignoreUnknownKeys = true
}
