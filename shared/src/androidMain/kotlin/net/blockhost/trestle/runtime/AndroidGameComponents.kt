package net.blockhost.trestle.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.download.DownloadRequest
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import okio.FileSystem
import okio.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

internal data class AndroidGameComponents(
    val classpath: List<Path>,
    val nativeDirectory: Path,
)

internal class AndroidGameComponentManager(
    private val directories: LauncherDirectories,
    private val downloadPipeline: DownloadPipeline,
    private val fileSystem: FileSystem,
    private val logger: LauncherLogger,
) {
    private val remoteComponentInstaller = AndroidRemoteComponentInstaller()

    suspend fun resolve(onProgress: suspend (DownloadProgress) -> Unit = {}): AndroidGameComponents {
        val root = directories.runtimes / COMPONENT_SET_ID
        val downloads = root / "downloads"
        val jars = root / "jars"
        val natives = root / "natives"
        val marker = root / ".complete"
        val classpath = COMPONENT_JARS.map { jars / it.name }
        if (
            fileSystem.exists(marker) &&
            classpath.all(fileSystem::exists) &&
            REQUIRED_NATIVE_FILES.all { fileSystem.exists(natives / it) }
        ) {
            return AndroidGameComponents(classpath, natives)
        }

        val artifacts = COMPONENT_JARS + COMPONENT_ARCHIVES
        downloadPipeline.download(
            requests = artifacts.map { artifact ->
                DownloadRequest(
                    url = artifact.url,
                    destination = downloads / artifact.name,
                    size = artifact.size,
                    sha256 = artifact.sha256,
                    progressLabel = "Downloading Android game components",
                )
            },
            stagingDirectory = directories.staging / "android-game-components",
            onProgress = onProgress,
        )

        withContext(Dispatchers.IO) {
            try {
                fileSystem.delete(marker, mustExist = false)
                fileSystem.createDirectories(jars)
                COMPONENT_JARS.forEach { artifact ->
                    val source = downloads / artifact.name
                    val destination = jars / artifact.name
                    if (fileSystem.exists(destination)) fileSystem.delete(destination)
                    fileSystem.copy(source, destination)
                }
                resetDirectory(natives)
                COMPONENT_ARCHIVES.forEach { artifact ->
                    extractArm64Libraries(downloads / artifact.name, natives)
                }
                remoteComponentInstaller.install(
                    sourceUrl = AMETHYST_APK_URL,
                    components = REMOTE_NATIVE_COMPONENTS,
                    destination = natives,
                    onProgress = onProgress,
                )
                val missing = REQUIRED_NATIVE_FILES.filterNot { fileSystem.exists(natives / it) }
                if (missing.isNotEmpty()) {
                    throw LauncherException.RuntimeUnavailable(
                        "The Android game components are missing ${missing.joinToString()}.",
                    )
                }
                fileSystem.write(marker) {
                    writeUtf8("$COMPONENT_SET_ID\n$AMETHYST_REVISION\n")
                    flush()
                }
                runCatching { deleteTree(downloads) }
                logger.info(
                    "runtime",
                    "Installed Android game components",
                    mapOf("componentSet" to COMPONENT_SET_ID, "source" to AMETHYST_REVISION),
                )
            } catch (error: LauncherException) {
                throw error
            } catch (error: Exception) {
                throw LauncherException.FileSystem("The Android game components could not be installed.", error)
            }
        }
        return AndroidGameComponents(classpath, natives)
    }

    private fun extractArm64Libraries(archive: Path, destination: Path) {
        ZipFile(archive.toString()).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.startsWith("assets/licenses/")) {
                    val licenseName = entry.name.removePrefix("assets/licenses/")
                    val licenseRoot = java.nio.file.Path.of(
                        (requireNotNull(destination.parent) / "licenses" / archive.name).toString(),
                    ).normalize()
                    val licenseTarget = licenseRoot.resolve(licenseName).normalize()
                    if (!licenseTarget.startsWith(licenseRoot)) {
                        throw LauncherException.InvalidMetadata("An Android component contains an unsafe license path.")
                    }
                    Files.createDirectories(requireNotNull(licenseTarget.parent))
                    zip.getInputStream(entry).use { input ->
                        Files.copy(input, licenseTarget, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                val relative = when {
                    entry.name.startsWith("jni/arm64-v8a/") -> entry.name.substringAfterLast('/')
                    entry.name.startsWith("lib/arm64-v8a/") -> entry.name.substringAfterLast('/')
                    entry.name.startsWith("assets/components/lwjgl-3.4.1-natives/arm64-v8a/") ->
                        entry.name.substringAfterLast('/')
                    else -> continue
                }
                if (entry.isDirectory || relative.isBlank() || !relative.endsWith(".so")) continue
                val target = java.nio.file.Path.of((destination / relative).toString()).normalize()
                val root = java.nio.file.Path.of(destination.toString()).normalize()
                if (!target.startsWith(root)) {
                    throw LauncherException.InvalidMetadata("An Android component contains an unsafe path.")
                }
                Files.createDirectories(requireNotNull(target.parent))
                zip.getInputStream(entry).use { input ->
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun resetDirectory(path: Path) {
        deleteTree(path)
        fileSystem.createDirectories(path)
    }

    private fun deleteTree(path: Path) {
        if (!fileSystem.exists(path)) return
        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

    private data class ComponentArtifact(
        val name: String,
        val relativePath: String,
        val size: Long,
        val sha256: String,
    ) {
        val url: String
            get() = "$RAW_AMETHYST_ROOT/$relativePath"
    }

    private companion object {
        const val AMETHYST_REVISION = "d8a195640a7e0929f2ee532d7784de2b980c6c48"
        const val COMPONENT_SET_ID = "minecraft-26.2-android-arm64-2"
        const val RAW_AMETHYST_ROOT =
            "https://raw.githubusercontent.com/AngelAuraMC/Amethyst-Android/$AMETHYST_REVISION"
        const val JAR_ROOT = "app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1"
        const val AAR_ROOT = "app_pojavlauncher/libs"

        val COMPONENT_JARS = listOf(
            ComponentArtifact("lwjgl.jar", "$JAR_ROOT/lwjgl.jar", 1_169_477, "a436d01be183cd77887c2eb8ed3ebf6a031dc6a70fc7d68ec1662f50e3ea54f6"),
            ComponentArtifact("lwjgl-3.4.1-merged-modules.jar", "$JAR_ROOT/lwjgl-3.4.1-merged-modules.jar", 1_105_331, "08584aeadb90fec11e0a2d96077dfafb609cd7b735327fe1153e0575add464c9"),
            ComponentArtifact("lwjgl-freetype.jar", "$JAR_ROOT/lwjgl-freetype.jar", 465_796, "a1993bc7d6f9f72715a4b457715911bd9268d04869d494f20c19884e7c8dbe05"),
            ComponentArtifact("lwjgl-nanovg.jar", "$JAR_ROOT/lwjgl-nanovg.jar", 76_348, "28dede1a39356bbd731d0a18110cadb9041e32dd7adc633bcbde944bb8201ff5"),
            ComponentArtifact("lwjgl-openal.jar", "$JAR_ROOT/lwjgl-openal.jar", 150_981, "10467797e14b478eb06dd51259eeceafec4a317fdaff0d713a848abe28fd0c78"),
            ComponentArtifact("lwjgl-shaderc.jar", "$JAR_ROOT/lwjgl-shaderc.jar", 147_618, "5dc22d389927e58eed04704d200cc091a9b5fb211daa31e8be1d07d880ffb7f8"),
            ComponentArtifact("lwjgl-spng.jar", "$JAR_ROOT/lwjgl-spng.jar", 114_650, "6b9f99dde99376efc045d47b2c39afe2a7155375e29e3785da38d1c9879b4ac8"),
            ComponentArtifact("lwjgl-spvc.jar", "$JAR_ROOT/lwjgl-spvc.jar", 141_553, "d5a00514d1d20ffbe1eaf120b3a2c6c6d8115ea23c2d757e6fa25b684242bcbd"),
            ComponentArtifact("lwjgl-stb.jar", "$JAR_ROOT/lwjgl-stb.jar", 136_953, "e2656fcb59554ec518a8ecf5d5b0cbb544a69a2eab58b427ee52c006e35a737b"),
            ComponentArtifact("lwjgl-tinyfd.jar", "$JAR_ROOT/lwjgl-tinyfd.jar", 7_677, "fa4a421127c062ac51789f0f69be57fa68958d17309bde9a370e2862a1efc6dc"),
            ComponentArtifact("lwjgl-vma.jar", "$JAR_ROOT/lwjgl-vma.jar", 103_366, "e4b550cf500996fa48abd54d36226e5b5f4ceb157ee284681b5a00d202fd9e4f"),
            ComponentArtifact("lwjgl-vulkan.jar", "$JAR_ROOT/lwjgl-vulkan.jar", 8_540_645, "997c1d80d0e5f0698c66f3644c901ac95923b3bfeb539f2ff53d6b47cb237776"),
        )
        val COMPONENT_ARCHIVES = listOf(
            ComponentArtifact("lwjgl-3.4.1-natives-release.aar", "$AAR_ROOT/lwjgl-3.4.1-natives-release.aar", 16_567_622, "ccb9c7abe942cd40a0490637ca70756a259a40ec1257a515d3343c2f536503c0"),
            ComponentArtifact("openal-soft-release.aar", "$AAR_ROOT/openal-soft-release.aar", 2_895_870, "45e630695b6b4c6506704330bf4da80a605b445ea5d187d7b71a370aab5494ea"),
            ComponentArtifact("kopper-zink-release.aar", "$AAR_ROOT/kopper-zink-release.aar", 16_002_238, "bf816fc9dc2047edff0284369b6433260ec462b7b26a3e3b544550c721ca26fe"),
        )
        const val AMETHYST_APK_URL =
            "https://github.com/AngelAuraMC/Amethyst-Android/releases/download/1.1.6/Amethyst.apk"
        val REMOTE_NATIVE_COMPONENTS = listOf(
            RemoteDeflatedComponent(
                "libc++_shared.so",
                rangeStart = 7_178_448,
                compressedSize = 406_559,
                uncompressedSize = 1_292_904,
                sha256 = "f4e1e97c1943e60311e47e8b024d78f5b3b7229b3ccc65feb33af83d6025a670",
            ),
            RemoteDeflatedComponent(
                "libpojavexec.so",
                rangeStart = 14_612_714,
                compressedSize = 26_875,
                uncompressedSize = 67_128,
                sha256 = "46025ba51fa0720ddf9449f2686aa36f19837d30179c8052e12311769fa11bd3",
            ),
            RemoteDeflatedComponent(
                "libspirv-cross-c-shared.so",
                rangeStart = 14_644_574,
                compressedSize = 1_188_950,
                uncompressedSize = 3_463_112,
                sha256 = "9f7a21ae51739d8cfe8b3a0ebb8d6e55cfea1cf95effbf991a13ca50436e185a",
            ),
        )
        val REQUIRED_NATIVE_FILES = listOf(
            "libc++_shared.so",
            "liblwjgl.so",
            "liblwjgl_opengl.so",
            "liblwjgl_stb.so",
            "libfreetype.so",
            "libopenal.so",
            "libpojavexec.so",
            "libspirv-cross-c-shared.so",
            "libEGL_mesa.so",
            "libglapi.so",
            "libglxshim.so",
            "libzink_dri.so",
        )
    }
}
