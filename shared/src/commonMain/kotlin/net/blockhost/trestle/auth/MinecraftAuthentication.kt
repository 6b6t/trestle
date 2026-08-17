package net.blockhost.trestle.auth

import eu.anifantakis.lib.ksafe.KSafe
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger

@kotlinx.serialization.Serializable
enum class MinecraftEdition {
    JAVA,
    BEDROCK,
}

@kotlinx.serialization.Serializable
enum class AccountAuthenticationMethod(val label: String, val edition: MinecraftEdition) {
    MICROSOFT_DEVICE_CODE("Java · Microsoft device code", MinecraftEdition.JAVA),
    MICROSOFT_BEDROCK_DEVICE_CODE("Bedrock · Microsoft device code", MinecraftEdition.BEDROCK),
    MICROSOFT_CREDENTIALS("Java · Microsoft email and password", MinecraftEdition.JAVA),
    MICROSOFT_BEDROCK_CREDENTIALS("Bedrock · Microsoft email and password", MinecraftEdition.BEDROCK),
    MICROSOFT_REFRESH_TOKEN("Java · Microsoft refresh token", MinecraftEdition.JAVA),
    MICROSOFT_COOKIES("Java · Microsoft cookies", MinecraftEdition.JAVA),
    MICROSOFT_ACCESS_TOKEN("Java · Minecraft access token", MinecraftEdition.JAVA),
    THE_ALTENING("TheAltening token", MinecraftEdition.JAVA),
    OFFLINE("Offline username", MinecraftEdition.JAVA),
}

sealed interface AccountLoginRequest {
    val method: AccountAuthenticationMethod
    val edition: MinecraftEdition get() = method.edition

    data class DeviceCode(
        override val method: AccountAuthenticationMethod,
        val bedrockGameVersion: String? = null,
    ) : AccountLoginRequest {
        init {
            require(
                method == AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE ||
                    method == AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE,
            )
        }
    }

    data class Credentials(
        override val method: AccountAuthenticationMethod,
        val email: String,
        val password: SecretValue,
        val bedrockGameVersion: String? = null,
    ) : AccountLoginRequest {
        init {
            require(
                method == AccountAuthenticationMethod.MICROSOFT_CREDENTIALS ||
                    method == AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS,
            )
        }
    }

    data class SecretImport(
        override val method: AccountAuthenticationMethod,
        val secret: SecretValue,
    ) : AccountLoginRequest {
        init {
            require(
                method == AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ||
                    method == AccountAuthenticationMethod.MICROSOFT_COOKIES ||
                    method == AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN ||
                    method == AccountAuthenticationMethod.THE_ALTENING,
            )
        }
    }

    data class Offline(val username: String) : AccountLoginRequest {
        override val method = AccountAuthenticationMethod.OFFLINE
    }
}

data class MinecraftApplicationConfiguration(
    val clientId: String,
    val scope: String,
    val deviceType: String,
)

object OfficialMinecraftApplications {
    const val TITLE_AUTH_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL"

    val java = MinecraftApplicationConfiguration(
        clientId = "00000000402b5328",
        scope = TITLE_AUTH_SCOPE,
        deviceType = "Win32",
    )
    val bedrockDesktop = MinecraftApplicationConfiguration(
        clientId = "0000000040159362",
        scope = TITLE_AUTH_SCOPE,
        deviceType = "Win32",
    )
    val bedrockAndroid = MinecraftApplicationConfiguration(
        clientId = "0000000048183522",
        scope = TITLE_AUTH_SCOPE,
        deviceType = "Android",
    )
}

data class DeviceAuthorization(
    val userCode: String,
    val verificationUri: String,
    val directVerificationUri: String,
    val expiresAtEpochMillis: Long,
)

data class AuthenticatedMinecraftAccount(
    val edition: MinecraftEdition,
    val profile: LauncherAccount,
    val javaSession: AuthSession?,
    val serializedState: SecretValue,
)

interface MinecraftAuthenticator {
    suspend fun authenticate(
        request: AccountLoginRequest,
        onDeviceAuthorization: (DeviceAuthorization) -> Unit,
    ): AuthenticatedMinecraftAccount

    suspend fun restore(
        profile: LauncherAccount,
        serializedState: SecretValue,
    ): AuthenticatedMinecraftAccount
}

interface AccountCredentialStore {
    val protection: CredentialProtection

    suspend fun read(profileId: String, edition: MinecraftEdition): SecretValue?
    suspend fun write(profileId: String, edition: MinecraftEdition, state: SecretValue)
    suspend fun remove(profileId: String, edition: MinecraftEdition)
}

data class CredentialProtection(
    val encryptionOperational: Boolean,
    val effectiveLevel: String,
    val intendedLevel: String,
    val notes: List<String>,
)

class KSafeAccountCredentialStore(
    private val vault: KSafe,
    private val logger: LauncherLogger = NoopLauncherLogger,
) : AccountCredentialStore {
    override val protection: CredentialProtection
        get() = vault.protectionInfo.let {
            CredentialProtection(
                encryptionOperational = it.isEncryptionOperational,
                effectiveLevel = it.effectiveLevel.name,
                intendedLevel = it.intendedLevel.name,
                notes = it.notes,
            )
        }

    override suspend fun read(profileId: String, edition: MinecraftEdition): SecretValue? {
        val value = vault.get(key(profileId, edition), "")
        return value.takeIf(String::isNotBlank)?.let(::SecretValue)
    }

    override suspend fun write(profileId: String, edition: MinecraftEdition, state: SecretValue) {
        check(protection.encryptionOperational) { "The secure credential vault is not available." }
        vault.put(key(profileId, edition), state.reveal())
        logger.info(
            "accounts",
            "Stored encrypted account credentials",
            mapOf("profileId" to profileId, "edition" to edition.name),
        )
    }

    override suspend fun remove(profileId: String, edition: MinecraftEdition) {
        vault.delete(key(profileId, edition))
        logger.info(
            "accounts",
            "Removed encrypted account credentials",
            mapOf("profileId" to profileId, "edition" to edition.name),
        )
    }

    private fun key(profileId: String, edition: MinecraftEdition): String =
        "account_${edition.name.lowercase()}_${profileId.lowercase().filter(Char::isLetterOrDigit)}"
}
