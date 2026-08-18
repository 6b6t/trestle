package net.blockhost.trestle.ui

import java.awt.Desktop
import java.io.File

internal actual val supportsPathTrash: Boolean
    get() = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)

internal actual suspend fun movePathToTrash(path: String): Boolean =
    runCatching { Desktop.getDesktop().moveToTrash(File(path)) }.getOrDefault(false)
