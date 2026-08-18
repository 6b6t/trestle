package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader

class AtLauncherResourcePlatform(
    private val httpClient: HttpClient,
    private val userAgent: String,
    private val apiBaseUrl: String = "https://api.atlauncher.com/v1",
    private val downloadBaseUrl: String = "https://download.nodecdn.net/containers/atl",
) : ResourcePlatform {
    override val provider = ResourceProvider.ATLAUNCHER
    override val isAvailable = true
    private var cachedCatalog: List<AtLauncherPack>? = null

    override fun supports(type: ResourceType) = type == ResourceType.MODPACK

    override suspend fun search(request: ResourceSearchRequest): ResourceSearchResult {
        require(supports(request.type)) { "ATLauncher only provides modpacks." }
        val packs = catalog()
            .filter { pack ->
                request.query.isBlank() || listOf(pack.name, pack.description)
                    .any { value -> value.contains(request.query, ignoreCase = true) }
            }
            .filter { pack ->
                request.gameVersion == null || pack.versions.any { it.minecraft == request.gameVersion }
            }
            .let { values ->
                when (request.sort) {
                    ResourceSearchSort.UPDATED,
                    ResourceSearchSort.NEWEST,
                    -> values.sortedByDescending(AtLauncherPack::latestPublished)
                    ResourceSearchSort.RELEVANCE,
                    ResourceSearchSort.FEATURED,
                    ResourceSearchSort.DOWNLOADS,
                    -> values
                }
            }
        return ResourceSearchResult(
            projects = packs.drop(request.offset).take(request.limit).map { it.toProject() },
            offset = request.offset,
            total = packs.size,
        )
    }

    override suspend fun details(project: ResourceProject): ResourceProject =
        catalog().firstOrNull { it.safeName == project.id }?.toProject() ?: project

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion> = pack(project.id).versions
        .filter { gameVersion == null || it.minecraft == gameVersion }
        .sortedByDescending(AtLauncherVersion::published)
        .map { it.toSummaryVersion(project.id) }

    override suspend fun version(projectId: String, versionId: String): ResourceVersion {
        val manifestUrl = packVersionUrl(projectId, versionId, "Configs.json")
        val response = httpClient.get(manifestUrl) { header(HttpHeaders.UserAgent, userAgent) }
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("ATLauncher returned HTTP ${response.status.value} for the selected version.")
        }
        val manifest = decode<AtLauncherManifest>(response.bodyAsText(), "ATLauncher pack manifest")
        val blocked = manifest.mods.filter { mod ->
            mod.client && (!mod.optional || mod.selected || mod.recommended) && mod.download == "browser"
        }
        if (blocked.isNotEmpty()) {
            val names = blocked.take(3).joinToString { it.name }
            throw LauncherException.InvalidMetadata(
                "This pack needs manual downloads for $names. Trestle cannot install it automatically yet.",
            )
        }
        if (manifest.libraries.isNotEmpty()) {
            throw LauncherException.InvalidMetadata(
                "This ATLauncher pack uses custom launch libraries that Trestle does not support yet.",
            )
        }

        val selectedMods = manifest.mods.filter { it.client && (!it.optional || it.selected || it.recommended) }
        val unsupported = selectedMods.filterNot { it.isInstallable() }
        if (unsupported.isNotEmpty()) {
            val names = unsupported.take(3).joinToString { it.name.ifBlank { it.file } }
            throw LauncherException.InvalidMetadata(
                "This ATLauncher pack uses unsupported install rules for $names.",
            )
        }
        val forgeMod = selectedMods.firstOrNull {
            it.type == "forge" || (it.type == "jar" && it.name.equals("Minecraft Forge", ignoreCase = true))
        }
        val loader = manifest.loader?.type.toModLoader().takeUnless { it == ModLoader.VANILLA }
            ?: forgeMod?.let { ModLoader.FORGE }
            ?: ModLoader.VANILLA
        val loaderVersion = manifest.loader?.resolvedVersion() ?: forgeMod?.version?.ifBlank { null }
        val files = selectedMods
            .filterNot { it.download == "browser" || it.type in extractedModTypes || it == forgeMod }
            .mapNotNull { mod ->
                val directory = mod.installDirectory(manifest.minecraft) ?: return@mapNotNull null
                ExternalPackFile(
                    path = listOf(directory, mod.file).filter(String::isNotBlank).joinToString("/"),
                    url = mod.downloadUrl(),
                    md5 = mod.md5?.takeIf(String::isNotBlank),
                    size = mod.filesize,
                )
            }
        val archives = buildList {
            if (!manifest.noConfigs && manifest.configs != null) {
                add(
                    ExternalPackArchive(
                        name = "Pack configuration",
                        url = packVersionUrl(projectId, versionId, "Configs.zip"),
                        size = manifest.configs.filesize,
                        sha1 = manifest.configs.sha1?.takeIf(String::isNotBlank),
                    ),
                )
            }
            selectedMods
                .filter { it.type in extractedModTypes && it.download != "browser" }
                .mapTo(this) { mod ->
                    ExternalPackArchive(
                        name = mod.name,
                        url = mod.downloadUrl(),
                        size = mod.filesize,
                        md5 = mod.md5?.takeIf(String::isNotBlank),
                        destination = mod.extractionDirectory(),
                        sourceDirectory = mod.extractFolder.orEmpty().replace("%s%", "/").trim('/'),
                    )
                }
        }
        return ResourceVersion(
            provider = provider,
            id = manifest.version,
            projectId = projectId,
            name = manifest.version,
            versionNumber = manifest.version,
            gameVersions = listOf(manifest.minecraft),
            loaders = listOf(loader.apiName()),
            channel = ReleaseChannel.RELEASE,
            publishedAt = "",
            files = emptyList(),
            dependencies = emptyList(),
            externalPack = ExternalModpackPlan(
                minecraftVersion = manifest.minecraft,
                loader = loader,
                loaderVersion = loaderVersion,
                files = files,
                componentArchives = archives,
            ),
        )
    }

    private suspend fun catalog(): List<AtLauncherPack> {
        cachedCatalog?.let { return it }
        return getApi<List<AtLauncherPack>>("$apiBaseUrl/packs/full/public")
            .also { cachedCatalog = it }
    }

    private suspend fun pack(id: String): AtLauncherPack =
        cachedCatalog?.firstOrNull { it.safeName == id }
            ?: getApi("$apiBaseUrl/pack/$id")

    private suspend inline fun <reified T> getApi(url: String): T {
        val response = httpClient.get(url) { header(HttpHeaders.UserAgent, userAgent) }
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("ATLauncher returned HTTP ${response.status.value}.")
        }
        val envelope = decode<AtLauncherResponse<T>>(response.bodyAsText(), "ATLauncher API")
        if (envelope.error || envelope.data == null) {
            throw LauncherException.Network(envelope.message ?: "ATLauncher did not return catalog data.")
        }
        return envelope.data
    }

    private inline fun <reified T> decode(value: String, label: String): T = try {
        catalogJson.decodeFromString(value)
    } catch (error: Exception) {
        throw LauncherException.InvalidMetadata("$label returned invalid data.", error)
    }

    private fun packVersionUrl(projectId: String, versionId: String, file: String) =
        "${downloadBaseUrl.trimEnd('/')}/packs/$projectId/versions/$versionId/$file"

    private fun AtLauncherPack.toProject() = ResourceProject(
        provider = provider,
        id = safeName,
        slug = safeName.lowercase(),
        name = name,
        summary = description.substringBefore('\n').take(240),
        author = "ATLauncher",
        type = ResourceType.MODPACK,
        downloads = 0,
        iconUrl = "${downloadBaseUrl.trimEnd('/')}/launcher/images/${name.filter(Char::isLetterOrDigit).lowercase()}.png",
        websiteUrl = websiteURL?.takeIf(String::isNotBlank),
        categories = versions.map(AtLauncherVersion::minecraft).filter(String::isNotBlank).distinct(),
        issuesUrl = supportURL?.takeIf(String::isNotBlank),
        description = description,
    )

    private fun AtLauncherVersion.toSummaryVersion(projectId: String) = ResourceVersion(
        provider = provider,
        id = version,
        projectId = projectId,
        name = version,
        versionNumber = version,
        gameVersions = listOf(minecraft).filter(String::isNotBlank),
        loaders = emptyList(),
        channel = ReleaseChannel.RELEASE,
        publishedAt = published.toString(),
        files = emptyList(),
        dependencies = emptyList(),
        externalPack = ExternalModpackPlan(minecraft, ModLoader.VANILLA),
    )

    private fun AtLauncherMod.downloadUrl(): String = when (download) {
        "server" -> "${downloadBaseUrl.trimEnd('/')}/${url.trimStart('/')}"
        "direct" -> url
        else -> throw LauncherException.InvalidMetadata("${name.ifBlank { file }} has an unsupported download type.")
    }

    private fun AtLauncherMod.installDirectory(minecraft: String): String? = when (type) {
        "root" -> ""
        "mods" -> "mods"
        "flan" -> "Flan"
        "dependency", "depandency" -> "mods/$minecraft"
        "ic2lib" -> "mods/ic2"
        "denlib" -> "mods/denlib"
        "coremods" -> "coremods"
        "plugins" -> "plugins"
        "texturepack" -> "texturepacks"
        "resourcepack" -> "resourcepacks"
        "shaderpack" -> "shaderpacks"
        else -> null
    }

    private fun AtLauncherMod.isInstallable(): Boolean = when {
        url.isBlank() || file.isBlank() -> false
        download !in setOf("server", "direct", "browser") -> false
        type in supportedDirectModTypes || type in extractedModTypes || type == "forge" -> true
        type == "jar" && name.equals("Minecraft Forge", ignoreCase = true) -> true
        else -> false
    }

    private fun AtLauncherMod.extractionDirectory(): String = when (type) {
        "texturepackextract" -> "texturepacks/extracted"
        "resourcepackextract" -> "resourcepacks/extracted"
        else -> extractTo.installDirectoryPath()
    }

    private fun String?.installDirectoryPath(): String = when (this) {
        null, "", "root" -> ""
        "mods" -> "mods"
        "flan" -> "Flan"
        "dependency", "depandency" -> "mods"
        "ic2lib" -> "mods/ic2"
        "denlib" -> "mods/denlib"
        "coremods" -> "coremods"
        "plugins" -> "plugins"
        "texturepack" -> "texturepacks"
        "resourcepack" -> "resourcepacks"
        "shaderpack" -> "shaderpacks"
        else -> ""
    }

    private fun String?.toModLoader(): ModLoader = when (this?.lowercase()) {
        "fabric" -> ModLoader.FABRIC
        "neoforge" -> ModLoader.NEOFORGE
        "forge" -> ModLoader.FORGE
        "quilt" -> ModLoader.QUILT
        else -> ModLoader.VANILLA
    }

    private fun ModLoader.apiName(): String = when (this) {
        ModLoader.VANILLA -> "vanilla"
        ModLoader.FABRIC -> "fabric"
        ModLoader.NEOFORGE -> "neoforge"
        ModLoader.FORGE -> "forge"
        ModLoader.QUILT -> "quilt"
    }

    private companion object {
        val extractedModTypes = setOf("extract", "texturepackextract", "resourcepackextract")
        val supportedDirectModTypes = setOf(
            "root",
            "mods",
            "flan",
            "dependency",
            "depandency",
            "ic2lib",
            "denlib",
            "coremods",
            "plugins",
            "texturepack",
            "resourcepack",
            "shaderpack",
        )
    }
}

