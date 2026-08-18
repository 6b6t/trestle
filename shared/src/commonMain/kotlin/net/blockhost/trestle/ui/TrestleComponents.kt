package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_close
import net.blockhost.trestle.resources.ic_search
import net.blockhost.trestle.resources.ui_clear_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val LocalTrestleWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo?> { null }

@Composable
internal fun TrestleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searching: Boolean = false,
    onSearch: (String) -> Unit = {},
    placeholder: @Composable () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        leadingIcon = {
            Icon(painterResource(Res.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = {
            when {
                searching -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                value.isNotEmpty() -> IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.ui_clear_search),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(value) }),
    )
}

@Composable
internal fun TrestleDialogSurface(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    heightFraction: Float? = null,
    minHeight: Dp? = null,
    maxHeight: Dp? = null,
    content: @Composable () -> Unit,
) {
    val adaptiveInfo = LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val compact = !adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    val sizeModifier = if (compact) {
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    } else {
        var sizing = Modifier.fillMaxWidth(widthFraction).widthIn(max = maxWidth)
        heightFraction?.let { sizing = sizing.fillMaxHeight(it) }
        if (minHeight != null || maxHeight != null) {
            sizing = sizing.heightIn(
                min = minHeight ?: 0.dp,
                max = maxHeight ?: Dp.Infinity,
            )
        }
        sizing
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = if (compact) RectangleShape else MaterialTheme.shapes.large,
        modifier = sizeModifier.then(modifier),
        content = content,
    )
}

@Composable
internal fun TrestleSwitchItem(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supportingText?.let { text -> { Text(text) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}
