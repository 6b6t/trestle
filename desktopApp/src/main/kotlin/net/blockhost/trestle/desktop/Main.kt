package net.blockhost.trestle.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.configureSwingGlobalsForCompose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import net.blockhost.trestle.app.createDesktopLauncherServices
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.trestle_icon
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
        val desktopIntegration = remember { DesktopIntegration() }
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
            DisposableEffect(window) {
                desktopIntegration.prepare(window)
                onDispose { desktopIntegration.clear(window) }
            }
            LaunchedEffect(window, state.operation, state.error) {
                desktopIntegration.update(window, state.desktopIndicator())
            }
            TrestleApp(viewModel)
        }
    }
}

private fun configureDesktopProperties() {
    System.setProperty("apple.awt.application.name", "Trestle")
    System.setProperty("apple.awt.application.appearance", "system")
    WindowsIntegration.configureProcess()
}
