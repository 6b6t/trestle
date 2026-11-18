package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_release_links_must_use_https
import net.blockhost.trestle.resources.msg_the_release_contains_invalid_download_metadata
import net.blockhost.trestle.resources.msg_the_release_download_manifest_does_not_match
import net.blockhost.trestle.resources.msg_the_release_metadata_is_too_large
import net.blockhost.trestle.resources.msg_update_check_failed_with_http_s_try
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text

@Serializable
data class ReleaseArtifact(
    val platform: String,
    val architecture: String,
    val format: String,
    val url: String,
    val sha256: String,
    val size: Long,
    val minimumOS: String,
)

@Serializable
data class ReleaseManifest(val schemaVersion: Int, val version: String, val artifacts: List<ReleaseArtifact>)

data class LauncherUpdate(
    val version: String,
    val releaseUrl: String,
    val releaseNotes: String = "",
    val downloads: List<ReleaseArtifact> = emptyList(),
    /** Set when a package manager owns this installation. The launcher then offers no download. */
    val managedBy: ManagedInstall? = null,
)

class UpdateChecker(
    private val httpClient: HttpClient,
    private val endpoint: String = "https://api.github.com/repos/6b6t/trestle/releases/latest",
    private val environment: PlatformEnvironment? = null,
    private val isMobile: Boolean = false,
    private val installChannel: InstallChannel = InstallChannel.Download(),
) {
    suspend fun availableUpdate(currentVersion: String = BuildInfo.VERSION, includePrereleases: Boolean = false): LauncherUpdate? {
        val current = ReleaseVersion.parse(currentVersion) ?: return null
        val url = if (includePrereleases) endpoint.removeSuffix("/latest") + "?per_page=30" else endpoint
        val body = fetch(url) ?: return null
        val releases = if (includePrereleases) {
            updateJson.decodeFromString<List<GitHubRelease>>(body)
        } else {
            listOf(updateJson.decodeFromString<GitHubRelease>(body))
        }
        val release = releases.filter { !it.draft && (includePrereleases || !it.prerelease) }
            .mapNotNull { release -> ReleaseVersion.parse(release.tagName)?.let { it to release } }
            .filter { (version, _) -> (includePrereleases || version.prerelease.isEmpty()) && version > current }
            .maxByOrNull { it.first }?.second ?: return null
        requireHttps(release.htmlUrl)
        val manifestAsset = release.assets.firstOrNull { it.name == "release-manifest.json" }
        val manifest = manifestAsset?.let { asset ->
            requireHttps(asset.url)
            fetch(asset.url)?.let { updateJson.decodeFromString<ReleaseManifest>(it) }
        }
        if (manifest != null) {
            requireUser(manifest.schemaVersion == 1 && manifest.version == release.tagName.removePrefix("v")) {
                text(Res.string.msg_the_release_download_manifest_does_not_match)
            }
            manifest.artifacts.forEach {
                requireHttps(it.url)
                requireUser(it.sha256.matches(Regex("[a-fA-F0-9]{64}")) && it.size > 0) {
                    text(Res.string.msg_the_release_contains_invalid_download_metadata)
                }
            }
        }
        val platform = if (isMobile) {
            when (environment?.operatingSystem) {
                OperatingSystem.IOS -> "ios"
                else -> "android"
            }
        } else {
            when (environment?.operatingSystem) {
                OperatingSystem.MACOS -> "macos"
                OperatingSystem.WINDOWS -> "windows"
                OperatingSystem.LINUX -> "linux"
                else -> null
            }
        }
        val architecture = when (environment?.architecture) {
            Architecture.X86_64 -> "x64"
            Architecture.ARM64 -> "arm64"
            else -> null
        }
        val version = release.tagName.removePrefix("v")
        if (installChannel is InstallChannel.PackageManager) {
            return LauncherUpdate(version, release.htmlUrl, release.body.orEmpty(), managedBy = installChannel.manager)
        }
        val preferredFormat = (installChannel as InstallChannel.Download).format
        val downloads = manifest?.artifacts.orEmpty().filter {
            it.platform == platform && it.architecture == architecture && it.format != "aab"
        }.sortedBy { artifact ->
            // The format this copy was installed from comes first, so a DEB install is offered the next DEB.
            if (artifact.format == preferredFormat) -1 else DOWNLOAD_ORDER.indexOf(artifact.format).let { if (it < 0) 99 else it }
        }
        return LauncherUpdate(version, release.htmlUrl, release.body.orEmpty(), downloads)
    }

    private suspend fun fetch(url: String): String? {
        val response = httpClient.get(url) { header(HttpHeaders.UserAgent, BuildInfo.USER_AGENT) }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) {
            throw LauncherException.Network(text(Res.string.msg_update_check_failed_with_http_s_try, response.status.value))
        }
        val body = response.bodyAsText()
        requireUser(body.length <= 2 * 1024 * 1024) { text(Res.string.msg_the_release_metadata_is_too_large) }
        return body
    }

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String,
        val body: String? = null,
        val prerelease: Boolean = false,
        val draft: Boolean = false,
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(val name: String, @SerialName("browser_download_url") val url: String)

    private companion object {
        val DOWNLOAD_ORDER = listOf("apk", "msi", "dmg", "deb", "rpm", "AppImage", "tar.gz", "exe", "pkg")
        val updateJson = Json { ignoreUnknownKeys = true }
        fun requireHttps(value: String) {
            val url = Url(value)
            requireUser(url.protocol.name == "https" && url.host.isNotBlank() && url.user == null && url.password == null) {
                text(Res.string.msg_release_links_must_use_https)
            }
        }
    }
}
