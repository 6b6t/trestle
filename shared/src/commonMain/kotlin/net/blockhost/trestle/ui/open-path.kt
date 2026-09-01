package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable

internal expect val supportsOpenPath: Boolean

@Composable
internal expect fun rememberOpenPath(): (String) -> Unit
