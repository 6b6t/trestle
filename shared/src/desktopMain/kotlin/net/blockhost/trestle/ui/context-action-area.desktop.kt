package net.blockhost.trestle.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

@Composable
internal actual fun ContextActionArea(
    actions: List<ContextAction>,
    content: @Composable () -> Unit,
) {
    ContextMenuArea(
        items = {
            actions.map { action ->
                ContextMenuItem(action.label, action.onClick)
            }
        },
        content = content,
    )
}