class FtbResourcePlatform(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.feed-the-beast.com/v1/modpacks/public",
) : ResourcePlatform {
    override val provider = ResourceProvider.FTB
    override val isAvailable = true
    private var cachedCatalog: List<FtbPack>? = null

    override fun supports(type: ResourceType) = type == ResourceType.MODPACK

    override suspend fun search(request: ResourceSearchRequest): ResourceSearchResult {
        require(supports(request.type)) { "FTB only provides modpacks." }
        val packs = catalog()
            .filter { pack ->
                request.query.isBlank() || listOf(pack.name, pack.synopsis, pack.description)
                    .any { it.contains(request.query, ignoreCase = true) }
            }
            .filter { pack -> request.gameVersion == null || pack.gameVersions().contains(request.gameVersion) }
            .filter { pack -> request.loader == null || pack.loaders().contains(request.loader) }
            .filter { pack -> request.category == null || pack.tags.any { it.name.equals(request.category, true) } }
            .let { values ->
                when (request.sort) {
                    ResourceSearchSort.DOWNLOADS -> values.sortedByDescending(FtbPack::installs)
                    ResourceSearchSort.UPDATED, ResourceSearchSort.NEWEST -> values.sortedByDescending(FtbPack::updated)
                    ResourceSearchSort.FEATURED -> values.sortedWith(compareByDescending<FtbPack> { it.featured }.thenByDescending { it.installs })
                    ResourceSearchSort.RELEVANCE -> values
                }
            }
        return ResourceSearchResult(
            projects = packs.drop(request.offset).take(request.limit).map(FtbPack::toProject),
            offset = request.offset,
            total = packs.size,
        )
    }

    override suspend fun details(project: ResourceProject): ResourceProject = pack(project.id).toProject()

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion> = pack(project.id).versions
        .filter { version -> gameVersion == null || version.minecraftVersion() == gameVersion }
        .filter { version -> loader == null || version.loader() == loader }
        .sortedByDescending(FtbVersion::released)
        .map { it.toResourceVersion(project.id) }

    override suspend fun version(projectId: String, versionId: String): ResourceVersion {
        val version = get<FtbVersion>("$baseUrl/modpack/$projectId/$versionId")
        return version.toResourceVersion(projectId, includeFiles = true)
    }

    private suspend fun catalog(): List<FtbPack> {
        cachedCatalog?.let { return it }
        val index = get<FtbCatalog>("$baseUrl/modpack/all")
        return coroutineScope {
            index.packs.map { id -> async { runCatching { get<FtbPack>("$baseUrl/modpack/$id") }.getOrNull() } }.awaitAll()
        }.filterNotNull().also { cachedCatalog = it }
    }

    private suspend fun pack(id: String): FtbPack =
        cachedCatalog?.firstOrNull { it.id.toString() == id } ?: get("$baseUrl/modpack/$id")

    private suspend inline fun <reified T> get(url: String): T {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("FTB returned HTTP ${response.status.value}.")
        }
        return try {
            catalogJson.decodeFromString(response.bodyAsText())
        } catch (error: Exception) {
            throw LauncherException.InvalidMetadata("FTB returned invalid catalog data.", error)
        }
    }
}

