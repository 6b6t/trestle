package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader

enum class ResourceProvider(val label: String) {
    MODRINTH("Modrinth"),
    CURSEFORGE("CurseForge"),
    ATLAUNCHER("ATLauncher"),
    FTB("FTB"),
    FTB_LEGACY("FTB Legacy"),
    TECHNIC("Technic"),
}

enum class ResourceType(
    val label: String,
    internal val modrinthFacet: String?,
    internal val curseForgeClassId: Int?,
) {
    MOD("Mods", "project_type:mod", 6),
    MODPACK("Modpacks", "project_type:modpack", 4471),
    RESOURCE_PACK("Resource packs", "project_type:resourcepack", 12),
    SHADER_PACK("Shaders", "project_type:shader", 6552),
}

enum class ReleaseChannel(val label: String) {
    RELEASE("Release"),
    BETA("Beta"),
    ALPHA("Alpha"),
    UNKNOWN("Unknown"),
}

enum class ResourceSearchSort(val label: String) {
    RELEVANCE("Relevance"),
    FEATURED("Featured"),
    DOWNLOADS("Downloads"),
    UPDATED("Recently updated"),
    NEWEST("Newest"),
}

enum class DependencyKind {
    REQUIRED,
    OPTIONAL,
    INCOMPATIBLE,
    EMBEDDED,
}

enum class ResourceEnvironmentSupport(val label: String) {
    REQUIRED("Required"),
    OPTIONAL("Optional"),
    UNSUPPORTED("Unsupported"),
    UNKNOWN("Unknown"),
}

data class ResourceSearchRequest(
    val query: String = "",
    val type: ResourceType,
    val gameVersion: String? = null,
    val loader: ModLoader? = null,
    val category: String? = null,
    val sort: ResourceSearchSort = ResourceSearchSort.RELEVANCE,
    val offset: Int = 0,
    val limit: Int = 30,
) {
    init {
        require(offset >= 0)
        require(limit in 1..50)
    }
}

data class ResourceSearchResult(
    val projects: List<ResourceProject>,
    val offset: Int,
    val total: Int,
)

data class ResourceProject(
    val provider: ResourceProvider,
    val id: String,
    val slug: String,
    val name: String,
    val summary: String,
    val author: String,
    val type: ResourceType,
    val downloads: Long,
    val iconUrl: String?,
    val websiteUrl: String?,
    val categories: List<String>,
    val featuredImageUrl: String? = null,
    val updatedAt: String? = null,
    val followers: Long? = null,
    val license: String? = null,
    val clientSupport: ResourceEnvironmentSupport? = null,
    val serverSupport: ResourceEnvironmentSupport? = null,
    val sourceUrl: String? = null,
    val issuesUrl: String? = null,
    val wikiUrl: String? = null,
    val description: String? = null,
    val galleryUrls: List<String> = emptyList(),
)

data class ResourceVersion(
    val provider: ResourceProvider,
    val id: String,
    val projectId: String,
    val name: String,
    val versionNumber: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val channel: ReleaseChannel,
    val publishedAt: String,
    val files: List<ResourceFile>,
    val dependencies: List<ResourceDependency>,
    val externalPack: ExternalModpackPlan? = null,
) {
    val primaryFile: ResourceFile?
        get() = files.firstOrNull { it.primary } ?: files.firstOrNull()
}

data class ExternalModpackPlan(
    val minecraftVersion: String,
    val loader: ModLoader,
    val loaderVersion: String? = null,
    val files: List<ExternalPackFile> = emptyList(),
    val archiveUrl: String? = null,
    val componentArchives: List<ExternalPackArchive> = emptyList(),
)

data class ExternalPackArchive(
    val name: String,
    val url: String,
    val size: Long? = null,
    val md5: String? = null,
    val sha1: String? = null,
    val destination: String = "",
    val sourceDirectory: String = "",
)

data class ExternalPackFile(
    val path: String,
    val url: String,
    val md5: String? = null,
    val sha1: String? = null,
    val sha512: String? = null,
    val size: Long? = null,
)

