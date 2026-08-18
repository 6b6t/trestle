package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberOpenPath(): (String) -> Unit = remember { { _ -> } }
