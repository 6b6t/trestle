package net.blockhost.trestle.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.trestle_mark
import net.blockhost.trestle.resources.ui_accounts
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_discover
import net.blockhost.trestle.resources.ui_file
import net.blockhost.trestle.resources.ui_import_local_file
import net.blockhost.trestle.resources.ui_instance
import net.blockhost.trestle.resources.ui_keyboard_shortcuts
import net.blockhost.trestle.resources.ui_launch_selected
import net.blockhost.trestle.resources.ui_library
import net.blockhost.trestle.resources.ui_maximize
import net.blockhost.trestle.resources.ui_menu_help
import net.blockhost.trestle.resources.ui_menu_view
import net.blockhost.trestle.resources.ui_minimize
import net.blockhost.trestle.resources.ui_new_instance
import net.blockhost.trestle.resources.ui_pin_selected
import net.blockhost.trestle.resources.ui_quit_trestle
import net.blockhost.trestle.resources.ui_remove_selected
import net.blockhost.trestle.resources.ui_restore_window
import net.blockhost.trestle.resources.ui_settings
import net.blockhost.trestle.resources.ui_stop_named
import net.blockhost.trestle.resources.ui_unpin_selected
import net.blockhost.trestle.text.formattedString
import net.blockhost.trestle.ui.LauncherCommand
import net.blockhost.trestle.ui.LauncherUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.MouseInfo
import java.awt.Point

private enum class TitleBarMenu(val label: StringResource) {
    FILE(Res.string.ui_file),
    VIEW(Res.string.ui_menu_view),
    INSTANCE(Res.string.ui_instance),
    HELP(Res.string.ui_menu_help),
}

private enum class WindowControl {
    MINIMIZE,
    MAXIMIZE,
    RESTORE,
    CLOSE,
}

internal object DesktopTitleBarTestTags {
    const val ROOT = "desktop-title-bar"
    const val DRAG_AREA = "desktop-title-bar-drag-area"
    const val MINIMIZE = "desktop-window-minimize"
    const val MAXIMIZE = "desktop-window-maximize"
    const val CLOSE = "desktop-window-close"
}

@Composable
internal fun WindowScope.DesktopTitleBar(
    title: String,
    state: LauncherUiState,
    isMac: Boolean,
    placement: WindowPlacement,
    onCommand: (LauncherCommand) -> Unit,
    onStopInstance: () -> Unit,
    onQuit: () -> Unit,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    DesktopTitleBarContent(
        title = title,
        state = state,
        isMac = isMac,
        placement = placement,
        onCommand = onCommand,
        onStopInstance = onStopInstance,
        onQuit = onQuit,
        onMinimize = onMinimize,
        onToggleMaximize = onToggleMaximize,
        onClose = onClose,
        dragArea = { modifier, content ->
            WindowDraggableArea(modifier = modifier, content = content)
        },
    )
}