data class ResourceFile(
    val id: String?,
    val fileName: String,
    val url: String?,
    val sha1: String?,
    val size: Long?,
    val primary: Boolean,
    val sha512: String? = null,
)

data class ResourceDependency(
    val projectId: String?,
    val versionId: String?,
    val fileName: String?,
    val kind: DependencyKind,
) {
    val selectionKey: String
        get() = versionId ?: projectId ?: fileName.orEmpty()
}

interface ResourcePlatform {
    val provider: ResourceProvider
    val isAvailable: Boolean

    fun supports(type: ResourceType): Boolean

    suspend fun search(request: ResourceSearchRequest): ResourceSearchResult

    suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion>

    suspend fun version(projectId: String, versionId: String): ResourceVersion

    suspend fun details(project: ResourceProject): ResourceProject = project

    suspend fun versionsByIds(references: List<Pair<String, String>>): List<ResourceVersion> =
        references.map { (projectId, versionId) -> version(projectId, versionId) }

    suspend fun versionBySha1(sha1: String): ResourceVersion? = null

    suspend fun versionByFingerprint(fingerprint: Long, sha1: String): ResourceVersion? = null

    suspend fun projectsByIds(projectIds: List<String>): Map<String, ResourceProject> = emptyMap()
}

class ResourcePlatformRegistry(platforms: List<ResourcePlatform>) {
    private val byProvider = platforms.associateBy(ResourcePlatform::provider)

    fun find(provider: ResourceProvider): ResourcePlatform? = byProvider[provider]

    fun platform(provider: ResourceProvider): ResourcePlatform =
        requireNotNull(byProvider[provider]) { "${provider.label} is not registered." }
}


@Serializable
private data class ModrinthSearchResponse(
    val hits: List<ModrinthSearchHit>,
    val offset: Int,
    @SerialName("total_hits") val totalHits: Int,
)

@Serializable
private data class ModrinthSearchHit(
    @SerialName("project_id") val projectId: String,
    val slug: String,
    val title: String,
    val description: String,
    val author: String,
    val downloads: Long,
    @SerialName("icon_url") val iconUrl: String? = null,
    val categories: List<String> = emptyList(),
    @SerialName("display_categories") val displayCategories: List<String> = emptyList(),
    val follows: Long? = null,
    @SerialName("date_modified") val dateModified: String? = null,
    val license: String? = null,
    @SerialName("client_side") val clientSide: String? = null,
    @SerialName("server_side") val serverSide: String? = null,
    val gallery: List<String> = emptyList(),
    @SerialName("featured_gallery") val featuredGallery: String? = null,
)

@Serializable
private data class ModrinthVersion(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    @SerialName("game_versions") val gameVersions: List<String>,
    val loaders: List<String>,
    @SerialName("version_type") val versionType: String,
    @SerialName("date_published") val datePublished: String,
    val files: List<ModrinthFile>,
    val dependencies: List<ModrinthDependency> = emptyList(),
)

@Serializable
private data class ModrinthFile(
    val hashes: Map<String, String>,
    val url: String,
    val filename: String,
    val primary: Boolean = false,
    val size: Long? = null,
)

@Serializable
private data class ModrinthDependency(
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("dependency_type") val dependencyType: String,
)

@Serializable
private data class ModrinthProjectDetails(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("project_type") val projectType: String = "mod",
    @SerialName("icon_url") val iconUrl: String? = null,
    val categories: List<String> = emptyList(),
    val downloads: Long = 0,
    val body: String = "",
    val gallery: List<ModrinthGalleryImage> = emptyList(),
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
)

@Serializable
private data class ModrinthGalleryImage(val url: String, val featured: Boolean = false)

