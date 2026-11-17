package net.blockhost.trestle.ui

import net.blockhost.trestle.platform.useOkio
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_files_larger_than_s_cannot_be_edited
import net.blockhost.trestle.resources.msg_the_breadcrumb_is_outside_the_current_directory
import net.blockhost.trestle.resources.msg_the_game_file_root_is_not_a
import net.blockhost.trestle.resources.msg_the_selected_path_is_not_a_directory
import net.blockhost.trestle.resources.msg_the_selected_path_is_not_a_file
import net.blockhost.trestle.resources.msg_the_selected_path_is_outside_this_instance
import net.blockhost.trestle.resources.msg_this_file_cannot_be_edited_as_text
import net.blockhost.trestle.resources.msg_this_file_changed_after_you_opened_it
import net.blockhost.trestle.resources.msg_this_file_type_cannot_be_edited
import net.blockhost.trestle.resources.ui_file_browser_read_only
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text
import net.blockhost.trestle.text.userError
import okio.Buffer
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

internal enum class GameFileType {
    DIRECTORY,
    TEXT,
    IMAGE,
    ARCHIVE,
    OTHER,
}

internal data class GameFileEntry(val path: String, val name: String, val type: GameFileType, val sizeBytes: Long?) {
    val isDirectory: Boolean
        get() = type == GameFileType.DIRECTORY
}

internal data class GameFileDirectory(val path: String, val relativeSegments: List<String>, val entries: List<GameFileEntry>)

internal data class GameTextDocument(
    val path: String,
    val relativePath: String,
    val name: String,
    val text: String,
    val editable: Boolean,
    val truncated: Boolean,
    val sizeBytes: Long,
    val lastModifiedAtMillis: Long?,
)

internal data class GameFileLocation(val directoryPath: String, val filePath: String?, val fileType: GameFileType?)

internal class GameFileBrowserFiles(rootPath: String, private val fileSystem: FileSystem = FileSystem.SYSTEM) {
    private val root: Path = fileSystem.canonicalize(rootPath.toPath()).also { path ->
        requireUser(fileSystem.metadata(path).isDirectory) { text(Res.string.msg_the_game_file_root_is_not_a) }
    }

    val rootPath: String
        get() = root.toString()

    fun locate(path: String): GameFileLocation {
        val target = requireInsideRoot(path.toPath())
        val metadata = fileSystem.metadata(target)
        return if (metadata.isDirectory) {
            GameFileLocation(target.toString(), null, null)
        } else {
            GameFileLocation(
                directoryPath = requireNotNull(target.parent).toString(),
                filePath = target.toString(),
                fileType = fileType(target, metadata.isDirectory),
            )
        }
    }

