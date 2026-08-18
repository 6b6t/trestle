package net.blockhost.trestle.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

@Serializable
enum class ThemePreference(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
}

@Serializable
enum class InstanceSortMode(val label: String) {
    NAME("Name"),
    LAST_LAUNCHED("Last launched"),
}

@Serializable
enum class LauncherProxyType(val label: String) {
    SYSTEM("Use system settings"),
    NONE("None"),
    SOCKS5("SOCKS5"),
    HTTP("HTTP"),
}

@Serializable
data class ProxyPreferences(
    val type: LauncherProxyType = LauncherProxyType.SYSTEM,
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val username: String = "",
    val password: String = "",
) {
    init {
        require(port in 1..65535) { "Proxy port must be between 1 and 65535." }
    }
}

@Serializable
data class FolderPreferences(
    val instances: String = "",
    val runtimes: String = "",
    val skins: String = "",
    val downloads: String = "",
)

@Serializable
data class ContentPreferences(
    val scanSubfolders: Boolean = false,
    val moveBlockedFiles: Boolean = false,
    val trackMetadata: Boolean = true,
    val installDependencies: Boolean = true,
    val detectIncompatibilities: Boolean = true,
    val suggestModpackUpdates: Boolean = true,
)

@Serializable
data class NetworkPreferences(
    val concurrentTasks: Int = 10,
    val concurrentDownloads: Int = 6,
    val retryLimit: Int = 3,
    val httpTimeoutSeconds: Int = 60,
) {
    init {
        require(concurrentTasks in 1..64)
        require(concurrentDownloads in 1..32)
        require(retryLimit in 1..10)
        require(httpTimeoutSeconds in 5..900)
    }
}

@Serializable
data class ConsolePreferences(
    val historyLimit: Int = 100_000,
    val stopLoggingOnOverflow: Boolean = true,
) {
    init {
        require(historyLimit in 100..1_000_000)
    }
}

@Serializable
data class LauncherPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val language: String = "System default",
    val instanceSort: InstanceSortMode = InstanceSortMode.NAME,
    val folders: FolderPreferences = FolderPreferences(),
    val content: ContentPreferences = ContentPreferences(),
    val network: NetworkPreferences = NetworkPreferences(),
    val console: ConsolePreferences = ConsolePreferences(),
    val proxy: ProxyPreferences = ProxyPreferences(),
    val technicClientId: String = "",
    val ftbAppInstancesPath: String = "",
)

class LauncherPreferencesStore(
    private val fileSystem: FileSystem,
    private val path: Path,
) {
    fun read(): LauncherPreferences {
        if (!fileSystem.exists(path)) return LauncherPreferences()
        return runCatching {
            preferencesJson.decodeFromString<LauncherPreferences>(fileSystem.read(path) { readUtf8() })
        }.getOrDefault(LauncherPreferences())
    }

    fun write(preferences: LauncherPreferences) {
        fileSystem.createDirectories(requireNotNull(path.parent))
        val temporary = requireNotNull(path.parent) / ".${path.name}.tmp"
        fileSystem.write(temporary) {
            writeUtf8(preferencesJson.encodeToString(LauncherPreferences.serializer(), preferences))
            flush()
        }
        fileSystem.atomicMove(temporary, path)
    }

    private companion object {
        val preferencesJson = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