class ModrinthResourcePlatform(
    private val httpClient: HttpClient,
    private val userAgent: String,
    private val baseUrl: String = "https://api.modrinth.com/v2",
) : ResourcePlatform {
    override val provider = ResourceProvider.MODRINTH
    override val isAvailable = true

    override fun supports(type: ResourceType): Boolean = type.modrinthFacet != null

    override suspend fun search(request: ResourceSearchRequest): ResourceSearchResult {
        val typeFacet = request.type.modrinthFacet
            ?: return ResourceSearchResult(emptyList(), request.offset, 0)
        val facets = buildList {
            add(listOf(typeFacet))
            request.gameVersion?.takeIf(String::isNotBlank)?.let { add(listOf("versions:$it")) }
            if (request.type in loaderFilteredTypes) {
                request.loader?.modrinthName()?.let { add(listOf("categories:$it")) }
            }
            request.category?.takeIf(String::isNotBlank)?.let { add(listOf("categories:$it")) }
        }
        val response: ModrinthSearchResponse = request("$baseUrl/search") {
            parameter("query", request.query.trim())
            parameter("facets", resourceJson.encodeToString(facets))
            parameter(
                "index",
                when (request.sort) {
                    ResourceSearchSort.RELEVANCE -> if (request.query.isBlank()) "downloads" else "relevance"
                    ResourceSearchSort.FEATURED -> "follows"
                    ResourceSearchSort.DOWNLOADS -> "downloads"
                    ResourceSearchSort.UPDATED -> "updated"
                    ResourceSearchSort.NEWEST -> "newest"
                },
            )
            parameter("offset", request.offset)
            parameter("limit", request.limit)
        }
        return ResourceSearchResult(
            projects = response.hits.map { hit ->
                ResourceProject(
                    provider = provider,
                    id = hit.projectId,
                    slug = hit.slug,
                    name = hit.title,
                    summary = hit.description,
                    author = hit.author,
                    type = request.type,
                    downloads = hit.downloads,
                    iconUrl = hit.iconUrl,
                    websiteUrl = "https://modrinth.com/${request.type.modrinthPath()}/${hit.slug}",
                    categories = hit.displayCategories.ifEmpty { hit.categories },
                    featuredImageUrl = hit.featuredGallery ?: hit.gallery.firstOrNull(),
                    updatedAt = hit.dateModified,
                    followers = hit.follows,
                    license = hit.license,
                    clientSupport = hit.clientSide?.toEnvironmentSupport(),
                    serverSupport = hit.serverSide?.toEnvironmentSupport(),
                )
            },
            offset = response.offset,
            total = response.totalHits,
        )
    }

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion> {
        val versions: List<ModrinthVersion> = request("$baseUrl/project/${project.id}/version") {
            gameVersion?.takeIf(String::isNotBlank)?.let {
                parameter("game_versions", resourceJson.encodeToString(listOf(it)))
            }
            if (project.type in loaderFilteredTypes) {
                loader?.modrinthName()?.let {
                    parameter("loaders", resourceJson.encodeToString(listOf(it)))
                }
            }
            parameter("include_changelog", false)
        }
        return versions.map(ModrinthVersion::toResourceVersion)
    }

    override suspend fun details(project: ResourceProject): ResourceProject {
        val details: ModrinthProjectDetails = request("$baseUrl/project/${project.id}") {}
        return project.copy(
            description = details.body,
            featuredImageUrl = project.featuredImageUrl
                ?: details.gallery.firstOrNull { it.featured }?.url
                ?: details.gallery.firstOrNull()?.url,
            galleryUrls = details.gallery.map { it.url },
            issuesUrl = details.issuesUrl ?: project.issuesUrl,
            sourceUrl = details.sourceUrl ?: project.sourceUrl,
            wikiUrl = details.wikiUrl ?: project.wikiUrl,
        )
    }

    override suspend fun version(projectId: String, versionId: String): ResourceVersion =
        request<ModrinthVersion>("$baseUrl/version/$versionId") {}.toResourceVersion()

    override suspend fun projectsByIds(projectIds: List<String>): Map<String, ResourceProject> =
        projectIds.distinct().chunked(50).flatMap { ids ->
            request<List<ModrinthProjectDetails>>("$baseUrl/projects") { parameter("ids", resourceJson.encodeToString(ids)) }
        }.associate { project ->
            val type = when (project.projectType) {
                "modpack" -> ResourceType.MODPACK
                "resourcepack" -> ResourceType.RESOURCE_PACK
                "shader" -> ResourceType.SHADER_PACK
                else -> ResourceType.MOD
            }
            project.id to ResourceProject(provider, project.id, project.slug, project.title, project.description,
                "", type, project.downloads, project.iconUrl, "https://modrinth.com/${type.modrinthPath()}/${project.slug}", project.categories)
        }

    override suspend fun versionBySha1(sha1: String): ResourceVersion? {
        val response = httpClient.get("$baseUrl/version_file/$sha1") {
            header("User-Agent", userAgent)
            parameter("algorithm", "sha1")
        }
        if (response.status == HttpStatusCode.NotFound) return null
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val detail = resourceJson.decodeErrorDescription(body)
            throw LauncherException.Network(
                "Modrinth request failed with HTTP ${response.status.value}${detail?.let { ": $it" }.orEmpty()}",
            )
        }
        return try {
            resourceJson.decodeFromString<ModrinthVersion>(body).toResourceVersion()
        } catch (error: Exception) {
            throw LauncherException.Network("Modrinth returned invalid version metadata.", error)
        }
    }

    private suspend inline fun <reified T> request(
        url: String,
        crossinline configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): T = resourceApiRequest(provider) {
        httpClient.get(url) {
            header("User-Agent", userAgent)
            configure()
        }
    }
}

