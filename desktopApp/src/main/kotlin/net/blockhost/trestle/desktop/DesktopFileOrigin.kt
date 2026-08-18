package net.blockhost.trestle.desktop

import com.sun.jna.Platform
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.UserDefinedFileAttributeView

/** Reads download provenance written by browsers without changing the imported file. */
internal object DesktopFileOrigin {
    fun label(path: Path): String? = readUrl(path)?.let { value ->
        val host = runCatching { URI(value).host }.getOrNull()
        if (host.isNullOrBlank()) value.take(120) else "Downloaded from $host"
    }

    private fun readUrl(path: Path): String? = when {
        Platform.isWindows() -> readWindowsZone(path)
        Platform.isMac() -> readMacWhereFrom(path)
        else -> readExtendedAttribute(path)
    }

    private fun readWindowsZone(path: Path): String? = runCatching {
        Files.readAllLines(Path.of("$path:Zone.Identifier"))
            .firstNotNullOfOrNull { line ->
                line.substringAfter('=', "").takeIf {
                    line.startsWith("HostUrl=", ignoreCase = true) && it.isNotBlank()
                }
            }
    }.getOrNull()

    private fun readMacWhereFrom(path: Path): String? = runCatching {
        val process = ProcessBuilder("mdls", "-raw", "-name", "kMDItemWhereFroms", path.toString())
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().use { it.readText() }
            .lineSequence()
            .flatMap { URL_PATTERN.findAll(it).map(MatchResult::value) }
            .firstOrNull()
    }.getOrNull()

    private fun readExtendedAttribute(path: Path): String? = runCatching {
        val view = Files.getFileAttributeView(path, UserDefinedFileAttributeView::class.java)
        val name = view.list().firstOrNull { attribute ->
            attribute in setOf("xdg.origin.url", "origin.url", "metadata::download-uri")
        } ?: return null
        val buffer = ByteBuffer.allocate(view.size(name))
        view.read(name, buffer)
        buffer.flip()
        StandardCharsets.UTF_8.decode(buffer).toString().trimEnd('\u0000').takeIf(String::isNotBlank)
    }.getOrNull()

    private val URL_PATTERN = Regex("https?://[^\\s\\\"),]+")
}
