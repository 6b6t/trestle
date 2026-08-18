package net.blockhost.trestle.app

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException

data class LauncherUpdate(
    val version: String,
    val releaseUrl: String,
)

class UpdateChecker(
    private val httpClient: HttpClient,
    private val endpoint: String = "https://api.github.com/repos/6b6t/trestle/releases/latest",
) {
    suspend fun availableUpdate(currentVersion: String = BuildInfo.VERSION): LauncherUpdate? {
        val response = httpClient.get(endpoint) { header(HttpHeaders.UserAgent, BuildInfo.USER_AGENT) }
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("Update check failed with HTTP ${response.status.value}.")
        }
        val release = updateJson.decodeFromString<GitHubRelease>(response.bodyAsText())
        val latest = release.tagName.removePrefix("v")
        return LauncherUpdate(latest, release.htmlUrl).takeIf {
            compareVersions(latest, currentVersion) > 0
        }
    }

    private fun compareVersions(first: String, second: String): Int {
        val left = first.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val right = second.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        return (0 until maxOf(left.size, right.size))
            .firstNotNullOfOrNull { index ->
                (left.getOrElse(index) { 0 } - right.getOrElse(index) { 0 }).takeIf { it != 0 }
            } ?: 0
    }

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String,
    )

    private companion object {
        val updateJson = Json { ignoreUnknownKeys = true }
    }
}
