package net.blockhost.trestle.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.distinctUntilChanged
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_close
import net.blockhost.trestle.resources.ic_search
import net.blockhost.trestle.resources.ui_clear_search
import net.blockhost.trestle.resources.ui_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val LocalTrestleWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo?> { null }

internal fun Modifier.trestleSelectable(
    selected: Boolean,
    onClickLabel: String,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
): Modifier = combinedClickable(
    onClickLabel = onClickLabel,
    role = Role.RadioButton,
    onClick = onClick,
    onDoubleClick = onDoubleClick,
).semantics { this.selected = selected }

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
                value.isNotEmpty() -> TrestleTooltipIconButton(
                    label = stringResource(Res.string.ui_clear_search),
                    onClick = { onValueChange("") },
                ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrestleSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    inputModifier: Modifier = Modifier,
    enabled: Boolean = true,
    searching: Boolean = false,
) {
    val textFieldState = rememberTextFieldState(value)

    LaunchedEffect(value) {
        if (textFieldState.text.toString() != value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect(onValueChange)
    }

    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                state = textFieldState,
                onSearch = onSearch,
                expanded = false,
                onExpandedChange = {},
                enabled = enabled,
                placeholder = placeholder,
                leadingIcon = {
                    Icon(painterResource(Res.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = {
                    when {
                        searching -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        textFieldState.text.isNotEmpty() -> TrestleTooltipIconButton(
                            label = stringResource(Res.string.ui_clear_search),
                            onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") },
                        ) {
                            Icon(
                                painterResource(Res.drawable.ic_close),
                                contentDescription = stringResource(Res.string.ui_clear_search),
                            )
                        }
                    }
                },
                modifier = inputModifier.fillMaxWidth(),
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = modifier,
        content = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrestleTooltip(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}

@Composable
internal fun TrestleTooltipIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TrestleTooltip(label, modifier) {
        IconButton(onClick = onClick, enabled = enabled, content = content)
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrestleDialog(
    onDismissRequest: () -> Unit,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    widthFraction: Float = 1f,
    heightFraction: Float? = null,
    minHeight: Dp? = null,
    maxHeight: Dp? = null,
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        TrestleDialogSurface(
            maxWidth = maxWidth,
            modifier = modifier,
            widthFraction = widthFraction,
            heightFraction = heightFraction,
            minHeight = minHeight,
            maxHeight = maxHeight,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrestleDialogHeader(
    title: String,
    onClose: () -> Unit,
    closeEnabled: Boolean = true,
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            TrestleTooltipIconButton(
                label = stringResource(Res.string.ui_close),
                onClick = onClose,
                enabled = closeEnabled,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.ui_close),
                )
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun TrestleDialogActions(content: @Composable RowScope.() -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageHeader(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = WindowInsets(0, 0, 0, 0),
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
