package net.blockhost.trestle.ui

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun Modifier.localFileDropTarget(
    enabled: Boolean,
    extensions: Set<String>,
    onActiveChange: (Boolean) -> Unit,
    onFiles: (List<LocalDroppedFile>) -> Unit,
    onFailure: (String) -> Unit,
): Modifier {
    if (!enabled) return this
    val currentExtensions = rememberUpdatedState(extensions.mapTo(mutableSetOf()) { it.lowercase() })
    val currentOnActiveChange = rememberUpdatedState(onActiveChange)
    val currentOnFiles = rememberUpdatedState(onFiles)
    val currentOnFailure = rememberUpdatedState(onFailure)
    val scope = rememberCoroutineScope()
    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) = currentOnActiveChange.value(true)

            override fun onEntered(event: DragAndDropEvent) = currentOnActiveChange.value(true)

            override fun onExited(event: DragAndDropEvent) = currentOnActiveChange.value(false)

            override fun onEnded(event: DragAndDropEvent) = currentOnActiveChange.value(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnActiveChange.value(false)
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                    ?.filterIsInstance<File>()
                    ?.filter { file ->
                        file.isFile && file.extension.lowercase() in currentExtensions.value
                    }
                    .orEmpty()
                if (files.isEmpty()) return false
                scope.launch {
                    val dropped = files.mapNotNull { file ->
                        if (file.length() > MAX_LOCAL_FILE_BYTES) {
                            currentOnFailure.value(file.name)
                            return@mapNotNull null
                        }
                        runCatching {
                            withContext(Dispatchers.IO) { LocalDroppedFile(file.name, file.readBytes()) }
                        }.onFailure {
                            currentOnFailure.value(file.name)
                        }.getOrNull()
                    }
                    if (dropped.isNotEmpty()) currentOnFiles.value(dropped)
                }
                return true
            }
        }
    }
    return dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        },
        target = target,
    )
}

private const val MAX_LOCAL_FILE_BYTES = 512L * 1024L * 1024L
