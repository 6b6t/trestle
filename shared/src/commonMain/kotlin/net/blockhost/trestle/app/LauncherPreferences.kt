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
data class LauncherPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
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
