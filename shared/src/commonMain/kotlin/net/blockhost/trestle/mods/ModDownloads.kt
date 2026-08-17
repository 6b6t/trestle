package net.blockhost.trestle.mods

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.download.DownloadRequest
import okio.Path.Companion.toPath

data class ModDownload(
    val provider: String,
    val projectId: String,
    val versionId: String,
    val fileName: String,
    val url: String,
    val sha1: String?,
    val size: Long?,
)

interface ModDownloadProvider {
    suspend fun resolve(projectId: String, gameVersion: String, loader: ModLoader): ModDownload
}

@Serializable
private data class ModrinthVersion(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val files: List<ModrinthFile>,
)

@Serializable
private data class ModrinthFile(
    val hashes: Map<String, String>,
    val url: String,
    val filename: String,
    val primary: Boolean = false,
    val size: Long? = null,
)

class ModrinthDownloadProvider(
    private val httpClient: HttpClient,
    private val userAgent: String,
    private val baseUrl: String = "https://api.modrinth.com/v2",
) : ModDownloadProvider {
    override suspend fun resolve(projectId: String, gameVersion: String, loader: ModLoader): ModDownload {
        val versions: List<ModrinthVersion> = request("$baseUrl/project/$projectId/version") {
            parameter("game_versions", "[\"$gameVersion\"]")
            if (loader != ModLoader.VANILLA) parameter("loaders", "[\"${loader.name.lowercase()}\"]")
        }
        val version = versions.firstOrNull()
            ?: throw LauncherException.InvalidMetadata("Modrinth has no compatible file for $projectId.")
        val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
            ?: throw LauncherException.InvalidMetadata("The Modrinth version has no downloadable file.")
        return ModDownload(
            provider = "Modrinth",
            projectId = version.projectId,
            versionId = version.id,
            fileName = safeFileName(file.filename),
            url = file.url,
            sha1 = file.hashes["sha1"],
            size = file.size,
        )
    }

    private suspend inline fun <reified T> request(
        url: String,
        crossinline configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): T = apiRequest {
        httpClient.get(url) {
            header("User-Agent", userAgent)
            configure()
        }
    }
}

@Serializable
private data class CurseForgeFilesResponse(val data: List<CurseForgeFile>)

@Serializable
private data class CurseForgeFile(
    val id: Long,
    val fileName: String,
    val downloadUrl: String? = null,
    val fileLength: Long? = null,
    val hashes: List<CurseForgeHash> = emptyList(),
)

@Serializable
private data class CurseForgeHash(val value: String, val algo: Int)

class CurseForgeDownloadProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://api.curseforge.com/v1",
) : ModDownloadProvider {
    init {
        require(apiKey.isNotBlank()) { "A CurseForge API key is required." }
    }

    override suspend fun resolve(projectId: String, gameVersion: String, loader: ModLoader): ModDownload {
        val response: CurseForgeFilesResponse = apiRequest {
            httpClient.get("$baseUrl/mods/$projectId/files") {
                header("x-api-key", apiKey)
                parameter("gameVersion", gameVersion)
                loader.curseForgeType()?.let { parameter("modLoaderType", it) }
                parameter("pageSize", 50)
            }
        }
        val file = response.data.firstOrNull { it.downloadUrl != null }
            ?: throw LauncherException.InvalidMetadata(
                "CurseForge has no compatible file with third-party downloads enabled.",
            )
        return ModDownload(
            provider = "CurseForge",
            projectId = projectId,
            versionId = file.id.toString(),
            fileName = safeFileName(file.fileName),
            url = requireNotNull(file.downloadUrl),
            sha1 = file.hashes.firstOrNull { it.algo == 1 }?.value,
            size = file.fileLength,
        )
    }
}

class ModInstaller(private val downloadPipeline: DownloadPipeline) {
    suspend fun install(
        instance: GameInstance,
        download: ModDownload,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ) {
        val instancePath = instance.instanceDirectory.toPath()
        downloadPipeline.download(
            requests = listOf(
                DownloadRequest(
                    url = download.url,
                    destination = instancePath / "game" / "mods" / download.fileName,
                    sha1 = download.sha1,
                    size = download.size,
                ),
            ),
            stagingDirectory = instancePath / ".trestle" / "mod-staging" / download.versionId,
            onProgress = onProgress,
        )
    }
}

private suspend inline fun <reified T> apiRequest(request: () -> io.ktor.client.statement.HttpResponse): T {
    try {
        val response = request()
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("Mod service request failed with HTTP ${response.status.value}.")
        }
        return modJson.decodeFromString(response.bodyAsText())
    } catch (error: CancellationException) {
        throw error
    } catch (error: LauncherException) {
        throw error
    } catch (error: Exception) {
        throw LauncherException.Network("Mod service request failed.", error)
    }
}

private fun ModLoader.curseForgeType(): Int? = when (this) {
    ModLoader.VANILLA -> null
    ModLoader.FORGE -> 1
    ModLoader.FABRIC -> 4
    ModLoader.QUILT -> 5
    ModLoader.NEOFORGE -> 6
}

private fun safeFileName(value: String): String {
    val name = value.substringAfterLast('/').substringAfterLast('\\')
    require(name.isNotBlank() && name !in setOf(".", "..")) { "Mod file name is unsafe." }
    return name
}

private val modJson = Json { ignoreUnknownKeys = true }
