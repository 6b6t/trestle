package net.blockhost.trestle.auth

class SecretValue(private val value: String) {
    init {
        require(value.isNotBlank())
    }

    internal fun reveal(): String = value

    override fun toString(): String = "[REDACTED]"
}

data class AuthSession(
    val playerName: String,
    val profileId: String,
    val accessToken: SecretValue?,
    val userType: String = "msa",
    val clientId: String = "",
    val xuid: String = "",
    val authenticationMethod: AccountAuthenticationMethod = AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
)

interface SessionProvider {
    suspend fun currentSession(profileId: String? = null): AuthSession?
}

object NoSessionProvider : SessionProvider {
    override suspend fun currentSession(profileId: String?): AuthSession? = null
}