private fun ModrinthVersion.toResourceVersion() = ResourceVersion(
    provider = ResourceProvider.MODRINTH,
    id = id,
    projectId = projectId,
    name = name,
    versionNumber = versionNumber,
    gameVersions = gameVersions,
    loaders = loaders,
    channel = versionType.toReleaseChannel(),
    publishedAt = datePublished,
    files = files.map {
        ResourceFile(
            id = null,
            fileName = safeFileName(it.filename),
            url = it.url,
            sha1 = it.hashes["sha1"],
            size = it.size,
            primary = it.primary,
            sha512 = it.hashes["sha512"],
        )
    },
    dependencies = dependencies.map {
        ResourceDependency(
            projectId = it.projectId,
            versionId = it.versionId,
            fileName = it.fileName,
            kind = it.dependencyType.toDependencyKind(),
        )
    },
)

@Serializable
private data class CurseForgeSearchResponse(
    val data: List<CurseForgeProject>,
    val pagination: CurseForgePagination,
)

@Serializable
private data class CurseForgeProjectsResponse(val data: List<CurseForgeProject>)

@Serializable
private data class CurseForgeDescriptionResponse(val data: String)

@Serializable
private data class CurseForgePagination(
    val index: Int,
    val totalCount: Int,
)

@Serializable
private data class CurseForgeProject(
    val id: Long,
    val classId: Int = 0,
    val name: String,
    val slug: String,
    val summary: String,
    val downloadCount: Long = 0,
    val authors: List<CurseForgeAuthor> = emptyList(),
    val links: CurseForgeLinks = CurseForgeLinks(),
    val logo: CurseForgeLogo? = null,
    val categories: List<CurseForgeCategory> = emptyList(),
    val screenshots: List<CurseForgeAsset> = emptyList(),
    val dateModified: String? = null,
)

@Serializable
private data class CurseForgeAuthor(val name: String)

@Serializable
private data class CurseForgeLinks(
    val websiteUrl: String? = null,
    val wikiUrl: String? = null,
    val issuesUrl: String? = null,
    val sourceUrl: String? = null,
)

@Serializable
private data class CurseForgeLogo(val thumbnailUrl: String? = null, val url: String? = null)

@Serializable
private data class CurseForgeAsset(val thumbnailUrl: String? = null, val url: String? = null)

@Serializable
private data class CurseForgeCategory(val name: String)

@Serializable
private data class CurseForgeFilesResponse(val data: List<CurseForgeFile>)

@Serializable
private data class CurseForgeFileResponse(val data: CurseForgeFile)

@Serializable
private data class CurseForgeFilesRequest(val fileIds: List<Long>)

@Serializable
private data class CurseForgeProjectsRequest(val modIds: List<Long>)