class TechnicResourcePlatform(
    private val httpClient: HttpClient,
    private val clientId: String = "",
    private val baseUrl: String = "https://api.technicpack.net",
) : ResourcePlatform {
    override val provider = ResourceProvider.TECHNIC
    override val isAvailable = true

    override fun supports(type: ResourceType) = type == ResourceType.MODPACK

    override suspend fun search(request: ResourceSearchRequest): ResourceSearchResult {
        require(supports(request.type)) { "Technic only provides modpacks." }
        val endpoint = if (request.query.isBlank()) "$baseUrl/trending" else "$baseUrl/search"
        val response = httpClient.get(endpoint) {
            parameter("build", "multimc")
            if (request.query.isNotBlank()) parameter("q", request.query)
            if (clientId.isNotBlank()) parameter("cid", clientId)
        }
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("Technic returned HTTP ${response.status.value}.")
        }
        val entries = decode<TechnicSearchResponse>(response.bodyAsText(), "Technic search").modpacks
        val projects = entries.drop(request.offset).take(request.limit).map(TechnicSearchPack::toProject)
        return ResourceSearchResult(projects, request.offset, entries.size)
    }

    override suspend fun details(project: ResourceProject): ResourceProject = pack(project.slug).toProject()

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion> {
        val pack = pack(project.slug)
        val versions = pack.solder?.let { solderVersions(pack, it) }
            ?: listOf(pack.toVersion())
        return versions
            .filter { gameVersion == null || gameVersion in it.gameVersions }
            .filter { loader == null || loader.apiName() in it.loaders }
    }

    override suspend fun version(projectId: String, versionId: String): ResourceVersion {
        val pack = pack(projectId)
        if (pack.solder == null) return pack.toVersion()
        val manifest = getSolder<TechnicSolderBuild>(pack.solder, "modpack/${pack.name}/$versionId")
        return manifest.toVersion(pack, versionId)
    }

    private suspend fun pack(slug: String): TechnicPack {
        val response = httpClient.get("$baseUrl/modpack/$slug") {
            parameter("build", "multimc")
            if (clientId.isNotBlank()) parameter("cid", clientId)
        }
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("Technic returned HTTP ${response.status.value}.")
        }
        return decode(response.bodyAsText(), "Technic modpack")
    }

    private suspend fun solderVersions(pack: TechnicPack, solder: String): List<ResourceVersion> {
        val manifest = getSolder<TechnicSolderPack>(solder, "modpack/${pack.name}")
        return manifest.builds.asReversed().map { build ->
            ResourceVersion(
                provider = provider,
                id = build,
                projectId = pack.name,
                name = build,
                versionNumber = build,
                gameVersions = listOf(pack.minecraft),
                loaders = emptyList(),
                channel = if (build == manifest.recommended) ReleaseChannel.RELEASE else ReleaseChannel.UNKNOWN,
                publishedAt = "",
                files = emptyList(),
                dependencies = emptyList(),
                externalPack = ExternalModpackPlan(pack.minecraft, ModLoader.VANILLA),
            )
        }
    }

    private suspend inline fun <reified T> getSolder(base: String, path: String): T {
        val response = httpClient.get("${base.trimEnd('/')}/$path")
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("Technic Solder returned HTTP ${response.status.value}.")
        }
        return decode(response.bodyAsText(), "Technic Solder")
    }

    private inline fun <reified T> decode(value: String, label: String): T = try {
        catalogJson.decodeFromString(value)
    } catch (error: Exception) {
        throw LauncherException.InvalidMetadata("$label returned invalid catalog data.", error)
    }
}

