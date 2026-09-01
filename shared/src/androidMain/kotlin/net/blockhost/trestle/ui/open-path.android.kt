package net.blockhost.trestle.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import net.blockhost.trestle.files.TrestleDocumentPaths

internal actual val supportsOpenPath: Boolean = true

@Composable
internal actual fun rememberOpenPath(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { path ->
            runCatching {
                val target = File(path).canonicalFile
                if (!target.exists()) return@runCatching
                val documentId = TrestleDocumentPaths.documentIdFor(
                    root = TrestleDocumentPaths.instancesDirectory(context),
                    target = target,
                )
                val documentUri = DocumentsContract.buildDocumentUri(
                    TrestleDocumentPaths.authority(context.packageName),
                    documentId,
                )
                val mimeType = if (target.isDirectory) {
                    DocumentsContract.Document.MIME_TYPE_DIR
                } else {
                    TrestleDocumentPaths.mimeTypeFor(target)
                }
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(documentUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(viewIntent)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = if (target.isDirectory) "*/*" else mimeType
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentUri)
                        },
                    )
                }
            }
        }
    }
}
