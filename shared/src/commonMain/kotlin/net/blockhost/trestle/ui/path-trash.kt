package net.blockhost.trestle.ui

internal expect val supportsPathTrash: Boolean

internal expect suspend fun movePathToTrash(path: String): Boolean
