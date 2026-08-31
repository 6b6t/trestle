package net.blockhost.trestle.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import net.blockhost.trestle.app.BuildInfo
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig
import net.raphimc.minecraftauth.msa.model.MsaCredentials
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier
import net.raphimc.minecraftauth.java.model.MinecraftToken
import net.raphimc.minecraftauth.java.request.MinecraftProfileRequest
import net.raphimc.minecraftauth.util.jwt.Jwt
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.function.Consumer

class JvmMinecraftAuthenticator(
    private val bedrockConfiguration: MinecraftApplicationConfiguration,
    private val nowMillis: () -> Long,
    private val logger: LauncherLogger = NoopLauncherLogger,
) : MinecraftAuthenticator {
    private val httpClient = MinecraftAuth.createHttpClient(BuildInfo.USER_AGENT)

    override suspend fun authenticate(
        request: AccountLoginRequest,
        onDeviceAuthorization: (DeviceAuthorization) -> Unit,
    ): AuthenticatedMinecraftAccount = execute("sign-in", request.edition) {
        when (request) {
            is AccountLoginRequest.DeviceCode -> when (request.edition) {
                MinecraftEdition.JAVA -> javaResult(
                    AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                    JavaAuthManager.create(httpClient)
                        .msaApplicationConfig(OfficialMinecraftApplications.java.toMsaConfiguration())
                        .deviceType(OfficialMinecraftApplications.java.deviceType)
                        .login(deviceCodeService(), deviceCodeConsumer(onDeviceAuthorization)),
                )
                MinecraftEdition.BEDROCK -> bedrockResult(
                    AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE,
                    BedrockAuthManager.create(httpClient, requireBedrockVersion(request.bedrockGameVersion))
                        .msaApplicationConfig(bedrockConfiguration.toMsaConfiguration())
                        .deviceType(bedrockConfiguration.deviceType)
                        .login(deviceCodeService(), deviceCodeConsumer(onDeviceAuthorization)),
                )
            }
            is AccountLoginRequest.Credentials -> when (request.edition) {
                MinecraftEdition.JAVA -> javaResult(
                    AccountAuthenticationMethod.MICROSOFT_CREDENTIALS,
                    JavaAuthManager.create(httpClient)
                        .msaApplicationConfig(OfficialMinecraftApplications.java.toMsaConfiguration())
                        .deviceType(OfficialMinecraftApplications.java.deviceType)
                        .login(credentialsService(), MsaCredentials(request.email.trim(), request.password.reveal())),
                )
                MinecraftEdition.BEDROCK -> bedrockResult(
                    AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS,
                    BedrockAuthManager.create(httpClient, requireBedrockVersion(request.bedrockGameVersion))
                        .msaApplicationConfig(bedrockConfiguration.toMsaConfiguration())
                        .deviceType(bedrockConfiguration.deviceType)
                        .login(credentialsService(), MsaCredentials(request.email.trim(), request.password.reveal())),
                )
            }
            is AccountLoginRequest.SecretImport -> when (request.method) {
                AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN -> javaResult(
                    request.method,
                    JavaAuthManager.create(httpClient)
                        .msaApplicationConfig(OfficialMinecraftApplications.java.toMsaConfiguration())
                        .deviceType(OfficialMinecraftApplications.java.deviceType)
                        .login(request.secret.reveal()),
                )
                AccountAuthenticationMethod.MICROSOFT_COOKIES -> javaResult(
                    request.method,
                    JavaAuthManager.create(httpClient)
                        .msaApplicationConfig(OfficialMinecraftApplications.java.toMsaConfiguration())
                        .deviceType(OfficialMinecraftApplications.java.deviceType)
                        .login(cookieService(), request.secret.reveal()),
                )
                AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> accessTokenResult(request.secret)
                else -> error("${request.method.label} is not a secret-import method.")
            }
            is AccountLoginRequest.Offline -> offlineResult(request.username)
        }
    }

    override suspend fun restore(
        profile: LauncherAccount,
        serializedState: SecretValue,
    ): AuthenticatedMinecraftAccount = execute("restore", profile.edition) {
        when (profile.authenticationMethod) {
            AccountAuthenticationMethod.OFFLINE -> return@execute offlineResult(profile.playerName)
            AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> return@execute accessTokenResult(serializedState)
            else -> Unit
        }
        val json = com.google.gson.JsonParser.parseString(serializedState.reveal()).asJsonObject
        when (profile.edition) {
            MinecraftEdition.JAVA -> javaResult(profile.authenticationMethod, JavaAuthManager.fromJson(httpClient, json))
            MinecraftEdition.BEDROCK -> bedrockResult(
                profile.authenticationMethod,
                BedrockAuthManager.fromJson(
                    httpClient,
                    requireBedrockVersion(profile.bedrockGameVersion),
                    json,
                ),
            )
        }
    }

    private fun javaResult(
        method: AccountAuthenticationMethod,
        manager: JavaAuthManager,
    ): AuthenticatedMinecraftAccount {
        val minecraftToken = manager.minecraftToken.upToDate
        val minecraftProfile = manager.minecraftProfile.upToDate
        val xboxProfile = manager.xboxUserProfile.upToDate
        val profileId = minecraftProfile.id.toString().replace("-", "")
        return AuthenticatedMinecraftAccount(
            edition = MinecraftEdition.JAVA,
            profile = LauncherAccount(
                profileId = profileId,
                playerName = minecraftProfile.name,
                edition = MinecraftEdition.JAVA,
                authenticationMethod = method,
                lastAuthenticatedAtEpochMillis = nowMillis(),
            ),
            javaSession = AuthSession(
                playerName = minecraftProfile.name,
                profileId = profileId,
                accessToken = SecretValue(minecraftToken.token),
                clientId = OfficialMinecraftApplications.java.clientId,
                xuid = xboxProfile.id,
            ),
            serializedState = SecretValue(JavaAuthManager.toJson(manager).toString()),
        )
    }

    private fun bedrockResult(
        method: AccountAuthenticationMethod,
        manager: BedrockAuthManager,
    ): AuthenticatedMinecraftAccount {
        manager.bedrockXstsToken.upToDate
        val xboxProfile = manager.xboxUserProfile.upToDate
        return AuthenticatedMinecraftAccount(
            edition = MinecraftEdition.BEDROCK,
            profile = LauncherAccount(
                profileId = xboxProfile.id,
                playerName = xboxProfile.settings["Gamertag"] ?: xboxProfile.id,
                edition = MinecraftEdition.BEDROCK,
                authenticationMethod = method,
                bedrockGameVersion = manager.gameVersion,
                lastAuthenticatedAtEpochMillis = nowMillis(),
            ),
            javaSession = null,
            serializedState = SecretValue(BedrockAuthManager.toJson(manager).toString()),
        )
    }

    private fun accessTokenResult(secret: SecretValue): AuthenticatedMinecraftAccount {
        val token = secret.reveal().trim()
        val expiresAt = Jwt.parse(token).expireTimeMs
        require(expiresAt > nowMillis()) { "The imported Minecraft access token has expired." }
        val profile = httpClient.executeAndHandle(MinecraftProfileRequest(MinecraftToken(expiresAt, "Bearer", token)))
        val profileId = profile.id.toString().replace("-", "")
        return AuthenticatedMinecraftAccount(
            edition = MinecraftEdition.JAVA,
            profile = LauncherAccount(
                profileId = profileId,
                playerName = profile.name,
                edition = MinecraftEdition.JAVA,
                authenticationMethod = AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN,
                lastAuthenticatedAtEpochMillis = nowMillis(),
            ),
            javaSession = AuthSession(
                playerName = profile.name,
                profileId = profileId,
                accessToken = secret,
            ),
            serializedState = secret,
        )
    }

    private fun offlineResult(rawUsername: String): AuthenticatedMinecraftAccount {
        val username = rawUsername.trim()
        require(OFFLINE_USERNAME.matches(username)) {
            "Offline usernames must contain 1 to 16 letters, numbers, or underscores."
        }
        val profileId = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "")
        return AuthenticatedMinecraftAccount(
            edition = MinecraftEdition.JAVA,
            profile = LauncherAccount(
                profileId = profileId,
                playerName = username,
                edition = MinecraftEdition.JAVA,
                authenticationMethod = AccountAuthenticationMethod.OFFLINE,
            ),
            javaSession = AuthSession(
                playerName = username,
                profileId = profileId,
                accessToken = null,
                userType = "legacy",
            ),
            serializedState = SecretValue("offline:$profileId"),
        )
    }

    private fun deviceCodeService() =
        ParamMsaAuthServiceSupplier<Consumer<MsaDeviceCode>> { client, configuration, callback ->
            DeviceCodeMsaAuthService(client, configuration, callback)
        }

    private fun credentialsService() =
        ParamMsaAuthServiceSupplier<MsaCredentials> { client, configuration, credentials ->
            CredentialsMsaAuthService(client, configuration, credentials)
        }

    private fun cookieService() =
        ParamMsaAuthServiceSupplier<String> { client, configuration, cookies ->
            CookieImportMsaAuthService(client, configuration, cookies)
        }

    private fun deviceCodeConsumer(callback: (DeviceAuthorization) -> Unit) = Consumer<MsaDeviceCode> { code ->
        callback(
            DeviceAuthorization(
                userCode = code.userCode,
                verificationUri = code.verificationUri,
                directVerificationUri = code.directVerificationUri,
                expiresAtEpochMillis = code.expireTimeMs,
            ),
        )
    }

    private suspend fun <T> execute(operation: String, edition: MinecraftEdition, block: () -> T): T =
        runInterruptible(Dispatchers.IO) {
            try {
                block().also {
                    logger.info(
                        "accounts",
                        "Minecraft authentication completed",
                        mapOf("operation" to operation, "edition" to edition.name),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                throw CancellationException("Minecraft authentication was cancelled.").also { it.initCause(error) }
            } catch (error: Exception) {
                logger.error(
                    "accounts",
                    "Minecraft authentication failed",
                    error,
                    mapOf("operation" to operation, "edition" to edition.name),
                )
                throw IllegalStateException(
                    "Account authentication failed. Check the selected method and account details, then try again.",
                    error,
                )
            }
        }

    private fun MinecraftApplicationConfiguration.toMsaConfiguration() = MsaApplicationConfig(clientId, scope)

    private fun requireBedrockVersion(version: String?): String =
        requireNotNull(version?.trim()?.takeIf(String::isNotBlank)) {
            "A Bedrock game version is required for Bedrock authentication."
        }

    private companion object {
        val OFFLINE_USERNAME = Regex("^[A-Za-z0-9_]{1,16}$")
    }
}
