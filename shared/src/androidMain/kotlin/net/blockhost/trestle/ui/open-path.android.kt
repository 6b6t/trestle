package net.blockhost.trestle.ui

import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File
import net.blockhost.trestle.files.TrestleDocumentPaths

internal actual val supportsOpenPath: Boolean = true

@Composable
internal actual fun rememberOpenPath(): (String) -> Unit {
    val context = LocalContext.current
    val instancesDirectory = remember(context) { TrestleDocumentPaths.instancesDirectory(context) }
    var request by remember { mutableStateOf<AndroidGameFileRequest?>(null) }
    request?.let { currentRequest ->
        GameFileBrowserDialog(
            rootPath = currentRequest.rootPath,
            initialPath = currentRequest.initialPath,
            onDismiss = { request = null },
            onOpenExternal = { path -> openAndroidGameFile(context, instancesDirectory, path) },
        )
    }
    return remember(context) {
        { path ->
            runCatching {
                val target = File(path).canonicalFile
                if (!target.exists()) return@runCatching
                TrestleDocumentPaths.documentIdFor(instancesDirectory, target)
                val relative = target.relativeTo(instancesDirectory)
                val instanceDirectory = relative.invariantSeparatorsPath.substringBefore('/')
                    .takeIf(String::isNotBlank)
                    ?.let(instancesDirectory::resolve)
                    ?.canonicalFile
                    ?: instancesDirectory
                request = AndroidGameFileRequest(
                    rootPath = instanceDirectory.path,
                    initialPath = target.path,
                )
            }
        }
    }
}

private fun openAndroidGameFile(
    context: android.content.Context,
    instancesDirectory: File,
    path: String,
): Result<Unit> = runCatching {
    val target = File(path).canonicalFile
    require(target.isFile) { "Choose a file to open with another app." }
    val documentId = TrestleDocumentPaths.documentIdFor(instancesDirectory, target)
    val documentUri = DocumentsContract.buildDocumentUri(
        TrestleDocumentPaths.authority(context.packageName),
        documentId,
    )
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(documentUri, TrestleDocumentPaths.mimeTypeFor(target))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(viewIntent, null))
}

private data class AndroidGameFileRequest(
    val rootPath: String,
    val initialPath: String,
)
