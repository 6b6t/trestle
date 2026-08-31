package net.blockhost.trestle.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import okio.FileSystem
import okio.Path

@Serializable
enum class SkinVariant(val apiValue: String) {
    CLASSIC("classic"),
    SLIM("slim"),
}

@Serializable
data class AccountSkin(
    val id: String,
    val url: String,
    val variant: SkinVariant = SkinVariant.CLASSIC,
)

@Serializable
data class LauncherAccount(
    val profileId: String,
    val playerName: String,
    val edition: MinecraftEdition = MinecraftEdition.JAVA,
    val authenticationMethod: AccountAuthenticationMethod = AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
    val bedrockGameVersion: String? = null,
    val skin: AccountSkin? = null,
    val lastAuthenticatedAtEpochMillis: Long? = null,
)

data class ManagedAccount(
    val profile: LauncherAccount,
    val isActive: Boolean,
    val isAuthenticated: Boolean,
) {
    val isReady: Boolean
        get() = isAuthenticated || profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE
}

@Serializable
private data class AccountRegistry(
    val schemaVersion: Int = 1,
    val activeProfileId: String? = null,
    val accounts: List<LauncherAccount> = emptyList(),
)

interface AccountManager : SessionProvider {
    val accounts: StateFlow<List<ManagedAccount>>

    suspend fun initialize()
    suspend fun register(session: AuthSession, profile: LauncherAccount)
    suspend fun addAccount(
        request: AccountLoginRequest,
        onDeviceAuthorization: (DeviceAuthorization) -> Unit,
    )
    suspend fun select(profileId: String)
    suspend fun signOut(profileId: String)
    suspend fun remove(profileId: String)
    suspend fun updateProfile(profile: LauncherAccount)
}

