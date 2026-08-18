package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun Modifier.localFileDropTarget(
    enabled: Boolean,
    extensions: Set<String>,
    onActiveChange: (Boolean) -> Unit,
    onFiles: (List<LocalDroppedFile>) -> Unit,
    onFailure: (String) -> Unit,
): Modifier = this
