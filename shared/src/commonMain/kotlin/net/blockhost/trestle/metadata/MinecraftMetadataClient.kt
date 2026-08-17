package net.blockhost.trestle.metadata

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import okio.ByteString.Companion.encodeUtf8

private val metadataJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

class MinecraftMetadataClient(
    private val httpClient: HttpClient,
    private val manifestUrl: String = OFFICIAL_VERSION_MANIFEST,
) {
    suspend fun fetchVersionManifest(): VersionManifest = getJson(manifestUrl)

    suspend fun fetchVersion(reference: VersionReference): VersionMetadata =
        getJson(reference.url, reference.sha1, "${reference.id}.json")

    suspend fun resolveVersion(versionId: String): VersionMetadata {
        val reference = fetchVersionManifest().versions.firstOrNull { it.id == versionId }
            ?: throw LauncherException.InvalidMetadata("Minecraft version $versionId is not in the official manifest.")
        return fetchVersion(reference)
    }

    suspend fun fetchAssetIndex(reference: AssetIndexReference): AssetIndex =
        getJson(reference.url, reference.sha1, "${reference.id}.json")

    private suspend inline fun <reified T> getJson(
        url: String,
        expectedSha1: String? = null,
        artifactName: String = safeHost(url),
    ): T {
        try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                throw LauncherException.Network("Metadata request failed with HTTP ${response.status.value}.")
            }
            val body = response.bodyAsText()
            if (expectedSha1 != null) {
                val actual = body.encodeUtf8().sha1().hex()
                if (!actual.equals(expectedSha1, ignoreCase = true)) {
                    throw LauncherException.ChecksumMismatch(artifactName, expectedSha1, actual)
                }
            }
            return metadataJson.decodeFromString(body)
        } catch (error: CancellationException) {
            throw error
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.Network("Metadata request failed for ${safeHost(url)}.", error)
        }
    }

    companion object {
        const val OFFICIAL_VERSION_MANIFEST =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }
}

@Serializable
data class FabricLoaderVersion(
    val separator: String = ".",
    val build: Int = 0,
    val maven: String,
    val version: String,
    val stable: Boolean,
)

@Serializable
data class FabricLoaderEntry(val loader: FabricLoaderVersion)

class FabricMetadataClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://meta.fabricmc.net/v2",
) {
    suspend fun loaderVersions(gameVersion: String): List<FabricLoaderVersion> =
        getJson<List<FabricLoaderEntry>>("$baseUrl/versions/loader/$gameVersion").map { it.loader }

    suspend fun profile(gameVersion: String, loaderVersion: String): VersionMetadata =
        getJson("$baseUrl/versions/loader/$gameVersion/$loaderVersion/profile/json")

    private suspend inline fun <reified T> getJson(url: String): T {
        try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                throw LauncherException.Network("Fabric metadata request failed with HTTP ${response.status.value}.")
            }
            return metadataJson.decodeFromString(response.bodyAsText())
        } catch (error: CancellationException) {
            throw error
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.Network("Fabric metadata request failed.", error)
        }
    }
}

private fun safeHost(url: String): String = url.substringAfter("://").substringBefore('/')
