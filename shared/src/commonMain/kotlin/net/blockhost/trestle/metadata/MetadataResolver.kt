package net.blockhost.trestle.metadata

import kotlinx.serialization.Serializable
import net.blockhost.trestle.domain.LauncherException

@Serializable
data class ResolvedLibrary(
    val name: String,
    val path: String,
    val url: String,
    val sha1: String?,
    val size: Long?,
    val native: Boolean,
    val extractionExcludes: List<String> = emptyList(),
    val classpath: Boolean = true,
)

data class ResolvedVersion(
    val metadata: VersionMetadata,
    val client: DownloadReference,
    val libraries: List<ResolvedLibrary>,
    val assetIndex: AssetIndexReference?,
    val logging: LoggingConfiguration?,
    val requiredJavaMajor: Int,
    val gameArguments: List<String>,
    val jvmArguments: List<String>,
)

@Serializable
data class InstalledVersion(
    val metadata: VersionMetadata,
    val libraries: List<ResolvedLibrary>,
    val requiredJavaMajor: Int,
    val gameArguments: List<String>,
    val jvmArguments: List<String>,
    val assetIndexId: String? = null,
    val loggingPath: String? = null,
)

object MinecraftMetadataResolver {
    fun resolve(metadata: VersionMetadata, environment: PlatformEnvironment): ResolvedVersion {
        val client = metadata.downloads.client
            ?: throw LauncherException.InvalidMetadata("Version ${metadata.id} has no client download.")
        val libraries = resolveLibraries(metadata.libraries, environment)

        val modern = metadata.arguments
        val gameArguments = metadata.minecraftArguments?.let(::parseLegacyArguments)
            ?: modern?.let { MojangArguments.resolve(it.game, environment) }.orEmpty()
        val jvmArguments = modern?.let { MojangArguments.resolve(it.jvm, environment) }.orEmpty()

        return ResolvedVersion(
            metadata = metadata,
            client = client,
            libraries = libraries,
            assetIndex = metadata.assetIndex,
            logging = metadata.logging["client"],
            requiredJavaMajor = metadata.javaVersion?.majorVersion ?: 8,
            gameArguments = gameArguments,
            jvmArguments = jvmArguments,
        )
    }

    fun resolveLibraries(
        libraries: List<MojangLibrary>,
        environment: PlatformEnvironment,
        classpath: Boolean = true,
    ): List<ResolvedLibrary> = buildList {
        for (library in libraries) {
            if (!MojangRuleEvaluator.allows(library.rules, environment)) continue
            library.downloads?.artifact?.let { artifact ->
                add(
                    ResolvedLibrary(
                        name = library.name,
                        path = artifact.path ?: MavenCoordinate.parse(library.name).path(),
                        url = artifact.url,
                        sha1 = artifact.sha1,
                        size = artifact.size,
                        native = false,
                        classpath = classpath,
                    ),
                )
            } ?: run {
                val repository = library.url ?: OFFICIAL_LIBRARY_REPOSITORY
                val path = MavenCoordinate.parse(library.name).path()
                add(
                    ResolvedLibrary(
                        name = library.name,
                        path = path,
                        url = repository.trimEnd('/') + "/" + path,
                        sha1 = null,
                        size = null,
                        native = false,
                        classpath = classpath,
                    ),
                )
            }

            val classifierTemplate = library.natives[environment.operatingSystem.ruleName] ?: continue
            val classifier = classifierTemplate.replace("\${arch}", environment.architecture.bits.toString())
            val native = library.downloads?.classifiers?.get(classifier) ?: continue
            add(
                ResolvedLibrary(
                    name = "${library.name}:$classifier",
                    path = native.path ?: nativeCoordinatePath(library.name, classifier),
                    url = native.url,
                    sha1 = native.sha1,
                    size = native.size,
                    native = true,
                    extractionExcludes = library.extract?.exclude.orEmpty(),
                    classpath = classpath,
                ),
            )
        }
    }.distinctBy { it.path }

    fun merge(base: VersionMetadata, overlay: VersionMetadata): VersionMetadata {
        require(overlay.inheritsFrom == null || overlay.inheritsFrom == base.id) {
            "Metadata overlay does not inherit from ${base.id}."
        }
        return base.copy(
            id = overlay.id,
            mainClass = overlay.mainClass,
            libraries = (overlay.libraries + base.libraries).distinctBy(::libraryModuleKey),
            arguments = mergeArguments(base.arguments, overlay.arguments),
            minecraftArguments = overlay.minecraftArguments ?: base.minecraftArguments,
            javaVersion = overlay.javaVersion ?: base.javaVersion,
        )
    }

    private fun mergeArguments(base: ModernArguments?, overlay: ModernArguments?): ModernArguments? {
        if (base == null) return overlay
        if (overlay == null) return base
        return ModernArguments(
            game = base.game + overlay.game,
            jvm = base.jvm + overlay.jvm,
        )
    }

    private fun nativeCoordinatePath(name: String, classifier: String): String {
        val base = MavenCoordinate.parse(name)
        return base.copy(classifier = classifier).path()
    }

    private fun libraryModuleKey(library: MojangLibrary): String {
        val coordinate = MavenCoordinate.parse(library.name)
        return listOf(
            coordinate.group,
            coordinate.artifact,
            coordinate.classifier.orEmpty(),
            coordinate.extension,
        ).joinToString(":")
    }

    private const val OFFICIAL_LIBRARY_REPOSITORY = "https://libraries.minecraft.net/"
}

fun AssetIndex.downloads(): List<DownloadReference> = objects.values.distinctBy { it.hash }.map { asset ->
    val prefix = asset.hash.take(2)
    DownloadReference(
        sha1 = asset.hash,
        size = asset.size,
        url = "https://resources.download.minecraft.net/$prefix/${asset.hash}",
        path = "objects/$prefix/${asset.hash}",
    )
}
