package net.blockhost.trestle.metadata

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import okio.ByteString.Companion.encodeUtf8

private val metadataJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
private const val NEOFORGE_MINECRAFT_COMPONENT = "net.minecraft"

class MinecraftMetadataClient(
    private val httpClient: HttpClient,
    private val manifestUrl: String = OFFICIAL_VERSION_MANIFEST,
    private val logger: LauncherLogger = NoopLauncherLogger,
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
            return metadataJson.decodeFromString<T>(body).also {
                logger.debug("metadata", "Loaded Minecraft metadata", mapOf("host" to safeHost(url)))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            logger.warn("metadata", "Minecraft metadata request failed", error, mapOf("host" to safeHost(url)))
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

@Serializable
data class NeoForgeLoaderVersion(
    val version: String,
    val recommended: Boolean = false,
    val releaseTime: String = "",
) {
    val stable: Boolean get() = '-' !in version
}

data class NeoForgeInstallProfile(
    val metadata: VersionMetadata,
    val mavenFiles: List<MojangLibrary>,
)

@Serializable
private data class NeoForgeVersionIndex(
    val versions: List<NeoForgeVersionReference>,
)

@Serializable
private data class NeoForgeVersionReference(
    val version: String,
    val recommended: Boolean = false,
    val releaseTime: String = "",
    val sha256: String,
    val requires: List<ComponentRequirement> = emptyList(),
) {
    fun supports(gameVersion: String): Boolean = requires.any {
        it.uid == NEOFORGE_MINECRAFT_COMPONENT && it.exactVersion == gameVersion
    }
}

@Serializable
private data class NeoForgeComponentProfile(
    val version: String,
    val mainClass: String,
    val libraries: List<MojangLibrary> = emptyList(),
    val mavenFiles: List<MojangLibrary> = emptyList(),
    val minecraftArguments: String,
    val requires: List<ComponentRequirement> = emptyList(),
)

@Serializable
private data class ComponentRequirement(
    val uid: String,
    @SerialName("equals") val exactVersion: String? = null,
)

class NeoForgeMetadataClient(
    private val httpClient: HttpClient,
    private val userAgent: String,
    private val baseUrl: String = "https://meta.prismlauncher.org/v1/net.neoforged",
) {
    suspend fun loaderVersions(gameVersion: String): List<NeoForgeLoaderVersion> =
        versionReferences(gameVersion).map {
            NeoForgeLoaderVersion(it.version, it.recommended, it.releaseTime)
        }

    suspend fun profile(gameVersion: String, loaderVersion: String): NeoForgeInstallProfile {
        val reference = versionReferences(gameVersion).firstOrNull { it.version == loaderVersion }
            ?: throw LauncherException.InvalidMetadata(
                "NeoForge $loaderVersion does not support Minecraft $gameVersion.",
            )
        val body = get("$baseUrl/$loaderVersion.json")
        val actualSha256 = body.encodeUtf8().sha256().hex()
        if (!actualSha256.equals(reference.sha256, ignoreCase = true)) {
            throw LauncherException.ChecksumMismatch(
                "NeoForge $loaderVersion metadata",
                reference.sha256,
                actualSha256,
            )
        }
        val profile = decode<NeoForgeComponentProfile>(body)
        val declaredGameVersion = profile.requires
            .firstOrNull { it.uid == NEOFORGE_MINECRAFT_COMPONENT }
            ?.exactVersion
        if (profile.version != loaderVersion || declaredGameVersion != gameVersion) {
            throw LauncherException.InvalidMetadata(
                "NeoForge $loaderVersion metadata does not target Minecraft $gameVersion.",
            )
        }
        return NeoForgeInstallProfile(
            metadata = VersionMetadata(
                id = "neoforge-$loaderVersion",
                mainClass = profile.mainClass,
                libraries = profile.libraries,
                minecraftArguments = profile.minecraftArguments,
                inheritsFrom = gameVersion,
            ),
            mavenFiles = profile.mavenFiles,
        )
    }

    private suspend fun versionReferences(gameVersion: String): List<NeoForgeVersionReference> =
        decode<NeoForgeVersionIndex>(get("$baseUrl/index.json")).versions.filter { it.supports(gameVersion) }

    private suspend fun get(url: String): String {
        try {
            val response = httpClient.get(url) { header(HttpHeaders.UserAgent, userAgent) }
            if (!response.status.isSuccess()) {
                throw LauncherException.Network(
                    "NeoForge metadata request failed with HTTP ${response.status.value}.",
                )
            }
            return response.bodyAsText()
        } catch (error: CancellationException) {
            throw error
        } catch (error: LauncherException) {
            throw error
        } catch (error: Exception) {
            throw LauncherException.Network("NeoForge metadata request failed.", error)
        }
    }

    private inline fun <reified T> decode(body: String): T = try {
        metadataJson.decodeFromString(body)
    } catch (error: Exception) {
        throw LauncherException.InvalidMetadata("NeoForge returned invalid component metadata.", error)
    }
}

private fun safeHost(url: String): String = url.substringAfter("://").substringBefore('/')
