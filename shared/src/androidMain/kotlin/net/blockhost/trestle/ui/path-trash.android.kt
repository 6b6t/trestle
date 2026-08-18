package net.blockhost.trestle.ui

internal actual val supportsPathTrash: Boolean = false

internal actual suspend fun movePathToTrash(path: String): Boolean = false
