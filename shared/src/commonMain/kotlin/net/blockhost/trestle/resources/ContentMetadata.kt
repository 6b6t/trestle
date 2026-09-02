package net.blockhost.trestle.resources

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.platform.useOkio
import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer

@Serializable
data class ContentMetadata(
    val name: String? = null,
    val version: String? = null,
    val authors: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val websiteUrl: String? = null,
    val iconUrl: String? = null,
    val provider: ResourceProvider? = null,
    val projectId: String? = null,
    val versionId: String? = null,
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val checkedAtMillis: Long = 0,
)

internal expect fun readEmbeddedMetadata(fileSystem: FileSystem, path: Path): ContentMetadata
internal expect fun curseForgeFingerprint(fileSystem: FileSystem, path: Path): Long

internal fun FileSystem.sha1(path: Path): String = HashingSource.sha1(source(path)).useOkio { source ->
    blackholeSink().buffer().useOkio { it.writeAll(source) }
    source.hash.hex()
}

internal fun FileSystem.sha256(path: Path): String = HashingSource.sha256(source(path)).useOkio { source ->
    blackholeSink().buffer().useOkio { it.writeAll(source) }
    source.hash.hex()
}

/** Cache keys are content hashes, never filenames or modification times. */
class ContentIdentifier(
    private val platforms: ResourcePlatformRegistry,
    private val fileSystem: FileSystem,
    private val nowMillis: () -> Long,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun identify(instance: GameInstance, content: List<InstalledContent>, online: Boolean): List<InstalledContent> {
        val root = instance.instanceDirectory.toPath()
        val cache = root / ".trestle" / "content-metadata"
        val unavailable = mutableSetOf<ResourceProvider>()
        return content.map { item ->
            currentCoroutineContext().ensureActive()
            if (!item.key.startsWith("local:")) return@map item
            try {
                val path = checkedContentPath(fileSystem, root / "game", item.key.removePrefix("local:"))
                if ((fileSystem.metadataOrNull(path)?.size ?: 0) > 512L * 1024 * 1024) return@map item
                val sha1 = fileSystem.sha1(path)
                val cacheFile = cache / "$sha1.json"
                var metadata = runCatching {
                    json.decodeFromString<ContentMetadata>(fileSystem.read(cacheFile) { readUtf8() })
                }.getOrNull() ?: readEmbeddedMetadata(fileSystem, path)
                val now = nowMillis()
                val expired = now < metadata.checkedAtMillis || now - metadata.checkedAtMillis > 7 * 24 * 60 * 60 * 1000L
                if (online && (metadata.checkedAtMillis == 0L || expired)) {
                    var failed = false
                    var found = false
                    for (provider in listOf(ResourceProvider.MODRINTH, ResourceProvider.CURSEFORGE)) {
                        val platform = platforms.find(provider) ?: continue
                        if (!platform.isAvailable || provider in unavailable) continue
                        try {
                            val version = if (provider == ResourceProvider.MODRINTH) platform.versionBySha1(sha1)
                            else platform.versionByFingerprint(curseForgeFingerprint(fileSystem, path), sha1)
                            if (version == null || version.files.none { it.sha1.equals(sha1, ignoreCase = true) }) continue
                            val project = platform.projectsByIds(listOf(version.projectId))[version.projectId] ?: continue
                            if (project.type != item.type) continue
                            metadata = metadata.copy(
                                name = project.name,
                                version = version.versionNumber,
                                authors = project.author.takeIf(String::isNotBlank)?.let(::listOf) ?: metadata.authors,
                                websiteUrl = project.websiteUrl,
                                iconUrl = project.iconUrl,
                                provider = provider,
                                projectId = version.projectId,
                                versionId = version.id,
                                gameVersions = version.gameVersions,
                                loaders = version.loaders,
                                checkedAtMillis = now,
                            )
                            found = true
                            break
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            failed = true
                            unavailable += provider
                        }
                    }
                    if (!failed && (found || unavailable.isEmpty())) metadata = metadata.copy(checkedAtMillis = now)
                }
                fileSystem.createDirectories(cache)
                val temporary = cache / "$sha1.tmp"
                fileSystem.write(temporary) { writeUtf8(json.encodeToString(ContentMetadata.serializer(), metadata)) }
                fileSystem.atomicMove(temporary, cacheFile)
                item.copy(
                    name = metadata.name ?: item.name,
                    versionNumber = metadata.version ?: item.versionNumber,
                    websiteUrl = metadata.websiteUrl?.takeIf { it.startsWith("https://") },
                    iconUrl = metadata.iconUrl?.takeIf { it.startsWith("https://") },
                    authors = metadata.authors,
                    dependencies = metadata.dependencies,
                    provider = metadata.provider,
                    projectId = metadata.projectId,
                    versionId = metadata.versionId,
                    gameVersions = metadata.gameVersions,
                    loaders = metadata.loaders,
                    contentSha1 = sha1,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                item // One unreadable archive must not hide the rest of the inventory.
            }
        }
    }
}

/** Validate every ancestor as well as the final entry to prevent symlink escapes. */
internal fun checkedContentPath(fileSystem: FileSystem, root: Path, relative: String): Path {
    require(relative.isNotBlank() && !relative.startsWith('/') && '\\' !in relative && ':' !in relative) { "Unsafe content path." }
    val parts = relative.split('/')
    require(parts.none { it.isBlank() || it == "." || it == ".." }) { "Unsafe content path." }
    var path = root
    require(fileSystem.metadataOrNull(path)?.symlinkTarget == null) { "Content directories must not be symlinks." }
    parts.forEach { part ->
        path /= part
        require(fileSystem.metadataOrNull(path)?.symlinkTarget == null) { "Content paths must not contain symlinks." }
    }
    return path
}