class FileAccountManager(
    private val fileSystem: FileSystem,
    private val registryPath: Path,
    private val logger: LauncherLogger = NoopLauncherLogger,
    private val credentialStore: AccountCredentialStore? = null,
    private val authenticator: MinecraftAuthenticator? = null,
) : AccountManager {
    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, AuthSession>()
    private val authenticatedProfiles = mutableSetOf<String>()
    private var profiles = emptyList<LauncherAccount>()
    private var activeProfileId: String? = null
    private val mutableAccounts = MutableStateFlow<List<ManagedAccount>>(emptyList())

    override val accounts: StateFlow<List<ManagedAccount>> = mutableAccounts.asStateFlow()

    override suspend fun initialize() {
        mutex.withLock {
            try {
                fileSystem.createDirectories(requireNotNull(registryPath.parent))
                if (!fileSystem.exists(registryPath)) {
                    persist()
                    return@withLock
                }
                val registry = readRegistry()
                if (registry.schemaVersion != 1) {
                    throw LauncherException.FileSystem(
                        "Account registry schema ${registry.schemaVersion} is not supported.",
                    )
                }
                profiles = registry.accounts.distinctBy { it.profileId }
                activeProfileId = registry.activeProfileId?.takeIf { id -> profiles.any { it.profileId == id } }
                publish()
                logger.info("accounts", "Loaded account profiles", mapOf("count" to profiles.size))
            } catch (error: LauncherException) {
                throw error
            } catch (error: Exception) {
                logger.error("accounts", "Could not load account profiles", error)
                throw LauncherException.FileSystem("The account registry could not be read.", error)
            }
        }
        restoreCredentials()
    }

    override suspend fun register(session: AuthSession, profile: LauncherAccount) = mutex.withLock {
        require(session.profileId == profile.profileId) { "Session and account profile IDs do not match." }
        sessions[profile.profileId] = session
        authenticatedProfiles += profile.profileId
        profiles = (profiles.filterNot { it.profileId == profile.profileId } + profile)
            .sortedBy { it.playerName.lowercase() }
        if (activeProfileId == null) activeProfileId = profile.profileId
        persist()
        logger.info("accounts", "Registered account", mapOf("player" to profile.playerName))
    }

    override suspend fun addAccount(
        request: AccountLoginRequest,
        onDeviceAuthorization: (DeviceAuthorization) -> Unit,
    ) {
        val availableAuthenticator = checkNotNull(authenticator) { "Account authentication is not available." }
        register(availableAuthenticator.authenticate(request, onDeviceAuthorization), select = true)
    }

    override suspend fun select(profileId: String) = mutex.withLock {
        require(profiles.any { it.profileId == profileId }) { "Account $profileId does not exist." }
        activeProfileId = profileId
        persist()
        logger.info("accounts", "Selected account", mapOf("profileId" to profileId))
    }

    override suspend fun signOut(profileId: String) = mutex.withLock {
        profiles.firstOrNull { it.profileId == profileId }?.let { profile ->
            if (profile.authenticationMethod != AccountAuthenticationMethod.OFFLINE) {
                credentialStore?.remove(profileId, profile.edition)
            }
        }
        sessions.remove(profileId)
        authenticatedProfiles -= profileId
        publish()
        logger.info("accounts", "Signed out account", mapOf("profileId" to profileId))
    }

    override suspend fun remove(profileId: String) = mutex.withLock {
        profiles.firstOrNull { it.profileId == profileId }?.let { profile ->
            if (profile.authenticationMethod != AccountAuthenticationMethod.OFFLINE) {
                credentialStore?.remove(profileId, profile.edition)
            }
        }
        sessions.remove(profileId)
        authenticatedProfiles -= profileId
        profiles = profiles.filterNot { it.profileId == profileId }
        if (activeProfileId == profileId) activeProfileId = profiles.firstOrNull()?.profileId
        persist()
        logger.info("accounts", "Removed account profile", mapOf("profileId" to profileId))
    }

    override suspend fun updateProfile(profile: LauncherAccount) = mutex.withLock {
        require(profiles.any { it.profileId == profile.profileId }) { "Account ${profile.profileId} does not exist." }
        profiles = profiles.map { if (it.profileId == profile.profileId) profile else it }
        persist()
    }

    override suspend fun currentSession(profileId: String?): AuthSession? = mutex.withLock {
        (profileId ?: activeProfileId)?.let(sessions::get)
    }

    private fun readRegistry(): AccountRegistry {
        val document = accountJson.parseToJsonElement(fileSystem.read(registryPath) { readUtf8() }).jsonObject
        val accounts = document["accounts"]?.jsonArray ?: JsonArray(emptyList())
        val supportedAccounts = accounts.filter { account ->
            val method = account.jsonObject["authenticationMethod"]?.jsonPrimitive
            method == null || !method.isString || AccountAuthenticationMethod.entries.any { it.name == method.content }
        }
        if (supportedAccounts.size != accounts.size) {
            logger.warn(
                "accounts",
                "Skipped accounts with unsupported authentication methods",
                details = mapOf("count" to accounts.size - supportedAccounts.size),
            )
        }
        return accountJson.decodeFromJsonElement(
            JsonObject(document + ("accounts" to JsonArray(supportedAccounts))),
        )
    }

    private fun persist() {
        try {
            val temporaryPath = registryPath.parent!! / ".${registryPath.name}.tmp"
            fileSystem.write(temporaryPath) {
                writeUtf8(
                    accountJson.encodeToString(
                        AccountRegistry.serializer(),
                        AccountRegistry(activeProfileId = activeProfileId, accounts = profiles),
                    ),
                )
                flush()
            }
            fileSystem.atomicMove(temporaryPath, registryPath)
            publish()
        } catch (error: Exception) {
            logger.error("accounts", "Could not save account profiles", error)
            throw LauncherException.FileSystem("The account registry could not be saved.", error)
        }
    }

    private fun publish() {
        mutableAccounts.value = profiles.map { profile ->
            ManagedAccount(
                profile = profile,
                isActive = profile.profileId == activeProfileId,
                isAuthenticated = profile.profileId in authenticatedProfiles,
            )
        }
    }

    private suspend fun register(authentication: AuthenticatedMinecraftAccount, select: Boolean) {
        if (authentication.profile.authenticationMethod != AccountAuthenticationMethod.OFFLINE) {
            credentialStore?.write(
                authentication.profile.profileId,
                authentication.edition,
                authentication.serializedState,
            )
        }
        mutex.withLock {
            authentication.javaSession?.let { sessions[authentication.profile.profileId] = it }
            if (authentication.profile.authenticationMethod != AccountAuthenticationMethod.OFFLINE) {
                authenticatedProfiles += authentication.profile.profileId
            }
            profiles = (profiles.filterNot { it.profileId == authentication.profile.profileId } + authentication.profile)
                .sortedBy { it.playerName.lowercase() }
            if (select || activeProfileId == null) activeProfileId = authentication.profile.profileId
            persist()
            logger.info(
                "accounts",
                "Registered account",
                mapOf(
                    "player" to authentication.profile.playerName,
                    "edition" to authentication.edition.name,
                ),
            )
        }
    }

    private suspend fun restoreCredentials() {
        val availableAuthenticator = authenticator ?: return
        val availableStore = credentialStore
        val savedProfiles = mutex.withLock { profiles }
        coroutineScope {
            savedProfiles.map { savedProfile ->
                async {
                    try {
                        if (savedProfile.authenticationMethod == AccountAuthenticationMethod.OFFLINE) {
                            register(
                                availableAuthenticator.authenticate(
                                    AccountLoginRequest.Offline(savedProfile.playerName),
                                    onDeviceAuthorization = {},
                                ),
                                select = false,
                            )
                            return@async
                        }
                        val state = availableStore?.read(savedProfile.profileId, savedProfile.edition) ?: return@async
                        val restored = availableAuthenticator.restore(savedProfile, state)
                        register(
                            restored.copy(
                                profile = restored.profile.copy(
                                    skin = restored.profile.skin ?: savedProfile.skin,
                                ),
                            ),
                            select = false,
                        )
                    } catch (error: Exception) {
                        logger.warn(
                            "accounts",
                            "Saved account needs to sign in again",
                            error,
                            mapOf("profileId" to savedProfile.profileId, "edition" to savedProfile.edition.name),
                        )
                    }
                }
            }.awaitAll()
        }
    }
}

private val accountJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    ignoreUnknownKeys = true
}
