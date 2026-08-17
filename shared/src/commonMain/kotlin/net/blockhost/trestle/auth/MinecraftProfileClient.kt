package net.blockhost.trestle.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger

@Serializable
private data class ProfileSkinResponse(
    val id: String,
    val state: String = "ACTIVE",
    val url: String,
    val variant: String = "CLASSIC",
)

@Serializable
private data class MinecraftProfileResponse(
    val id: String,
    val name: String,
    val skins: List<ProfileSkinResponse> = emptyList(),
)

class MinecraftProfileClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.minecraftservices.com",
    private val logger: LauncherLogger = NoopLauncherLogger,
) {
    suspend fun fetchProfile(session: AuthSession): LauncherAccount = requestProfile(session, "fetch")

    suspend fun uploadSkin(session: AuthSession, bytes: ByteArray, variant: SkinVariant): LauncherAccount {
        inspectMinecraftSkin(bytes)
        return execute("upload") {
            httpClient.post("$baseUrl/minecraft/profile/skins") {
                bearerAuth(session.requireAccessToken().reveal())
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("variant", variant.apiValue)
                            append(
                                "file",
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/png")
                                    append(HttpHeaders.ContentDisposition, "filename=skin.png")
                                },
                            )
                        },
                    ),
                )
            }.toAccount()
        }
    }

    suspend fun resetActiveSkin(session: AuthSession): LauncherAccount {
        execute("reset") {
            val response = httpClient.delete("$baseUrl/minecraft/profile/skins/active") {
                bearerAuth(session.requireAccessToken().reveal())
            }
            if (!response.status.isSuccess()) {
                throw LauncherException.Network("Skin reset failed with HTTP ${response.status.value}.")
            }
        }
        return requestProfile(session, "refresh")
    }

    suspend fun fetchSkinTexture(url: String): ByteArray = execute("download") {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw LauncherException.Network("Skin download failed with HTTP ${response.status.value}.")
        }
        response.bodyAsBytes().also(::inspectMinecraftSkin)
    }

    private suspend fun requestProfile(session: AuthSession, operation: String): LauncherAccount = execute(operation) {
        httpClient.get("$baseUrl/minecraft/profile") {
            bearerAuth(session.requireAccessToken().reveal())
        }.toAccount()
    }

    private suspend fun io.ktor.client.statement.HttpResponse.toAccount(): LauncherAccount {
        if (!status.isSuccess()) {
            throw LauncherException.Network("Minecraft profile request failed with HTTP ${status.value}.")
        }
        val response = profileJson.decodeFromString<MinecraftProfileResponse>(bodyAsText())
        val activeSkin = response.skins.firstOrNull { it.state.equals("ACTIVE", ignoreCase = true) }
            ?: response.skins.firstOrNull()
        return LauncherAccount(
            profileId = response.id,
            playerName = response.name,
            skin = activeSkin?.let {
                AccountSkin(
                    id = it.id,
                    url = it.url.replace("http://textures.minecraft.net", "https://textures.minecraft.net"),
                    variant = if (it.variant.equals("SLIM", ignoreCase = true)) SkinVariant.SLIM else SkinVariant.CLASSIC,
                )
            },
        )
    }

    private suspend inline fun <T> execute(operation: String, block: () -> T): T {
        try {
            val result = block()
            logger.info("skins", "Minecraft profile operation completed", mapOf("operation" to operation))
            return result
        } catch (error: CancellationException) {
            throw error
        } catch (error: LauncherException) {
            logger.warn("skins", "Minecraft profile operation failed", error, mapOf("operation" to operation))
            throw error
        } catch (error: Exception) {
            logger.error("skins", "Minecraft profile operation failed", error, mapOf("operation" to operation))
            throw LauncherException.Network("The Minecraft profile service could not be reached.", error)
        }
    }

    private companion object {
        val profileJson = Json { ignoreUnknownKeys = true }
    }
}

private fun AuthSession.requireAccessToken(): SecretValue =
    requireNotNull(accessToken) { "This account does not provide an online Minecraft access token." }
