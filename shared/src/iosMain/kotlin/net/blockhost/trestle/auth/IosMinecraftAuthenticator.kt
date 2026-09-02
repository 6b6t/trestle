package net.blockhost.trestle.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import okio.ByteString.Companion.encodeUtf8

internal class IosMinecraftAuthenticator(
    private val httpClient: HttpClient,
    private val nowMillis: () -> Long,
    private val logger: LauncherLogger = NoopLauncherLogger,
) : MinecraftAuthenticator {
    override suspend fun authenticate(
        request: AccountLoginRequest,
        onDeviceAuthorization: (DeviceAuthorization) -> Unit,
    ): AuthenticatedMinecraftAccount = execute("sign-in") {
        when (request) {
            is AccountLoginRequest.DeviceCode -> {
                require(request.edition == MinecraftEdition.JAVA) {
                    "Bedrock authentication is not available on iOS yet."
                }
                deviceCodeResult(onDeviceAuthorization)
            }
            is AccountLoginRequest.SecretImport -> when (request.method) {
                AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ->
                    microsoftResult(refreshMicrosoftToken(request.secret.reveal()), request.method)
                AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> accessTokenResult(request.secret)
                else -> unsupported(request.method)
            }
            is AccountLoginRequest.Offline -> offlineResult(request.username)
            is AccountLoginRequest.Credentials -> unsupported(request.method)
        }
    }

    override suspend fun restore(
        profile: LauncherAccount,
        serializedState: SecretValue,
    ): AuthenticatedMinecraftAccount = execute("restore") {
        when (profile.authenticationMethod) {
            AccountAuthenticationMethod.OFFLINE -> offlineResult(profile.playerName)
            AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> accessTokenResult(serializedState)
            AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
            AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN,
            -> {
                val state = authJson.decodeFromString<IosAuthenticationState>(serializedState.reveal())
                microsoftResult(refreshMicrosoftToken(state.refreshToken), profile.authenticationMethod)
            }
            else -> unsupported(profile.authenticationMethod)
        }
    }

    private suspend fun deviceCodeResult(
        callback: (DeviceAuthorization) -> Unit,
    ): AuthenticatedMinecraftAccount {
        val response = postForm(
            DEVICE_CODE_ENDPOINT,
            "client_id" to OfficialMinecraftApplications.java.clientId,
            "scope" to "XboxLive.signin offline_access",
        )
        val deviceCode = response.requiredString("device_code")
        val expiresIn = response.requiredLong("expires_in")
        var intervalMillis = response.optionalLong("interval")?.times(1_000L) ?: 5_000L
        callback(
            DeviceAuthorization(
                userCode = response.requiredString("user_code"),
                verificationUri = response.requiredString("verification_uri"),
                directVerificationUri = response.optionalString("verification_uri_complete")
                    ?: response.requiredString("verification_uri"),
                expiresAtEpochMillis = nowMillis() + expiresIn * 1_000L,
            ),
        )
        val deadline = nowMillis() + expiresIn * 1_000L
        while (nowMillis() < deadline) {
            delay(intervalMillis)
            try {
                val token = postForm(
                    TOKEN_ENDPOINT,
                    "client_id" to OfficialMinecraftApplications.java.clientId,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code" to deviceCode,
                )
                return microsoftResult(token.toMicrosoftToken(), AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE)
            } catch (error: MicrosoftAuthorizationPending) {
                continue
            } catch (error: MicrosoftAuthorizationSlowDown) {
                intervalMillis += 5_000L
            }
        }
        error("The Microsoft device code expired before sign-in completed.")
    }

    private suspend fun refreshMicrosoftToken(refreshToken: String): MicrosoftToken = postForm(
        TOKEN_ENDPOINT,
        "client_id" to OfficialMinecraftApplications.java.clientId,
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken.trim(),
        "scope" to "XboxLive.signin offline_access",
    ).toMicrosoftToken()

    private suspend fun microsoftResult(
        microsoftToken: MicrosoftToken,
        method: AccountAuthenticationMethod,
    ): AuthenticatedMinecraftAccount {
        val xbox = postJson(
            XBOX_AUTH_ENDPOINT,
            buildJsonObject {
                put("Properties", buildJsonObject {
                    put("AuthMethod", JsonPrimitive("RPS"))
                    put("SiteName", JsonPrimitive("user.auth.xboxlive.com"))
                    put("RpsTicket", JsonPrimitive("d=${microsoftToken.accessToken}"))
                })
                put("RelyingParty", JsonPrimitive("http://auth.xboxlive.com"))
                put("TokenType", JsonPrimitive("JWT"))
            },
        )
        val xboxToken = xbox.requiredString("Token")
        val userHash = xbox.xui().requiredString("uhs")
        val xuid = xbox.xui().optionalString("xid").orEmpty()
        val xsts = postJson(
            XSTS_ENDPOINT,
            buildJsonObject {
                put("Properties", buildJsonObject {
                    put("SandboxId", JsonPrimitive("RETAIL"))
                    put("UserTokens", buildJsonArray { add(JsonPrimitive(xboxToken)) })
                })
                put("RelyingParty", JsonPrimitive("rp://api.minecraftservices.com/"))
                put("TokenType", JsonPrimitive("JWT"))
            },
        )
        val minecraft = postJson(
            MINECRAFT_LOGIN_ENDPOINT,
            buildJsonObject {
                put("identityToken", JsonPrimitive("XBL3.0 x=$userHash;${xsts.requiredString("Token")}"))
            },
        )
        val minecraftAccessToken = minecraft.requiredString("access_token")
        return authenticatedResult(
            profile = minecraftProfile(minecraftAccessToken),
            method = method,
            accessToken = SecretValue(minecraftAccessToken),
            xuid = xuid,
            serializedState = SecretValue(
                authJson.encodeToString(
                    IosAuthenticationState(
                        refreshToken = microsoftToken.refreshToken,
                        expiresAtEpochMillis = nowMillis() + minecraft.requiredLong("expires_in") * 1_000L,
                    ),
                ),
            ),
        )
    }

    private suspend fun accessTokenResult(secret: SecretValue): AuthenticatedMinecraftAccount {
        val token = secret.reveal().trim()
        return authenticatedResult(
            profile = minecraftProfile(token),
            method = AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN,
            accessToken = SecretValue(token),
            serializedState = SecretValue(token),
        )
    }

    private fun authenticatedResult(
        profile: MinecraftProfile,
        method: AccountAuthenticationMethod,
        accessToken: SecretValue,
        xuid: String = "",
        serializedState: SecretValue,
    ) = AuthenticatedMinecraftAccount(
        edition = MinecraftEdition.JAVA,
        profile = LauncherAccount(
            profileId = profile.id,
            playerName = profile.name,
            edition = MinecraftEdition.JAVA,
            authenticationMethod = method,
            lastAuthenticatedAtEpochMillis = nowMillis(),
        ),
        javaSession = AuthSession(
            playerName = profile.name,
            profileId = profile.id,
            accessToken = accessToken,
            clientId = OfficialMinecraftApplications.java.clientId,
            xuid = xuid,
        ),
        serializedState = serializedState,
    )

    private suspend fun minecraftProfile(accessToken: String): MinecraftProfile {
        val response = httpClient.get(MINECRAFT_PROFILE_ENDPOINT) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.UserAgent, BuildInfo.USER_AGENT)
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) {
            "Minecraft profile request failed with HTTP ${response.status.value}."
        }
        val profile = authJson.parseToJsonElement(body).jsonObject
        return MinecraftProfile(profile.requiredString("id"), profile.requiredString("name"))
    }

    private fun offlineResult(rawUsername: String): AuthenticatedMinecraftAccount {
        val username = rawUsername.trim()
        require(OFFLINE_USERNAME.matches(username)) {
            "Offline usernames must contain 1 to 16 letters, numbers, or underscores."
        }
        val uuid = "OfflinePlayer:$username".encodeUtf8().md5().toByteArray().also { bytes ->
            bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
            bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        }.joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        return AuthenticatedMinecraftAccount(
            edition = MinecraftEdition.JAVA,
            profile = LauncherAccount(
                profileId = uuid,
                playerName = username,
                authenticationMethod = AccountAuthenticationMethod.OFFLINE,
            ),
            javaSession = AuthSession(username, uuid, null, userType = "legacy"),
            serializedState = SecretValue("offline:$uuid"),
        )
    }

    private suspend fun postForm(url: String, vararg values: Pair<String, String>): JsonObject {
        val response = httpClient.post(url) {
            header(HttpHeaders.UserAgent, BuildInfo.USER_AGENT)
            setBody(FormDataContent(Parameters.build { values.forEach { (name, value) -> append(name, value) } }))
        }
        val body = authJson.parseToJsonElement(response.bodyAsText()).jsonObject
        if (!response.status.isSuccess()) {
            when (body.optionalString("error")) {
                "authorization_pending" -> throw MicrosoftAuthorizationPending()
                "slow_down" -> throw MicrosoftAuthorizationSlowDown()
            }
            error(body.optionalString("error_description") ?: "Microsoft authentication failed with HTTP ${response.status.value}.")
        }
        return body
    }

    private suspend fun postJson(url: String, document: JsonObject): JsonObject {
        val response = httpClient.post(url) {
            header(HttpHeaders.UserAgent, BuildInfo.USER_AGENT)
            if (url == XBOX_AUTH_ENDPOINT || url == XSTS_ENDPOINT) {
                header("x-xbl-contract-version", "1")
            }
            contentType(ContentType.Application.Json)
            setBody(document.toString())
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "Minecraft authentication failed with HTTP ${response.status.value}." }
        return authJson.parseToJsonElement(body).jsonObject
    }

    private suspend fun <T> execute(operation: String, block: suspend () -> T): T = try {
        block().also { logger.info("accounts", "Minecraft authentication completed", mapOf("operation" to operation)) }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        logger.error("accounts", "Minecraft authentication failed", error, mapOf("operation" to operation))
        throw IllegalStateException(error.message ?: "Account authentication failed.", error)
    }

    private fun unsupported(method: AccountAuthenticationMethod): Nothing =
        error("${method.label} is not available on iOS. Use Microsoft device code, a refresh token, or an offline profile.")

    private fun JsonObject.toMicrosoftToken() = MicrosoftToken(
        accessToken = requiredString("access_token"),
        refreshToken = requiredString("refresh_token"),
    )

    private fun JsonObject.xui(): JsonObject =
        getValue("DisplayClaims").jsonObject.getValue("xui").jsonArray.first().jsonObject

    private fun JsonObject.requiredString(name: String): String =
        optionalString(name) ?: error("Authentication response is missing $name.")

    private fun JsonObject.optionalString(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

    private fun JsonObject.requiredLong(name: String): Long =
        optionalLong(name) ?: error("Authentication response is missing $name.")

    private fun JsonObject.optionalLong(name: String): Long? =
        (get(name) as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

    private data class MicrosoftToken(val accessToken: String, val refreshToken: String)
    private data class MinecraftProfile(val id: String, val name: String)
    private class MicrosoftAuthorizationPending : Exception()
    private class MicrosoftAuthorizationSlowDown : Exception()

    @Serializable
    private data class IosAuthenticationState(
        val refreshToken: String,
        val expiresAtEpochMillis: Long,
    )

    private companion object {
        val authJson = Json { ignoreUnknownKeys = true }
        val OFFLINE_USERNAME = Regex("^[A-Za-z0-9_]{1,16}$")
        const val DEVICE_CODE_ENDPOINT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
        const val TOKEN_ENDPOINT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        const val XBOX_AUTH_ENDPOINT = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_ENDPOINT = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val MINECRAFT_LOGIN_ENDPOINT = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val MINECRAFT_PROFILE_ENDPOINT = "https://api.minecraftservices.com/minecraft/profile"
    }
}
