package net.blockhost.trestle.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.configureSwingGlobalsForCompose
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import net.blockhost.trestle.app.createDesktopLauncherServices
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.trestle_icon
import net.blockhost.trestle.ui.LauncherCommand
import net.blockhost.trestle.ui.LauncherCommandRequest
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureSwingGlobalsForCompose()
    configureDesktopProperties()
    FileKit.init(appId = "net.blockhost.trestle")
    val viewModel = LauncherViewModel(createDesktopLauncherServices())
    application {
        val icon = painterResource(Res.drawable.trestle_icon)
        val state by viewModel.state.collectAsState()
        var commandSequence by remember { mutableLongStateOf(0L) }
        var pendingCommand by remember { mutableStateOf<LauncherCommandRequest?>(null) }
        val sendCommand: (LauncherCommand) -> Unit = { command ->
            commandSequence += 1
            pendingCommand = LauncherCommandRequest(commandSequence, command)
        }
        val desktopIntegration = remember { DesktopIntegration() }
        val systemAccent = remember { SystemAccent() }
        val accentArgb by systemAccent.color
        val windowState = rememberWindowState(
            width = 1180.dp,
            height = 760.dp,
            position = WindowPosition.PlatformDefault,
        )
        Window(
            onCloseRequest = {
                viewModel.close()
                exitApplication()
            },
            state = windowState,
            title = state.operation?.let { "${it.title} · Trestle" } ?: "Trestle",
            icon = icon,
        ) {
            MenuBar {
                Menu("File") {
                    Item("New instance", onClick = { sendCommand(LauncherCommand.NEW_INSTANCE) })
                    Item("Import local file…", onClick = { sendCommand(LauncherCommand.IMPORT_LOCAL_FILE) })
                    Separator()
                    Item(
                        "Quit Trestle",
                        onClick = {
                            viewModel.close()
                            exitApplication()
                        },
                    )
                }
                Menu("View") {
                    Item("Library", onClick = { sendCommand(LauncherCommand.SHOW_LIBRARY) })
                    Item("Discover", onClick = { sendCommand(LauncherCommand.SHOW_DISCOVER) })
                    Item("Accounts", onClick = { sendCommand(LauncherCommand.SHOW_ACCOUNTS) })
                    Item("Settings", onClick = { sendCommand(LauncherCommand.SHOW_SETTINGS) })
                }
                Menu("Instance") {
                    Item(
                        "Launch selected",
                        enabled = state.selectedInstance != null,
                        onClick = { sendCommand(LauncherCommand.LAUNCH_SELECTED) },
                    )
                    Item(
                        if (state.selectedInstance?.pinned == true) "Unpin selected" else "Pin selected",
                        enabled = state.selectedInstance != null,
                        onClick = { sendCommand(LauncherCommand.TOGGLE_SELECTED_PIN) },
                    )
                    Separator()
                    Item(
                        "Remove selected",
                        enabled = state.selectedInstance != null,
                        onClick = { sendCommand(LauncherCommand.REMOVE_SELECTED) },
                    )
                }
                Menu("Help") {
                    Item("Keyboard shortcuts", onClick = { sendCommand(LauncherCommand.SHOW_SHORTCUTS) })
                }
            }
            DisposableEffect(systemAccent) {
                onDispose { systemAccent.close() }
            }
            DisposableEffect(window) {
                desktopIntegration.prepare(window)
                onDispose { desktopIntegration.clear(window) }
            }
            LaunchedEffect(window, state.operation, state.error) {
                desktopIntegration.update(window, state.desktopIndicator())
            }
            TrestleApp(
                state = state,
                actions = viewModel,
                accentColor = accentArgb?.let(::Color),
                externalCommand = pendingCommand,
                onExternalCommandHandled = { sequence ->
                    if (pendingCommand?.sequence == sequence) pendingCommand = null
                },
            )
        }
    }
}

private fun configureDesktopProperties() {
    System.setProperty("apple.awt.application.name", "Trestle")
    System.setProperty("apple.awt.application.appearance", "system")
    WindowsIntegration.configureProcess()
}
