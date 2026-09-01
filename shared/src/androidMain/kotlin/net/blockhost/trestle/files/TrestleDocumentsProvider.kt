package net.blockhost.trestle.files

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class TrestleDocumentsProvider : DocumentsProvider() {
    private val providerContext: Context
        get() = requireNotNull(context)

    private val rootDirectory: File
        get() = TrestleDocumentPaths.instancesDirectory(providerContext)

    override fun onCreate(): Boolean {
        rootDirectory.mkdirs()
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val columns = projection?.copyOf() ?: DEFAULT_ROOT_PROJECTION
        return MatrixCursor(columns).apply {
            newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, TrestleDocumentPaths.ROOT_ID)
                add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, TrestleDocumentPaths.ROOT_DOCUMENT_ID)
                add(DocumentsContract.Root.COLUMN_TITLE, "Trestle")
                add(DocumentsContract.Root.COLUMN_SUMMARY, "Minecraft instances and game files")
                add(DocumentsContract.Root.COLUMN_ICON, providerContext.applicationInfo.icon)
                add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_LOCAL_ONLY or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
                add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
                add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, rootDirectory.usableSpace)
            }
        }
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val columns = projection?.copyOf() ?: DEFAULT_DOCUMENT_PROJECTION
        return MatrixCursor(columns).apply {
            includeDocument(resolve(documentId), documentId)
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor = queryChildren(parentDocumentId, projection)

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        queryArgs: Bundle?,
    ): Cursor = queryChildren(parentDocumentId, projection)

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        if (mode != "r") throw UnsupportedOperationException("Trestle game files are read-only.")
        val file = resolve(documentId)
        if (!file.isFile) throw FileNotFoundException("The requested document is not a file.")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean = runCatching {
        val parent = resolve(parentDocumentId).toPath()
        val child = resolve(documentId).toPath()
        child != parent && child.startsWith(parent)
    }.getOrDefault(false)

    private fun queryChildren(parentDocumentId: String, projection: Array<out String>?): Cursor {
        val parent = resolve(parentDocumentId)
        if (!parent.isDirectory) throw FileNotFoundException("The requested document is not a directory.")
        val columns = projection?.copyOf() ?: DEFAULT_DOCUMENT_PROJECTION
        return MatrixCursor(columns).apply {
            parent.listFiles().orEmpty()
                .mapNotNull { child ->
                    runCatching { TrestleDocumentPaths.documentIdFor(rootDirectory, child) }
                        .getOrNull()
                        ?.let { documentId -> child to documentId }
                }
                .sortedWith(compareBy<Pair<File, String>>({ !it.first.isDirectory }, { it.first.name.lowercase() }))
                .forEach { (child, documentId) -> includeDocument(child, documentId) }
        }
    }

    private fun MatrixCursor.includeDocument(file: File, documentId: String) {
        newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
            add(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                if (documentId == TrestleDocumentPaths.ROOT_DOCUMENT_ID) "Trestle" else file.name,
            )
            add(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                if (file.isDirectory) {
                    DocumentsContract.Document.MIME_TYPE_DIR
                } else {
                    TrestleDocumentPaths.mimeTypeFor(file)
                },
            )
            add(DocumentsContract.Document.COLUMN_FLAGS, 0)
            add(DocumentsContract.Document.COLUMN_SIZE, if (file.isFile) file.length() else null)
            add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
        }
    }

    private fun resolve(documentId: String): File = try {
        TrestleDocumentPaths.fileFor(rootDirectory, documentId).also { file ->
            if (!file.exists()) throw FileNotFoundException("The requested document does not exist.")
        }
    } catch (error: IllegalArgumentException) {
        throw FileNotFoundException("The requested document is outside Trestle storage.").apply {
            initCause(error)
        }
    }

    private companion object {
        val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
        )

        val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}

internal object TrestleDocumentPaths {
    const val ROOT_ID = "trestle"
    const val ROOT_DOCUMENT_ID = "trestle-root"

    fun authority(packageName: String): String = "$packageName.documents"

    fun instancesDirectory(context: Context): File = context.filesDir.resolve("trestle/instances").canonicalFile

    fun mimeTypeFor(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "cfg", "log", "properties", "txt" -> "text/plain"
            "json", "mcmeta" -> "application/json"
            "jar" -> "application/java-archive"
            "toml" -> "application/toml"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }

    fun documentIdFor(root: File, target: File): String {
        val canonicalRoot = root.canonicalFile
        val canonicalTarget = target.canonicalFile
        require(canonicalTarget.toPath().startsWith(canonicalRoot.toPath())) {
            "The file is outside Trestle's instance storage."
        }
        if (canonicalTarget == canonicalRoot) return ROOT_DOCUMENT_ID
        val relativePath = canonicalTarget.relativeTo(canonicalRoot).invariantSeparatorsPath
        return "$ROOT_DOCUMENT_ID/$relativePath"
    }

    fun fileFor(root: File, documentId: String): File {
        val canonicalRoot = root.canonicalFile
        if (documentId == ROOT_DOCUMENT_ID) return canonicalRoot
        require(documentId.startsWith("$ROOT_DOCUMENT_ID/")) { "Unknown Trestle document ID." }
        val relativePath = documentId.removePrefix("$ROOT_DOCUMENT_ID/")
        require(relativePath.isNotBlank()) { "The document path is empty." }
        val target = canonicalRoot.resolve(relativePath).canonicalFile
        require(target.toPath().startsWith(canonicalRoot.toPath())) {
            "The document path escapes Trestle's instance storage."
        }
        return target
    }
}
