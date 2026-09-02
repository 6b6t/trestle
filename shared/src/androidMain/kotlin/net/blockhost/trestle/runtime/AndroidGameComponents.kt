package net.blockhost.trestle.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.download.DownloadProgress
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import okio.FileSystem
import okio.Path
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

internal data class AndroidGameComponents(
    val classpath: List<Path>,
    val nativeDirectory: Path,
)

internal class AndroidGameComponentManager(
    private val directories: LauncherDirectories,
    private val bundledAssets: AndroidBundledRuntimeAssets,
    private val fileSystem: FileSystem,
    private val logger: LauncherLogger,
) {
    suspend fun resolve(
        abi: AndroidRuntimeAbi,
        onProgress: suspend (DownloadProgress) -> Unit = {},
    ): AndroidGameComponents {
        val componentSetId = "minecraft-26.2-android-${abi.releaseName}-5"
        val root = directories.runtimes / componentSetId
        val jars = root / "jars"
        val natives = root / "natives"
        val marker = root / ".complete"
        val classpath = REQUIRED_JARS.map { jars / it }
        if (
            fileSystem.exists(marker) &&
            classpath.all(fileSystem::exists) &&
            REQUIRED_NATIVE_FILES.all { fileSystem.exists(natives / it) }
        ) {
            prepareLibrariesForLoading(natives, abi)
            return AndroidGameComponents(classpath, natives)
        }

        withContext(Dispatchers.IO) {
            try {
                deleteTree(root)
                fileSystem.createDirectories(root)
                bundledAssets.install(abi.releaseName, "classpath", jars, onProgress)
                bundledAssets.install(abi.releaseName, "native", natives, onProgress)
                val missingJars = REQUIRED_JARS.filterNot { fileSystem.exists(jars / it) }
                val missingNatives = REQUIRED_NATIVE_FILES.filterNot { fileSystem.exists(natives / it) }
                if (missingJars.isNotEmpty() || missingNatives.isNotEmpty()) {
                    throw LauncherException.RuntimeUnavailable(
                        "The bundled Android runtime is incomplete. Missing " +
                            (missingJars + missingNatives).joinToString() + ".",
                    )
                }
                prepareLibrariesForLoading(natives, abi)
                fileSystem.write(marker) {
                    writeUtf8("$componentSetId\n$AMETHYST_REVISION\n")
                    flush()
                }
                logger.info(
                    "runtime",
                    "Installed bundled Android game components",
                    mapOf("componentSet" to componentSetId, "source" to AMETHYST_REVISION),
                )
            } catch (error: LauncherException) {
                deleteTree(root)
                throw error
            } catch (error: Exception) {
                deleteTree(root)
                throw LauncherException.FileSystem("The bundled Android game components could not be installed.", error)
            }
        }
        return AndroidGameComponents(classpath, natives)
    }

    private fun prepareLibrariesForLoading(natives: Path, abi: AndroidRuntimeAbi) {
        val libraries = fileSystem.list(natives).filter { it.name.endsWith(".so") }
        libraries.forEach(abi::verifyLibrary)
        libraries.forEach(AndroidNativeLibraryPermissions::makeReadOnly)
    }

    private fun deleteTree(path: Path) {
        if (!fileSystem.exists(path)) return
        if (fileSystem.metadata(path).isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

    private companion object {
        const val AMETHYST_REVISION = "d8a195640a7e0929f2ee532d7784de2b980c6c48"
        val REQUIRED_JARS = listOf(
            "lwjgl.jar",
            "lwjgl-3.4.1-merged-modules.jar",
            "lwjgl-freetype.jar",
            "lwjgl-nanovg.jar",
            "lwjgl-openal.jar",
            "lwjgl-shaderc.jar",
            "lwjgl-spng.jar",
            "lwjgl-spvc.jar",
            "lwjgl-stb.jar",
            "lwjgl-tinyfd.jar",
            "lwjgl-vma.jar",
            "lwjgl-vulkan.jar",
        )
        val REQUIRED_NATIVE_FILES = listOf(
            "libc++_shared.so",
            "liblwjgl.so",
            "liblwjgl_opengl.so",
            "liblwjgl_stb.so",
            "libfreetype.so",
            "libopenal.so",
            "libjnidispatch.so",
            "libpojavexec.so",
            "libspirv-cross-c-shared.so",
            "libEGL_mesa.so",
            "libglapi.so",
            "libglxshim.so",
            "libmobileglues.so",
            "libzink_dri.so",
        )
    }
}

internal object AndroidNativeLibraryPermissions {
    private val WRITE_PERMISSIONS = setOf(
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.OTHERS_WRITE,
    )

    fun makeReadOnly(library: Path) {
        val path = java.nio.file.Path.of(library.toString())
        Files.setPosixFilePermissions(
            path,
            Files.getPosixFilePermissions(path) - WRITE_PERMISSIONS,
        )
    }
}
