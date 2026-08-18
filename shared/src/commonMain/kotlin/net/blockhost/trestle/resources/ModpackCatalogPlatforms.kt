package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader

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