    fun list(directoryPath: String): GameFileDirectory {
        val directory = requireInsideRoot(directoryPath.toPath())
        requireUser(fileSystem.metadata(directory).isDirectory) { text(Res.string.msg_the_selected_path_is_not_a_directory) }
        val entries = fileSystem.list(directory)
            .mapNotNull { child ->
                runCatching {
                    val canonicalChild = requireInsideRoot(child)
                    val metadata = fileSystem.metadata(canonicalChild)
                    GameFileEntry(
                        path = canonicalChild.toString(),
                        name = canonicalChild.name,
                        type = fileType(canonicalChild, metadata.isDirectory),
                        sizeBytes = metadata.size.takeUnless { metadata.isDirectory },
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy<GameFileEntry>({ !it.isDirectory }, { it.name.lowercase() }, { it.name }))
        return GameFileDirectory(
            path = directory.toString(),
            relativeSegments = directory.relativeTo(root).segments,
            entries = entries,
        )
    }

    fun parentOf(directoryPath: String): String? {
        val directory = requireInsideRoot(directoryPath.toPath())
        if (directory == root) return null
        return directory.parent?.let(::requireInsideRoot)?.toString()
    }

    fun pathForBreadcrumb(directoryPath: String, segmentCount: Int): String {
        val directory = requireInsideRoot(directoryPath.toPath())
        val segments = directory.relativeTo(root).segments
        requireUser(segmentCount in 0..segments.size) { text(Res.string.msg_the_breadcrumb_is_outside_the_current_directory) }
        return segments.take(segmentCount).fold(root) { path, segment -> path.resolve(segment) }.toString()
    }

    fun readText(filePath: String): GameTextDocument {
        val file = requireInsideRoot(filePath.toPath())
        val metadata = fileSystem.metadata(file)
        requireUser(metadata.isRegularFile) { text(Res.string.msg_the_selected_path_is_not_a_file) }
        requireUser(isTextFile(file)) { text(Res.string.msg_this_file_cannot_be_edited_as_text) }
        val size = metadata.size ?: 0L
        val compressed = file.name.lowercase().endsWith(".gz")
        val (text, truncated) = if (compressed) {
            readCompressedTail(file)
        } else {
            readPlainTail(file, size)
        }
        return GameTextDocument(
            path = file.toString(),
            relativePath = file.relativeTo(root).toString(),
            name = file.name,
            text = text,
            editable = !compressed && !truncated && isEditableTextFile(file),
            truncated = truncated,
            sizeBytes = size,
            lastModifiedAtMillis = metadata.lastModifiedAtMillis,
        )
    }

    fun saveText(document: GameTextDocument, text: String): GameTextDocument {
        requireUser(document.editable) { text(Res.string.ui_file_browser_read_only) }
        val file = requireInsideRoot(document.path.toPath())
        requireUser(isEditableTextFile(file)) { text(Res.string.msg_this_file_type_cannot_be_edited) }
        val encodedSize = text.encodeToByteArray().size
        requireUser(encodedSize <= MAX_EDITABLE_TEXT_BYTES) {
            text(Res.string.msg_files_larger_than_s_cannot_be_edited, formatGameFileSize(MAX_EDITABLE_TEXT_BYTES.toLong()))
        }
        val currentMetadata = fileSystem.metadata(file)
        if (
            document.lastModifiedAtMillis != null &&
            currentMetadata.lastModifiedAtMillis != null &&
            document.lastModifiedAtMillis != currentMetadata.lastModifiedAtMillis
        ) {
            userError(text(Res.string.msg_this_file_changed_after_you_opened_it))
        }

        val temporary = requireNotNull(file.parent).resolve(".${file.name}.trestle-edit.tmp")
        try {
            fileSystem.write(temporary) { writeUtf8(text) }
            fileSystem.atomicMove(temporary, file)
        } finally {
            if (fileSystem.exists(temporary)) runCatching { fileSystem.delete(temporary) }
        }
        return readText(file.toString())
    }

    private fun readPlainTail(file: Path, size: Long): Pair<String, Boolean> {
        val truncated = size > MAX_TEXT_PREVIEW_BYTES
        val offset = if (truncated) size - MAX_TEXT_PREVIEW_BYTES else 0L
        val text = fileSystem.openReadOnly(file).useOkio { handle ->
            handle.source(offset).buffer().useOkio { source -> source.readUtf8() }
        }
        return trimPartialFirstLine(text, truncated) to truncated
    }

    private fun readCompressedTail(file: Path): Pair<String, Boolean> {
        val tail = Buffer()
        var truncated = false
        GzipSource(fileSystem.source(file)).buffer().useOkio { source ->
            while (true) {
                val read = source.read(tail, READ_CHUNK_BYTES)
                if (read == -1L) break
                if (tail.size > MAX_TEXT_PREVIEW_BYTES) {
                    tail.skip(tail.size - MAX_TEXT_PREVIEW_BYTES)
                    truncated = true
                }
            }
        }
        return trimPartialFirstLine(tail.readUtf8(), truncated) to truncated
    }

    private fun requireInsideRoot(path: Path): Path {
        val canonical = fileSystem.canonicalize(path)
        requireUser(
            canonical.root == root.root &&
                canonical.segments.size >= root.segments.size &&
                canonical.segments.take(root.segments.size) == root.segments,
        ) { text(Res.string.msg_the_selected_path_is_outside_this_instance) }
        return canonical
    }
}

internal fun isTextFile(path: Path): Boolean {
    val name = path.name.lowercase()
    return name.endsWith(".log.gz") || name.substringAfterLast('.', "") in TEXT_FILE_EXTENSIONS
}

internal fun formatGameFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${bytes / 107_374_182L / 10.0} GiB"
    bytes >= 1_048_576L -> "${bytes / 104_857L / 10.0} MiB"
    bytes >= 1_024L -> "${bytes / 102L / 10.0} KiB"
    else -> "$bytes B"
}

private fun isEditableTextFile(path: Path): Boolean = path.name.substringAfterLast('.', "").lowercase() in EDITABLE_TEXT_FILE_EXTENSIONS

private fun fileType(path: Path, isDirectory: Boolean): GameFileType {
    if (isDirectory) return GameFileType.DIRECTORY
    val extension = path.name.substringAfterLast('.', "").lowercase()
    return when {
        isTextFile(path) -> GameFileType.TEXT
        extension in IMAGE_FILE_EXTENSIONS -> GameFileType.IMAGE
        extension in ARCHIVE_FILE_EXTENSIONS -> GameFileType.ARCHIVE
        else -> GameFileType.OTHER
    }
}

private fun trimPartialFirstLine(text: String, truncated: Boolean): String {
    if (!truncated) return text
    val firstLineBreak = text.indexOf('\n')
    return if (firstLineBreak >= 0) text.substring(firstLineBreak + 1) else text
}

private const val MAX_EDITABLE_TEXT_BYTES = 256 * 1024
private const val MAX_TEXT_PREVIEW_BYTES = 256 * 1024L
private const val READ_CHUNK_BYTES = 16_384L

private val TEXT_FILE_EXTENSIONS = setOf(
    "cfg",
    "conf",
    "csv",
    "ini",
    "json",
    "json5",
    "log",
    "mcmeta",
    "md",
    "properties",
    "toml",
    "tsv",
    "txt",
    "xml",
    "yaml",
    "yml",
)

private val EDITABLE_TEXT_FILE_EXTENSIONS = TEXT_FILE_EXTENSIONS - setOf("log")
private val IMAGE_FILE_EXTENSIONS = setOf("gif", "jpeg", "jpg", "png", "webp")
private val ARCHIVE_FILE_EXTENSIONS = setOf("jar", "mrpack", "rar", "tar", "xz", "zip")