class LegacyFtbResourcePlatform(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://dist.creeper.host/FTB2",
) : ResourcePlatform {
    override val provider = ResourceProvider.FTB_LEGACY
    override val isAvailable = true
    private var cachedCatalog: List<LegacyFtbPack>? = null

    override fun supports(type: ResourceType) = type == ResourceType.MODPACK

    override suspend fun search(request: ResourceSearchRequest): ResourceSearchResult {
        require(supports(request.type)) { "FTB Legacy only provides modpacks." }
        val values = catalog()
            .filter { request.query.isBlank() || listOf(it.name, it.description, it.author).any { value -> value.contains(request.query, true) } }
            .filter { request.gameVersion == null || it.minecraftVersion == request.gameVersion }
            .let { packs ->
                when (request.sort) {
                    ResourceSearchSort.DOWNLOADS,
                    ResourceSearchSort.FEATURED,
                    ResourceSearchSort.RELEVANCE,
                    -> packs
                    ResourceSearchSort.UPDATED,
                    ResourceSearchSort.NEWEST,
                    -> packs.sortedByDescending(LegacyFtbPack::version)
                }
            }
        return ResourceSearchResult(
            projects = values.drop(request.offset).take(request.limit).map { it.toProject() },
            offset = request.offset,
            total = values.size,
        )
    }

    override suspend fun details(project: ResourceProject): ResourceProject =
        catalog().firstOrNull { it.directory == project.id }?.toProject() ?: project

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion> {
        val pack = catalog().firstOrNull { it.directory == project.id }
            ?: throw LauncherException.InvalidMetadata("The FTB Legacy pack no longer exists.")
        if (gameVersion != null && pack.minecraftVersion != gameVersion) return emptyList()
        if (loader != null && loader != ModLoader.FORGE) return emptyList()
        return pack.versions().map { pack.toVersion(it) }
    }

    override suspend fun version(projectId: String, versionId: String): ResourceVersion {
        val pack = catalog().firstOrNull { it.directory == projectId }
            ?: throw LauncherException.InvalidMetadata("The FTB Legacy pack no longer exists.")
        require(versionId in pack.versions()) { "The selected FTB Legacy version no longer exists." }
        return pack.toVersion(versionId)
    }

    private suspend fun catalog(): List<LegacyFtbPack> {
        cachedCatalog?.let { return it }
        return buildList {
            addAll(fetch("$baseUrl/static/modpacks.xml"))
            addAll(fetch("$baseUrl/static/thirdparty.xml"))
        }.distinctBy(LegacyFtbPack::directory).also { cachedCatalog = it }
    }

    private suspend fun fetch(url: String): List<LegacyFtbPack> {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("FTB Legacy returned HTTP ${response.status.value}.")
        }
        return legacyModpackTag.findAll(response.bodyAsText()).mapNotNull { match ->
            val attributes = legacyAttribute.findAll(match.groupValues[1])
                .associate { it.groupValues[1] to decodeXml(it.groupValues[2]) }
            val directory = attributes["dir"].orEmpty()
            val name = attributes["name"].orEmpty()
            val minecraft = attributes["mcVersion"].orEmpty()
            val file = attributes["url"].orEmpty()
            if (directory.isBlank() || name.isBlank() || minecraft.isBlank() || file.isBlank()) return@mapNotNull null
            LegacyFtbPack(
                name = name,
                author = attributes["author"].orEmpty(),
                description = attributes["description"].orEmpty(),
                directory = directory,
                file = file,
                minecraftVersion = minecraft,
                version = attributes["version"].orEmpty(),
                oldVersions = attributes["oldVersions"].orEmpty().split(';').filter(String::isNotBlank),
                logo = attributes["logo"].orEmpty(),
            )
        }.toList()
    }

    private fun LegacyFtbPack.toProject() = ResourceProject(
        provider = provider,
        id = directory,
        slug = directory.lowercase(),
        name = name,
        summary = description.substringBefore('\n').take(240),
        author = author,
        type = ResourceType.MODPACK,
        downloads = 0,
        iconUrl = logo.takeIf(String::isNotBlank)?.let { "$baseUrl/static/$it" },
        websiteUrl = null,
        categories = listOf("Legacy", minecraftVersion),
        description = description,
    )

    private fun LegacyFtbPack.toVersion(selectedVersion: String) = ResourceVersion(
        provider = provider,
        id = selectedVersion,
        projectId = directory,
        name = selectedVersion,
        versionNumber = selectedVersion,
        gameVersions = listOf(minecraftVersion),
        loaders = listOf("forge"),
        channel = if (selectedVersion == version) ReleaseChannel.RELEASE else ReleaseChannel.UNKNOWN,
        publishedAt = "",
        files = emptyList(),
        dependencies = emptyList(),
        externalPack = ExternalModpackPlan(
            minecraftVersion = minecraftVersion,
            loader = ModLoader.FORGE,
            archiveUrl = "$baseUrl/modpacks/$directory/${selectedVersion.replace('.', '_')}/$file",
        ),
    )
}

