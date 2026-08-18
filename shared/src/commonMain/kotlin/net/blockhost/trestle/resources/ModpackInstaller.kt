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
import net.blockhost.trestle.metadata.ForgeMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.NeoForgeMetadataClient
import net.blockhost.trestle.metadata.QuiltMetadataClient
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
    private val forgeMetadataClient: ForgeMetadataClient,
    private val quiltMetadataClient: QuiltMetadataClient,
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
        if (version.externalPack != null) {
            val resolvedVersion = if (version.externalPack.isUnresolved()) {
                platforms.platform(version.provider).version(project.id, version.id)
            } else {
                version
            }
            val plan = resolvedVersion.externalPack
                ?: throw LauncherException.InvalidMetadata("${version.name} has no installation plan.")
            return installExternal(project, resolvedVersion, plan, onProgress)
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

    private suspend fun installExternal(
        project: ResourceProject,
        version: ResourceVersion,
        external: ExternalModpackPlan,
        onProgress: suspend (DownloadProgress) -> Unit,
    ): GameInstance {
        if (external.isUnresolved()) {
            throw LauncherException.InvalidMetadata("${project.name} does not provide downloadable client files.")
        }
        val staging = directories.staging / "modpacks" / project.provider.name.lowercase() / project.id / version.id
        resetDirectory(staging)
        val resolvedFiles = staging / "resolved-files"
        fileSystem.createDirectories(resolvedFiles)

        external.archiveUrl?.let { url ->
            val archive = staging / "pack.zip"
            downloadPipeline.download(
                listOf(DownloadRequest(url, archive, progressLabel = "Downloading ${project.name}")),
                staging / "archive-download",
                onProgress,
            )
            val extracted = staging / "archive"
            archiveExtractor.extract(archive, extracted)
            val content = listOf(extracted / ".minecraft", extracted / "minecraft")
                .firstOrNull(fileSystem::exists) ?: extracted
            copyDirectory(content, resolvedFiles)
        }

        if (external.componentArchives.isNotEmpty()) {
            val downloads = staging / "components"
            val componentRequests = external.componentArchives.mapIndexed { index, archive ->
                DownloadRequest(
                    url = archive.url,
                    destination = downloads / "$index.zip",
                    md5 = archive.md5,
                    sha1 = archive.sha1,
                    size = archive.size,
                    progressLabel = archive.name,
                )
            }
            downloadPipeline.download(componentRequests, staging / "component-downloads", onProgress)
            componentRequests.forEachIndexed { index, request ->
                val archive = external.componentArchives[index]
                val extracted = staging / "component-$index"
                archiveExtractor.extract(request.destination, extracted)
                val source = archive.sourceDirectory
                    .takeIf(String::isNotBlank)
                    ?.let { safePackDestination(extracted, it) }
                    ?: extracted
                val destination = archive.destination
                    .takeIf(String::isNotBlank)
                    ?.let { safePackDestination(resolvedFiles, it) }
                    ?: resolvedFiles
                copyDirectory(source, destination)
            }
        }

        val requests = external.files.map { file ->
            DownloadRequest(
                url = file.url,
                destination = safePackDestination(resolvedFiles, file.path),
                md5 = file.md5,
                sha1 = file.sha1,
                size = file.size,
                progressLabel = file.path.substringAfterLast('/'),
                sha512 = file.sha512,
            )
        }
        if (requests.isNotEmpty()) {
            downloadPipeline.download(requests, staging / "file-downloads", onProgress)
        }

        val detectedLoader = detectExternalLoader(resolvedFiles, external.minecraftVersion)
        val plan = ModpackPlan(
            name = project.name,
            minecraftVersion = external.minecraftVersion,
            loader = detectedLoader?.first ?: external.loader,
            loaderVersion = detectedLoader?.second ?: external.loaderVersion,
            files = emptyList(),
            overrideDirectories = emptyList(),
        )
        ensureRuntimeSupported(plan)
        val minecraftMetadata = metadataClient.resolveVersion(plan.minecraftVersion)
        val instance = repository.create(
            CreateInstanceRequest(
                displayName = project.name,
                minecraftVersionId = plan.minecraftVersion,
                modLoader = plan.loader,
                loaderVersion = plan.loaderVersion,
                requiredJavaMajor = minecraftMetadata.javaVersion?.majorVersion ?: 8,
                memory = LaunchTuningAdvisor.recommendMemory(plan.loader, systemProfile),
                iconReference = project.iconUrl,
            ),
        )
        try {
            val installed = minecraftInstaller.install(instance, onProgress)
            copyDirectory(resolvedFiles, installed.instanceDirectory.toPath() / "game")
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

    private fun detectExternalLoader(root: Path, minecraftVersion: String): Pair<ModLoader, String>? {
        val profile = listOf(root / "pack.json", root / "bin" / "version.json", root / "version.json")
            .firstOrNull(fileSystem::exists) ?: return null
        val libraries = runCatching { decode<ExternalPackProfile>(profile, "modpack loader profile").libraries }.getOrNull()
            ?: return null
        for (library in libraries) {
            val coordinate = library.name
            val artifact = coordinate.substringBefore(':') + ":" + coordinate.substringAfter(':').substringBefore(':')
            val rawVersion = coordinate.split(':').getOrNull(2).orEmpty()
            val detected = when (artifact) {
                "net.neoforged:neoforge", "net.neoforged:fancymodloader" -> ModLoader.NEOFORGE to rawVersion
                "net.fabricmc:fabric-loader" -> ModLoader.FABRIC to rawVersion
                "org.quiltmc:quilt-loader" -> ModLoader.QUILT to rawVersion
                "net.minecraftforge:forge" -> ModLoader.FORGE to rawVersion.removePrefix("$minecraftVersion-")
                "net.minecraftforge:minecraftforge" -> ModLoader.FORGE to rawVersion
                else -> null
            }
            if (detected != null) return detected
        }
        return null
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

    suspend fun importFtbAppInstances(rootValue: String): List<GameInstance> {
        val root = rootValue.toPath()
        require(fileSystem.metadataOrNull(root)?.isDirectory == true) { "The FTB App instances folder does not exist." }
        val candidates = buildList {
            if (fileSystem.exists(root / "instance.json")) add(root)
            fileSystem.list(root).filterTo(this) { child ->
                fileSystem.metadataOrNull(child)?.isDirectory == true && fileSystem.exists(child / "instance.json")
            }
        }
        require(candidates.isNotEmpty()) { "No FTB App instances were found in that folder." }
        return candidates.map { source -> importFtbAppInstance(source) }
    }

    private suspend fun importFtbAppInstance(source: Path): GameInstance {
        val metadata = decode<FtbAppInstance>(source / "instance.json", "FTB App instance")
        val loaderName = metadata.modLoader.substringBefore('-', "").lowercase()
        val loader = when (loaderName) {
            "fabric" -> ModLoader.FABRIC
            "neoforge" -> ModLoader.NEOFORGE
            "forge" -> ModLoader.FORGE
            "quilt" -> ModLoader.QUILT
            else -> ModLoader.VANILLA
        }
        val loaderVersion = metadata.modLoader.substringAfter('-', "").ifBlank { null }
        val minecraftMetadata = metadataClient.resolveVersion(metadata.mcVersion)
        val icon = (source / "folder.jpg").takeIf(fileSystem::exists)?.toString()
        val instance = repository.create(
            CreateInstanceRequest(
                displayName = metadata.name,
                minecraftVersionId = metadata.mcVersion,
                modLoader = loader,
                loaderVersion = loaderVersion,
                requiredJavaMajor = minecraftMetadata.javaVersion?.majorVersion ?: 8,
                memory = LaunchTuningAdvisor.recommendMemory(loader, systemProfile),
                iconReference = icon,
            ),
        )
        try {
            val installed = minecraftInstaller.install(instance)
            val gameDirectory = installed.instanceDirectory.toPath() / "game"
            copyDirectory(source, gameDirectory)
            runCatching { fileSystem.delete(gameDirectory / "instance.json", mustExist = false) }
            runCatching { deleteTree(gameDirectory / ".ftbapp") }
            return repository.update(installed.copy(playTimeMillis = metadata.totalPlayTime.coerceAtLeast(0)))
        } catch (error: Exception) {
            runCatching { repository.delete(instance.id) }
            runCatching { deleteTree(instance.instanceDirectory.toPath()) }
            throw error
        }
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
            fileSystem.exists(extracted / "mmc-pack.json") -> readPrismPlan(extracted)
            else -> throw LauncherException.InvalidMetadata(
                "The archive is not a supported Modrinth, CurseForge, Prism Launcher, or MultiMC pack.",
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
                group = "Modpacks",
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
            val restored = if (fileSystem.exists(extracted / "trestle-instance.json")) {
                val exported = decode<GameInstance>(extracted / "trestle-instance.json", "Trestle instance settings")
                repository.update(
                    installed.copy(
                        displayName = exported.displayName,
                        jvmArguments = exported.jvmArguments,
                        memory = exported.memory,
                        gameArguments = exported.gameArguments,
                        javaExecutable = exported.javaExecutable,
                        environmentVariables = exported.environmentVariables,
                        iconReference = exported.iconReference,
                        pinned = exported.pinned,
                        group = exported.group,
                    ),
                )
            } else {
                installed
            }
            runCatching { deleteTree(staging) }
            return restored
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
        val PRISM_LOADER_COMPONENTS = setOf(
            "net.fabricmc.fabric-loader",
            "net.neoforged",
            "net.minecraftforge",
            "org.quiltmc.quilt-loader",
        )
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

    private fun readPrismPlan(extracted: Path): ModpackPlan {
        val pack = decode<PrismPackManifest>(extracted / "mmc-pack.json", "Prism Launcher component manifest")
        val minecraftVersion = pack.components.firstOrNull { it.uid == "net.minecraft" }?.version
            ?: throw LauncherException.InvalidMetadata("The Prism pack does not declare a Minecraft version.")
        val loaderComponent = pack.components.firstOrNull { it.uid in PRISM_LOADER_COMPONENTS }
        val loader = when (loaderComponent?.uid) {
            "net.fabricmc.fabric-loader" -> ModLoader.FABRIC
            "net.neoforged" -> ModLoader.NEOFORGE
            "net.minecraftforge" -> ModLoader.FORGE
            "org.quiltmc.quilt-loader" -> ModLoader.QUILT
            null -> ModLoader.VANILLA
            else -> throw LauncherException.InvalidMetadata(
                "The Prism pack uses unsupported component ${loaderComponent.uid}.",
            )
        }
        val name = if (fileSystem.exists(extracted / "instance.cfg")) {
            fileSystem.read(extracted / "instance.cfg") { readUtf8() }
                .lineSequence()
                .firstOrNull { it.startsWith("name=") }
                ?.substringAfter('=')
                ?.trim()
                .orEmpty()
        } else {
            ""
        }
        val gameDirectory = listOf(".minecraft", "minecraft").firstOrNull {
            fileSystem.exists(extracted / it)
        } ?: throw LauncherException.InvalidMetadata("The Prism pack does not contain a game directory.")
        return ModpackPlan(
            name = name,
            minecraftVersion = minecraftVersion,
            loader = loader,
            loaderVersion = loaderComponent?.version,
            files = emptyList(),
            overrideDirectories = listOf(gameDirectory),
        )
    }

    private suspend fun ensureRuntimeSupported(plan: ModpackPlan) {
        when (plan.loader) {
            ModLoader.VANILLA -> Unit
            ModLoader.FABRIC -> {
                val requested = plan.loaderVersion ?: return
                val available = fabricMetadataClient.loaderVersions(plan.minecraftVersion)
                if (available.none { it.version == requested }) {
                    throw LauncherException.InvalidMetadata(
                        "Fabric Loader $requested does not support Minecraft ${plan.minecraftVersion}.",
                    )
                }
            }
            ModLoader.NEOFORGE -> {
                val requested = plan.loaderVersion ?: return
                val available = neoForgeMetadataClient.loaderVersions(plan.minecraftVersion)
                if (available.none { it.version == requested }) {
                    throw LauncherException.InvalidMetadata(
                        "NeoForge $requested does not support Minecraft ${plan.minecraftVersion}.",
                    )
                }
            }
            ModLoader.FORGE -> {
                val requested = plan.loaderVersion ?: return
                val available = forgeMetadataClient.loaderVersions(plan.minecraftVersion)
                if (available.none { it.version == requested }) {
                    throw LauncherException.InvalidMetadata(
                        "Forge $requested does not support Minecraft ${plan.minecraftVersion}.",
                    )
                }
            }
            ModLoader.QUILT -> {
                val requested = plan.loaderVersion ?: return
                val available = quiltMetadataClient.loaderVersions(plan.minecraftVersion)
                if (available.none { it.version == requested }) {
                    throw LauncherException.InvalidMetadata(
                        "Quilt Loader $requested does not support Minecraft ${plan.minecraftVersion}.",
                    )
                }
            }
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

private fun ExternalModpackPlan.isUnresolved(): Boolean =
    files.isEmpty() && archiveUrl == null && componentArchives.isEmpty()

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

@Serializable
private data class PrismPackManifest(
    val components: List<PrismPackComponent>,
)

@Serializable
private data class PrismPackComponent(
    val uid: String,
    val version: String,
)

@Serializable
private data class FtbAppInstance(
    val name: String,
    val mcVersion: String,
    val modLoader: String = "",
    val totalPlayTime: Long = 0,
)

@Serializable
private data class ExternalPackProfile(
    val libraries: List<ExternalPackLibrary> = emptyList(),
)

@Serializable
private data class ExternalPackLibrary(val name: String)

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
