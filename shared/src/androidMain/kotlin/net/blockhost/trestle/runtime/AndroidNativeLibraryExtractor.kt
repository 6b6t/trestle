package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.LauncherException
import okio.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

internal object AndroidNativeLibraryExtractor {
    fun extract(archive: Path, destination: Path, abi: AndroidRuntimeAbi) {
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
                    entry.name.startsWith("jni/${abi.directoryName}/") -> entry.name.substringAfterLast('/')
                    entry.name.startsWith("lib/${abi.directoryName}/") -> entry.name.substringAfterLast('/')
                    entry.name.startsWith("assets/components/lwjgl-3.4.1-natives/${abi.directoryName}/") ->
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
                abi.verifyLibrary(destination / relative)
            }
        }
    }
}