@Composable
internal fun DesktopTitleBarContent(
    title: String,
    state: LauncherUiState,
    isMac: Boolean,
    placement: WindowPlacement,
    onCommand: (LauncherCommand) -> Unit,
    onStopInstance: () -> Unit,
    onQuit: () -> Unit,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    dragArea: @Composable (Modifier, @Composable () -> Unit) -> Unit,
) {
    var expandedMenu by remember { mutableStateOf<TitleBarMenu?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag(DesktopTitleBarTestTags.ROOT),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.trestle_mark),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp).width(24.dp).height(18.dp),
                )
                TitleBarMenu.entries.forEach { menu ->
                    TitleBarMenuButton(
                        menu = menu,
                        expanded = expandedMenu == menu,
                        onExpandedChange = { expandedMenu = if (it) menu else null },
                    ) {
                        when (menu) {
                            TitleBarMenu.FILE -> FileMenu(
                                isMac = isMac,
                                onCommand = onCommand,
                                onQuit = onQuit,
                                onDismiss = { expandedMenu = null },
                            )

                            TitleBarMenu.VIEW -> ViewMenu(
                                isMac = isMac,
                                onCommand = onCommand,
                                onDismiss = { expandedMenu = null },
                            )

                            TitleBarMenu.INSTANCE -> InstanceMenu(
                                state = state,
                                onCommand = onCommand,
                                onStopInstance = onStopInstance,
                                onDismiss = { expandedMenu = null },
                            )

                            TitleBarMenu.HELP -> HelpMenu(
                                onCommand = onCommand,
                                onDismiss = { expandedMenu = null },
                            )
                        }
                    }
                }
                dragArea(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(DesktopTitleBarTestTags.DRAG_AREA),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .titleBarDoubleClick(onToggleMaximize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                WindowControlButton(WindowControl.MINIMIZE, onMinimize)
                WindowControlButton(
                    control = if (placement == WindowPlacement.Maximized) {
                        WindowControl.RESTORE
                    } else {
                        WindowControl.MAXIMIZE
                    },
                    onClick = onToggleMaximize,
                )
                WindowControlButton(WindowControl.CLOSE, onClose)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun TitleBarMenuButton(
    menu: TitleBarMenu,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box {
        TextButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxHeight(),
            shape = RectangleShape,
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Text(stringResource(menu.label), style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.widthIn(min = 220.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun FileMenu(isMac: Boolean, onCommand: (LauncherCommand) -> Unit, onQuit: () -> Unit, onDismiss: () -> Unit) {
    MenuAction(stringResource(Res.string.ui_new_instance), primaryShortcut(isMac, "N")) {
        onDismiss()
        onCommand(LauncherCommand.NEW_INSTANCE)
    }
    MenuAction(stringResource(Res.string.ui_import_local_file) + "…", primaryShortcut(isMac, "O")) {
        onDismiss()
        onCommand(LauncherCommand.IMPORT_LOCAL_FILE)
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    MenuAction(stringResource(Res.string.ui_quit_trestle), primaryShortcut(isMac, "Q")) {
        onDismiss()
        onQuit()
    }
}

@Composable
private fun ViewMenu(isMac: Boolean, onCommand: (LauncherCommand) -> Unit, onDismiss: () -> Unit) {
    MenuAction(stringResource(Res.string.ui_library), primaryShortcut(isMac, "1")) {
        onDismiss()
        onCommand(LauncherCommand.SHOW_LIBRARY)
    }
    MenuAction(stringResource(Res.string.ui_discover), primaryShortcut(isMac, "2")) {
        onDismiss()
        onCommand(LauncherCommand.SHOW_DISCOVER)
    }
    MenuAction(stringResource(Res.string.ui_accounts), primaryShortcut(isMac, "3")) {
        onDismiss()
        onCommand(LauncherCommand.SHOW_ACCOUNTS)
    }
    MenuAction(stringResource(Res.string.ui_settings), primaryShortcut(isMac, ",")) {
        onDismiss()
        onCommand(LauncherCommand.SHOW_SETTINGS)
    }
}

@Composable
private fun InstanceMenu(state: LauncherUiState, onCommand: (LauncherCommand) -> Unit, onStopInstance: () -> Unit, onDismiss: () -> Unit) {
    MenuAction(
        label = stringResource(Res.string.ui_launch_selected),
        enabled = state.selectedInstance?.installationState is InstallationState.Installed &&
            state.activeLaunch == null,
    ) {
        onDismiss()
        onCommand(LauncherCommand.LAUNCH_SELECTED)
    }
    state.activeInstance?.let { instance ->
        MenuAction(formattedString(Res.string.ui_stop_named, instance.displayName)) {
            onDismiss()
            onStopInstance()
        }
    }
    MenuAction(
        label = stringResource(
            if (state.selectedInstance?.pinned == true) Res.string.ui_unpin_selected else Res.string.ui_pin_selected,
        ),
        enabled = state.selectedInstance != null,
    ) {
        onDismiss()
        onCommand(LauncherCommand.TOGGLE_SELECTED_PIN)
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    MenuAction(stringResource(Res.string.ui_remove_selected), enabled = state.selectedInstance != null) {
        onDismiss()
        onCommand(LauncherCommand.REMOVE_SELECTED)
    }
}

@Composable
private fun HelpMenu(onCommand: (LauncherCommand) -> Unit, onDismiss: () -> Unit) {
    MenuAction(stringResource(Res.string.ui_keyboard_shortcuts), "F1") {
        onDismiss()
        onCommand(LauncherCommand.SHOW_SHORTCUTS)
    }
}

@Composable
private fun MenuAction(label: String, shortcut: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        enabled = enabled,
        trailingIcon = shortcut?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun WindowControlButton(control: WindowControl, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val isClose = control == WindowControl.CLOSE
    val background = when {
        isClose && hovered -> MaterialTheme.colorScheme.error
        hovered -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }
    val contentColor = if (isClose && hovered) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val description = stringResource(
        when (control) {
            WindowControl.MINIMIZE -> Res.string.ui_minimize
            WindowControl.MAXIMIZE -> Res.string.ui_maximize
            WindowControl.RESTORE -> Res.string.ui_restore_window
            WindowControl.CLOSE -> Res.string.ui_close
        },
    )
    val testTag = when (control) {
        WindowControl.MINIMIZE -> DesktopTitleBarTestTags.MINIMIZE
        WindowControl.MAXIMIZE, WindowControl.RESTORE -> DesktopTitleBarTestTags.MAXIMIZE
        WindowControl.CLOSE -> DesktopTitleBarTestTags.CLOSE
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
                .background(background)
                .hoverable(interactionSource)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .testTag(testTag),
            contentAlignment = Alignment.Center,
        ) {
            WindowControlIcon(control, contentColor)
        }
    }
}

@Composable
private fun WindowControlIcon(control: WindowControl, color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val stroke = 1.25.dp.toPx()
        when (control) {
            WindowControl.MINIMIZE -> drawLine(
                color = color,
                start = Offset(2.dp.toPx(), 10.dp.toPx()),
                end = Offset(12.dp.toPx(), 10.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )

            WindowControl.MAXIMIZE -> drawRect(
                color = color,
                topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                size = Size(9.dp.toPx(), 9.dp.toPx()),
                style = Stroke(stroke),
            )

            WindowControl.RESTORE -> {
                drawRect(
                    color = color,
                    topLeft = Offset(4.dp.toPx(), 2.dp.toPx()),
                    size = Size(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(stroke),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(2.dp.toPx(), 4.dp.toPx()),
                    size = Size(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(stroke),
                )
            }

            WindowControl.CLOSE -> {
                drawLine(
                    color = color,
                    start = Offset(3.dp.toPx(), 3.dp.toPx()),
                    end = Offset(11.dp.toPx(), 11.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square,
                )
                drawLine(
                    color = color,
                    start = Offset(11.dp.toPx(), 3.dp.toPx()),
                    end = Offset(3.dp.toPx(), 11.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square,
                )
            }
        }
    }
}

private fun Modifier.titleBarDoubleClick(onDoubleClick: () -> Unit): Modifier = pointerInput(onDoubleClick) {
    var previousTapTime: Long? = null
    var previousTapPosition: Point? = null

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        val downPosition = currentPointerPosition()
        val interval = previousTapTime?.let { down.uptimeMillis - it }
        val isSecondTap = interval != null &&
            interval in viewConfiguration.doubleTapMinTimeMillis..viewConfiguration.doubleTapTimeoutMillis &&
            previousTapPosition.isNear(downPosition, viewConfiguration.touchSlop)
        val up = waitForUpOrCancellation(PointerEventPass.Final)
        val upPosition = currentPointerPosition()
        val stayedWithinTapSlop = downPosition.isNear(upPosition, viewConfiguration.touchSlop)

        if (up != null && isSecondTap && stayedWithinTapSlop) {
            previousTapTime = null
            previousTapPosition = null
            onDoubleClick()
        } else if (up != null && stayedWithinTapSlop) {
            previousTapTime = up.uptimeMillis
            previousTapPosition = upPosition
        } else {
            previousTapTime = null
            previousTapPosition = null
        }
    }
}

private fun currentPointerPosition(): Point? = MouseInfo.getPointerInfo()?.location

private fun Point?.isNear(other: Point?, distance: Float): Boolean = this != null && other != null && this.distance(other) <= distance

private fun primaryShortcut(isMac: Boolean, key: String): String = if (isMac) "⌘$key" else "Ctrl+$key"
