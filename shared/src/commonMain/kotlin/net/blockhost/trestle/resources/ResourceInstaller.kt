package net.blockhost.trestle.resources

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.download.DownloadRequest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

data class ResourceInstallSummary(
    val installedFiles: Int,
    val dependencyCount: Int,
)

data class InstalledContent(
    val key: String,
    val name: String,
    val type: ResourceType,
    val fileNames: List<String>,
    val enabled: Boolean,
    val direct: Boolean,
    val provider: ResourceProvider? = null,
    val projectId: String? = null,
    val versionId: String? = null,
    val requiredByCount: Int = 0,
) {
    val isTracked: Boolean get() = provider != null && projectId != null
    val canManage: Boolean get() = direct
}

class ResourceInstaller(
    private val platforms: ResourcePlatformRegistry,
    private val downloadPipeline: DownloadPipeline,
    private val fileSystem: FileSystem,
) {
    fun installedContent(instance: GameInstance): List<InstalledContent> {
        val root = instance.instanceDirectory.toPath()
        val manifest = readManifest(root / ".trestle" / "resources.json")
        val trackedFiles = manifest.resources.flatMapTo(mutableSetOf()) { it.files }
        val tracked = manifest.resources.map { resource ->
            InstalledContent(
                key = resourceKey(resource.provider, resource.projectId),
                name = resource.name,
                type = resource.type,
                fileNames = resource.files.map { it.substringAfterLast('/') },
                enabled = resource.enabled,
                direct = resource.direct,
                provider = resource.provider,
                projectId = resource.projectId,
                versionId = resource.versionId,
                requiredByCount = resource.requiredBy.size,
            )
        }
        val local = MANAGED_RESOURCE_TYPES.flatMap { type ->
            val folder = root / "game" / type.installFolder()
            val metadata = fileSystem.metadataOrNull(folder)
            if (metadata?.isDirectory != true) return@flatMap emptyList()
            fileSystem.list(folder).mapNotNull { file ->
                if (fileSystem.metadataOrNull(file)?.isRegularFile != true) return@mapNotNull null
                val relativePath = "${type.installFolder()}/${file.name}"
                if (relativePath in trackedFiles) return@mapNotNull null
                val enabled = !file.name.endsWith(DISABLED_SUFFIX)
                val visibleName = file.name.removeSuffix(DISABLED_SUFFIX)
                if (visibleName.substringAfterLast('.', "").lowercase() !in type.localExtensions()) {
                    return@mapNotNull null
                }
                InstalledContent(
                    key = "local:$relativePath",
                    name = visibleName,
                    type = type,
                    fileNames = listOf(file.name),
                    enabled = enabled,
                    direct = true,
                )
            }
        }
        return (tracked + local).sortedWith(
            compareBy<InstalledContent> { it.type.ordinal }
                .thenByDescending { it.direct }
                .thenBy { it.name.lowercase() },
        )
    }

    suspend fun latestCompatibleVersion(instance: GameInstance, content: InstalledContent): ResourceVersion? {
        val provider = content.provider ?: return null
        val projectId = content.projectId ?: return null
        val platform = platforms.platform(provider)
        if (!platform.isAvailable || !platform.supports(content.type)) return null
        val project = ResourceProject(
            provider = provider,
            id = projectId,
            slug = projectId,
            name = content.name,
            summary = "",
            author = "",
            type = content.type,
            downloads = 0,
            iconUrl = null,
            websiteUrl = null,
            categories = emptyList(),
        )
        return platform.versions(project, instance.minecraftVersionId, instance.modLoader)
            .firstOrNull()
            ?.takeIf { it.id != content.versionId }
    }

    suspend fun update(
        instance: GameInstance,
        content: InstalledContent,
        version: ResourceVersion,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): ResourceInstallSummary {
        val provider = requireNotNull(content.provider) { "Local files do not have an update source." }
        val projectId = requireNotNull(content.projectId) { "Local files do not have an update source." }
        val project = ResourceProject(
            provider = provider,
            id = projectId,
            slug = projectId,
            name = content.name,
            summary = "",
            author = "",
            type = content.type,
            downloads = 0,
            iconUrl = null,
            websiteUrl = null,
            categories = emptyList(),
        )
        return install(instance, project, version, onProgress = onProgress)
    }

    suspend fun installLocal(
        instance: GameInstance,
        fileName: String,
        bytes: ByteArray,
        type: ResourceType,
    ) {
        require(instance.installationState is net.blockhost.trestle.domain.InstallationState.Installed) {
            "Install the instance before adding local content."
        }
        require(type != ResourceType.MODPACK) { "Modpacks create new instances." }
        require(bytes.isNotEmpty()) { "The selected file is empty." }
        require(bytes.size <= MAX_LOCAL_RESOURCE_BYTES) { "Local content files must be smaller than 512 MiB." }
        val safeName = safeFileName(fileName)
        val extension = safeName.substringAfterLast('.', "").lowercase()
        require(extension in type.localExtensions()) {
            "${type.label} files must use ${type.localExtensions().joinToString { ".$it" }}."
        }
        val folder = type.installFolder()
        val destination = instance.instanceDirectory.toPath() / "game" / folder / safeName
        require(!fileSystem.exists(destination)) {
            "$safeName already exists in ${instance.displayName}."
        }
        try {
            fileSystem.createDirectories(requireNotNull(destination.parent))
            val temporary = destination.parent!! / ".${destination.name}.tmp"
            fileSystem.write(temporary) {
                write(bytes)
                flush()
            }
            fileSystem.atomicMove(temporary, destination)
        } catch (error: Exception) {
            throw LauncherException.FileSystem("$safeName could not be added to the instance.", error)
        }
    }

    suspend fun install(
        instance: GameInstance,
        project: ResourceProject,
        version: ResourceVersion,
        optionalDependencies: Set<String> = emptySet(),
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): ResourceInstallSummary {
        require(project.provider == version.provider && project.id == version.projectId) {
            "The selected resource and version do not match."
        }
        if (project.type == ResourceType.MODPACK) {
            throw LauncherException.InvalidMetadata("Modpacks create new instances and cannot be added to an existing instance.")
        }
        val root = instance.instanceDirectory.toPath()
        val resolved = resolveDependencies(project, version, instance, optionalDependencies)
        val plannedFiles = buildList {
            resolved.forEach { resolvedVersion ->
                val file = downloadableFile(resolvedVersion.version)
                val url = requireNotNull(file.url)
                val type = if (resolvedVersion.isRoot) project.type else ResourceType.MOD
                val folder = type.installFolder()
                add(
                    PlannedResourceFile(
                        owner = resolvedVersion,
                        file = file,
                        url = url,
                        relativePath = "$folder/${file.fileName}",
                        destination = root / "game" / folder / file.fileName,
                    ),
                )
            }
        }
        rejectPathCollisions(plannedFiles)

        val rootKey = resourceKey(project.provider, project.id)
        val manifestPath = root / ".trestle" / "resources.json"
        val currentManifest = readManifest(manifestPath)
        rejectInstalledVersionConflicts(currentManifest, resolved, rootKey)
        val requests = plannedFiles.map {
            DownloadRequest(
                url = it.url,
                destination = it.destination,
                sha1 = it.file.sha1,
                size = it.file.size,
                progressLabel = it.file.fileName,
                sha512 = it.file.sha512,
            )
        }
        downloadPipeline.download(
            requests = requests,
            stagingDirectory = root / ".trestle" / "resource-staging" / version.id,
            onProgress = onProgress,
        )

        val installedEntries = resolved.map { resolvedVersion ->
            val ownedFiles = plannedFiles.filter { it.owner == resolvedVersion }.map { it.relativePath }
            InstalledResource(
                provider = resolvedVersion.version.provider,
                projectId = resolvedVersion.version.projectId,
                versionId = resolvedVersion.version.id,
                type = if (resolvedVersion.isRoot) project.type else ResourceType.MOD,
                name = resolvedVersion.version.name,
                files = ownedFiles,
                direct = resolvedVersion.isRoot,
                requiredBy = if (resolvedVersion.isRoot) emptyList() else listOf(rootKey),
            )
        }
        val nextManifest = mergeManifest(currentManifest, installedEntries, rootKey)
        removeReplacedFiles(currentManifest, nextManifest, root)
        writeManifest(manifestPath, nextManifest)
        return ResourceInstallSummary(
            installedFiles = plannedFiles.size,
            dependencyCount = resolved.count { !it.isRoot },
        )
    }

    private suspend fun downloadableFile(version: ResourceVersion): ResourceFile {
        val file = version.primaryFile
            ?: throw LauncherException.InvalidMetadata("${version.name} has no downloadable file.")
        if (file.url != null) return file
        val sha1 = file.sha1
        if (sha1 != null) {
            val modrinthVersion = platforms.platform(ResourceProvider.MODRINTH).versionBySha1(sha1)
            val alternative = modrinthVersion?.primaryFile
            if (alternative?.url != null && alternative.sha1.equals(sha1, ignoreCase = true)) return alternative
        }
        throw LauncherException.InvalidMetadata(
            "${version.name} blocks downloads from third-party launchers and has no verified Modrinth alternative.",
        )
    }

    suspend fun uninstall(instance: GameInstance, provider: ResourceProvider, projectId: String): Boolean {
        val root = instance.instanceDirectory.toPath()
        val manifestPath = root / ".trestle" / "resources.json"
        val manifest = readManifest(manifestPath)
        val key = resourceKey(provider, projectId)
        val target = manifest.resources.firstOrNull { resourceKey(it.provider, it.projectId) == key } ?: return false
        if (!target.direct) return false
        val retained = manifest.resources.mapNotNull { resource ->
            val resourceKey = resourceKey(resource.provider, resource.projectId)
            val updated = resource.copy(
                direct = if (resourceKey == key) false else resource.direct,
                requiredBy = resource.requiredBy.filterNot { it == key },
            )
            updated.takeIf { it.direct || it.requiredBy.isNotEmpty() }
        }
        val nextManifest = ResourceManifest(resources = retained)
        removeReplacedFiles(manifest, nextManifest, root)
        writeManifest(manifestPath, nextManifest)
        return true
    }

    suspend fun uninstall(instance: GameInstance, content: InstalledContent): Boolean {
        val provider = content.provider
        val projectId = content.projectId
        if (provider != null && projectId != null) return uninstall(instance, provider, projectId)
        if (!content.key.startsWith("local:")) return false
        val relativePath = content.key.removePrefix("local:")
        val path = safeOwnedPath(instance.instanceDirectory.toPath(), relativePath) ?: return false
        fileSystem.delete(path, mustExist = false)
        return true
    }

    suspend fun setEnabled(instance: GameInstance, content: InstalledContent, enabled: Boolean): Boolean {
        if (!content.canManage || content.enabled == enabled) return false
        val root = instance.instanceDirectory.toPath()
        if (!content.isTracked) {
            if (!content.key.startsWith("local:")) return false
            val relativePath = content.key.removePrefix("local:")
            val source = safeOwnedPath(root, relativePath) ?: return false
            val destination = if (enabled) {
                source.parent!! / source.name.removeSuffix(DISABLED_SUFFIX)
            } else {
                source.parent!! / "${source.name}$DISABLED_SUFFIX"
            }
            if (!fileSystem.exists(source) || fileSystem.exists(destination)) return false
            fileSystem.atomicMove(source, destination)
            return true
        }

        val manifestPath = root / ".trestle" / "resources.json"
        val manifest = readManifest(manifestPath)
        val provider = requireNotNull(content.provider)
        val projectId = requireNotNull(content.projectId)
        val key = resourceKey(provider, projectId)
        val resource = manifest.resources.firstOrNull { resourceKey(it.provider, it.projectId) == key }
            ?: return false
        if (!resource.direct || resource.enabled == enabled) return false
        val renamedFiles = resource.files.map { relativePath ->
            val source = safeOwnedPath(root, relativePath)
                ?: throw LauncherException.FileSystem("The installed resource contains an unsafe file path.")
            val destination = if (enabled) {
                source.parent!! / source.name.removeSuffix(DISABLED_SUFFIX)
            } else {
                source.parent!! / "${source.name}$DISABLED_SUFFIX"
            }
            if (fileSystem.exists(source)) {
                require(!fileSystem.exists(destination)) { "${destination.name} already exists." }
                fileSystem.atomicMove(source, destination)
            }
            relativePath.substringBeforeLast('/', "")
                .takeIf(String::isNotEmpty)
                ?.let { "$it/${destination.name}" }
                ?: destination.name
        }
        val next = manifest.copy(
            resources = manifest.resources.map {
                if (resourceKey(it.provider, it.projectId) == key) {
                    it.copy(files = renamedFiles, enabled = enabled)
                } else {
                    it
                }
            },
        )
        writeManifest(manifestPath, next)
        return true
    }

    private fun mergeManifest(
        current: ResourceManifest,
        installed: List<InstalledResource>,
        rootKey: String,
    ): ResourceManifest {
        val resources = current.resources.associateByTo(linkedMapOf()) { resourceKey(it.provider, it.projectId) }
        resources.entries.toList().forEach { (key, resource) ->
            val withoutRoot = resource.copy(requiredBy = resource.requiredBy.filterNot { it == rootKey })
            if (!withoutRoot.direct && withoutRoot.requiredBy.isEmpty()) resources.remove(key)
            else resources[key] = withoutRoot
        }
        installed.forEach { incoming ->
            val key = resourceKey(incoming.provider, incoming.projectId)
            val existing = resources[key]
            resources[key] = incoming.copy(
                direct = incoming.direct || existing?.direct == true,
                requiredBy = (incoming.requiredBy + existing.orEmptyRequiredBy()).distinct().sorted(),
            )
        }
        return ResourceManifest(resources = resources.values.sortedBy { resourceKey(it.provider, it.projectId) })
    }

    private fun rejectInstalledVersionConflicts(
        manifest: ResourceManifest,
        resolved: List<ResolvedResourceVersion>,
        rootKey: String,
    ) {
        val installed = manifest.resources.associateBy { resourceKey(it.provider, it.projectId) }
        resolved.forEach { candidate ->
            val key = resourceKey(candidate.version.provider, candidate.version.projectId)
            val current = installed[key] ?: return@forEach
            val otherOwners = current.requiredBy.filterNot { it == rootKey }
            if (current.versionId != candidate.version.id && (otherOwners.isNotEmpty() || (!candidate.isRoot && current.direct))) {
                throw LauncherException.InvalidMetadata(
                    "${candidate.version.name} conflicts with version ${current.versionId}, which another installed resource uses.",
                )
            }
        }
    }

    private suspend fun resolveDependencies(
        project: ResourceProject,
        rootVersion: ResourceVersion,
        instance: GameInstance,
        optionalDependencies: Set<String>,
    ): List<ResolvedResourceVersion> {
        val platform = platforms.platform(project.provider)
        val queue = ArrayDeque<ResolvedResourceVersion>()
        val resolved = linkedMapOf<String, ResolvedResourceVersion>()
        queue.add(ResolvedResourceVersion(rootVersion, isRoot = true))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentKey = resourceKey(current.version.provider, current.version.projectId)
            val existing = resolved[currentKey]
            if (existing != null) {
                if (existing.version.id != current.version.id) {
                    throw LauncherException.InvalidMetadata(
                        "Required dependencies select conflicting versions of project ${current.version.projectId}.",
                    )
                }
                continue
            }
            resolved[currentKey] = current
            if (resolved.size > MAX_RESOLVED_RESOURCES) {
                throw LauncherException.InvalidMetadata("The dependency graph contains more than $MAX_RESOLVED_RESOURCES resources.")
            }
            current.version.dependencies
                .filter {
                    it.kind == DependencyKind.REQUIRED ||
                        (it.kind == DependencyKind.OPTIONAL && it.selectionKey in optionalDependencies)
                }
                .forEach { dependency ->
                    val dependencyVersion = when {
                        dependency.versionId != null -> platform.version(
                            dependency.projectId.orEmpty(),
                            dependency.versionId,
                        )
                        dependency.projectId != null -> {
                            val dependencyProject = ResourceProject(
                                provider = project.provider,
                                id = dependency.projectId,
                                slug = dependency.projectId,
                                name = dependency.projectId,
                                summary = "",
                                author = "",
                                type = ResourceType.MOD,
                                downloads = 0,
                                iconUrl = null,
                                websiteUrl = null,
                                categories = emptyList(),
                            )
                            val versions = platform.versions(
                                dependencyProject,
                                instance.minecraftVersionId,
                                instance.modLoader,
                            )
                            versions.firstOrNull { it.channel == ReleaseChannel.RELEASE }
                                ?: versions.firstOrNull()
                                ?: throw LauncherException.InvalidMetadata(
                                    "No compatible version exists for required dependency ${dependency.projectId}.",
                                )
                        }
                        else -> throw LauncherException.InvalidMetadata(
                            "${current.version.name} has an external required dependency that cannot be resolved automatically.",
                        )
                    }
                    queue.add(ResolvedResourceVersion(dependencyVersion, isRoot = false))
                }
        }
        return resolved.values.toList()
    }

    private fun rejectPathCollisions(files: List<PlannedResourceFile>) {
        files.groupBy { it.destination }.values.forEach { matches ->
            val distinctHashes = matches.map { it.file.sha1 ?: it.file.sha512 ?: "${it.file.size}:${it.url}" }.distinct()
            if (distinctHashes.size > 1) {
                throw LauncherException.InvalidMetadata(
                    "Multiple resources install different files named ${matches.first().file.fileName}.",
                )
            }
        }
    }

    private fun readManifest(path: Path): ResourceManifest {
        if (!fileSystem.exists(path)) return ResourceManifest()
        return try {
            installJson.decodeFromString(fileSystem.read(path) { readUtf8() })
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The installed resource registry could not be read.", error)
        }
    }

    private fun writeManifest(path: Path, manifest: ResourceManifest) {
        try {
            fileSystem.createDirectories(requireNotNull(path.parent))
            val temporary = path.parent!! / ".${path.name}.tmp"
            fileSystem.write(temporary) {
                writeUtf8(installJson.encodeToString(ResourceManifest.serializer(), manifest))
                flush()
            }
            fileSystem.atomicMove(temporary, path)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.FileSystem("The installed resource registry could not be saved.", error)
        }
    }

    private fun removeReplacedFiles(previous: ResourceManifest, next: ResourceManifest, instanceRoot: Path) {
        val nextFiles = next.resources.flatMapTo(mutableSetOf()) { it.files }
        previous.resources.flatMap { it.files }.filterNot { it in nextFiles }.forEach { relativePath ->
            safeOwnedPath(instanceRoot, relativePath)?.let { fileSystem.delete(it, mustExist = false) }
        }
    }

    private companion object {
        const val MAX_RESOLVED_RESOURCES = 128
        const val MAX_LOCAL_RESOURCE_BYTES = 512 * 1024 * 1024
    }
}

