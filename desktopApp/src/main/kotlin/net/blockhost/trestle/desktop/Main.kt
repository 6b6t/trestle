package net.blockhost.trestle.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import net.blockhost.trestle.app.createDesktopLauncherServices
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.trestle_icon
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp
import org.jetbrains.compose.resources.painterResource

fun main() {
    val viewModel = LauncherViewModel(createDesktopLauncherServices())
    application {
        val icon = painterResource(Res.drawable.trestle_icon)
        Window(
            onCloseRequest = {
                viewModel.close()
                exitApplication()
            },
            state = WindowState(width = 1180.dp, height = 760.dp),
            title = "Trestle",
            icon = icon,
        ) {
            TrestleApp(viewModel)
        }
    }
}
