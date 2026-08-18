@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
internal actual fun ContextActionArea(
    actions: List<ContextAction>,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val menuOffset = with(density) {
        DpOffset(menuPosition.x.toDp(), menuPosition.y.toDp())
    }

    Box(
        modifier = Modifier
            .semantics {
                customActions = actions.map { action ->
                    CustomAccessibilityAction(action.label) {
                        action.onClick()
                        true
                    }
                }
            }
            .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) { event ->
                if (event.buttons.isSecondaryPressed) {
                    menuPosition = event.changes.firstOrNull()?.position ?: Offset.Zero
                    expanded = true
                    event.changes.forEach { it.consume() }
                }
            },
    ) {
        content()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = menuOffset,
            modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
        ) {
            actions.forEach { action ->
                if (action.separatorBefore) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
