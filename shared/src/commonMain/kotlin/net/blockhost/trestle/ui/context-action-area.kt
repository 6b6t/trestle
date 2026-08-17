package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable

internal data class ContextAction(
    val label: String,
    val separatorBefore: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
internal expect fun ContextActionArea(
    actions: List<ContextAction>,
    content: @Composable () -> Unit,
)