@Serializable
private data class CurseForgeFile(
    val id: Long,
    val modId: Long,
    val displayName: String,
    val fileName: String,
    val fileDate: String,
    val releaseType: Int,
    val downloadUrl: String? = null,
    val fileLength: Long? = null,
    val hashes: List<CurseForgeHash> = emptyList(),
    val gameVersions: List<String> = emptyList(),
    val dependencies: List<CurseForgeDependency> = emptyList(),
)

@Serializable
private data class CurseForgeHash(val value: String, val algo: Int)

@Serializable
private data class CurseForgeDependency(val modId: Long, val relationType: Int)

class CurseForgeResourcePlatform(
    private val httpClient: HttpClient,
    apiKey: String,
    private val baseUrl: String = "https://api.curseforge.com/v1",
) : ResourcePlatform {
    private val apiKey = apiKey.trim()
    override val provider = ResourceProvider.CURSEFORGE
    override val isAvailable: Boolean get() = apiKey.isNotEmpty()

    override fun supports(type: ResourceType): Boolean = type.curseForgeClassId != null

    override suspend fun search(request: ResourceSearchRequest): ResourceSearchResult {
        ensureAvailable()
        val classId = request.type.curseForgeClassId
            ?: return ResourceSearchResult(emptyList(), request.offset, 0)
        val response: CurseForgeSearchResponse = request("$baseUrl/mods/search") {
            parameter("gameId", MINECRAFT_GAME_ID)
            parameter("classId", classId)
            request.query.trim().takeIf(String::isNotBlank)?.let { parameter("searchFilter", it) }
            request.gameVersion?.takeIf(String::isNotBlank)?.let { parameter("gameVersion", it) }
            if (request.type in loaderFilteredTypes) {
                request.loader?.curseForgeType()?.let { parameter("modLoaderType", it) }
            }
            parameter(
                "sortField",
                when (request.sort) {
                    ResourceSearchSort.RELEVANCE -> 1
                    ResourceSearchSort.FEATURED -> 2
                    ResourceSearchSort.DOWNLOADS -> 6
                    ResourceSearchSort.UPDATED -> 3
                    ResourceSearchSort.NEWEST -> 11
                },
            )
            parameter("sortOrder", "desc")
            parameter("index", request.offset)
            parameter("pageSize", request.limit)
        }
        return ResourceSearchResult(
            projects = response.data.map { it.toResourceProject(request.type) },
            offset = response.pagination.index,
            total = response.pagination.totalCount,
        )
    }

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ): List<ResourceVersion> {
        ensureAvailable()
        val response: CurseForgeFilesResponse = request("$baseUrl/mods/${project.id}/files") {
            gameVersion?.takeIf(String::isNotBlank)?.let { parameter("gameVersion", it) }
            if (project.type in loaderFilteredTypes) {
                loader?.curseForgeType()?.let { parameter("modLoaderType", it) }
            }
            parameter("pageSize", 50)
        }
        return response.data.map(CurseForgeFile::toResourceVersion).sortedByDescending { it.publishedAt }
    }

    override suspend fun details(project: ResourceProject): ResourceProject {
        ensureAvailable()
        val description = request<CurseForgeDescriptionResponse>("$baseUrl/mods/${project.id}/description") {}.data
        return project.copy(description = description)
    }

    override suspend fun version(projectId: String, versionId: String): ResourceVersion {
        ensureAvailable()
        return request<CurseForgeFileResponse>("$baseUrl/mods/$projectId/files/$versionId") {}
            .data
            .toResourceVersion()
    }

    override suspend fun versionsByIds(references: List<Pair<String, String>>): List<ResourceVersion> {
        ensureAvailable()
        val filesById = references.map { it.second.toLong() }.chunked(50).flatMap { fileIds ->
            resourceApiRequest<CurseForgeFilesResponse>(provider) {
                httpClient.post("$baseUrl/mods/files") {
                    header("x-api-key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(resourceJson.encodeToString(CurseForgeFilesRequest(fileIds)))
                }
            }.data
        }.associateBy { it.id.toString() }
        return references.map { (_, fileId) ->
            filesById[fileId]?.toResourceVersion()
                ?: throw LauncherException.InvalidMetadata("CurseForge did not return file $fileId.")
        }
    }

    override suspend fun versionByFingerprint(fingerprint: Long, sha1: String): ResourceVersion? {
        ensureAvailable()
        val matches = resourceApiRequest<CurseForgeFingerprintResponse>(provider) {
            httpClient.post("$baseUrl/fingerprints/432") {
                header("x-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(resourceJson.encodeToString(CurseForgeFingerprintRequest(listOf(fingerprint))))
            }
        }
        return matches.data.exactMatches.map { it.file.toResourceVersion() }
            .firstOrNull { version -> version.files.any { it.sha1.equals(sha1, ignoreCase = true) } }
    }

    override suspend fun projectsByIds(projectIds: List<String>): Map<String, ResourceProject> {
        ensureAvailable()
        return projectIds.distinct().map(String::toLong).chunked(50).flatMap { modIds ->
            resourceApiRequest<CurseForgeProjectsResponse>(provider) {
                httpClient.post("$baseUrl/mods") {
                    header("x-api-key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(resourceJson.encodeToString(CurseForgeProjectsRequest(modIds)))
                }
            }.data
        }.associate { project ->
            val type = resourceTypeForCurseForgeClass(project.classId)
            project.id.toString() to project.toResourceProject(type)
        }
    }

    private fun ensureAvailable() {
        if (!isAvailable) {
            throw LauncherException.InvalidMetadata(
                "CurseForge is not configured for this build. Set TRESTLE_CURSEFORGE_API_KEY to a key issued for Trestle.",
            )
        }
    }

    private suspend inline fun <reified T> request(
        url: String,
        crossinline configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): T = resourceApiRequest(provider) {
        httpClient.get(url) {
            header("x-api-key", apiKey)
            configure()
        }
    }

    private companion object {
        const val MINECRAFT_GAME_ID = 432
    }
}

private fun CurseForgeProject.toResourceProject(type: ResourceType) = ResourceProject(
    provider = ResourceProvider.CURSEFORGE,
    id = id.toString(),
    slug = slug,
    name = name,
    summary = summary,
    author = authors.joinToString { it.name },
    type = type,
    downloads = downloadCount,
    iconUrl = logo?.thumbnailUrl ?: logo?.url,
    websiteUrl = links.websiteUrl,
    categories = categories.map { it.name },
    featuredImageUrl = screenshots.firstNotNullOfOrNull { it.thumbnailUrl ?: it.url },
    updatedAt = dateModified,
    sourceUrl = links.sourceUrl,
    issuesUrl = links.issuesUrl,
    wikiUrl = links.wikiUrl,
)

private fun resourceTypeForCurseForgeClass(classId: Int): ResourceType = when (classId) {
    6 -> ResourceType.MOD
    4471 -> ResourceType.MODPACK
    12 -> ResourceType.RESOURCE_PACK
    6552 -> ResourceType.SHADER_PACK
    else -> ResourceType.MOD
}

private fun CurseForgeFile.toResourceVersion(): ResourceVersion {
    val loaders = gameVersions.filter { it.lowercase() in knownLoaderNames }
    val minecraftVersions = gameVersions.filter { it.any(Char::isDigit) && '.' in it }
    return ResourceVersion(
        provider = ResourceProvider.CURSEFORGE,
        id = id.toString(),
        projectId = modId.toString(),
        name = displayName,
        versionNumber = displayName,
        gameVersions = minecraftVersions,
        loaders = loaders,
        channel = when (releaseType) {
            1 -> ReleaseChannel.RELEASE
            2 -> ReleaseChannel.BETA
            3 -> ReleaseChannel.ALPHA
            else -> ReleaseChannel.UNKNOWN
        },
        publishedAt = fileDate,
        files = listOf(
            ResourceFile(
                id = id.toString(),
                fileName = safeFileName(fileName),
                url = downloadUrl,
                sha1 = hashes.firstOrNull { it.algo == 1 }?.value,
                size = fileLength,
                primary = true,
                sha512 = null,
            ),
        ),
        dependencies = dependencies.map {
            ResourceDependency(
                projectId = it.modId.toString(),
                versionId = null,
                fileName = null,
                kind = when (it.relationType) {
                    2 -> DependencyKind.OPTIONAL
                    3 -> DependencyKind.REQUIRED
                    5 -> DependencyKind.INCOMPATIBLE
                    1, 6 -> DependencyKind.EMBEDDED
                    else -> DependencyKind.OPTIONAL
                },
            )
        },
    )
}

private suspend inline fun <reified T> resourceApiRequest(
    provider: ResourceProvider,
    request: () -> HttpResponse,
): T {
    try {
        val response = request()
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val detail = resourceJson.decodeErrorDescription(body)
            val suffix = detail?.let { ": $it" }.orEmpty()
            throw LauncherException.Network(
                "${provider.label} request failed with HTTP ${response.status.value}$suffix",
            )
        }
        return resourceJson.decodeFromString(body)
    } catch (error: CancellationException) {
        throw error
    } catch (error: LauncherException) {
        throw error
    } catch (error: Exception) {
        throw LauncherException.Network("${provider.label} request failed: ${error.message ?: "unknown error"}", error)
    }
}

private fun Json.decodeErrorDescription(body: String): String? = runCatching {
    val element = parseToJsonElement(body).jsonObject
    (element["description"] ?: element["error"] ?: element["message"])
        ?.toString()
        ?.trim('"')
        ?.take(240)
}.getOrNull()

private fun ResourceType.modrinthPath(): String = when (this) {
    ResourceType.MOD -> "mod"
    ResourceType.MODPACK -> "modpack"
    ResourceType.RESOURCE_PACK -> "resourcepack"
    ResourceType.SHADER_PACK -> "shader"
}

internal fun ModLoader.modrinthName(): String? = when (this) {
    ModLoader.VANILLA -> null
    ModLoader.FORGE -> "forge"
    ModLoader.FABRIC -> "fabric"
    ModLoader.QUILT -> "quilt"
    ModLoader.NEOFORGE -> "neoforge"
}

internal fun ModLoader.curseForgeType(): Int? = when (this) {
    ModLoader.VANILLA -> null
    ModLoader.FORGE -> 1
    ModLoader.FABRIC -> 4
    ModLoader.QUILT -> 5
    ModLoader.NEOFORGE -> 6
}

internal fun safeFileName(value: String): String {
    val name = value.substringAfterLast('/').substringAfterLast('\\')
    require(name.isNotBlank() && name !in setOf(".", "..")) { "Resource file name is unsafe." }
    return name
}

private fun String.toReleaseChannel(): ReleaseChannel = when (lowercase()) {
    "release" -> ReleaseChannel.RELEASE
    "beta" -> ReleaseChannel.BETA
    "alpha" -> ReleaseChannel.ALPHA
    else -> ReleaseChannel.UNKNOWN
}

private fun String.toDependencyKind(): DependencyKind = when (lowercase()) {
    "required" -> DependencyKind.REQUIRED
    "optional" -> DependencyKind.OPTIONAL
    "incompatible" -> DependencyKind.INCOMPATIBLE
    "embedded" -> DependencyKind.EMBEDDED
    else -> DependencyKind.OPTIONAL
}

private fun String.toEnvironmentSupport(): ResourceEnvironmentSupport = when (lowercase()) {
    "required" -> ResourceEnvironmentSupport.REQUIRED
    "optional" -> ResourceEnvironmentSupport.OPTIONAL
    "unsupported" -> ResourceEnvironmentSupport.UNSUPPORTED
    else -> ResourceEnvironmentSupport.UNKNOWN
}

private val loaderFilteredTypes = setOf(ResourceType.MOD, ResourceType.MODPACK)
private val knownLoaderNames = setOf("forge", "fabric", "quilt", "neoforge", "cauldron", "liteloader")
private val resourceJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class CurseForgeFingerprintRequest(val fingerprints: List<Long>)
@Serializable
private data class CurseForgeFingerprintResponse(val data: CurseForgeFingerprintMatches)
@Serializable
private data class CurseForgeFingerprintMatches(val exactMatches: List<CurseForgeFingerprintMatch> = emptyList())
@Serializable
private data class CurseForgeFingerprintMatch(val file: CurseForgeFile)
