package net.blockhost.trestle.resources

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.download.DownloadRequest
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.install.MinecraftInstaller
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.instance.InstanceRepository
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.NeoForgeMetadataClient
import net.blockhost.trestle.runtime.LaunchTuningAdvisor
import net.blockhost.trestle.runtime.SystemProfile
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class ModpackInstaller(
    private val platforms: ResourcePlatformRegistry,
    private val repository: InstanceRepository,
    private val metadataClient: MinecraftMetadataClient,
    private val fabricMetadataClient: FabricMetadataClient,
    private val neoForgeMetadataClient: NeoForgeMetadataClient,
    private val minecraftInstaller: MinecraftInstaller,
    private val downloadPipeline: DownloadPipeline,
    private val fileSystem: FileSystem,
    private val directories: LauncherDirectories,
    private val archiveExtractor: ArchiveExtractor,
    private val systemProfile: SystemProfile,
) {
    suspend fun install(
        project: ResourceProject,
        version: ResourceVersion,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): GameInstance {
        require(project.type == ResourceType.MODPACK) { "The selected project is not a modpack." }
        require(project.provider == version.provider && project.id == version.projectId) {
            "The selected modpack and version do not match."
        }
        val archiveFile = version.primaryFile
            ?: throw LauncherException.InvalidMetadata("${version.name} has no modpack archive.")
        val archiveUrl = archiveFile.url ?: throw LauncherException.InvalidMetadata(
            "${version.name} blocks downloads from third-party launchers.",
        )
        val staging = directories.staging / "modpacks" / project.provider.name.lowercase() / project.id / version.id
        resetDirectory(staging)
        val archive = staging / safeFileName(archiveFile.fileName)
        downloadPipeline.download(
            requests = listOf(
                DownloadRequest(
                    url = archiveUrl,
                    destination = archive,
                    sha1 = archiveFile.sha1,
                    size = archiveFile.size,
                    progressLabel = "Downloading ${project.name}",
                    sha512 = archiveFile.sha512,
                ),
            ),
            stagingDirectory = staging / "archive-download",
            onProgress = onProgress,
        )
        return installArchive(archive, staging, project.name, project.iconUrl, onProgress)
    }

    suspend fun installLocal(
        fileName: String,
        bytes: ByteArray,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): GameInstance {
        require(bytes.isNotEmpty()) { "The selected modpack is empty." }
        require(bytes.size <= MAX_LOCAL_MODPACK_BYTES) { "Local modpacks must be smaller than 1 GiB." }
        val safeName = safeFileName(fileName)
        require(safeName.substringAfterLast('.', "").lowercase() in setOf("mrpack", "zip")) {
            "Choose a .mrpack or .zip modpack archive."
        }
        val importId = bytes.contentHashCode().toUInt().toString(16)
        val staging = directories.staging / "modpacks" / "local" / importId
        resetDirectory(staging)
        val archive = staging / safeName
        fileSystem.write(archive) {
            write(bytes)
            flush()
        }
        return installArchive(archive, staging, safeName.substringBeforeLast('.'), null, onProgress)
    }

    private suspend fun installArchive(
        archive: Path,
        staging: Path,
        fallbackName: String,
        iconReference: String?,
        onProgress: suspend (DownloadProgress) -> Unit,
    ): GameInstance {
        val extracted = staging / "extracted"
        archiveExtractor.extract(archive, extracted)
        val plan = when {
            fileSystem.exists(extracted / "modrinth.index.json") -> readModrinthPlan(extracted)
            fileSystem.exists(extracted / "manifest.json") -> readCurseForgePlan(extracted)
            else -> throw LauncherException.InvalidMetadata(
                "The archive is not a supported Modrinth or CurseForge modpack.",
            )
        }
        ensureRuntimeSupported(plan)

        val resolvedFiles = staging / "resolved-files"
        val packRequests = plan.files.map { file ->
            DownloadRequest(
                url = file.url,
                destination = safePackDestination(resolvedFiles, file.path),
                sha1 = file.sha1,
                size = file.size,
                progressLabel = file.path.substringAfterLast('/'),
                sha512 = file.sha512,
            )
        }
        downloadPipeline.download(
            requests = packRequests,
            stagingDirectory = staging / "file-downloads",
            onProgress = onProgress,
        )

        val minecraftMetadata = metadataClient.resolveVersion(plan.minecraftVersion)
        val instance = repository.create(
            CreateInstanceRequest(
                displayName = plan.name.ifBlank { fallbackName },
                minecraftVersionId = plan.minecraftVersion,
                modLoader = plan.loader,
                loaderVersion = plan.loaderVersion,
                requiredJavaMajor = minecraftMetadata.javaVersion?.majorVersion ?: 8,
                memory = LaunchTuningAdvisor.recommendMemory(plan.loader, systemProfile),
                iconReference = iconReference,
            ),
        )
        try {
            val installed = minecraftInstaller.install(instance, onProgress)
            val gameDirectory = installed.instanceDirectory.toPath() / "game"
            copyDirectory(resolvedFiles, gameDirectory)
            plan.overrideDirectories.forEach { directory ->
                val source = extracted / directory
                if (fileSystem.exists(source)) copyDirectory(source, gameDirectory)
            }
            runCatching { deleteTree(staging) }
            return installed
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                runCatching { repository.delete(instance.id) }
                runCatching { deleteTree(instance.instanceDirectory.toPath()) }
            }
            throw error
        } catch (error: Exception) {
            repository.update(
                instance.copy(
                    installationState = InstallationState.Failed(
                        error.message ?: "The modpack could not be installed.",
                    ),
                ),
            )
            throw error
        }
    }

    private companion object {
        const val MAX_LOCAL_MODPACK_BYTES = 1024 * 1024 * 1024
    }

    private fun readModrinthPlan(extracted: Path): ModpackPlan {
        val index = decode<ModrinthPackIndex>(extracted / "modrinth.index.json", "Modrinth modpack index")
        val minecraftVersion = index.dependencies["minecraft"]
            ?: throw LauncherException.InvalidMetadata("The Modrinth pack does not declare a Minecraft version.")
        val loader = when {
            index.dependencies["fabric-loader"] != null -> ModLoader.FABRIC
            index.dependencies["neoforge"] != null -> ModLoader.NEOFORGE
            index.dependencies["forge"] != null -> ModLoader.FORGE
            index.dependencies["quilt-loader"] != null -> ModLoader.QUILT
            else -> ModLoader.VANILLA
        }
        val loaderVersion = when (loader) {
            ModLoader.FABRIC -> index.dependencies["fabric-loader"]
            ModLoader.NEOFORGE -> index.dependencies["neoforge"]
            ModLoader.FORGE -> index.dependencies["forge"]
            ModLoader.QUILT -> index.dependencies["quilt-loader"]
            ModLoader.VANILLA -> null
        }
        return ModpackPlan(
            name = index.name,
            minecraftVersion = minecraftVersion,
            loader = loader,
            loaderVersion = loaderVersion,
            files = index.files
                .filter { it.environment?.client !in setOf("unsupported", "optional") }
                .map { file ->
                    val url = file.downloads.firstOrNull()
                        ?: throw LauncherException.InvalidMetadata("${file.path} has no download source.")
                    PackFile(
                        path = safeRelativePath(file.path),
                        url = url,
                        sha1 = file.hashes["sha1"],
                        size = file.fileSize,
                        sha512 = file.hashes["sha512"],
                    )
                },
            overrideDirectories = listOf("overrides", "client-overrides"),
        )
    }

    private suspend fun readCurseForgePlan(extracted: Path): ModpackPlan {
        val manifest = decode<CurseForgePackManifest>(extracted / "manifest.json", "CurseForge modpack manifest")
        val primaryLoader = manifest.minecraft.modLoaders.firstOrNull { it.primary }
            ?: manifest.minecraft.modLoaders.firstOrNull()
        val loaderId = primaryLoader?.id.orEmpty()
        val normalizedLoaderId = loaderId.lowercase()
        val loader = when {
            normalizedLoaderId.startsWith("fabric-") -> ModLoader.FABRIC
            normalizedLoaderId.startsWith("neoforge-") -> ModLoader.NEOFORGE
            normalizedLoaderId.startsWith("forge-") -> ModLoader.FORGE
            normalizedLoaderId.startsWith("quilt-") -> ModLoader.QUILT
            loaderId.isBlank() -> ModLoader.VANILLA
            else -> throw LauncherException.InvalidMetadata("The modpack uses unsupported loader $loaderId.")
        }
        val loaderVersion = loaderId.substringAfter('-', missingDelimiterValue = "").ifBlank { null }
        val platform = platforms.platform(ResourceProvider.CURSEFORGE)
        val includedFiles = manifest.files.filter { it.required }
        val referencedProjects = platform.projectsByIds(includedFiles.map { it.projectId.toString() })
        val resolvedVersions = platform.versionsByIds(
            includedFiles.map { it.projectId.toString() to it.fileId.toString() },
        )
        val files = includedFiles.zip(resolvedVersions).map { (reference, curseForgeVersion) ->
            val resourceType = referencedProjects[reference.projectId.toString()]?.type ?: ResourceType.MOD
            var file = curseForgeVersion.primaryFile
                ?: throw LauncherException.InvalidMetadata(
                    "CurseForge file ${reference.fileId} has no downloadable artifact.",
                )
            if (file.url == null && file.sha1 != null) {
                val alternative = platforms.platform(ResourceProvider.MODRINTH).versionBySha1(file.sha1)?.primaryFile
                if (alternative?.url != null && alternative.sha1.equals(file.sha1, ignoreCase = true)) file = alternative
            }
            PackFile(
                path = "${resourceType.packInstallFolder()}/${file.fileName}",
                url = file.url ?: throw LauncherException.InvalidMetadata(
                    "${file.fileName} blocks downloads from third-party launchers.",
                ),
                sha1 = file.sha1,
                size = file.size,
                sha512 = file.sha512,
            )
        }
        return ModpackPlan(
            name = manifest.name,
            minecraftVersion = manifest.minecraft.version,
            loader = loader,
            loaderVersion = loaderVersion,
            files = files,
            overrideDirectories = listOf(safeRelativePath(manifest.overrides)),
        )
    }

    private suspend fun ensureRuntimeSupported(plan: ModpackPlan) {
        when (plan.loader) {
            ModLoader.VANILLA -> Unit
            ModLoader.FABRIC -> {
                val requested = requireNotNull(plan.loaderVersion)
                val available = fabricMetadataClient.loaderVersions(plan.minecraftVersion)
                if (available.none { it.version == requested }) {
                    throw LauncherException.InvalidMetadata(
                        "Fabric Loader $requested does not support Minecraft ${plan.minecraftVersion}.",
                    )
                }
            }
            ModLoader.NEOFORGE -> {
                val requested = requireNotNull(plan.loaderVersion)
                val available = neoForgeMetadataClient.loaderVersions(plan.minecraftVersion)
                if (available.none { it.version == requested }) {
                    throw LauncherException.InvalidMetadata(
                        "NeoForge $requested does not support Minecraft ${plan.minecraftVersion}.",
                    )
                }
            }
            else -> throw LauncherException.UnsupportedLoader(plan.loader)
        }
    }

    private inline fun <reified T> decode(path: Path, label: String): T = try {
        packJson.decodeFromString(fileSystem.read(path) { readUtf8() })
    } catch (error: LauncherException) {
        throw error
    } catch (error: Exception) {
        throw LauncherException.InvalidMetadata("The $label is invalid.", error)
    }

    private fun copyDirectory(source: Path, destination: Path) {
        fileSystem.createDirectories(destination)
        fileSystem.list(source).forEach { child ->
            val target = destination / child.name
            val metadata = fileSystem.metadata(child)
            if (metadata.isDirectory) {
                copyDirectory(child, target)
            } else if (metadata.isRegularFile) {
                fileSystem.createDirectories(requireNotNull(target.parent))
                fileSystem.copy(child, target)
            }
        }
    }

    private fun resetDirectory(path: Path) {
        if (fileSystem.exists(path)) deleteTree(path)
        fileSystem.createDirectories(path)
    }

    private fun deleteTree(path: Path) {
        val metadata = fileSystem.metadataOrNull(path) ?: return
        if (metadata.isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

}

private data class ModpackPlan(
    val name: String,
    val minecraftVersion: String,
    val loader: ModLoader,
    val loaderVersion: String?,
    val files: List<PackFile>,
    val overrideDirectories: List<String>,
)

private data class PackFile(
    val path: String,
    val url: String,
    val sha1: String?,
    val size: Long?,
    val sha512: String? = null,
)

@Serializable
private data class ModrinthPackIndex(
    val name: String,
    val files: List<ModrinthPackFile>,
    val dependencies: Map<String, String>,
)

@Serializable
private data class ModrinthPackFile(
    val path: String,
    val hashes: Map<String, String>,
    val downloads: List<String>,
    @SerialName("fileSize") val fileSize: Long? = null,
    val environment: ModrinthPackEnvironment? = null,
)

@Serializable
private data class ModrinthPackEnvironment(
    val client: String = "required",
    val server: String = "required",
)

@Serializable
private data class CurseForgePackManifest(
    val name: String,
    val minecraft: CurseForgePackMinecraft,
    val files: List<CurseForgePackFile>,
    val overrides: String = "overrides",
)

@Serializable
private data class CurseForgePackMinecraft(
    val version: String,
    val modLoaders: List<CurseForgePackLoader> = emptyList(),
)

@Serializable
private data class CurseForgePackLoader(
    val id: String,
    val primary: Boolean = false,
)

@Serializable
private data class CurseForgePackFile(
    @SerialName("projectID") val projectId: Long,
    @SerialName("fileID") val fileId: Long,
    val required: Boolean = true,
)

private fun safeRelativePath(value: String): String {
    val normalized = value.replace('\\', '/').trim('/')
    val segments = normalized.split('/').filter(String::isNotBlank)
    require(segments.isNotEmpty() && segments.none { it == "." || it == ".." }) {
        "The modpack contains an unsafe file path."
    }
    return segments.joinToString("/")
}

private fun safePackDestination(root: Path, relativePath: String): Path {
    var destination = root
    safeRelativePath(relativePath).split('/').forEach { segment -> destination /= segment }
    return destination
}

private fun ResourceType.packInstallFolder(): String = when (this) {
    ResourceType.RESOURCE_PACK -> "resourcepacks"
    ResourceType.SHADER_PACK -> "shaderpacks"
    else -> "mods"
}

private val packJson = Json { ignoreUnknownKeys = true }
