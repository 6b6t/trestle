package net.blockhost.trestle.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_proxy_port_must_be_between_and
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text
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
        requireUser(port in 1..65535) { text(Res.string.msg_proxy_port_must_be_between_and) }
    }
}

@Serializable
data class FolderPreferences(val instances: String = "", val runtimes: String = "", val skins: String = "", val downloads: String = "")

@Serializable
data class ContentPreferences(
    val scanSubfolders: Boolean = false,
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
data class ConsolePreferences(val historyLimit: Int = 100_000, val stopLoggingOnOverflow: Boolean = true) {
    init {
        require(historyLimit in 100..1_000_000)
    }
}

/**
 * The interface language. [tag] is the BCP 47 tag, or null to follow the system. [nativeName] is the name the
 * language picker shows, written in that language so speakers can find it from any interface language.
 */
enum class LauncherLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)"),
    RUSSIAN("ru", "Русский"),
    POLISH("pl", "Polski"),
    TURKISH("tr", "Türkçe"),
    JAPANESE("ja", "日本語"),
    CHINESE_SIMPLIFIED("zh-CN", "简体中文"),
    ARABIC("ar", "العربية"),
    PERSIAN("fa", "فارسی"),
    HEBREW("he", "עברית"),
    URDU("ur", "اردو"),
    ;

    companion object {
        /** Reads a saved preference. Older versions saved the English label instead of the name. */
        fun fromPreference(value: String): LauncherLanguage =
            entries.firstOrNull { it.name == value } ?: if (value == "English") ENGLISH else SYSTEM

        /**
         * The language for a BCP 47 [tag] from the platform, such as Android's per-app language. A region Trestle
         * has no translation for falls back to the language alone, and an unknown or blank tag means [SYSTEM].
         */
        fun fromLanguageTag(tag: String): LauncherLanguage {
            if (tag.isBlank()) return SYSTEM
            // Older Android versions report Hebrew with its legacy code.
            val language = tag.substringBefore('-').lowercase().let { if (it == "iw") "he" else it }
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                ?: entries.firstOrNull { it.tag?.substringBefore('-') == language }
                ?: SYSTEM
        }
    }
}

@Serializable
data class UpdatePreferences(
    val automaticChecks: Boolean = true,
    val includePrereleases: Boolean = false,
    val lastCheckedAtMillis: Long = 0,
    val remindAfterMillis: Long = 0,
)

@Serializable
data class LauncherPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val language: String = LauncherLanguage.SYSTEM.name,
    val instanceSort: InstanceSortMode = InstanceSortMode.NAME,
    val folders: FolderPreferences = FolderPreferences(),
    val content: ContentPreferences = ContentPreferences(),
    val updates: UpdatePreferences = UpdatePreferences(),
    val network: NetworkPreferences = NetworkPreferences(),
    val console: ConsolePreferences = ConsolePreferences(),
    val proxy: ProxyPreferences = ProxyPreferences(),
    val technicClientId: String = "",
    val ftbAppInstancesPath: String = "",
) {
    val launcherLanguage: LauncherLanguage get() = LauncherLanguage.fromPreference(language)
}

class LauncherPreferencesStore(private val fileSystem: FileSystem, private val path: Path) {
    fun read(): LauncherPreferences {
        if (!fileSystem.exists(path)) return LauncherPreferences()
        return runCatching { fileSystem.read(path) { readUtf8() } }
            .mapCatching { content ->
                runCatching { preferencesJson.decodeFromString<LauncherPreferences>(content) }
                    .getOrElse { decodeReadableSections(content) }
            }
            .getOrDefault(LauncherPreferences())
    }

    /**
     * Keeps every top-level section that still decodes when another one does not, for example an option value
     * written by a newer Trestle. Without this, one bad value would reset every setting, including custom folders.
     */
    private fun decodeReadableSections(content: String): LauncherPreferences {
        val saved = preferencesJson.parseToJsonElement(content).jsonObject
        val readable = saved.filter { (key, value) ->
            runCatching {
                preferencesJson.decodeFromJsonElement(LauncherPreferences.serializer(), JsonObject(mapOf(key to value)))
            }.isSuccess
        }
        return preferencesJson.decodeFromJsonElement(LauncherPreferences.serializer(), JsonObject(readable))
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
