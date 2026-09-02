package net.blockhost.trestle.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionReference>,
)

@Serializable
data class LatestVersions(
    val release: String,
    val snapshot: String,
)

@Serializable
data class VersionReference(
    val id: String,
    val type: String,
    val url: String,
    val sha1: String? = null,
    val time: String? = null,
    @SerialName("releaseTime") val releaseTime: String? = null,
)

@Serializable
data class VersionMetadata(
    val id: String,
    val type: String = "release",
    val mainClass: String,
    val assets: String? = null,
    val assetIndex: AssetIndexReference? = null,
    val downloads: VersionDownloads = VersionDownloads(),
    val libraries: List<MojangLibrary> = emptyList(),
    val logging: Map<String, LoggingConfiguration> = emptyMap(),
    val arguments: ModernArguments? = null,
    val minecraftArguments: String? = null,
    val javaVersion: JavaVersionRequirement? = null,
    val inheritsFrom: String? = null,
)

@Serializable
data class JavaVersionRequirement(
    val component: String? = null,
    val majorVersion: Int = 8,
)

@Serializable
data class ModernArguments(
    val game: List<JsonElement> = emptyList(),
    val jvm: List<JsonElement> = emptyList(),
)

@Serializable
data class VersionDownloads(
    val client: DownloadReference? = null,
    val clientMappings: DownloadReference? = null,
    val server: DownloadReference? = null,
    val serverMappings: DownloadReference? = null,
)

@Serializable
data class DownloadReference(
    val sha1: String? = null,
    val size: Long? = null,
    val url: String,
    val path: String? = null,
)

@Serializable
data class AssetIndexReference(
    val id: String,
    val sha1: String? = null,
    val size: Long? = null,
    @SerialName("totalSize") val totalSize: Long? = null,
    val url: String,
)

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject>,
    @SerialName("map_to_resources") val mapToResources: Boolean = false,
    val virtual: Boolean = false,
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long,
)

@Serializable
data class MojangLibrary(
    val name: String,
    val downloads: LibraryDownloads? = null,
    val natives: Map<String, String> = emptyMap(),
    val rules: List<MojangRule> = emptyList(),
    val extract: ExtractionRules? = null,
    val url: String? = null,
)

@Serializable
data class LibraryDownloads(
    val artifact: DownloadReference? = null,
    val classifiers: Map<String, DownloadReference> = emptyMap(),
)

@Serializable
data class ExtractionRules(val exclude: List<String> = emptyList())

@Serializable
data class LoggingConfiguration(
    val argument: String,
    val file: DownloadReference,
    val type: String,
)

@Serializable
data class MojangRule(
    val action: RuleAction,
    val os: RuleOs? = null,
    val features: Map<String, Boolean> = emptyMap(),
)

@Serializable
enum class RuleAction {
    @SerialName("allow") ALLOW,
    @SerialName("disallow") DISALLOW,
}

@Serializable
data class RuleOs(
    val name: String? = null,
    val arch: String? = null,
    val version: String? = null,
)

enum class OperatingSystem(val ruleName: String) {
    WINDOWS("windows"),
    MACOS("osx"),
    IOS("osx"),
    LINUX("linux"),
    UNKNOWN("unknown"),
}

enum class Architecture(val aliases: Set<String>, val bits: Int) {
    X86_64(setOf("x86_64", "amd64", "x64"), 64),
    X86(setOf("x86", "i386", "i486", "i586", "i686"), 32),
    ARM64(setOf("aarch64", "arm64", "arm64-v8a"), 64),
    ARM32(setOf("arm", "arm32", "armeabi", "armeabi-v7a"), 32),
    UNKNOWN(emptySet(), 0),
}

data class PlatformEnvironment(
    val operatingSystem: OperatingSystem,
    val architecture: Architecture,
    val osVersion: String = "",
    val features: Map<String, Boolean> = emptyMap(),
)