private data class ResolvedResourceVersion(
    val version: ResourceVersion,
    val isRoot: Boolean,
)

private data class PlannedResourceFile(
    val owner: ResolvedResourceVersion,
    val file: ResourceFile,
    val url: String,
    val relativePath: String,
    val destination: Path,
)

@Serializable
private data class ResourceManifest(
    val schemaVersion: Int = 1,
    val resources: List<InstalledResource> = emptyList(),
)

@Serializable
private data class InstalledResource(
    val provider: ResourceProvider,
    val projectId: String,
    val versionId: String,
    val type: ResourceType,
    val name: String,
    val files: List<String>,
    val direct: Boolean = true,
    val requiredBy: List<String> = emptyList(),
    val enabled: Boolean = true,
)

private fun InstalledResource?.orEmptyRequiredBy(): List<String> = this?.requiredBy.orEmpty()

private fun ResourceType.installFolder(): String = when (this) {
    ResourceType.MOD -> "mods"
    ResourceType.RESOURCE_PACK -> "resourcepacks"
    ResourceType.SHADER_PACK -> "shaderpacks"
    ResourceType.MODPACK -> throw LauncherException.InvalidMetadata("Modpacks create new instances.")
}

private fun ResourceType.localExtensions(): Set<String> = when (this) {
    ResourceType.MOD -> setOf("jar")
    ResourceType.RESOURCE_PACK,
    ResourceType.SHADER_PACK,
    -> setOf("zip")
    ResourceType.MODPACK -> setOf("mrpack", "zip")
}

private fun resourceKey(provider: ResourceProvider, projectId: String) = "${provider.name}:$projectId"

private fun safeOwnedPath(instanceRoot: Path, relativePath: String): Path? {
    val segments = relativePath.replace('\\', '/').split('/').filter(String::isNotBlank)
    if (segments.isEmpty() || segments.any { it == "." || it == ".." }) return null
    var path = instanceRoot / "game"
    segments.forEach { path /= it }
    return path
}

private val MANAGED_RESOURCE_TYPES = listOf(
    ResourceType.MOD,
    ResourceType.RESOURCE_PACK,
    ResourceType.SHADER_PACK,
)

private const val DISABLED_SUFFIX = ".disabled"

private val installJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}