@Serializable
private data class AtLauncherResponse<T>(
    val error: Boolean = false,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
private data class AtLauncherPack(
    val name: String,
    val safeName: String,
    val description: String = "",
    val supportURL: String? = null,
    val websiteURL: String? = null,
    val versions: List<AtLauncherVersion> = emptyList(),
) {
    fun latestPublished(): Long = versions.maxOfOrNull(AtLauncherVersion::published) ?: 0
}

@Serializable
private data class AtLauncherVersion(
    val version: String,
    val minecraft: String = "",
    val published: Long = 0,
)

@Serializable
private data class AtLauncherManifest(
    val version: String,
    val minecraft: String,
    val loader: AtLauncherLoader? = null,
    val libraries: List<AtLauncherLibrary> = emptyList(),
    val mods: List<AtLauncherMod> = emptyList(),
    val noConfigs: Boolean = false,
    val configs: AtLauncherConfigs? = null,
)

@Serializable
private data class AtLauncherLoader(
    val type: String = "",
    val metadata: AtLauncherLoaderMetadata = AtLauncherLoaderMetadata(),
) {
    fun resolvedVersion(): String? = when (type.lowercase()) {
        "fabric", "quilt" -> metadata.loader.ifBlank { metadata.version }.ifBlank { null }
        "forge", "neoforge" -> metadata.version.ifBlank { null }
        else -> null
    }
}

@Serializable
private data class AtLauncherLoaderMetadata(
    val version: String = "",
    val loader: String = "",
)

@Serializable
private data class AtLauncherLibrary(val file: String = "")

@Serializable
private data class AtLauncherConfigs(
    val filesize: Long? = null,
    val sha1: String? = null,
)

@Serializable
private data class AtLauncherMod(
    val name: String = "",
    val version: String = "",
    val url: String,
    val file: String,
    val download: String,
    val type: String,
    val md5: String? = null,
    val filesize: Long? = null,
    val extractTo: String? = null,
    val extractFolder: String? = null,
    val optional: Boolean = false,
    val recommended: Boolean = false,
    val selected: Boolean = false,
    val client: Boolean = true,
)

@Serializable
private data class FtbCatalog(val packs: List<Int> = emptyList())

@Serializable
private data class FtbPack(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String = "",
    val synopsis: String = "",
    val featured: Boolean = false,
    val installs: Long = 0,
    val plays: Long = 0,
    val updated: Long = 0,
    val tags: List<FtbNamedValue> = emptyList(),
    val links: List<FtbLink> = emptyList(),
    val art: List<FtbArt> = emptyList(),
    val authors: List<FtbAuthor> = emptyList(),
    val versions: List<FtbVersion> = emptyList(),
) {
    fun gameVersions() = versions.mapNotNull(FtbVersion::minecraftVersion).toSet()
    fun loaders() = versions.map(FtbVersion::loader).toSet()

    fun toProject() = ResourceProject(
        provider = ResourceProvider.FTB,
        id = id.toString(),
        slug = slug,
        name = name,
        summary = synopsis,
        author = authors.joinToString { it.name }.ifBlank { "FTB Team" },
        type = ResourceType.MODPACK,
        downloads = installs,
        iconUrl = art.firstOrNull { it.type == "square" }?.url,
        websiteUrl = "https://www.feed-the-beast.com/modpacks/$id-$slug",
        categories = tags.map(FtbNamedValue::name),
        featuredImageUrl = art.firstOrNull { it.type == "splash" }?.url,
        updatedAt = null,
        description = description,
        galleryUrls = art.filter { it.type == "screenshot" }.map(FtbArt::url),
        issuesUrl = links.firstOrNull { it.name.contains("issue", true) }?.link,
        wikiUrl = links.firstOrNull { it.name.contains("wiki", true) }?.link,
    )
}

@Serializable
private data class FtbNamedValue(val name: String)

@Serializable
private data class FtbLink(val name: String = "", val link: String = "")

@Serializable
private data class FtbArt(val url: String, val type: String = "")

@Serializable
private data class FtbAuthor(val name: String)

@Serializable
private data class FtbVersion(
    val id: Int,
    val name: String,
    val type: String = "release",
    val released: Long = 0,
    val targets: List<FtbTarget> = emptyList(),
    val files: List<FtbFile> = emptyList(),
) {
    fun minecraftVersion() = targets.firstOrNull { it.type == "game" || it.name == "minecraft" }?.version
    fun loaderTarget() = targets.firstOrNull { it.type == "modloader" }
    fun loader() = loaderTarget()?.name?.toLoader() ?: ModLoader.VANILLA

    fun toResourceVersion(projectId: String, includeFiles: Boolean = false): ResourceVersion {
        val minecraft = minecraftVersion()
            ?: throw LauncherException.InvalidMetadata("FTB version $name does not declare Minecraft.")
        val loaderTarget = loaderTarget()
        val planFiles = if (includeFiles) files
            .filterNot { it.serveronly || it.optional }
            .map { file ->
                ExternalPackFile(
                    path = listOf(file.path.removePrefix("./"), file.name).filter(String::isNotBlank).joinToString("/"),
                    url = file.url,
                    sha1 = file.hashes["sha1"] ?: file.sha1,
                    sha512 = file.hashes["sha512"],
                    size = file.size,
                )
            } else emptyList()
        return ResourceVersion(
            provider = ResourceProvider.FTB,
            id = id.toString(),
            projectId = projectId,
            name = name,
            versionNumber = name,
            gameVersions = listOf(minecraft),
            loaders = listOfNotNull(loaderTarget?.name),
            channel = when (type.lowercase()) {
                "release" -> ReleaseChannel.RELEASE
                "beta" -> ReleaseChannel.BETA
                "alpha" -> ReleaseChannel.ALPHA
                else -> ReleaseChannel.UNKNOWN
            },
            publishedAt = "",
            files = emptyList(),
            dependencies = emptyList(),
            externalPack = ExternalModpackPlan(
                minecraftVersion = minecraft,
                loader = loader(),
                loaderVersion = loaderTarget?.version,
                files = planFiles,
            ),
        )
    }
}

@Serializable
private data class FtbTarget(val name: String, val version: String, val type: String = "")

@Serializable
private data class FtbFile(
    val path: String = "",
    val name: String,
    val url: String,
    val sha1: String? = null,
    val hashes: Map<String, String> = emptyMap(),
    val size: Long? = null,
    val serveronly: Boolean = false,
    val optional: Boolean = false,
)

@Serializable
private data class TechnicSearchResponse(val modpacks: List<TechnicSearchPack> = emptyList())

@Serializable
private data class TechnicSearchPack(
    val id: String,
    val name: String,
    val slug: String,
    val url: String? = null,
    val iconUrl: String? = null,
) {
    fun toProject() = ResourceProject(
        provider = ResourceProvider.TECHNIC,
        id = slug,
        slug = slug,
        name = name,
        summary = "",
        author = "Technic",
        type = ResourceType.MODPACK,
        downloads = 0,
        iconUrl = iconUrl,
        websiteUrl = url,
        categories = emptyList(),
    )
}

@Serializable
private data class TechnicPack(
    val id: Long,
    val name: String,
    val displayName: String = name,
    val user: String = "",
    val url: String? = null,
    val platformUrl: String? = null,
    val minecraft: String,
    val installs: Long = 0,
    val runs: Long = 0,
    val description: String = "",
    val tags: String = "",
    val version: String,
    val icon: TechnicImage? = null,
    val logo: TechnicImage? = null,
    val background: TechnicImage? = null,
    val solder: String? = null,
) {
    fun toProject() = ResourceProject(
        provider = ResourceProvider.TECHNIC,
        id = name,
        slug = name,
        name = displayName,
        summary = description,
        author = user,
        type = ResourceType.MODPACK,
        downloads = installs,
        iconUrl = icon?.url,
        websiteUrl = platformUrl,
        categories = tags.split(',').map(String::trim).filter(String::isNotBlank),
        featuredImageUrl = background?.url,
        description = description,
        galleryUrls = listOfNotNull(logo?.url, background?.url),
    )

    fun toVersion() = ResourceVersion(
        provider = ResourceProvider.TECHNIC,
        id = version,
        projectId = name,
        name = version,
        versionNumber = version,
        gameVersions = listOf(minecraft),
        loaders = emptyList(),
        channel = ReleaseChannel.RELEASE,
        publishedAt = "",
        files = emptyList(),
        dependencies = emptyList(),
        externalPack = ExternalModpackPlan(minecraft, tags.toLoader(), archiveUrl = url),
    )
}

@Serializable
private data class TechnicImage(val url: String)

@Serializable
private data class TechnicSolderPack(
    val recommended: String,
    val latest: String,
    val builds: List<String> = emptyList(),
)

@Serializable
private data class TechnicSolderBuild(
    val minecraft: String,
    val forge: String? = null,
    val mods: List<TechnicSolderMod> = emptyList(),
) {
    fun toVersion(pack: TechnicPack, version: String): ResourceVersion {
        val loader = if (forge.isNullOrBlank()) pack.tags.toLoader() else ModLoader.FORGE
        return ResourceVersion(
            provider = ResourceProvider.TECHNIC,
            id = version,
            projectId = pack.name,
            name = version,
            versionNumber = version,
            gameVersions = listOf(minecraft),
            loaders = listOf(loader.apiName()),
            channel = ReleaseChannel.RELEASE,
            publishedAt = "",
            files = emptyList(),
            dependencies = emptyList(),
            externalPack = ExternalModpackPlan(
                minecraftVersion = minecraft,
                loader = loader,
                loaderVersion = forge,
                componentArchives = mods.map { ExternalPackArchive(it.name, it.url, it.filesize) },
            ),
        )
    }
}

@Serializable
private data class TechnicSolderMod(
    val name: String,
    val version: String = "",
    val url: String,
    val filesize: Long? = null,
)

private data class LegacyFtbPack(
    val name: String,
    val author: String,
    val description: String,
    val directory: String,
    val file: String,
    val minecraftVersion: String,
    val version: String,
    val oldVersions: List<String>,
    val logo: String,
) {
    fun versions(): List<String> = (listOf(version) + oldVersions).filter(String::isNotBlank).distinct()
}

private fun String.toLoader(): ModLoader = when {
    contains("neoforge", true) -> ModLoader.NEOFORGE
    contains("fabric", true) -> ModLoader.FABRIC
    contains("quilt", true) -> ModLoader.QUILT
    contains("forge", true) -> ModLoader.FORGE
    else -> ModLoader.VANILLA
}

private fun ModLoader.apiName(): String = when (this) {
    ModLoader.VANILLA -> "vanilla"
    ModLoader.FABRIC -> "fabric"
    ModLoader.NEOFORGE -> "neoforge"
    ModLoader.FORGE -> "forge"
    ModLoader.QUILT -> "quilt"
}

private val catalogJson = Json { ignoreUnknownKeys = true }
private val legacyModpackTag = Regex("<modpack\\s+([^>]*?)/?>", RegexOption.IGNORE_CASE)
private val legacyAttribute = Regex("([A-Za-z][A-Za-z0-9]*)=\"([^\"]*)\"")

private fun decodeXml(value: String): String = value
    .replace("&lt;br&gt;", "\n", ignoreCase = true)
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
