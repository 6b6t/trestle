@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

@Composable
internal actual fun ContextActionArea(
    actions: List<ContextAction>,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
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
            .pointerInput(actions) {
                detectTapGestures(onLongPress = { expanded = true })
            },
    ) {
        content()
        if (expanded) {
            ModalBottomSheet(onDismissRequest = { expanded = false }) {
                Column(Modifier.fillMaxWidth()) {
                    actions.forEach { action ->
                        if (action.separatorBefore) HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(action.label) },
                            modifier = Modifier.fillMaxWidth().clickable {
                                expanded = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
    }
}
